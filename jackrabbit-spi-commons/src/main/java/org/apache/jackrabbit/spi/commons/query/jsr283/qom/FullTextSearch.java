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
 * Performs a full-text search.
 * <p/>
 * The full-text search expression is evaluated against the set of full-text
 * indexed properties within the full-text search scope.  If
 * {@link #getPropertyName property} is specified, the full-text search scope
 * is the property of that name on the {@link #getSelectorName selector} node
 * in the node-tuple; otherwise the full-text search scope is all properties
 * of the {@link #getSelectorName selector} node (or, in some implementations,
 * all properties in the node subtree).
 * <p/>
 * Which properties (if any) in a repository are full-text indexed is
 * implementation determined.
 * <p/>
 * It is also implementation determined whether
 * {@link #getFullTextSearchExpression fullTextSearchExpression} is
 * independently evaluated against each full-text indexed property in the
 * full-text search scope, or collectively evaluated against the set of such
 * properties using some implementation-determined mechanism.
 * <p/>
 * Similarly, for multi-valued properties, it is implementation determined
 * whether {@link #getFullTextSearchExpression fullTextSearchExpression} is
 * independently evaluated against each element in the array of values, or
 * collectively evaluated against the array of values using some
 * implementation-determined mechanism.
 * <p/>
 * At minimum, an implementation must support the following
 * {@link #getFullTextSearchExpression fullTextSearchExpression} grammar:
 * <pre>  fullTextSearchExpression ::= [-]term {whitespace [OR] whitespace [-]term}
 * <p/>
 *  term ::= word | '"' word {whitespace word} '"'
 * <p/>
 *  word ::= (A string containing no whitespace)
 * <p/>
 *  whitespace ::= (A string of only whitespace)
 * </pre>
 * <p/>
 * A query satisfies a <code>FullTextSearch</code> constraint if the
 * value (or values) of the full-text indexed properties within the
 * full-text search scope satisfy the specified
 * {@link #getFullTextSearchExpression fullTextSearchExpression},
 * evaluated as follows:
 * <ul>
 * <li>A term not preceded with "<code>-</code>" (minus sign) is satisfied
 * only if the value contains that term.</li>
 * <li>A term preceded with "<code>-</code>" (minus sign) is satisfied only
 * if the value does not contain that term.</li>
 * <li>Terms separated by whitespace are implicitly "ANDed".</li>
 * <li>Terms separated by "<code>OR</code>" are "ORed".</li>
 * <li>"AND" has higher precedence than "OR".
 * <li>Within a term, each double quote (<code>"</code>), "<code>-</code>"
 * (minus sign), and "<code>\</code>" (backslash) must be escaped by a
 * preceding "<code>\</code>" (backslash).</li>
 * </ul>
 * <p/>
 * The query is invalid if:
 * <ul>
 * <li>{@link #getSelectorName selector} is not the name of a selector in the
 * query, or</li>
 * <li>{@link #getPropertyName property} is specified but is not a syntactically
 * valid JCR name, or</li>
 * <li>{@link #getFullTextSearchExpression fullTextSearchExpression} does not
 * conform to the above grammar (as augmented by the implementation).</li>
 * </ul>
 * <p/>
 * If {@link #getPropertyName property} is specified but, for a node-tuple,
 * the selector node does not have a property named {@link #getPropertyName
 * property}, the query is valid but the constraint is not satisfied.</li>
 *
 * @since JCR 2.0
 */
public interface FullTextSearch extends Constraint {

    /**
     * Gets the name of the selector against which to apply this constraint.
     *
     * @return the selector name; non-null
     */
    String getSelectorName();

    /**
     * Gets the name of the property.
     *
     * @return the property name if the full-text search scope
     *         is a property, otherwise null if the full-text
     *         search scope is the node (or node subtree, in
     *         some implementations).
     */
    String getPropertyName();

    /**
     * Gets the full-text search expression.
     *
     * @return the full-text search expression; non-null
     */
    String getFullTextSearchExpression();

}
