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
package org.apache.jackrabbit.spi.commons.query.jsr283.qom;

/**
 * Tests whether two nodes are "the same" according to the <code>isSame</code>
 * method of <code>javax.jcr.Item</code>.
 * <p/>
 * If {@link #getSelector2Path selector2Path} is omitted:
 * <ul><li>Tests whether the {@link #getSelector1Name selector1} node is the
 * same as the {@link #getSelector2Name selector2} node.  A node-tuple
 * satisfies the constraint only if:
 * <pre>  selector1Node.isSame(selector2Node)</pre>
 * would return true, where <code>selector1Node</code> is the node
 * for {@link #getSelector1Name selector1} and <code>selector2Node</code>
 * is the node for {@link #getSelector2Name selector2}.</li></ul>
 * Otherwise, if {@link #getSelector2Path selector2Path} is specified:
 * <ul><li>Tests whether the {@link #getSelector1Name selector1} node is the
 * same as a node identified by relative path from the
 * {@link #getSelector2Name selector2} node.  A node-tuple satisfies
 * the constraint only if:
 * <pre>  selector1Node.isSame(selector2Node.getNode(selector2Path))</pre>
 * would return true, where <code>selector1Node</code> is the node for
 * {@link #getSelector1Name selector1} and <code>selector2Node</code>
 * is the node for {@link #getSelector2Name selector2}.</li></ul>
 * <p/>
 * The query is invalid if:
 * <ul>
 * <li>{@link #getSelector1Name selector1} is not the name of a selector in the
 * query, or</li>
 * <li>{@link #getSelector2Name selector2} is not the name of a selector in the
 * query, or</li>
 * <li>{@link #getSelector1Name selector1} is the same as
 * {@link #getSelector2Name selector2}, or</li>
 * <li>{@link #getSelector2Path selector2Path} is not a syntactically valid
 * relative path.  Note, however, that if the path is syntactically valid
 * but does not identify a node in the repository (or the node is not
 * visible to this session, because of access control constraints), the
 * query is valid but the constraint is not satisfied.</li>
 * </ul>
 *
 * @since JCR 2.0
 */
public interface SameNodeJoinCondition extends JoinCondition {

    /**
     * Gets the name of the first selector.
     *
     * @return the selector name; non-null
     */
    String getSelector1Name();

    /**
     * Gets the name of the second selector.
     *
     * @return the selector name; non-null
     */
    String getSelector2Name();

    /**
     * Gets the path relative to the second selector.
     *
     * @return the relative path, or null for none
     */
    String getSelector2Path();

}
