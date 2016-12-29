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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Defines an abstract query node for nodes that have child nodes.
 */
public abstract class NAryQueryNode<T extends QueryNode> extends QueryNode {

    /**
     * Empty result.
     */
    private static final Object[] EMPTY = new Object[0];

    /**
     * The list of operands / children
     */
    protected List<T> operands = null;

    /**
     * Creates a new <code>NAryQueryNode</code> with a reference to a parent
     * {@link QueryNode}.
     *
     * @param parent the parent node.
     */
    public NAryQueryNode(QueryNode parent) {
        super(parent);
    }

    /**
     * Creates a new <code>NAryQueryNode</code> with a reference to a parent
     * {@link QueryNode} and initial <code>operands</code>.
     *
     * @param parent   the parent node.
     * @param operands child nodes of this <code>NAryQueryNode</code>.
     */
    public NAryQueryNode(QueryNode parent, T[] operands) {
        super(parent);
        if (operands.length > 0) {
            this.operands = new ArrayList<T>();
            this.operands.addAll(Arrays.asList(operands));
        }
    }

    /**
     * Adds a new <code>operand</code> (child node) to this query node.
     *
     * @param operand the child {@link QueryNode} to add.
     */
    public void addOperand(T operand) {
        if (operands == null) {
            operands = new ArrayList<T>();
        }
        operands.add(operand);
    }

    /**
     * Removes an <code>operand</code> (child node) from this query node.
     *
     * @param operand the child to remove.
     * @return <code>true</code> if the operand was in the list of child nodes
     *  and has been removed; <code>false</code> if this node does not contain
     *  <code>operand</code> as a child node.
     */
    public boolean removeOperand(T operand) {
        if (operands == null) {
            return false;
        }
        // JCR-1650 search the operand without relying on Object#equals(Object)
        Iterator it = operands.iterator();
        while (it.hasNext()) {
            if (it.next() == operand) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an array of currently set <code>QueryNode</code> operands of this
     * <code>QueryNode</code>. Returns an empty array if no operands are set.
     *
     * @return currently set <code>QueryNode</code> operands.
     */
    public QueryNode[] getOperands() {
        if (operands == null) {
            return new QueryNode[0];
        } else {
            return operands.toArray(new QueryNode[operands.size()]);
        }
    }

    /**
     * Returns the number of operands.
     * @return the number of operands.
     */
    public int getNumOperands() {
        if (operands == null) {
            return 0;
        } else {
            return operands.size();
        }
    }

    /**
     * Helper class to accept a <code>visitor</code> for all operands
     * of this <code>NAryQueryNode</code>.
     *
     * @param visitor the visitor to call back.
     * @param data    arbitrary data for the visitor.
     * @return the return values of the <code>visitor.visit()</code> calls.
     * @throws RepositoryException if an error occurs.
     */
    public Object[] acceptOperands(QueryNodeVisitor visitor, Object data) throws RepositoryException {
        if (operands == null) {
            return EMPTY;
        }

        List<Object> result = new ArrayList<Object>(operands.size());
        for (T operand : operands) {
            Object r = operand.accept(visitor, data);
            if (r != null) {
                result.add(r);
            }
        }
        return result.toArray();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof NAryQueryNode) {
            NAryQueryNode other = (NAryQueryNode) obj;
            return operands == null ? other.operands == null : operands.equals(other.operands);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean needsSystemTree() {
        if (operands == null) {
            return false;
        }
        for (T operand : operands) {
            if (operand.needsSystemTree()) {
                return true;
            }
        }
        return false;
    }

}
