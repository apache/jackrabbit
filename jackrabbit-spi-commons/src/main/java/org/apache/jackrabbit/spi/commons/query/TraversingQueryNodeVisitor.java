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
 * <code>TraversingQueryNodeVisitor</code> implements a base class for a
 * traversing query node visitor.
 */
public class TraversingQueryNodeVisitor extends DefaultQueryNodeVisitor {

    public Object visit(OrQueryNode node, Object data) throws RepositoryException {
        return node.acceptOperands(this, data);
    }

    public Object visit(AndQueryNode node, Object data) throws RepositoryException {
        return node.acceptOperands(this, data);
    }

    public Object visit(QueryRootNode node, Object data) throws RepositoryException {
        PathQueryNode pathNode = node.getLocationNode();
        if (pathNode != null) {
            pathNode.accept(this, data);
        }
        OrderQueryNode orderNode = node.getOrderNode();
        if (orderNode != null) {
            orderNode.accept(this, data);
        }
        return data;
    }

    public Object visit(NotQueryNode node, Object data) throws RepositoryException {
        return node.acceptOperands(this, data);
    }

    public Object visit(PathQueryNode node, Object data) throws RepositoryException {
        return node.acceptOperands(this, data);
    }

    public Object visit(LocationStepQueryNode node, Object data) throws RepositoryException {
        return node.acceptOperands(this, data);
    }

    public Object visit(DerefQueryNode node, Object data) throws RepositoryException {
        return node.acceptOperands(this, data);
    }
}
