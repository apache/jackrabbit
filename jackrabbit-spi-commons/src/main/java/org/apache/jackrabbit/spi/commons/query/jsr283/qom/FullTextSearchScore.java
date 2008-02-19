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
 * Evaluates to a <code>DOUBLE</code> value equal to the full-text search score
 * of a node.
 * <p/>
 * Full-text search score ranks a selector's nodes by their relevance to the
 * <code>fullTextSearchExpression</code> specified in a {@link FullTextSearch}.
 * The values to which <code>FullTextSearchScore</code> evaluates and the
 * interpretation of those values are implementation specific.
 * <code>FullTextSearchScore</code> may evaluate to a constant value in a
 * repository that does not support full-text search scoring or has no
 * full-text indexed properties.
 * <p/>
 * The query is invalid if {@link #getSelectorName selector} is not the name
 * of a selector in the query.
 *
 * @since JCR 2.0
 */
public interface FullTextSearchScore extends DynamicOperand {

    /**
     * Gets the name of the selector against which to evaluate this operand.
     *
     * @return the selector name; non-null
     */
    String getSelectorName();

}
