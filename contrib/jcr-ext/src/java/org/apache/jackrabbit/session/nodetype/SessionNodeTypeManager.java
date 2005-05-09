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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.iterator.ArrayNodeTypeIterator;
import org.apache.jackrabbit.session.SessionHelper;
import org.apache.jackrabbit.state.nodetype.NodeTypeManagerState;
import org.apache.jackrabbit.state.nodetype.NodeTypeState;

/**
 * Immutable and session-bound node type manager frontend. An instance
 * of this class presents the underlying node type manager state using
 * the JCR NodeTypeManager interface.
 * <p>
 * By not exposing the setter methods of the underlying state instance,
 * this class intentionally makes it impossible for a JCR client to modify
 * node type information.
 */
public class SessionNodeTypeManager implements NodeTypeManager {

    /** Helper for accessing the current session. */
    private final SessionHelper helper;

    /** The underlying node type manager state instance. */
    private final NodeTypeManagerState state;

    /** Memorized set of all node types. Initially <code>null</code>. */
    private NodeType[] allTypes;

    /** Memorized set of primary node types. Initially <code>null</code>. */
    private NodeType[] primaryTypes;

    /** Memorized set of mixin node types. Initially <code>null</code>. */
    private NodeType[] mixinTypes;

    /**
     * Creates a node type manager frontend that is bound to the
     * given session and underlying node type manager state.
     *
     * @param helper helper for accessing the current session
     * @param state underlying node type manager state
     */
    public SessionNodeTypeManager(
            SessionHelper helper, NodeTypeManagerState state) {
        this.helper = helper;
        this.state = state;
        this.allTypes = null;
        this.primaryTypes = null;
        this.mixinTypes = null;
    }

    /**
     * Returns the named node type. This implementation iterates through
     * all the available node types and returns the one that matches the
     * given name. If no matching node type is found, then a
     * NoSuchNodeTypeException is thrown.
     *
     * @param name node type name
     * @return named node type
     * @throws NoSuchNodeTypeException if the named node type does not exist
     * @see NodeTypeManager#getNodeType(String)
     */
    public NodeType getNodeType(String name) throws NoSuchNodeTypeException {
        NodeTypeIterator iterator = getAllNodeTypes();
        while (iterator.hasNext()) {
            NodeType type = iterator.nextNodeType();
            if (name.equals(type.getName())) {
                return type;
            }
        }
        throw new NoSuchNodeTypeException("Node type " + name + " not found");
    }

    /**
     * Returns all available node types. The returned node types are
     * SessionNodeTypes instantiated using the node type states returned
     * by the underlying node type manager state.
     * <p>
     * The set of all node types is memorized to improve performance,
     * and will therefore not change even if the underlying state changes!
     *
     * @return all node types
     * @see SessionNodeType
     * @see NodeTypeManager#getAllNodeTypes()
     */
    public NodeTypeIterator getAllNodeTypes() {
        if (allTypes == null) {
            Set types = new HashSet();
            NodeTypeState[] states = state.getNodeTypeStates();
            for (int i = 0; i < states.length; i++) {
                types.add(new SessionNodeType(helper, states[i]));
            }
            allTypes = (NodeType[]) types.toArray(new NodeType[types.size()]);
        }
        return new ArrayNodeTypeIterator(allTypes);
    }

    /**
     * Returns all primary node types. This method is implemented by
     * listing all available node types and selecting only the primary
     * node types.
     * <p>
     * The set of primary node types is memorized to improve performance,
     * and will therefore not change even if the underlying state changes!
     *
     * @return primary node types
     * @see NodeTypeManager#getPrimaryNodeTypes()
     */
    public NodeTypeIterator getPrimaryNodeTypes() {
        if (primaryTypes == null) {
            Set types = new HashSet();
            NodeTypeIterator iterator = getAllNodeTypes();
            while (iterator.hasNext()) {
                NodeType type = iterator.nextNodeType();
                if (!type.isMixin()) {
                    types.add(type);
                }
            }
            primaryTypes =
                (NodeType[]) types.toArray(new NodeType[types.size()]);
        }
        return new ArrayNodeTypeIterator(primaryTypes);
    }

    /**
     * Returns all mixin node types. This method is implemented by
     * listing all available node types and selecting only the mixin
     * node types.
     * <p>
     * The set of mixin node types is memorized to improve performance,
     * and will therefore not change even if the underlying state changes!
     *
     * @return mixin node types
     * @see NodeTypeManager#getMixinNodeTypes()
     */
    public NodeTypeIterator getMixinNodeTypes() {
        if (mixinTypes == null) {
            Set types = new HashSet();
            NodeTypeIterator iterator = getAllNodeTypes();
            while (iterator.hasNext()) {
                NodeType type = iterator.nextNodeType();
                if (type.isMixin()) {
                    types.add(type);
                }
            }
            mixinTypes =
                (NodeType[]) types.toArray(new NodeType[types.size()]);
        }
        return new ArrayNodeTypeIterator(mixinTypes);
    }

}
