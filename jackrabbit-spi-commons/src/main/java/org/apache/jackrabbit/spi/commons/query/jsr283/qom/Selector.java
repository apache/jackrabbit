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
 * Selects a subset of the nodes in the repository based on node type.
 * <p/>
 * A selector selects every node in the repository, subject to access control
 * constraints, that satisfies at least one of the following conditions:
 * <ul>
 * <li>the node's primary node type is {@link #getNodeTypeName nodeType},
 * or</li>
 * <li>the node's primary node type is a subtype of
 * {@link #getNodeTypeName nodeType}, or</li>
 * <li>the node has a mixin node type that is
 * {@link #getNodeTypeName nodeType}, or</li>
 * <li>the node has a mixin node type that is a subtype of
 * {@link #getNodeTypeName nodeType}.</li>
 * </ul>
 * <p/>
 * The query is invalid if {@link #getNodeTypeName nodeType} or
 * {@link #getSelectorName selectorName} is not a syntactically valid JCR name.
 * <p/>
 * The query is invalid if {@link #getSelectorName selectorName} is identical
 * to the {@link #getSelectorName selectorName} of another selector in the
 * query.
 * <p/>
 * If {@link #getNodeTypeName nodeType} is a valid JCR name but not the name
 * of a node type available in the repository, the query is valid but the
 * selector selects no nodes.
 *
 * @since JCR 2.0
 */
public interface Selector extends Source {

    /**
     * Gets the name of the required node type.
     *
     * @return the node type name; non-null
     */
    String getNodeTypeName();

    /**
     * Gets the selector name.
     * <p/>
     * A selector's name can be used elsewhere in the query to identify the
     * selector.
     *
     * @return the selector name; non-null
     */
    String getSelectorName();

}
