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

package com.gemstone.gemfire.internal.shared.unsafe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import com.gemstone.gemfire.internal.shared.ChannelBufferInputStream;
import com.gemstone.gemfire.internal.shared.ClientSharedUtils;
import com.gemstone.gemfire.internal.shared.InputStreamChannel;

/**
 * A somewhat more efficient implementation of {@link ChannelBufferInputStream}
 * using internal unsafe class (~30% in raw read calls). Use this only when
 * {@link UnsafeHolder#getDirectByteBufferAddressMethod()} returns non-null.
 * Alternatively use {@link UnsafeHolder#newChannelBufferInputStream} method to
 * create either this or {@link ChannelBufferInputStream} depending on
 * availability.
 * <p>
 * Note that the close() method of this class does not closing the underlying
 * channel.
 * 
 * @author swale
 * @since gfxd 1.1
 */
public class ChannelBufferUnsafeInputStream extends InputStreamChannel {

  protected final ByteBuffer buffer;
  protected final long baseAddress;
  /**
   * Actual buffer position (+baseAddress) accounting is done by this. Buffer
   * position is adjusted during refill and other places where required using
   * this.
   */
  protected long addrPosition;
  protected long addrLimit;

  protected static final sun.misc.Unsafe unsafe = UnsafeHolder.getUnsafe();

  public ChannelBufferUnsafeInputStream(ReadableByteChannel channel)
      throws IOException {
    this(channel, ChannelBufferInputStream.DEFAULT_BUFFER_SIZE);
  }

  public ChannelBufferUnsafeInputStream(ReadableByteChannel channel,
      int bufferSize) throws IOException {
    super(channel);
    if (bufferSize <= 0) {
      throw new IllegalArgumentException("invalid bufferSize=" + bufferSize);
    }
    this.buffer = allocateBuffer(bufferSize);
    // flip to force refill on first use
    this.buffer.flip();

    try {
      this.baseAddress = (Long)UnsafeHolder.getDirectByteBufferAddressMethod()
          .invoke(this.buffer);
      resetBufferPositions();
    } catch (Exception e) {
      throw ClientSharedUtils.newRuntimeException(
          "failed in creating an 'unsafe' buffered channel stream", e);
    }
  }

  protected final void resetBufferPositions() {
    this.addrPosition = this.baseAddress + this.buffer.position();
    this.addrLimit = this.baseAddress + this.buffer.limit();
  }

  protected ByteBuffer allocateBuffer(int bufferSize) {
    return ByteBuffer.allocateDirect(bufferSize);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int read() throws IOException {
    final long addrPos = this.addrPosition;
    if (addrPos < this.addrLimit) {
      this.addrPosition++;
      return (unsafe.getByte(addrPos) & 0xff);
    }
    else if (refillBuffer(this.buffer, 1, null) > 0) {
      return (unsafe.getByte(this.addrPosition++) & 0xff);
    }
    else {
      return -1;
    }
  }

  private final int read_(byte[] buf, int off, int len) throws IOException {
    if (len == 1) {
      final long addrPos = this.addrPosition;
      if (addrPos < this.addrLimit) {
        buf[off] = unsafe.getByte(addrPos);
        this.addrPosition++;
        return 1;
      }
      else if (refillBuffer(this.buffer, 1, null) > 0) {
        buf[off] = unsafe.getByte(this.addrPosition++);
        return 1;
      }
      else {
        return -1;
      }
    }

    // first copy anything remaining from buffer
    final int remaining = (int)(this.addrLimit - this.addrPosition);
    if (len <= remaining) {
      if (len > 0) {
        UnsafeHolder.bufferGet(buf, this.addrPosition, off, len, unsafe);
        this.addrPosition += len;
        return len;
      }
      else {
        return 0;
      }
    }

    // refill buffer once and read whatever available into buf;
    // caller should invoke in a loop if buffer is still not full
    int readBytes = 0;
    if (remaining > 0) {
      UnsafeHolder.bufferGet(buf, this.addrPosition, off, remaining, unsafe);
      this.addrPosition += remaining;
      off += remaining;
      len -= remaining;
      readBytes += remaining;
    }
    final int bufBytes = refillBuffer(this.buffer, 1, null);
    if (bufBytes > 0) {
      if (len > bufBytes) {
        len = bufBytes;
      }
      UnsafeHolder.bufferGet(buf, this.addrPosition, off, len, unsafe);
      this.addrPosition += len;
      return (readBytes + len);
    }
    else {
      return readBytes > 0 ? readBytes : bufBytes;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int read(byte[] buf) throws IOException {
    return read_(buf, 0, buf.length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int read(ByteBuffer dst) throws IOException {
    // We will just use our ByteBuffer for the read. It might be possible
    // to get slight performance advantage in using unsafe instead, but
    // copying to ByteBuffer will not be efficient without reflection
    // to get dst's native address in case it is a direct byte buffer.
    // Avoiding the complication since the benefit will be very small
    // in any case (and reflection cost may well offset that).
    // We can use unsafe for a small perf benefit for heap byte buffers.

    // adjust this buffer position first
    this.buffer.position((int)(this.addrPosition - this.baseAddress));
    try {
      // now we are actually set to just call base class method
      return super.readBuffered(dst, this.buffer);
    } finally {
      // finally reset the raw positions from buffer
      resetBufferPositions();
    }
  }

  /**
  /**
   * {@inheritDoc}
   */
  @Override
  public final int read(byte[] buf, int off, int len) throws IOException {
    if (UnsafeHolder.checkBounds(off, len, buf.length)) {
      return read_(buf, off, len);
    }
    else {
      throw new IndexOutOfBoundsException("offset=" + off + " length=" + len
          + " size=" + buf.length);
    }
  }

  /**
   * {@inheritDoc}
   */
  public final int readInt() throws IOException {
    final long addrPos = this.addrPosition;
    if ((this.addrLimit - addrPos) >= 4) {
      return getInt(addrPos);
    }
    else {
      refillBuffer(this.buffer, 4, "readInt: premature end of stream");
      return getInt(this.addrPosition);
    }
  }

  protected final int getInt(long addrPos) {
    final sun.misc.Unsafe unsafe = ChannelBufferUnsafeInputStream.unsafe;
    int result = (unsafe.getByte(addrPos++) & 0xff);
    result = (result << 8) | (unsafe.getByte(addrPos++) & 0xff);
    result = (result << 8) | (unsafe.getByte(addrPos++) & 0xff);
    result = (result << 8) | (unsafe.getByte(addrPos++) & 0xff);
    this.addrPosition = addrPos;
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int available() throws IOException {
    return (int)(this.addrLimit - this.addrPosition);
  }

  @Override
  protected int refillBuffer(final ByteBuffer channelBuffer,
      final int tryReadBytes, final String eofMessage) throws IOException {
    // adjust this buffer position first
    channelBuffer.position((int)(this.addrPosition - this.baseAddress));
    try {
      return super.refillBuffer(channelBuffer, tryReadBytes, eofMessage);
    } finally {
      // adjust back position and limit
      resetBufferPositions();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean isOpen() {
    return this.channel.isOpen();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException {
    this.buffer.clear();
    resetBufferPositions();
  }
}
