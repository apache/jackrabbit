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

/**
 * Defines field names that are used internally to store UUID, etc in the
 * search index.
 */
public class FieldNames {

    /**
     * Private constructor.
     */
    private FieldNames() {
    }

    /**
     * Name of the field that contains the UUID of the node. Terms are stored
     * but not tokenized.
     */
    public static final String UUID = "_:UUID".intern();

    /**
     * Name of the field that contains the fulltext index including terms
     * from all properties of a node. Terms are tokenized.
     */
    public static final String FULLTEXT = "_:FULLTEXT".intern();

    /**
     * Prefix for all field names that are fulltext indexed by property name.
     */
    public static final String FULLTEXT_PREFIX = "FULL:";

    /**
     * Name of the field that contains the UUID of the parent node. Terms are
     * stored and but not tokenized.
     */
    public static final String PARENT = "_:PARENT".intern();

    /**
     * Name of the field that contains the label of the node. Terms are not
     * tokenized.
     */
    public static final String LABEL = "_:LABEL".intern();

    /**
     * Name of the field that contains the local name of the node. Terms are not
     * tokenized.
     */
    public static final String LOCAL_NAME = "_:LOCAL_NAME".intern();

    /**
     * Name of the field that contains the namespace URI of the node name. Terms
     * are not tokenized.
     */
    public static final String NAMESPACE_URI = "_:NAMESPACE_URI".intern();

    /**
     * Name of the field that contains the names of multi-valued properties that
     * hold more than one value. Terms are not tokenized and not stored, only
     * indexed.
     */
    public static final String MVP = "_:MVP".intern();

    /**
     * Name of the field that contains all values of properties that are indexed
     * as is without tokenizing. Terms are prefixed with the property name.
     */
    public static final String PROPERTIES = "_:PROPERTIES".intern();

    /**
     * Name of the field that contains the names of all properties that are set
     * on an indexed node.
     */
    public static final String PROPERTIES_SET = "_:PROPERTIES_SET".intern();

    /**
     * Name of the field that contains the UUIDs of the aggregated nodes. The
     * terms are not tokenized and not stored, only indexed.
     */
    public static final String AGGREGATED_NODE_UUID = "_:AGGR_NODE_UUID".intern();

    /**
     * Name of the field that contains the lengths of properties. The lengths
     * are encoded using {@link #createNamedLength(String, long)}.
     */
    public static final String PROPERTY_LENGTHS = "_:PROPERTY_LENGTHS".intern();

    /**
     * Name of the field that marks nodes that require reindexing because the
     * text extraction process timed out. See also {@link IndexingQueue}.
     */
    public static final String REINDEXING_REQUIRED = "_:REINDEXING_REQUIRED".intern();

    /**
     * Name of the field that marks shareable nodes.
     */
    public static final String SHAREABLE_NODE = "_:SHAREABLE_NODE".intern();

    /**
     * Name of the field that contains all weak reference property values.
     */
    public static final String WEAK_REFS = "_:WEAK_REFS".intern();

    /**
     * Returns a named length for use as a term in the index. The named length
     * is of the form: <code>propertyName</code> + '[' +
     * {@link LongField#longToString(long)}.
     *
     * @param propertyName a property name.
     * @param length the length of the property value.
     * @return the named length string for use as a term in the index.
     */
    public static String createNamedLength(String propertyName, long length) {
        return propertyName + '[' + LongField.longToString(length);
    }

    /**
     * Returns a named value for use as a term in the index. The named
     * value is of the form: <code>fieldName</code> + '[' + value
     *
     * @param fieldName the field name.
     * @param value the value.
     * @return value prefixed with field name.
     */
    public static String createNamedValue(String fieldName, String value) {
        return fieldName + '[' + value;
    }

    /**
     * Returns the length of the field prefix in <code>namedValue</code>. See
     * also {@link #createNamedValue(String, String)}. If <code>namedValue</code>
     * does not contain a name prefix, this method return 0.
     *
     * @param namedValue the named value as created by {@link #createNamedValue(String, String)}.
     * @return the length of the field prefix including the separator char '['.
     */
    public static int getNameLength(String namedValue) {
        return namedValue.indexOf('[') + 1;
    }

    /**
     * Returns <code>true</code> if the given <code>fieldName</code> denotes a
     * fulltext field like {@link #FULLTEXT} or a field with a
     * {@link #FULLTEXT_PREFIX}.
     *
     * @param fieldName a field name.
     * @return <code>true</code> if <code>fieldName</code> is a fulltext field;
     *         <code>false</code> otherwise.
     */
    public static boolean isFulltextField(String fieldName) {
        return fieldName.equals(FULLTEXT)
                || fieldName.indexOf(FULLTEXT_PREFIX) != -1;
    }
}
