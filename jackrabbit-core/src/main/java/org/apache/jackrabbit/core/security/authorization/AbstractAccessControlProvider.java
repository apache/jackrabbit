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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.api.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <code>AbstractAccessControlProvider</code>...
 */
public abstract class AbstractAccessControlProvider implements AccessControlProvider {

    private static Logger log = LoggerFactory.getLogger(AbstractAccessControlProvider.class);

    private final String policyName;
    private final String policyDesc;

    /**
     * Returns the system session this provider has been created for.
     */
    protected SessionImpl session;
    protected ObservationManager observationMgr;
    protected NamePathResolver resolver;

    private boolean initialized;
    private Principal everyone;

    protected AbstractAccessControlProvider() {
        this(AbstractAccessControlProvider.class.getName() + ": default Policy", null);
    }

    protected AbstractAccessControlProvider(String defaultPolicyName, String defaultPolicyDesc) {
        policyName = defaultPolicyName;
        policyDesc = defaultPolicyDesc;
    }

    /**
     *
     */
    protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized or already closed.");
        }
    }

    /**
     * Simple test if the specified path points to an item that defines AC
     * information.
     * 
     * @param absPath
     * @return
     */
    protected abstract boolean isAcItem(Path absPath) throws RepositoryException;

    /**
     *
     * @param principals
     * @return
     */
    protected static boolean isAdminOrSystem(Set principals) {
        for (Iterator it = principals.iterator(); it.hasNext();) {
            Principal p = (Principal) it.next();
            if (p instanceof AdminPrincipal || p instanceof SystemPrincipal) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return
     */
    protected static CompiledPermissions getAdminPermissions() {
        return new CompiledPermissions() {
            public void close() {
                //nop
            }
            public boolean grants(Path absPath, int permissions) throws RepositoryException {
                return true;
            }
            public int getPrivileges(Path absPath) throws RepositoryException {
                return PrivilegeRegistry.ALL;
            }
            public boolean canReadAll() throws RepositoryException {
                return true;
            }
        };
    }

    /**
     * Simple implementation to determine if the given set of principals
     * only will result in read-only access.
     *
     * @param principals
     * @return true if the given set only contains the everyone group.
     */
    protected boolean isReadOnly(Set principals) {
        // TODO: improve. need to detect if 'anonymous' is included.
        return principals.size() == 1 && principals.contains(everyone);
    }

    /**
     *
     * @return
     */
    protected CompiledPermissions getReadOnlyPermissions() {
        return new CompiledPermissions() {
            public void close() {
                //nop
            }
            public boolean grants(Path absPath, int permissions) throws RepositoryException {
                if (isAcItem(absPath)) {
                    // read-only never has read-AC permission
                    return false;
                } else {
                    return permissions == Permission.READ;
                }
            }
            public int getPrivileges(Path absPath) throws RepositoryException {
                if (isAcItem(absPath)) {
                    return PrivilegeRegistry.NO_PRIVILEGE;
                } else {
                    return PrivilegeRegistry.READ;
                }
            }
            public boolean canReadAll() throws RepositoryException {
                return false;
            }
        };
    }

    //----------------------------------------------< AccessControlProvider >---
    /**
     * Tests if the given <code>systemSession</code> is a SessionImpl and
     * retrieves the observation manager. The it sets the internal 'initialized'
     * field to true.
     *
     * @throws RepositoryException If the specified session is not a
     * <code>SessionImpl</code> or if retrieving the observation manager fails.
     * @see AccessControlProvider#init(Session, Map)
     */
    public void init(Session systemSession, Map options) throws RepositoryException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        if (!(systemSession instanceof SessionImpl)) {
            throw new RepositoryException("SessionImpl (system session) expected.");
        }
        session = (SessionImpl) systemSession;
        observationMgr = systemSession.getWorkspace().getObservationManager();
        resolver = (SessionImpl) systemSession;

        everyone = session.getPrincipalManager().getEveryone();

        initialized = true;
    }

    /**
     * @see AccessControlProvider#close()
     */
    public void close() {
        checkInitialized();
        initialized = false;
    }

    /**
     * @see AccessControlProvider#getPolicy(Path)
     * @param absPath
     */
    public AccessControlPolicy getPolicy(Path absPath) throws ItemNotFoundException, RepositoryException {
        checkInitialized();
        return new AccessControlPolicy() {
            public String getName() throws RepositoryException {
                return policyName;
            }
            public String getDescription() throws RepositoryException {
                return policyDesc;
            }
        };
    }

    /**
     * @see AccessControlProvider#getAccessControlEntries(Path)
     * @param absPath
     */
    public AccessControlEntry[] getAccessControlEntries(Path absPath) throws RepositoryException {
        checkInitialized();
        // always empty array, since aces will never be changed using the api.
        return new AccessControlEntry[0];
    }

    /**
     * @see AccessControlProvider#getEditor(Session)
     */
    public AccessControlEditor getEditor(Session session) {
        checkInitialized();
        // not editable at all: policy is always the default and cannot be
        // changed using the JCR API.
        return null;
    }
}