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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.core.security.jsr283.security.AccessControlEntry;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.ObservationManager;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.security.Principal;

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

    protected static boolean isAdminOrSystem(Set principals) {
        for (Iterator it = principals.iterator(); it.hasNext();) {
            Principal p = (Principal) it.next();
            if (p instanceof AdminPrincipal || p instanceof SystemPrincipal) {
                return true;
            }
        }
        return false;
    }

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
        };
    }

    protected static CompiledPermissions getReadOnlyPermissions() {
        return new CompiledPermissions() {
            public void close() {
                //nop
            }
            public boolean grants(Path absPath, int permissions) throws RepositoryException {
                return permissions == Permission.READ;
            }
            public int getPrivileges(Path absPath) throws RepositoryException {
                return PrivilegeRegistry.READ;
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
     * @see AccessControlProvider#getPolicy(NodeId)
     */
    public AccessControlPolicy getPolicy(NodeId nodeId) throws ItemNotFoundException, RepositoryException {
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
     * @see AccessControlProvider#getAccessControlEntries(NodeId)
     */
    public AccessControlEntry[] getAccessControlEntries(NodeId nodeId) throws RepositoryException {
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