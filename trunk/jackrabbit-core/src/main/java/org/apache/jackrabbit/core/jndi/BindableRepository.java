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

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.AbstractRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.naming.Reference;
import javax.naming.Referenceable;

/**
 * A referenceable and serializable content repository proxy.
 * This class implements the Proxy design pattern (GoF) for the
 * Jackrabbit Repository implementation. The proxy implementation
 * delays the instantiation of the actual Repository instance and
 * implements serialization and JNDI referenceability by keeping
 * track of the repository configuration parameters.
 * <p>
 * A BindableRepository instance contains the configuration file
 * and home directory paths of a Jackrabbit repository. The separate
 * {@link #init() init()} method is used to create a transient
 * {@link RepositoryImpl RepositoryImpl} instance to which all the
 * JCR API calls are delegated.
 * <p>
 * An instance of this class is normally always also initialized.
 * The uninitialized state is only used briefly during the static
 * construction, deserialization, and JNDI "referenciation".
 * <p>
 * A JVM shutdown hook is used to make sure that the initialized
 * repository is properly closed when the JVM shuts down. The
 * {@link RegistryHelper#unregisterRepository(javax.naming.Context, String)}
 * method should be used to explicitly close the repository if
 * needed.
 */
public class BindableRepository extends AbstractRepository
        implements javax.jcr.Repository, JackrabbitRepository, Referenceable, Serializable {

    /**
     * The serialization UID of this class.
     */
    private static final long serialVersionUID = 8864716577016297651L;

    /**
     * type of <code>configFilePath</code> reference address
     * @see Reference#get(String)
     */
    public static final String CONFIGFILEPATH_ADDRTYPE = "configFilePath";

    /**
     * type of <code>repHomeDir</code> reference address
     * @see Reference#get(String)
     */
    public static final String REPHOMEDIR_ADDRTYPE = "repHomeDir";

    /**
     * The repository reference
     */
    private final Reference reference;

    /**
     * The delegate repository instance. Created by {@link #init() init}.
     */
    private transient JackrabbitRepository repository;

    /**
     * Thread that is registered as shutdown hook after {@link #init} has been
     * called.
     */
    private transient Thread hook;

    /**
     * Creates a BindableRepository instance with the configuration
     * information in the given JNDI reference.
     *
     * @param reference JNDI reference
     * @throws RepositoryException if the repository can not be started
     */
    public BindableRepository(Reference reference) throws RepositoryException {
        this.reference = reference;
        init();
    }

    /**
     * Creates the underlying repository instance. A shutdown hook is
     * registered to make sure that the initialized repository gets closed
     * when the JVM shuts down.
     *
     * @throws RepositoryException if the repository cannot be created
     */
    private void init() throws RepositoryException {
        repository = createRepository();
        hook = new Thread() {
            public void run() {
                shutdown();
            }
        };
        Runtime.getRuntime().addShutdownHook(hook);
    }

    /**
     * Creates a repository instance based on the contained JNDI reference.
     * Can be overridden by subclasses to return different repositories.
     * A subclass can access the JNDI reference through the
     * {@link #getReference()} method. The default implementation
     * returns a {@link RepositoryImpl} instance.
     *
     * @return repository instance
     * @throws RepositoryException if the repository could not be created
     */
    protected JackrabbitRepository createRepository()
            throws RepositoryException {
        RepositoryConfig config = RepositoryConfig.create(
                reference.get(CONFIGFILEPATH_ADDRTYPE).getContent().toString(),
                reference.get(REPHOMEDIR_ADDRTYPE).getContent().toString());
        return RepositoryImpl.create(config);
    }

    /**
     * Returns the underlying repository instance. Can be used by subclasses
     * to access the repository instance.
     *
     * @return repository instance
     */
    protected JackrabbitRepository getRepository() {
        return repository;
    }

    //-----------------------------------------------------------< Repository >

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public Session login(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return repository.login(credentials, workspaceName);
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public String getDescriptor(String key) {
        return repository.getDescriptor(key);
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public String[] getDescriptorKeys() {
        return repository.getDescriptorKeys();
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public Value getDescriptorValue(String key) {
        return repository.getDescriptorValue(key);
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public Value[] getDescriptorValues(String key) {
        return repository.getDescriptorValues(key);
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public boolean isSingleValueDescriptor(String key) {
        return repository.isSingleValueDescriptor(key);
    }

    /**
     * Delegated to the underlying repository instance.
     * {@inheritDoc}
     */
    public boolean isStandardDescriptor(String key) {
        return repository.isStandardDescriptor(key);
    }

    //--------------------------------------------------------< Referenceable >

    /**
     * Returns the JNDI reference for this content repository. The returned
     * reference holds the configuration information required to create a
     * copy of this instance.
     *
     * @return the JNDI reference
     */
    public Reference getReference() {
        return reference;
    }

    //-------------------------------------------------< Serializable support >

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
        } catch (RepositoryException e) {
            // failed to reinstantiate repository
            IOException exception = new IOException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * Delegated to the underlying repository instance.
     */
    public void shutdown() {
        BindableRepositoryFactory.removeReference(reference);
        repository.shutdown();
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException e) {
            // ignore. exception is thrown when hook itself calls shutdown
        }
    }
}
