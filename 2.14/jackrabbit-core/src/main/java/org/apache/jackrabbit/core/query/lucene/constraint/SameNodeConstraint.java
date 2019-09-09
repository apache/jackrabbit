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

import org.apache.jackrabbit.spi.commons.query.qom.SameNodeImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;

/**
 * <code>SameNodeConstraint</code> implements a same node constraint.
 */
public class SameNodeConstraint extends HierarchyConstraint {

    /**
     * Creates a same node constraint.
     *
     * @param constraint the QOM constraint.
     * @param selector   the selector for this constraint.
     */
    public SameNodeConstraint(SameNodeImpl constraint, SelectorImpl selector) {
        super(constraint.getPath(), selector);
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
        } else {
            return sn.getNodeId().equals(getBaseNodeId(context));
        }
    }
}
