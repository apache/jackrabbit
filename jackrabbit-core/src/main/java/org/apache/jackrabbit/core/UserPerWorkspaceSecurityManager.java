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

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.config.UserManagerConfig;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.security.authentication.AuthContextProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.principal.AbstractPrincipalProvider;
import org.apache.jackrabbit.core.security.principal.DefaultPrincipalProvider;
import org.apache.jackrabbit.core.security.principal.GroupPrincipals;
import org.apache.jackrabbit.core.security.principal.PrincipalManagerImpl;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.apache.jackrabbit.core.security.simple.SimpleWorkspaceAccessManager;
import org.apache.jackrabbit.core.security.user.MembershipCache;
import org.apache.jackrabbit.core.security.user.UserPerWorkspaceUserManager;
import org.apache.jackrabbit.core.security.user.UserManagerImpl;
import org.apache.jackrabbit.core.security.user.action.AuthorizableAction;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Derived security manager implementation that expects that users information
 * is present in each workspace instead of having a single, dedicated
 * "security-workspace" that provides user information. Consequently, the
 * UserManager used to retrieve and manipulate user content is always
 * bound to the <code>Session</code> passed to {@link #getUserManager(Session)}.
 * <p> In addition the default (user-based) principal provider created by
 * {@link org.apache.jackrabbit.core.DefaultSecurityManager}
 * cannot be used to retrieve principals. Instead this implementation keeps
 * a distinct pp-registry for each workspace.
 * </p>
 * NOTE: While this security manager asserts that a minimal set of system
 * users (admin and anonymous) is present in each workspace
 * it doesn't make any attempt to set or define the access permissions on the
 * tree containing user related information. 
 */
public class UserPerWorkspaceSecurityManager extends DefaultSecurityManager {

    private final Map<String, PrincipalProviderRegistry> ppRegistries = new HashMap<String, PrincipalProviderRegistry>();
    private final Object monitor = new Object();

    /**
     * List of workspace names for which {@link #createSystemUsers} has already
     * been called.
     */
    private final List<String> systemUsersInitialized = new ArrayList<String>();

    private PrincipalProviderRegistry getPrincipalProviderRegistry(SessionImpl s) throws RepositoryException {
        String wspName = s.getWorkspace().getName();
        synchronized (monitor) {
            PrincipalProviderRegistry p = ppRegistries.get(wspName);
            if (p == null) {
                SystemSession systemSession;
                if (s instanceof SystemSession) {
                    systemSession = (SystemSession) s;
                } else {
                    RepositoryImpl repo = (RepositoryImpl) getRepository();
                    systemSession = repo.getSystemSession(wspName);
                    // TODO: review again... this workaround is used in several places.
                    repo.markWorkspaceActive(wspName);
                }

                Properties[] moduleConfig = new AuthContextProvider("", ((RepositoryImpl) getRepository()).getConfig().getSecurityConfig().getLoginModuleConfig()).getModuleConfig();

                PrincipalProvider defaultPP = new DefaultPrincipalProvider(systemSession, (UserManagerImpl) getUserManager(systemSession));

                boolean initialized = false;
                for (Properties props : moduleConfig) {
                    //GRANITE-4470: apply config to DefaultPrincipalProvider if there is no explicit PrincipalProvider configured
                    if (!props.containsKey(LoginModuleConfig.PARAM_PRINCIPAL_PROVIDER_CLASS) && props.containsKey(AbstractPrincipalProvider.MAXSIZE_KEY)) {
                        defaultPP.init(props);
                        initialized = true;
                        break;
                    }
                }
                if (!initialized) {
                    defaultPP.init(new Properties());
                }

                p = new WorkspaceBasedPrincipalProviderRegistry(defaultPP);
                ppRegistries.put(wspName, p);
            }
            return p;
        }
    }

    //------------------------------------------< JackrabbitSecurityManager >---
    /**
     * @see org.apache.jackrabbit.core.security.JackrabbitSecurityManager#init(Repository, Session)
     */
    @Override
    public void init(Repository repository, Session systemSession) throws RepositoryException {
        super.init(repository, systemSession);

        systemUsersInitialized.add(systemSession.getWorkspace().getName());
    }

    /**
     * @see org.apache.jackrabbit.core.security.JackrabbitSecurityManager#dispose(String)
     */
    @Override
    public void dispose(String workspaceName) {
        super.dispose(workspaceName);
        synchronized (monitor) {
            PrincipalProviderRegistry reg = ppRegistries.remove(workspaceName);
            if (reg != null) {
                reg.getDefault().close();
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.core.security.JackrabbitSecurityManager#close()
     */
    @Override
    public void close() {
        super.close();
        synchronized (monitor) {
            for (PrincipalProviderRegistry registry : ppRegistries.values()) {
                registry.getDefault().close();
            }
            ppRegistries.clear();
        }
    }

    /**
     * As this implementation expects that users information in present in
     * every workspace, the UserManager is always created with the given
     * session.
     * 
     * @see org.apache.jackrabbit.core.security.JackrabbitSecurityManager#getUserManager(javax.jcr.Session)
     */
    @Override
    public UserManager getUserManager(Session session) throws RepositoryException {
        checkInitialized();                    
        if (session == getSystemSession()) {
            return super.getUserManager(session);
        } else if (session instanceof SessionImpl) {
            UserManager uMgr = createUserManager((SessionImpl) session);
            // Since users are not stored in a dedicated security workspace:
            // make sure the system users are present. this is always the case
            // for the configured security-workspace (or if missing the default
            // workspace) but not for other workspaces.
            // However, the check is only executed if the given session is a
            // SystemSession (see also #getPrincipalProviderRegistry(Session)
            // that initializes a SystemSession based UserManager for each workspace).
            String wspName = session.getWorkspace().getName();
            if (session instanceof SystemSession && !systemUsersInitialized.contains(wspName)) {
                createSystemUsers(uMgr, (SystemSession) session, adminId, anonymousId);
                systemUsersInitialized.add(wspName);
            }
            return uMgr;
        } else {
            throw new RepositoryException("Internal error: SessionImpl expected.");
        }
    }

    /**
     * Creates an AuthContext for the given {@link javax.jcr.Credentials} and
     * {@link javax.security.auth.Subject}.<br>
     * This includes selection of application specific LoginModules and
     * initialization with credentials and Session to System-Workspace
     *
     * @return an {@link org.apache.jackrabbit.core.security.authentication.AuthContext} for the given Credentials, Subject
     * @throws javax.jcr.RepositoryException in other exceptional repository states
     */
    @Override
    public AuthContext getAuthContext(Credentials creds, Subject subject, String workspaceName)
            throws RepositoryException {
        checkInitialized();
        SystemSession systemSession = ((RepositoryImpl) getRepository()).getSystemSession(workspaceName);
        return getAuthContextProvider().getAuthContext(creds, subject, systemSession,
                getPrincipalProviderRegistry(systemSession), adminId, anonymousId);
    }

    //--------------------------------------------------------------------------
    /**
     * Always returns <code>null</code>. The default principal provider is
     * workspace depending as users are expected to exist in every workspace.
     * 
     * @return <code>null</code>
     * @throws RepositoryException
     */
    @Override
    protected PrincipalProvider createDefaultPrincipalProvider(Properties[] moduleConfig) throws RepositoryException {
        return null;
    }

    @Override
    protected UserManager getSystemUserManager(String workspaceName) throws RepositoryException {
        if (workspaceName.equals(getSystemSession().getWorkspace().getName())) {
            return super.getSystemUserManager(workspaceName);
        } else {
            return ((RepositoryImpl) getRepository()).getWorkspaceInfo(workspaceName).getSystemSession().getUserManager();
        }
    }

    /**
     * Creates a new instanceof <code>TransientChangeUserManagerImpl</code>.
     * 
     * @param session session
     * @return an instanceof <code>TransientChangeUserManagerImpl</code>
     * @throws RepositoryException
     */
    @Override
    protected UserManagerImpl createUserManager(SessionImpl session) throws RepositoryException {
        UserManagerConfig umc = getConfig().getUserManagerConfig();
        UserManagerImpl umgr;
        // in contrast to the DefaultSecurityManager users are not retrieved
        // from a dedicated workspace: the system session of each workspace must
        // get a system user manager that asserts the existence of the admin user.
        if (umc != null) {
            Class<?>[] paramTypes = new Class[] {
                    SessionImpl.class,
                    String.class,
                    Properties.class,
                    MembershipCache.class};
            umgr = (UserPerWorkspaceUserManager) umc.getUserManager(UserPerWorkspaceUserManager.class,
                    paramTypes, session, adminId, umc.getParameters(), getMembershipCache(session));
        } else {
            umgr = new UserPerWorkspaceUserManager(session, adminId, null, getMembershipCache(session));
        }

        if (umc != null && !(session instanceof SystemSession)) {
            AuthorizableAction[] actions = umc.getAuthorizableActions();
            umgr.setAuthorizableActions(actions);
        }
        return umgr;
    }

    /**
     * @param session Session for the principal manager must be created.
     * @return A new instance of PrincipalManagerImpl. Note that this implementation
     * uses a workspace specific principal provider registry, that retrieves
     * the configured providers from the registry obtained through
     * {@link #getPrincipalProviderRegistry()} but has a workspace specific
     * default provider.
     * @throws RepositoryException
     */
    @Override
    protected PrincipalManager createPrincipalManager(SessionImpl session) throws RepositoryException {
        return new PrincipalManagerImpl(session, getPrincipalProviderRegistry(session).getProviders());
    }

    /**
     * Returns a new instance of <code>SimpleWorkspaceAccessManager</code>, since
     * with the <code>DefaultLoginModule</code> the existence of the user
     * is checked in order to successfully complete the login. Since with this
     * SecurityManager users are stored separately in each workspace, a user
     * may only login to a workspace if the corresponding user node exists.
     * Consequently a lazy workspace access manager is sufficient.
     * <p>
     * If this SecurityManager is used with a distinct <code>LoginModule</code>
     * implementation, the {@link org.apache.jackrabbit.core.config.SecurityManagerConfig#getWorkspaceAccessConfig() configuration}
     * for <code>WorkspaceAccessManager</code> should be adjusted as well.
     *
     * @return An new instance of {@link SimpleWorkspaceAccessManager}.
     */
    @Override
    protected WorkspaceAccessManager createDefaultWorkspaceAccessManager() {
        return new WorkspaceAccessManagerImpl();
    }

    //--------------------------------------------------------------------------
    /**
     * Workaround to get a default (user-based) principal provider depending
     * on the workspace being accessed. This is required for this security
     * manager as users aren't stored in a single, dedicated workspace.
     */
    private final class WorkspaceBasedPrincipalProviderRegistry implements PrincipalProviderRegistry {

        private final PrincipalProvider defaultPrincipalProvider;

        public WorkspaceBasedPrincipalProviderRegistry(PrincipalProvider defaultPrincipalProvider) {
            this.defaultPrincipalProvider = defaultPrincipalProvider;
        }

        public PrincipalProvider registerProvider(Properties configuration) throws RepositoryException {
            return getPrincipalProviderRegistry().registerProvider(configuration);
        }

        public PrincipalProvider getDefault() {
            return defaultPrincipalProvider;
        }

        public PrincipalProvider getProvider(String className) {
            PrincipalProvider p = getPrincipalProviderRegistry().getProvider(className);
            if (p == null && defaultPrincipalProvider.getClass().getName().equals(className)) {
                p = defaultPrincipalProvider;
            }
            return p;
        }

        public PrincipalProvider[] getProviders() {
            List<PrincipalProvider> l = new ArrayList<PrincipalProvider>();
            l.addAll(Arrays.asList(getPrincipalProviderRegistry().getProviders()));
            l.add(defaultPrincipalProvider);
            return l.toArray(new PrincipalProvider[l.size()]);
        }
    }

    private final class WorkspaceAccessManagerImpl implements WorkspaceAccessManager {
        /**
         * Does nothing.
         * @see WorkspaceAccessManager#init(javax.jcr.Session)
         */
        public void init(Session systemSession) throws RepositoryException {
            // nothing to do.
        }

        /**
         * Does nothing.
         * @see org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager#close()
         */
        public void close() throws RepositoryException {
            // nothing to do.
        }

        /**
         * Returns <code>true</code> if a workspace with the given
         * <code>workspaceName</code> exists and if that workspace defines a
         * user that matches any of the given <code>principals</code>;
         * <code>false</code> otherwise.
         *
         * @see WorkspaceAccessManager#grants(java.util.Set, String)
         */
        public boolean grants(Set<Principal> principals, String workspaceName) throws RepositoryException {
            if (!(Arrays.asList(((RepositoryImpl) getRepository()).getWorkspaceNames())).contains(workspaceName)) {
                return false;
            } else {
                UserManager umgr = UserPerWorkspaceSecurityManager.this.getSystemUserManager(workspaceName);
                for (Principal principal : principals) {
                    if (!GroupPrincipals.isGroup(principal)) {
                        // check if the workspace identified by the given workspace
                        // name contains a user with this principal
                        if (umgr.getAuthorizable(principal) != null) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}