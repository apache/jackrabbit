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

import java.util.Arrays;

import org.apache.jackrabbit.name.QName;

/**
 * Node definition state. Instances of this class are used to hold
 * and manage the internal state of node definitions.
 */
public class NodeDefinitionState extends ItemDefinitionState {

    /** Name of the default primary type of the defined node. */
    private QName defaultPrimaryTypeName = null;

    /** Names of the required primary types of the defined node. */
    private QName[] requiredPrimaryTypeNames = new QName[0];

    /** The AllowsSameNameSiblings node definition property. */
    private boolean allowsSameNameSiblings = false;

    /**
     * Returns the name of the default primary type of the defined node.
     *
     * @return default primary type name
     */
    public QName getDefaultPrimaryTypeName() {
        return defaultPrimaryTypeName;
    }

    /**
     * Sets the name of the default primary type of the defined node.
     *
     * @param defaultPrimaryType new default primary type name
     */
    public void setDefaultPrimaryTypeName(QName defaultPrimaryType) {
        this.defaultPrimaryTypeName = defaultPrimaryType;
    }

    /**
     * Returns the names of the required primary types of the defined node.
     *
     * @return type names
     */
    public QName[] getRequiredPrimaryTypeNames() {
        return requiredPrimaryTypeNames;
    }

    /**
     * Sets the list of required primary types.
     *
     * @param requiredPrimaryTypeNames type names
     */
    public void setRequiredPrimaryTypeName(QName[] requiredPrimaryTypeNames) {
        this.requiredPrimaryTypeNames = requiredPrimaryTypeNames;
        Arrays.sort(this.requiredPrimaryTypeNames);
    }

    /**
     * Returns the value of the AllowsSameNameSiblings node definition property.
     *
     * @return AllowsSameNameSiblings property value
     */
    public boolean allowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }

    /**
     * Sets the value of the AllowsSameNameSiblings node definition property.
     *
     * @param allowsSameNameSiblings new AllowsSameNameSiblings property value
     */
    public void setAllowsSameNameSiblings(boolean allowsSameNameSiblings) {
        this.allowsSameNameSiblings = allowsSameNameSiblings;
    }

    public boolean equals(Object object) {
        return (this == object)
            || (object != null && new StateComparator().compare(this, object) == 0);
    }

    public int hashCode() {
        int code = super.hashCode();
        code = code * 17 + (allowsSameNameSiblings ? 1 : 0);
        code = code * 17 + ((defaultPrimaryTypeName != null) ? defaultPrimaryTypeName.hashCode() : 0);
        for (int i = 0; i < requiredPrimaryTypeNames.length; i++) {
            code = code * 17 + requiredPrimaryTypeNames[i].hashCode();
        }
        return code;
    }

}
