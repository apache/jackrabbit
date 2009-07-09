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
package org.apache.jackrabbit.core;

import javax.jcr.security.AccessControlException;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.config.SecurityConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.config.WorkspaceSecurityConfig;
import org.apache.jackrabbit.core.config.SecurityManagerConfig;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.JackrabbitSecurityManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.DefaultAccessManager;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.security.authentication.AuthContextProvider;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AccessControlProviderFactory;
import org.apache.jackrabbit.core.security.authorization.AccessControlProviderFactoryImpl;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.principal.DefaultPrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.core.security.principal.PrincipalManagerImpl;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.apache.jackrabbit.core.security.principal.ProviderRegistryImpl;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The security manager acts as central managing class for all security related
 * operations on a low-level non-protected level. It manages the
 * <ul>
 * <li> {@link PrincipalProvider}s
 * <li> {@link AccessControlProvider}s
 * <li> {@link WorkspaceAccessManager}
 * <li> {@link UserManager}
 * </ul>
 */
public class DefaultSecurityManager implements JackrabbitSecurityManager {

    // TODO: should rather be placed in the core.security package. However protected access to SystemSession required to move here.
    /**
     * the default logger
     */
    private static final Logger log = LoggerFactory.getLogger(DefaultSecurityManager.class);

    /**
     * Flag indicating if the security manager was properly initialized.
     */
    private boolean initialized;

    /**
     * the repository implementation
     */
    private RepositoryImpl repository;

    /**
     * session on the system workspace.
     */
    private SystemSession securitySession;

    /**
     * System user manager. Implementation needed here for the DefaultPrincipalProvider.
     */
    private UserManager systemUserManager;

    /**
     * System Sessions PrincipalMangager used for internal access to Principals
     */
    private PrincipalManager systemPrincipalManager;

    /**
     * The user id of the administrator. The value is retrieved from
     * configuration. If the config entry is missing a default id is used (see
     * {@link SecurityConstants#ADMIN_ID}).
     */
    protected String adminId;

    /**
     * The user id of the anonymous user. The value is retrieved from
     * configuration. If the config entry is missing a default id is used (see
     * {@link SecurityConstants#ANONYMOUS_ID}).
     */
    private String anonymousId;

    /**
     * Contains the access control providers per workspace.
     * key = name of the workspace,
     * value = {@link AccessControlProvider}
     */
    private final Map<String, AccessControlProvider> acProviders = new HashMap<String, AccessControlProvider>();

    /**
     * the AccessControlProviderFactory
     */
    private AccessControlProviderFactory acProviderFactory;

    /**
     * the configured WorkspaceAccessManager
     */
    private WorkspaceAccessManager workspaceAccessManager;

    /**
     * the principal provider registry
     */
    private PrincipalProviderRegistry principalProviderRegistry;

    /**
     * factory for login-context {@see Repository#login())
     */
    private AuthContextProvider authContextProvider;

    //------------------------------------------< JackrabbitSecurityManager >---
    /**
     * @see JackrabbitSecurityManager#init(Repository, Session)
     */
    public synchronized void init(Repository repository, Session systemSession) throws RepositoryException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        if (!(repository instanceof RepositoryImpl)) {
            throw new RepositoryException("RepositoryImpl expected");
        }
        if (!(systemSession instanceof SystemSession)) {
            throw new RepositoryException("SystemSession expected");
        }

        securitySession = (SystemSession) systemSession;
        this.repository = (RepositoryImpl) repository;

        SecurityConfig config = this.repository.getConfig().getSecurityConfig();
        LoginModuleConfig loginModConf = config.getLoginModuleConfig();

        // build AuthContextProvider based on appName + optional LoginModuleConfig
        authContextProvider = new AuthContextProvider(config.getAppName(), loginModConf);
        if (authContextProvider.isLocal()) {
            log.info("init: use Repository Login-Configuration for " + config.getAppName());
        } else if (authContextProvider.isJAAS()) {
            log.info("init: use JAAS login-configuration for " + config.getAppName());
        } else {
            String msg = "Neither JAAS nor RepositoryConfig contained a valid Configuriation for " + config.getAppName();
            log.error(msg);
            throw new RepositoryException(msg);
        }

        Properties[] moduleConfig = authContextProvider.getModuleConfig();

        // retrieve default-ids (admin and anomymous) from login-module-configuration.
        for (Properties props : moduleConfig) {
            if (props.containsKey(LoginModuleConfig.PARAM_ADMIN_ID)) {
                adminId = props.getProperty(LoginModuleConfig.PARAM_ADMIN_ID);
            }
            if (props.containsKey(LoginModuleConfig.PARAM_ANONYMOUS_ID)) {
                anonymousId = props.getProperty(LoginModuleConfig.PARAM_ANONYMOUS_ID);
            }
        }
        // fallback:
        if (adminId == null) {
            log.debug("No adminID defined in LoginModule/JAAS config -> using default.");
            adminId = SecurityConstants.ADMIN_ID;
        }
        if (anonymousId == null) {
            log.debug("No anonymousID defined in LoginModule/JAAS config -> using default.");
            anonymousId = SecurityConstants.ANONYMOUS_ID;
        }

        // create the system userManager and make sure the system-users exist.
        systemUserManager = createUserManager(securitySession);
        createSystemUsers(systemUserManager, adminId, anonymousId);

        // init default ac-provider-factory
        acProviderFactory = new AccessControlProviderFactoryImpl();
        acProviderFactory.init(securitySession);

        // create the evalutor for workspace access
        SecurityManagerConfig smc = config.getSecurityManagerConfig();
        if (smc != null && smc.getWorkspaceAccessConfig() != null) {
            workspaceAccessManager = (WorkspaceAccessManager) smc.getWorkspaceAccessConfig().newInstance();
        } else {
            // fallback -> the default implementation
            log.debug("No WorkspaceAccessManager configured; using default.");
            workspaceAccessManager = new WorkspaceAccessManagerImpl();
        }
        workspaceAccessManager.init(securitySession);

        // initialize principa-provider registry
        // 1) create default
        PrincipalProvider defaultPP = new DefaultPrincipalProvider(securitySession, (UserManagerImpl) systemUserManager);
        defaultPP.init(new Properties());
        // 2) create registry instance
        principalProviderRegistry = new ProviderRegistryImpl(defaultPP);
        // 3) register all configured principal providers.
        for (Properties props : moduleConfig) {
            principalProviderRegistry.registerProvider(props);
        }

        // create the principal manager for the security workspace
        systemPrincipalManager = new PrincipalManagerImpl(securitySession, principalProviderRegistry.getProviders());

        initialized = true;
    }

    /**
     * Creates a {@link UserManagerImpl} for the given session. May be overridden
     * to return a custom implementation.
     *
     * @param session session
     * @return user manager
     * @throws RepositoryException if an error occurs
     */
    protected UserManagerImpl createUserManager(SessionImpl session)
            throws RepositoryException {

        return new UserManagerImpl(session, adminId);
    }

    /**
     * @see JackrabbitSecurityManager#dispose(String)
     */
    public void dispose(String workspaceName) {
        checkInitialized();
        synchronized (acProviders) {
            AccessControlProvider prov = acProviders.remove(workspaceName);
            if (prov != null) {
                prov.close();
            }
        }
    }

    /**
     * @see JackrabbitSecurityManager#close()
     */
    public void close() {
        checkInitialized();
        synchronized (acProviders) {
            Iterator<AccessControlProvider> itr = acProviders.values().iterator();
            while (itr.hasNext()) {
                itr.next().close();
            }
            acProviders.clear();
        }
    }

    /**
     * @see JackrabbitSecurityManager#getAccessManager(Session,AMContext)
     */
    public AccessManager getAccessManager(Session session, AMContext amContext) throws RepositoryException {
        checkInitialized();
        AccessManagerConfig amConfig = repository.getConfig().getSecurityConfig().getAccessManagerConfig();
        try {
            String wspName = session.getWorkspace().getName();
            AccessControlProvider pp = getAccessControlProvider(wspName);
            AccessManager accessMgr;
            if (amConfig == null) {
                log.debug("No configuration entry for AccessManager. Using org.apache.jackrabbit.core.security.DefaultAccessManager");
                accessMgr = new DefaultAccessManager();
            } else {
                accessMgr = (AccessManager) amConfig.newInstance();
            }

            accessMgr.init(amContext, pp, workspaceAccessManager);
            return accessMgr;
        } catch (AccessDeniedException e) {
            // re-throw
            throw e;
        } catch (Exception e) {
            // wrap in RepositoryException
            String clsName = (amConfig == null) ? "-- missing access manager configuration --" : amConfig.getClassName();
            String msg = "Failed to instantiate AccessManager (" + clsName + ")";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see JackrabbitSecurityManager#getPrincipalManager(Session)
     */
    public PrincipalManager getPrincipalManager(Session session) throws RepositoryException {
        checkInitialized();
        if (session == securitySession) {
            return systemPrincipalManager;
        } else if (session instanceof SessionImpl) {
            SessionImpl sImpl = (SessionImpl) session;
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
        if (session == securitySession) {
            return systemUserManager;
        } else if (session instanceof SessionImpl) {
            String workspaceName = securitySession.getWorkspace().getName();
            try {
                SessionImpl sImpl = (SessionImpl) session;
                UserManagerImpl uMgr;
                if (workspaceName.equals(sImpl.getWorkspace().getName())) {
                    uMgr = createUserManager(sImpl);
                } else {
                    SessionImpl s = (SessionImpl) sImpl.createSession(workspaceName);
                    uMgr = createUserManager(s);
                    sImpl.addListener(uMgr);
                }
                return uMgr;
            } catch (NoSuchWorkspaceException e) {
                throw new AccessControlException("Cannot build UserManager for " + session.getUserID(), e);
            }
        } else {
            throw new RepositoryException("Internal error: SessionImpl expected.");
        }
    }

    /**
     * @see JackrabbitSecurityManager#getUserID(Subject)
     */
    public String getUserID(Subject subject) throws RepositoryException {
        checkInitialized();
        /* shortcut if the subject contains the AdminPrincipal in which case
           the userID is already known. */
        if (!subject.getPrincipals(AdminPrincipal.class).isEmpty()) {
            return adminId;
        }
        /*
         Retrieve userID from the subject.
         Since the subject may contain multiple principals and the principal
         name must not be equals to the UserID by definition, the userID
         may either be obtained from the login-credentials or from the
         user manager. in the latter case the set of principals present with
         the specified subject is used to search for the user.
        */
        String uid = null;
        // try simple access to userID over SimpleCredentials first.
        Iterator<SimpleCredentials> creds = subject.getPublicCredentials(
                SimpleCredentials.class).iterator();
        if (creds.hasNext()) {
            SimpleCredentials sc = creds.next();
            uid = sc.getUserID();
        } else {
            // no SimpleCredentials: retrieve authorizables corresponding to
            // a non-group principal. the first one present is used to determine
            // the userID.
            for (Iterator<Principal> it = subject.getPrincipals().iterator(); it.hasNext();) {
                Principal p = (Principal) it.next();
                if (!(p instanceof Group)) {
                    Authorizable authorz = systemUserManager.getAuthorizable(p);
                    if (authorz != null && !authorz.isGroup()) {
                        uid = authorz.getID();
                        break;
                    }
                }
            }
        }
        return uid;
    }

    /**
     * Creates an AuthContext for the given {@link Credentials} and
     * {@link Subject}.<br>
     * This includes selection of application specific LoginModules and
     * initialization with credentials and Session to System-Workspace
     *
     * @return an {@link AuthContext} for the given Credentials, Subject
     * @throws RepositoryException in other exceptional repository states
     */
    public AuthContext getAuthContext(Credentials creds, Subject subject)
            throws RepositoryException {
        checkInitialized();
        return authContextProvider.getAuthContext(creds, subject, securitySession,
                principalProviderRegistry, adminId, anonymousId);
    }

    //--------------------------------------------------------------------------
    /**
     * Returns the access control provider for the specified
     * <code>workspaceName</code>.
     *
     * @param workspaceName Name of the workspace.
     * @return access control provider
     * @throws NoSuchWorkspaceException If no workspace with 'workspaceName' exists.
     * @throws RepositoryException
     */
    private AccessControlProvider getAccessControlProvider(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        checkInitialized();
        AccessControlProvider provider = acProviders.get(workspaceName);
        if (provider == null || !provider.isLive()) {
            SystemSession systemSession = repository.getSystemSession(workspaceName);
            // mark this session as 'active' so the workspace does not get disposed
            // by the workspace-janitor until the garbage collector is done
            // TODO: review again... this workaround is now used in several places.
            repository.onSessionCreated(systemSession);

            WorkspaceConfig conf = repository.getConfig().getWorkspaceConfig(workspaceName);
            WorkspaceSecurityConfig secConf = (conf == null) ?  null : conf.getSecurityConfig();
            synchronized (acProviders) {
                provider = acProviderFactory.createProvider(systemSession, secConf);
                acProviders.put(workspaceName, provider);
            }
        }
        return provider;
    }

    /**
     * Make sure the 'administrators' group exists and the user with the
     * configured (or default) adminID is member of this user-group.
     *
     * @param userManager Manager to create users/groups.
     * @param adminId UserID of the administrator.
     * @param anonymousId UserID of the anonymous user.
     * @throws RepositoryException If an error occurs.
     */
    private static void createSystemUsers(UserManager userManager,
                                          String adminId,
                                          String anonymousId) throws RepositoryException {
        Principal pr = new PrincipalImpl(SecurityConstants.ADMINISTRATORS_NAME);
        Group admins = (Group) userManager.getAuthorizable(pr);
        if (admins == null) {
            admins = userManager.createGroup(new PrincipalImpl(SecurityConstants.ADMINISTRATORS_NAME));
            log.debug("...created administrators group with name '"+SecurityConstants.ADMINISTRATORS_NAME+"'");
        }

        if (adminId != null) {
            Authorizable admin = userManager.getAuthorizable(adminId);
            if (admin == null) {
                admin = userManager.createUser(adminId, adminId);
                log.info("...created admin-user with id \'" + adminId + "\' ...");
                admins.addMember(admin);
                log.info("...added admin \'" + adminId + "\' as member of the administrators group.");
            }
        }

        if (anonymousId != null) {
            Authorizable anonymous = userManager.getAuthorizable(anonymousId);
            if (anonymous == null) {
                userManager.createUser(anonymousId, "");
                log.info("...created anonymous-user with id \'" + anonymousId + "\' ...");
            }
        }
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized");
        }
    }

    //------------------------------------------------------< inner classes >---
    /**
     * <code>WorkspaceAccessManager</code> that upon {@link #grants(Set principals, String)}
     * evaluates if access to the root node of a workspace with the specified
     * name is granted.
     */
    private class WorkspaceAccessManagerImpl implements SecurityConstants, WorkspaceAccessManager {

        //-----------------------------------------< WorkspaceAccessManager >---
        /**
         * {@inheritDoc}
         * @param securitySession
         */
        public void init(Session securitySession) throws RepositoryException {
            // nothing to do here.
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws RepositoryException {
            // nothing to do here.
        }

        /**
         * {@inheritDoc}
         */
        public boolean grants(Set principals, String workspaceName) throws RepositoryException {
            AccessControlProvider prov = getAccessControlProvider(workspaceName);
            return prov.canAccessRoot(principals);
        }
    }
}
