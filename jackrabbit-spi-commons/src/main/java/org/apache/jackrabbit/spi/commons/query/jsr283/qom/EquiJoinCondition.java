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
 * Tests whether the value of a property in a first selector is equal to the
 * value of a property in a second selector.
 * <p/>
 * A node-tuple satisfies the constraint only if:
 * <ul>
 * <li>{@link #getSelector1Name selector1} has a property named
 * {@link #getProperty1Name property1}, and</li>
 * <li>{@link #getSelector2Name selector2} has a property named
 * {@link #getProperty2Name property2}, and</li>
 * <li>the value of {@link #getProperty1Name property1} equals the value of
 * {@link #getProperty2Name property2}</li>
 * </ul>
 * <p/>
 * The query is invalid if:
 * <ul>
 * <li>{@link #getSelector1Name selector1} is not the name of a selector in
 * the query, or</li>
 * <li>{@link #getSelector2Name selector2} is not the name of a selector in
 * the query, or</li>
 * <li>{@link #getSelector1Name selector1} is the same as
 * {@link #getSelector2Name selector2}, or</li>
 * <li>{@link #getProperty1Name property1} is not a syntactically valid JCR
 * name, or</li>
 * <li>{@link #getProperty2Name property2} is not a syntactically valid JCR
 * name, or</li>
 * <li>the value of {@link #getProperty1Name property1} is not the same
 * property type as the name of {@link #getProperty2Name property2}, or</li>
 * <li>{@link #getProperty1Name property1} is a multi-valued property, or</li>
 * <li>{@link #getProperty2Name property2} is a multi-valued property, or</li>
 * <li>{@link #getProperty1Name property1} is a <code>BINARY</code> property, or</li>
 * <li>{@link #getProperty2Name property2} is a <code>BINARY</code> property.</li>
 * </ul>
 *
 * @since JCR 2.0
 */
public interface EquiJoinCondition extends JoinCondition {

    /**
     * Gets the name of the first selector.
     *
     * @return the selector name; non-null
     */
    String getSelector1Name();

    /**
     * Gets the property name in the first selector.
     *
     * @return the property name; non-null
     */
    String getProperty1Name();

    /**
     * Gets the name of the second selector.
     *
     * @return the selector name; non-null
     */
    String getSelector2Name();

    /**
     * Gets the property name in the second selector.
     *
     * @return the property name; non-null
     */
    String getProperty2Name();

}
