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

import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.session.SessionHelper;
import org.apache.jackrabbit.state.nodetype.PropertyDefinitionState;

/**
 * Immutable and session-bound property definition frontend. An instance
 * of this class presents the underlying property definition state using
 * the JCR PropertyDef interface.
 * <p>
 * By not exposing the setter methods of the underlying state instance,
 * this class intentionally makes it impossible for a JCR client to modify
 * property definition information.
 */
final class SessionPropertyDefinition extends SessionItemDefinition
        implements PropertyDefinition {

    /** The underlying property definition state. */
    private final PropertyDefinitionState state;

    /**
     * Creates a property definition frontend that is bound to the
     * given node type, session, and underlying property definition state.
     *
     * @param helper helper for accessing the current session
     * @param type declaring node type
     * @param state underlying property definition state
     */
    public SessionPropertyDefinition(
            SessionHelper helper, NodeType type, PropertyDefinitionState state) {
        super(helper, type, state);
        this.state = state;
    }

    /**
     * Returns the required type of the defined property. The returned value
     * is retrieved from the underlying property definition state.
     *
     * @return required property type
     * @see PropertyDef#getRequiredType()
     */
    public int getRequiredType() {
        return state.getRequiredType();
    }

    /**
     * Returns the constraint strings that specify the value constraint
     * of the defined property. The returned string array is retrieved
     * from the underlying property definition state, but is not by itself
     * a part of the state and can thus be modified freely.
     *
     * @return value constraint strings
     * @see PropertyDef#getValueConstraints()
     */
    public String[] getValueConstraints() {
        return null; // TODO: See PropertyDefinitionState
    }

    /**
     * Returns the value of the Multiple property definition property.
     * The returned value is retrieved from the underlying property
     * definition state.
     *
     * @return Multiple property value
     * @see PropertyDef#isMultiple()
     */
    public boolean isMultiple() {
        return state.isMultiple();
    }

    /** Not implemented. */
    public Value[] getDefaultValues() {
        return null; // TODO
    }
}
