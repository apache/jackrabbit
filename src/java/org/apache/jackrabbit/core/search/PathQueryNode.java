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
 * Implements a query node that defines a path restriction.
 */
public class PathQueryNode extends QueryNode {

    /**
     * Match path exact
     */
    public static final int TYPE_EXACT = 1;

    /**
     * Match child nodes of path
     */
    public static final int TYPE_CHILDREN = 2;

    /**
     * Match descendant nodes or self of path
     */
    public static final int TYPE_DESCENDANT_SELF = 3;

    /**
     * The base path
     */
    private final String path;

    /**
     * Converted path without indexes. /bla[2] -> /bla
     */
    private final String indexlessPath;

    /**
     * The match type for this query node
     */
    private final int type;

    /**
     * Flag indicating if this path query contains indexed location steps
     */
    private final boolean indexedName;

    /**
     * Creates a new <code>PathQueryNode</code> instance.
     *
     * @param parent this parent node of this query node.
     * @param path   the base path.
     * @param type   one of {@link #TYPE_CHILDREN}, {@link #TYPE_DESCENDANT_SELF},
     *               {@link #TYPE_EXACT}
     */
    public PathQueryNode(QueryNode parent, String path, int type) {
        super(parent);
        if (type < TYPE_EXACT || type > TYPE_DESCENDANT_SELF) {
            throw new IllegalArgumentException(String.valueOf(type));
        }
        this.path = path;
        this.type = type;
        this.indexedName = (path.indexOf('[') > -1);
        if (indexedName) {
            // also create an indexless path
            StringBuffer tmp = new StringBuffer(path);
            int idx;
            while ((idx = tmp.indexOf("[")) > -1) {
                int end = tmp.indexOf("]", idx);
                if (end > -1) {
                    tmp.replace(idx, end, "");
                } else {
                    // should never happen
                    // FIXME do some error logging?
                    break;
                }
            }
            this.indexlessPath = tmp.toString();
        } else {
            this.indexlessPath = path;
        }
    }


    /**
     * @see QueryNode#accept(org.apache.jackrabbit.core.search.QueryNodeVisitor, java.lang.Object)
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Returns the unmodified path for this query node, as passed to the
     * constructor.
     *
     * @return the unmodified path for this query node.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns a normalized path without indexes.
     *
     * @return a normalized path without indexes.
     */
    public String getIndexlessPath() {
        return indexlessPath;
    }

    /**
     * Returns the type of this <code>PathQueryNode</code>.
     *
     * @return one of {@link #TYPE_CHILDREN}, {@link #TYPE_DESCENDANT_SELF},
     *         {@link #TYPE_EXACT}
     */
    public int getType() {
        return type;
    }

    /**
     * Returns <code>true</code> if the path contains an index. E.g. a location
     * step in XPath has a position predicate. If the path does not contain any
     * indexes <code>false</code> is returned.
     *
     * @return <code>true</code> if the path contains an indexed location step.
     */
    public boolean hasIndexedName() {
        return indexedName;
    }


    /**
     * Returns a JCRQL representation for this query node.
     *
     * @return a JCRQL representation for this query node.
     */
    public String toJCRQLString() {
        StringBuffer jcrql = new StringBuffer("LOCATION ");
        jcrql.append(path);
        if (type == TYPE_CHILDREN) {
            jcrql.append("/*");
        } else if (type == TYPE_DESCENDANT_SELF) {
            jcrql.append("//");
        }
        return jcrql.toString();
    }

    /**
     * Returns an XPath representation for this query node.
     *
     * @return an XPath representation for this query node.
     */
    public String toXPathString() {
        // todo implement correctly.
        return "";
    }
}
