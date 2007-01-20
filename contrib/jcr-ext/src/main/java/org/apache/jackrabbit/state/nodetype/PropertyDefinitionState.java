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
package org.apache.jackrabbit.state.nodetype;

import javax.jcr.PropertyType;

/**
 * Property definition state. Instances of this class are used to hold
 * and manage the internal state of property definitions.
 */
public class PropertyDefinitionState extends ItemDefinitionState {

    /** Required type of the defined property. */
    private int requiredType = PropertyType.UNDEFINED;

    /** The Multiple property definition property. */
    private boolean multiple = false;

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

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object instanceof PropertyDefinitionState) {
            PropertyDefinitionState that = (PropertyDefinitionState) object;
            return super.equals(that)
                && this.multiple == that.multiple
                && this.requiredType == that.requiredType;
        } else {
            return false;
        }
    }

    public int hashCode() {
        int code = super.hashCode();
        code = code * 17 + (multiple ? 1 : 0);
        code = code * 17 + requiredType;
        return code;
    }

}
