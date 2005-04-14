/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.core.InternalValue;

/**
 * This interface defines the property def.
 */
public interface PropDef extends ItemDef {

    /**
     * Returns the property definition id.
     * @return the PropDefId
     */
    public PropDefId getId();

    /**
     * Returns the required type
     * @return the required type
     */
    public int getRequiredType();

    /**
     * Returns the array of value constraints
     * @return the array of value constraints
     */
    public ValueConstraint[] getValueConstraints();

    /**
     * Returns the array of default values
     * @return the array of default values
     */
    public InternalValue[] getDefaultValues();

    /**
     * Returns the 'multiple' flag.
     * @return the 'multiple' flag.
     */
    public boolean isMultiple();

}
