/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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

import org.apache.jackrabbit.core.QName;

/**
 * Implements a query node that defines the order of nodes according to the
 * values of properties.
 */
public class OrderQueryNode extends QueryNode {

    /**
     * The name of the properties to order
     */
    private QName[] properties;

    /**
     * Array of flag indicating whether a node is ordered ascending or descending
     */
    private boolean[] orderSpecs;

    /**
     * Creates a new <code>OrderQueryNode</code> with a reference to a parent
     * node and sort properties.
     *
     * @param parent     the parent node of this query node.
     * @param properties the names of the properties to sort the result nodes.
     * @param orderSpecs if <code>true</code> a result node is orderd ascending;
     *                   otherwise descending.
     */
    public OrderQueryNode(QueryNode parent, QName[] properties, boolean[] orderSpecs) {
        super(parent);
        if (properties.length != orderSpecs.length) {
            throw new IllegalArgumentException("Number of propertes and orderSpecs must be the same");
        }
        this.properties = properties;
        this.orderSpecs = orderSpecs;
    }


    /**
     * @see QueryNode#accept(org.apache.jackrabbit.core.search.QueryNodeVisitor, java.lang.Object)
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Returns <code>true</code> if the property <code>i</code> should be orderd
     * ascending. If <code>false</code> the property is ordered descending.
     * @param i index of the property
     *
     * @return the order spec for the property <code>i</code>.
     *
     * @exception ArrayIndexOutOfBoundsException if there is no property with
     * index <code>i</code>.
     */
    public boolean isAscending(int i) {
        return orderSpecs[i];
    }

    /**
     * Returns a <code>QName</code> array that contains the name of the properties
     * to sort the result nodes.
     *
     * @return names of order properties.
     */
    public QName[] getOrderByProperties() {
        return properties;
    }

    /**
     * Returns a boolean array that contains the sort order specification
     * for each property returned by {@link #getOrderByProperties()}.
     * @return the sort specification.
     */
    public boolean[] getOrderBySpecs() {
        return orderSpecs;
    }

}
