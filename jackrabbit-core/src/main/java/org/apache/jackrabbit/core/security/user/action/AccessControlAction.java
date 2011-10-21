/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.apache.jackrabbit.core.security.user.action;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.core.security.principal.UnknownPrincipal;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>AccessControlAction</code>
 */
public class AccessControlAction implements AuthorizableAction {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(AccessControlAction.class);

    private String[] groupPrivilegeNames = new String[0];
    private String[] userPrivilegeNames = new String[0];

    /**
     * Create a new instance.
     */
    public AccessControlAction() {}

    //-------------------------------------------------< AuthorizableAction >---
    /**
     * @see AuthorizableAction#onCreate(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session)
     */
    public void onCreate(Authorizable authorizable, Session session) throws RepositoryException {
        Node aNode;
        String path = authorizable.getPath();

        JackrabbitAccessControlList acl = null;
        AccessControlManager acMgr = session.getAccessControlManager();
        for (AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path); it.hasNext();) {
            AccessControlPolicy plc = it.nextAccessControlPolicy();
            if (plc instanceof JackrabbitAccessControlList) {
                acl = (JackrabbitAccessControlList) plc;
                break;
            }
        }

        if (acl == null) {
            log.warn("Cannot process AccessControlAction: no applicable ACL at " + path);
        } else {
            // setup acl according to configuration.
            Principal principal = new UnknownPrincipal(authorizable.getPrincipal().getName());
            boolean modified = false;
            if (authorizable.isGroup()) {
                // new authorizable is a Group
                if (groupPrivilegeNames.length > 0) {
                    modified = acl.addAccessControlEntry(principal, getPrivileges(groupPrivilegeNames, acMgr));
                }
            } else {
                // new authorizable is a User
                if (userPrivilegeNames.length > 0) {
                    modified = acl.addAccessControlEntry(principal, getPrivileges(userPrivilegeNames, acMgr));
                }
            }
            if (modified) {
                acMgr.setPolicy(path, acl);
            }
        }
    }

    /**
     * @see AuthorizableAction#onRemove(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session)
     */
    public void onRemove(Authorizable authorizable, Session session) throws RepositoryException {
        // nothing to do.
    }

    //--------------------------------------------------------< Bean Config >---

    public void setGroupPrivilegeNames(String privilegeNames) {
        if (privilegeNames != null && privilegeNames.length() > 0) {
            groupPrivilegeNames = split(privilegeNames);
        }

    }

    public void setUserPrivilegeNames(String privilegeNames) {
        if (privilegeNames != null && privilegeNames.length() > 0) {
            userPrivilegeNames = split(privilegeNames);
        }
    }

    //------------------------------------------------------------< private >---
    /**
     * Retrieve privileges for the specified privilege names.
     *
     * @param privNames
     * @param acMgr
     * @return Array of <code>Privilege</code>
     * @throws javax.jcr.RepositoryException If a privilege name cannot be
     * resolved to a valid privilege.
     */
    private static Privilege[] getPrivileges(String[] privNames, AccessControlManager acMgr) throws RepositoryException {
        if (privNames == null || privNames.length == 0) {
            return new Privilege[0];
        }
        Privilege[] privileges = new Privilege[privNames.length];
        for (int i = 0; i < privNames.length; i++) {
            privileges[i] = acMgr.privilegeFromName(privNames[i]);
        }
        return privileges;
    }

    /**
     *
     * @param configParam
     * @return
     */
    private static String[] split(String configParam) {
        List<String> nameList = new ArrayList<String>();
        for (String pn : Text.explode(configParam, ',', false)) {
            String privName = pn.trim();
            if (privName.length()  > 0) {
                nameList.add(privName);
            }
        }
        return nameList.toArray(new String[nameList.size()]);
    }
}