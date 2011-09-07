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

/**
 * Implements an abstract base class for nodes of a query tree that represents
 * a query. The query tree is independent from the query syntax which is used
 * to search the repository.
 */
public abstract class QueryNode {

    /** Type value for {@link QueryRootNode} */
    public static final int TYPE_ROOT = 1;

    /** Type value for {@link RelationQueryNode} */
    public static final int TYPE_RELATION = 2;

    /** Type value for {@link OrderQueryNode} */
    public static final int TYPE_ORDER = 3;

    /** Type value for {@link TextsearchQueryNode} */
    public static final int TYPE_TEXTSEARCH = 4;

    /** Type value for {@link ExactQueryNode} */
    public static final int TYPE_EXACT = 5;

    /** Type value for {@link NodeTypeQueryNode} */
    public static final int TYPE_NODETYPE = 6;

    /** Type value for {@link AndQueryNode} */
    public static final int TYPE_AND = 7;

    /** Type value for {@link OrQueryNode} */
    public static final int TYPE_OR = 8;

    /** Type value for {@link NotQueryNode} */
    public static final int TYPE_NOT = 9;

    /** Type value for {@link LocationStepQueryNode} */
    public static final int TYPE_LOCATION = 10;

    /** Type value for {@link PathQueryNode} */
    public static final int TYPE_PATH = 11;

    /** Type value for {@link DerefQueryNode} */
    public static final int TYPE_DEREF = 12;

    /** Type value for {@link PropertyFunctionQueryNode} */
    public static final int TYPE_PROP_FUNCTION = 13;

    /**
     * References the parent of this <code>QueryNode</code>. If this is the root
     * of a query tree, then <code>parent</code> is <code>null</code>.
     */
    private final QueryNode parent;

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
     * Dumps this QueryNode and its child nodes to a String.
     * @return the query tree as a String.
     * @throws RepositoryException
     */
    public String dump() throws RepositoryException {
        StringBuffer tmp = new StringBuffer();
        QueryTreeDump.dump(this, tmp);
        return tmp.toString();
    }

    /**
     * Accepts a {@link QueryNodeVisitor} and calls the appropriate <code>visit</code>
     * method on the visitor depending on the concrete implementation of
     * this <code>QueryNode</code>.
     *
     * @param visitor the visitor to call back.
     * @param data    arbitrary data for the visitor.
     * @return the return value of the <code>visitor.visit()</code> call.
     * @throws RepositoryException
     */
    public abstract Object accept(QueryNodeVisitor visitor, Object data) throws RepositoryException;

    /**
     * Returns the type of this query node.
     * @return the type of this query node.
     */
    public abstract int getType();

    /**
     * Returns <code>true</code> if <code>obj</code> is the same type of
     * <code>QueryNode</code> as <code>this</code> node and is equal to
     * <code>this</code> node.
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if <code>obj</code> is equal to
     *   <code>this</code>; <code>false</code> otherwise.
     */
    public abstract boolean equals(Object obj);

    /**
     * Returns <code>true</code> if this query node needs items under
     * /jcr:system to be queried.
     *
     * @return <code>true</code> if this query node needs content under
     *         /jcr:system to be queried; <code>false</code> otherwise.
     */
    public abstract boolean needsSystemTree();
}
