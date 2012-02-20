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

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

import org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager RemoteNodeTypeManager}
 * interface. This class makes a remote node type manager locally available
 * using the JCR {@link javax.jcr.nodetype.NodeTypeManager NodeTypeManager}
 * interface.
 *
 * @see javax.jcr.nodetype.NodeTypeManager
 * @see org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager
 */
public class ClientNodeTypeManager extends ClientObject
        implements NodeTypeManager {

    /** The adapted remote node type manager. */
    private RemoteNodeTypeManager remote;

    /**
     * Creates a local adapter for the given remote node type manager.
     *
     * @param remote remote node type manager
     * @param factory local adapter factory
     */
    public ClientNodeTypeManager(
            RemoteNodeTypeManager remote, LocalAdapterFactory factory) {
        super(factory);
        this.remote = remote;
    }

    /** {@inheritDoc} */
    public NodeType getNodeType(String name) throws RepositoryException {
        try {
            return getFactory().getNodeType(remote.getNodeType(name));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        try {
            return getFactory().getNodeTypeIterator(remote.getAllNodeTypes());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        try {
            return getFactory().getNodeTypeIterator(remote.getPrimaryNodeTypes());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        try {
            return getFactory().getNodeTypeIterator(remote.getMixinNodeTypes());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public NodeDefinitionTemplate createNodeDefinitionTemplate()
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

    public NodeTypeTemplate createNodeTypeTemplate()
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

    public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

    public PropertyDefinitionTemplate createPropertyDefinitionTemplate()
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

    public boolean hasNodeType(String name) throws RepositoryException {
        try {
            return remote.hasNodeType(name);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public NodeType registerNodeType(
            NodeTypeDefinition ntd, boolean allowUpdate)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

    public NodeTypeIterator registerNodeTypes(
            NodeTypeDefinition[] ntds, boolean allowUpdate)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

    public void unregisterNodeType(String name) throws RepositoryException {
        unregisterNodeTypes(new String[] { name });
    }

    public void unregisterNodeTypes(String[] names) throws RepositoryException {
        try {
            remote.unregisterNodeTypes(names);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

}
