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

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Defines an abstract query node for nodes that have child nodes.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public abstract class NAryQueryNode extends QueryNode {

    /** The list of operands / children */
    protected List operands = new ArrayList();

    /**
     * Creates a new <code>NAryQueryNode</code> with a reference to a parent
     * {@link QueryNode}.
     * @param parent the parent node.
     */
    public NAryQueryNode(QueryNode parent) {
	super(parent);
    }

    /**
     * Creates a new <code>NAryQueryNode</code> with a reference to a parent
     * {@link QueryNode} and initial <code>operands</code>.
     * @param parent the parent node.
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
	operands.add(operand);
    }

    /**
     * Helper class to accept a <code>visitor</code> for all operands
     * of this <code>NAryQueryNode</code>.
     *
     * @param visitor the visitor to call back.
     * @param data arbitrary data for the visitor.
     * @return the return values of the <code>visitor.visit()</code> calls.
     */
    public Object[] acceptOperands(QueryNodeVisitor visitor, Object data) {
	Object[] result = new Object[operands.size()];
	for (int i = 0; i < operands.size(); i++) {
	    result[i] = ((QueryNode)operands.get(i)).accept(visitor, data);
	}
	return result;
    }
}
