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
 * Tests whether the {@link #getDescendantSelectorName descendantSelector} node
 * is a descendant of the {@link #getAncestorSelectorName ancestorSelector}
 * node.  A node-tuple satisfies the constraint only if:
 * <pre>  descendantSelectorNode.getAncestor(n).isSame(ancestorSelectorNode) &&
 *     descendantSelectorNode.getDepth() > n</pre>
 * would return true some some non-negative integer <code>n</code>, where
 * <code>descendantSelectorNode</code> is the node for
 * {@link #getDescendantSelectorName descendantSelector} and
 * <code>ancestorSelectorNode</code> is the node for
 * {@link #getAncestorSelectorName ancestorSelector}.
 * <p/>
 * The query is invalid if:
 * <ul>
 * <li>{@link #getDescendantSelectorName descendantSelector} is not the name
 * of a selector in the query, or</li>
 * <li>{@link #getAncestorSelectorName ancestorSelector} is not the name of a
 * selector in the query, or</li>
 * <li>{@link #getDescendantSelectorName descendantSelector} is the same as
 * {@link #getAncestorSelectorName ancestorSelector}.
 * </ul>
 *
 * @since JCR 2.0
 */
public interface DescendantNodeJoinCondition extends JoinCondition {

    /**
     * Gets the name of the descendant selector.
     *
     * @return the selector name; non-null
     */
    String getDescendantSelectorName();

    /**
     * Gets the name of the ancestor selector.
     *
     * @return the selector name; non-null
     */
    String getAncestorSelectorName();

}
