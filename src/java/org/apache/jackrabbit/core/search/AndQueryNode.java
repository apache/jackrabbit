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

import java.util.Iterator;

/**
 * Implements a query node that defines an AND operation between arbitrary
 * other {@link QueryNode}s.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class AndQueryNode extends NAryQueryNode {

    /**
     * Creates a new <code>AndQueryNode</code> with a <code>parent</code>
     * query node.
     *
     * @param parent the parent of <code>this</code> <code>AndQueryNode</code>.
     */
    public AndQueryNode(QueryNode parent) {
	super(parent);
    }

    /**
     * Creates a new <code>AndQueryNode</code> with a <code>parent</code> query
     * node and <code>operands</code> for <code>this</code>
     * <code>AndQueryNode</code>.
     *
     * @param parent   the parent of <code>this</code> <code>AndQueryNode</code>.
     * @param operands the operands for this AND operation.
     */
    public AndQueryNode(QueryNode parent, QueryNode[] operands) {
	super(parent, operands);
    }

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
	StringBuffer sb = new StringBuffer();
	boolean bracket = false;
	if (getParent() instanceof NotQueryNode) {
	    bracket = true;
	}
	if (bracket) sb.append("(");
	String and = "";
	for (Iterator it = operands.iterator(); it.hasNext(); ) {
	    sb.append(and);
	    sb.append(((QueryNode)it.next()).toJCRQLString());
	    and = " AND ";
	}
	if (bracket) sb.append(")");
	return sb.toString();
    }

    /**
     * Returns a string representation of this query node including its sub-nodes.
     * The returned string is formatted in XPath syntax.
     *
     * @return a string representation of this query node including its sub-nodes.
     */
    public String toXPathString() {
	// @todo implement
	return "";
    }
}
