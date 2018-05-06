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

import java.io.InputStream;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.rmi.remote.RemoteLock;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.value.SerialValueFactory;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteNode RemoteNode}
 * interface. This class makes a remote node locally available using
 * the JCR {@link javax.jcr.Node Node} interface.
 *
 * @see javax.jcr.Node
 * @see org.apache.jackrabbit.rmi.remote.RemoteNode
 */
public class ClientNode extends ClientItem implements Node {

    /** The adapted remote node. */
    private RemoteNode remote;

    /**
     * Creates a local adapter for the given remote node.
     *
     * @param session current session
     * @param remote  remote node
     * @param factory local adapter factory
     */
    public ClientNode(
            Session session, RemoteNode remote, LocalAdapterFactory factory) {
        super(session, remote, factory);
        this.remote = remote;
    }

    /**
     * Returns <code>true</code> without contacting the remote node.
     *
     * {@inheritDoc}
     */
    public boolean isNode() {
        return true;
    }

    /**
     * Calls the {@link ItemVisitor#visit(Node) ItemVisitor.visit(Node)}
     * method of the given visitor. Does not contact the remote node, but
     * the visitor may invoke other methods that do contact the remote node.
     *
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    /** {@inheritDoc} */
    public Node addNode(String path) throws RepositoryException {
        try {
            return getNode(getSession(), remote.addNode(path));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node addNode(String path, String type) throws RepositoryException {
        try {
            RemoteNode node = remote.addNode(path, type);
            return getNode(getSession(), node);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void orderBefore(String src, String dst) throws RepositoryException {
        try {
            remote.orderBefore(src, dst);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Value value)
            throws RepositoryException {
        try {
            if (value == null) {
                remote.setProperty(name, value);
                return null;
            } else {
                RemoteProperty property = remote.setProperty(
                        name, SerialValueFactory.makeSerialValue(value));
                return getFactory().getProperty(getSession(), property);
            }
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Value[] values)
            throws RepositoryException {
        try {
            if (values == null) {
                remote.setProperty(name, values);
                return null;
            } else {
                Value[] serials = SerialValueFactory.makeSerialValueArray(values);
                RemoteProperty property = remote.setProperty(name, serials);
                return getFactory().getProperty(getSession(), property);
            }
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, String[] strings)
            throws RepositoryException {
        try {
            if (strings == null) {
                remote.setProperty(name, (Value[]) null);
                return null;
            } else {
                Value[] serials = SerialValueFactory.makeSerialValueArray(strings);
                RemoteProperty property = remote.setProperty(name, serials);
                return getFactory().getProperty(getSession(), property);
            }
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, String value)
            throws RepositoryException {
        if (value == null) {
            return setProperty(name, (Value) null);
        } else {
            return setProperty(name, getSession().getValueFactory().createValue(value));
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, InputStream value)
            throws RepositoryException {
        if (value == null) {
            return setProperty(name, (Value) null);
        } else {
            return setProperty(name, getSession().getValueFactory().createValue(value));
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, boolean value)
            throws RepositoryException {
        return setProperty(name, getSession().getValueFactory().createValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, double value)
            throws RepositoryException {
        return setProperty(name, getSession().getValueFactory().createValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, long value)
            throws RepositoryException {
        return setProperty(name, getSession().getValueFactory().createValue(value));
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Calendar value)
            throws RepositoryException {
        if (value == null) {
            return setProperty(name, (Value) null);
        } else {
            return setProperty(name, getSession().getValueFactory().createValue(value));
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Node value)
            throws RepositoryException {
        if (value == null) {
            return setProperty(name, (Value) null);
        } else {
            return setProperty(name, getSession().getValueFactory().createValue(value));
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Binary value)
            throws RepositoryException {
        if (value == null) {
            return setProperty(name, (Value) null);
        } else {
            return setProperty(
                    name, getSession().getValueFactory().createValue(value));
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, BigDecimal value)
            throws RepositoryException {
        if (value == null) {
            return setProperty(name, (Value) null);
        } else {
            return setProperty(
                    name, getSession().getValueFactory().createValue(value));
        }
    }

    /** {@inheritDoc} */
    public Node getNode(String path) throws RepositoryException {
        try {
            return getNode(getSession(), remote.getNode(path));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator getNodes() throws RepositoryException {
        try {
            return getFactory().getNodeIterator(getSession(), remote.getNodes());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator getNodes(String pattern) throws RepositoryException {
        try {
            return getFactory().getNodeIterator(getSession(), remote.getNodes(pattern));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator getNodes(String[] globs) throws RepositoryException {
        try {
            return getFactory().getNodeIterator(getSession(), remote.getNodes(globs));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property getProperty(String path) throws RepositoryException {
        try {
            RemoteProperty property = remote.getProperty(path);
            return getFactory().getProperty(getSession(), property);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyIterator getProperties() throws RepositoryException {
        try {
            return getFactory().getPropertyIterator(getSession(), remote.getProperties());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyIterator getProperties(String pattern)
            throws RepositoryException {
        try {
            return getFactory().getPropertyIterator(getSession(), remote.getProperties(pattern));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyIterator getProperties(String[] globs)
            throws RepositoryException {
        try {
            return getFactory().getPropertyIterator(getSession(), remote.getProperties(globs));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Item getPrimaryItem() throws RepositoryException {
        try {
            return getItem(getSession(), remote.getPrimaryItem());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getIdentifier() throws RepositoryException {
        try {
            return remote.getIdentifier();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getUUID() throws RepositoryException {
        try {
            return remote.getUUID();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyIterator getReferences() throws RepositoryException {
        try {
            return getFactory().getPropertyIterator(getSession(), remote.getReferences());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyIterator getReferences(String name)
            throws RepositoryException {
        try {
            return getFactory().getPropertyIterator(getSession(), remote.getReferences(name));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasNode(String path) throws RepositoryException {
        try {
            return remote.hasNode(path);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasProperty(String path) throws RepositoryException {
        try {
            return remote.hasProperty(path);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasNodes() throws RepositoryException {
        try {
            return remote.hasNodes();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasProperties() throws RepositoryException {
        try {
            return remote.hasProperties();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        try {
            return getFactory().getNodeType(remote.getPrimaryNodeType());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        try {
            return getNodeTypeArray(remote.getMixinNodeTypes());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isNodeType(String type) throws RepositoryException {
        try {
            return remote.isNodeType(type);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void addMixin(String name) throws RepositoryException {
        try {
            remote.addMixin(name);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeMixin(String name) throws RepositoryException {
        try {
            remote.removeMixin(name);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean canAddMixin(String name) throws RepositoryException {
        try {
            return remote.canAddMixin(name);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeDefinition getDefinition() throws RepositoryException {
        try {
            return getFactory().getNodeDef(remote.getDefinition());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Version checkin() throws RepositoryException {
        try {
            return getFactory().getVersion(getSession(), remote.checkin());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void checkout() throws RepositoryException {
        try {
            remote.checkout();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void update(String workspace) throws RepositoryException {
        try {
            remote.update(workspace);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator merge(String workspace, boolean bestEffort)
            throws RepositoryException {
        try {
            return getFactory().getNodeIterator(getSession(), remote.merge(workspace, bestEffort));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void cancelMerge(Version version) throws RepositoryException {
        try {
            remote.cancelMerge(version.getUUID());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void doneMerge(Version version) throws RepositoryException {
        try {
            remote.doneMerge(version.getUUID());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getCorrespondingNodePath(String workspace)
            throws RepositoryException {
        try {
            return remote.getCorrespondingNodePath(workspace);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public int getIndex() throws RepositoryException {
        try {
            return remote.getIndex();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restore(String version, boolean removeExisting)
            throws RepositoryException {
        try {
            remote.restore(version, removeExisting);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restore(Version version, boolean removeExisting)
            throws RepositoryException {
        try {
            remote.restoreByUUID(version.getUUID(), removeExisting);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restore(Version version, String path, boolean removeExisting)
            throws RepositoryException {
        try {
            remote.restore(version.getUUID(), path, removeExisting);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restoreByLabel(String label, boolean removeExisting)
            throws RepositoryException {
        try {
            remote.restoreByLabel(label, removeExisting);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, String[] strings, int type)
            throws RepositoryException {
        try {
            if (strings == null) {
                remote.setProperty(name, (Value[]) null);
                return null;
            } else {
                Value[] serials = SerialValueFactory.makeSerialValueArray(strings);
                RemoteProperty property = remote.setProperty(name, serials, type);
                return getFactory().getProperty(getSession(), property);
            }
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Value[] values, int type)
            throws RepositoryException {
        try {
            if (values != null) {
                values = SerialValueFactory.makeSerialValueArray(values);
            }
            RemoteProperty property = remote.setProperty(name, values, type);
            if (property != null) {
                return getFactory().getProperty(getSession(), property);
            } else {
                return null;
            }
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, Value value, int type)
            throws RepositoryException {
        try {
            if (value != null) {
                value = SerialValueFactory.makeSerialValue(value);
            }
            RemoteProperty property = remote.setProperty(name, value, type);
            if (property != null) {
                return getFactory().getProperty(getSession(), property);
            } else {
                return null;
            }
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property setProperty(String name, String string, int type)
            throws RepositoryException {
        Value value = null;
        if (string != null) {
            value = getSession().getValueFactory().createValue(string);
        }
        return setProperty(name, value, type);
    }

    /** {@inheritDoc} */
    public boolean isCheckedOut() throws RepositoryException {
        try {
            return remote.isCheckedOut();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public VersionHistory getVersionHistory() throws RepositoryException {
        try {
            return getFactory().getVersionHistory(getSession(), remote.getVersionHistory());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Version getBaseVersion() throws RepositoryException {
        try {
            return getFactory().getVersion(getSession(), remote.getBaseVersion());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Lock lock(boolean isDeep, boolean isSessionScoped)
            throws RepositoryException {
        try {
            RemoteLock lock = remote.lock(isDeep, isSessionScoped);
            return getFactory().getLock(getSession(), lock);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Lock getLock() throws RepositoryException {
        try {
            return getFactory().getLock(getSession(), remote.getLock());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void unlock() throws RepositoryException {
        try {
            remote.unlock();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean holdsLock() throws RepositoryException {
        try {
            return remote.holdsLock();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
     }

    /** {@inheritDoc} */
    public boolean isLocked() throws RepositoryException {
        try {
            return remote.isLocked();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void followLifecycleTransition(String transition)
            throws RepositoryException {
        try {
        	remote.followLifecycleTransition(transition);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getAllowedLifecycleTransistions()
            throws RepositoryException {
        try {
        	return remote.getAllowedLifecycleTransistions();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator getSharedSet() throws RepositoryException {
        try {
            return getFactory().getNodeIterator(getSession(), remote.getSharedSet());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyIterator getWeakReferences() throws RepositoryException {
        try {
            return getFactory().getPropertyIterator(getSession(), remote.getWeakReferences());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public PropertyIterator getWeakReferences(String name)
            throws RepositoryException {
        try {
            return getFactory().getPropertyIterator(getSession(), remote.getWeakReferences(name));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeShare() throws RepositoryException {
        try {
            remote.removeShare();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeSharedSet() throws RepositoryException {
        try {
            remote.removeSharedSet();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void setPrimaryType(String nodeTypeName)
            throws RepositoryException {
        try {
            remote.setPrimaryType(nodeTypeName);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

}
