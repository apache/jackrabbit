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
package org.apache.jackrabbit.api.security.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.AbstractConcurrencyTest;
import org.apache.jackrabbit.core.security.TestPrincipal;

public class ConcurrentCreateUserTest extends AbstractConcurrencyTest {

    /**
     * The number of threads.
     */
    private static final int CONCURRENCY = 5;

    /**
     * Number of tries per user.
     */
    private static final int RETRIES = 3;

    List<String> userIDs = Collections
            .synchronizedList(new ArrayList<String>());
    public static final String INTERMEDIATE_PATH = UUID.randomUUID().toString();

    @Override
    protected void tearDown() throws Exception {

        try {
            for (String id : userIDs) {
                Authorizable a = ((JackrabbitSession) superuser)
                        .getUserManager().getAuthorizable(id);
                a.remove();
                superuser.save();
            }
        } catch (Exception e) {
            // this is best effort
        }

        super.tearDown();
    }

    public void testCreateUsers() throws Exception {

        log.println("ConcurrentCreateUserTest.testCreateUsers: c="
                + CONCURRENCY);
        log.flush();

        runTask(new Task() {
            public void execute(Session session, Node test)
                    throws RepositoryException {
                JackrabbitSession s = null;
                try {
                    s = (JackrabbitSession) getHelper().getSuperuserSession();

                    String name = "newname-" + UUID.randomUUID();
                    Authorizable authorizable = null;
                    int maxtries = RETRIES;
                    RepositoryException lastex = null;

                    while (authorizable == null && maxtries > 0) {
                        try {
                            maxtries -= 1;
                            authorizable = s.getUserManager().createUser(name,
                                    "password1", new TestPrincipal(name),
                                    INTERMEDIATE_PATH);
                            s.save();
                        } catch (InvalidItemStateException ex) {
                            lastex = ex;
                            log.println("got " + ex + ", retrying");
                        }
                    }
                    if (authorizable == null) {
                        throw new RepositoryException("user " + name
                                + " not created in " + RETRIES + " attempts.",
                                lastex);
                    }
                    userIDs.add(authorizable.getID());
                    log.println(authorizable + " created");
                } finally {
                    s.logout();
                }
            }
        }, CONCURRENCY, "/" + testPath);
    }
}
