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

import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.RangeIterator;

import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.QueryResultRow;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;

/**
 * <code>QueryInfoImpl</code>...
 */
public class QueryInfoImpl implements QueryInfo {

    private static final String COLUMNS = "Columns: ";

    private static final String SELECTORS = "Selectors: ";

    private final List<String> columnNames = new ArrayList<String>();

    private final List<String> selectorNames = new ArrayList<String>();

    private final List<QueryResultRow> results = new ArrayList<QueryResultRow>();

    public QueryInfoImpl(MultiStatus ms, IdFactory idFactory,
                         NamePathResolver resolver, ValueFactory valueFactory,
                         QValueFactory qValueFactory)
        throws RepositoryException {

        String responseDescription = ms.getResponseDescription();
        if (responseDescription == null) {
            throw new RepositoryException(
                    "Missing column infos: Unable to build QueryInfo object.");
        }
        if (responseDescription.startsWith(COLUMNS)) {
            for (String line : responseDescription.split("\n")) {
                if (line.startsWith(COLUMNS)) {
                    decode(line.substring(COLUMNS.length()), columnNames);
                } else if (line.startsWith(SELECTORS)) {
                    decode(line.substring(SELECTORS.length()), selectorNames);
                }
            }
        } else {
            // Backwards compatibility with old servers that only provide
            // the list of columns as the response description
            decode(responseDescription, columnNames);
        }

        for (MultiStatusResponse response : ms.getResponses()) {
            results.add(new QueryResultRowImpl(
                    response, getColumnNames(), resolver,
                    qValueFactory, valueFactory, idFactory));
        }
    }

    /**
     * Splits the given string at spaces and ISO9075-decodes the parts.
     *
     * @param string source string
     * @param list where the decoded parts get added
     */
    private void decode(String string, List<String> list) {
        String[] parts = string.split(" ");
        for (int i = 0; i < parts.length; i++) {
            list.add(ISO9075.decode(parts[i]));
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
        return columnNames.toArray(new String[columnNames.size()]);
    }

    /**
     * @see QueryInfo#getSelectorNames()
     */
    public String[] getSelectorNames() {
        return selectorNames.toArray(new String[selectorNames.size()]);
    }
}
