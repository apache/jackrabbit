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

import java.io.File;
import java.io.FileInputStream;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.jcr.AccessDeniedException;

import org.apache.jackrabbit.core.ItemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    protected boolean system = false;

    protected boolean anonymous = false;

    //--------------------------------------------------------< AccessManager >

    public void init(AMContext context)
            throws AccessDeniedException, Exception {
        Properties rolemaps = new Properties();
        File rolemap = new File(context.getHomeDir(), "rolemapping.properties");
        log.info("Loading jbossgroup role mappings from {}", rolemap.getPath());
        FileInputStream rolefs = new FileInputStream(rolemap);
        try {
            rolemaps.load(rolefs);
        } finally {
            rolefs.close();
        }

        Iterator iterator = context.getSubject().getPrincipals().iterator();
        while (iterator.hasNext()) {
            Principal principal = (Principal) iterator.next();
            if (principal instanceof Group
                    && principal.getName().equalsIgnoreCase("Roles")) {
                Group group = (Group) principal;
                Enumeration members = group.members();
                while (members.hasMoreElements()) {
                    Principal member = (Principal) members.nextElement();
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
            throws AccessDeniedException {
        if (!isGranted(id, permissions)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public boolean isGranted(ItemId id, int permissions) {
        // system has always all permissions
        // anonymous has all but WRITE & REMOVE premissions
        return system || (anonymous && ((permissions & (WRITE | REMOVE)) == 0));
    }

    public boolean canAccess(String workspaceName) {
        return system || anonymous;
    }

}
