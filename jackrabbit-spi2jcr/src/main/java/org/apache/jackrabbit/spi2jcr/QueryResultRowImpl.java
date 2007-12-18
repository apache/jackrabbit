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

import org.apache.jackrabbit.spi.QueryResultRow;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;

import javax.jcr.query.Row;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * <code>QueryResultRowImpl</code> implements a <code>QueryResultRow</code>
 * based on a JCR {@link javax.jcr.query.Row}.
 */
class QueryResultRowImpl implements QueryResultRow {

    /**
     * The node id of the underlying row.
     */
    private final NodeId nodeId;

    /**
     * The score value for this row.
     */
    private final double score;

    /**
     * The QValues for this row.
     */
    private final QValue[] values;

    /**
     * Creates a new query result row for the given <code>row</code>.
     *
     * @param row           the JCR row.
     * @param columnNames   the resolved names of the columns.
     * @param scoreName     the name of the jcr:score column.
     * @param pathName      the name of the jcr:path column
     * @param idFactory     the id factory.
     * @param resolver
     * @param qValueFactory the QValue factory.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>row</code>.
     */
    public QueryResultRowImpl(Row row,
                              String[] columnNames,
                              String scoreName,
                              String pathName,
                              IdFactoryImpl idFactory,
                              NamePathResolver resolver,
                              QValueFactory qValueFactory) throws RepositoryException {
        String jcrPath = row.getValue(pathName).getString();
        Path path;
        try {
            path = resolver.getQPath(jcrPath);
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        this.nodeId = idFactory.createNodeId((String) null, path);
        this.score = row.getValue(scoreName).getDouble();
        this.values = new QValue[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            Value v = row.getValue(columnNames[i]);
            if (v == null) {
                values[i] = null;
            } else {
                values[i] = ValueFormat.getQValue(v, resolver, qValueFactory);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getNodeId() {
        return nodeId;
    }

    /**
     * {@inheritDoc}
     */
    public double getScore() {
        return score;
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
