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

import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.jcr.search.SearchResultProperty;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.IdIterator;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.value.ValueFormat;
import org.apache.jackrabbit.value.QValue;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.ValueFactory;
import java.io.InputStream;
import java.util.Iterator;
import java.util.AbstractCollection;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * <code>QueryInfoImpl</code>...
 */
public class QueryInfoImpl implements QueryInfo {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(QueryInfoImpl.class);

    private final Map results = new LinkedHashMap();

    private final QName[] columnNames;
    private final NamespaceResolver nsResolver;

    public QueryInfoImpl(MultiStatus ms, SessionInfo sessionInfo, URIResolver uriResolver,
                         NamespaceResolver nsResolver, ValueFactory valueFactory)
        throws RepositoryException {
        this.nsResolver = nsResolver;

        String responseDescription = ms.getResponseDescription();
        if (responseDescription != null) {
            String[] cn = responseDescription.split(" ");
            this.columnNames = new QName[cn.length];
            for (int i = 0; i < cn.length; i++) {
                String jcrColumnNames = ISO9075.decode(cn[i]);
                try {
                    columnNames[i] = NameFormat.parse(jcrColumnNames, nsResolver);
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

            NodeId nodeId = uriResolver.getNodeId(href, sessionInfo);
            this.results.put(nodeId, resultProp);
        }
    }

    public IdIterator getNodeIds() {
        return new IteratorHelper(new AbstractCollection() {
            public int size() {
                return results.size();
            }

            public Iterator iterator() {
                return results.keySet().iterator();
            }
        });
    }

    public QName[] getColumnNames() {
        return columnNames;
    }

    public String[] getValues(NodeId nodeId) {
        SearchResultProperty prop = (SearchResultProperty) results.get(nodeId);
        if (prop == null) {
            throw new NoSuchElementException();
        } else {
            Value[] values = prop.getValues();
            String[] ret = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                try {
                    QValue qValue = (values[i] == null) ?  null : ValueFormat.getQValue(values[i], nsResolver);
                    ret[i] = qValue.getString();
                } catch (RepositoryException e) {
                    // should not occur
                    log.error("malformed value: " + values[i].toString());
                }
            }
            return ret;
        }
    }

    public InputStream[] getValuesAsStream(NodeId nodeId) {
        SearchResultProperty prop = (SearchResultProperty) results.get(nodeId);
        if (prop == null) {
            throw new NoSuchElementException();
        } else {
            Value[] values = prop.getValues();
            InputStream[] ret = new InputStream[values.length];
            for (int i = 0; i < ret.length; i++) {
                try {
                    // make sure we return the qualified value if the type is
                    // name or path.
                    if (values[i].getType() == PropertyType.NAME || values[i].getType() == PropertyType.PATH) {
                        ret[i] = ValueFormat.getQValue(values[i], nsResolver).getStream();
                    } else {
                        ret[i] = values[i].getStream();
                    }
                } catch (RepositoryException e) {
                    // ignore this value
                    log.warn("unable to get stream value: " + values[i].toString());
                }
            }
            return ret;
        }
    }

}
