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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines an abstract query node for nodes that have child nodes.
 */
public abstract class NAryQueryNode extends QueryNode {

    /**
     * Empty result.
     */
    private static final Object[] EMPTY = new Object[0];

    /**
     * The list of operands / children
     */
    protected List operands = null;

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
    public NAryQueryNode(QueryNode parent, QueryNode[] operands) {
        super(parent);
        this.operands.addAll(Arrays.asList(operands));
    }

    /**
     * Adds a new <code>operand</code> (child node) to this query node.
     *
     * @param operand the child {@link QueryNode} to add.
     */
    public void addOperand(QueryNode operand) {
        if (operands == null) {
            operands = new ArrayList();
        }
        operands.add(operand);
    }

    /**
     * Helper class to accept a <code>visitor</code> for all operands
     * of this <code>NAryQueryNode</code>.
     *
     * @param visitor the visitor to call back.
     * @param data    arbitrary data for the visitor.
     * @return the return values of the <code>visitor.visit()</code> calls.
     */
    public Object[] acceptOperands(QueryNodeVisitor visitor, Object data) {
        if (operands == null) {
            return EMPTY;
        }
        
        List result = new ArrayList(operands.size());
        for (int i = 0; i < operands.size(); i++) {
            Object r = ((QueryNode) operands.get(i)).accept(visitor, data);
            if (r != null) {
                result.add(r);
            }
        }
        return result.toArray();
    }
}
