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
package org.apache.jackrabbit.commons.repository;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.commons.AbstractRepository;
import org.apache.jackrabbit.commons.JcrUtils;

/**
 * Repository that proxies all method calls to another repository.
 * The other repository is accessed lazily using a
 * {@link RepositoryFactory repository factory}.
 *
 * @since 1.4
 */
public class ProxyRepository extends AbstractRepository {

    /**
     * Factory for accessing the proxied repository.
     */
    private final RepositoryFactory factory;

    /**
     * Repository access parameters. Used if an explicit repository
     * factory has not been configured.
     */
    private final Map<String, String> parameters =
        new HashMap<String, String>();

    /**
     * Creates a proxy for the repository (or repositories) accessible
     * through the given factory.
     *
     * @param factory repository factory
     */
    public ProxyRepository(RepositoryFactory factory) {
        this.factory = factory;
    }

    /**
     * Creates a proxy for the repository (or repositories) accessible
     * using the given repository parameters.
     *
     * @param parameters repository parameters
     */
    public ProxyRepository(Map<String, String> parameters) {
        this.factory = null;
        this.parameters.putAll(parameters);
    }

    /**
     * Creates a proxy for the repository accessible using the given
     * repository URI.
     *
     * @param uri repository URI
     */
    public ProxyRepository(String uri) {
        this.factory = null;
        this.parameters.put(JcrUtils.REPOSITORY_URI, uri);
    }

    /**
     * Protected constructor for subclasses that want to override the
     * {@link #getRepository()} method.
     */
    protected ProxyRepository() {
        this.factory = null;
    }

    /**
     * Returns the proxied repository. Subclasses can override this
     * method to implement custom repository access mechanisms.
     *
     * @return repository
     * @throws RepositoryException if the repository can not be accessed
     */
    protected Repository getRepository() throws RepositoryException {
        if (factory != null) {
            return factory.getRepository();
        } else {
            return JcrUtils.getRepository(parameters);
        }
    }

    /**
     * Returns the descriptor keys of the proxied repository, or an empty
     * array if the proxied repository can not be accessed.
     *
     * @return descriptor keys (possibly empty)
     */
    public String[] getDescriptorKeys() {
        try {
            return getRepository().getDescriptorKeys();
        } catch (RepositoryException e) {
            return new String[0];
        }
    }

    /**
     * Checks whether the given key identifies a valid single-valued
     * descriptor key in the proxied repository. Returns <code>false</code>
     * if the proxied repository can not be accessed.
     *
     * @return <code>true</code> if the key identifies a valid single-valued
     *         descriptor in the proxied repository,
     *         <code>false</code> otherwise
     */
    public boolean isSingleValueDescriptor(String key) {
        try {
            return getRepository().isSingleValueDescriptor(key);
        } catch (RepositoryException e) {
            return false;
        }
    }

    /**
     * Returns the descriptor with the given key from the proxied repository.
     * Returns <code>null</code> if the descriptor does not exist or if the
     * proxied repository can not be accessed.
     *
     * @param key descriptor key
     * @return descriptor value, or <code>null</code>
     */
    public String getDescriptor(String key) {
        try {
            return getRepository().getDescriptor(key);
        } catch (RepositoryException e) {
            return null;
        }
    }

    /**
     * Returns the value of the descriptor with the given key from the proxied
     * repository. Returns <code>null</code> if the descriptor does not exist
     * or if the proxied repository can not be accessed.
     *
     * @param key descriptor key
     * @return descriptor value, or <code>null</code>
     */
    public Value getDescriptorValue(String key) {
        try {
            return getRepository().getDescriptorValue(key);
        } catch (RepositoryException e) {
            return null;
        }
    }

    /**
     * Returns the values of the descriptor with the given key from the proxied
     * repository. Returns <code>null</code> if the descriptor does not exist
     * or if the proxied repository can not be accessed.
     *
     * @param key descriptor key
     * @return descriptor values, or <code>null</code>
     */
    public Value[] getDescriptorValues(String key) {
        try {
            return getRepository().getDescriptorValues(key);
        } catch (RepositoryException e) {
            return null;
        }
    }

    /**
     * Logs in to the proxied repository and returns the resulting session.
     * <p>
     * Note that the {@link Session#getRepository()} method of the resulting
     * session will return the proxied repository, not this repository proxy!
     *
     * @throws RepositoryException if the proxied repository can not be
     *                             accessed, or if the login in the proxied
     *                             repository fails
     */
    public Session login(Credentials credentials, String workspace)
            throws RepositoryException {
        return getRepository().login(credentials, workspace);
    }

}
