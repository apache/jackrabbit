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
package org.apache.jackrabbit.state.nodetype;

import java.util.List;
import java.util.Vector;

import javax.jcr.PropertyType;

/**
 * Property definition state. Instances of this class are used to hold
 * and manage the internal state of property definitions.
 */
public class PropertyDefinitionState extends ItemDefinitionState {

    /** Required type of the defined property. */
    private int requiredType;

    /** List of value constraint strings. */
    private List valueConstraints;

    /** The Multiple property definition property. */
    private boolean multiple;

    /** Creates an empty property definition state instance. */
    public PropertyDefinitionState() {
        requiredType = PropertyType.UNDEFINED;
        valueConstraints = new Vector();
        multiple = false;
    }

    /**
     * Returns the required type of the defined property.
     *
     * @return required property type
     */
    public int getRequiredType() {
        return requiredType;
    }

    /**
     * Sets the required type of the defined property.
     *
     * @param requiredType new required property type
     */
    public void setRequiredType(int requiredType) {
        this.requiredType = requiredType;
    }

    /**
     * Returns the property value constraint strings. The returned
     * array can be modified freely as it is freshly instantiated and
     * not a part of the property definition state.
     *
     * @return value constraints
     */
    public String[] getValueConstraints() {
        return (String[])
            valueConstraints.toArray(new String[valueConstraints.size()]);
    }

    /**
     * Adds a constraint string to the list of property value constraints.
     *
     * @param constraint constraint string
     */
    public void addValueConstraints(String constraint) {
        valueConstraints.add(constraint);
    }

    /**
     * Returns the value of the Multiple property definition property.
     *
     * @return Multiple property value
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * Sets the value of the Multiple property definition property.
     *
     * @param multiple new Multiple property value
     */
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

}
