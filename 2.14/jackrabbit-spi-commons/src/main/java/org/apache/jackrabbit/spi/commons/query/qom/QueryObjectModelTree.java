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
package org.apache.jackrabbit.spi.commons.query.qom;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.query.InvalidQueryException;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Iterator;

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

    /**
     * All selectors available in this query object model. Key=Name
     */
    private final Map selectors = new HashMap();

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
        for (Iterator it = Arrays.asList(source.getSelectors()).iterator(); it.hasNext(); ) {
            SelectorImpl selector = (SelectorImpl) it.next();
            if (selectors.put(selector.getSelectorQName(), selector) != null) {
                throw new InvalidQueryException("Duplicate selector name: " +
                        selector.getSelectorName());
            }
        }
        if (selectors.size() == 1) {
            // there is only one selector, which is also a default selector
            selectors.put(null, selectors.values().iterator().next());
        }
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

    /**
     * Returns the selector with the given <code>name</code> or
     * <code>null</code> if there is no selector with this name.
     *
     * @param name the name of a selector.
     * @return the selector or <code>null</code> if there is no such selector.
     */
    public SelectorImpl getSelector(Name name) {
        return (SelectorImpl) selectors.get(name);
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
        // TODO: validate query completely.
        // checks currently implemented:
        // - check for selector names
        try {
            accept(new DefaultTraversingQOMTreeVisitor() {
                public Object visit(ChildNodeImpl node, Object data) throws Exception {
                    return checkSelector(node.getSelectorQName());
                }

                public Object visit(ColumnImpl node, Object data) throws Exception {
                    return checkSelector(node.getSelectorQName());
                }

                public Object visit(DescendantNodeImpl node, Object data) throws Exception {
                    return checkSelector(node.getSelectorQName());
                }

                public Object visit(EquiJoinConditionImpl node, Object data)
                        throws Exception {
                    checkSelector(node.getSelector1QName());
                    return checkSelector(node.getSelector2QName());
                }

                public Object visit(FullTextSearchImpl node, Object data) throws Exception {
                    return checkSelector(node.getSelectorQName());
                }

                public Object visit(FullTextSearchScoreImpl node, Object data)
                        throws Exception {
                    return checkSelector(node.getSelectorQName());
                }

                public Object visit(NodeLocalNameImpl node, Object data) throws Exception {
                    return checkSelector(node.getSelectorQName());
                }

                public Object visit(NodeNameImpl node, Object data) throws Exception {
                    return checkSelector(node.getSelectorQName());
                }

                public Object visit(PropertyExistenceImpl node, Object data)
                        throws Exception {
                    return checkSelector(node.getSelectorQName());
                }

                public Object visit(PropertyValueImpl node, Object data) throws Exception {
                    return checkSelector(node.getSelectorQName());
                }

                public Object visit(SameNodeImpl node, Object data) throws Exception {
                    return checkSelector(node.getSelectorQName());
                }

                public Object visit(SameNodeJoinConditionImpl node, Object data)
                        throws Exception {
                    checkSelector(node.getSelector1QName());
                    return checkSelector(node.getSelector2QName());
                }

                private Object checkSelector(Name selectorName)
                        throws InvalidQueryException {
                    if (!selectors.containsKey(selectorName)) {
                        String msg = "Unknown selector: ";
                        if (selectorName != null) {
                            msg += QueryObjectModelTree.this.getJCRName(selectorName);
                        } else {
                            msg += "<default>";
                        }
                        throw new InvalidQueryException(msg);
                    }
                    return null;
                }
            }, null);
        } catch (Exception e) {
            throw new InvalidQueryException(e.getMessage());
        }
    }

    //------------------------< Object >----------------------------------------

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        if (columns != null && columns.length > 0) {
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(columns[i]);
            }
        } else {
            builder.append("*");
        }

        builder.append(" FROM ");
        builder.append(source);

        if (constraint != null) {
            builder.append(" WHERE ");
            builder.append(constraint);
        }

        if (orderings != null && orderings.length > 0) {
            builder.append(" ORDER BY ");
            for (int i = 0; i < orderings.length; i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(orderings[i]);
            }
        }

        return builder.toString();
    }


}
