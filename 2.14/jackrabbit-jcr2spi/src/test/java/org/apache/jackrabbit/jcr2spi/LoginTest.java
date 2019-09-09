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
package org.apache.jackrabbit.jcr2spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Session;
import javax.jcr.RepositoryException;

/** <code>LoginTest</code>... */
public class LoginTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(LoginTest.class);

    public void testNullLogin() throws RepositoryException {
        Session s = getHelper().getRepository().login();
        try {
            assertNotNull(s.getWorkspace().getName());
        } finally {
            s.logout();
        }
    }

    public void testNullWorkspaceLogin() throws RepositoryException {
        Session s = getHelper().getRepository().login((String) null);
        try {
            assertNotNull(s.getWorkspace().getName());
        } finally {
            s.logout();
        }
    }

    public void testNullCredentialsNullWorkspaceLogin() throws RepositoryException {
        Session s = getHelper().getRepository().login(null, null);
        try {
            assertNotNull(s.getWorkspace().getName());
        } finally {
            s.logout();
        }
    }
}