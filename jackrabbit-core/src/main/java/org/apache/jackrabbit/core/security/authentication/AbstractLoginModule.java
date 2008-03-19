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

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
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
import java.io.IOException;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This Abstract class provides the means for the common Authentication tasks,
 * within the Repository.<p />
 * It implements an authentication by User-ID / Password - Credentials
 * {@link SimpleCredentials}<p />
 * On successfull authentication it relates this credentials to principals
 * by the use of * the {@link PrincipalProvider} configured for this LoginModule<p />
 * Jackrabbit knows about two typs of Login, the one for its "own" Credentials and one
 * for Impersonation.<br>
 * {@link #login()}-method dispatches to
 * {@link #authenticate(java.security.Principal, javax.jcr.Credentials)} and
 * {@link #impersonate(java.security.Principal, javax.jcr.Credentials)} for the
 * two cases for implemenations.<br>
 * This LoginModule imlements default behaviours for this methods.
 *
 * @see LoginModule
 */
public abstract class AbstractLoginModule implements LoginModule {

    private static final Logger log = LoggerFactory.getLogger(AbstractLoginModule.class);

    private static final String KEY_SIMPLE_CREDENTIALS = "org.apache.jackrabbit.credentials.simple";
    private static final String KEY_LOGIN_NAME = "javax.security.auth.login.name";

    protected String anonymousId;
    protected String defaultUserId;

    private CallbackHandler callbackHandler;
    private boolean initialized;

    protected Principal principal;
    protected SimpleCredentials credentials;
    protected Subject subject;
    protected PrincipalProvider principalProvider;

    private Map sharedState;

    /**
     * Initialize this LoginModule.<br> This abstract implementation, initalizes
     * the following fields for later use:
     * <ul>
     * <li>{@link PrincipalManager} for group-membership resoultion</li>
     * <li>{@link PrincipalProvider} for user-{@link Principal} resolution.</li>
     * <li>{@link LoginModuleConfig#PARAM_DEFAULT_USERID} option is evaluated</li>
     * <li>{@link LoginModuleConfig#PARAM_ADMIN_ID} option is evaluated</li>
     * <li>{@link LoginModuleConfig#PARAM_ANONYMOUS_ID} option is evaluated</li>
     * </ul>
     * Implementations are called via
     * {@link #doInit(CallbackHandler, Session, Map)} to implement
     * additional initalization
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
                           Map sharedState, Map options) {
        try {
            log.debug("Initalize LoginModule: ");
            //Properties configProps = new Properties();
            //configProps.putAll(options);
            RepositoryCallback repositoryCb = new RepositoryCallback();
            callbackHandler.handle(new Callback[]{repositoryCb});

            // retrieve the principal-provider configured for this module
            PrincipalProviderRegistry registry = repositoryCb.getPrincipalProviderRegistry();
            if (options.containsKey(LoginModuleConfig.PARAM_PRINCIPAL_PROVIDER_CLASS)) {
                String providerName = (String) options.get(LoginModuleConfig.PARAM_PRINCIPAL_PROVIDER_CLASS);
                principalProvider = registry.getProvider(providerName);
            }
            if (principalProvider == null) {
                principalProvider = registry.getDefault();
                if (principalProvider==null) {
                    return; // abort. not even a default principal provider
                }
            }
            log.debug("- PrincipalProvider -> '" + principalProvider.getClass().getName() + "'");

            //call implementation for additional setup
            doInit(callbackHandler, repositoryCb.getSession(), options);

            if (options.containsKey(LoginModuleConfig.PARAM_ANONYMOUS_ID)) {
                anonymousId = (String) options.get(LoginModuleConfig.PARAM_ANONYMOUS_ID);
            }
            if (options.containsKey(LoginModuleConfig.PARAM_DEFAULT_USERID)) {
                defaultUserId = (String) options.get(LoginModuleConfig.PARAM_DEFAULT_USERID);
            }

            //common jaas state variables
            this.callbackHandler = callbackHandler;
            this.subject = subject;

            //log config values for debug
            if (log.isDebugEnabled()) {
                Iterator itr = options.keySet().iterator();
                while (itr.hasNext()) {
                    String option = (String) itr.next();
                    log.debug("- Option: "+ option +" -> '"+ options.get(option) +"'");

                }
            }

            this.sharedState = sharedState;
            initialized = (this.subject != null);

        } catch (Exception e) {
            log.error("LoginModule failed to initialize.", e);
        }
    }

    /**
     * Implementations may set-up their own state. E. g. a DataSource if it is
     * authorized against an external System
     *
     * @param callbackHandler as passed by {@link javax.security.auth.login.LoginContext}
     * @param session         to security-workspace of Jackrabbit
     * @param options         options from Logini config
     * @throws LoginException in case initializeaiton failes
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
     * Method to authenticate a <code>Subject</code> (phase 1).<p/>
     * The login is devided into 3 Phases:<p/>
     *
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
     * <li>{@link #getCredentials()}</li> and
     * <li>{@link #getUserID(Credentials)}</li>
     * </ul>
     * <p/>
     *
     * <b>2) User-Principal resolution </b><br>
     * In a second step it is tested, if the resolved User-ID belongs to a User
     * known to the system, i.e. if the {@link PrincipalProvider} has a principal
     * for the given ID and the principal can be found via
     * {@link PrincipalProvider#findPrincipals(String)}.<br>
     * The provider implemenation can be set by the configuration option with the
     * name {@link LoginModuleConfig#PARAM_PRINCIPAL_PROVIDER_CLASS principal_provider.class}.
     * If the option is missing, the system default prinvipal provider will
     * be used.<p/>
     *
     * <b>3) Verfication</b><br>
     * There are two cases, how the User-ID can be verfied:
     * Either the login is the result of an impersonation request (see
     * {@link javax.jcr.Session#impersonate(Credentials)} or of a login to the Repository ({@link
     * javax.jcr.Repository#login(Credentials)}). The concrete implementation
     * of the LoginModule is responsible for both impersonation and login:
     * <ul>
     * <li>{@link #authenticate(Principal, Credentials)}</li>
     * <li>{@link #impersonate(Principal, Credentials)}</li>
     * </ul>
     *
     * Under the following conditions, the login process is aborted and the
     * module is marked to be ignored:
     * <ul>
     * <li>No User-ID could be resolve, and anyonymous access is switched off</li>
     * <li>No Principal is found for the User-ID resolved</li>
     * </ul>
     *
     * Under the follwoing conditions, the login process is marked to be invalid
     * by throwing an LoginException:
     * <ul>
     * <li>It is an impersonation request, but the impersonator is not allowed
     * to impersonate to the requested User-ID</li>
     * <li>The user tries to login, but the Credentials can not be verified.</li>
     * </ul>
     * <p/>
     * The LoginModule keeps the Credentials and the Principal as instance fields,
     * to mark that login has been successfull.
     *
     * @return true if the authentication succeeded, or false if this
     *         <code>LoginModule</code> should be ignored.
     * @throws LoginException if the authentication fails
     * @see LoginModule#login()
     * @see #getCredentials()
     * @see #getUserID(Credentials)
     * @see #getImpersonatorSubject(Credentials)
     */
    public boolean login() throws LoginException {
        if (!isInitialized()) {
            log.warn("Unable to perform login: initialization not completed.");
            return false;
        }

        //check for availablity of Credentials;
        Credentials creds = getCredentials();
        if (creds == null) {
            log.warn("No credentials available -> try default (anonymous) authentication.");
        }
        try {
            Principal userPrincipal = getPrincipal(creds);
            if (userPrincipal == null) {
                // unknown principal or a Group-principal
                log.debug("Unknown User -> ignore.");
                return false;
            }
            boolean authenticated;
            // test for anonymous, impersonation or common authentication.
            if (isAnonymous(creds)) {
                authenticated = true;
            } else if (isImpersonation(creds)) {
                authenticated = impersonate(userPrincipal, creds);
            } else {
                authenticated = authenticate(userPrincipal, creds);
            }

            // process authenticated user or return false
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
     * <p/>
     * <p> This method is called if the LoginContext's overall authentication
     * succeeded (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules succeeded).
     * <p/>
     * <p> If this LoginModule's own authentication attempt succeeded (checked
     * by retrieving the private state saved by the <code>login</code> method),
     * then this method associates relevant Principals and Credentials with the
     * <code>Subject</code> located in the <code>LoginModule</code>.  If this
     * LoginModule's own authentication attempted failed, then this method
     * removes/destroys any state that was originally saved.
     * <p/>
     * The login is considers as succeeded if the credentials field is set. If
     * there is no principalstate. the login is considered as ignored.
     * <p/>
     * The implementation stores the principal associated to the UserID and all
     * the Groups it is member of. {@link PrincipalManager#getGroupMembership(Principal)}
     * An instance of (#link SimpleCredentials} containing only the UserID used
     * to login is set to the Subject's public Credentials
     *
     * @return true if this method succeeded, or false if this
     *         <code>LoginModule</code> should be ignored.
     * @throws LoginException if the commit fails
     * @see LoginModule#commit()
     * @see AbstractLoginModule#login()
     */
    public boolean commit() throws LoginException {
        //check login-state
        if (credentials == null) {
            abort();
        }
        if (!isInitialized() || principal == null) {
            return false;
        }

        subject.getPrincipals().addAll(getPrincipals());
        subject.getPublicCredentials().add(credentials);
        return true;
    }

    /**
     * Method to abort the authentication process (phase 2).
     * <p/>
     * <p> This method is called if the LoginContext's overall authentication
     * failed. (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL
     * LoginModules did not succeed).
     * <p/>
     * <p> If this LoginModule's own authentication attempt succeeded (checked
     * by retrieving the private state saved by the <code>login</code> method),
     * then this method cleans up any state that was originally saved.
     * <p/>
     * <p/>
     *
     * @return true if this method succeeded, or false if this
     *         <code>LoginModule</code> should be ignored.
     * @throws LoginException if the abort fails
     */
    public boolean abort() throws LoginException {
        if (!isInitialized()) {
            return false;
        } else {
            sharedState.remove(KEY_SIMPLE_CREDENTIALS);
            callbackHandler = null;
            principal = null;
            credentials = null;
            return logout();
        }
    }

    /**
     * Method which logs out a <code>Subject</code>.
     * <p/>
     * <p>An implementation of this method might remove/destroy a Subject's
     * Principals and Credentials.
     * <p/>
     * <p/>
     *
     * @return true if this method succeeded, or false if this
     *         <code>LoginModule</code> should be ignored.
     * @throws LoginException if the logout fails
     */
    public boolean logout() throws LoginException {
        Set thisPrincipals = subject.getPrincipals();
        Set thisCredentials = subject.getPublicCredentials(SimpleCredentials.class);
        if (thisPrincipals == null || thisCredentials == null
                || thisPrincipals.isEmpty() || thisCredentials.isEmpty()) {
            return false;
        } else {
            thisPrincipals.clear();
            thisCredentials.clear();
            return true;
        }
    }

    /**
     *
     * @param principal
     * @param credentials
     * @return <code>true</code> if Credentails authenticate,
     *         <code>false</code> if no <code>Authentication</code> can handle
     *         the given <code>Credentials</code>
     * @throws javax.security.auth.login.FailedLoginException
     *          if the authentication failed.
     * @see AbstractLoginModule#getAuthentication(java.security.Principal, javax.jcr.Credentials)
     * @see AbstractLoginModule#authenticate(java.security.Principal, javax.jcr.Credentials)
     */
    protected boolean authenticate(Principal principal, Credentials credentials)
            throws RepositoryException, FailedLoginException {

        Authentication auth = getAuthentication(principal, credentials);
        if(auth == null) {
            return false;
        } else if (auth.authenticate(credentials)){
            return true;
        }
        throw new FailedLoginException();
    }

    /**
     * @return a Collection of principals that contains the current user
     * principal and all groups it is member of.
     */
    protected Set getPrincipals() {
        // use ListOrderedSet instead of Hashset in order to maintain the order
        // of principals (as in the Subject).
        Set principals = new ListOrderedSet();
        principals.add(principal);
        Iterator groups = principalProvider.getGroupMembership(principal);
        while (groups.hasNext()) {
            principals.add(groups.next());
        }
        return principals;
    }

    /**
     * Test if the current request is an Impersonation attempt. The default
     * implementation returns <code>true</code> if an
     * {@link #getImpersonatorSubject(Credentials) subject} for the
     * impersonation can be retrieved.<p/>
     *
     * @param credentials potentially containing impersonation data
     * @return true if this is an impersonation attempt
     * @see #getImpersonatorSubject(Credentials)
     */
    protected boolean isImpersonation(Credentials credentials) {
        return getImpersonatorSubject(credentials) != null;
    }

    /**
     * Handles the impersonation of given Credentials.<p />
     * Current implementation takes {@link User} for the given Principal and
     * delegates the check to {@link Impersonation#allows(javax.security.auth.Subject)} }
     *
     * @param principal
     * @param credentials
     * @return false, if there is no User to impersonate,
     *         true if impersonation is allowed
     * @throws RepositoryException
     * @throws FailedLoginException if credentials don't allow to impersonate to principal
     */
    abstract protected boolean impersonate(Principal principal, Credentials credentials)
            throws RepositoryException, LoginException;

    /**
     *
     * @param principal
     * @param creds
     * @return
     * @throws RepositoryException
     */
    abstract protected Authentication getAuthentication(Principal principal, Credentials creds)
            throws RepositoryException;

    /**
     * Method tries to acquire an Impersonator in the follwing order:
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

    //------------------------------------------------------------< private >---
    /**
     * Method tries to resolve the {@link Credentials} used for login. It takes
     * into account, that an authentication-extension of an allready
     * authenticate {@link Subject} could take place<p/> Therefore the
     * credentials are searchred for in the following search-order: <ol> <li>
     * Ask CallbackHandler for Credentials with use of {@link
     * CredentialsCallback}. Expects {@link CredentialsCallback#getCredentials}
     * to return an instance of {@link SimpleCredentials}.</li> <li> Ask the
     * Subject for its public credentials {@link Subject#getPublicCredentials(Class)},
     * with {@link SimpleCredentials#getClass()} as argument.<p> This enables to
     * preauthenticate the Subject.</li> </ol> NOTE: While the method signiture
     * works with {@link Credentials} it actually searches and returns {@link
     * SimpleCredentials}.<br> This is done to allow implementations to make use
     * of this abstract class, without beeing bound to a {@link Credentials}
     * implementation.
     *
     * @return Credentials or null if not found
     * @see #login()
     */
    private Credentials getCredentials() {
        SimpleCredentials credentials = null;
        if (sharedState.containsKey(KEY_SIMPLE_CREDENTIALS)) {
            credentials = (SimpleCredentials) sharedState.get(KEY_SIMPLE_CREDENTIALS);
        } else {
            try {
                CredentialsCallback callback = new CredentialsCallback();
                callbackHandler.handle(new Callback[]{callback});
                Credentials creds = callback.getCredentials();
                if (null != creds && creds instanceof SimpleCredentials) {
                    credentials = (SimpleCredentials) creds;
                    sharedState.put(KEY_SIMPLE_CREDENTIALS, credentials);
                }
            } catch (UnsupportedCallbackException e) {
                log.warn("Credentials-Callback not supported try Name-Callback");
            } catch (IOException e) {
                log.error("Credentials-Callback failed: " + e.getMessage() + ": try Name-Callback");
            }
        }
        // ask subject if still no credentials
        if (null == credentials) {
            Set preAuthCreds = subject.getPublicCredentials(SimpleCredentials.class);
            if (!preAuthCreds.isEmpty()) {
                credentials = (SimpleCredentials) subject.getPublicCredentials(SimpleCredentials.class).iterator().next();
            }
        }
        return credentials;
    }

    /**
     * Method supports tries to acquire a UserID in the follwing order: <ol>
     * <li>Try to access it from the {@link Credentials} via {@link
     * SimpleCredentials#getUserID()}</li>
     * <li>Ask CallbackHandler for User-ID with use of {@link NameCallback}.</li>
     * <li>Test if the 'sharedState' contains a login name.</li>
     * <li>Test a defaultUserID is present in the LoginModule configuration.</li>
     * <li>Fallback: return the anonymous UserID.</li>
     * </ol>
     *
     * @param credentials which, may contain a User-ID
     * @return The userId retrieved from the credentials or by any other means
     * described above.
     * @see #login()
     */
    private String getUserID(Credentials credentials) {
        String userId = null;
        if (credentials != null) {
            if (credentials instanceof SimpleCredentials) {
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

        // still no userId -> if a defaultUserID has been specified or return
        // the anonymous UserID.
        // TODO: check again if correct when used with 'extendedAuth'
        if (userId == null) {
            if (defaultUserId != null) {
                userId = defaultUserId;
            } else {
                userId = anonymousId;
            }
        }

        return userId;
    }

    /**
     * Indicate if the given Credentials are considered to be anonymous.
     *
     * @param credentials
     * @return true if is anonymous
     */
    private boolean isAnonymous(Credentials credentials) {
        // TODO: check again. former simple-login-module treated 'null' as anonymous and had no anonymous config entry.
        String userId = getUserID(credentials);
        if (anonymousId == null) {
            return userId == null;
        } else {
            return anonymousId.equals(userId);
        }
    }

    /**
     * Authentication process associates a Principal to Credentials<br>
     * This method resolves the Principal for the given Credentials. If there
     * is no Principal for the Credentials, the LoginModule should be ignored.<p>
     * This Abstract implementation uses the {@link PrincipalProvider} configured
     * for it, to resolve this association.
     * It takes the {@link PrincipalProvider#findPrincipals(String)} for the User-ID
     * resolved by  {@link #getUserID(Credentials)}
     *
     * @param credentials
     * @return if credentials are associated to one or null if none found
     */
    private Principal getPrincipal(Credentials credentials) {
        Principal principal = null;
        String userId = isAnonymous(credentials) ? anonymousId : getUserID(credentials);
        PrincipalIterator res = principalProvider.findPrincipals(userId, PrincipalManager.SEARCH_TYPE_NOT_GROUP);
        if (res.hasNext()) {
            principal = res.nextPrincipal();
        } // no matching principal -> return null
        return principal;
    }
}
