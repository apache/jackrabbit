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

import java.io.IOException;
import java.util.Map;

import javax.jcr.Session;
import javax.jcr.query.qom.Ordering;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

public final class DynamicOperandFieldComparatorSource extends
        FieldComparatorSource {

    private static final long serialVersionUID = 1L;

    private final Session session;
    private final OperandEvaluator evaluator;
    private Map<String, Ordering> orderByProperties;

    public DynamicOperandFieldComparatorSource(final Session session,
            final OperandEvaluator evaluator,
            final Map<String, Ordering> orderByProperties) {
        this.session = session;
        this.evaluator = evaluator;
        this.orderByProperties = orderByProperties;
    }

    @Override
    public FieldComparator newComparator(String fieldname, int numHits,
            int sortPos, boolean reversed) throws IOException {
        final Ordering o = orderByProperties.get(fieldname);
        return new DynamicOperandFieldComparator(session, evaluator, o, numHits);
    }
}