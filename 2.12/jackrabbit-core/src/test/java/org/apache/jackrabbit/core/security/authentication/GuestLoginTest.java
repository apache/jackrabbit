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
package org.apache.jackrabbit.core.security.authentication;

import javax.jcr.GuestCredentials;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/** <code>GuestLoginTest</code>... */
public class GuestLoginTest extends AbstractJCRTest {

    private Session guest;

    protected void setUp() throws Exception {
        super.setUp();
        guest = getHelper().getRepository().login(new GuestCredentials());
    }

    protected void tearDown() throws Exception {
        if (guest != null) {
            guest.logout();
        }
        super.tearDown();
    }

    /**
     * Implementation specific test: userID must never be null.
     *
     * @throws RepositoryException
     */
    public void testUserID() throws RepositoryException {
        assertNotNull(guest.getUserID());
    }
}