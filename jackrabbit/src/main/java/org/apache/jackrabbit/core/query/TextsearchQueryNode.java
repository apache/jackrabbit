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
package org.apache.jackrabbit.core.query;

import org.apache.jackrabbit.name.QName;

/**
 * Implements a query node that defines a textsearch clause.
 */
public class TextsearchQueryNode extends QueryNode {

    /**
     * The query statement inside the textsearch clause
     */
    private final String query;

    /**
     * Limits the scope of this textsearch clause to properties with this name.
     * If <code>null</code> the scope of this textsearch clause is the fulltext
     * index of all properties of a node.
     */
    private QName propertyName;

    /**
     * Creates a new <code>TextsearchQueryNode</code> with a <code>parent</code>
     * and a textsearch <code>query</code> statement. The scope of the query
     * is the fulltext index of the node, that contains all properties.
     *
     * @param parent the parent node of this query node.
     * @param query  the textsearch statement.
     */
    public TextsearchQueryNode(QueryNode parent, String query) {
        this(parent, query, null);
    }

    /**
     * Creates a new <code>TextsearchQueryNode</code> with a <code>parent</code>
     * and a textsearch <code>query</code> statement. The scope of the query
     * is property with name <code>propertyName</code>.
     *
     * @param parent the parent node of this query node.
     * @param query  the textsearch statement.
     * @param propertyName scope of the fulltext search.
     */
    public TextsearchQueryNode(QueryNode parent, String query, QName propertyName) {
        super(parent);
        this.query = query;
        this.propertyName = propertyName;
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

    /**
     * Returns a property name if the scope is limited to just a single property
     * or <code>null</code> if the scope is spawned across all properties of a
     * node.
     *
     * @return property name or <code>null</code>.
     */
    public QName getPropertyName() {
        return propertyName;
    }

    /**
     * Sets a new name as the search scope for this fulltext query.
     *
     * @param property the name of the property.
     */
    public void setPropertyName(QName property) {
        this.propertyName = property;
    }

    /**
     * @inheritDoc
     */
    public boolean equals(Object obj) {
        if (obj instanceof TextsearchQueryNode) {
            TextsearchQueryNode other = (TextsearchQueryNode) obj;
            return (query == null ? other.query == null : query.equals(other.query))
                    && (propertyName == null ? other.propertyName == null : propertyName.equals(other.propertyName));
        }
        return false;
    }
}
