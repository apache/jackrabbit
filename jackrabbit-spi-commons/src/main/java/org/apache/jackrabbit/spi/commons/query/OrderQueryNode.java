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
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;

/**
 * Implements a query node that defines the order of nodes according to the
 * values of properties.
 */
public class OrderQueryNode extends QueryNode {

    /**
     * The order spects
     */
    private final List specs = new ArrayList();

    /**
     * Creates a new <code>OrderQueryNode</code> with a reference to a parent
     * node and sort properties.
     *
     * @param parent the parent node of this query node.
     */
    protected OrderQueryNode(QueryNode parent) {
        super(parent);
    }

    /**
     * Returns the type of this node.
     *
     * @return the type of this node.
     */
    public int getType() {
        return QueryNode.TYPE_ORDER;
    }

    /**
     * Adds an order specification to this query node.
     *
     * @param property  the name of the property.
     * @param ascending if <code>true</code> values of this properties are
     *                  ordered ascending; descending if <code>false</code>.
     * @deprecated use {@link #addOrderSpec(Path , boolean)} instead.
     */
    public void addOrderSpec(Name property, boolean ascending) {
        addOrderSpec(createPath(property), ascending);
    }

    /**
     * Adds an order specification to this query node.
     *
     * @param property  the relative path of the property.
     * @param ascending if <code>true</code> values of this properties are
     *                  ordered ascending; descending if <code>false</code>.
     */
    public void addOrderSpec(Path property, boolean ascending) {
        specs.add(new OrderSpec(property, ascending));
    }

    /**
     * Adds an order specification to this query node.
     *
     * @param spec the order spec.
     */
    public void addOrderSpec(OrderSpec spec) {
        specs.add(spec);
    }

    /**
     * {@inheritDoc}
     * @throws RepositoryException
     */
    public Object accept(QueryNodeVisitor visitor, Object data) throws RepositoryException {
        return visitor.visit(this, data);
    }

    /**
     * Returns <code>true</code> if the property <code>i</code> should be orderd
     * ascending. If <code>false</code> the property is ordered descending.
     *
     * @param i index of the property
     * @return the order spec for the property <code>i</code>.
     * @throws IndexOutOfBoundsException if there is no property with
     *                                   index <code>i</code>.
     */
    public boolean isAscending(int i) throws IndexOutOfBoundsException {
        return ((OrderSpec) specs.get(i)).ascending;
    }

    /**
     * Returns a <code>OrderSpec</code> array that contains order by
     * specifications.
     *
     * @return order by specs.
     */
    public OrderSpec[] getOrderSpecs() {
        return (OrderSpec[]) specs.toArray(new OrderSpec[specs.size()]);
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object obj) {
        if (obj instanceof OrderQueryNode) {
            OrderQueryNode other = (OrderQueryNode) obj;
            return specs.equals(other.specs);
        }
        return false;
    }

    //------------------< OrderSpec class >-------------------------------------

    /**
     * Implements a single order specification. Contains a property name
     * and whether it is ordered ascending or descending.
     */
    public static final class OrderSpec {

        /**
         * The relative path to of the property
         */
        private final Path property;

        /**
         * If <code>true</code> this property is orderd ascending
         */
        private boolean ascending;

        /**
         * Creates a new <code>OrderSpec</code> for <code>property</code>.
         *
         * @param property  the name of the property.
         * @param ascending if <code>true</code> the property is ordered
         *                  ascending, otherwise descending.
         * @deprecated use {@link OrderSpec#OrderSpec(Path, boolean)} instead.
         */
        public OrderSpec(Name property, boolean ascending) {
            this(createPath(property), ascending);
        }

        /**
         * Creates a new <code>OrderSpec</code> for <code>property</code>.
         *
         * @param property  the relative path of the property.
         * @param ascending if <code>true</code> the property is ordered
         *                  ascending, otherwise descending.
         */
        public OrderSpec(Path property, boolean ascending) {
            this.property = property;
            this.ascending = ascending;
        }

        /**
         * Returns the name of the property.
         *
         * @return the name of the property.
         * @deprecated use {@link #getPropertyPath()} instead.
         */
        public Name getProperty() {
            return property.getNameElement().getName();
        }

        /**
         * Returns the relative path of the property.
         *
         * @return the relative path of the property.
         */
        public Path getPropertyPath() {
            return property;
        }

        /**
         * If <code>true</code> the property is ordered ascending, otherwise
         * descending.
         *
         * @return <code>true</code> for ascending; <code>false</code> for
         *         descending.
         */
        public boolean isAscending() {
            return ascending;
        }

        /**
         * Sets the new value for the ascending property.
         *
         * @param ascending <code>true</code> for ascending; <code>false</code>
         *                  for descending.
         */
        public void setAscending(boolean ascending) {
            this.ascending = ascending;
        }

        /**
         * Returns <code>true</code> if <code>this</code> order spec is equal
         * to <code>obj</code>
         * @param obj the reference object with which to compare.
         * @return <code>true</code> if <code>this</code> order spec is equal
         *   to <code>obj</code>; <code>false</code> otherwise.
         */
        public boolean equals(Object obj) {
            if (obj instanceof OrderSpec) {
                OrderSpec other = (OrderSpec) obj;
                return (property == null ? other.property == null : property.equals(other.property))
                        && ascending == other.ascending;
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean needsSystemTree() {
        return false;
    }

    //--------------------------------< internal >------------------------------

    /**
     * Creates a path with a single element out of the given <code>name</code>.
     *
     * @param name the name to create the path from.
     * @return a path with a single element.
     */
    private static Path createPath(Name name) {
        try {
            PathBuilder builder = new PathBuilder();
            builder.addLast(name);
            return builder.getPath();
        } catch (MalformedPathException e) {
            // never happens, we just added an element
            throw new InternalError();
        }
    }
}
