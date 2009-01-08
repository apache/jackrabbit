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

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public abstract class AbstractAccessControlProvider implements AccessControlProvider, AccessControlUtils {

    private static Logger log = LoggerFactory.getLogger(AbstractAccessControlProvider.class);

    public static final String PARAM_OMIT_DEFAULT_PERMISSIONS = "omit-default-permission";

    /**
     * the system session this provider has been created for.
     */
    protected SessionImpl session;
    protected ObservationManager observationMgr;
    protected NamePathResolver resolver;

    protected int privAll;
    protected int privRead;

    private boolean initialized;

    protected AbstractAccessControlProvider() {
    }

    /**
     * Throws <code>IllegalStateException</code> if the provider has not
     * been initialized or has been closed.
     */
    protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized or already closed.");
        }
    }

    /**
     * Returns compiled permissions for the administrator i.e. permissions
     * that grants everything and returns the int representation of {@link Privilege#JCR_ALL}
     * upon {@link CompiledPermissions#getPrivileges(Path)} for all
     * paths.
     *
     * @return an implementation of <code>CompiledPermissions</code> that
     * grants everything and always returns the int representation of
     * {@link Privilege#JCR_ALL} upon {@link CompiledPermissions#getPrivileges(Path)}.
     */
    protected CompiledPermissions getAdminPermissions() {
        return new CompiledPermissions() {
            public void close() {
                //nop
            }
            public boolean grants(Path absPath, int permissions) {
                return true;
            }
            public int getPrivileges(Path absPath) {
                return privAll;
            }
            public boolean canReadAll() {
                return true;
            }
        };
    }

    /**
     * Returns compiled permissions for a read-only user i.e. permissions
     * that grants READ permission for all non-AC items.
     *
     * @return an implementation of <code>CompiledPermissions</code> that
     * grants READ permission for all non-AC items.
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
                    return privRead;
                }
            }
            public boolean canReadAll() {
                return false;
            }
        };
    }

    //-------------------------------------------------< AccessControlUtils >---
    /**
     * @see AccessControlUtils#isAdminOrSystem(Set)
     */
    public boolean isAdminOrSystem(Set principals) {
        for (Iterator it = principals.iterator(); it.hasNext();) {
            Principal p = (Principal) it.next();
            if (p instanceof AdminPrincipal || p instanceof SystemPrincipal) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see AccessControlUtils#isReadOnly(Set)
     */
    public boolean isReadOnly(Set principals) {
        // TODO: find ways to determine read-only status
        return false;
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
    public void init(Session systemSession, Map configuration) throws RepositoryException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        if (!(systemSession instanceof SessionImpl)) {
            throw new RepositoryException("SessionImpl (system session) expected.");
        }
        session = (SessionImpl) systemSession;
        observationMgr = systemSession.getWorkspace().getObservationManager();
        resolver = (SessionImpl) systemSession;

        privAll = PrivilegeRegistry.getBits(new Privilege[] {session.getAccessControlManager().privilegeFromName(Privilege.JCR_ALL)});
        privRead = PrivilegeRegistry.getBits(new Privilege[] {session.getAccessControlManager().privilegeFromName(Privilege.JCR_READ)});

        initialized = true;
    }

    /**
     * @see AccessControlProvider#close()
     */
    public void close() {
        checkInitialized();
        initialized = false;
    }
}