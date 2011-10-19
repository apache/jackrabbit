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

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.qom.Ordering;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;

public final class DynamicOperandFieldComparator extends
        AbstractFieldComparator {

    private final Session session;
    private final OperandEvaluator evaluator;
    private final Ordering ordering;

    public DynamicOperandFieldComparator(final Session session,
            final OperandEvaluator evaluator, final Ordering ordering,
            int numHits) {
        super(numHits);
        this.session = session;
        this.evaluator = evaluator;
        this.ordering = ordering;
    }

    @Override
    protected Comparable<ValueComparableWrapper> sortValue(int doc) {
        try {
            final String uuid = getUUIDForIndex(doc);
            final Node n = session.getNodeByIdentifier(uuid);
            final Value[] v = evaluator.getValues(ordering.getOperand(), n);
            return new ValueComparableWrapper(v);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}