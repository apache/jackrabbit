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

/**
 * Implements a query node that defines the order of nodes according to the
 * values of properties.
 */
public class OrderQueryNode extends QueryNode {

    /**
     * The name of the properties to order
     */
    private String[] properties;

    /**
     * Flag indicating whether nodes are ordered ascending or descending
     */
    private boolean ascending;

    /**
     * Creates a new <code>OrderQueryNode</code> with a reference to a parent
     * node and sort properties.
     *
     * @param parent     the parent node of this query node.
     * @param properties the names of the properties to sort the result nodes.
     * @param asc        if <code>true</code> result nodes are orderd ascending;
     *                   otherwise descending.
     */
    public OrderQueryNode(QueryNode parent, String[] properties, boolean asc) {
        super(parent);
        this.properties = properties;
        this.ascending = asc;
    }


    /**
     * @see QueryNode#accept(org.apache.jackrabbit.core.search.QueryNodeVisitor, java.lang.Object)
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Returns <code>true</code> if result nodes should be orderd ascending.
     * If <code>false</code> result nodes are ordered descending.
     *
     * @return the value of the ascending property.
     */
    public boolean isAscending() {
        return ascending;
    }

    /**
     * Returns a String array that contains the name of the properties
     * to sort the result nodes.
     *
     * @return names of order properties.
     */
    public String[] getOrderByProperties() {
        return properties;
    }

    /**
     * Returns a JCRQL representation for this query node.
     *
     * @return a JCRQL representation for this query node.
     */
    public String toJCRQLString() {
        StringBuffer sb = new StringBuffer("ORDER BY");
        if (properties.length > 0) {
            String comma = "";
            for (int i = 0; i < properties.length; i++) {
                sb.append(comma).append(" ");
                sb.append(properties[i]);
                comma = ",";
            }
        } else {
            sb.append(" SCORE");
        }
        if (ascending) {
            // FIXME really default to descending?
            sb.append(" ASCENDING");
        }
        return sb.toString();
    }

    /**
     * Returns an XPath representation for this query node.
     *
     * @return an XPath representation for this query node.
     */
    public String toXPathString() {
        return "";
    }
}
