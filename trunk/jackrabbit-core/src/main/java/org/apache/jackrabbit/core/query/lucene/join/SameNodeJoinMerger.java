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
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.SameNodeJoinCondition;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;

class SameNodeJoinMerger extends JoinMerger {

    private final String selector1;

    private final String selector2;

    private final String path;

    public SameNodeJoinMerger(
            Join join, Map<String, PropertyValue> columns,
            OperandEvaluator evaluator, QueryObjectModelFactory factory,
            SameNodeJoinCondition condition) throws RepositoryException {
        super(join, columns, evaluator, factory);
        this.selector1 = condition.getSelector1Name();
        this.selector2 = condition.getSelector2Name();
        this.path = condition.getSelector2Path();
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
            if (rightSelectors.contains(selector1)) {
                constraints.add(factory.sameNode(selector1, path));
            } else {
                constraints.add(factory.sameNode(selector2, path));
            }
        }
        return constraints;
    }

    private Set<String> getValues(Set<String> selectors, Row row)
            throws RepositoryException {
        if (selectors.contains(selector1)) {
            Node node = row.getNode(selector1);
            if (node != null) {
                return Collections.singleton(node.getPath());
            }
        } else if (selectors.contains(selector2)) {
            Node node = row.getNode(selector2);
            if (node != null) {
                try {
                    String p = node.getPath();
                    if (path != null && !".".equals(path)) {
                        if (!"/".equals(p)) {
                            p += "/";
                        }
                        p += path;
                    }
                    return Collections.singleton(p);
                } catch (PathNotFoundException e) {
                    // fall through
                }
            }
        } else {
            throw new RepositoryException("Invalid same node join");
        }
        return Collections.emptySet();
    }

}
