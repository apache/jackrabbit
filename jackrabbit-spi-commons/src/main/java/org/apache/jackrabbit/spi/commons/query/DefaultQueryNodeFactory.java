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

import java.util.Collection;

import org.apache.jackrabbit.spi.Name;

/**
 * Default implementation of a {@link QueryNodeFactory}.
 */
public class DefaultQueryNodeFactory implements QueryNodeFactory {

    /**
     * Valid node type names under /jcr:system
     */
    private final Collection<Name> validJcrSystemNodeTypeNames;

    /**
     * Creates a DefaultQueryNodeFactory with the given node types under
     * /jcr:system .
     */
    public DefaultQueryNodeFactory(
            Collection<Name> validJcrSystemNodeTypeNames) {
        this.validJcrSystemNodeTypeNames = validJcrSystemNodeTypeNames;
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeQueryNode createNodeTypeQueryNode(QueryNode parent,
                                                     Name nodeType) {
        return new NodeTypeQueryNode(parent, nodeType);
    }

    /**
     * {@inheritDoc}
     */
    public AndQueryNode createAndQueryNode(QueryNode parent) {
        return new AndQueryNode(parent);
    }

    /**
     * {@inheritDoc}
     */
    public LocationStepQueryNode createLocationStepQueryNode(QueryNode parent) {
        return new LocationStepQueryNode(parent);
    }

    /**
     * {@inheritDoc}
     */
    public DerefQueryNode createDerefQueryNode(QueryNode parent,
                                               Name nameTest,
                                               boolean descendants) {
        return new DerefQueryNode(parent, nameTest, descendants);
    }

    /**
     * {@inheritDoc}
     */
    public NotQueryNode createNotQueryNode(QueryNode parent) {
        return new NotQueryNode(parent);
    }

    /**
     * {@inheritDoc}
     */
    public OrQueryNode createOrQueryNode(QueryNode parent) {
        return new OrQueryNode(parent);
    }

    /**
     * {@inheritDoc}
     */
    public RelationQueryNode createRelationQueryNode(QueryNode parent,
                                                     int operation) {
        return new RelationQueryNode(parent, operation, this);
    }

    /**
     * {@inheritDoc}
     */
    public PathQueryNode createPathQueryNode(QueryNode parent) {
        return new PathQueryNode(parent, validJcrSystemNodeTypeNames);
    }

    /**
     * {@inheritDoc}
     */
    public OrderQueryNode createOrderQueryNode(QueryNode parent) {
        return new OrderQueryNode(parent);
    }

    /**
     * {@inheritDoc}
     */
    public PropertyFunctionQueryNode createPropertyFunctionQueryNode(
            QueryNode parent, String functionName) {
        return new PropertyFunctionQueryNode(parent, functionName);
    }

    /**
     * {@inheritDoc}
     */
    public QueryRootNode createQueryRootNode() {
        return new QueryRootNode();
    }

    /**
     * {@inheritDoc}
     */
    public TextsearchQueryNode createTextsearchQueryNode(QueryNode parent,
                                                         String query) {
        return new TextsearchQueryNode(parent, query);
    }
}
