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
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.value.SerialValueFactory;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteRepository RemoteRepository}
 * interface. This class makes a remote repository locally available using
 * the JCR {@link javax.jcr.Repository Repository} interface.
 *
 * @see javax.jcr.Repository
 * @see org.apache.jackrabbit.rmi.remote.RemoteRepository
 */
public class ClientRepository implements Repository {

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

    /** The adapted remote repository. */
    private final RemoteRepository remote;

    /** Local adapter factory. */
    private final LocalAdapterFactory factory;

    /**
     * Creates a client adapter for the given remote repository.
     *
     * @param remote remote repository
     * @param factory local adapter factory
     */
    public ClientRepository(
            RemoteRepository remote, LocalAdapterFactory factory) {
        this.remote = remote;
        this.factory = factory;
    }

    /** {@inheritDoc} */
    public String getDescriptor(String name) {
        try {
            return remote.getDescriptor(name);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public Value getDescriptorValue(String key) {
        String descriptor = getDescriptor(key);
        if (descriptor != null) {
            return SerialValueFactory.getInstance().createValue(descriptor);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public Value[] getDescriptorValues(String key) {
        try {
            return remote.getDescriptorValues(key);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getDescriptorKeys() {
        try {
            return remote.getDescriptorKeys();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isSingleValueDescriptor(String key) {
        return getDescriptor(key) != null;
    }

    /** {@inheritDoc} */
    public Session login(Credentials credentials, String workspace)
            throws RepositoryException {
        try {
            RemoteSession session = remote.login(credentials, workspace);
            return factory.getSession(this, session);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
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
