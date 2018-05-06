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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;

/**
 * Implements a query node that defines a textsearch clause.
 */
public class TextsearchQueryNode extends QueryNode {

    /**
     * The query statement inside the textsearch clause
     */
    private final String query;

    /**
     * Limits the scope of this textsearch clause to a node or a property with
     * the given relative path.
     * If <code>null</code> the scope of this textsearch clause is the fulltext
     * index of all properties of the context node.
     */
    private Path relPath;

    /**
     * If set to <code>true</code> {@link #relPath} references a property,
     * otherwise references a node.
     */
    private boolean propertyRef;

    /**
     * Creates a new <code>TextsearchQueryNode</code> with a <code>parent</code>
     * and a textsearch <code>query</code> statement. The scope of the query
     * is the fulltext index of the node, that contains all properties.
     *
     * @param parent the parent node of this query node.
     * @param query  the textsearch statement.
     */
    protected TextsearchQueryNode(QueryNode parent, String query) {
        super(parent);
        this.query = query;
        this.relPath = null;
        this.propertyRef = false;
    }

    /**
     * {@inheritDoc}
     * @throws RepositoryException
     */
    public Object accept(QueryNodeVisitor visitor, Object data) throws RepositoryException {
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
     * node. Please note that this method does not return the full relative path
     * that reference the item to match, but only the name of the final name
     * element of the path returned by {@link #getRelativePath()}.
     *
     * @return property name or <code>null</code>.
     * @deprecated Use {@link #getRelativePath()} instead.
     */
    public Name getPropertyName() {
        return relPath == null ? null : relPath.getName();
    }

    /**
     * Sets a new name as the search scope for this fulltext query.
     *
     * @param property the name of the property.
     * @deprecated Use {@link #setRelativePath(Path)} instead.
     */
    public void setPropertyName(Name property) {
        PathBuilder builder = new PathBuilder();
        builder.addLast(property);
        try {
            this.relPath = builder.getPath();
            this.propertyRef = true;
        } catch (MalformedPathException e) {
            // path is always valid
        }
    }

    /**
     * @return the relative path that references the item where the textsearch
     *         is performed. Returns <code>null</code> if the textsearch is
     *         performed on the context node.
     */
    public Path getRelativePath() {
        return relPath;
    }

    /**
     * Sets the relative path to the item where the textsearch is performed. If
     * <code>relPath</code> is <code>null</code> the textsearch is performed on
     * the context node.
     *
     * @param relPath the relative path to an item.
     * @throws IllegalArgumentException if <code>relPath</code> is absolute.
     */
    public void setRelativePath(Path relPath) {
        if (relPath != null && relPath.isAbsolute()) {
            throw new IllegalArgumentException("relPath must be relative");
        }
        this.relPath = relPath;
        if (relPath == null) {
            // context node is never a property
            propertyRef = false;
        }
    }

    /**
     * Adds a path element to the existing relative path. To add a path element
     * which matches all node names use {@link RelationQueryNode#STAR_NAME_TEST}.
     *
     * @param element the path element to append.
     */
    public void addPathElement(Path.Element element) {
        PathBuilder builder = new PathBuilder();
        if (relPath != null) {
            builder.addAll(relPath.getElements());
        }
        builder.addLast(element);
        try {
            relPath = builder.getPath();
        } catch (MalformedPathException e) {
            // path is always valid
        }
    }

    /**
     * @return <code>true</code> if {@link #getRelativePath()} references a
     *         property, returns <code>false</code> if it references a node.
     */
    public boolean getReferencesProperty() {
        return propertyRef;
    }

    /**
     * Is set to <code>true</code>, indicates that {@link #getRelativePath()}
     * references a property, if set to <code>false</code> indicates that it
     * references a node.
     *
     * @param b flag whether a property is referenced.
     */
    public void setReferencesProperty(boolean b) {
        propertyRef = b;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof TextsearchQueryNode) {
            TextsearchQueryNode other = (TextsearchQueryNode) obj;
            return (query == null ? other.query == null : query.equals(other.query))
                    && (relPath == null ? other.relPath == null : relPath.equals(other.relPath)
                    && propertyRef == other.propertyRef);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean needsSystemTree() {
        return false;
    }

}
