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

import org.apache.jackrabbit.spi.Name;

/**
 * A factory for {@link QueryNode}s.
 */
public interface QueryNodeFactory {

    /**
     * Creates a {@link NodeTypeQueryNode} instance.
     *
     * @param parent the parent node.
     * @param nodeType the name of the node type.
     * @return a {@link NodeTypeQueryNode}.
     */
    public NodeTypeQueryNode createNodeTypeQueryNode(
            QueryNode parent, Name nodeType);

    /**
     * Creates a {@link AndQueryNode} instance.
     *
     * @param parent the parent node.
     * @return a {@link AndQueryNode}.
     */
    public AndQueryNode createAndQueryNode(
            QueryNode parent);

    /**
     * Creates a {@link LocationStepQueryNode} instance.
     *
     * @param parent the parent node.
     * @return a {@link LocationStepQueryNode}.
     */
    public LocationStepQueryNode createLocationStepQueryNode(
            QueryNode parent);

    /**
     * Creates a {@link DerefQueryNode} instance.
     *
     * @param parent the parent node.
     * @param nameTest the name test on the referenced target node.
     * @param descendants if the axis is //
     * @return a {@link DerefQueryNode}.
     */
    public DerefQueryNode createDerefQueryNode(
            QueryNode parent, Name nameTest, boolean descendants);

    /**
     * Creates a {@link NotQueryNode} instance.
     *
     * @param parent the parent node.
     * @return a {@link NotQueryNode}.
     */
    public NotQueryNode createNotQueryNode(
            QueryNode parent);

    /**
     * Creates a {@link OrQueryNode} instance.
     *
     * @param parent the parent node.
     * @return a {@link OrQueryNode}.
     */
    public OrQueryNode createOrQueryNode(
            QueryNode parent);

    /**
     * Creates a {@link RelationQueryNode} instance.
     *
     * @param parent the parent node.
     * @param operation the operation type.
     * @return a {@link RelationQueryNode}.
     */
    public RelationQueryNode createRelationQueryNode(
            QueryNode parent, int operation);

    /**
     * Creates a {@link PathQueryNode} instance.
     *
     * @param parent the parent node.
     * @return a {@link PathQueryNode}.
     */
    public PathQueryNode createPathQueryNode(
            QueryNode parent);

    /**
     * Creates a {@link OrderQueryNode} instance.
     *
     * @param parent the parent node.
     * @return a {@link OrderQueryNode}.
     */
    public OrderQueryNode createOrderQueryNode(
            QueryNode parent);

    /**
     * Creates a {@link PropertyFunctionQueryNode} instance.
     *
     * @param parent the parent node.
     * @param functionName the name of the function.
     * @return a {@link PropertyFunctionQueryNode}.
     */
    public PropertyFunctionQueryNode createPropertyFunctionQueryNode(
            QueryNode parent, String functionName);

    /**
     * Creates a {@link QueryRootNode} instance.
     *
     * @return a {@link QueryRootNode}.
     */
    public QueryRootNode createQueryRootNode();

    /**
     * Creates a {@link TextsearchQueryNode} instance.
     *
     * @param parent the parent node.
     * @param query the textsearch statement.
     * @return a {@link TextsearchQueryNode}.
     */
    public TextsearchQueryNode createTextsearchQueryNode(
            QueryNode parent, String query);
}