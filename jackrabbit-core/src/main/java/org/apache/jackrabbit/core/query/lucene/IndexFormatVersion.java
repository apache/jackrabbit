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

import java.util.Collection;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;

/**
 * This class indicates the lucene index format that is used.
 * <ul>
 * <li><b>Version 1</b> is the initial index format, which is used for Jackrabbit
 * releases 1.0 to 1.3.x. Unless a re-index happens upgraded Jackrabbit
 * instances will still use this version.</li>
 * <li><b>Version 2</b> is the index format introduced with Jackrabbit 1.4.x. It
 * adds a <code>PROPERTIES_SET</code> field which contains all property names of
 * a node. This speeds up queries that check the existence of a property.</li>
 * <li><b>Version 3</b> is the index format introduced with Jackrabbit 1.5.x. It
 * adds support for length queries using the newly added
 * <code>PROPERTY_LENGTHS</code> field. Furthermore a Payload is added to
 * <code>PROPERTIES</code> fields to indicate the property type.</li>
 * </ul>
 * Please note that existing indexes are not automatically upgraded to a newer
 * version! If you want to take advantage of a certain 'feature' in an index
 * format version you need to re-index the repository.
 */
public class IndexFormatVersion {

    /**
     * V1 is the index format for Jackrabbit releases 1.0 to 1.3.x.
     */
    public static final IndexFormatVersion V1 = new IndexFormatVersion(1);

    /**
     * V2 is the index format for Jackrabbit releases 1.4.x
     */
    public static final IndexFormatVersion V2 = new IndexFormatVersion(2);

    /**
     * V3 is the index format for Jackrabbit releases >= 1.5
     */
    public static final IndexFormatVersion V3 = new IndexFormatVersion(3);

    /**
     * The used version of the index format
     */
    private final int version;

    /**
     * Creates a index format version.
     *
     * @param version       The version of the index.
     */
    private IndexFormatVersion(int version) {
        this.version = version;
    }

    /**
     * Returns the index format version
     * @return the index format version.
     */
    public int getVersion() {
        return version;
    }

    /**
     * @return a string representation of this index format version.
     */
    public String toString() {
        return String.valueOf(getVersion());
    }

    /**
     * @return the index format version of the index used by the given
     * index reader.
     */
    public static IndexFormatVersion getVersion(IndexReader indexReader)
            throws IOException {
        Collection fields = indexReader.getFieldNames(
                IndexReader.FieldOption.ALL);
        if (hasPayloads(indexReader) || indexReader.numDocs() == 0) {
            return IndexFormatVersion.V3;
        } else if (fields.contains(FieldNames.PROPERTIES_SET)) {
            return IndexFormatVersion.V2;
        } else {
            return IndexFormatVersion.V1;
        }
    }

    /**
     * @param reader the index reader.
     * @return <code>true</code> if the {@link FieldNames#PROPERTIES} fields
     *         contain payloads; <code>false</code> otherwise.
     * @throws IOException if an error occurs while reading from the index.
     */
    public static boolean hasPayloads(IndexReader reader) throws IOException {
        TermPositions tp = reader.termPositions();
        try {
            TermEnum terms = reader.terms(
                    new Term(FieldNames.PROPERTIES, ""));
            try {
                if (terms.next() && terms.term().field() == FieldNames.PROPERTIES) {
                    tp.seek(terms);
                    if (tp.next()) {
                        tp.nextPosition();
                        return tp.isPayloadAvailable();
                    }
                }
            } finally {
                terms.close();
            }
        } finally {
            tp.close();
        }
        return false;
    }
}
