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
package org.apache.jackrabbit.core.security;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileInputStream;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.Properties;

/**
 * The simple JBoss access manager is a specialized Access Manager to
 * handle Authorization of individuals authenticated through JBoss
 * login modules. It maps roles from the JBoss simplegroup class to
 * Jackrabbit permissions.
 *
 * @author dhartford
 * @date 2006-11-27
 * @see http://wiki.apache.org/jackrabbit/SimpleJbossAccessManager
 */
public class SimpleJBossAccessManager implements AccessManager {

    /**
     * Logger instance.
     */
    private static Logger log =
        LoggerFactory.getLogger(SimpleJBossAccessManager.class);

    protected boolean system;

    protected boolean anonymous;

    //--------------------------------------------------------< AccessManager >

    public void init(AMContext context)
            throws AccessDeniedException, Exception {
        init(context, null, null);
    }

    public void init(AMContext context, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessMgr) throws AccessDeniedException, Exception {
        Properties rolemaps = new Properties();
        File rolemap = new File(context.getHomeDir(), "rolemapping.properties");
        log.info("Loading jbossgroup role mappings from {}", rolemap.getPath());
        FileInputStream rolefs = new FileInputStream(rolemap);
        try {
            rolemaps.load(rolefs);
        } finally {
            rolefs.close();
        }

        for (Principal principal : context.getSubject().getPrincipals()) {
            if (principal instanceof Group
                    && principal.getName().equalsIgnoreCase("Roles")) {
                Group group = (Group) principal;
                Enumeration< ? extends Principal> members = group.members();
                while (members.hasMoreElements()) {
                    Principal member = members.nextElement();
                    String role = rolemaps.getProperty(member.getName());
                    system = system || "full".equalsIgnoreCase(role);
                    anonymous = anonymous || "read".equalsIgnoreCase(role);
                }
            }
        }

        // @todo check permission to access given workspace based on principals
    }

    public synchronized void close() {
    }

    public void checkPermission(ItemId id, int permissions)
            throws AccessDeniedException, RepositoryException {
        if (!isGranted(id, permissions)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public void checkPermission(Path absPath, int permissions) throws AccessDeniedException, RepositoryException {
        if (!isGranted(absPath, permissions)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public boolean isGranted(ItemId id, int permissions) throws RepositoryException {
        // system has always all permissions
        // anonymous has only READ permissions
        return system || (anonymous && ((permissions & (WRITE | REMOVE)) == 0));
    }

    public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
        return internalIsGranted(permissions);
    }

    public boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException {
        return internalIsGranted(permissions);
    }

    public boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException {
        return true;
    }

    public boolean canAccess(String workspaceName) throws RepositoryException {
        return system || anonymous;
    }

    private boolean internalIsGranted(int permissions) {
        /* system has always all permissions,
           anonymous has only READ permissions */
        return system || (anonymous && Permission.READ == permissions);
    }
}
