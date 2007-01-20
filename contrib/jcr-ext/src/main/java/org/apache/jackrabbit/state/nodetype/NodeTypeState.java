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

import org.apache.jackrabbit.name.QName;

/**
 * Node type state. Instances of this class are used to hold
 * and manage the internal state of node types.
 */
public class NodeTypeState {

    /** Name of the node type. */
    private QName name = null;

    /** The Mixin node type property. */
    private boolean mixin = false;

    /** The HasOrderableChildNodes node type property. */
    private boolean hasOrderableChildNodes = false;

    /** Name of the primary item of the node type. */
    private QName primaryItemName = null;

    /** Names of the declared supertypes. */
    private QName[] supertypeNames = new QName[0];

    /** Child node definition states. */
    private NodeDefinitionState[] childNodeDefinitionStates =
        new NodeDefinitionState[0];

    /** Property definition states. */
    private PropertyDefinitionState[] propertyDefinitionStates =
        new PropertyDefinitionState[0];

    /**
     * Returns the node type name.
     *
     * @return qualified name
     */
    public QName getName() {
        return name;
    }

    /**
     * Sets the node type name.
     *
     * @param name new qualified name
     */
    public void setName(QName name) {
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
    public boolean hasOrderableChildNodes() {
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
    public QName getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * Sets the name of the primary item of the node type.
     *
     * @param primaryItemName new primary item name
     */
    public void setPrimaryItemName(QName primaryItemName) {
        this.primaryItemName = primaryItemName;
    }

    /**
     * Returns the names of the declared supertypes.
     *
     * @return supertype names
     */
    public QName[] getSupertypeNames() {
        return supertypeNames;
    }

    /**
     * Sets the list of declared supertypes.
     *
     * @param supertypeNames supertype names
     */
    public void setSupertypeNames(QName[] supertypeNames) {
        this.supertypeNames = supertypeNames;
    }

    /**
     * Returns the child node definition states of the node type.
     *
     * @return child node definition states
     */
    public NodeDefinitionState[] getChildNodeDefinitionStates() {
        return childNodeDefinitionStates;
    }

    /**
     * Sets the list of child node definition states of the node type.
     *
     * @param childNodeDefinitionStates child node definition states
     */
    public void setChildNodeDefinitionStates(
            NodeDefinitionState[] childNodeDefinitionStates) {
        this.childNodeDefinitionStates = childNodeDefinitionStates;
    }

    /**
     * Returns the property definition states of the node type.
     *
     * @return property definition states
     */
    public PropertyDefinitionState[] getPropertyDefinitionStates() {
        return propertyDefinitionStates;
    }

    /**
     * Sets the list of property definition states of the node type.
     *
     * @param propertyDefinitionStates property definition states
     */
    public void setPropertyDefinitionStates(
            PropertyDefinitionState[] propertyDefinitionStates) {
        this.propertyDefinitionStates = propertyDefinitionStates;
    }

    public boolean equals(Object object) {
        return (this == object)
            || (object != null && new StateComparator().compare(this, object) == 0);
    }

    public int hashCode() {
        int code = 37;
        code = code * 17 + ((name != null) ? name.hashCode() : 0);
        code = code * 17 + (mixin ? 1 : 0);
        code = code * 17 + (hasOrderableChildNodes ? 1 : 0);
        return code;
    }

}
