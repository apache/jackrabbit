/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.QName;

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
    public NodeTypeQueryNode(QueryNode parent, QName nodeType) {
        // we only use the jcr primary type as a dummy value
        // the property name is actually replaced in the query builder
        // when the runtime query is created to search the index.
        super(parent, Constants.JCR_PRIMARYTYPE, nodeType);
    }

    /**
     * {@inheritDoc}
     */
    public Object accept(QueryNodeVisitor visitor, Object data) {
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
}
