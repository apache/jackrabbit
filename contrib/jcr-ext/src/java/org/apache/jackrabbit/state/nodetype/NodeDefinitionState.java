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

import java.util.HashSet;
import java.util.Set;

import org.apache.jackrabbit.name.Name;

/**
 * Node definition state. Instances of this class are used to hold
 * and manage the internal state of node definitions.
 */
public class NodeDefinitionState extends ItemDefinitionState {

    /** Name of the default primary type of the defined node. */
    private Name defaultPrimaryTypeName;

    /** Names of the required primary types of the defined node. */
    private Set requiredPrimaryTypeNames;

    /** The AllowsSameNameSiblings node definition property. */
    private boolean allowsSameNameSiblings;

    /** Creates an empty node definition state instance. */
    public NodeDefinitionState() {
        super();
        defaultPrimaryTypeName = null;
        requiredPrimaryTypeNames = new HashSet();
        allowsSameNameSiblings = false;
    }

    /**
     * Returns the name of the default primary type of the defined node.
     *
     * @return default primary type name
     */
    public Name getDefaultPrimaryTypeName() {
        return defaultPrimaryTypeName;
    }

    /**
     * Sets the name of the default primary type of the defined node.
     *
     * @param defaultPrimaryType new default primary type name
     */
    public void setDefaultPrimaryTypeName(Name defaultPrimaryType) {
        this.defaultPrimaryTypeName = defaultPrimaryType;
    }

    /**
     * Returns the names of the required primary types of the defined node.
     *
     * @return type names
     */
    public Name[] getRequiredPrimaryTypeNames() {
        return (Name[]) requiredPrimaryTypeNames.toArray(
                new Name[requiredPrimaryTypeNames.size()]);
    }

    /**
     * Adds a type name to the list of required primary types.
     *
     * @param requiredPrimaryTypeName type name
     */
    public void addRequiredPrimaryTypeNames(Name requiredPrimaryTypeName) {
        requiredPrimaryTypeNames.add(requiredPrimaryTypeName);
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

}
