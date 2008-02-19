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
 * Tests whether the {@link #getChildSelectorName childSelector} node is a child
 * of the {@link #getParentSelectorName parentSelector} node.  A node-tuple
 * satisfies the constraint only if:
 * <pre>  childSelectorNode.getParent().isSame(parentSelectorNode)</pre>
 * would return true, where <code>childSelectorNode</code> is the node for
 * {@link #getChildSelectorName childSelector} and <code>parentSelectorNode</code>
 * is the node for {@link #getParentSelectorName parentSelector}.
 * <p/>
 * The query is invalid if:
 * <ul>
 * <li>{@link #getChildSelectorName childSelector} is not the name of a
 * selector in the query, or</li>
 * <li>{@link #getParentSelectorName parentSelector} is not the name of a
 * selector in the query, or</li>
 * <li>{@link #getChildSelectorName childSelector} is the same as
 * {@link #getParentSelectorName parentSelector}.
 * </ul>
 *
 * @since JCR 2.0
 */
public interface ChildNodeJoinCondition extends JoinCondition {

    /**
     * Gets the name of the child selector.
     *
     * @return the selector name; non-null
     */
    String getChildSelectorName();

    /**
     * Gets the name of the parent selector.
     *
     * @return the selector name; non-null
     */
    String getParentSelectorName();

}
