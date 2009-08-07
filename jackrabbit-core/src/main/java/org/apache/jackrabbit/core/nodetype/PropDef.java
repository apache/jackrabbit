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

import org.apache.jackrabbit.core.value.InternalValue;

/**
 * <code>PropDef</code> is the internal representation of
 * a property definition. It refers to <code>Name</code>s only
 * and is thus isolated from session-specific namespace mappings.
 *
 * @see javax.jcr.nodetype.PropertyDefinition
 */
public interface PropDef extends ItemDef {

    PropDef[] EMPTY_ARRAY = new PropDef[0];

    /**
     * Returns an identifier for this property definition.
     *
     * @return an identifier for this property definition.
     */
    PropDefId getId();

    /**
     * Returns the required type.
     *
     * @return the required type.
     */
    int getRequiredType();

    /**
     * Returns the array of value constraints.
     *
     * @return the array of value constraints.
     */
    ValueConstraint[] getValueConstraints();

    /**
     * Returns the array of default values.
     *
     * @return the array of default values.
     */
    InternalValue[] getDefaultValues();

    /**
     * Reports whether this property can have multiple values.
     *
     * @return the 'multiple' flag.
     */
    boolean isMultiple();
}
