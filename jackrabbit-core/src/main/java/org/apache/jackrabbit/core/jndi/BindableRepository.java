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
package org.apache.jackrabbit.core.jndi;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.ConfigurationException;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A referenceable and serializable content repository proxy.
 * This class implements the Proxy design pattern (GoF) for the
 * Jackrabbit Repository implementation. The proxy implementation
 * delays the instantiation of the actual Repository instance and
 * implements serialization and JNDI referenceability by keeping
 * track of the repository configuration parameters.
 * <p/>
 * A BindableRepository instance contains the configuration file
 * and home directory paths of a Jackrabbit repository. The separate
 * {@link #init() init()} method is used to create a transient
 * {@link RepositoryImpl RepositoryImpl} instance to which all the
 * JCR API calls are delegated.
 * <p/>
 * An instance of this class is normally always also initialized.
 * The uninitialized state is only used briefly during the static
 * {@link #create(String, String) create} method and during
 * serialization and JNDI "referenciation".
 * <p/>
 * A JVM shutdown hook is used to make sure that the initialized
 * repository is properly closed when the JVM shuts down. The
 * {@link RegistryHelper#unregisterRepository(javax.naming.Context, String)}
 * method should be used to explicitly close the repository if
 * needed.
 */
class BindableRepository implements Repository, Referenceable, Serializable {

    /**
     * The serialization UID of this class.
     */
    static final long serialVersionUID = -2298220550793843166L;

    /**
     * The repository configuration file path.
     */
    private final String configFilePath;

    /**
     * The repository home directory path.
     */
    private final String repHomeDir;

    /**
     * type of <code>configFilePath</code> reference address (@see <code>{@link Reference#get(String)}</code>
     */
    static final String CONFIGFILEPATH_ADDRTYPE = "configFilePath";
    /**
     * type of <code>repHomeDir</code> reference address (@see <code>{@link Reference#get(String)}</code>
     */
    static final String REPHOMEDIR_ADDRTYPE = "repHomeDir";

    /**
     * The delegate repository instance. Created by {@link #init() init}.
     */
    protected transient Repository delegatee;

    /**
     * Thread that is registered as shutdown hook after {@link #init} has been
     * called.
     */
    private transient Thread hook;

    /**
     * Creates a BindableRepository instance with the given configuration
     * information, but does not create the underlying repository instance.
     *
     * @param configFilePath repository configuration file path
     * @param repHomeDir     repository home directory path
     */
    protected BindableRepository(String configFilePath, String repHomeDir) {
        this.configFilePath = configFilePath;
        this.repHomeDir = repHomeDir;
        delegatee = null;
    }

    /**
     * Creates an initialized BindableRepository instance using the given
     * configuration information.
     *
     * @param configFilePath repository configuration file path
     * @param repHomeDir     repository home directory path
     * @return initialized repository instance
     * @throws RepositoryException if the repository cannot be created
     */
    static BindableRepository create(String configFilePath, String repHomeDir)
            throws RepositoryException {
        BindableRepository rep = new BindableRepository(configFilePath, repHomeDir);
        rep.init();
        return rep;
    }

    /**
     * Creates the underlying repository instance. A shutdown hook is
     * registered to make sure that the initialized repository gets closed
     * when the JVM shuts down.
     *
     * @throws RepositoryException if the repository cannot be created
     */
    protected void init() throws RepositoryException {
        RepositoryConfig config = createRepositoryConfig(configFilePath, repHomeDir);
        delegatee = createRepository(config);
        hook = new Thread() {
            public void run() {
                shutdown();
            }
        };

        Runtime.getRuntime().addShutdownHook(hook);
    }

    /**
     * Creates a repository configuration from a path to the repository.xml file
     * and the repository home directory.
     *
     * @param configFilePath path to the repository.xml file.
     * @param repHomeDir     the repository home directory.
     * @return the repository configuration.
     * @throws ConfigurationException on configuration error.
     */
    protected RepositoryConfig createRepositoryConfig(String configFilePath,
                                                      String repHomeDir)
            throws ConfigurationException {
        return RepositoryConfig.create(configFilePath, repHomeDir);
    }

    /**
     * Creates a plain repository instance from a repository
     * <code>config</code>.
     *
     * @param config the repository configuration.
     * @return the repository instance.
     * @throws RepositoryException if an error occurs while creating the
     *                             repository instance.
     */
    protected Repository createRepository(RepositoryConfig config)
            throws RepositoryException {
        return RepositoryImpl.create(config);
    }

    //-----------------------------------------------------------< Repository >

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public Session login(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return delegatee.login(credentials, workspaceName);
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public Session login(String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return delegatee.login(workspaceName);
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public Session login() throws LoginException, RepositoryException {
        return delegatee.login();
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public Session login(Credentials credentials)
            throws LoginException, RepositoryException {
        return delegatee.login(credentials);
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public String getDescriptor(String key) {
        return delegatee.getDescriptor(key);
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public String[] getDescriptorKeys() {
        return delegatee.getDescriptorKeys();
    }

    //--------------------------------------------------------< Referenceable >

    /**
     * Creates a JNDI reference for this content repository. The returned
     * reference holds the configuration information required to create a
     * copy of this instance.
     *
     * @return the created JNDI reference
     */
    public Reference getReference() {
        Reference ref = new Reference(BindableRepository.class.getName(),
                BindableRepositoryFactory.class.getName(),
                null); // no classpath defined
        ref.add(new StringRefAddr(CONFIGFILEPATH_ADDRTYPE, configFilePath));
        ref.add(new StringRefAddr(REPHOMEDIR_ADDRTYPE, repHomeDir));
        return ref;
    }

    //-------------------------------------------------< Serializable support >

    /**
     * Serializes the repository configuration. The default serialization
     * mechanism is used, as the underlying delegate repository is referenced
     * using a transient variable.
     *
     * @param out the serialization stream
     * @throws IOException on IO errors
     * @see Serializable
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // delegate to default implementation
        out.defaultWriteObject();
    }

    /**
     * Deserializes a repository instance. The repository configuration
     * is deserialized using the standard deserialization mechanism, and
     * the underlying delegate repository is created using the
     * {@link #init() init} method.
     *
     * @param in the serialization stream
     * @throws IOException            if configuration information cannot be deserialized
     *                                or if the configured repository cannot be created
     * @throws ClassNotFoundException on deserialization errors
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // delegate deserialization to default implementation
        in.defaultReadObject();
        // initialize reconstructed instance
        try {
            init();
        } catch (RepositoryException re) {
            // failed to reinstantiate repository
            throw new IOException(re.getMessage());
        }
    }

    /**
     * Delegated to the underlying repository instance.
     */
    void shutdown() {
        ((RepositoryImpl) delegatee).shutdown();
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException e) {
            // ignore. exception is thrown when hook itself calls shutdown
        }
    }
}
