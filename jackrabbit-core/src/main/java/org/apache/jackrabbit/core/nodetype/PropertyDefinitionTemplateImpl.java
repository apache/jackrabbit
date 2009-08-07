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
package org.apache.jackrabbit.core.nodetype;

import org.apache.jackrabbit.api.jsr283.nodetype.PropertyDefinitionTemplate;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

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

    /**
     * Package private constructor
     */
    PropertyDefinitionTemplateImpl() {
        type = PropertyType.STRING;
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
}
