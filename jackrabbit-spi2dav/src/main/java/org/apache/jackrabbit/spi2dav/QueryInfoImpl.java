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
package org.apache.jackrabbit.spi2dav;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.RangeIterator;

import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.QueryResultRow;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>QueryInfoImpl</code>...
 */
public class QueryInfoImpl implements QueryInfo {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(QueryInfoImpl.class);

    private final String[] columnNames;

    private final List<QueryResultRow> results = new ArrayList<QueryResultRow>();

    public QueryInfoImpl(MultiStatus ms, IdFactory idFactory,
                         NamePathResolver resolver, ValueFactory valueFactory,
                         QValueFactory qValueFactory)
        throws RepositoryException {

        String responseDescription = ms.getResponseDescription();
        if (responseDescription != null) {
            String[] cn = responseDescription.split(" ");
            this.columnNames = new String[cn.length];
            for (int i = 0; i < cn.length; i++) {
                columnNames[i] = ISO9075.decode(cn[i]);
            }
        } else {
            throw new RepositoryException("Missing column infos: Unable to build QueryInfo object.");
        }

        for (MultiStatusResponse response : ms.getResponses()) {
            results.add(new QueryResultRowImpl(response, columnNames, resolver,
                    qValueFactory, valueFactory, idFactory));
        }
    }

    /**
     * @see QueryInfo#getRows()
     */
    public RangeIterator getRows() {
        return new RangeIteratorAdapter(results);
    }

    /**
     * @see QueryInfo#getColumnNames()
     */
    public String[] getColumnNames() {
        String[] names = new String[columnNames.length];
        System.arraycopy(columnNames, 0, names, 0, columnNames.length);
        return names;
    }

    /**
     * @see QueryInfo#getSelectorNames()
     */
    public Name[] getSelectorNames() {
        if (results.isEmpty()) {
            // TODO: this is not correct
            return new Name[0];
        } else {
            Set<Name> uniqueNames = new HashSet<Name>();
            QueryResultRowImpl row = (QueryResultRowImpl) results.get(0);
            for (Name n : row.getSelectorNames()) {
                if (n != null) {
                    uniqueNames.add(n);
                }
            }
            return uniqueNames.toArray(new Name[uniqueNames.size()]);
        }
    }
}
