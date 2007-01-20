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

import org.apache.jackrabbit.session.SessionHelper;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.state.nodetype.NodeDefinitionState;

/**
 * Immutable and session-bound node definition frontend. An instance
 * of this class presents the underlying node definition state using
 * the JCR NodeDef interface.
 * <p>
 * By not exposing the setter methods of the underlying state instance,
 * this class intentionally makes it impossible for a JCR client to modify
 * node definition information.
 */
final class SessionNodeDefinition extends SessionItemDefinition
        implements NodeDefinition {

    /** Helper for accessing the current session. */
    private final SessionHelper helper;

    /** The underlying node definition state. */
    private final NodeDefinitionState state;

    /**
     * Creates a node definition frontend that is bound to the
     * given node type, session, and underlying node definition state.
     *
     * @param helper helper for accessing the current session
     * @param type declaring node type
     * @param state underlying node definition state
     */
    public SessionNodeDefinition(
            SessionHelper helper, NodeType type, NodeDefinitionState state) {
        super(helper, type, state);
        this.helper = helper;
        this.state = state;
    }

    /**
     * Returns the default primary type of the defined node. The returned
     * node type is retrieved from the node type manager of the current
     * session using the node type name stored in the underlying state.
     *
     * @return default primary type
     * @see NodeDefinition#getDefaultPrimaryType()
     */
    public NodeType getDefaultPrimaryType() {
        return helper.getNodeType(state.getDefaultPrimaryTypeName());
    }

    /**
     * Returns the required primary types of the defined node. The returned
     * node types are retrieved from the node type manager of the current
     * session using the node type names stored in the underlying state.
     * <p>
     * The returned array is freshly instantiated and not a part of the
     * underlying state, so it can be freely modified.
     *
     * @return required primary types
     * @see NodeDefinition#getRequiredPrimaryTypes()
     */
    public NodeType[] getRequiredPrimaryTypes() {
        Set types = new HashSet();
        QName[] names = state.getRequiredPrimaryTypeNames();
        for (int i = 0; i < names.length; i++) {
            types.add(helper.getNodeType(names[i]));
        }
        return (NodeType[]) types.toArray(new NodeType[types.size()]);
    }

    /**
     * Returns the value of the AllowsSameNameSiblings node definition property.
     * The returned value is retrieved from the underlying node definition
     * state.
     *
     * @return AllowsSameNameSiblings property value
     * @see NodeDefinition#allowsSameNameSiblings()
     */
    public boolean allowsSameNameSiblings() {
        return state.allowsSameNameSiblings();
    }

}
