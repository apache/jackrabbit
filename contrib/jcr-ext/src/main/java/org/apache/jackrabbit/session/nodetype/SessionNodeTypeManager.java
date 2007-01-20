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

import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.base.nodetype.BaseNodeTypeManager;
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
public final class SessionNodeTypeManager extends BaseNodeTypeManager
        implements NodeTypeManager {

    /** Helper for accessing the current session. */
    private final SessionHelper helper;

    /** The underlying node type manager state instance. */
    private final NodeTypeManagerState state;

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
    }

    /**
     * Returns all available node types. The returned node types are
     * SessionNodeTypes instantiated using the node type states returned
     * by the underlying node type manager state.
     *
     * @return all node types
     * @see SessionNodeType
     * @see NodeTypeManager#getAllNodeTypes()
     * @see NodeTypeManagerState#getNodeTypeStates()
     */
    public NodeTypeIterator getAllNodeTypes() {
        Set types = new HashSet();
        NodeTypeState[] states = state.getNodeTypeStates();
        for (int i = 0; i < states.length; i++) {
            types.add(new SessionNodeType(helper, states[i]));
        }
        return new ArrayNodeTypeIterator(types);
    }

    /**
     * Compares objects for equality. Returns <code>true</code> if the
     * given object is a SessionNodeTypeManager with the same underlying node
     * type manager state and session.
     * <p>
     * Note that the node type manager state class does not override the
     * equals method and thus the mutable state instances are compared for
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
        } else if (that instanceof SessionNodeTypeManager) {
            return state.equals(((SessionNodeTypeManager) that).state)
                && helper.equals(((SessionNodeTypeManager) that).helper);
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code for this object. To satisfy the equality
     * constraints the returned hash code is a combination of the
     * hash codes of the underlying node type manager state and session.
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
