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
package org.apache.jackrabbit.spi.commons.query.jsr283.qom;

/**
 * Defines a column to include in the tabular view of query results.
 * <p/>
 * If {@link #getPropertyName property} is not specified, a column is included
 * for each single-valued non-residual property of the node type specified by
 * the <code>nodeType</code> attribute of {@link #getSelectorName selector}.
 * <p/>
 * If {@link #getPropertyName property} is specified,
 * {@link #getColumnName columnName} is required and used to name the column
 * in the tabular results.  If {@link #getPropertyName property} is not
 * specified, {@link #getColumnName columnName} must not be specified, and
 * the included columns will be named
 * "{@link #getSelectorName selector}.<i>propertyName</i>".
 * <p/>
 * The query is invalid if:
 * <ul>
 * <li>{@link #getSelectorName selector} is not the name of a selector in the
 * query, or</li>
 * <li>{@link #getPropertyName property} is specified but it not a a
 * syntactically valid JCR name, or</li>
 * <li>{@link #getPropertyName property} is specified but does not evaluate to
 * a scalar value, or</li>
 * <li>{@link #getPropertyName property} is specified but
 * {@link #getColumnName columnName} is omitted, or</li>
 * <li>{@link #getPropertyName property} is omitted but
 * {@link #getColumnName columnName} is specified, or</li>
 * <li>the columns in the tabular view are not uniquely named, whether those
 * column names are specified by {@link #getColumnName columnName} (if
 * {@link #getPropertyName property} is specified) or generated as
 * described above (if {@link #getPropertyName property} is omitted).</li>
 * </ul>
 * If {@link #getPropertyName property} is specified but, for a node-tuple, the
 * selector node does not have a property named {@link #getPropertyName property},
 * the query is valid and the column has null value.
 *
 * @since JCR 2.0
 */
public interface Column {

    /**
     * Gets the name of the selector.
     *
     * @return the selector name; non-null
     */
    String getSelectorName();

    /**
     * Gets the name of the property.
     *
     * @return the property name, or null to include a column for
     *         each single-value non-residual property of the
     *         selector's node type
     */
    String getPropertyName();

    /**
     * Gets the column name.
     * <p/>
     *
     * @return the column name; must be null if
     *         <code>getPropertyName</code> is null and non-null
     *         otherwise
     */
    String getColumnName();

}
