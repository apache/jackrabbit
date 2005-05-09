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
 * Node type state. Instances of this class are used to hold
 * and manage the internal state of node types.
 */
public class NodeTypeState {

    /** Name of the node type. */
    private Name name;

    /** The Mixin node type property. */
    private boolean mixin;

    /** The HasOrderableChildNodes node type property. */
    private boolean hasOrderableChildNodes;

    /** Name of the primary item of the node type. */
    private Name primaryItemName;

    /** Names of the declared supertypes. */
    private Set supertypeNames;

    /** Child node definition states. */
    private Set childNodeDefinitionStates;

    /** Property definition states. */
    private Set propertyDefinitionStates;

    /** Creates an empty node type state instance. */
    public NodeTypeState() {
        name = null;
        mixin = false;
        hasOrderableChildNodes = false;
        primaryItemName = null;
        supertypeNames = new HashSet();
        childNodeDefinitionStates = new HashSet();
        propertyDefinitionStates = new HashSet();
    }

    /**
     * Returns the node type name.
     *
     * @return qualified name
     */
    public Name getName() {
        return name;
    }

    /**
     * Sets the node type name.
     *
     * @param name new qualified name
     */
    public void setName(Name name) {
        this.name = name;
    }

    /**
     * Returns the value of the Mixin node type property.
     *
     * @return Mixin property value
     */
    public boolean isMixin() {
        return mixin;
    }

    /**
     * Sets the value of the Mixin node type property.
     *
     * @param mixin new Mixin property value
     */
    public void setMixin(boolean mixin) {
        this.mixin = mixin;
    }

    /**
     * Returns the value of the HasOrderableChildNodes node type property.
     *
     * @return HasOrderableChildNodes property value
     */
    public boolean isHasOrderableChildNodes() {
        return hasOrderableChildNodes;
    }

    /**
     * Sets the value of the HasOrderableChildNodes node type property.
     *
     * @param hasOrderableChildNodes new HasOrderableChildNodes property value
     */
    public void setHasOrderableChildNodes(boolean hasOrderableChildNodes) {
        this.hasOrderableChildNodes = hasOrderableChildNodes;
    }

    /**
     * Returns the name of the primary item of the node type.
     *
     * @return primary item name
     */
    public Name getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * Sets the name of the primary item of the node type.
     *
     * @param primaryItemName new primary item name
     */
    public void setPrimaryItemName(Name primaryItemName) {
        this.primaryItemName = primaryItemName;
    }

    /**
     * Returns the names of the declared supertypes.
     *
     * @return supertype names
     */
    public Name[] getSupertypeNames() {
        return (Name[])
            supertypeNames.toArray(new Name[supertypeNames.size()]);
    }

    /**
     * Adds a supertype name to the list of declared supertypes.
     *
     * @param name supertype name
     */
    public void addSupertypeName(Name name) {
        supertypeNames.add(name);
    }

    /**
     * Returns the child node definition states of the node type.
     *
     * @return child node definition states
     */
    public NodeDefinitionState[] getChildNodeDefinitionStates() {
        return (NodeDefinitionState[]) childNodeDefinitionStates.toArray(
                new NodeDefinitionState[childNodeDefinitionStates.size()]);
    }

    /**
     * Adds a node definition state to the list of child node definition
     * states of the node type.
     *
     * @param state child node definition state
     */
    public void addChildNodeDefinition(NodeDefinitionState state) {
        childNodeDefinitionStates.add(state);
    }

    /**
     * Returns the property definition states of the node type.
     *
     * @return property definition states
     */
    public PropertyDefinitionState[] getPropertyDefinitionStates() {
        return (PropertyDefinitionState[]) propertyDefinitionStates.toArray(
                new PropertyDefinitionState[propertyDefinitionStates.size()]);
    }

    /**
     * Adds a property definition state to the list of property definition
     * states of the node type.
     *
     * @param state property definition state
     */
    public void addPropertyDefinitionState(PropertyDefinitionState state) {
        propertyDefinitionStates.add(state);
    }

}
