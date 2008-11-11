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
package org.apache.jackrabbit.api.jsr283;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyIterator;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

/**
 * This interface holds extensions made in JCR 2.0 while work
 * is in progress implementing JCR 2.0.
 * 
 * @since JCR 2.0
 */
public interface Node extends javax.jcr.Node {

    /**
     * Returns the identifier of this node. Applies to both referenceable and
     * non-referenceable nodes.
     * <p/>
     * A <code>RepositoryException</code> is thrown if an error occurs.
     *
     * @return the identifier of this node
     * @throws RepositoryException If an error occurs.
     * @since JCR 2.0
     */
    public String getIdentifier() throws RepositoryException;

    /**
     * This method returns all <code>REFERENCE</code> properties that refer to
     * this node, have the specified <code>name</code> and that are accessible
     * through the current <code>Session</code>.
     * <p/>
     * If the <code>name</code> parameter is <code>null</code> then all
     * referring <code>REFERENCES</code> are returned regardless of name.
     * <p/>
     * Some level 2 implementations may only return properties that have been
     * saved (in a transactional setting this includes both those properties
     * that have been saved but not yet committed, as well as properties that
     * have been committed). Other level 2 implementations may additionally
     * return properties that have been added within the current <code>Session</code>
     * but are not yet saved.
     * <p/>
     * In implementations that support versioning, this method does not return
     * properties that are part of the frozen state of a version in version
     * storage.
     * <p/>
     * If this node has no referring properties with the specified name,
     * an empty iterator is returned.
     *
     * @param name name of referring <code>REFERENCE</code> properties to be
     *             returned; if <code>null</code> then all referring <code>REFERENCE</code>s
     *             are returned
     * @return A <code>PropertyIterator</code>.
     * @throws RepositoryException if an error occurs
     * @since JCR 2.0
     */
    public PropertyIterator getReferences(String name) throws RepositoryException;

    /**
     * Changes the primary node type of this node to <code>nodeTypeName</code>.
     * Also immediately changes this node's <code>jcr:primaryType</code> property
     * appropriately. Semantically, the new node type may take effect
     * immediately and <i>must</i> take effect on <code>save</code>. Whichever
     * behavior is adopted it must be the same as the behavior adopted for
     * <code>addMixin()</code> (see below) and the behavior that occurs when a
     * node is first created.
     * <p/>
     * If the presence of an existing property or child node would cause an
     * incompatibility with the new node type a <code>ConstraintViolationException</code>
     * is thrown either immediately or on <code>save</code>.
     * <p/>
     * If the new node type would cause this node to be incompatible with the
     * node type of its parent then a <code>ConstraintViolationException</code>
     * is thrown either immediately or on <code>save</code>.
     * <p/>
     * A <code>ConstraintViolationException</code> is also thrown either
     * immediately or on <code>save</code> if a conflict with an already
     * assigned mixin occurs.
     * <p/>
     * A <code>ConstraintViolationException</code> may also be thrown either
     * immediately or on <code>save</code> if the attempted change violates
     * implementation-specific node type transition rules. A repository that
     * disallows all primary node type changes would simple throw this
     * exception in all cases.
     * <p/>
     * If the specified node type is not recognized a
     * <code>NoSuchNodeTypeException</code> is thrown either immediately
     * or on <code>save</code>.
     * <p/>
     * A <code>VersionException</code> is thrown either immediately or on
     * <code>save</code> if this node is versionable and checked-in, or is
     * non-versionable but its nearest versionable ancestor is checked-in.
     * <p/>
     * A <code>LockException</code> is thrown either immediately or on
     * <code>save</code> if a lock prevents the change of node type.
     * <p/>
     * A <code>RepositoryException</code> will be thrown if another error occurs.
     *
     * @param nodeTypeName the name of the new node type.
     * @throws ConstraintViolationException If the specified primary node type
     *                                      is prevented from being assigned.
     * @throws NoSuchNodeTypeException      If the specified <code>nodeTypeName</code>
     *                                      is not recognized and this implementation performs this validation
     *                                      immediately instead of waiting until <code>save</code>.
     * @throws VersionException             if this node is versionable and checked-in or is
     *                                      non-versionable but its nearest versionable ancestor is checked-in and this
     *                                      implementation performs this validation immediately instead of waiting until
     *                                      <code>save</code>.
     * @throws LockException                if a lock prevents the change of the primary node type
     *                                      and this implementation performs this validation immediately instead of
     *                                      waiting until <code>save</code>.
     * @throws RepositoryException          if another error occurs.
     * @since JCR 2.0
     */
    public void setPrimaryType(String nodeTypeName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException;

    /**
     * Returns an iterator over all nodes that are in the shared set of this
     * node. If this node is not shared then the returned iterator contains
     * only this node.
     *
     * @return a <code>NodeIterator</code>
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public NodeIterator getSharedSet() throws RepositoryException;

    /**
     * A special kind of <code>remove()</code> that removes this node, but does
     * not remove any other node in the shared set of this node.
     * <p/>
     * All of the exceptions defined for <code>remove()</code> apply to this
     * function. In addition, a <code>RepositoryException</code> is thrown if
     * this node cannot be removed without removing another node in the shared
     * set of this node.
     * <p/>
     * If this node is not shared this method removes only this node.
     *
     * @throws VersionException if the parent node of this item is versionable and checked-in
     * or is non-versionable but its nearest versionable ancestor is checked-in and this
     * implementation performs this validation immediately instead of waiting until <code>save</code>.
     * @throws LockException if a lock prevents the removal of this item and this
     * implementation performs this validation immediately instead of waiting until <code>save</code>.
     * @throws ConstraintViolationException if removing the specified item would violate a node type or
     * implementation-specific constraint and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * @throws RepositoryException if another error occurs.
     * @see #removeSharedSet()
     * @see javax.jcr.Item#remove()
     * @see Session#removeItem(String)
     * @since JCR 2.0
     */
    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException;
    
    /**
     * A special kind of <code>remove()</code> that removes this node and every
     * other node in the shared set of this node.
     * <p/>
     * This removal must be done atomically, i.e., if one of the nodes cannot be
     * removed, the function throws the exception <code>remove()</code> would
     * have thrown in that case, and none of the nodes are removed.
     * <p/>
     * If this node is not shared this method removes only this node.
     *
     * @throws VersionException if the parent node of this item is versionable and checked-in
     * or is non-versionable but its nearest versionable ancestor is checked-in and this
     * implementation performs this validation immediately instead of waiting until <code>save</code>.
     * @throws LockException if a lock prevents the removal of this item and this
     * implementation performs this validation immediately instead of waiting until <code>save</code>.
     * @throws ConstraintViolationException if removing the specified item would violate a node type or
     * implementation-specific constraint and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * @throws RepositoryException if another error occurs.@throws VersionException if
     * @see #removeShare()
     * @see javax.jcr.Item#remove()
     * @see Session#removeItem(String)
     * @since JCR 2.0
     */
    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException;

}
