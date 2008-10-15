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
package org.apache.jackrabbit.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Credentials;

/** <code>LoginTest</code>... */
public class LoginTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(LoginTest.class);

    private static int MINTIME = 500;
    private static int MINCOUNT = 5;

    private void performTest(String testName, Credentials creds, boolean accessRoot) throws RepositoryException {
        long start = System.currentTimeMillis();
        long cnt = 0;

        while (System.currentTimeMillis() - start < MINTIME || cnt < MINCOUNT) {
            Session s = helper.getRepository().login(creds);
            try {
                if (accessRoot) {
                    s.getRootNode();
                }
            } finally {
                s.logout();
            }
            cnt++;
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info(testName + ": " +  (double)elapsed / cnt + "ms per call (" + cnt + " iterations)");
    }

    public void testLoginReadOnly() throws RepositoryException {
        performTest("testLoginReadOnly", helper.getReadOnlyCredentials(), false);
    }

    public void testLoginSuperuser() throws RepositoryException {
        performTest("testLoginSuperuser", helper.getSuperuserCredentials(), false);
    }

    public void testLoginReadWrite() throws RepositoryException {
        performTest("testLoginReadWrite", helper.getReadWriteCredentials(), false);
    }

    public void testLoginAccessRoot() throws RepositoryException {
        performTest("testLoginAccessRoot", helper.getReadOnlyCredentials(), true);
    }
}