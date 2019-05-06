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
package org.apache.jackrabbit.core.security.authentication;

import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractLoginModule</code> provides the means for the common
 * authentication tasks within the Repository.
 * <p>
 * On successful authentication it associates the credentials to principals
 * using the {@link PrincipalProvider} configured for this LoginModule
 * <p>
 * Jackrabbit distinguishes between Login and Impersonation dispatching the
 * the corresponding Repository/Session methods to
 * {@link #authenticate(java.security.Principal, javax.jcr.Credentials)} and
 * {@link #impersonate(java.security.Principal, javax.jcr.Credentials)}, respectively.
 * <br>
 * This LoginModule implements default behavior for either method.
 *
 * @see LoginModule
 */
public abstract class AbstractLoginModule implements LoginModule {

    private static final Logger log = LoggerFactory.getLogger(AbstractLoginModule.class);

    private static final String KEY_CREDENTIALS = "org.apache.jackrabbit.credentials";
    private static final String KEY_LOGIN_NAME = "javax.security.auth.login.name";

    /**
     * The name of the login module configuration option providing the name
     * of the SimpleCredentials attribute used to identify a pre-authenticated
     * login.
     *
     * @see #isPreAuthenticated(Credentials)
     * @deprecated For security reasons this configuration option has been
     * deprecated and will no longer be supported in a subsequent release.
     * See also <a href="https://issues.apache.org/jira/browse/JCR-3293">JCR-3293</a>
     */
    private static final String PRE_AUTHENTICATED_ATTRIBUTE_OPTION = "trust_credentials_attribute";

    private String principalProviderClassName;
    private boolean initialized;

    protected String adminId;
    protected String anonymousId;

    /**
     * The name of the credentials attribute providing a hint that the
     * credentials should be taken as is and the user requesting access
     * has already been authenticated outside of this LoginModule.
     *
     * @see #getPreAuthAttributeName()
     * @deprecated For security reasons the support for the preAuth attribute
     * has been deprecated and will no longer be available in a subsequent release.
     * See also <a href="https://issues.apache.org/jira/browse/JCR-3293">JCR-3293</a>
     */
    private String preAuthAttributeName;


    protected CallbackHandler callbackHandler;

    protected Principal principal;
    protected SimpleCredentials credentials;
    protected Subject subject;
    protected PrincipalProvider principalProvider;

    protected Map sharedState;

    /**
     * Initialize this LoginModule and sets the following fields for later usage:
     * <ul>
     * <li>{@link PrincipalProvider} for user-{@link Principal} resolution.</li>
     * <li>{@link LoginModuleConfig#PARAM_ADMIN_ID} option is evaluated</li>
     * <li>{@link LoginModuleConfig#PARAM_ANONYMOUS_ID} option is evaluated</li>
     * </ul>
     * Implementations are called via
     * {@link #doInit(CallbackHandler, Session, Map)} to implement
     * additional initialization
     *
     * @param subject         the <code>Subject</code> to be authenticated. <p>
     * @param callbackHandler a <code>CallbackHandler</code> for communicating
     *                        with the end user (prompting for usernames and
     *                        passwords, for example). <p>
     * @param sharedState     state shared with other configured
     *                        LoginModules.<p>
     * @param options         options specified in the login <code>Configuration</code>
     *                        for this particular <code>LoginModule</code>.
     * @see LoginModule#initialize(Subject, CallbackHandler, Map, Map)
     * @see #doInit(CallbackHandler, Session, Map)
     * @see #isInitialized()
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String,?> sharedState, Map<String,?> options) {
        // common jaas state variables
        this.callbackHandler = callbackHandler;
        this.subject = subject;
        this.sharedState = sharedState;

        // initialize the login module
        try {
            log.debug("Initialize LoginModule: ");
            RepositoryCallback repositoryCb = new RepositoryCallback();
            callbackHandler.handle(new Callback[]{repositoryCb});

            PrincipalProviderRegistry registry = repositoryCb.getPrincipalProviderRegistry();
            // check if the class name of a PrincipalProvider implementation
            // is present with the module configuration.
            if (options.containsKey(LoginModuleConfig.PARAM_PRINCIPAL_PROVIDER_CLASS)) {
                Object pcOption = options.get(LoginModuleConfig.PARAM_PRINCIPAL_PROVIDER_CLASS);
                if (pcOption != null) {
                    principalProviderClassName = pcOption.toString();
                }
            }
            if (principalProviderClassName == null) {
                // try compatibility parameters
                if (options.containsKey(LoginModuleConfig.COMPAT_PRINCIPAL_PROVIDER_NAME)) {
                    principalProviderClassName = options.get(LoginModuleConfig.COMPAT_PRINCIPAL_PROVIDER_NAME).toString();
                } else if (options.containsKey(LoginModuleConfig.COMPAT_PRINCIPAL_PROVIDER_CLASS)) {
                    principalProviderClassName = options.get(LoginModuleConfig.COMPAT_PRINCIPAL_PROVIDER_CLASS).toString();
                }
            }
            if (principalProviderClassName != null) {
                principalProvider = registry.getProvider(principalProviderClassName);
            }
            if (principalProvider == null) {
                principalProvider = registry.getDefault();
                if (principalProvider == null) {
                    return; // abort. not even a default principal provider
                }
            }
            log.debug("- PrincipalProvider -> '" + principalProvider.getClass().getName() + "'");

            // call implementation for additional setup
            doInit(callbackHandler, repositoryCb.getSession(), options);

            // adminId: if not present in options -> retrieve from callback
            if (options.containsKey(LoginModuleConfig.PARAM_ADMIN_ID)) {
                adminId = (String) options.get(LoginModuleConfig.PARAM_ADMIN_ID);
            }
            if (adminId == null) {
                adminId = repositoryCb.getAdminId();
            }
            // anonymousId: if not present in options -> retrieve from callback
            if (options.containsKey(LoginModuleConfig.PARAM_ANONYMOUS_ID)) {
                anonymousId = (String) options.get(LoginModuleConfig.PARAM_ANONYMOUS_ID);
            }
            if (anonymousId == null) {
                anonymousId = repositoryCb.getAnonymousId();
            }
            // trusted credentials attribute name (may be missing to not
            // support) (normalized to null aka missing aka unset if an empty
            // string)
            preAuthAttributeName = (String) options.get(PRE_AUTHENTICATED_ATTRIBUTE_OPTION);
            if (preAuthAttributeName != null
                && preAuthAttributeName.length() == 0) {
                preAuthAttributeName = null;
            }

            //log config values for debug
            if (log.isDebugEnabled()) {
                for (String option : options.keySet()) {
                    log.debug("- Option: " + option + " -> '" + options.get(option) + "'");
                }
            }
            initialized = (this.subject != null);

        } catch (Exception e) {
            log.error("LoginModule failed to initialize.", e);
        }
    }

    /**
     * Implementations may set-up their own state.
     *
     * @param callbackHandler as passed by {@link javax.security.auth.login.LoginContext}
     * @param session         to security-workspace of Jackrabbit
     * @param options         options from LoginModule config
     * @throws LoginException in case initialization fails.
     */
    protected abstract void doInit(CallbackHandler callbackHandler,
                                   Session session,
                                   Map options) throws LoginException;


    /**
     * Returns <code>true</code> if this module has been successfully initialized.
     *
     * @return <code>true</code> if this module has been successfully initialized.
     * @see LoginModule#initialize(Subject, CallbackHandler, Map, Map)
     */
    protected boolean isInitialized() {
        return initialized;
    }

    /**
     * Method to authenticate a <code>Subject</code> (phase 1).
     * <p>
     * The login is divided into 3 Phases:
     * <p>
     * <b>1) User-ID resolution</b><br>
     * In a first step it is tried to resolve a User-ID for further validation.
     * As for JCR the identification is marked with the {@link Credentials}
     * interface, credentials are accessed in this phase.<br>
     * If no User-ID can be found, anonymous access is granted with the ID of
     * the anonymous user (as defined in the security configuration).
     * Anonymous access can be switched off removing the configuration entry.
     * <br> This implementation uses two helper-methods, which allow for
     * customization:
     * <ul>
     * <li>{@link #getCredentials()} and</li>
     * <li>{@link #getUserID(Credentials)}</li>
     * </ul>
     * <p>
     *
     * <b>2) User-Principal resolution </b><br>
     * In a second step it is tested, if the resolved User-ID belongs to a User
     * known to the system, i.e. if the {@link PrincipalProvider} has a principal
     * for the given ID and the principal can be found via
     * {@link PrincipalProvider#findPrincipals(String)}.<br>
     * The provider implementation can be set by the LoginModule configuration.
     * If the option is missing, the system default principal provider will
     * be used.
     * <p>
     * <b>3) Verification</b><br>
     * There are four cases, how the User-ID can be verified:
     * The login is anonymous, pre-authenticated or the login is the result of
     * an impersonation request (see {@link javax.jcr.Session#impersonate(Credentials)}
     * or of a login to the Repository ({@link javax.jcr.Repository#login(Credentials)}).
     * The concrete implementation of the LoginModule is responsible for all
     * four cases:
     * <ul>
     * <li>{@link #isAnonymous(Credentials)}</li>
     * <li>{@link #isPreAuthenticated(Credentials)}</li>
     * <li>{@link #authenticate(Principal, Credentials)}</li>
     * <li>{@link #impersonate(Principal, Credentials)}</li>
     * </ul>
     *
     * Under the following conditions, the login process is aborted and the
     * module is marked to be ignored:
     * <ul>
     * <li>No User-ID could be resolve, and anonymous access is switched off</li>
     * <li>No Principal is found for the User-ID resolved</li>
     * </ul>
     *
     * Under the following conditions, the login process is marked to be invalid
     * by throwing an LoginException:
     * <ul>
     * <li>It is an impersonation request, but the impersonator is not allowed
     * to impersonate to the requested User-ID</li>
     * <li>The user tries to login, but the Credentials can not be verified.</li>
     * </ul>
     * <p>
     * The LoginModule keeps the Credentials and the Principal as instance fields,
     * to mark that login has been successful.
     *
     * @return true if the authentication succeeded, or false if this
     *         <code>LoginModule</code> should be ignored.
     * @throws LoginException if the authentication fails
     * @see javax.security.auth.spi.LoginModule#login()
     * @see #getCredentials()
     * @see #getUserID(Credentials)
     * @see #getImpersonatorSubject(Credentials)
     */
    public boolean login() throws LoginException {
        if (!isInitialized()) {
            log.warn("Unable to perform login: initialization not completed.");
            return false;
        }

        // check the availability and validity of Credentials
        Credentials creds = getCredentials();
        if (creds == null) {
            log.debug("No credentials available -> try default (anonymous) authentication.");
        } else if (!supportsCredentials(creds)) {
            log.debug("Unsupported credentials implementation : " + creds.getClass().getName());
            return false;
        }
        
        try {
            Principal userPrincipal = getPrincipal(creds);
            if (userPrincipal == null) {
                // unknown or disabled user or a group
                log.debug("No valid user -> ignore.");
                return false;
            }
            boolean authenticated;
            // test for anonymous, pre-authentication, impersonation or common authentication.
            if (isAnonymous(creds) || isPreAuthenticated(creds)) {
                authenticated = true;
            } else if (isImpersonation(creds)) {
                authenticated = impersonate(userPrincipal, creds);
            } else {
                authenticated = authenticate(userPrincipal, creds);
            }

            // process authenticated user
            if (authenticated) {
                if (creds instanceof SimpleCredentials) {
                    credentials = (SimpleCredentials) creds;
                } else {
                    credentials = new SimpleCredentials(getUserID(creds), new char[0]);
                }
                principal = userPrincipal;
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Login failed:", e);
        }
        return false;
    }

    /**
     * Method to commit the authentication process (phase 2).
     * <p>
     * This method is called if the LoginContext's overall authentication
     * succeeded (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules succeeded).
     * <p>
     * If this LoginModule's own authentication attempt succeeded (checked
     * by retrieving the private state saved by the <code>login</code> method),
     * then this method associates relevant Principals and Credentials with the
     * <code>Subject</code> located in the <code>LoginModule</code>.  If this
     * LoginModule's own authentication attempted failed, then this method
     * removes/destroys any state that was originally saved.
     * <p>
     * The login is considered as succeeded if there is a principal set.
     * <p>
     * The implementation stores the principal associated to the UserID and all
     * the Groups it is member of with the Subject and in addition adds an
     * instance of (#link SimpleCredentials} to the Subject's public credentials.
     *
     * @return true if this method succeeded, or false if this
     *         <code>LoginModule</code> should be ignored.
     * @throws LoginException if the commit fails
     * @see javax.security.auth.spi.LoginModule#commit()
     */
    public boolean commit() throws LoginException {
        if (!isInitialized() || principal == null) {
            return false;
        }

        Set<Principal> principals = getPrincipals();
        subject.getPrincipals().addAll(principals);
        subject.getPublicCredentials().add(credentials);
        return true;
    }

    /**
     * Method to abort the authentication process (phase 2).
     * <p>
     * <p> This method is called if the LoginContext's overall authentication
     * failed. (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules did not succeed).
     * <p>
     * <p> If this LoginModule's own authentication attempt succeeded (checked
     * by retrieving the private state saved by the <code>login</code> method),
     * then this method cleans up any state that was originally saved.
     * <p>
     * <p>
     *
     * @return true if this method succeeded, or false if this
     *         <code>LoginModule</code> should be ignored.
     * @throws LoginException if the abort fails
     * @see javax.security.auth.spi.LoginModule#abort()
     */
    public boolean abort() throws LoginException {
        if (!isInitialized()) {
            return false;
        } else {
            sharedState.remove(KEY_CREDENTIALS);
            callbackHandler = null;
            principal = null;
            credentials = null;
            return logout();
        }
    }

    /**
     * @return <code>true</code> if this method succeeded,
     * or <code>false</code> if this <code>LoginModule</code> should be ignored.
     * @throws LoginException if the logout fails
     * @see javax.security.auth.spi.LoginModule#logout()
     */
    public boolean logout() throws LoginException {
        if (subject.getPrincipals().isEmpty() || subject.getPublicCredentials(Credentials.class).isEmpty()) {
            return false;
        } else {
            // clear subject if not readonly
            if (!subject.isReadOnly()) {
                subject.getPrincipals().clear();
                subject.getPublicCredentials().clear();
            }
            return true;
        }
    }

    /**
     * @param principal Principal used to retrieve the <code>Authentication</code>
     * object.
     * @param credentials Credentials used for the authentication.
     * @return <code>true</code> if Credentials authenticate,
     *         <code>false</code> if no <code>Authentication</code> can handle
     *         the given <code>Credentials</code>
     * @throws javax.security.auth.login.FailedLoginException
     *          if the authentication failed.
     * @throws RepositoryException If another error occurs.
     * @see AbstractLoginModule#getAuthentication(java.security.Principal, javax.jcr.Credentials)
     * @see AbstractLoginModule#authenticate(java.security.Principal, javax.jcr.Credentials)
     */
    protected boolean authenticate(Principal principal, Credentials credentials)
            throws FailedLoginException, RepositoryException {

        Authentication auth = getAuthentication(principal, credentials);
        if (auth == null) {
            return false;
        } else if (auth.authenticate(credentials)) {
            return true;
        }
        throw new FailedLoginException();
    }

    /**
     * Test if the current request is an Impersonation attempt. The default
     * implementation returns <code>true</code> if an
     * {@link #getImpersonatorSubject(Credentials) subject} for the
     * impersonation can be retrieved.
     *
     * @param credentials potentially containing impersonation data
     * @return true if this is an impersonation attempt
     * @see #getImpersonatorSubject(Credentials)
     */
    protected boolean isImpersonation(Credentials credentials) {
        return getImpersonatorSubject(credentials) != null;
    }

    /**
     * Handles the impersonation of given Credentials.
     *
     * @param principal Principal to impersonate.
     * @param credentials Credentials used to create the impersonation subject.
     * @return false, if there is no User to impersonate,
     *         true if impersonation is allowed
     * @throws LoginException If credentials don't allow to impersonate to principal.
     * @throws RepositoryException If another error occurs.
     */
    protected abstract boolean impersonate(Principal principal, Credentials credentials)
            throws RepositoryException, LoginException;

    /**
     * Retrieve the <code>Authentication</code>.
     *
     * @param principal A principal.
     * @param creds The Credentials used for the login.
     * @return Authentication object for the given principal / credentials.
     * @throws RepositoryException If an error occurs.
     */
    protected abstract Authentication getAuthentication(Principal principal, Credentials creds)
            throws RepositoryException;

    /**
     * Method tries to acquire an Impersonator in the following order:
     * <ul>
     * <li> Try to access it from the {@link Credentials} via {@link SimpleCredentials#getAttribute(String)}</li>
     * <li> Ask CallbackHandler for Impersonator with use of {@link ImpersonationCallback}.</li>
     * </ul>
     *
     * @param credentials which, may contain an impersonation Subject
     * @return impersonation subject or null if non contained
     * @see #login()
     * @see #impersonate(java.security.Principal, javax.jcr.Credentials)
     */
    protected Subject getImpersonatorSubject(Credentials credentials) {
        Subject impersonator = null;
        if (credentials == null) {
            try {
                ImpersonationCallback impers = new ImpersonationCallback();
                callbackHandler.handle(new Callback[]{impers});
                impersonator = impers.getImpersonator();
            } catch (UnsupportedCallbackException e) {
                log.warn(e.getCallback().getClass().getName() + " not supported: Unable to perform Impersonation.");
            } catch (IOException e) {
                log.error("Impersonation-Callback failed: " + e.getMessage() + ": Unable to perform Impersonation.");
            }
        } else if (credentials instanceof SimpleCredentials) {
            SimpleCredentials sc = (SimpleCredentials) credentials;
            impersonator = (Subject) sc.getAttribute(SecurityConstants.IMPERSONATOR_ATTRIBUTE);
        }
        return impersonator;
    }

    /**
     * Method tries to resolve the {@link Credentials} used for login. It takes
     * authentication-extension of an already authenticated {@link Subject} into
     * account.
     * <p>
     * Therefore the credentials are retrieved as follows:
     * <ol>
     * <li>Test if the shared state contains credentials.</li>
     * <li>Ask CallbackHandler for Credentials with using a {@link
     * CredentialsCallback}. Expects {@link CredentialsCallback#getCredentials}
     * to return an instance of {@link Credentials}.</li>
     * <li>Ask the Subject for its public <code>SimpleCredentials</code> see
     * {@link Subject#getPublicCredentials(Class)}, thus enabling to
     * pre-authenticate the Subject.</li>
     * </ol>
     *
     * @return Credentials or null if not found
     * @see #login()
     */
    protected Credentials getCredentials() {
        Credentials credentials = null;
        if (sharedState.containsKey(KEY_CREDENTIALS)) {
            credentials = (Credentials) sharedState.get(KEY_CREDENTIALS);
        } else {
            try {
                CredentialsCallback callback = new CredentialsCallback();
                callbackHandler.handle(new Callback[]{callback});
                credentials = callback.getCredentials();
                if (credentials != null && supportsCredentials(credentials)) {
                    sharedState.put(KEY_CREDENTIALS, credentials);                    
                }
            } catch (UnsupportedCallbackException e) {
                log.warn("Credentials-Callback not supported try Name-Callback");
            } catch (IOException e) {
                log.error("Credentials-Callback failed: " + e.getMessage() + ": try Name-Callback");
            }
        }
        // if still no credentials -> try to retrieve them from the subject.
        if (null == credentials) {
            // try if subject contains SimpleCredentials
            Set<SimpleCredentials> preAuthCreds = subject.getPublicCredentials(SimpleCredentials.class);
            if (!preAuthCreds.isEmpty()) {
                credentials = preAuthCreds.iterator().next();
            }
        }
        if (null == credentials) {
            // try if subject contains GuestCredentials
            Set<GuestCredentials> preAuthCreds = subject.getPublicCredentials(GuestCredentials.class);
            if (!preAuthCreds.isEmpty()) {
                credentials = preAuthCreds.iterator().next();
            }
        }
        return credentials;
    }

    /**
     * Return a flag indicating whether the credentials are supported by
     * this login module. Default implementation supports
     * {@link SimpleCredentials} and {@link GuestCredentials}.
     *
     * @param creds credentials
     * @return <code>true</code> if the credentials are supported;
     *         <code>false</code> otherwise
     */
    protected boolean supportsCredentials(Credentials creds) {
        return creds instanceof SimpleCredentials ||
            creds instanceof GuestCredentials;
    }

    /**
     * Method supports tries to acquire a UserID in the following order:
     * <ol>
     * <li>If passed credentials are {@link GuestCredentials} the anonymous user id
     * is returned.</li>
     * <li>Try to access it from the {@link Credentials} via {@link
     * SimpleCredentials#getUserID()}</li>
     * <li>Ask CallbackHandler for User-ID with use of {@link NameCallback}.</li>
     * <li>Test if the 'sharedState' contains a login name.</li>
     * <li>Fallback: return the anonymous UserID.</li>
     * </ol>
     *
     * @param credentials which, may contain a User-ID
     * @return The userId retrieved from the credentials or by any other means
     * described above.
     * @see #login()
     */
    protected String getUserID(Credentials credentials) {
        String userId = null;
        if (credentials != null) {
            if (credentials instanceof GuestCredentials) {
                userId = anonymousId;
            } else if (credentials instanceof SimpleCredentials) {
                userId = ((SimpleCredentials) credentials).getUserID();
            } else {
                try {
                    NameCallback callback = new NameCallback("User-ID: ");
                    callbackHandler.handle(new Callback[]{callback});
                    userId = callback.getName();
                } catch (UnsupportedCallbackException e) {
                    log.warn("Credentials- or NameCallback must be supported");
                } catch (IOException e) {
                    log.error("Name-Callback failed: " + e.getMessage());
                }
            }
        }
        if (userId == null && sharedState.containsKey(KEY_LOGIN_NAME)) {
            userId = (String) sharedState.get(KEY_LOGIN_NAME);
        }

        // still no userId -> anonymousID if its has been defined.
        // TODO: check again if correct when used with 'extendedAuth'
        if (userId == null) {
            userId = anonymousId;
        }
        return userId;
    }

    /**
     * Indicate if the given Credentials are considered to be anonymous.
     *
     * @param credentials The Credentials to be tested.
     * @return <code>true</code> if is anonymous; <code>false</code> otherwise.
     */
    protected boolean isAnonymous(Credentials credentials) {
        if (credentials instanceof GuestCredentials) {
            return true;
        } else {
            // TODO: review again. former simple-login-module treated 'null' as anonymous (probably wrong).
            String userId = getUserID(credentials);
            return (anonymousId == null) ? userId == null : anonymousId.equals(userId);
        }
    }


    /**
     * Authentication process associates a Principal to Credentials<br>
     * This method resolves the Principal for the given Credentials. If no valid
     * Principal can be determined, the LoginModule should be ignored.
     *
     * @param credentials Credentials used for to login.
     * @return the principal associated with the given credentials or <code>null</code>.
     */
    protected abstract Principal getPrincipal(Credentials credentials);

    /**
     * @return a Collection of principals that contains the current user
     * principal and all groups it is member of.
     */
    protected Set<Principal> getPrincipals() {
        // use linked HashSet instead of HashSet in order to maintain the order
        // of principals (as in the Subject).
        Set<Principal> principals = new LinkedHashSet<Principal>();
        principals.add(principal);
        PrincipalIterator groups = principalProvider.getGroupMembership(principal);
        while (groups.hasNext()) {
            principals.add(groups.nextPrincipal());
        }
        return principals;
    }

    //--------------------------------------------------------------------------
    /**
     * Returns the admin user id.
     *
     * @return admin user id
     */
    public String getAdminId() {
        return adminId;
    }

    /**
     * Sets the administrator's user id.
     *
     * @param adminId the administrator's user id.
     */
    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    /**
     * Returns the anonymous user id.
     *
     * @return anonymous user id
     */
    public String getAnonymousId() {
        return anonymousId;
    }

    /**
     * Sets the anonymous user id.
     *
     * @param anonymousId anonymous user id
     */
    public void setAnonymousId(String anonymousId) {
        this.anonymousId = anonymousId;
    }

    /**
     * Returns the configured name of the principal provider class.
     *
     * @return name of the principal provider class.
     */
    public String getPrincipalProvider() {
        return principalProviderClassName;
    }

    /**
     * Sets the configured name of the principal provider class
     *
     * @param principalProvider Name of the principal provider class.
     */
    public void setPrincipalProvider(String principalProvider) {
        this.principalProviderClassName = principalProvider;
    }

    /**
     * The name of the credentials attribute providing a hint that the
     * credentials should be taken as is and the user requesting access
     * has already been authenticated outside of this LoginModule.
     * <p>
     * This name is configured as the value of the LoginModule configuration
     * parameter <code>trust_credentials_attribute</code>. If the configuration
     * parameter is missing (or empty) the name is not set and this method
     * returns <code>null</code>.
     *
     * @see #isPreAuthenticated(Credentials)
     * @deprecated For security reasons the support for the preAuth attribute
     * has been deprecated and will no longer be available in a subsequent release.
     * See also <a href="https://issues.apache.org/jira/browse/JCR-3293">JCR-3293</a>
     */
    protected final String getPreAuthAttributeName() {
        return preAuthAttributeName;
    }

    /**
     * Returns <code>true</code> if the credentials should be considered as
     * pre-authenticated and a password check is not required.
     * <p>
     * This base class implementation returns <code>true</code> if the
     * <code>creds</code> object is a SimpleCredentials instance and the
     * configured {@link #getPreAuthAttributeName() trusted
     * credentials property} is set to a non-<code>null</code> value in the
     * credentials attributes.
     * <p>
     * Extensions of this class may overwrite this method to apply more or
     * different checks to the credentials.
     *
     * @param creds The Credentials to check
     *
     * @see #getPreAuthAttributeName()
     * @deprecated For security reasons the support for the preAuth attribute
     * has been deprecated and will no longer be available in a subsequent release.
     * See also <a href="https://issues.apache.org/jira/browse/JCR-3293">JCR-3293</a>
     */
    protected boolean isPreAuthenticated(final Credentials creds) {
        final String preAuthAttrName = getPreAuthAttributeName();
        boolean isPreAuth = preAuthAttrName != null
            && (creds instanceof SimpleCredentials)
            && ((SimpleCredentials) creds).getAttribute(preAuthAttrName) != null;
        if (isPreAuth) {
            log.warn("Usage of deprecated 'trust_credentials_attribute' option. " +
                    "Please note that for security reasons this feature will not" +
                    "be supported in future releases.");
        }
        return isPreAuth;
    }
}
