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
package org.apache.jackrabbit.spi.commons.query;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;

/**
 * Implements the root node of a query tree.
 */
public class QueryRootNode extends QueryNode {

    /**
     * The path sub query
     */
    private PathQueryNode locationNode;

    /**
     * The list of property names (as {@link org.apache.jackrabbit.spi.Name}s
     * to select.
     */
    private final List selectProperties = new ArrayList();

    /**
     * The list of property names to order the result nodes. Might be null
     */
    private OrderQueryNode orderNode;

    /**
     * Creates a new <code>QueryRootNode</code> instance.
     */
    protected QueryRootNode() {
        super(null);
    }

    /**
     * Returns the {@link PathQueryNode} or <code>null</code> if this query does
     * not have a location node.
     *
     * @return the {@link PathQueryNode} or <code>null</code> if this query does
     *         not have a location node.
     */
    public PathQueryNode getLocationNode() {
        return locationNode;
    }

    /**
     * Sets the location node.
     *
     * @param locationNode the new location node.
     */
    public void setLocationNode(PathQueryNode locationNode) {
        this.locationNode = locationNode;
    }

    /**
     * Adds a new select property to the query.
     *
     * @param propName the name of the property to select.
     */
    public void addSelectProperty(Name propName) {
        selectProperties.add(propName);
    }

    /**
     * Returns an array of select properties.
     *
     * @return an array of select properties.
     */
    public Name[] getSelectProperties() {
        return (Name[]) selectProperties.toArray(new Name[selectProperties.size()]);
    }

    /**
     * Returns the order node or <code>null</code> if no order is specified.
     *
     * @return the order node.
     */
    public OrderQueryNode getOrderNode() {
        return orderNode;
    }

    /**
     * Sets a new order node.
     *
     * @param orderNode the new order node.
     */
    public void setOrderNode(OrderQueryNode orderNode) {
        this.orderNode = orderNode;
    }

    /**
     * {@inheritDoc}
     * @throws RepositoryException
     */
    public Object accept(QueryNodeVisitor visitor, Object data) throws RepositoryException {
        return visitor.visit(this, data);
    }

    /**
     * Returns the type of this node.
     *
     * @return the type of this node.
     */
    public int getType() {
        return QueryNode.TYPE_ROOT;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof QueryRootNode) {
            QueryRootNode other = (QueryRootNode) obj;
            return (locationNode == null ? other.locationNode == null : locationNode.equals(other.locationNode))
                    && selectProperties.equals(other.selectProperties)
                    && (orderNode == null ? other.orderNode == null : orderNode.equals(other.orderNode));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean needsSystemTree() {
        return (locationNode != null && locationNode.needsSystemTree()) || (orderNode != null && orderNode.needsSystemTree());
    }

}
