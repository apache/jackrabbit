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
package org.apache.jackrabbit.core.query.lucene.constraint;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.NodeImpl;

/**
 * <code>HierarchyConstraint</code> is a base class for hierarchy related
 * constraints.
 */
public abstract class HierarchyConstraint extends SelectorBasedConstraint {

    /**
     * A base path.
     */
    private final String path;

    /**
     * The id of the node at {@link #path}.
     */
    private NodeId id;

    /**
     * Creates a new hierarchy constraint with the given base
     * <code>path</code>.
     *
     * @param path     the base path.
     * @param selector the selector this constraint is placed on.
     */
    public HierarchyConstraint(String path, SelectorImpl selector) {
        super(selector);
        this.path = path;
    }

    /**
     * Returns the id of the base node or <code>null</code> if there is no node
     * at the base path.
     *
     * @param context the evaluation context.
     * @return the id or <code>null</code> if it doesn't exist.
     */
    protected final NodeId getBaseNodeId(EvaluationContext context) {
        if (id == null) {
            try {
                NodeImpl node = (NodeImpl) context.getSession().getNode(path);
                id = (NodeId) node.getId();
            } catch (RepositoryException e) {
                return null;
            }
        }
        return id;
    }
}
