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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.AbstractRepository;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A repository proxy that automatically initializes and shuts down the
 * underlying repository instance when the first session is opened
 * or the last one closed. As long as all sessions are properly closed
 * when no longer used, this class can be used to avoid having to explicitly
 * shut down the repository.
 */
public class TransientRepository extends AbstractRepository
        implements JackrabbitRepository, SessionListener {

    /**
     * The logger instance used to log the repository and session lifecycles.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(TransientRepository.class);

    /**
     * Name of the repository configuration file property.
     */
    private static final String CONF_PROPERTY =
        "org.apache.jackrabbit.repository.conf";

    /**
     * Default value of the repository configuration file property.
     */
    private static final String CONF_DEFAULT = "repository.xml";

    /**
     * Name of the repository home directory property.
     */
    private static final String HOME_PROPERTY =
        "org.apache.jackrabbit.repository.home";

    /**
     * Default value of the repository home directory property.
     */
    private static final String HOME_DEFAULT = "repository";

    /**
     * Factory interface for creating {@link RepositoryImpl} instances.
     * Used to give greater control of the repository initialization process
     * to users of the TransientRepository class.
     */
    public interface RepositoryFactory {

        /**
         * Creates and initializes a repository instance. The returned instance
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
    private final Map<Session, Session> sessions =
        new ReferenceMap<>(ReferenceStrength.WEAK, ReferenceStrength.WEAK);

    /**
     * The static repository descriptors. The default {@link RepositoryImpl}
     * descriptors are loaded as the static descriptors and used whenever a
     * live repository instance is not available (no open sessions).
     */
    private final Properties descriptors;

    /**
     * The path to the repository home directory.
     */
    private final String home;

    /**
     * Creates a transient repository proxy that will use the given repository
     * factory to initialize the underlying repository instances.
     *
     * @param factory repository factory
     * @param home    the path to the repository home directory.
     */
    public TransientRepository(RepositoryFactory factory, String home) {
        this.factory = factory;
        this.home = home;
        this.repository = null;
        this.descriptors = new Properties();

        // FIXME: The current RepositoryImpl class does not allow static
        // access to the repository descriptors, so we need to load them
        // directly from the underlying property file.
        try {
            InputStream in = RepositoryImpl.class.getResourceAsStream(
                    "repository.properties");
            try {
                descriptors.load(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            logger.warn("Unable to load static repository descriptors", e);
        }
    }

    /**
     * Creates a transient repository proxy that will use the repository
     * configuration file and home directory specified in system properties
     * <code>org.apache.jackrabbit.repository.conf</code> and
     * <code>org.apache.jackrabbit.repository.home</code>. If these properties
     * are not found, then the default values "<code>repository.xml</code>"
     * and "<code>repository</code>" are used.
     */
    public TransientRepository() {
        this(System.getProperty(CONF_PROPERTY, CONF_DEFAULT),
             System.getProperty(HOME_PROPERTY, HOME_DEFAULT));
    }

    /**
     * Creates a transient repository proxy that will use a copy of the given 
     * repository configuration to initialize the underlying repository 
     * instance.
     *
     * @param config repository configuration
     */
    public TransientRepository(final RepositoryConfig config) {
        this(new RepositoryFactory() {
            public RepositoryImpl getRepository() throws RepositoryException {
                return RepositoryImpl.create(RepositoryConfig.create(config));
            }
        }, config.getHomeDir());
    }

    /**
     * Creates a transient repository proxy that will use the given repository
     * configuration file and home directory paths to initialize the underlying
     * repository instances.
     *
     * @see #TransientRepository(File, File)
     * @param config repository configuration file
     * @param home repository home directory
     */
    public TransientRepository(String config, String home) {
        this(new File(config), new File(home));
    }

    /**
     * Creates a transient repository proxy based on the given repository
     * home directory and the repository configuration file "repository.xml"
     * contained in that directory.
     *
     * @since Apache Jackrabbit 1.6
     * @param dir repository home directory
     */
    public TransientRepository(File dir) {
        this(new File(dir, "repository.xml"), dir);
    }

    /**
     * Creates a transient repository proxy that will use the given repository
     * configuration file and home directory paths to initialize the underlying
     * repository instances. The repository configuration file is reloaded
     * whenever the repository is restarted, so it is safe to modify the
     * configuration when all sessions have been closed.
     * <p>
     * If the given repository configuration file does not exist, then a
     * default configuration file is copied to the given location when the
     * first session starts. Similarly, if the given repository home
     * directory does not exist, it is automatically created when the first
     * session starts. This is a convenience feature designed to reduce the
     * need for manual configuration.
     *
     * @since Apache Jackrabbit 1.6
     * @param xml repository configuration file
     * @param dir repository home directory
     */
    public TransientRepository(final File xml, final File dir) {
        this(new RepositoryFactory() {
            public RepositoryImpl getRepository() throws RepositoryException {
                try {
                    return RepositoryImpl.create(
                            RepositoryConfig.install(xml, dir));
                } catch (IOException e) {
                    throw new RepositoryException(
                            "Automatic repository configuration failed", e);
                } catch (ConfigurationException e) {
                    throw new RepositoryException(
                            "Invalid repository configuration file: " + xml, e);
                }
            }
        }, dir.getAbsolutePath());
    }

    public TransientRepository(final Properties properties)
            throws ConfigurationException, IOException {
        this(new RepositoryFactory() {
            public RepositoryImpl getRepository() throws RepositoryException {
                try {
                    return RepositoryImpl.create(
                            RepositoryConfig.install(properties));
                } catch (IOException e) {
                    throw new RepositoryException(
                            "Automatic repository configuration failed: "
                            + properties, e);
                } catch (ConfigurationException e) {
                    throw new RepositoryException(
                            "Invalid repository configuration: "
                            + properties, e);
                }
            }
        }, RepositoryConfig.getRepositoryHome(properties).getAbsolutePath());
    }

    /**
     * @return the path to the repository home directory.
     */
    public String getHomeDir() {
        return home;
    }

    /**
     * Starts the underlying repository.
     *
     * @throws RepositoryException if the repository cannot be started
     */
    private synchronized void startRepository() throws RepositoryException {
        assert repository == null && sessions.isEmpty();
        logger.debug("Initializing transient repository");
        repository = factory.getRepository();
        logger.info("Transient repository initialized");
    }

    /**
     * Stops the underlying repository.
     */
    private synchronized void stopRepository() {
        assert repository != null && sessions.isEmpty();
        logger.debug("Shutting down transient repository");
        repository.shutdown();
        logger.info("Transient repository shut down");
        repository = null;
    }

    //------------------------------------------------------------<Repository>

    /**
     * Returns the available descriptor keys. If the underlying repository
     * is initialized, then the call is proxied to it, otherwise the static
     * descriptor keys are returned.
     *
     * @return descriptor keys
     */
    public synchronized String[] getDescriptorKeys() {
        if (repository != null) {
            return repository.getDescriptorKeys();
        } else {
            String[] keys = Collections.list(
                    descriptors.propertyNames()).toArray(new String[0]);
            Arrays.sort(keys);
            return keys;
        }
    }

    /**
     * Returns the identified repository descriptor. If the underlying
     * repository is initialized, then the call is proxied to it, otherwise
     * the static descriptors are used.
     *
     * @param key descriptor key
     * @return descriptor value
     * @see javax.jcr.Repository#getDescriptor(String)
     */
    public synchronized String getDescriptor(String key) {
        if (repository != null) {
            return repository.getDescriptor(key);
        } else {
            return descriptors.getProperty(key);
        }
    }

    public Value getDescriptorValue(String key) {
        if (repository != null) {
            return repository.getDescriptorValue(key);
        } else {
            throw new UnsupportedOperationException(
                    "not implemented yet - see JCR-2062");
        }
    }

    public Value[] getDescriptorValues(String key) {
        if (repository != null) {
            return repository.getDescriptorValues(key);
        } else {
            throw new UnsupportedOperationException(
                    "not implemented yet - see JCR-2062");
        }
    }

    public boolean isSingleValueDescriptor(String key) {
        if (repository != null) {
            return repository.isSingleValueDescriptor(key);
        } else {
            throw new UnsupportedOperationException(
                    "not implemented yet - see JCR-2062");
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
     * @see javax.jcr.Repository#login(Credentials,String)
     */
    public synchronized Session login(
            Credentials credentials, String workspaceName)
            throws RepositoryException {
        // Start the repository if this is the first login
        if (repository == null) {
            startRepository();
        }

        try {
            logger.debug("Opening a new session");
            SessionImpl session = (SessionImpl) repository.login(
                    credentials, workspaceName);
            sessions.put(session, session);
            session.addListener(this);
            logger.info("Session opened");

            return session;
        } finally {
            // Stop the repository if the login failed
            // and no other sessions are active
            if (sessions.isEmpty()) {
                stopRepository();
            }
        }
    }

    //--------------------------------------------------<JackrabbitRepository>

    /**
     * Forces all active sessions to logout. Once the last session has logged
     * out, the underlying repository instance will automatically be shut down.
     *
     * @see Session#logout()
     */
    public synchronized void shutdown() {
        Session[] copy = sessions.keySet().toArray(new Session[0]);
        for (Session session : copy) {
            session.logout();
        }
    }

    //-------------------------------------------------------<SessionListener>

    /**
     * Removes the given session from the set of open sessions. If no open
     * sessions remain, then the underlying repository instance is shut down.
     *
     * @param session closed session
     * @see SessionListener#loggedOut(SessionImpl)
     */
    public synchronized void loggedOut(SessionImpl session) {
        assert sessions.containsKey(session);
        sessions.remove(session);
        logger.info("Session closed");
        if (sessions.isEmpty()) {
            // FIXME: This is an ugly hack to avoid an infinite loop when
            // RepositoryImpl.shutdown() repeatedly calls logout() on all
            // remaining active sessions including the one that just emitted
            // the loggedOut() message to us!
            repository.loggedOut(session);

            stopRepository();
        }
    }

    /**
     * Ignored. {@inheritDoc}
     */
    public void loggingOut(SessionImpl session) {
    }

    /**
     * Get the current repository.
     *
     * @return the repository
     */
    RepositoryImpl getRepository() {
        return repository;
    }

}
