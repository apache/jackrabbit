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
package org.apache.jackrabbit.spi.commons.nodetype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.commons.query.qom.Operator;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.QPropertyDefinitionImpl;

/**
 * A builder for {@link QPropertyDefinition}.
 */
public class QPropertyDefinitionBuilder extends QItemDefinitionBuilder {

    private int requiredType = PropertyType.UNDEFINED;
    private List<QValueConstraint> valueConstraints = new ArrayList<QValueConstraint>();
    private List<QValue> defaultValues;
    private boolean isMultiple = false;
    private boolean fullTextSearchable = true;
    private boolean queryOrderable = true;
    private String[] queryOperators = Operator.getAllQueryOperators();

    /**
     * @param type the required type of the property definition being built.
     * @see PropertyDefinition#getRequiredType()
     */
    public void setRequiredType(int type) {
        requiredType = type;
    }

    /**
     * @return the required type of the property definition being built.
     * @see PropertyDefinition#getRequiredType()
     */
    public int getRequiredType() {
        return requiredType;
    }

    /**
     * Adds a value constraint of the property definition being built.
     *
     * @param constraint the constraint.
     */
    public void addValueConstraint(QValueConstraint constraint) {
        valueConstraints.add(constraint);
    }

    /**
     * @param constraints array of value constraints of the property definition
     *                    being built.
     * @see PropertyDefinition#getValueConstraints()
     */
    public void setValueConstraints(QValueConstraint[] constraints) {
        valueConstraints.clear();
        valueConstraints.addAll(Arrays.asList(constraints));
    }

    /**
     * @return array of value constraints of the property definition being
     *         built.
     * @see PropertyDefinition#getValueConstraints()
     */
    public QValueConstraint[] getValueConstraints() {
        return valueConstraints.toArray(new QValueConstraint[valueConstraints.size()]);
    }

    /**
     * Adds a default value of the property definition being built.
     *
     * @param value a default value.
     */
    public void addDefaultValue(QValue value) {
        if (defaultValues == null) {
            defaultValues = new ArrayList<QValue>();
        }
        defaultValues.add(value);
    }

    /**
     * @param values array of default values of the property definition being
     *               built.
     * @see PropertyDefinition#getDefaultValues()
     */
    public void setDefaultValues(QValue[] values) {
        if (values == null) {
            defaultValues = null;
        } else {
            // replace
            defaultValues = new ArrayList<QValue>(Arrays.asList(values));
        }
    }

    /**
     * @return array of default values of the property definition being built or
     *         <code>null</code> if no default values are defined.
     * @see PropertyDefinition#getDefaultValues()
     */
    public QValue[] getDefaultValues() {
        if (defaultValues == null) {
            return null;
        } else {
            return defaultValues.toArray(new QValue[defaultValues.size()]);
        }
    }

    /**
     * @param isMultiple true if building a 'multiple' property definition.
     * @see PropertyDefinition#isMultiple()
     */
    public void setMultiple(boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

    /**
     * @return true if building a 'multiple' property definition.
     * @see PropertyDefinition#isMultiple()
     */
    public boolean getMultiple() {
        return isMultiple;
    }

    /**
     * @return <code>true</code> if the property is fulltext searchable
     * @see PropertyDefinition#isFullTextSearchable()
     */
    public boolean getFullTextSearchable() {
        return fullTextSearchable;
    }

    /**
     * @param fullTextSearchable <code>true</code> if building a 'fulltext
     *                           searchable' property definition
     * @see PropertyDefinition#isFullTextSearchable()
     */
    public void setFullTextSearchable(boolean fullTextSearchable) {
        this.fullTextSearchable = fullTextSearchable;
    }

    /**
     * @return <code>true</code> if the property is orderable in a query
     * @see PropertyDefinition#isQueryOrderable()
     */
    public boolean getQueryOrderable() {
        return queryOrderable;
    }

    /**
     * @param queryOrderable <code>true</code> if the property is orderable in a
     *                       query
     * @see PropertyDefinition#isQueryOrderable()
     */
    public void setQueryOrderable(boolean queryOrderable) {
        this.queryOrderable = queryOrderable;
    }

    /**
     * @return the query operators of the property
     * @see PropertyDefinition#getAvailableQueryOperators()
     */
    public String[] getAvailableQueryOperators() {
        return queryOperators;
    }

    /**
     * @param queryOperators the query operators of the property
     * @see PropertyDefinition#getAvailableQueryOperators()
     */
    public void setAvailableQueryOperators(String[] queryOperators) {
        this.queryOperators = queryOperators;
    }

    /**
     * Creates a new {@link QPropertyDefinition} instance based on the state of
     * this builder.
     *
     * @return a new {@link QPropertyDefinition} instance.
     * @throws IllegalStateException if the instance has not the necessary
     *                               information to build the QPropertyDefinition
     *                               instance.
     */
    public QPropertyDefinition build() throws IllegalStateException {
        return new QPropertyDefinitionImpl(getName(), getDeclaringNodeType(), getAutoCreated(), getMandatory(), getOnParentVersion(), getProtected(), getDefaultValues(), getMultiple(), getRequiredType(), getValueConstraints(), getAvailableQueryOperators(), getFullTextSearchable(), getQueryOrderable());
    }
}
