/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.commons.jackrabbit.authorization;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;

/**
 * This class provides common access control related utilities.
 */
public class AccessControlUtils {

    /**
     * Retrieves the {@link Privilege}s from the specified privilege names.
     *
     * @param session The editing session.
     * @param privilegeNames The privilege names.
     * @return An array of privileges.
     * @throws RepositoryException If an error occurs or if {@code privilegeNames}
     * contains an unknown/invalid privilege name.
     */
    public static Privilege[] privilegesFromNames(Session session, String... privilegeNames) throws RepositoryException {
        return privilegesFromNames(session.getAccessControlManager(), privilegeNames);
    }

    /**
     * Retrieves the {@link Privilege}s from the specified privilege names.
     *
     * @param accessControlManager The access control manager.
     * @param privilegeNames The privilege names.
     * @return An array of privileges.
     * @throws RepositoryException If an error occurs or if {@code privilegeNames}
     * contains an unknown/invalid privilege name.
     */
    public static Privilege[] privilegesFromNames(AccessControlManager accessControlManager, String... privilegeNames) throws RepositoryException {
        Set<Privilege> privileges = new HashSet<Privilege>(privilegeNames.length);
        for (String privName : privilegeNames) {
            privileges.add(accessControlManager.privilegeFromName(privName));
        }
        return privileges.toArray(new Privilege[privileges.size()]);
    }

    /**
     * Retrieves the names of the specified privileges.
     *
     * @param privileges One or more privileges.
     * @return The names of the specified privileges.
     */
    public static String[] namesFromPrivileges(Privilege... privileges) {
        if (privileges == null || privileges.length == 0) {
            return new String[0];
        } else {
            String[] names = new String[privileges.length];
            for (int i = 0; i < privileges.length; i++) {
                names[i] = privileges[i].getName();
            }
            return names;
        }
    }

    /**
     * Utility that combines {@link AccessControlManager#getApplicablePolicies(String)}
     * and {@link AccessControlManager#getPolicies(String)} to retrieve
     * a modifiable {@code JackrabbitAccessControlList} for the given path.<br>
     *
     * Note that the policy must be {@link AccessControlManager#setPolicy(String,
     * javax.jcr.security.AccessControlPolicy) reapplied}
     * and the changes must be saved in order to make the AC modifications take
     * effect.
     *
     * @param session The editing session.
     * @param absPath The absolute path of the target node.
     * @return A modifiable access control list or null if there is none.
     * @throws RepositoryException If an error occurs.
     */
    public static JackrabbitAccessControlList getAccessControlList(Session session, String absPath) throws RepositoryException {
        AccessControlManager acMgr = session.getAccessControlManager();

        // try applicable (new) ACLs
        AccessControlPolicyIterator itr = acMgr.getApplicablePolicies(absPath);
        while (itr.hasNext()) {
            AccessControlPolicy policy = itr.nextAccessControlPolicy();
            if (policy instanceof JackrabbitAccessControlList) {
                return (JackrabbitAccessControlList) policy;
            }
        }

        // try if there is an acl that has been set before
        AccessControlPolicy[] pcls = acMgr.getPolicies(absPath);
        for (AccessControlPolicy policy : pcls) {
            if (policy instanceof JackrabbitAccessControlList) {
                return (JackrabbitAccessControlList) policy;
            }
        }

        // no policy found
        return null;
    }

    /**
     * A utility method to add a new access control entry.<br>
     * Please note, that calling {@link javax.jcr.Session#save()()} is required
     * in order to persist the changes.
     *
     * @param session The editing session.
     * @param absPath The absolute path of the target node.
     * @param principal The principal to grant/deny privileges to.
     * @param privilegeNames The names of the privileges to grant or deny.
     * @param isAllow {@code true} to grant; {@code false} otherwise.
     * @return {@code true} if the node's ACL was modified and the session has
     * pending changes.
     * @throws RepositoryException If an error occurs.
     */
    public static boolean addAccessControlEntry(Session session, String absPath,
                                                Principal principal, String[] privilegeNames,
                                                boolean isAllow) throws RepositoryException {
        return addAccessControlEntry(session, absPath, principal, privilegesFromNames(session, privilegeNames), isAllow);
    }

    /**
     * A utility method to add a new access control entry. Please note, that
     * a call to {@link javax.jcr.Session#save()()} is required in order
     * to persist the changes.
     *
     * @param session The editing session
     * @param absPath The absolute path of the target node.
     * @param principal The principal to grant/deny privileges to.
     * @param privileges The privileges to grant or deny
     * @param isAllow {@code true} to grant; {@code false} otherwise;
     * @return {@code true} if the node's ACL was modified and the session has
     * pending changes.
     * @throws RepositoryException If an error occurs.
     */
    public static boolean addAccessControlEntry(Session session, String absPath,
                                                Principal principal, Privilege[] privileges,
                                                boolean isAllow) throws RepositoryException {
        JackrabbitAccessControlList acl = getAccessControlList(session, absPath);
        if (acl != null) {
            if (acl.addEntry(principal, privileges, isAllow)) {
                session.getAccessControlManager().setPolicy(absPath, acl);
                return true;
            } // else: not modified
        } // else: no acl found.

        return false;
    }

    /**
     * Utility to grant jcr:all privilege to the everyone group principal.
     * Please note, that {@link javax.jcr.Session#save()()} is required in order
     * to persist the changes.
     *
     * @param session The editing session.
     * @param absPath The absolute path of the target node
     * @return {@code true} if the node's access control list was modified;
     * {@code false} otherwise;
     * @throws RepositoryException If an error occurs.
     */
    public static boolean grantAllToEveryone(Session session, String absPath) throws RepositoryException {
        Principal everyone = getEveryonePrincipal(session);
        Privilege[] privileges = privilegesFromNames(session, Privilege.JCR_ALL);
        return addAccessControlEntry(session, absPath, everyone, privileges, true);
    }

    /**
     * Utility to deny jcr:all privilege to the everyone group principal.
     * Please note, that {@link javax.jcr.Session#save()()} is required in order
     * to persist the changes.
     *
     * @param session The editing session.
     * @param absPath The absolute path of the target node
     * @return {@code true} if the node's access control list was modified;
     * {@code false} otherwise;
     * @throws RepositoryException If an error occurs.
     */
    public static boolean denyAllToEveryone(Session session, String absPath) throws RepositoryException {
        Principal everyone = getEveryonePrincipal(session);
        Privilege[] privileges = privilegesFromNames(session, Privilege.JCR_ALL);
        return addAccessControlEntry(session, absPath, everyone, privileges, false);
    }

    private static Principal getEveryonePrincipal(Session session) throws RepositoryException {
        if (session instanceof JackrabbitSession) {
            return ((JackrabbitSession) session).getPrincipalManager().getEveryone();
        } else {
            throw new UnsupportedOperationException("Failed to retrieve everyone principal: JackrabbitSession expected.");
        }
    }
}

