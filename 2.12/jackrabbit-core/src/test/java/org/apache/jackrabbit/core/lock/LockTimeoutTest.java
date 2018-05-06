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
package org.apache.jackrabbit.core.lock;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.transaction.UserTransaction;
import org.apache.jackrabbit.core.UserTransactionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test lock timeout expiration.
 */
public class LockTimeoutTest extends AbstractJCRTest {

    public void testExpired() throws Exception {
        testExpired(false);
        testExpired(true);
    }

    private void testExpired(boolean xa) throws Exception {
        Session s = testRootNode.getSession();
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        s.save();

        UserTransaction utx = null;
        if (xa) {
            utx = new UserTransactionImpl(s);
            utx.begin();
        }

        javax.jcr.lock.LockManager lm = s.getWorkspace().getLockManager();

        boolean isDeep;
        boolean isSessionScoped;
        long timeoutHint;
        String ownerInfo;

        isDeep = false;
        isSessionScoped = false;
        timeoutHint = 1;
        ownerInfo = "";
        Session s2 = getHelper().getSuperuserSession();

        Lock l = lm.lock(n.getPath(), isDeep, isSessionScoped, timeoutHint, ownerInfo);
        // this works only for timeout = 1,
        // as getSecondsRemaining always returns a positive value
        assertEquals(timeoutHint, l.getSecondsRemaining());
        assertTrue(l.isLive());

        if (xa) {
            utx.commit();
        }

        long start = System.currentTimeMillis();
        while (true) {
            Thread.sleep(100);
            long now = System.currentTimeMillis();
            boolean success;
            try {
                s2.getNode(n.getPath()).setProperty("x", 1);
                s2.save();
                success = true;
            } catch (Exception e) {
                success = false;
            }
            long t = now - start;
            if (t > timeoutHint + 3000) {
                assertTrue(success);
                break;
            } else if (t < timeoutHint) {
                assertFalse(success);
            }
        }
        n.remove();
        s.save();
    }

}
