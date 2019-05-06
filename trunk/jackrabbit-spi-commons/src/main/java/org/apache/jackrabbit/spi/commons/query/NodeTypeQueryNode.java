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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * Implements a query node that defines a node type match.
 */
public class NodeTypeQueryNode extends ExactQueryNode {

    /**
     * Creates a new <code>NodeTypeQueryNode</code>.
     *
     * @param parent   the parent node for this query node.
     * @param nodeType the name of the node type.
     */
    protected NodeTypeQueryNode(QueryNode parent, Name nodeType) {
        // we only use the jcr primary type as a dummy value
        // the property name is actually replaced in the query builder
        // when the runtime query is created to search the index.
        super(parent, NameConstants.JCR_PRIMARYTYPE, nodeType);
    }

    /**
     * {@inheritDoc}
     * @throws RepositoryException
     */
    public Object accept(QueryNodeVisitor visitor, Object data) throws RepositoryException {
        return visitor.visit(this, data);
    }

    /**
     * Returns the type of this node.
     *
     * @return the type of this node.
     */
    public int getType() {
        return QueryNode.TYPE_NODETYPE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof NodeTypeQueryNode) {
            return super.equals(obj);
        }
        return false;
    }
}
