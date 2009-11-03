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

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;

/**
 * <code>CryptedSimpleCredentialsTest</code>...
 */
public class CryptedSimpleCredentialsTest extends AbstractJCRTest {

    private String userID;
    private CryptedSimpleCredentials creds;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        userID = superuser.getUserID();
        if (superuser instanceof JackrabbitSession) {
            UserManager umg = ((JackrabbitSession) superuser).getUserManager();
            User u = (User) umg.getAuthorizable(userID);
            Credentials crd = u.getCredentials();
            if (crd instanceof SimpleCredentials) {
                creds = new CryptedSimpleCredentials((SimpleCredentials) crd);
            } else {
                throw new NotExecutableException();
            }
        } else {
            throw new NotExecutableException();
        }
    }

    public void testUserIDMatchesCaseInsensitive() throws Exception {
        String uid = userID.toUpperCase();
        assertTrue(creds.matches(new SimpleCredentials(uid, creds.getPassword().toCharArray())));

        uid = userID.toLowerCase();
        assertTrue(creds.matches(new SimpleCredentials(uid, creds.getPassword().toCharArray())));
    }

    public void testGetUserID() {
        assertEquals(userID, creds.getUserID());
    }
}