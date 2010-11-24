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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.state.DefaultISMLocking;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ISMLocking.ReadLock;
import org.apache.jackrabbit.core.state.ISMLocking.WriteLock;
import org.apache.jackrabbit.test.JUnitTest;

/**
 * Tests the DefaultISMLocking class.
 */
public class DefaultISMLockingDeadlockTest extends JUnitTest {

    public void test() throws InterruptedException {
        final ISMLocking lock = new DefaultISMLocking();
        WriteLock w1 = lock.acquireWriteLock(null);
        ReadLock r1 = w1.downgrade();
        final InterruptedException[] ex = new InterruptedException[1];
        Thread thread = new Thread() {
            public void run() {
                try {
                    lock.acquireWriteLock(null).release();
                } catch (InterruptedException e) {
                    ex[0] = e;
                }
            }
        };
        thread.start();
        Thread.sleep(100);
        lock.acquireReadLock(null).release();
        r1.release();
        thread.join();
        if (ex[0] != null) {
            throw ex[0];
        }
    }

}
