/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implements the root node of a query tree.
 */
public class QueryRootNode extends QueryNode {

    /**
     * The path sub query
     */
    private PathQueryNode locationNode;

    /**
     * The list of nodeType constraints. Might be null
     */
    private List nodeTypes = new ArrayList();

    /**
     * The list of property names to select. Might be null
     */
    private List selectProperties = new ArrayList();

    /**
     * Sub node that defines constraints. Might be null
     */
    private AndQueryNode constraintNode = new AndQueryNode(this);

    /**
     * The textsearch clause. Might be null
     */
    private TextsearchQueryNode textsearchNode;

    /**
     * The list of property names to order the result nodes. Might be null
     */
    private OrderQueryNode orderNode;

    /**
     * Creates a new <code>QueryRootNode</code> instance.
     */
    public QueryRootNode() {
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
    public void addSelectProperty(String propName) {
        selectProperties.add(propName);
    }

    /**
     * Returns an array of select properties.
     *
     * @return an array of select properties.
     */
    public String[] getSelectProperties() {
        return (String[]) selectProperties.toArray(new String[selectProperties.size()]);
    }

    /**
     * Returns the constraint node.
     *
     * @return the constraint node.
     */
    public AndQueryNode getConstraintNode() {
        return constraintNode;
    }

    /**
     * Returns the textsearch node.
     *
     * @return the textsearch node.
     */
    public TextsearchQueryNode getTextsearchNode() {
        return textsearchNode;
    }

    /**
     * Sets a new textsearch node.
     *
     * @param textsearchNode the new textsearch node.
     */
    public void setTextsearchNode(TextsearchQueryNode textsearchNode) {
        this.textsearchNode = textsearchNode;
    }

    /**
     * Returns the order node.
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
    //--------------------------------------------------------------------------

    /**
     * @see QueryNode#accept(org.apache.jackrabbit.core.search.QueryNodeVisitor, java.lang.Object)
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }


    /**
     * Returns a string representation of this query node including its sub-nodes.
     * The returned string is formatted in JCRQL syntax.
     *
     * @return a string representation of this query node including its sub-nodes.
     */
    public String toJCRQLString() {
        StringBuffer sb = new StringBuffer("SELECT *");
        String comma = "";
        if (nodeTypes.size() > 0) {
            sb.append(" FROM");
        }
        for (Iterator it = nodeTypes.iterator(); it.hasNext();) {
            NodeTypeQueryNode nodeType = (NodeTypeQueryNode) it.next();
            sb.append(comma);
            sb.append(" ").append(nodeType.getValue());
            comma = ",";
        }
        if (locationNode != null) {
            sb.append(" ").append(locationNode.toJCRQLString());
        }
        if (constraintNode != null) {
            sb.append(" WHERE ").append(constraintNode.toJCRQLString());
        }
        return sb.toString();
    }

    /**
     * Returns a string representation of this query node including its sub-nodes.
     * The returned string is formatted in XPath syntax.
     *
     * @return a string representation of this query node including its sub-nodes.
     */
    public String toXPathString() {
        return "";
    }
}
