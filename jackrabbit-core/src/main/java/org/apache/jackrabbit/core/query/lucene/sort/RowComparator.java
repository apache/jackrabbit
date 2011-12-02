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
package org.apache.jackrabbit.core.query.lucene.sort;

import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_ORDER_DESCENDING;

import java.util.Comparator;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.qom.Operand;
import javax.jcr.query.qom.Ordering;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;
import org.apache.jackrabbit.core.query.lucene.join.ValueComparator;

/**
 * Row comparator.
 */
public class RowComparator implements Comparator<Row> {

    private static final ValueComparator comparator = new ValueComparator();

    private final Ordering[] orderings;

    private final OperandEvaluator evaluator;

    public RowComparator(Ordering[] orderings, OperandEvaluator evaluator) {
        this.orderings = orderings;
        this.evaluator = evaluator;
    }

    public int compare(Row a, Row b) {
        try {
            for (Ordering ordering : orderings) {
                Operand operand = ordering.getOperand();
                Value[] va = evaluator.getValues(operand, a);
                Value[] vb = evaluator.getValues(operand, b);
                int d = comparator.compare(va, vb);
                if (d != 0) {
                    if (JCR_ORDER_DESCENDING.equals(ordering.getOrder())) {
                        return -d;
                    } else {
                        return d;
                    }
                }
            }
            return 0;
        } catch (RepositoryException e) {
            throw new RuntimeException("Unable to compare rows " + a + " and "
                    + b, e);
        }
    }
}
