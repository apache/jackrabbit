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
import java.util.List;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.security.CurrentUserPrivilegeSetProperty;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.xml.Namespace;

/**
 * JcrPrivilegesProperty...
 */
public class JcrUserPrivilegesProperty {

    private final Session session;
    private final String absPath;

    /**
     * @param session The reading session
     * @param absPath An absolute path to an existing JCR node or {@code null}.
     */
    public JcrUserPrivilegesProperty(Session session, String absPath) throws RepositoryException {
        this.session = session;
        this.absPath = absPath;
    }

    public CurrentUserPrivilegeSetProperty asDavProperty() throws RepositoryException {
        List<Privilege> davPrivs = new ArrayList<Privilege>();
        for (javax.jcr.security.Privilege privilege : session.getAccessControlManager().getPrivileges(absPath)) {
            String privilegeName = privilege.getName();

            String prefix = Text.getNamespacePrefix(privilegeName);
            Namespace ns = (prefix.isEmpty()) ? Namespace.EMPTY_NAMESPACE : Namespace.getNamespace(prefix, session.getNamespaceURI(prefix));
            davPrivs.add(Privilege.getPrivilege(Text.getLocalName(privilegeName), ns));
        }

        return new CurrentUserPrivilegeSetProperty(davPrivs.toArray(new Privilege[davPrivs.size()]));
    }
}