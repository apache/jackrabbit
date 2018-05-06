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

import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.EquiJoinCondition;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;

class EquiJoinMerger extends JoinMerger {

    private final PropertyValue leftProperty;

    private final PropertyValue rightProperty;

    public EquiJoinMerger(
            Join join, Map<String, PropertyValue> columns,
            OperandEvaluator evaluator, QueryObjectModelFactory factory,
            EquiJoinCondition condition) throws RepositoryException {
        super(join, columns, evaluator, factory);

        PropertyValue property1 = factory.propertyValue(
                condition.getSelector1Name(), condition.getProperty1Name());
        PropertyValue property2 = factory.propertyValue(
                condition.getSelector2Name(), condition.getProperty2Name());

        if (leftSelectors.contains(property1.getSelectorName())
                && rightSelectors.contains(property2.getSelectorName())) {
            leftProperty = property1;
            rightProperty = property2;
        } else if (leftSelectors.contains(property2.getSelectorName())
                && rightSelectors.contains(property1.getSelectorName())) {
            leftProperty = property2;
            rightProperty = property1;
        } else {
            throw new RepositoryException("Invalid equi-join");
        }
    }

    @Override
    public Set<String> getLeftValues(Row row) throws RepositoryException {
        return getValues(leftProperty, row);
    }

    @Override
    public Set<String> getRightValues(Row row) throws RepositoryException {
        return getValues(rightProperty, row);
    }

    @Override
    public List<Constraint> getRightJoinConstraints(Collection<Row> leftRows)
            throws RepositoryException {
        Map<String, Literal> literals = new HashMap<String, Literal>();
        for (Row leftRow : leftRows) {
            for (Value value : evaluator.getValues(leftProperty, leftRow)) {
                literals.put(value.getString(), factory.literal(value));
            }
        }

        List<Constraint> constraints =
            new ArrayList<Constraint>(literals.size());
        for (Literal literal : literals.values()) {
            constraints.add(factory.comparison(
                    rightProperty, JCR_OPERATOR_EQUAL_TO, literal));
        }
        return constraints;
    }

    private Set<String> getValues(PropertyValue property, Row row)
            throws RepositoryException {
        Set<String> strings = new HashSet<String>();
        for (Value value : evaluator.getValues(property, row)) {
            strings.add(value.getString());
        }
        return strings;
    }

}
