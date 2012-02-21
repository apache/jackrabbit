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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Remote version of the JCR {@link javax.jcr.Node Node} interface.
 * Used by the {@link org.apache.jackrabbit.rmi.server.ServerNode ServerNode}
 * and {@link org.apache.jackrabbit.rmi.client.ClientNode ClientNode}
 * adapters to provide transparent RMI access to remote nodes.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding Node method. The remote object will simply forward
 * the method call to the underlying Node instance. Argument and return
 * values, as well as possible exceptions, are copied over the network.
 * Complex return values (like Nodes and Properties) are returned as remote
 * references to the corresponding remote interfaces. Iterator values
 * are transmitted as object arrays. RMI errors are signaled with
 * RemoteExceptions.
 * <p>
 * Note that only two generic setProperty methods are included in this
 * interface. Clients should implement the type-specific setProperty
 * methods by wrapping the argument values into generic Value objects
 * and calling the generic setProperty methods. Note also that the
 * Value objects must be serializable and implemented using classes
 * available on both the client and server side. The
 * {@link org.apache.jackrabbit.rmi.value.SerialValueFactory SerialValueFactory}
 * class provides two convenience methods to satisfy these requirements.
 *
 * @see javax.jcr.Node
 * @see org.apache.jackrabbit.rmi.client.ClientNode
 * @see org.apache.jackrabbit.rmi.server.ServerNode
 */
public interface RemoteNode extends RemoteItem {

    /**
     * Remote version of the
     * {@link javax.jcr.Node#addNode(String) Node.addNode(Sring)} method.
     *
     * @param path relative path
     * @return new node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNode addNode(String path) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#addNode(String,String) Node.addNode(String,String)}
     * method.
     *
     * @param path relative path
     * @param type node type name
     * @return new node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNode addNode(String path, String type)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getProperty(String) Node.getProperty(String)}
     * method.
     *
     * @param path relative path
     * @return node property
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteProperty getProperty(String path)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getProperties() Node.getProperties()} method.
     *
     * @return node properties
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getProperties() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getProperties(String) Node.getProperties(String)}
     * method.
     *
     * @param pattern property name pattern
     * @return matching node properties
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getProperties(String pattern)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getProperties(String[]) Node.getProperties(String[])}
     * method.
     *
     * @param globs property name globs
     * @return matching node properties
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getProperties(String[] globs)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getPrimaryItem() Node.getPrimaryItem()} method.
     *
     * @return primary item
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteItem getPrimaryItem() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getIdentifier() Node.getIdentifier()} method.
     *
     * @return node identifier
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String getIdentifier() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getUUID() Node.getUUID()} method.
     *
     * @return node uuid
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String getUUID() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getReferences() Node.getReferences()} method.
     *
     * @return reference properties
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getReferences() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getReferences(String) Node.getReferences(String)} method.
     *
     * @param name reference property name
     * @return reference properties
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getReferences(String name) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getNodes() Node.getNodes()} method.
     *
     * @return child nodes
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getNodes() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getNodes(String) Node.getNodes(String)} method.
     *
     * @param pattern node name pattern
     * @return matching child nodes
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getNodes(String pattern)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getNodes(String[]) Node.getNodes(String[])} method.
     *
     * @param globs node name globs
     * @return matching child nodes
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getNodes(String[] globs)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#hasNode(String) Node.hasNode(String)} method.
     *
     * @param path relative path
     * @return <code>true</code> if the identified node exists,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean hasNode(String path) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#hasProperty(String) Node.hasProperty()} method.
     *
     * @param path relative path
     * @return <code>true</code> if the identified property exists,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean hasProperty(String path)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#hasNodes() Node.hasNodes()} method.
     *
     * @return <code>true</code> if this node has child nodes,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean hasNodes() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#hasProperties() Node.hasProperties()} method.
     *
     * @return <code>true</code> if this node has properties,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean hasProperties() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getPrimaryNodeType() Node.getPrimaryNodeType()}
     * method.
     *
     * @return primary node type
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType getPrimaryNodeType()
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getMixinNodeTypes() Node.getMixinNodeTypes()}
     * method.
     *
     * @return mixin node types
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNodeType[] getMixinNodeTypes()
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#isNodeType(String) Node.isNodeType(String)} method.
     *
     * @param type node type name
     * @return <code>true</code> if this node is an instance of the
     *         identified type, <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean isNodeType(String type) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getNode(String) Node.getNode(String)} method.
     *
     * @param path relative path
     * @return node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNode getNode(String path) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#orderBefore(String,String) Node.orderBefore(String,String)}
     * method.
     *
     * @param src source path
     * @param dst destination path
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void orderBefore(String src, String dst)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#setProperty(String,Value) Node.setProperty(String,Value)}
     * method.
     *
     * @param name property name
     * @param value property value
     * @return property
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteProperty setProperty(String name, Value value)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#setProperty(String,Value,int) Node.setProperty(String,Value)}
     * method.
     *
     * @param name property name
     * @param value property value
     * @param type property type
     * @return property
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteProperty setProperty(String name, Value value, int type)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#setProperty(String,Value[]) Node.setProperty(String,Value[])}
     * method.
     *
     * @param name property name
     * @param values property values
     * @return property
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteProperty setProperty(String name, Value[] values)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#addMixin(String) Node.addMixin(String)} method.
     *
     * @param name mixin type name
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void addMixin(String name) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#removeMixin(String) Node.removeMixin(String)}
     * method.
     *
     * @param name mixin type name
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void removeMixin(String name) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#canAddMixin(String) Node.canAddMixin(String)}
     * method.
     *
     * @param name mixin type name
     * @return <code>true</code> if the mixin type can be added,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean canAddMixin(String name)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getDefinition() Node.getDefinition()} method.
     *
     * @return node definition
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNodeDefinition getDefinition() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#checkin() Node.checkin()} method.
     *
     * @return checked in version
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteVersion checkin() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#checkout() Node.checkout()} method.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void checkout() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#update(String) Node.update(String)} method.
     *
     * @param workspace source workspace name
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void update(String workspace) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#merge(String,boolean) Node.merge(String,boolean)}
     * method.
     *
     * @param workspace source workspace name
     * @param bestEffort best effort flag
     * @return nodes that failed to merge
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteIterator merge(String workspace, boolean bestEffort)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#cancelMerge(javax.jcr.version.Version) Node.cancelMerge(Version)}
     * method.
     *
     * @param versionUUID The UUID of the version whose labels are to be returned.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void cancelMerge(String versionUUID)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#doneMerge(javax.jcr.version.Version) Node.doneMerge(Version)}
     * method.
     *
     * @param versionUUID The UUID of the version whose labels are to be returned.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void doneMerge(String versionUUID)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getCorrespondingNodePath(String) Node.getCorrespondingNodePath(String)}
     * method.
     *
     * @param workspace workspace name
     * @return corresponding node path
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String getCorrespondingNodePath(String workspace)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getIndex() Node.getIndex()} method.
     *
     * @return node index
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    int getIndex() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#restore(String,boolean) Node.restore(String,boolean)}
     * method.
     *
     * @param version version name
     * @param removeExisting flag to remove conflicting nodes
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void restore(String version, boolean removeExisting)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#restore(javax.jcr.version.Version, boolean) Node.restore(Version,boolean)}
     * method.
     * <p>
     * This method has been rename to prevent a naming clash with
     * {@link #restore(String, boolean)}.
     *
     * @param versionUUID The UUID of the version whose labels are to be returned.
     * @param removeExisting flag to remove conflicting nodes
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void restoreByUUID(String versionUUID, boolean removeExisting)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#restore(javax.jcr.version.Version, String, boolean) Node.restore(Version,String,boolean)}
     * method.
     *
     * @param versionUUID The UUID of the version whose labels are to be returned.
     * @param path the path to which the version is to be restored
     * @param removeExisting flag to remove conflicting nodes
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void restore(String versionUUID, String path, boolean removeExisting)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#restoreByLabel(String,boolean) Node.restoreByLabel(String,boolean)}
     * method.
     *
     * @param label version label
     * @param removeExisting flag to remove conflicting nodes
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void restoreByLabel(String label, boolean removeExisting)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#unlock() Node.unlock()} method.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void unlock() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#holdsLock() Node.holdsLock()} method.
     *
     * @return <code>true</code> if this node holds a lock,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean holdsLock() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#isLocked() Node.isLocked()} method.
     *
     * @return <code>true</code> if this node is locked,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean isLocked() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#isCheckedOut() Node.isCheckedOut()} method.
     *
     * @return <code>true</code> if this node is checked out,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean isCheckedOut() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getVersionHistory() Node.getVersionHistory()} method.
     *
     * @return the remote version history.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteVersionHistory getVersionHistory() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getBaseVersion() Node.getBaseVersion()} method.
     *
     * @return the remote base version
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteVersion getBaseVersion() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#setProperty(String,Value[],int) Node.setProperty(String,Value[],int)}
     * method.
     *
     * @param name property name
     * @param values property values
     * @param type property type
     * @return property
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteProperty setProperty(String name, Value[] values, int type)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#lock(boolean,boolean) Node.lock(boolean,boolean)}
     * method.
     *
     * @param isDeep flag to create a deep lock
     * @param isSessionScoped flag to create a session-scoped lock
     * @return lock
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteLock lock(boolean isDeep, boolean isSessionScoped)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getLock() Node.getLock()} method.
     *
     * @return lock
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteLock getLock() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getSharedSet() Node.getSharedSet()} method.
     *
     * @return a <code>NodeIterator</code>.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	RemoteIterator getSharedSet() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#followLifecycleTransition(String) Node.followLifecycleTransition(String)}
     * method.
     *
     * @param transition a state transition
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	void followLifecycleTransition(String transition) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getAllowedLifecycleTransistions() Node.getAllowedLifecycleTransistions()}
     * method.
     *
     * @return a <code>String</code> array.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	String[] getAllowedLifecycleTransistions() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getWeakReferences() Node.getWeakReferences()}
     * method.
     *
     * @return A <code>PropertyIterator</code>.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	RemoteIterator getWeakReferences() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#getWeakReferences(String) Node.getWeakReferences(String)}
     * method.
     *
     * @param name name of referring <code>WEAKREFERENCE</code> properties to be
     *             returned; if <code>null</code> then all referring
     *             <code>WEAKREFERENCE</code>s are returned.
     * @return A <code>PropertyIterator</code>.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	RemoteIterator getWeakReferences(String name) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#removeShare() Node.removeShare()}
     * method.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	void removeShare() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#removeSharedSet() Node.removeSharedSet()}
     * method.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	void removeSharedSet() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Node#setPrimaryType(String) Node.setPrimaryType(String)}
     * method.
     *
     * @param nodeTypeName the node type name
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	void setPrimaryType(String nodeTypeName) throws RepositoryException, RemoteException;

}
