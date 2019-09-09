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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.security.SupportedPrivilege;
import org.apache.jackrabbit.webdav.security.SupportedPrivilegeSetProperty;
import org.apache.jackrabbit.webdav.xml.Namespace;

/**
 * JcrSupportedPrivilegesProperty...
 */
public class JcrSupportedPrivilegesProperty {

    private final Session session;
    private final String absPath;
    private final Set<Privilege> privileges = new HashSet<Privilege>();

    private final Map<String, SupportedPrivilege> supportedPrivileges = new HashMap<String, SupportedPrivilege>();
    private final HashSet<String> aggregated = new HashSet<String>();

    /**
     * Build supported privileges for the jcr:all privilege.
     *
     * @param session The reading session
     */
    public JcrSupportedPrivilegesProperty(Session session) throws RepositoryException {
        this.session = session;
        this.absPath = null;
        AccessControlManager acMgr = session.getAccessControlManager();
        Privilege jcrAll = acMgr.privilegeFromName(Privilege.JCR_ALL);
        privileges.add(jcrAll);
    }

    /**
     * @param session The reading session
     * @param absPath An absolute path to an existing JCR node or {@code null}.
     */
    public JcrSupportedPrivilegesProperty(Session session, String absPath) {
        this.session = session;
        this.absPath = absPath;
    }

    /**
     * Calculated the supported privileges at {@code absPath} and build a
     * {@link org.apache.jackrabbit.webdav.security.SupportedPrivilegeSetProperty}
     * from the result.
     *
     * @return a new {@code SupportedPrivilegeSetProperty} property.
     * @throws RepositoryException
     */
    public SupportedPrivilegeSetProperty asDavProperty() throws RepositoryException {
        if (privileges.isEmpty()) {
            AccessControlManager acMgr = session.getAccessControlManager();
            privileges.addAll(Arrays.asList(acMgr.getSupportedPrivileges(absPath)));
        }
        for (Privilege p : privileges) {
            if (!aggregated.contains(p.getName())) {
                createSupportedPrivilege(p);
            }
        }
        return new SupportedPrivilegeSetProperty(supportedPrivileges.values().toArray(new SupportedPrivilege[supportedPrivileges.size()]));
    }

    private SupportedPrivilege createSupportedPrivilege(Privilege privilege) throws RepositoryException {
        String privilegeName = privilege.getName();

        String localName = Text.getLocalName(privilegeName);
        String prefix = Text.getNamespacePrefix(privilegeName);
        Namespace ns = (prefix.isEmpty()) ? Namespace.EMPTY_NAMESPACE : Namespace.getNamespace(prefix, session.getNamespaceURI(prefix));
        org.apache.jackrabbit.webdav.security.Privilege davPrivilege = org.apache.jackrabbit.webdav.security.Privilege.getPrivilege(localName, ns);

        SupportedPrivilege[] aggregates = (privilege.isAggregate()) ? getDeclaredAggregates(privilege) : null;

        SupportedPrivilege sp = new SupportedPrivilege(davPrivilege, null, null, privilege.isAbstract(), aggregates);
        if (!aggregated.contains(privilegeName)) {
            supportedPrivileges.put(privilegeName, sp);
        }
        return sp;
    }

    private SupportedPrivilege[] getDeclaredAggregates(Privilege privilege) throws RepositoryException {
        List<SupportedPrivilege> declAggr = new ArrayList<SupportedPrivilege>();
        for (Privilege decl : privilege.getDeclaredAggregatePrivileges()) {
            String name = decl.getName();
            if (aggregated.add(name)) {
                if (supportedPrivileges.containsKey(name)) {
                    declAggr.add(supportedPrivileges.remove(name));
                } else {
                    declAggr.add(createSupportedPrivilege(decl));
                }
            }
        }
        return declAggr.toArray(new SupportedPrivilege[declAggr.size()]);
    }
}