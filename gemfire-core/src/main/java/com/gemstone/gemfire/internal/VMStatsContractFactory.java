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
package com.gemstone.gemfire.internal;

import com.gemstone.gemfire.*;
import com.gemstone.gemfire.internal.stats50.VMStats50;

/**
 * Factory used to produce an instance of VMStatsContract.
 */
public class VMStatsContractFactory {
  /**
   * Create and return a VMStatsContract.
   */
  public static VMStatsContract create(StatisticsFactory f, long id) {
    VMStatsContract result;
    try {
      result = new VMStats50(f, id);
    } 
    catch (Throwable t) {
      Error err;
      if (t instanceof Error && SystemFailure.isJVMFailureError(
          err = (Error)t)) {
        SystemFailure.initiateFailure(err);
        // If this ever returns, rethrow the error. We're poisoned
        // now, so don't let this thread continue.
        throw err;
      }
      // Now that we no longer support 1.4 I'm not sure why we would get here.
      // But just in case other vm vendors don't support mxbeans I've left
      // this logic in that will create a simple VMStats instance.
      // Whenever you catch Error or Throwable, you must also
      // check for fatal JVM error (see above).  However, there is
      // _still_ a possibility that you are dealing with a cascading
      // error condition, so you also need to check to see if the JVM
      // is still usable:
      SystemFailure.checkFailure();
      //LogWriterI18n log = com.gemstone.gemfire.distributed.internal.InternalDistributedSystem.getAnyInstance().getLogWriter();
      //log.warning("Could not create 5.0 VMStats", ignore);
      // couldn't create the 1.5 version so create the old 1.4 version
      result = new VMStats(f, id);
    }
    return result;
  }
  
  private VMStatsContractFactory() {
    // private so no instances allowed. static methods only
  }
}
