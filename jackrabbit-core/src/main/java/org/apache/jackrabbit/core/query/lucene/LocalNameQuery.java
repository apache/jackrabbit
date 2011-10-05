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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import java.io.IOException;
import java.util.Set;

/**
 * <code>LocalNameQuery</code> implements a query for the local name of a node.
 */
@SuppressWarnings("serial")
public class LocalNameQuery extends Query {

    /**
     * The local name of a node.
     */
    private final String localName;

    /**
     * The index format version.
     */
    private final IndexFormatVersion version;

    /**
     * Creates a new <code>LocalNameQuery</code> for the given
     * <code>localName</code>.
     *
     * @param localName the local name of a node.
     * @param version   the version of the index.
     */
    public LocalNameQuery(String localName, IndexFormatVersion version) {
        this.localName = localName;
        this.version = version;
    }

    /**
     * {@inheritDoc}
     */
    public Query rewrite(IndexReader reader) throws IOException {
        if (version.getVersion() >= IndexFormatVersion.V3.getVersion()) {
            return new JackrabbitTermQuery(new Term(FieldNames.LOCAL_NAME, localName));
        } else {
            throw new IOException("LocalNameQuery requires IndexFormatVersion V3");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void extractTerms(Set<Term> terms) {
    }

    /**
     * {@inheritDoc}
     */
    public String toString(String field) {
        return "local-name() = " + localName;
    }
}
