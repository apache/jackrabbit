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

/**
 * Implements a query node that defines a textsearch clause.
 */
public class TextsearchQueryNode extends QueryNode {

    /**
     * The query statement inside the textsearch clause
     */
    private final String query;

    /**
     * Creates a new <code>TextsearchQueryNode</code> with a <code>parent</code>
     * and a textsearch <code>query</code> statement.
     *
     * @param parent the parent node of this query node.
     * @param query  the textsearch statement.
     */
    public TextsearchQueryNode(QueryNode parent, String query) {
        super(parent);
        this.query = query;
    }

    /**
     * {@inheritDoc}
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Returns the type of this node.
     *
     * @return the type of this node.
     */
    public int getType() {
        return QueryNode.TYPE_TEXTSEARCH;
    }

    /**
     * Returns the textsearch statement.
     *
     * @return the textsearch statement.
     */
    public String getQuery() {
        return query;
    }

}
