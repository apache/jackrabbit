/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.Value;
import javax.jcr.RangeIterator;

import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.QueryResultRow;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.jcr.search.SearchResultProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
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

    private static final double UNDEFINED_SCORE = -1;

    private final Name[] columnNames;
    private int scoreIndex = -1;
    private final Map results = new LinkedHashMap();

    public QueryInfoImpl(MultiStatus ms, SessionInfo sessionInfo, URIResolver uriResolver,
                         NamePathResolver resolver, ValueFactory valueFactory,
                         QValueFactory qValueFactory)
        throws RepositoryException {

        String responseDescription = ms.getResponseDescription();
        if (responseDescription != null) {
            String[] cn = responseDescription.split(" ");
            this.columnNames = new Name[cn.length];
            for (int i = 0; i < cn.length; i++) {
                String jcrColumnNames = ISO9075.decode(cn[i]);
                try {
                    columnNames[i] = resolver.getQName(jcrColumnNames);
                    if (NameConstants.JCR_SCORE.equals(columnNames[i])) {
                        scoreIndex = i;
                    }
                } catch (NameException e) {
                    throw new RepositoryException(e);
                }
            }
        } else {
            throw new RepositoryException("Missing column infos: Unable to build QueryInfo object.");
        }

        MultiStatusResponse[] responses = ms.getResponses();
        for (int i = 0; i < responses.length; i++) {
            MultiStatusResponse response = responses[i];
            String href = response.getHref();
            DavPropertySet okSet = response.getProperties(DavServletResponse.SC_OK);

            DavProperty davProp = okSet.get(SearchResultProperty.SEARCH_RESULT_PROPERTY);
            SearchResultProperty resultProp = new SearchResultProperty(davProp, valueFactory);
            Value[] values = resultProp.getValues();
            QValue[] qValues = new QValue[values.length];
            for (int j = 0; j < values.length; j++) {
                try {
                    qValues[j] = (values[j] == null) ?  null : ValueFormat.getQValue(values[j], resolver, qValueFactory);
                } catch (RepositoryException e) {
                    // should not occur
                    log.error("Malformed value: " + values[j].toString());
                }
            }

            NodeId nodeId = uriResolver.getNodeId(href, sessionInfo);
            results.put(nodeId, qValues);
        }
    }

    /**
     * @see QueryInfo#getRows()
     */
    public RangeIterator getRows() {
        return new QueryResultRowIterator();
    }

    /**
     * @see QueryInfo#getColumnNames()
     */
    public Name[] getColumnNames() {
        return columnNames;
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Inner class implementing the <code>RangeIterator</code> for the query
     * result rows. 
     */
    private class QueryResultRowIterator implements RangeIterator {

        private final Iterator keyIterator;
        private long pos = 0;

        private QueryResultRowIterator() {
            keyIterator = results.keySet().iterator();
        }

        private QueryResultRow nextQueryResultRow() {
            final NodeId nId = (NodeId) keyIterator.next();
            final QValue[] qValues = (QValue[]) results.get(nId);
            pos++;

            return new QueryResultRow() {
                /**
                 * @see QueryResultRow#getNodeId()
                 */
                public NodeId getNodeId() {
                    return nId;
                }

                /**
                 * @see QueryResultRow#getScore()
                 */
                public double getScore() {
                    if (scoreIndex != -1 && qValues[scoreIndex] != null) {
                        try {
                            return Double.parseDouble(qValues[scoreIndex].getString());
                        } catch (RepositoryException e) {
                            log.error("Error while building query score", e);
                        }   return UNDEFINED_SCORE;
                    } else {
                        log.error("Cannot determined jcr:score from query results.");
                        return UNDEFINED_SCORE;
                    }
                }

                /**
                 * @see QueryResultRow#getValues()
                 */
                public QValue[] getValues() {
                    return qValues;
                }
            };
        }

        //--------------------------------------------------< RangeIterator >---
        /**
         * @see RangeIterator#skip(long)
         */
        public void skip(long skipNum) {
            while (skipNum-- > 0) {
                nextQueryResultRow();
            }
        }

        /**
         * @see RangeIterator#getSize()
         */
        public long getSize() {
            return results.size();
        }

        /**
         * @see RangeIterator#getPosition()
         */
        public long getPosition() {
            return pos;
        }

        //-------------------------------------------------------< Iterator >---
        /**
         * @see Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException("Remove not implemented");
        }

        /**
         * @see Iterator#hasNext()
         */
        public boolean hasNext() {
            return keyIterator.hasNext();
        }

        /**
         * @see Iterator#next()
         */
        public Object next() {
            return nextQueryResultRow();
        }
    }
}
