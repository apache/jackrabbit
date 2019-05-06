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
package org.apache.jackrabbit.core.security.simple;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.config.SecurityConfig;
import org.apache.jackrabbit.core.config.SecurityManagerConfig;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.JackrabbitSecurityManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.security.authentication.AuthContextProvider;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.core.security.principal.GroupPrincipals;
import org.apache.jackrabbit.core.security.principal.PrincipalIteratorAdapter;
import org.apache.jackrabbit.core.security.principal.PrincipalManagerImpl;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.apache.jackrabbit.core.security.principal.ProviderRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>SimpleSecurityManager</code>: simple implementation ignoring both
 * configuration entries for 'principalProvider' and for 'workspaceAccessManager'.
 * The AccessManager is initialized using
 * {@link AccessManager#init(org.apache.jackrabbit.core.security.AMContext)}.
 */
public class SimpleSecurityManager implements JackrabbitSecurityManager {

    private static Logger log = LoggerFactory.getLogger(SimpleSecurityManager.class);

    private boolean initialized;

    private SecurityConfig config;

    /**
     * session on the system workspace.
     */
    private Session systemSession;

    /**
     * the principal provider registry
     */
    private PrincipalProviderRegistry principalProviderRegistry;

    /**
     * The workspace access manager
     */
    private WorkspaceAccessManager workspaceAccessManager;

    /**
     * factory for login-context {@see Repository#login())
     */
    private AuthContextProvider authCtxProvider;

    private String adminID;
    private String anonymID;

    /**
     * Always returns <code>null</code>. AccessControlProvider configuration
     * is ignored with this security manager. Subclasses may overwrite this
     * lazy behavior that originates from the <code>SimpleAccessManager</code>.
     *
     * @param systemSession The system session used to init the security manager.
     * @param workspaceName The name of the workspace for which the provider
     * should be retrieved.
     * @return Always returns <code>null</code>.
     */
    protected AccessControlProvider getAccessControlProvider(Session systemSession, String workspaceName) {
        return null;
    }

    //------------------------------------------< JackrabbitSecurityManager >---
    /**
     * @see JackrabbitSecurityManager#init(Repository, Session)
     */
    public void init(Repository repository, Session systemSession) throws RepositoryException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        if (!(repository instanceof RepositoryImpl)) {
            throw new RepositoryException("RepositoryImpl expected");
        }

        this.systemSession = systemSession;
        config = ((RepositoryImpl) repository).getConfig().getSecurityConfig();

        // read the LoginModule configuration
        LoginModuleConfig loginModConf = config.getLoginModuleConfig();
        authCtxProvider = new AuthContextProvider(config.getAppName(), loginModConf);
        if (authCtxProvider.isLocal()) {
            log.info("init: using Repository LoginModule configuration for " + config.getAppName());
        } else if (authCtxProvider.isJAAS()) {
            log.info("init: using JAAS LoginModule configuration for " + config.getAppName());
        } else {
            String msg = "No valid LoginModule configuriation for " + config.getAppName();
            log.error(msg);
            throw new RepositoryException(msg);
        }

        Properties[] moduleConfig = authCtxProvider.getModuleConfig();

        // retrieve default-ids (admin and anonymous) from login-module-configuration.
        for (Properties aModuleConfig1 : moduleConfig) {
            if (aModuleConfig1.containsKey(LoginModuleConfig.PARAM_ADMIN_ID)) {
                adminID = aModuleConfig1.getProperty(LoginModuleConfig.PARAM_ADMIN_ID);
            }
            if (aModuleConfig1.containsKey(LoginModuleConfig.PARAM_ANONYMOUS_ID)) {
                anonymID = aModuleConfig1.getProperty(LoginModuleConfig.PARAM_ANONYMOUS_ID);
            }
        }
        // fallback:
        if (adminID == null) {
            log.debug("No adminID defined in LoginModule/JAAS config -> using default.");
            adminID = SecurityConstants.ADMIN_ID;
        }
        if (anonymID == null) {
            log.debug("No anonymousID defined in LoginModule/JAAS config -> using default.");
            anonymID = SecurityConstants.ANONYMOUS_ID;
        }

        // most simple principal provider registry, that does not read anything
        // from configuration
        PrincipalProvider principalProvider = new SimplePrincipalProvider();
        // skip init of provider (nop)
        principalProviderRegistry = new ProviderRegistryImpl(principalProvider);
        // register all configured principal providers.
        for (Properties aModuleConfig : moduleConfig) {
            principalProviderRegistry.registerProvider(aModuleConfig);
        }

        SecurityManagerConfig smc = config.getSecurityManagerConfig();
        if (smc != null && smc.getWorkspaceAccessConfig() != null) {
            workspaceAccessManager =
                smc.getWorkspaceAccessConfig().newInstance(WorkspaceAccessManager.class);
        } else {
            // fallback -> the default simple implementation
            log.debug("No WorkspaceAccessManager configured; using default.");
            workspaceAccessManager = new SimpleWorkspaceAccessManager();
        }
        workspaceAccessManager.init(systemSession);

        initialized = true;
    }

    /**
     * @see JackrabbitSecurityManager#dispose(String)
     */
    public void dispose(String workspaceName) {
        checkInitialized();
        // nop
    }

    /**
     * @see JackrabbitSecurityManager#close()
     */
    public void close() {
        checkInitialized();
    }

    /**
     * @see JackrabbitSecurityManager#getAccessManager(Session,AMContext)
     */
    public AccessManager getAccessManager(Session session, AMContext amContext) throws RepositoryException {
        checkInitialized();
        try {
            String wspName = session.getWorkspace().getName();
            AccessControlProvider acP = getAccessControlProvider(systemSession, wspName);

            AccessManagerConfig amc = config.getAccessManagerConfig();
            AccessManager accessMgr;
            if (amc == null) {
                accessMgr = new SimpleAccessManager();
            } else {
                accessMgr = amc.newInstance(AccessManager.class);
            }
            accessMgr.init(amContext, acP, workspaceAccessManager);
            return accessMgr;
        } catch (AccessDeniedException ade) {
            // re-throw
            throw ade;
        } catch (Exception e) {
            // wrap in RepositoryException
            String msg = "failed to instantiate AccessManager implementation: " + SimpleAccessManager.class.getName();
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see JackrabbitSecurityManager#getPrincipalManager(Session)
     */
    public synchronized PrincipalManager getPrincipalManager(Session session)
            throws RepositoryException {
        checkInitialized();
        if (session instanceof SessionImpl) {
            SessionImpl sImpl = ((SessionImpl)session);
            return new PrincipalManagerImpl(sImpl, principalProviderRegistry.getProviders());
        } else {
            throw new RepositoryException("Internal error: SessionImpl expected.");
        }
    }

    /**
     * @see JackrabbitSecurityManager#getUserManager(Session)
     */
    public UserManager getUserManager(Session session) throws RepositoryException {
        checkInitialized();
        throw new UnsupportedRepositoryOperationException("UserManager not supported.");
    }

    /**
     * @see JackrabbitSecurityManager#getUserID(javax.security.auth.Subject, String)
     */
    public String getUserID(Subject subject, String workspaceName) throws RepositoryException {
        String uid = null;

        // if SimpleCredentials are present, the UserID can easily be retrieved.
        Iterator<SimpleCredentials> creds = subject.getPublicCredentials(SimpleCredentials.class).iterator();
        if (creds.hasNext()) {
            SimpleCredentials sc = creds.next();
            uid = sc.getUserID();
        } else if (anonymID != null && !subject.getPrincipals(AnonymousPrincipal.class).isEmpty()) {
            uid = anonymID;
        } else {
            // assume that UserID and principal name
            // are the same (not totally correct) and thus return the name
            // of the first non-group principal.
            for (Principal p : subject.getPrincipals()) {
                if (!GroupPrincipals.isGroup(p)) {
                    uid = p.getName();
                    break;
                }
            }
        }
        return uid;
    }

    /**
     * Creates an AuthContext for the given {@link Credentials} and
     * {@link Subject}.<br>
     * This includes selection of applicatoin specific LoginModules and
     * initalization with credentials and Session to System-Workspace
     *
     * @return an {@link AuthContext} for the given Credentials, Subject
     * @throws RepositoryException in other exceptional repository states
     */
    public AuthContext getAuthContext(Credentials creds, Subject subject, String workspaceName)
            throws RepositoryException {
        checkInitialized();
        return authCtxProvider.getAuthContext(creds, subject, systemSession, principalProviderRegistry, adminID, anonymID);
    }

    //--------------------------------------------------------------------------
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized");
        }
    }

    /**
     * Simple Principal provider
     */
    private class SimplePrincipalProvider implements PrincipalProvider {

        private final Map<String, Principal> principals = new HashMap<String, Principal>();

        private SimplePrincipalProvider() {
            if (adminID != null) {
                principals.put(adminID, new AdminPrincipal(adminID));
            }
            if (anonymID != null) {
                principals.put(anonymID, new AnonymousPrincipal());
            }

            EveryonePrincipal everyone = EveryonePrincipal.getInstance();
            principals.put(everyone.getName(), everyone);
        }

        public Principal getPrincipal(String principalName) {
            if (principals.containsKey(principalName)) {
                return principals.get(principalName);
            } else {
                return new UserPrincipal(principalName);
            }
        }

        public PrincipalIterator findPrincipals(String simpleFilter) {
            return findPrincipals(simpleFilter, PrincipalManager.SEARCH_TYPE_ALL);
        }

        public PrincipalIterator findPrincipals(String simpleFilter, int searchType) {
            Principal p = getPrincipal(simpleFilter);
            if (p == null) {
                return PrincipalIteratorAdapter.EMPTY;
            } else if (GroupPrincipals.isGroup(p) && searchType == PrincipalManager.SEARCH_TYPE_NOT_GROUP ||
                       !GroupPrincipals.isGroup(p) && searchType == PrincipalManager.SEARCH_TYPE_GROUP) {
                return PrincipalIteratorAdapter.EMPTY;
            } else {
                return new PrincipalIteratorAdapter(Collections.singletonList(p));
            }
        }

        public PrincipalIterator getPrincipals(int searchType) {
            PrincipalIterator it;
            switch (searchType) {
                case PrincipalManager.SEARCH_TYPE_GROUP:
                    it = new PrincipalIteratorAdapter(Collections.singletonList(EveryonePrincipal.getInstance()));
                    break;
                case PrincipalManager.SEARCH_TYPE_NOT_GROUP:
                    Set<Principal> set = new HashSet<Principal>(principals.values());
                    set.remove(EveryonePrincipal.getInstance());
                    it = new PrincipalIteratorAdapter(set);
                    break;
                case PrincipalManager.SEARCH_TYPE_ALL:
                    it = new PrincipalIteratorAdapter(principals.values());
                    break;
                // no default
                default:
                    throw new IllegalArgumentException("Unknown search type " + searchType);
            }
            return it;
        }

        public PrincipalIterator getGroupMembership(Principal principal) {
            if (principal instanceof EveryonePrincipal) {
                return PrincipalIteratorAdapter.EMPTY;
            } else {
                return new PrincipalIteratorAdapter(Collections.singletonList(EveryonePrincipal.getInstance()));
            }
        }

        public void init(Properties options) {
            // nothing to do
        }

        public void close() {
            // nothing to do
        }

        public boolean canReadPrincipal(Session session, Principal principal) {
            return true;
        }
    }
}
