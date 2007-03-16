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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Session;
import javax.jcr.Credentials;
import javax.jcr.NodeIterator;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.LoginException;
import java.security.AccessControlException;

/**
 * Tests if {@link Session#impersonate(Credentials)} to a read-only session
 * respects access controls.
 *
 * @test
 * @sources ImpersonateTest.java
 * @executeClass org.apache.jackrabbit.test.api.ImpersonateTest
 * @keywords level2
 */
public class ImpersonateTest extends AbstractJCRTest {

    /**
     * Tests if <code>Session.impersonate(Credentials)</code> works properly
     */
    public void testImpersonate() throws RepositoryException, NotExecutableException {
        // impersonate to read-only user
        Session session;
        try {
            session = superuser.impersonate(helper.getReadOnlyCredentials());
        } catch (LoginException e) {
            throw new NotExecutableException("impersonate threw LoginException");
        }

        try {
            // get a path to test the permissions on
            String thePath = "";
            NodeIterator ni = session.getRootNode().getNodes();
            while (ni.hasNext()) {
                Node n = ni.nextNode();
                if (!n.getPath().equals("/" + jcrSystem)) {
                    thePath = n.getPath();
                    break;
                }
            }

            // check that all 4 permissions are granted/denied correctly
            session.checkPermission(thePath, "read");

            try {
                session.checkPermission(thePath + "/" + nodeName1, "add_node");
                fail("add_node permission on \"" + thePath + "/" + nodeName1 + "\" granted to read-only Session");
            } catch (AccessControlException success) {
                // ok
            }

            try {
                session.checkPermission(thePath + "/" + propertyName1, "set_property");
                fail("set_property permission on \"" + thePath + "/" + propertyName1 + "\" granted to read-only Session");
            } catch (AccessControlException success) {
                // ok
            }

            try {
                session.checkPermission(thePath, "remove");
                fail("remove permission on \"" + thePath + "\" granted to read-only Session");
            } catch (AccessControlException success) {
                // ok
            }

        } finally {
            session.logout();
        }
    }
}
