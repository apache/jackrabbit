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
package org.apache.jackrabbit.webdav.jcr.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.xml.Namespace;

public class JcrUserPrivilegesPropertyTest extends AbstractSecurityTest {

    private Set<Privilege> getExpected(AccessControlManager acMgr, Session s) throws RepositoryException {
        Set<Privilege> expected = new HashSet<Privilege>();
        for (javax.jcr.security.Privilege p : acMgr.getPrivileges(testRoot)) {
            String localName = Text.getLocalName(p.getName());
            String prefix = Text.getNamespacePrefix(p.getName());
            Namespace ns = (prefix.isEmpty()) ? Namespace.EMPTY_NAMESPACE : Namespace.getNamespace(prefix, s.getNamespaceURI(prefix));
            expected.add(Privilege.getPrivilege(localName, ns));
        }
        return expected;
    }

    public void testAdminPrivileges() throws RepositoryException {
        Set<Privilege> expected = getExpected(acMgr, superuser);

        JcrUserPrivilegesProperty upp = new JcrUserPrivilegesProperty(superuser, testRoot);
        Collection<Privilege> davPrivs = upp.asDavProperty().getValue();

        assertEquals(expected.size(), davPrivs.size());
        assertTrue(davPrivs.containsAll(expected));
    }

    public void testReadOnlyPrivileges() throws RepositoryException {
        Session readOnly = getHelper().getReadOnlySession();
        try {
            Set<Privilege> expected = getExpected(readOnly.getAccessControlManager(), readOnly);

            JcrUserPrivilegesProperty upp = new JcrUserPrivilegesProperty(readOnly, testRoot);
            Collection<Privilege> davPrivs = upp.asDavProperty().getValue();

            assertEquals(expected.size(), davPrivs.size());
            assertTrue(davPrivs.containsAll(expected));
        } finally {
            if (readOnly != null) {
                readOnly.logout();
            }
        }
    }
}