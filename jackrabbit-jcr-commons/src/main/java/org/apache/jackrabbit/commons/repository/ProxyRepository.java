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

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.AbstractRepository;

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
     * Creates a proxy for the repository (or repositories) accessible
     * through the given factory.
     *
     * @param factory repository factory
     */
    public ProxyRepository(RepositoryFactory factory) {
        this.factory = factory;
    }

    /**
     * Returns the descriptor keys of the proxied repository, or an empty
     * array if the proxied repository can not be accessed.
     *
     * @return descriptor keys (possibly empty)
     */
    public String[] getDescriptorKeys() {
        try {
            return factory.getRepository().getDescriptorKeys();
        } catch (RepositoryException e) {
            return new String[0];
        }
    }

    /**
     * Returns the descriptor with the given key from the proxied repository.
     * Returns <code>null</code> if the descriptor does not exist or if the
     * proxied repository can not be accessed.
     *
     * @return descriptor value, or <code>null</code>
     */
    public String getDescriptor(String key) {
        try {
            return factory.getRepository().getDescriptor(key);
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
        return factory.getRepository().login(credentials, workspace);
    }

}
