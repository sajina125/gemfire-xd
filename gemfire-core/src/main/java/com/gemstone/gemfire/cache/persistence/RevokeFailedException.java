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
package com.gemstone.gemfire.cache.persistence;

import com.gemstone.gemfire.GemFireException;

/**
 * Thrown when a member tries to revoke a persistent ID, but the member
 * with that persistent ID is currently running. You can only revoke
 * members which is not running.
 * 
 * @author dsmith
 * @since 6.6.2
 */
public class RevokeFailedException extends GemFireException {

  private static final long serialVersionUID = -2629287782021455875L;

  public RevokeFailedException() {
    super();
  }

  public RevokeFailedException(String message, Throwable cause) {
    super(message, cause);
  }

  public RevokeFailedException(String message) {
    super(message);
  }

  public RevokeFailedException(Throwable cause) {
    super(cause);
  }

  

}