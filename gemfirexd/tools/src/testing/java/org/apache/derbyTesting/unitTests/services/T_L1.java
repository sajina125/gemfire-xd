/*

   Derby - Class org.apache.derbyTesting.unitTests.services.T_L1

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package org.apache.derbyTesting.unitTests.services;

import com.pivotal.gemfirexd.internal.iapi.services.locks.*;

import com.pivotal.gemfirexd.internal.iapi.services.sanity.SanityManager;
import java.util.Hashtable;

/**
	Unit test Lockable
	<BR>
	A simple Lockable that allows a single locker, but that locker
	can lock the object multiple times, standard Lockable behaviour.
*/

class T_L1 implements Lockable {

	long value = 0;
	int count = 0;

	Latch latch;

	T_L1() {
	}

	/*
	** Lockable methods (Simple, qualifier assumed to be null).
	*/

	/** 
		Qualififier is assumed to be null.
	@see Lockable#lockEvent
	*/
	public void lockEvent(Latch lockInfo) {
        if (SanityManager.DEBUG)
            SanityManager.ASSERT(lockInfo.getQualifier() == null);

		latch = lockInfo;

		count++;
	}

	public boolean requestCompatible(Object requestedQualifier, Object grantedQualifier) {
		return false;
	}

	public boolean lockerAlwaysCompatible() {
		return true;
	}

	/** 
		Qualififier is assumed to be null.
	@see Lockable#unlockEvent
	*/
	public void unlockEvent(Latch lockInfo) {
        if (SanityManager.DEBUG)
            SanityManager.ASSERT(lockInfo.getQualifier() == null);
		
		count--;
        if (SanityManager.DEBUG)
            SanityManager.ASSERT(count >= 0);
		latch = null;
	}

	public boolean lockAttributes(int flag, Hashtable t)
	{
		return false;
	}
}