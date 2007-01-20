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
package org.apache.jackrabbit.base.nodetype;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Value;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Node type base class.
 */
public class BaseNodeType implements NodeType {

    /** Not implemented. {@inheritDoc} */
    public String getName() {
        throw new UnsupportedOperationException();
    }

    /** Always returns <code>false</code>. {@inheritDoc} */
    public boolean isMixin() {
        return false;
    }

    /** Always returns <code>false</code>. {@inheritDoc} */
    public boolean hasOrderableChildNodes() {
        return false;
    }

    /** Not implemented. {@inheritDoc} */
    public String getPrimaryItemName() {
        throw new UnsupportedOperationException();
    }

    /** Always returns an empty supertype array. {@inheritDoc} */
    public NodeType[] getDeclaredSupertypes() {
        return new NodeType[0];
    }

    /**
     * Implemented by calling <code>getDeclaredSupertypes()</code> and
     * recursively collecting all supertypes. The collected supertype
     * set is returned as a node type array.
     * {@inheritDoc}
     */
    public NodeType[] getSupertypes() {
        Set supertypes = new HashSet();

        NodeType[] declared = getDeclaredSupertypes();
        for (int i = 0; i < declared.length; i++) {
            supertypes.addAll(Arrays.asList(declared[i].getSupertypes()));
        }
        supertypes.addAll(Arrays.asList(declared));

        return (NodeType[]) supertypes.toArray(new NodeType[supertypes.size()]);
    }

    /**
     * Implemented by calling <code>getName()</code> and comparing the
     * result to the given node type name. If the match fails, recursively
     * checks all declared supertypes.
     * {@inheritDoc}
     */
    public boolean isNodeType(String nodeTypeName) {
        if (nodeTypeName.equals(getName())) {
            return true;
        } else {
            NodeType[] types = getDeclaredSupertypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i].isNodeType(nodeTypeName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Always returns an empty property definition array. {@inheritDoc} */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        return new PropertyDefinition[0];
    }

    /**
     * Implemented by calling <code>getDeclaredPropertyDefinitions()</code>
     * this node type and all supertypes returned by
     * <code>getSupertypes()</code>. The collected property definition set
     * is returned as a property definition array.
     * {@inheritDoc}
     */
    public PropertyDefinition[] getPropertyDefinitions() {
        Set definitions = new HashSet();

        NodeType[] supertypes = getSupertypes();
        for (int i = 0; i < supertypes.length; i++) {
            definitions.addAll(
                    Arrays.asList(supertypes[i].getPropertyDefinitions()));
        }
        definitions.addAll(Arrays.asList(getDeclaredPropertyDefinitions()));

        return (PropertyDefinition[])
            definitions.toArray(new PropertyDefinition[definitions.size()]);
    }

    /** Always returns an empty node definition array. {@inheritDoc} */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        return new NodeDefinition[0];
    }

    /**
     * Implemented by calling <code>getDeclaredChildNodeDefinitions()</code>
     * on this node type and all supertypes returned by
     * <code>getSupertypes()</code>. The collected node definition set
     * is returned as a node definition array.
     * {@inheritDoc}
     */
    public NodeDefinition[] getChildNodeDefinitions() {
        Set defs = new HashSet();

        NodeType[] types = getSupertypes();
        for (int i = 0; i < types.length; i++) {
            defs.addAll(Arrays.asList(types[i].getChildNodeDefinitions()));
        }
        defs.addAll(Arrays.asList(getDeclaredChildNodeDefinitions()));

        return (NodeDefinition[]) defs.toArray(new NodeDefinition[0]);
    }


    /**
     * Returns the definition of the named property.
     * <p>
     * This internal utility method is used by the predicate methods
     * in this class.
     *
     * @param propertyName property name
     * @return property definition, or <code>null</code> if not found
     */
    private PropertyDefinition getPropertyDefinition(String propertyName) {
        PropertyDefinition[] definitions = getPropertyDefinitions();
        for (int i = 0; i < definitions.length; i++) {
            if (propertyName.equals(definitions[i].getName())) {
                return definitions[i];
            }
        }
        return null;
    }

    /**
     * Implemented by finding the definition of the named property (or the
     * wildcard property definition if the named property definition is not
     * found) and checking whether the defined property is single-valued.
     * More detailed value constraints are not implemented, but this method
     * will simply return <code>true</code> instead of throwing an
     * {@link UnsupportedOperationException UnsupportedOperationException}
     * for all value constraint comparisons.
     * {@inheritDoc}
     */
    public boolean canSetProperty(String propertyName, Value value) {
        PropertyDefinition definition = getPropertyDefinition(propertyName);
        if (definition == null) {
            definition = getPropertyDefinition("*");
        }
        if (definition == null || definition.isMultiple()) {
            return false;
        } else {
            return true; // TODO check constraints!
        }
    }

    /**
     * Implemented by finding the definition of the named property (or the
     * wildcard property definition if the named property definition is not
     * found) and checking whether the defined property is multi-valued.
     * More detailed value constraints are not implemented, but this method
     * will simply return <code>true</code> instead of throwing an
     * {@link UnsupportedOperationException UnsupportedOperationException}
     * for all value constraint comparisons.
     * {@inheritDoc}
     */
    public boolean canSetProperty(String propertyName, Value[] values) {
        PropertyDefinition def = getPropertyDefinition(propertyName);
        if (def == null) {
            def = getPropertyDefinition("*");
        }
        if (def == null || !def.isMultiple()) {
            return false;
        } else {
            return true; // TODO check constraints!
        }
    }

    /**
     * Returns the definition of the named child node.
     * <p>
     * This internal utility method is used by the predicate methods
     * in this class.
     *
     * @param childNodeName child node name
     * @return node definition, or <code>null</code> if not found
     */
    private NodeDefinition getChildNodeDefinition(String childNodeName) {
        NodeDefinition[] definitions = getChildNodeDefinitions();
        for (int i = 0; i < definitions.length; i++) {
            if (childNodeName.equals(definitions[i].getName())) {
                return definitions[i];
            }
        }
        return null;
    }

    /**
     * Implemented by finding the definition of the named child node (or the
     * wildcard child node definition if the named child node definition is
     * not found). Returns <code>true</code> if a node definition is found,
     * <code>false</code> otherwise.
     * {@inheritDoc}
     */
    public boolean canAddChildNode(String childNodeName) {
        NodeDefinition definition = getChildNodeDefinition(childNodeName);
        if (definition == null) {
            definition = getChildNodeDefinition("*");
        }
        return definition != null;
    }

    /**
     * Not implemented. Implementing this method requires access to the
     * node type manager in order to resolve the given node type name.
     * {@inheritDoc}
     */
    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Implemented by finding the definition of the named item (property or
     * child node) and checking that the defined item is not mandatory.
     * {@inheritDoc}
     */
    public boolean canRemoveItem(String itemName) {
        ItemDefinition definition = getPropertyDefinition(itemName);
        if (definition == null) {
            definition = getChildNodeDefinition(itemName);
        }
        if (definition == null) {
            return true;
        } else {
            return definition.isMandatory();
        }
    }

}
