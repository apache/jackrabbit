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
package javax.jcr.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>SessionScopedLockTest</code>... */
public class SessionScopedLockTest extends AbstractLockTest {

    private static Logger log = LoggerFactory.getLogger(SessionScopedLockTest.class);

    protected boolean isSessionScoped() {
        return true;
    }

    protected boolean isDeep() {
        return false;
    }

    /**
     * {@link javax.jcr.lock.Lock#getLockToken()} must
     * always return <code>null</code> for session scoped locks.
     */
    public void testGetLockToken() {
        assertNull("A session scoped lock may never expose the token.", lock.getLockToken());
    }
}