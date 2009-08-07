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

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;

/**
 * <code>AbstractItemDefinitionTemplate</code> serves as base class for
 * <code>NodeDefinitionTemplateImpl</code> and
 * <code>PropertyDefinitionTemplateImpl</code>.
 */
abstract class AbstractItemDefinitionTemplate implements ItemDefinition {

    private String name;
    private boolean autoCreated;
    private boolean mandatory;
    private int opv;
    private boolean protectedStatus;

    /**
     * Package private constructor
     */
    AbstractItemDefinitionTemplate() {
    }

    /**
     * Package private constructor
     *
     * @param def
     */
    AbstractItemDefinitionTemplate(ItemDefinition def) {
        name = def.getName();
        autoCreated = def.isAutoCreated();
        mandatory = def.isMandatory();
        opv = def.getOnParentVersion();
        protectedStatus = def.isProtected();
    }

    //-----------------------------------------------< ItemDefinition setters >
    /**
     * Sets the name of the child item.
     *
     * @param name a <code>String</code>.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the auto-create status of the child item.
     *
     * @param autoCreated a <code>boolean</code>.
     */
    public void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    /**
     * Sets the mandatory status of the child item.
     *
     * @param mandatory a <code>boolean</code>.
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Sets the on-parent-version status of the child item.
     *
     * @param opv an <code>int</code> constant member of <code>OnParentVersionAction</code>.
     */
    public void setOnParentVersion(int opv) {
        this.opv = opv;
    }

    /**
     * Sets the protected status of the child item.
     *
     * @param protectedStatus a <code>boolean</code>.
     */
    public void setProtected(boolean protectedStatus) {
        this.protectedStatus = protectedStatus;
    }

    //-------------------------------------------------------< ItemDefinition >
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public NodeType getDeclaringNodeType() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAutoCreated() {
        return autoCreated;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * {@inheritDoc}
     */
    public int getOnParentVersion() {
        return opv;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isProtected() {
        return protectedStatus;
    }

}
