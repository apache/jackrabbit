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
 * Implements a query node that defines a not operation on the child query.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class NotQueryNode extends NAryQueryNode {

    /**
     * Creates a new <code>NotQueryNode</code> instance.
     * @param parent the parent node for this query node.
     */
    public NotQueryNode(QueryNode parent) {
	super(parent);
    }

    /**
     * Creates a new <code>NotQueryNode</code> instance.
     * @param parent the parent node for this query node.
     * @param node the child query node to invert.
     */
    public NotQueryNode(QueryNode parent, QueryNode node) {
	super(parent, new QueryNode[] { node });
    }

    /**
     * @see QueryNode#accept(org.apache.jackrabbit.core.search.QueryNodeVisitor, java.lang.Object)
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
	return visitor.visit(this, data);
    }

    /**
     * Returns a JCRQL representation for this query node.
     * @return a JCRQL representation for this query node.
     */
    public String toJCRQLString() {
        if (operands.size() > 0) {
	    return "NOT " + ((QueryNode)operands.get(0)).toJCRQLString();
	}
	return "";
    }

    /**
     * Returns an XPath representation for this query node.
     * @return an XPath representation for this query node.
     */
    public String toXPathString() {
	// todo implement
	return "";
    }
}
