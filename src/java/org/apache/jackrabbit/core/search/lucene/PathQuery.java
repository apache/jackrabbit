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
package org.apache.jackrabbit.core.search.lucene;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;
import org.apache.jackrabbit.core.search.PathQueryNode;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;

/**
 * Implements a query for a path with a match type.
 */
class PathQuery extends BooleanQuery {

    /**
     * The path to query
     */
    private final Path path;

    /**
     * The path type.
     * The path <code>type</code> must be one of:
     * <ul>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_EXACT}</li>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_CHILDREN}</li>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_DESCENDANT_SELF}</li>
     * </ul>
     */
    private final int type;

    private final int index;

    private final NamespaceResolver nsMappings;

    /**
     * Creates a <code>PathQuery</code> for a <code>path</code> and a path
     * <code>type</code>. The query does not care about a specific index.
     * <p/>
     * The path <code>type</code> must be one of: <ul> <li>{@link
     * org.apache.jackrabbit.core.search.PathQueryNode#TYPE_EXACT}</li>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_CHILDREN}</li>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_DESCENDANT_SELF}</li>
     * </ul>
     *
     * @param path     the base path
     * @param resolver namespace resolver to resolve <code>path</code>.
     * @param type     the path type.
     * @throws NullPointerException     if path is null.
     * @throws IllegalArgumentException if type is not one of the defined types
     *                                  in {@link org.apache.jackrabbit.core.search.PathQueryNode}.
     */
    PathQuery(Path path, NamespaceResolver resolver, int type) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (type < PathQueryNode.TYPE_EXACT || type > PathQueryNode.TYPE_DESCENDANT_SELF) {
            throw new IllegalArgumentException("type: " + type);
        }
        this.path = path;
        this.nsMappings = resolver;
        this.type = type;
        index = -1;
        populateQuery();
    }

    /**
     * Creates a <code>PathQuery</code> for a <code>path</code>, a path
     * <code>type</code> and a position index for the last location step.
     * <p/>
     * The path <code>type</code> must be one of: <ul> <li>{@link
     * org.apache.jackrabbit.core.search.PathQueryNode#TYPE_EXACT}</li>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_CHILDREN}</li>
     * <li>{@link org.apache.jackrabbit.core.search.PathQueryNode#TYPE_DESCENDANT_SELF}</li>
     * </ul>
     *
     * @param path     the base path
     * @param type     the path type.
     * @param index    position index of the last location step.
     * @param resolver namespace resolver to resolve <code>path</code>.
     * @throws NullPointerException     if path is null.
     * @throws IllegalArgumentException if type is not one of the defined types
     *                                  in {@link org.apache.jackrabbit.core.search.PathQueryNode}.
     *                                  Or if <code>index</code> &lt; 1.
     */
    PathQuery(Path path, NamespaceResolver resolver, int type, int index) {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (type < PathQueryNode.TYPE_EXACT || type > PathQueryNode.TYPE_DESCENDANT_SELF) {
            throw new IllegalArgumentException("type: " + type);
        }
        if (index < 1) {
            throw new IllegalArgumentException("index: " + index);
        }
        this.path = path;
        this.nsMappings = resolver;
        this.type = type;
        this.index = index;
        populateQuery();
    }

    /**
     * Populates this <code>BooleanQuery</code> with clauses according
     * to the path and match type.
     */
    private void populateQuery() {
        try {
            if (type == PathQueryNode.TYPE_EXACT) {
                Term t = new Term(FieldNames.PATH, path.toJCRPath(nsMappings));
                add(new TermQuery(t), true, false);
            } else if (type == PathQueryNode.TYPE_CHILDREN) {
                if (path.denotesRoot()) {
                    // get all nodes on level 1
                    add(new TermQuery(new Term(FieldNames.LEVEL, String.valueOf(1))),
                            true, false);
                } else {
                    Term t = new Term(FieldNames.ANCESTORS,
                            path.toJCRPath(nsMappings));
                    add(new TermQuery(t), true, false);
                    int level = path.getAncestorCount() + 1;
                    add(new TermQuery(new Term(FieldNames.LEVEL, String.valueOf(level))),
                            true, false);
                }
            } else {
                if (path.denotesRoot()) {
                    // no restrictions
                } else {
                    String jcrPath = path.toJCRPath(nsMappings);
                    // descendant or self
                    Term t = new Term(FieldNames.PATH, jcrPath);
                    // self
                    add(new TermQuery(t), false, false);
                    // or nodes with ancestors = self
                    t = new Term(FieldNames.ANCESTORS, jcrPath);
                    add(new TermQuery(t), false, false);
                }
            }
        } catch (NoPrefixDeclaredException e) {
            // will never happen, this.nsMappings dynamically adds unknown
            // uri->prefix mappings
        }
    }
}
