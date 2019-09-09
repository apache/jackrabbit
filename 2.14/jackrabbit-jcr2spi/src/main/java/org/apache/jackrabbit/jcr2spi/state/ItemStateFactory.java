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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import java.util.Iterator;

/**
 * <code>ItemStateFactory</code> provides methods to create child
 * <code>NodeState</code>s and <code>PropertyState</code>s for a given
 * <code>NodeState</code>.
 */
public interface ItemStateFactory {

    /**
     * @param entry
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public NodeState createRootState(NodeEntry entry) throws ItemNotFoundException, RepositoryException;

    /**
     * Creates the child <code>NodeState</code> with the given
     * <code>nodeId</code>.
     *
     * @param nodeId the id of the <code>NodeState</code> to create.
     * @param entry the <code>HierarchyEntry</code> the new state should
     * be attached to.
     * @return the created <code>NodeState</code>.
     * @throws ItemNotFoundException if there is no such <code>NodeState</code>.
     * @throws RepositoryException if an error occurs while retrieving the <code>NodeState</code>.
     */
    public NodeState createNodeState(NodeId nodeId, NodeEntry entry)
            throws ItemNotFoundException, RepositoryException;


    /**
     * Tries to retrieve the <code>NodeState</code> with the given <code>NodeId</code>
     * and if the state exists, fills in the NodeEntries missing between the
     * last known NodeEntry marked by <code>anyParent</code>.
     *
     * @param nodeId
     * @param anyParent
     * @return the created <code>NodeState</code>.
     * @throws ItemNotFoundException if there is no such <code>NodeState</code>.
     * @throws RepositoryException if an error occurs while retrieving the <code>NodeState</code>.
     */
    public NodeState createDeepNodeState(NodeId nodeId, NodeEntry anyParent)
            throws ItemNotFoundException, RepositoryException;


    /**
     * Creates the <code>PropertyState</code> with the given
     * <code>propertyId</code>.
     *
     * @param propertyId the id of the <code>PropertyState</code> to create.
     * @param entry the <code>HierarchyEntry</code> the new state should
     * be attached to.
     * @return the created <code>PropertyState</code>.
     * @throws ItemNotFoundException if there is no such <code>PropertyState</code>.
     * @throws RepositoryException if an error occurs while retrieving the
     * <code>PropertyState</code>.
     */
    public PropertyState createPropertyState(PropertyId propertyId, PropertyEntry entry)
            throws ItemNotFoundException, RepositoryException;


    /**
     * Tries to retrieve the <code>PropertyState</code> with the given <code>PropertyId</code>
     * and if the state exists, fills in the HierarchyEntries missing between the
     * last known NodeEntry marked by <code>anyParent</code>.
     *
     * @param propertyId
     * @param anyParent
     * @return
     * @throws ItemNotFoundException if there is no such <code>NodeState</code>.
     * @throws RepositoryException if an error occurs while retrieving the <code>NodeState</code>.
     */
    public PropertyState createDeepPropertyState(PropertyId propertyId, NodeEntry anyParent) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns an Iterator over <code>ChildInfo</code>s for the given <code>NodeState</code>.
     *
     * @param nodeId
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public Iterator<ChildInfo> getChildNodeInfos(NodeId nodeId) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the identifiers of all reference properties that  point to
     * the given node.
     *
     * @param nodeState reference target
     * @param propertyName
     * @param weak Boolean flag indicating whether weak references should be
     * returned or not.
     * @return reference property identifiers
     */
    public Iterator<PropertyId> getNodeReferences(NodeState nodeState, Name propertyName, boolean weak);

    /**
     * Adds the given <code>ItemStateCreationListener</code>.
     *
     * @param listener
     */
    public void addCreationListener(ItemStateCreationListener listener);

    /**
     * Removes the given <code>ItemStateCreationListener</code>.
     *
     * @param listener
     */
    public void removeCreationListener(ItemStateCreationListener listener);
}
