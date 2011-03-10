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
package org.apache.jackrabbit.core.query.lucene.join;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DescendantNodeJoinCondition;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;

class DescendantNodeJoinMerger extends JoinMerger {

    private final String descendantSelector;

    private final String ancestorSelector;

    public DescendantNodeJoinMerger(
            Join join, Map<String, PropertyValue> columns,
            OperandEvaluator evaluator, QueryObjectModelFactory factory,
            DescendantNodeJoinCondition condition)
            throws RepositoryException {
        super(join, columns, evaluator, factory);
        this.descendantSelector = condition.getDescendantSelectorName();
        this.ancestorSelector = condition.getAncestorSelectorName();
    }

    @Override
    public Set<String> getLeftValues(Row row) throws RepositoryException {
        return getValues(leftSelectors, row);
    }

    @Override
    public Set<String> getRightValues(Row row) throws RepositoryException {
        return getValues(rightSelectors, row);
    }

    @Override
    public List<Constraint> getRightJoinConstraints(Collection<Row> leftRows)
            throws RepositoryException {
        Set<String> paths = new HashSet<String>();
        for (Row row : leftRows) {
            paths.addAll(getLeftValues(row));
        }

        List<Constraint> constraints = new ArrayList<Constraint>();
        for (String path : paths) {
            if (rightSelectors.contains(descendantSelector)) {
                constraints.add(
                        factory.descendantNode(descendantSelector, path));
            } else {
                constraints.add(factory.sameNode(ancestorSelector, path));
            }
        }
        return constraints;
    }

    private Set<String> getValues(Set<String> selectors, Row row)
            throws RepositoryException {
        if (selectors.contains(descendantSelector)) {
            Node node = row.getNode(descendantSelector);
            if (node != null) {
                Set<String> values = new HashSet<String>();
                while (node.getDepth() > 0) {
                    node = node.getParent();
                    values.add(node.getPath());
                }
                return values;
            }
        } else if (selectors.contains(ancestorSelector)) {
            Node node = row.getNode(ancestorSelector);
            if (node != null) {
                return Collections.singleton(node.getPath());
            }
        } else {
            throw new RepositoryException("Invalid descendant node join");
        }
        return Collections.emptySet();
    }

}
