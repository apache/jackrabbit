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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.ToStringUtils;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * <code>NameRangeQuery</code>...
 */
@SuppressWarnings("serial")
public class NameRangeQuery extends Query {

    /**
     * The lower name. May be <code>null</code> if <code>upperName</code> is not
     * <code>null</code>.
     */
    private final Name lowerName;

    /**
     * The upper name. May be <code>null</code> if <code>lowerName</code> is not
     * <code>null</code>.
     */
    private final Name upperName;

    /**
     * If <code>true</code> the range interval is inclusive.
     */
    private final boolean inclusive;

    /**
     * The index format version.
     */
    private final IndexFormatVersion version;

    /**
     * The internal namespace mappings.
     */
    private final NamespaceMappings nsMappings;

    private final PerQueryCache cache;

    /**
     * Creates a new NameRangeQuery. The lower or the upper name may be
     * <code>null</code>, but not both!
     *
     * @param lowerName the lower name of the interval, or <code>null</code>
     * @param upperName the upper name of the interval, or <code>null</code>.
     * @param inclusive if <code>true</code> the interval is inclusive.
     * @param version the index format version.
     * @param nsMappings the internal namespace mappings.
     */
    public NameRangeQuery(Name lowerName,
                          Name upperName,
                          boolean inclusive,
                          IndexFormatVersion version,
                          NamespaceMappings nsMappings,
                          PerQueryCache cache) {
        if (lowerName == null && upperName == null) {
            throw new IllegalArgumentException("At least one term must be non-null");
        }
        if (lowerName != null && upperName != null &&
                !lowerName.getNamespaceURI().equals(upperName.getNamespaceURI())) {
            throw new IllegalArgumentException("Both names must have the same namespace URI");
        }
        this.lowerName = lowerName;
        this.upperName = upperName;
        this.inclusive = inclusive;
        this.version = version;
        this.nsMappings = nsMappings;
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    public Query rewrite(IndexReader reader) throws IOException {
        Query q;
        if (version.getVersion() >= IndexFormatVersion.V3.getVersion()) {
            RangeQuery localNames = new RangeQuery(
                    getLowerLocalNameTerm(), getUpperLocalNameTerm(),
                    inclusive, cache);
            BooleanQuery query = new BooleanQuery();
            query.add(new JackrabbitTermQuery(new Term(FieldNames.NAMESPACE_URI,
                    getNamespaceURI())), BooleanClause.Occur.MUST);
            query.add(localNames, BooleanClause.Occur.MUST);
            q = query;
        } else {
            q = new RangeQuery(
                    getLowerTerm(), getUpperTerm(), inclusive, cache);
        }
        return q.rewrite(reader);
    }

    /**
     * {@inheritDoc}
     */
    public String toString(String field) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("name():");
        buffer.append(inclusive ? "[" : "{");
        buffer.append(lowerName != null ? lowerName.toString() : "null");
        buffer.append(" TO ");
        buffer.append(upperName != null ? upperName.toString() : "null");
        buffer.append(inclusive ? "]" : "}");
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    }

    //----------------------------< internal >----------------------------------

    /**
     * @return the namespace URI of this name query.
     */
    private String getNamespaceURI() {
        return lowerName != null ? lowerName.getNamespaceURI() : upperName.getNamespaceURI();
    }

    /**
     * @return the local name term of the lower name or <code>null</code> if no
     *         lower name is set.
     */
    private Term getLowerLocalNameTerm() {
        if (lowerName == null) {
            return null;
        } else {
            return new Term(FieldNames.LOCAL_NAME, lowerName.getLocalName());
        }
    }

    /**
     * @return the local name term of the upper name or <code>null</code> if no
     *         upper name is set.
     */
    private Term getUpperLocalNameTerm() {
        if (upperName == null) {
            return null;
        } else {
            return new Term(FieldNames.LOCAL_NAME, upperName.getLocalName());
        }
    }

    /**
     * @return the lower term. Must only be used for IndexFormatVersion &lt; 3.
     * @throws IOException if a name cannot be translated.
     */
    private Term getLowerTerm() throws IOException {
        try {
            String text;
            if (lowerName == null) {
                text = nsMappings.getPrefix(upperName.getNamespaceURI()) + ":";
            } else {
                text = nsMappings.translateName(lowerName);
            }
            return new Term(FieldNames.LABEL, text);
        } catch (RepositoryException e) {
            throw Util.createIOException(e);
        }
    }

    /**
     * @return the upper term. Must only be used for IndexFormatVersion &lt; 3.
     * @throws IOException if a name cannot be translated.
     */
    private Term getUpperTerm() throws IOException {
        try {
            String text;
            if (upperName == null) {
                text = nsMappings.getPrefix(lowerName.getNamespaceURI()) + ":\uFFFF";
            } else {
                text = nsMappings.translateName(upperName);
            }
            return new Term(FieldNames.LABEL, text);
        } catch (RepositoryException e) {
            throw Util.createIOException(e);
        }
    }
}
