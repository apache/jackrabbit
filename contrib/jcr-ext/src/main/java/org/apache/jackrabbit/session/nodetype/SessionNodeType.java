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
package org.apache.jackrabbit.session.nodetype;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.session.SessionHelper;
import org.apache.jackrabbit.base.nodetype.BaseNodeType;
import org.apache.jackrabbit.name.QName;
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
final class SessionNodeType extends BaseNodeType implements NodeType {

    /** The wildcard item definition name. */
    private static final String WILDCARD = "*";

    /** Helper for accessing the current session. */
    private final SessionHelper helper;

    /** The underlying node type state. */
    private final NodeTypeState state;

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
        return state.hasOrderableChildNodes();
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
        QName name = state.getPrimaryItemName();
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
     *
     * @return declared supertypes
     * @see NodeType#getDeclaredSupertypes()
     */
    public NodeType[] getDeclaredSupertypes() {
        Set types = new HashSet();
        QName[] names = state.getSupertypeNames();
        for (int i = 0; i < names.length; i++) {
            types.add(helper.getNodeType(names[i]));
        }
        return (NodeType[]) types.toArray(new NodeType[types.size()]);
    }

    /**
     * Returns the declared child node definitions of this node type.
     * The returned child node definitions are SessionNodeDefs instantiated
     * using the node definition states returned by the underlying node type
     * state.
     * <p>
     * The returned array is freshly instantiated and not a part of the
     * underlying state, so it can be freely modified.
     *
     * @return declared child node definitions
     * @see SessionNodeDefinition
     * @see NodeType#getDeclaredChildNodeDefinitions()
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        Set definitions = new HashSet();
        NodeDefinitionState[] states = state.getChildNodeDefinitionStates();
        for (int i = 0; i < states.length; i++) {
            definitions.add(new SessionNodeDefinition(helper, this, states[i]));
        }
        return (NodeDefinition[])
            definitions.toArray(new NodeDefinition[definitions.size()]);
    }

    /**
     * Returns the declared property definitions of this node type.
     * The returned property definitions are SessionPropertyDefs instantiated
     * using the property definition states returned by the underlying
     * node type state.
     * <p>
     * The returned array is freshly instantiated and not a part of the
     * underlying state, so it can be freely modified.
     *
     * @return declared child node definitions
     * @see SessionPropertyDefinition
     * @see NodeType#getDeclaredChildNodeDefs()
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        Set definitions = new HashSet();
        PropertyDefinitionState[] states =
            state.getPropertyDefinitionStates();
        for (int i = 0; i < states.length; i++) {
            definitions.add(
                    new SessionPropertyDefinition(helper, this, states[i]));
        }
        return (PropertyDefinition[])
            definitions.toArray(new PropertyDefinition[definitions.size()]);
    }

    /**
     * Compares objects for equality. Returns <code>true</code> if the
     * given object is a SessionNodeType with the same underlying node
     * type state and session.
     * <p>
     * Note that the node type state class does not override the equals
     * method and thus the mutable state instances are compared for
     * reference equality.
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
            return state.equals(((SessionNodeType) that).state)
                && helper.equals(((SessionNodeType) that).helper);
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code for this object. To satisfy the equality
     * constraints the returned hash code is a combination of the
     * hash codes of the underlying node type state and session.
     *
     * @return hash code
     * @see Object#hashCode()
     */
    public int hashCode() {
        int code = 17;
        code = code * 37 + state.hashCode();
        code = code * 37 + helper.hashCode();
        return code;
    }

}
