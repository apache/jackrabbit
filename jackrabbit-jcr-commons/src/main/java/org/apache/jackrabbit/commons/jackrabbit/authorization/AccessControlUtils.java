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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;

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
        return getAccessControlList(acMgr, absPath);
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
     * @param accessControlManager The {@code AccessControlManager} .
     * @param absPath The absolute path of the target node.
     * @return A modifiable access control list or null if there is none.
     * @throws RepositoryException If an error occurs.
     */
    public static JackrabbitAccessControlList getAccessControlList(AccessControlManager accessControlManager, String absPath) throws RepositoryException {
        // try applicable (new) ACLs
        AccessControlPolicyIterator itr = accessControlManager.getApplicablePolicies(absPath);
        while (itr.hasNext()) {
            AccessControlPolicy policy = itr.nextAccessControlPolicy();
            if (policy instanceof JackrabbitAccessControlList) {
                return (JackrabbitAccessControlList) policy;
            }
        }

        // try if there is an acl that has been set before
        AccessControlPolicy[] pcls = accessControlManager.getPolicies(absPath);
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
     * Please note, that calling {@link javax.jcr.Session#save()} is required
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
     * a call to {@link javax.jcr.Session#save()} is required in order
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
     * Please note, that {@link javax.jcr.Session#save()} is required in order
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
     * Please note, that {@link javax.jcr.Session#save()} is required in order
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

    /**
     * <b>Allow</b> certain privileges on a given node for a given principal.
     *
     * <p>To activate the ACL change, session.save() must be called.</p>
     *
     * @param node node to set the resource-based ACL entry on; underlying session is used to write the ACL
     * @param principalName Name of the principal for which the ACL entry should apply
     * @param privileges list of privileges to set by name (see {@link javax.jcr.security.Privilege})
     * @return {@code true} if the node's ACL was modified and the session has pending changes.
     * @throws RepositoryException If an unexpected repository error occurs
     */
    public static boolean allow(Node node, String principalName, String... privileges) throws RepositoryException {
        return addAccessControlEntry(
            node.getSession(),
            node.getPath(),
            getPrincipal(node.getSession(), principalName),
            privileges,
            true // allow
        );
    }

    /**
     * <b>Deny</b> certain privileges on a node for a given principal.
     *
     * <p>To activate the ACL change, session.save() must be called.</p>
     *
     * @param node node to set the resource-based ACL entry on; underlying session is used to write the ACL
     * @param principalName Name of the principal for which the ACL entry should apply
     * @param privileges list of privileges to set by name (see {@link javax.jcr.security.Privilege})
     * @return {@code true} if the node's ACL was modified and the session has pending changes.
     * @throws RepositoryException If an unexpected repository error occurs
     */
    public static boolean deny(Node node, String principalName, String... privileges) throws RepositoryException {
        return addAccessControlEntry(
            node.getSession(),
            node.getPath(),
            getPrincipal(node.getSession(), principalName),
            privileges,
            false // deny
        );
    }

    /**
     * Removes all ACL entries for a principal at a given absolute path. If the specified
     * {@code principalName} is {@code null} the policy will be removed altogether.
     * <p>Modifications only take effect upon {@code Session.save()}.</p>
     *
     * @param session The editing session.
     * @param absPath Absolute path of an existing node from which to remove ACL entries (or the policy)
     * @param principalName Name of the principal whose entries should be removed;
     * use {@code null} to clear the policy.
     * @return {@code true} if the policy has been modified; {@code false} otherwise.
     * @throws RepositoryException If an unexpected repository error occurs
     */
    public static boolean clear(Session session, String absPath, String principalName) throws RepositoryException {
        AccessControlManager acm = session.getAccessControlManager();
        JackrabbitAccessControlList acl = null;
        // only clear if there is an existing acl (no need to retrieve applicable policies)
        AccessControlPolicy[] pcls = acm.getPolicies(absPath);
        for (AccessControlPolicy policy : pcls) {
            if (policy instanceof JackrabbitAccessControlList) {
                acl = (JackrabbitAccessControlList) policy;
            }
        }
        if (acl != null) {
            if (principalName == null) {
                acm.removePolicy(absPath, acl);
                return true;
            } else {
                Principal principal = getPrincipal(session, principalName);
                if (principal == null) {
                    return false;
                }
                boolean removedEntries = false;
                // remove all existing entries for principal
                for (AccessControlEntry ace : acl.getAccessControlEntries()) {
                    if (ace.getPrincipal().equals(principal)) {
                        acl.removeAccessControlEntry(ace);
                        removedEntries = true;
                    }
                }
                if (removedEntries) {
                    acm.setPolicy(absPath, acl);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes all ACL entries for a principal on a given node.
     *
     * <p>Modification to the policy only take effect upon {@code Session.save()} must be called.</p>
     *
     * @param node node from which to remove ACL entries; underlying session is used to write the changes
     * @param principalName Name of the principal whose entries should be removed; use {@code null} to clear the policy altogether.
     * @return {@code true} if the node's ACL was modified, {@code false} otherwise.
     * @throws RepositoryException If an unexpected repository error occurs
     */
    public static boolean clear(Node node, String principalName) throws RepositoryException {
        return clear(node.getSession(), node.getPath(), principalName);
    }

    /**
     * Removes the access control list at a given node.
     * <p>To persist the modifications, {@code Session.save()} must be called.</p>
     *
     * @param node node from which to remove the ACL; underlying session is used to write the changes
     * @return {@code true} if the node's ACL was removed, {@code false} otherwise.
     * @throws RepositoryException If an unexpected repository error occurs
     */
    public static boolean clear(Node node) throws RepositoryException {
        return clear(node, null);
    }

    /**
     * Removes the access control list at the specified absolute path.
     * <p>To persist the modification, session.save() must be called.</p>
     *
     * @param session The editing session.
     * @param absPath An absolute path of a valid node accessible to the editing session from which to remove the ACL.
     * @return {@code true} if the node's ACL got removed, {@code false} otherwise.
     * @throws RepositoryException If an unexpected repository error occurs
     */
    public static boolean clear(Session session, String absPath) throws RepositoryException {
        return clear(session, absPath, null);
    }

    /**
     * Retrieves the principal with the specified {@code principalName}. Shortcut
     * for calling {@link PrincipalManager#getPrincipal(String)}.
     *
     * @param session The editing session which must be a {@code JackrabbitSession}.
     * @param principalName The name of the principal.
     * @return The principal with the specified name or {@code null} if no such principal exists.
     * @throws RepositoryException If an error occurs or if the session is not a {@code JackrabbitSession}.
     */
    public static Principal getPrincipal(Session session, String principalName) throws RepositoryException {
        if (session instanceof JackrabbitSession) {
            return ((JackrabbitSession) session).getPrincipalManager().getPrincipal(principalName);
        } else {
            throw new UnsupportedOperationException("Failed to retrieve principal: JackrabbitSession expected.");
        }
    }

    /**
     * Shortcut for calling {@link PrincipalManager#getEveryone()}.
     *
     * @param session The editing session which must be a {@code JackrabbitSession}.
     * @return The group principal presenting everyone.
     * @throws RepositoryException If an error occurs or if the session is not a {@code JackrabbitSession}.
     */
    public static Principal getEveryonePrincipal(Session session) throws RepositoryException {
        if (session instanceof JackrabbitSession) {
            return ((JackrabbitSession) session).getPrincipalManager().getEveryone();
        } else {
            throw new UnsupportedOperationException("Failed to retrieve everyone principal: JackrabbitSession expected.");
        }
    }
}

