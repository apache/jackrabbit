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

import java.io.IOException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.query.qom.ChildNodeImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.NodeImpl;

/**
 * <code>ChildNodeConstraint</code> implements a child node constraint.
 */
public class ChildNodeConstraint extends HierarchyConstraint {

    /**
     * Creates a child node constraint from the given QOM
     * <code>constraint</code> on the given <code>selector</code>.
     *
     * @param constraint the QOM child node constraint.
     * @param selector   the selector.
     */
    public ChildNodeConstraint(ChildNodeImpl constraint,
                               SelectorImpl selector) {
        super(constraint.getParentPath(), selector);
    }

    /**
     * {@inheritDoc}
     */
    public boolean evaluate(ScoreNode[] row,
                            Name[] selectorNames,
                            EvaluationContext context)
            throws IOException {
        ScoreNode sn = row[getSelectorIndex(selectorNames)];
        if (sn == null) {
            return false;
        }
        SessionImpl session = context.getSession();
        NodeImpl parent;
        try {
            parent = (NodeImpl) session.getNodeById(sn.getNodeId()).getParent();
        } catch (RepositoryException e) {
            return false;
        }
        return parent.getId().equals(getBaseNodeId(context));
    }
}
