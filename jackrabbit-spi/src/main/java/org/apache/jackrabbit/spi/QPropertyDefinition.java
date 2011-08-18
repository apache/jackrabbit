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
package org.apache.jackrabbit.spi;

import javax.jcr.nodetype.PropertyDefinition;

/**
 * <code>QPropertyDefinition</code> is the SPI representation of
 * a {@link PropertyDefinition property definition}. It refers to <code>Name</code>s,
 * SPI default values and value constraints only and is thus isolated
 * from session-specific namespace mappings.
 *
 * @see javax.jcr.nodetype.PropertyDefinition
 */
public interface QPropertyDefinition extends QItemDefinition {

    /**
     * Empty array of <code>QPropertyDefinition</code>.
     */
    public static final QPropertyDefinition[] EMPTY_ARRAY = new QPropertyDefinition[0];

    /**
     * Returns the required type.
     *
     * @return the required type.
     */
    public int getRequiredType();

    /**
     * Returns the array of value constraints.
     *
     * @return the array of value constraints.
     */
    public QValueConstraint[] getValueConstraints();

    /**
     * Returns the array of default values or <code>null</code> if no default
     * values are defined.
     *
     * @return the array of default values or <code>null</code>
     */
    public QValue[] getDefaultValues();

    /**
     * Reports whether this property can have multiple values.
     *
     * @return the 'multiple' flag.
     */
    public boolean isMultiple();

    /**
     * Returns the available query operators.
     *
     * @return the available query operators.
     * @since JCR 2.0
     */
    public String[] getAvailableQueryOperators();

    /**
     * Reports whether this property definition is full text searchable.
     *
     * @return <code>true</code> if this property definition is full text searchable.
     * @since JCR 2.0
     */
    public boolean isFullTextSearchable();

    /**
     * Reports whether this property definition is query-orderable.
     *
     * @return <code>true</code> if this property definition is query-orderable.
     * @since JCR 2.0
     */
    public boolean isQueryOrderable();
}
