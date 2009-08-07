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

import javax.jcr.Value;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.core.query.lucene.Util;

/**
 * <code>ComparisonConstraint</code> implements a comparison constraint.
 */
public class ComparisonConstraint extends SelectorBasedConstraint
        implements QueryObjectModelConstants {

    /**
     * The dynamic operand.
     */
    private final DynamicOperand operand1;

    /**
     * The operator.
     */
    private final int operator;

    /**
     * The static operand.
     */
    private final Value operand2;

    /**
     * Creates a new comparision constraint.
     *
     * @param operand1 the dynamic operand.
     * @param operator the operator.
     * @param operand2 the static operand.
     * @param selector the selector for this constraint.
     */
    public ComparisonConstraint(DynamicOperand operand1,
                                int operator,
                                Value operand2,
                                SelectorImpl selector) {
        super(selector);
        this.operand1 = operand1;
        this.operator = operator;
        this.operand2 = operand2;
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
        Value[] values = operand1.getValues(sn, context);
        try {
            for (int i = 0; i < values.length; i++) {
                if (evaluate(values[i])) {
                    return true;
                }
            }
        } catch (RepositoryException e) {
            throw Util.createIOException(e);
        }
        return false;
    }

    /**
     * Evaluates this constraint for the given dynamic operand value
     * <code>op1</code>.
     *
     * @param op1 the current value of the dynamic operand.
     * @return <code>true</code> if the given value satisfies the constraint.
     * @throws RepositoryException if an error occurs while converting the
     *                             values.
     */
    protected boolean evaluate(Value op1) throws RepositoryException {
        int c = Util.compare(op1, operand2);
        switch (operator) {
            case OPERATOR_EQUAL_TO:
                return c == 0;
            case OPERATOR_GREATER_THAN:
                return c > 0;
            case OPERATOR_GREATER_THAN_OR_EQUAL_TO:
                return c >= 0;
            case OPERATOR_LESS_THAN:
                return c < 0;
            case OPERATOR_LESS_THAN_OR_EQUAL_TO:
                return c <= 0;
            case OPERATOR_NOT_EQUAL_TO:
                return c != 0;
            default:
                throw new IllegalStateException("unsupported operation: " + operator);
        }
    }
}
