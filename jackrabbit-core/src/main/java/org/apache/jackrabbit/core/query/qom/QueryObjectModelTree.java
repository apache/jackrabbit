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
package org.apache.jackrabbit.core.query.qom;

import org.apache.jackrabbit.name.NamePathResolver;

import javax.jcr.query.InvalidQueryException;

/**
 * <code>QueryObjectModelTree</code> implements the root node of an object
 * query model tree.
 */
public class QueryObjectModelTree extends AbstractQOMNode {

    /**
     * The node-tuple source for this query.
     */
    private final SourceImpl source;

    /**
     * The constraint for this query.
     */
    private final ConstraintImpl constraint;

    /**
     * The orderings for this query.
     */
    private final OrderingImpl[] orderings;

    /**
     * The columns for this query.
     */
    private final ColumnImpl[] columns;

    public QueryObjectModelTree(NamePathResolver resolver,
                                SourceImpl source,
                                ConstraintImpl constraint,
                                OrderingImpl[] orderings,
                                ColumnImpl[] columns)
            throws InvalidQueryException {
        super(resolver);
        this.source = source;
        this.constraint = constraint;
        this.orderings = orderings;
        this.columns = columns;
        checkQuery();
    }

    /**
     * Gets the node-tuple source for this query.
     *
     * @return the node-tuple source; non-null
     */
    public SourceImpl getSource() {
        return source;
    }

    /**
     * Gets the constraint for this query.
     *
     * @return the constraint, or null if none
     */
    public ConstraintImpl getConstraint() {
        return constraint;
    }

    /**
     * Gets the orderings for this query.
     *
     * @return an array of zero or more orderings; non-null
     */
    public OrderingImpl[] getOrderings() {
        OrderingImpl[] temp = new OrderingImpl[orderings.length];
        System.arraycopy(orderings, 0, temp, 0, orderings.length);
        return temp;
    }

    /**
     * Gets the columns for this query.
     *
     * @return an array of zero or more columns; non-null
     */
    public ColumnImpl[] getColumns() {
        ColumnImpl[] temp = new ColumnImpl[columns.length];
        System.arraycopy(columns, 0, temp, 0, columns.length);
        return temp;
    }

    //-----------------------< AbstractQOMNode >--------------------------------

    /**
     * Accepts a <code>visitor</code> and calls the appropriate visit method
     * depending on the type of this QOM node.
     *
     * @param visitor the visitor.
     */
    public Object accept(QOMTreeVisitor visitor, Object data) throws Exception {
        return visitor.visit(this, data);
    }

    /**
     * Checks if this QOM is valid.
     *
     * @throws InvalidQueryException if the QOM is invalid.
     */
    private void checkQuery() throws InvalidQueryException {
        // TODO: validate query
    }
}
