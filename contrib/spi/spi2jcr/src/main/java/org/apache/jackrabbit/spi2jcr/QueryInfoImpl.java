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

import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.QueryResultRowIterator;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Row;
import javax.jcr.RepositoryException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <code>QueryInfoImpl</code> implements a <code>QueryInfo</code> based on a
 * JCR {@link javax.jcr.query.QueryResult}.
 */
class QueryInfoImpl implements QueryInfo {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(QueryInfoImpl.class);

    /**
     * The underlying query result.
     */
    private final QueryResult result;

    /**
     * The id factory.
     */
    private final IdFactoryImpl idFactory;

    /**
     * The namespace resolver.
     */
    private final NamespaceResolver nsResolver;

    /**
     * The QValue factory.
     */
    private final QValueFactory qValueFactory;

    /**
     * The names of the columns in the query result.
     */
    private final QName[] columnNames;

    /**
     * The resolved name of the jcr:score column.
     */
    private final String scoreName;

    /**
     * The resolved name of the jcr:path column.
     */
    private final String pathName;

    /**
     * Creates a new query info based on a given <code>result</code>.
     *
     * @param result        the JCR query result.
     * @param idFactory     the id factory.
     * @param nsResolver    the namespace resolver in use.
     * @param qValueFactory the QValue factory.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>result</code>.
     */
    public QueryInfoImpl(QueryResult result,
                         IdFactoryImpl idFactory,
                         NamespaceResolver nsResolver,
                         QValueFactory qValueFactory)
            throws RepositoryException {
        this.result = result;
        this.idFactory = idFactory;
        this.nsResolver = nsResolver;
        this.qValueFactory = qValueFactory;
        String[] jcrNames = result.getColumnNames();
        this.columnNames = new QName[jcrNames.length];
        try {
            for (int i = 0; i < jcrNames.length; i++) {
                columnNames[i] = NameFormat.parse(jcrNames[i], nsResolver);
            }
            this.scoreName = NameFormat.format(QName.JCR_SCORE, nsResolver);
            this.pathName = NameFormat.format(QName.JCR_PATH, nsResolver);
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QueryResultRowIterator getRows() {
        final String[] columnJcrNames;
        final RowIterator rows;
        try {
            columnJcrNames = result.getColumnNames();
            rows = result.getRows();
        } catch (RepositoryException e) {
            return IteratorHelper.EMPTY;
        }
        return new IteratorHelper(new Iterator() {
            public void remove() {
                rows.remove();
            }

            public boolean hasNext() {
                return rows.hasNext();
            }

            public Object next() {
                try {
                    Row row = rows.nextRow();
                    return new QueryResultRowImpl(row, columnJcrNames, scoreName,
                            pathName, idFactory, nsResolver, qValueFactory);
                } catch (RepositoryException e) {
                    log.warn("Exception when creating QueryResultRowImpl: " +
                            e.getMessage(), e);
                    throw new NoSuchElementException();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getColumnNames() {
        QName[] names = new QName[columnNames.length];
        System.arraycopy(columnNames, 0, names, 0, columnNames.length);
        return names;
    }
}
