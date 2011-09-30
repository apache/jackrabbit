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

import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.commons.iterator.AccessControlPolicyIteratorAdapter;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import java.security.Principal;

/**
 * <code>AbstractAccessControlManager</code>...
 */
public abstract class AbstractAccessControlManager implements JackrabbitAccessControlManager {

    private static Logger log = LoggerFactory.getLogger(AbstractAccessControlManager.class);

    /**
     * Always returns all registered <code>Privilege</code>s.
     *
     * @param absPath Path to an existing node.
     * @return Always returns all registered <code>Privilege</code>s.
     * @see javax.jcr.security.AccessControlManager#getSupportedPrivileges(String)
     */
    public Privilege[] getSupportedPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        checkValidNodePath(absPath);

        // return all known privileges everywhere.
        return getPrivilegeManager().getRegisteredPrivileges();
    }

    /**
     * @see javax.jcr.security.AccessControlManager#privilegeFromName(String)
     */
    public Privilege privilegeFromName(String privilegeName)
            throws AccessControlException, RepositoryException {
        checkInitialized();

        return getPrivilegeManager().getPrivilege(privilegeName);
    }

    /**
     * Returns <code>null</code>.
     *
     * @param absPath Path to an existing node.
     * @return always returns <code>null</code>.
     * @see javax.jcr.security.AccessControlManager#getApplicablePolicies(String)
     */
    public AccessControlPolicy[] getPolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPermission(absPath, Permission.READ_AC);

        log.debug("Implementation does not provide applicable policies -> getPolicy() always returns an empty array.");
        return new AccessControlPolicy[0];
    }

    /**
     * Returns an empty iterator.
     *
     * @param absPath Path to an existing node.
     * @return always returns an empty iterator.
     * @see javax.jcr.security.AccessControlManager#getApplicablePolicies(String)
     */
    public AccessControlPolicyIterator getApplicablePolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPermission(absPath, Permission.READ_AC);

        log.debug("Implementation does not provide applicable policies -> returning empty iterator.");
        return AccessControlPolicyIteratorAdapter.EMPTY;
    }

    /**
     * Always throws <code>AccessControlException</code>
     *
     * @see javax.jcr.security.AccessControlManager#setPolicy(String, AccessControlPolicy)
     */
    public void setPolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPermission(absPath, Permission.MODIFY_AC);

        throw new AccessControlException("AccessControlPolicy " + policy + " cannot be applied.");
    }

    /**
     * Always throws <code>AccessControlException</code>
     *
     * @see javax.jcr.security.AccessControlManager#removePolicy(String, AccessControlPolicy)
     */
    public void removePolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        checkPermission(absPath, Permission.MODIFY_AC);

        throw new AccessControlException("No AccessControlPolicy has been set through this API -> Cannot be removed.");
    }


    //-------------------------------------< JackrabbitAccessControlManager >---
    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#getApplicablePolicies(java.security.Principal)
     */
    public JackrabbitAccessControlPolicy[] getApplicablePolicies(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        checkInitialized();

        log.debug("Implementation does not provide applicable policies -> returning empty array.");
        return new JackrabbitAccessControlPolicy[0];
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlManager#getPolicies(java.security.Principal)
     */
    public JackrabbitAccessControlPolicy[] getPolicies(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        checkInitialized();

        log.debug("Implementation does not provide applicable policies -> returning empty array.");
        return new JackrabbitAccessControlPolicy[0];
    }
    
    //--------------------------------------------------------------------------
    /**
     * Check if this manager has been properly initialized.
     *
     * @throws IllegalStateException If this manager has not been properly initialized.
     */
    protected abstract void checkInitialized() throws IllegalStateException;

    /**
     * Check if the specified privileges are granted at <code>absPath</code>.
     *
     * @param absPath Path to an existing node.
     * @param permission Permissions to be checked.
     * @throws AccessDeniedException if the session does not have the
     * specified privileges.
     * @throws PathNotFoundException if no node exists at <code>absPath</code>
     * of if the session does not have the permission to READ it.
     * @throws RepositoryException If another error occurs.
     */
    protected abstract void checkPermission(String absPath, int permission) throws AccessDeniedException, PathNotFoundException, RepositoryException;

    /**
     * @return the privilege manager
     * @throws RepositoryException If another error occurs.
     */
    protected abstract PrivilegeManager getPrivilegeManager() throws RepositoryException;

    /**
     * Tests if the given <code>absPath</code> is absolute and points to an existing node.
     *
     * @param absPath Path to an existing node.
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     * or the session does not have privilege to retrieve the node.
     * @throws RepositoryException If the given <code>absPath</code> is not
     * absolute or if some other error occurs.
     */
    protected abstract void checkValidNodePath(String absPath) throws PathNotFoundException, RepositoryException;

}
