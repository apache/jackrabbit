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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Credentials;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * <code>ConcurrentLoginTest</code> starts multiple threads and repeatedly does
 * a {@link javax.jcr.Repository#login(javax.jcr.Credentials)}.
 */
public class ConcurrentLoginTest extends AbstractJCRTest {

    /**
     * The number of threads.
     */
    private static final int NUM_THREADS = 10;

    /**
     * The number of login calls each thread will do in a test run.
     */
    private static final int NUM_LOGINS_PER_THREAD = 100;

    /**
     * Tests concurrent logins on the Repository.
     */
    public void testLogin() throws RepositoryException {
        final Exception[] exception = new Exception[1];
        List testRunner = new ArrayList();
        for (int i = 0; i < NUM_THREADS; i++) {
            testRunner.add(new Thread(new Runnable() {
                public void run() {
                    Credentials cred = getHelper().getSuperuserCredentials();
                    for (int i = 0; i < NUM_LOGINS_PER_THREAD; i++) {
                        try {
                            Session s = getHelper().getRepository().login(cred);
                            // immediately logout
                            s.logout();
                        } catch (Exception e) {
                            exception[0] = e;
                            break;
                        }
                    }
                }
            }));
        }

        // start threads
        for (Iterator it = testRunner.iterator(); it.hasNext(); ) {
            ((Thread) it.next()).start();
        }

        // join threads
        for (Iterator it = testRunner.iterator(); it.hasNext(); ) {
            try {
                ((Thread) it.next()).join();
            } catch (InterruptedException e) {
                fail(e.toString());
            }
        }

        if (exception[0] != null) {
            fail(exception[0].toString());
        }
    }
}
