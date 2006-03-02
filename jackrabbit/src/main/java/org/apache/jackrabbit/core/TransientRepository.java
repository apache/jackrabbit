/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.log4j.Logger;

/**
 * A repository proxy that automatically initializes and shuts down the
 * underlying repository instance when the first session is opened
 * or the last one closed. As long as all sessions are properly closed
 * when no longer used, this class can be used to avoid having to explicitly
 * shut down the repository.
 */
public class TransientRepository implements Repository {

    /**
     * The logger instance used to log the repository and session lifecycles.
     */
    private static final Logger logger =
        Logger.getLogger(TransientRepository.class);

    /**
     * Factory interface for creating {@link RepositoryImpl} instances.
     * Used to give greater control of the repository initialization process
     * to users of the TransientRepository class.
     */
    public interface RepositoryFactory {

        /**
         * Creates and intializes a repository instance. The returned instance
         * will be used and finally shut down by the caller of this method.
         *
         * @return initialized repository instance
         * @throws RepositoryException if an instance can not be created
         */
        RepositoryImpl getRepository() throws RepositoryException;

    }

    /**
     * The repository configuration. Set in the constructor and used to
     * initialize the repository instance when the first session is opened.
     */
    private final RepositoryFactory factory;

    /**
     * The initialized repository instance. Set when the first session is
     * opened and cleared when the last one is closed.
     */
    private RepositoryImpl repository;

    /**
     * The set of open sessions. When no more open sessions remain, the
     * repository instance is automatically shut down until a new session
     * is opened.
     */
    private final Set sessions;

    /**
     * The static repository descriptors. The default {@link RepositoryImpl}
     * descriptors are loaded as the static descriptors and used whenever a
     * live repository instance is not available (no open sessions).
     */
    private final Properties descriptors;

    /**
     * Creates a transient repository proxy that will use the given repository
     * factory to initialize the underlying repository instances.
     * 
     * @param factory repository factory
     * @throws IOException if the static repository descriptors cannot be loaded
     */
    public TransientRepository(RepositoryFactory factory) throws IOException {
        this.factory = factory;
        this.repository = null;
        this.sessions = new HashSet();
        this.descriptors = new Properties();

        // FIXME: The current RepositoryImpl class does not allow static
        // access to the repository descriptors, so we need to load them
        // directly from the underlying property file.
        InputStream in =
            RepositoryImpl.class.getResourceAsStream("repository.properties");
        try {
            descriptors.load(in);
        } finally {
            in.close();
        }
    }

    /**
     * Creates a transient repository proxy that will use the given repository
     * configuration file and home directory paths to initialize the underlying
     * repository instances. The repository configuration file will be reloaded
     * whenever 
     * 
     * @param config repository configuration file
     * @param home repository home directory
     * @throws IOException if the static repository descriptors cannot be loaded
     */
    public TransientRepository(final String config, final String home)
            throws ConfigurationException, IOException {
        this(new RepositoryFactory() {
            public RepositoryImpl getRepository() throws RepositoryException {
                try {
                    RepositoryConfig rc = RepositoryConfig.create(config, home);
                    return RepositoryImpl.create(rc);
                } catch (ConfigurationException e) {
                    throw new RepositoryException(
                            "Invalid repository configuration: " + config, e);
                }
            }
        });
    }

    /**
     * Returns the available descriptor keys. If the underlying repository
     * is initialized, then the call is proxied to it, otherwise the static
     * descriptor keys are returned.
     *
     * @return descriptor keys
     * @see Repository#getDescriptorKeys()
     */
    public synchronized String[] getDescriptorKeys() {
        if (repository != null) {
            return repository.getDescriptorKeys();
        } else {
            List keys = Collections.list(descriptors.propertyNames());
            Collections.sort(keys);
            return (String[]) keys.toArray(new String[keys.size()]);
        }
    }

    /**
     * Returns the identified repository descriptor. If the underlying
     * repository is initialized, then the call is proxied to it, otherwise
     * the static descriptors are used.
     *
     * @param key descriptor key
     * @return descriptor value
     * @see Repository#getDescriptor(String)
     */
    public synchronized String getDescriptor(String key) {
        if (repository != null) {
            return repository.getDescriptor(key);
        } else {
            return descriptors.getProperty(key);
        }
    }

    /**
     * Removes the given session from the set of open sessions. If no open
     * sessions remain, then the underlying repository instance is shut down.
     *
     * @param session closed session
     */
    private synchronized void removeSession(SessionImpl session) {
        sessions.remove(session);
        logger.info("Session closed");
        if (sessions.isEmpty()) {
            // FIXME: This is an ugly hack to avoid an infinite loop when
            // RepositoryImpl.shutdown() repeatedly calls logout() on all
            // remaining active sessions including the one that just emitted
            // the loggedOut() message to us!
            repository.loggedOut(session);

            logger.debug("Shutting down transient repository");
            repository.shutdown();
            logger.info("Transient repository shut down");
            repository = null;
        }
    }

    /**
     * Logs in to the content repository. Initializes the underlying repository
     * instance if needed. The opened session is added to the set of open
     * sessions and a session listener is added to track when the session gets
     * closed.
     *
     * @param credentials login credentials
     * @param workspaceName workspace name
     * @return new session
     * @throws RepositoryException if the session could not be created
     * @see Repository#login(Credentials,String)
     */
    public synchronized Session login(Credentials credentials, String workspaceName)
            throws RepositoryException {
        if (repository == null) {
            logger.debug("Initializing transient repository");
            repository = factory.getRepository();
            logger.info("Transient repository initialized");
        }

        logger.debug("Opening a new session");
        SessionImpl session = (SessionImpl)
            repository.login(credentials, workspaceName);
        sessions.add(session);
        session.addListener(new SessionListener() {
            
            public void loggedOut(SessionImpl session) {
                removeSession(session);
            }
            
            public void loggingOut(SessionImpl session) {
            }
            
        });
        logger.info("Session opened");

        return session;
    }

    /**
     * Calls {@link #login(Credentials, String)} with a <code>null</code>
     * workspace name.
     *
     * @param credentials login credentials
     * @return new session
     * @throws RepositoryException if the session could not be created
     * @see Repository#login(Credentials)
     */
    public Session login(Credentials credentials) throws RepositoryException {
        return login(credentials, null);
    }

    /**
     * Calls {@link #login(Credentials, String)} with <code>null</code> login
     * credentials.
     *
     * @param workspaceName workspace name
     * @return new session
     * @throws RepositoryException if the session could not be created
     * @see Repository#login(String)
     */
    public Session login(String workspaceName) throws RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * Calls {@link #login(Credentials, String)} with <code>null</code> login
     * credentials and a <code>null</code> workspace name.
     *
     * @return new session
     * @throws RepositoryException if the session could not be created
     * @see Repository#login(Credentials)
     */
    public Session login() throws RepositoryException {
        return login(null, null);
    }

}
