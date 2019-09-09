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
import javax.jcr.query.qom.ChildNodeJoinCondition;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;

class ChildNodeJoinMerger extends JoinMerger {

    private final String childSelector;

    private final String parentSelector;

    public ChildNodeJoinMerger(
            Join join, Map<String, PropertyValue> columns,
            OperandEvaluator evaluator, QueryObjectModelFactory factory,
            ChildNodeJoinCondition condition)
            throws RepositoryException {
        super(join, columns, evaluator, factory);
        this.childSelector = condition.getChildSelectorName();
        this.parentSelector = condition.getParentSelectorName();
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
        for (String path: paths) {
            if (rightSelectors.contains(childSelector)) {
                constraints.add(factory.childNode(childSelector, path));
            } else {
                constraints.add(factory.sameNode(parentSelector, path));
            }
        }
        return constraints;
    }

    private Set<String> getValues(Set<String> selectors, Row row)
            throws RepositoryException {
        if (selectors.contains(childSelector)) {
            Node node = row.getNode(childSelector);
            if (node != null && node.getDepth() > 0) {
                return Collections.singleton(node.getParent().getPath());
            }
        } else if (selectors.contains(parentSelector)) {
            Node node = row.getNode(parentSelector);
            if (node != null) {
                return Collections.singleton(node.getPath());
            }
        } else {
            throw new RepositoryException("Invalid child node join");
        }
        return Collections.emptySet();
    }

}
