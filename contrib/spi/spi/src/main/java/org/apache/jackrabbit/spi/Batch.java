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
import java.io.InputStream;

/**
 * <code>Batch</code>...
 */
public interface Batch {

    /**
     * @param parentId
     * @param nodeName Name of the node to be created
     * @param nodetypeName
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
     *
     * @param parentId
     * @param propertyName Name of the property to be created
     * @param value
     * @param propertyType
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
    public void addProperty(NodeId parentId, QName propertyName, String value, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * @param parentId
     * @param propertyName Name of the property to be created
     * @param values
     * @param propertyType the property type
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
    public void addProperty(NodeId parentId, QName propertyName, String[] values, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     *
     * @param parentId
     * @param propertyName Name of the property to be created
     * @param value
     * @param propertyType
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws AccessDeniedException
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     * @see javax.jcr.Node#setProperty(String, javax.jcr.Value, int)
     * @see javax.jcr.Node#setProperty(String, String, int)
     * @see javax.jcr.Node#setProperty(String, java.io.InputStream)
     * @see javax.jcr.Session#importXML(String, java.io.InputStream, int)
     */
    public void addProperty(NodeId parentId, QName propertyName, InputStream value, int propertyType) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * @param parentId
     * @param propertyName Name of the property to be created
     * @param values
     * @param propertyType the property type
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
    public void addProperty(NodeId parentId, QName propertyName, InputStream[] values, int propertyType) throws RepositoryException;

    /**
     *
     * @param propertyId
     * @param value
     * @param propertyType
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
    public void setValue(PropertyId propertyId, String value, int propertyType) throws RepositoryException;

    /**
     * @param propertyId
     * @param values
     * @param propertyType the type of the property
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
    public void setValue(PropertyId propertyId, String[] values, int propertyType) throws RepositoryException;

    /**
     *
     * @param propertyId
     * @param value
     * @param propertyType
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     * @see javax.jcr.Property#setValue(javax.jcr.Value)
     * @see javax.jcr.Property#setValue(java.io.InputStream)
     */
    public void setValue(PropertyId propertyId, InputStream value, int propertyType) throws RepositoryException;

    /**
     * @param propertyId
     * @param values
     * @param propertyType the type of the property
     * @throws javax.jcr.ValueFormatException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Property#setValue(javax.jcr.Value[])
     */
    public void setValue(PropertyId propertyId, InputStream[] values, int propertyType) throws RepositoryException;

    /**
     * @param itemId
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
     * @param parentId
     * @param srcNodeId
     * @param beforeNodeId
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
     * @param nodeId
     * @param mixinNodeTypeIds
     * @throws javax.jcr.nodetype.NoSuchNodeTypeException
     * @throws javax.jcr.version.VersionException
     * @throws javax.jcr.nodetype.ConstraintViolationException
     * @throws javax.jcr.lock.LockException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.UnsupportedRepositoryOperationException
     * @throws javax.jcr.RepositoryException
     * @see javax.jcr.Node#addMixin(String)
     */
    public void setMixins(NodeId nodeId, QName[] mixinNodeTypeIds) throws RepositoryException;

    /**
     * @param srcNodeId
     * @param destParentNodeId
     * @param destName
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