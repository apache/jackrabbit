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
 * Implements an abstract base class for nodes of a query tree that represents
 * a query. The query tree is independent from the query syntax which is used
 * to search the repository.
 * <p/>
 * todo: extend QueryNode with toString method for both XPath flavours
 */
public abstract class QueryNode {

    /**
     * References the parent of this <code>QueryNode</code>. If this is the root
     * of a query tree, then <code>parent</code> is <code>null</code>.
     */
    private QueryNode parent;

    /**
     * Constructs a new <code>QueryNode</code> with a reference to it's parent.
     *
     * @param parent the parent node, or <code>null</code> if this is the root
     *               node of a query tree.
     */
    public QueryNode(QueryNode parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent <code>QueryNode</code> or <code>null</code> if this is
     * the root node of a query tree.
     *
     * @return the parent <code>QueryNode</code> or <code>null</code> if this is
     *         the root node of a query tree.
     */
    public QueryNode getParent() {
        return parent;
    }

    /**
     * Accepts a {@link QueryNodeVisitor} and calls the apropriate <code>visit</code>
     * method on the visitor depending on the concrete implementation of
     * this <code>QueryNode</code>.
     *
     * @param visitor the visitor to call back.
     * @param data    arbitrary data for the visitor.
     * @return the return value of the <code>visitor.visit()</code> call.
     */
    public abstract Object accept(QueryNodeVisitor visitor, Object data);

    /**
     * Returns a string representation of this query node including its sub-nodes.
     * The returned string is formatted in JCRQL syntax.
     *
     * @return a string representation of this query node including its sub-nodes.
     */
    public abstract String toJCRQLString();

    /**
     * Returns a string representation of this query node including its sub-nodes.
     * The returned string is formatted in XPath syntax.
     *
     * @return a string representation of this query node including its sub-nodes.
     */
    public abstract String toXPathString();
}
