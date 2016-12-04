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
package org.apache.jackrabbit.commons.predicate;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Filters on the node type of a node.
 *
 */
public class NodeTypePredicate extends DepthPredicate {

    /**
     * the nodetype to filter on
     */
    protected final String nodeType;

    /**
     * indicates if supertypes should be respected
     */
    protected final boolean respectSupertype;

    /**
     * Creates a new node type filter.
     * @param nodeType the node type to filter on
     * @param respectSupertype indicates if supertype should be respected
     * @param minDepth the minimal depth
     * @param maxDepth the maximal depth
     */
    public NodeTypePredicate(String nodeType, boolean respectSupertype,
                              int minDepth, int maxDepth) {
        super(minDepth, maxDepth);
        this.nodeType = nodeType;
        this.respectSupertype = respectSupertype;
    }

    /**
     * Creates a new node type filter.
     * @param nodeType the node type to filter on
     * @param respectSupertype indicates if supertype should be respected
     */
    public NodeTypePredicate(String nodeType, boolean respectSupertype) {
        this(nodeType, respectSupertype, 0, Integer.MAX_VALUE);
    }

    /**
     * Returns <code>true</code> if the item is a node and if the configured
     * nodetype is equal to the primary type of the node. if supertypes are
     * respected it also returns <code>true</code> if the items nodetype
     * extends from the configured node type (Node.isNodeType() check).
     * @see org.apache.jackrabbit.commons.predicate.DepthPredicate#matches(javax.jcr.Item)
     */
    @Override
    protected boolean matches(Item item) throws RepositoryException {
        if (item.isNode()) {
            if (respectSupertype) {
                try {
                    return ((Node) item).isNodeType(nodeType);
                } catch (RepositoryException e) {
                    // ignore
                    return false;
                }
            }
            return ((Node) item).getPrimaryNodeType().getName().equals(nodeType);
        }
        return false;

    }
}