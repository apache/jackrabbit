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
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * This interface holds extensions made in JCR 2.0 while work
 * is in progress implementing JCR 2.0.
 * 
 * @since JCR 2.0
 */
public interface Node extends javax.jcr.Node {
  
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
     * @see Item#remove()
     * @see javax.jcr.Workspace#removeItem
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
     * @see Item#remove()
     * @see javax.jcr.Workspace#removeItem
     * @since JCR 2.0
     */
    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException;

}
