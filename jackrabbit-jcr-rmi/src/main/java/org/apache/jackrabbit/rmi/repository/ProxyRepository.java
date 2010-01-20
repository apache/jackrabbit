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
package org.apache.jackrabbit.rmi.repository;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * Repository that proxies all method calls to another repository.
 * The other repository is accessed lazily using a
 * {@link RepositoryFactory repository factory}.
 *
 * @since 1.4
 */
public class ProxyRepository implements Repository {

    /**
     * The set of standard descriptor keys defined in the
     * {@link Repository} interface. 
     */
    private static final Set<String> STANDARD_KEYS = new HashSet<String>() {{
        add(Repository.IDENTIFIER_STABILITY);
        add(Repository.LEVEL_1_SUPPORTED);
        add(Repository.LEVEL_2_SUPPORTED);
        add(Repository.OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_AUTOCREATED_DEFINITIONS_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_INHERITANCE);
        add(Repository.NODE_TYPE_MANAGEMENT_MULTIPLE_BINARY_PROPERTIES_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_MULTIVALUED_PROPERTIES_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_ORDERABLE_CHILD_NODES_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_OVERRIDES_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_PRIMARY_ITEM_NAME_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_PROPERTY_TYPES);
        add(Repository.NODE_TYPE_MANAGEMENT_RESIDUAL_DEFINITIONS_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_SAME_NAME_SIBLINGS_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_VALUE_CONSTRAINTS_SUPPORTED);
        add(Repository.NODE_TYPE_MANAGEMENT_UPDATE_IN_USE_SUPORTED);
        add(Repository.OPTION_ACCESS_CONTROL_SUPPORTED);
        add(Repository.OPTION_JOURNALED_OBSERVATION_SUPPORTED);
        add(Repository.OPTION_LIFECYCLE_SUPPORTED);
        add(Repository.OPTION_LOCKING_SUPPORTED);
        add(Repository.OPTION_OBSERVATION_SUPPORTED);
        add(Repository.OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED);
        add(Repository.OPTION_QUERY_SQL_SUPPORTED);
        add(Repository.OPTION_RETENTION_SUPPORTED);
        add(Repository.OPTION_SHAREABLE_NODES_SUPPORTED);
        add(Repository.OPTION_SIMPLE_VERSIONING_SUPPORTED);
        add(Repository.OPTION_TRANSACTIONS_SUPPORTED);
        add(Repository.OPTION_UNFILED_CONTENT_SUPPORTED);
        add(Repository.OPTION_UPDATE_MIXIN_NODE_TYPES_SUPPORTED);
        add(Repository.OPTION_UPDATE_PRIMARY_NODE_TYPE_SUPPORTED);
        add(Repository.OPTION_VERSIONING_SUPPORTED);
        add(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED);
        add(Repository.OPTION_XML_EXPORT_SUPPORTED);
        add(Repository.OPTION_XML_IMPORT_SUPPORTED);
        add(Repository.OPTION_ACTIVITIES_SUPPORTED);
        add(Repository.OPTION_BASELINES_SUPPORTED);
        
        add(Repository.QUERY_FULL_TEXT_SEARCH_SUPPORTED);
        add(Repository.QUERY_JOINS);
        add(Repository.QUERY_LANGUAGES);
        add(Repository.QUERY_STORED_QUERIES_SUPPORTED);
        add(Repository.QUERY_XPATH_DOC_ORDER);
        add(Repository.QUERY_XPATH_POS_INDEX);
        add(Repository.REP_NAME_DESC);
        add(Repository.REP_VENDOR_DESC);
        add(Repository.REP_VENDOR_URL_DESC);
        add(Repository.SPEC_NAME_DESC);
        add(Repository.SPEC_VERSION_DESC);
        add(Repository.WRITE_SUPPORTED);
    }};

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
            return factory.getRepository().isSingleValueDescriptor(key);
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
            return factory.getRepository().getDescriptor(key);
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
            return factory.getRepository().getDescriptorValue(key);
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
            return factory.getRepository().getDescriptorValues(key);
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

    /**
     * Returns true if the given key identifies a standard descriptor.
     *
     * @param key descriptor key
     * @return <code>true</code> if the key identifies a standard descriptor,
     *         <code>false</code> otherwise
     */
    public boolean isStandardDescriptor(String key) {
        return STANDARD_KEYS.contains(key);
    }

    /**
     * Calls {@link Repository#login(Credentials, String)} with
     * <code>null</code> arguments.
     *
     * @return logged in session
     * @throws RepositoryException if an error occurs
     */
    public Session login() throws RepositoryException {
        return login(null, null);
    }

    /**
     * Calls {@link Repository#login(Credentials, String)} with
     * the given credentials and a <code>null</code> workspace name.
     *
     * @param credentials login credentials
     * @return logged in session
     * @throws RepositoryException if an error occurs
     */
    public Session login(Credentials credentials) throws RepositoryException {
        return login(credentials, null);
    }

    /**
     * Calls {@link Repository#login(Credentials, String)} with
     * <code>null</code> credentials and the given workspace name.
     *
     * @param workspace workspace name
     * @return logged in session
     * @throws RepositoryException if an error occurs
     */
    public Session login(String workspace) throws RepositoryException {
        return login(null, workspace);
    }

}
