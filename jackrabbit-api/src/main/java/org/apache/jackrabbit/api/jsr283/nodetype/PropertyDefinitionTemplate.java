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
package org.apache.jackrabbit.api.jsr283.nodetype;

import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * The <code>PropertyDefinitionTemplate</code> interface extends
 * <code>PropertyDefinition</code> with the addition of write methods, enabling the
 * characteristics of a child property definition to be set, after which the
 * <code>PropertyDefinitionTemplate</code> is added to a <code>NodeTypeTemplate</code>.
 * <p/>
 * See the corresponding <code>get</code> methods for each attribute in
 * <code>PropertyDefinition</code> for the default values assumed when a new
 * empty <code>PropertyDefinitionTemplate</code> is created (as opposed to one
 * extracted from an existing <code>NodeType</code>).
 *
 * @since JCR 2.0
 */
public interface PropertyDefinitionTemplate extends PropertyDefinition {

    /**
     * Sets the name of the property.
     *
     * @param name a <code>String</code>.
     */
    void setName(String name);

    /**
     * Sets the auto-create status of the property.
     *
     * @param autoCreated a <code>boolean</code>.
     */
    void setAutoCreated(boolean autoCreated);

    /**
     * Sets the mandatory status of the property.
     *
     * @param mandatory a <code>boolean</code>.
     */
    void setMandatory(boolean mandatory);

    /**
     * Sets the on-parent-version status of the property.
     *
     * @param opv an <code>int</code> constant member of <code>OnParentVersionAction</code>.
     */
    void setOnParentVersion(int opv);

    /**
     * Sets the protected status of the property.
     *
     * @param protectedStatus a <code>boolean</code>.
     */
    void setProtected(boolean protectedStatus);

    /**
     * Sets the required type of the property.
     *
     * @param type an <code>int</code> constant member of <code>PropertyType</code>.
     */
    void setRequiredType(int type);

    /**
     * Sets the value constraints of the property.
     *
     * @param constraints a <code>String</code> array.
     */
    void setValueConstraints(String[] constraints);

    /**
     * Sets the default value (or values, in the case of a multi-value property) of the property.
     *
     * @param defaultValues a <code>Value</code> array.
     */
    void setDefaultValues(Value[] defaultValues);

    /**
     * Sets the multi-value status of the property.
     *
     * @param multiple a <code>boolean</code>.
     */
    void setMultiple(boolean multiple);

}
