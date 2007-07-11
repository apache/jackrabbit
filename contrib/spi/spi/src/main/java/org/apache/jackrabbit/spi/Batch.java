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
package org.apache.jackrabbit.spi;

import org.apache.jackrabbit.name.QName;

import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * <code>Batch</code> defines a set of modifications that must be executed and
 * persisted at once. If any of the modifications added to the batch fails, none
 * of the other changes must be persisted, thus leaving the persistent layer
 * unaffected by the given batch. The <code>Batch</code> object is obtained by calling
 * {@link RepositoryService#createBatch(ItemId, SessionInfo)}. The modifications
 * collected in a Batch are persisted upon a sucessful call to
 * {@link RepositoryService#submit(Batch)}.
 */
public interface Batch {

    /**
     * Add a new node to the persistent layer.
     *
     * @param parentId NodeId identifying the parent node.
     * @param nodeName Name of the node to be created.
     * @param nodetypeName Primary node type name of the node to be created.
     * @param uuid Value for the jcr:uuid property of the node to be created or
     * <code>null</code>. If due to an import the uuid of the resulting node is
     * already defined, it must be passed as separate uuid parameter, indicating
     * a binding value for the server. Otherwise the uuid must be <code>null</code>.
     * @throws javax.jcr.ItemExistsException
     * @throws javax.jcr.PathNotFoundException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.nodetype.NoSuchNodeTypeException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#addNode(String)
     * @see javax.jcr.Node#addNode(String, String)
     * @see javax.jcr.Session#importXML(String, java.io.InputStream, int)
     * @see javax.jcr.query.Query#storeAsNode(String)
     */
    public void addNode(NodeId parentId, QName nodeName, QName nodetypeName, String uuid) throws RepositoryException;

    /**
     * Add a new property to the persistent layer.
     *
     * @param parentId NodeId identifying the parent node.
     * @param propertyName Name of the property to be created.
     * @param value The qualified value of the property to be created.
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws AccessDeniedException
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     * @see javax.jcr.Node#setProperty(String, javax.jcr.Value)
     * @see javax.jcr.Node#setProperty(String, javax.jcr.Value, int)
     * @see javax.jcr.Node#setProperty(String, String)
     * @see javax.jcr.Node#setProperty(String, String, int)
     * @see javax.jcr.Node#setProperty(String, java.util.Calendar)
     * @see javax.jcr.Node#setProperty(String, boolean)
     * @see javax.jcr.Node#setProperty(String, double)
     * @see javax.jcr.Node#setProperty(String, long)
     * @see javax.jcr.Node#setProperty(String, javax.jcr.Node)
     * @see javax.jcr.Session#importXML(String, java.io.InputStream, int)
     * @see javax.jcr.query.Query#storeAsNode(String)
     */
    public void addProperty(NodeId parentId, QName propertyName, QValue value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Add a new multi-valued property to the persistent layer.
     *
     * @param parentId NodeId identifying the parent node.
     * @param propertyName Name of the property to be created.
     * @param values The qualified values of the property to be created.
     * @throws javax.jcr.ValueFormatException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.PathNotFoundException
     * @throws javax.jcr.ItemExistsException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#setProperty(String, javax.jcr.Value[])
     * @see javax.jcr.Node#setProperty(String, javax.jcr.Value[], int)
     * @see javax.jcr.Node#setProperty(String, String[])
     * @see javax.jcr.Node#setProperty(String, String[], int)
     * @see javax.jcr.Session#importXML(String, java.io.InputStream, int)
     */
    public void addProperty(NodeId parentId, QName propertyName, QValue[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Modify the value of an existing property.
     *
     * @param propertyId PropertyId identifying the property to be modified.
     * @param value The new value.
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     * @see javax.jcr.Property#setValue(javax.jcr.Value)
     * @see javax.jcr.Property#setValue(String)
     * @see javax.jcr.Property#setValue(long)
     * @see javax.jcr.Property#setValue(double)
     * @see javax.jcr.Property#setValue(java.util.Calendar)
     * @see javax.jcr.Property#setValue(boolean)
     * @see javax.jcr.Property#setValue(javax.jcr.Node)
     */
    public void setValue(PropertyId propertyId, QValue value) throws RepositoryException;

    /**
     * Modify the value of an existing, multi-valued property.
     *
     * @param propertyId PropertyId identifying the property to be modified.
     * @param values The new values.
     * @throws javax.jcr.ValueFormatException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Property#setValue(javax.jcr.Value[])
     * @see javax.jcr.Property#setValue(String[])
     */
    public void setValue(PropertyId propertyId, QValue[] values) throws RepositoryException;

    /**
     * Remove an existing item.
     *
     * @param itemId ItemId identifying the item to be removed.
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Item#remove()
     */
    public void remove(ItemId itemId) throws RepositoryException;

    /**
     * Modify the order of the child nodes identified by the given
     * <code>NodeId</code>s.
     *
     * @param parentId NodeId identifying the parent node.
     * @param srcNodeId NodeId identifying the node to be reordered.
     * @param beforeNodeId NodeId identifying the child node, before which the
     * source node must be placed.
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.ItemNotFoundException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#orderBefore(String, String)
     */
    public void reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) throws RepositoryException;

    /**
     * Modify the set of mixin node types present on the node identified by the
     * given id.
     *
     * @param nodeId NodeId identifying the node to be modified.
     * @param mixinNodeTypeIds The new set of mixin types. Compared to the
     * previous values this may result in both adding and/or removing mixin types.
     * @throws javax.jcr.nodetype.NoSuchNodeTypeException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#addMixin(String)
     * @see javax.jcr.Node#removeMixin(String)
     */
    public void setMixins(NodeId nodeId, QName[] mixinNodeTypeIds) throws RepositoryException;

    /**
     * Move the node identified by the given <code>srcNodeId</code> to the
     * new parent identified by <code>destParentNodeId</code> and change its
     * name to <code>destName</code>.
     *
     * @param srcNodeId NodeId identifying the node to be moved.
     * @param destParentNodeId NodeId identifying the new parent.
     * @param destName The new name of the moved node.
     * @throws javax.jcr.ItemExistsException
     * @throws javax.jcr.PathNotFoundException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Session#move(String, String)
     */
    public void move(NodeId srcNodeId, NodeId destParentNodeId, QName destName) throws RepositoryException;
}
