/*
 * Copyright (c) 2010-2015 Pivotal Software, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */
package com.gemstone.gemfire.internal.cache.persistence.soplog;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import com.gemstone.gemfire.internal.cache.persistence.soplog.Compactor.CompactionHandler;
import com.gemstone.gemfire.internal.cache.persistence.soplog.Compactor.CompactionTracker;
import com.gemstone.gemfire.internal.cache.persistence.soplog.SortedOplog.SortedOplogWriter;
import com.gemstone.gemfire.internal.cache.persistence.soplog.hfile.HFileSortedOplogFactory;
import com.gemstone.gemfire.internal.util.LogService;

public abstract class CompactionTestCase<T extends Comparable<T>> extends TestCase {
  protected SortedOplogFactory factory;
  protected AbstractCompactor<T> compactor;
  
  public void testMaxCompaction() throws Exception {
    for (int i = 0; i < 100; i += 2) {
      compactor.add(createSoplog(0, 100, i));
      compactor.add(createSoplog(100, 100, i+1));

      WaitingHandler wh = new WaitingHandler();
      compactor.compact(false, wh);
      wh.waitForCompletion();
    }
    
    WaitingHandler wh = new WaitingHandler();
    compactor.compact(true, wh);
    wh.waitForCompletion();
  }
  
  public void testClear() throws IOException {
    compactor.add(createSoplog(0, 100, 0));
    compactor.add(createSoplog(100, 100, 1));
    compactor.clear();
    
    assertEquals(0, compactor.getActiveReaders(null, null).size());
    assertFalse(compactor.getLevel(0).needsCompaction());
  }
  
  public void testInterruptedCompaction() throws IOException {
    compactor.add(createSoplog(0, 100, 0));
    compactor.add(createSoplog(100, 100, 1));
    
    compactor.testAbortDuringCompaction = true;
    boolean compacted = compactor.compact();
    
    assertFalse(compacted);
    assertEquals(2, compactor.getActiveReaders(null, null).size());
    assertTrue(compactor.getLevel(0).needsCompaction());
  }
  
  public void testClearDuringCompaction() throws Exception {
    compactor.add(createSoplog(0, 100, 0));
    compactor.add(createSoplog(100, 100, 1));
    
    compactor.testDelayDuringCompaction = new CountDownLatch(1);
    WaitingHandler wh = new WaitingHandler();
    
    LogService.logger().fine("Invoking compact");
    compactor.compact(false, wh);
    
    LogService.logger().fine("Invoking clear");
    compactor.clear();
    
    boolean compacted = wh.waitForCompletion();
    
    assertFalse(compacted);
    assertEquals(0, compactor.getActiveReaders(null, null).size());
  }
  
  public void testClearRepeat() throws Exception {
    int i = 0;
    do {
      testClearDuringCompaction();
      LogService.logger().fine("Test " + i + " is complete");
      tearDown();
      setUp();
    } while (i++ < 100);
  }

  public void testCloseRepeat() throws Exception {
    int i = 0;
    do {
      testCloseDuringCompaction();
      LogService.logger().fine("Test " + i + " is complete");
      tearDown();
      setUp();
    } while (i++ < 100);
  }

  public void testCloseDuringCompaction() throws Exception {
    compactor.add(createSoplog(0, 100, 0));
    compactor.add(createSoplog(100, 100, 1));
    
    compactor.testDelayDuringCompaction = new CountDownLatch(1);
    WaitingHandler wh = new WaitingHandler();
    
    compactor.compact(false, wh);
    compactor.close();
    
    boolean compacted = wh.waitForCompletion();
    
    assertFalse(compacted);
    assertEquals(0, compactor.getActiveReaders(null, null).size());
  }

  public void setUp() throws IOException {
    factory = new HFileSortedOplogFactory("test", null, new SortedOplogStatistics("stats", "test"), new HFileStoreStatistics("storeStats", "test"));
    compactor = createCompactor(factory);
  }
  
  public void tearDown() throws Exception {
    compactor.close();
    for (File f : SortedReaderTestCase.getSoplogsToDelete()) {
      f.delete();
    }
  }
  
  protected SortedOplog createSoplog(int start, int count, int id) throws IOException {
    SortedOplog soplog = factory.createSortedOplog(new File("test-" + id + ".soplog"));
    SortedOplogWriter wtr = soplog.createWriter();
    try {
      for (int i = start; i < start + count; i++) {
        wtr.append(SortedReaderTestCase.wrapInt(i), SortedReaderTestCase.wrapInt(i));
      }
    } finally {
      wtr.close(null);
    }
    return soplog;
  }

  protected abstract AbstractCompactor<T> createCompactor(SortedOplogFactory factory) throws IOException;
  
  static class WaitingHandler implements CompactionHandler {
    private final CountDownLatch latch;
    private final AtomicReference<Throwable> err;
    private volatile boolean compacted;
    
    public WaitingHandler() {
      latch = new CountDownLatch(1);
      err = new AtomicReference<Throwable>();
    }
    
    public boolean waitForCompletion() throws InterruptedException {
      LogService.logger().fine("Waiting for compaction to complete");
      latch.await();
      LogService.logger().fine("Done waiting!");
      
      return compacted;
    }
    
    public Throwable getError() {
      return err.get();
    }
    
    @Override
    public void complete(boolean compacted) {
      LogService.logger().fine("Compaction completed with " + compacted);
      this.compacted = compacted;
      latch.countDown();
    }

    @Override
    public void failed(Throwable ex) {
      err.set(ex);
      latch.countDown();
    }
  }
  
  static class FileTracker implements CompactionTracker<Integer> {
    @Override
    public void fileDeleted(File f) {
    }

    @Override
    public void fileAdded(File f, Integer attach) {
    }

    @Override
    public void fileRemoved(File f, Integer attach) {
    }
  }
}
