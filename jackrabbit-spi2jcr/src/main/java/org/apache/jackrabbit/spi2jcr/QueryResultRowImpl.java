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
package org.apache.jackrabbit.spi2jcr;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.jackrabbit.spi.QueryResultRow;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.query.Row;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.Node;

/**
 * <code>QueryResultRowImpl</code> implements a <code>QueryResultRow</code>
 * based on a JCR {@link javax.jcr.query.Row}.
 */
class QueryResultRowImpl implements QueryResultRow {

    /**
     * The node ids of the underlying row.
     */
    private final Map<Name, NodeId> nodeIds = new HashMap<Name, NodeId>();

    /**
     * The score values for this row.
     */
    private final Map<Name, Double> scores = new HashMap<Name, Double>();

    /**
     * The QValues for this row.
     */
    private final QValue[] values;

    /**
     * Creates a new query result row for the given <code>row</code>.
     *
     * @param row           the JCR row.
     * @param columnNames   the resolved names of the columns.
     * @param selectorNames the selector names.
     * @param idFactory     the id factory.
     * @param resolver      the name path resolver.
     * @param qValueFactory the QValue factory.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>row</code>.
     */
    public QueryResultRowImpl(Row row,
                              String[] columnNames,
                              Name[] selectorNames,
                              IdFactoryImpl idFactory,
                              NamePathResolver resolver,
                              QValueFactory qValueFactory) throws RepositoryException {
        this.values = new QValue[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            Value v = row.getValue(columnNames[i]);
            if (v == null) {
                values[i] = null;
            } else {
                values[i] = ValueFormat.getQValue(v, resolver, qValueFactory);
            }
        }
        List<Name> selNames = new ArrayList<Name>();
        selNames.addAll(Arrays.asList(selectorNames));
        if (selNames.isEmpty()) {
            selNames.add(null); // default selector
        }
        for (Name sn : selNames) {
            Node n;
            double score;
            if (sn == null) {
                n = row.getNode();
                score = row.getScore();
            } else {
                String selName = resolver.getJCRName(sn);
                n = row.getNode(selName);
                score = row.getScore(selName);
            }
            NodeId id = null;
            if (n != null) {
                id = idFactory.fromJcrIdentifier(n.getIdentifier());
            }
            nodeIds.put(sn, id);
            scores.put(sn, score);
        }
    }

    public NodeId getNodeId(Name selectorName) {
        if (nodeIds.containsKey(selectorName)) {
            return nodeIds.get(selectorName);
        } else {
            if (nodeIds.size() == 1) {
                return nodeIds.values().iterator().next();
            } else {
                throw new IllegalArgumentException(selectorName + " is not a valid selectorName");
            }
        }
    }

    public double getScore(Name selectorName) {
        Double score;
        if (scores.containsKey(selectorName)) {
            score = scores.get(selectorName);
        } else {
            if (scores.size() == 1) {
                score = scores.values().iterator().next();
            } else {
                throw new IllegalArgumentException(selectorName + " is not a valid selectorName");
            }
        }
        if (score == null) {
            return Double.NaN;
        } else {
            return score;
        }
    }

    /**
     * {@inheritDoc}
     */
    public QValue[] getValues() {
        QValue[] vals = new QValue[values.length];
        System.arraycopy(values, 0, vals, 0, values.length);
        return vals;
    }
}
