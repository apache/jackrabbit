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

import org.apache.jackrabbit.spi.commons.query.qom.Operator;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

/**
 * A <code>PropertyDefinitionTemplateImpl</code> ...
 */
class PropertyDefinitionTemplateImpl
        extends AbstractItemDefinitionTemplate
        implements PropertyDefinitionTemplate {

    private int type;
    private String[] constraints;
    private Value[] defaultValues;
    private boolean multiple;
    private boolean fullTextSearchable;
    private boolean queryOrderable;
    private String[] queryOperators;

    /**
     * Package private constructor
     */
    PropertyDefinitionTemplateImpl() {
        type = PropertyType.STRING;
        fullTextSearchable = true;
        queryOrderable = true;
        queryOperators = Operator.getAllQueryOperators();
    }

    /**
     * Package private constructor
     *
     * @param def
     */
    PropertyDefinitionTemplateImpl(PropertyDefinition def) {
        super(def);
        type = def.getRequiredType();
        constraints = def.getValueConstraints();
        defaultValues = def.getDefaultValues();
        multiple = def.isMultiple();
        fullTextSearchable = def.isFullTextSearchable();
        queryOrderable = def.isQueryOrderable();
        queryOperators = def.getAvailableQueryOperators();
    }

    //-------------------------------------------< PropertyDefinitionTemplate >
    /**
     * {@inheritDoc}
     */
    public void setRequiredType(int type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    public void setValueConstraints(String[] constraints) {
        this.constraints = constraints;
    }

    /**
     * {@inheritDoc}
     */
    public void setDefaultValues(Value[] defaultValues) {
        this.defaultValues = defaultValues;
    }

    /**
     * {@inheritDoc}
     */
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    /**
     * {@inheritDoc}
     */
    public void setAvailableQueryOperators(String[] operators) {
        queryOperators = operators;
    }

    /**
     * {@inheritDoc}
     */
    public void setFullTextSearchable(boolean searchable) {
        fullTextSearchable = searchable;
    }

    /**
     * {@inheritDoc}
     */
    public void setQueryOrderable(boolean orderable) {
        queryOrderable = orderable;
    }

    //---------------------------------------------------< PropertyDefinition >
    /**
     * {@inheritDoc}
     */
    public int getRequiredType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getValueConstraints() {
        return constraints;
    }

    /**
     * {@inheritDoc}
     */
    public Value[] getDefaultValues() {
        return defaultValues;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAvailableQueryOperators() {
        return queryOperators;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFullTextSearchable() {
        return fullTextSearchable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isQueryOrderable() {
        return queryOrderable;
    }

}
