/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.version;

import junit.framework.TestCase;

import java.util.Calendar;

/**
 * <code>InternalVersionHistoryImplTest</code> executes tests for
 * {@link InternalVersionHistoryImpl}.
 */
public class InternalVersionHistoryImplTest extends TestCase {

    /**
     * Checks if {@link InternalVersionHistoryImpl#getCurrentTime()} returns
     * monotonically increasing Calendar instances.
     */
    public void testGetCurrentTime() {
        Calendar last = InternalVersionHistoryImpl.getCurrentTime();
        for (int i = 0; i < 100; i++) {
            Calendar next = InternalVersionHistoryImpl.getCurrentTime();
            assertTrue("InternalVersionHistoryImpl.getCurrentTime() not monotonically increasing",
                    last.getTimeInMillis() < next.getTimeInMillis());
            last = next;
        }
    }
}
