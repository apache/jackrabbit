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
package org.apache.jackrabbit.session.nodetype;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.session.SessionHelper;
import org.apache.jackrabbit.name.Name;
import org.apache.jackrabbit.state.nodetype.NodeDefinitionState;
import org.apache.jackrabbit.state.nodetype.NodeTypeState;
import org.apache.jackrabbit.state.nodetype.PropertyDefinitionState;

/**
 * Immutable and session-bound node type frontend. An instance
 * of this class presents the underlying node type state using
 * the JCR NodeType interface. This class also contains simple
 * implementations of the higher-order methods defined by the
 * NodeType interface.
 * <p>
 * By not exposing the setter methods of the underlying state instance,
 * this class intentionally makes it impossible for a JCR client to modify
 * node type information.
 */
public class SessionNodeType implements NodeType {

    /** The wildcard item definition name. */
    private static final String WILDCARD = "*";

    /** Helper for accessing the current session. */
    private final SessionHelper helper;

    /** The underlying node type state. */
    private final NodeTypeState state;

    /** Memorized set of declared supertypes. Initially <code>null</code>. */
    private NodeType[] declaredSupertypes;

    /** Memorized set of all supertypes. Initially <code>null</code>. */
    private NodeType[] supertypes;

    /** Memorized set of declared property defs. Initially <code>null</code>. */
    private PropertyDefinition[] declaredPropertyDefinitions;

    /** Memorized set of all property defs. Initially <code>null</code>. */
    private PropertyDefinition[] propertyDefinitions;

    /** Memorized set of declared child node defs. Initially <code>null</code>. */
    private NodeDefinition[] declaredChildNodeDefinitions;

    /** Memorized set of all child node defs. Initially <code>null</code>. */
    private NodeDefinition[] childNodeDefinitions;

    /**
     * Creates a node type frontend that is bound to the
     * given session and underlying node type state.
     *
     * @param helper helper for accessing the current session
     * @param state underlying node type state
     */
    public SessionNodeType(SessionHelper helper, NodeTypeState state) {
        this.helper = helper;
        this.state = state;
        this.declaredSupertypes = null;
        this.supertypes = null;
        this.declaredPropertyDefinitions = null;
        this.propertyDefinitions = null;
        this.declaredChildNodeDefinitions = null;
        this.childNodeDefinitions = null;
    }

    /**
     * Compares objects for equality. Returns <code>true</code> if the
     * given object is a SessionNodeType with the same (refrence equality)
     * underlying node type state.
     *
     * @param that the object to compare this object with
     * @return <code>true</code> if the objects are equal,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that instanceof SessionNodeType) {
            return state == ((SessionNodeType) that).state;
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code for this object. To satisfy the equality
     * constraints the returned hash code is the hash code of the
     * underlying node type state.
     *
     * @return hash code
     * @see Object#hashCode()
     */
    public int hashCode() {
        return state.hashCode();
    }

    /**
     * Returns the name of the node type. The returned name is retrieved
     * from the underlying node type state and converted into a prefixed
     * JCR name using the namespace mappings of the current session.
     *
     * @return node type name
     * @see NodeType#getName()
     */
    public String getName() {
        return helper.getName(state.getName());
    }

    /**
     * Returns the value of the Mixin node type property. The returned
     * value is retrieved from the underlying node type state.
     *
     * @return Mixin property value
     * @see NodeType#isMixin()
     */
    public boolean isMixin() {
        return state.isMixin();
    }

    /**
     * Returns the value of the HasOrderableChildNodes node type property.
     * The returned value is retrieved from the underlying node type state.
     *
     * @return HasOrderableChildNodes property value
     * @see NodeType#hasOrderableChildNodes()
     */
    public boolean hasOrderableChildNodes() {
        return state.isHasOrderableChildNodes();
    }

    /**
     * Returns the name of the primary item of this node type.
     * The returned name is retrieved from the underlying node type state
     * and converted into a prefixed JCR name using the namespace mappings
     * of the current session.
     *
     * @return primary item name, or <code>null</code> if not specified
     * @see NodeType#getPrimaryItemName()
     */
    public String getPrimaryItemName() {
        Name name = state.getPrimaryItemName();
        if (name != null) {
            return helper.getName(name);
        } else {
            return null;
        }
    }

    /**
     * Returns the declared supertypes of this node type. The returned
     * node types are retrieved from the node type manager of the current
     * session using the supertype names stored in the underlying state.
     * <p>
     * The returned array is freshly instantiated and not a part of the
     * underlying state, so it can be freely modified.
     * <p>
     * The set of declared supertypes is memorized to improve performance,
     * and will therefore not change even if the underlying state changes!
     *
     * @return declared supertypes
     * @see NodeType#getDeclaredSupertypes()
     */
    public NodeType[] getDeclaredSupertypes() {
        if (declaredSupertypes == null) {
            Set types = new HashSet();
            Name[] names = state.getSupertypeNames();
            for (int i = 0; i < names.length; i++) {
                types.add(helper.getNodeType(names[i]));
            }
            declaredSupertypes =
                (NodeType[]) types.toArray(new NodeType[types.size()]);
        }
        return (NodeType[]) declaredSupertypes.clone();
    }

    /**
     * Returns the declared child node definitions of this node type.
     * The returned child node definitions are SessionNodeDefs instantiated
     * using the node definition states returned by the underlying node type
     * state.
     * <p>
     * The returned array is freshly instantiated and not a part of the
     * underlying state, so it can be freely modified.
     * <p>
     * The set of declared child node defs is memorized to improve performance,
     * and will therefore not change even if the underlying state changes!
     *
     * @return declared child node definitions
     * @see SessionNodeDefinition
     * @see NodeType#getDeclaredChildNodeDefinitions()
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        if (declaredChildNodeDefinitions == null) {
            Set defs = new HashSet();
            NodeDefinitionState[] states = state.getChildNodeDefinitionStates();
            for (int i = 0; i < states.length; i++) {
                defs.add(new SessionNodeDefinition(helper, this, states[i]));
            }
            declaredChildNodeDefinitions = (NodeDefinition[])
                defs.toArray(new NodeDefinition[defs.size()]);
        }
        return (NodeDefinition[]) declaredChildNodeDefinitions.clone();
    }

    /**
     * Returns the declared property definitions of this node type.
     * The returned property definitions are SessionPropertyDefs instantiated
     * using the property definition states returned by the underlying
     * node type state.
     * <p>
     * The returned array is freshly instantiated and not a part of the
     * underlying state, so it can be freely modified.
     * <p>
     * The set of declared property defs is memorized to improve performance,
     * and will therefore not change even if the underlying state changes!
     *
     * @return declared child node definitions
     * @see SessionPropertyDefinition
     * @see NodeType#getDeclaredChildNodeDefs()
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        if (declaredPropertyDefinitions == null) {
            Set defs = new HashSet();
            PropertyDefinitionState[] states =
                state.getPropertyDefinitionStates();
            for (int i = 0; i < states.length; i++) {
                defs.add(new SessionPropertyDefinition(helper, this, states[i]));
            }
            declaredPropertyDefinitions = (PropertyDefinition[])
                defs.toArray(new PropertyDefinition[defs.size()]);
        }
        return (PropertyDefinition[]) declaredPropertyDefinitions.clone();
    }

    /**
     * Checks whether this node type is or inherits the named node type.
     * The check is implemented by first comparing the given name to the name
     * of this node type and then (if names did not match) recursively checking
     * all declared supertypes.
     *
     * @param name node type name
     * @return <code>true</code> if this node type is or inherits the given
     *         node type, <code>false</code> otherwise
     * @see NodeType#isNodeType(String)
     */
    public boolean isNodeType(String name) {
        if (name.equals(getName())) {
            return true;
        } else {
            NodeType[] types = getDeclaredSupertypes();
            for (int i = 0; i < types.length; i++) {
                if (types[i].isNodeType(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Returns all supertypes (declared and inherited) of this node type.
     * Implemented by recursively getting the supertypes of all the declared
     * supertypes of this node type, and combining all the returned types
     * into a single set.
     * <p>
     * The returned array is freshly instantiated and not a part of the
     * internal state, so it can be freely modified.
     * <p>
     * The set of all supertypes is memorized to improve performance,
     * and will therefore not change even if the underlying state changes!
     *
     * @return all supertypes
     * @see NodeType#getSupertypes()
     */
    public NodeType[] getSupertypes() {
        if (supertypes == null) {
            Set types = new HashSet();
            NodeType[] declaredSupertypes = getDeclaredSupertypes();
            for (int i = 0; i < declaredSupertypes.length; i++) {
                types.add(declaredSupertypes[i]);
                types.addAll(
                        Arrays.asList(declaredSupertypes[i].getSupertypes()));
            }
            supertypes = (NodeType[]) types.toArray(new NodeType[types.size()]);
        }
        return (NodeType[]) supertypes.clone();
    }

    /**
     * Returns all child node definitions (declared and inherited) of this
     * node type. Implemented by recursively getting the child node definitions
     * of all the declared supertypes of this node type, and combining all
     * the returned definitions into a single set.
     * <p>
     * The returned array is freshly instantiated and not a part of the
     * internal state, so it can be freely modified.
     * <p>
     * The set of all child node defs is memorized to improve performance,
     * and will therefore not change even if the underlying state changes!
     *
     * @return all child node definitions
     * @see NodeType#getChildNodeDefs()
     */
    public NodeDefinition[] getChildNodeDefinitions() {
        if (childNodeDefinitions == null) {
            Set defs = new HashSet();
            defs.addAll(Arrays.asList(getDeclaredChildNodeDefinitions()));
            NodeType[] supertypes = getDeclaredSupertypes();
            for (int i = 0; i < supertypes.length; i++) {
                defs.addAll(
                        Arrays.asList(supertypes[i].getChildNodeDefinitions()));
            }
            childNodeDefinitions = (NodeDefinition[])
                defs.toArray(new NodeDefinition[defs.size()]);
        }
        return (NodeDefinition[]) childNodeDefinitions.clone();
    }

    /**
     * Returns all property definitions (declared and inherited) of this
     * node type. Implemented by recursively getting the property definitions
     * of all the declared supertypes of this node type, and combining all
     * the returned definitions into a single set.
     * <p>
     * The returned array is freshly instantiated and not a part of the
     * internal state, so it can be freely modified.
     * <p>
     * The set of all property defs is memorized to improve performance,
     * and will therefore not change even if the underlying state changes!
     *
     * @return all property definitions
     * @see NodeType#getPropertyDefs()
     */
    public PropertyDefinition[] getPropertyDefinitions() {
        if (propertyDefinitions == null) {
            Set defs = new HashSet();
            defs.addAll(Arrays.asList(getDeclaredPropertyDefinitions()));
            NodeType[] supertypes = getDeclaredSupertypes();
            for (int i = 0; i < supertypes.length; i++) {
                defs.addAll(
                        Arrays.asList(supertypes[i].getPropertyDefinitions()));
            }
            propertyDefinitions = (PropertyDefinition[])
                defs.toArray(new PropertyDefinition[defs.size()]);
        }
        return (PropertyDefinition[]) propertyDefinitions.clone();
    }

    /**
     * Returns the named property definition of this node type.
     * The property definition is located by iterating over all the
     * property definitions and selecting the one that matches the
     * given name (or the wildcard name if no exact match is found).
     *
     * @param name property name
     * @return property definition, or <code>null</code> if not found
     */
    private PropertyDefinition getPropertyDefinition(String name) {
        PropertyDefinition[] defs = getPropertyDefinitions();
        for (int i = 0; i < defs.length; i++) {
            if (name.equals(defs[i].getName())) {
                return defs[i];
            }
        }
        for (int i = 0; i < defs.length; i++) {
            if (WILDCARD.equals(defs[i].getName())) {
                return defs[i];
            }
        }
        return null;
    }

    /**
     * Returns the named child node definition of this node type.
     * The node definition is located by iterating over all the
     * child node definitions and selecting the one that matches the
     * given name (or the wildcard name if no exact match is found).
     *
     * @param name child node name
     * @return child node definition, or <code>null</code> if not found
     */
    private NodeDefinition getChildNodeDefinition(String name) {
        NodeDefinition[] defs = getChildNodeDefinitions();
        for (int i = 0; i < defs.length; i++) {
            if (name.equals(defs[i].getName())) {
                return defs[i];
            }
        }
        for (int i = 0; i < defs.length; i++) {
            if (WILDCARD.equals(defs[i].getName())) {
                return defs[i];
            }
        }
        return null;
    }

    /**
     * Checks whether the given property can be set to the given
     * value in an instance of this node type. The check is implemented
     * by retrieving a matching property definition and validating
     * all defined constraints and other settings for the given value.
     *
     * @param name property name
     * @param value property value
     * @return <code>true</code> if the property can be set,
     *         <code>false</code> otherwise
     * @see NodeType#canSetProperty(String, Value)
     */
    public boolean canSetProperty(String name, Value value) {
        PropertyDefinition def = getPropertyDefinition(name);
        if (def == null || def.isMultiple() || def.isProtected()) {
            return false;
        } else {
            // TODO check type conversion & value constraints
            return value != null;
        }
    }

    /**
     * Checks whether the given property can be set to the given
     * multi-value in an instance of this node type. The check is
     * implemented by retrieving a matching property definition and
     * validating all defined constraints and other settings for the
     * given values.
     *
     * @param name property name
     * @param values property values
     * @return <code>true</code> if the property can be set,
     *         <code>false</code> otherwise
     * @see NodeType#canSetProperty(String, Value[])
     */
    public boolean canSetProperty(String name, Value[] values) {
        PropertyDefinition def = getPropertyDefinition(name);
        if (def == null || !def.isMultiple() || def.isProtected()) {
            return false;
        } else {
            // TODO check type conversion & value constraints
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Checks whether the given child node can be added to an instance
     * of this node type. The check is implemented by retrieving a
     * matching child node definition and verifying that the definition
     * allows the child node to be added.
     *
     * @param name child node name
     * @return <code>true</code> if the child node can be added,
     *         <code>false</code> otherwise
     * @see NodeType#canAddChildNode(String)
     */
    public boolean canAddChildNode(String name) {
        NodeDefinition def = getChildNodeDefinition(name);
        return def != null && !def.isAutoCreated() && !def.isProtected();
    }

    /**
     * Checks whether the given child node can be added to an instance
     * of this node type. The check is implemented by retrieving a
     * matching child node definition and verifying that the definition
     * allows the child node to be added with the given node type.
     *
     * @param name child node name
     * @param typeName node type name
     * @return <code>true</code> if the typed child node can be added,
     *         <code>false</code> otherwise
     * @see NodeType#canAddChildNode(String, String)
     */
    public boolean canAddChildNode(String name, String typeName) {
        NodeDefinition def = getChildNodeDefinition(name);
        if (def != null && !def.isAutoCreated() && !def.isProtected()) {
            NodeType type = helper.getNodeType(typeName);
            NodeType[] types = def.getRequiredPrimaryTypes();
            for (int i = 0; i < types.length; i++) {
                if (!type.isNodeType(types[i].getName())) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether the given item can be removed from an instance
     * of this node type. The check is implemented by retrieving a
     * matching item definition and verifying that the item is not
     * mandatory or protected.
     *
     * @param name item name
     * @return <code>true</code> if the item can be removed,
     *         <code>false</code> otherwise
     * @see NodeType#canRemoveItem(String)
     */
    public boolean canRemoveItem(String name) {
        NodeDefinition nodeDef = getChildNodeDefinition(name);
        if (nodeDef != null && !WILDCARD.equals(nodeDef.getName())) {
            return !nodeDef.isMandatory() && !nodeDef.isProtected();
        }

        PropertyDefinition propertyDef = getPropertyDefinition(name);
        if (propertyDef != null && !WILDCARD.equals(nodeDef.getName())) {
            return !propertyDef.isMandatory() && !propertyDef.isProtected();
        }

        return nodeDef != null || propertyDef != null;
    }

}
