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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.PropertyState;

import javax.jcr.version.VersionException;

/**
 * The InternalFrozenNode interface represents the frozen node that was generated
 * during a {@link javax.jcr.Node#checkin()}. It holds the set of frozen
 * properties, the frozen child nodes and the frozen version history
 * references of the original node.
 */
public interface InternalFrozenNode extends InternalFreeze {

    /**
     * Returns the list of frozen child nodes
     *
     * @return an array of internal freezes
     * @throws VersionException if the freezes cannot be retrieved
     */
    public InternalFreeze[] getFrozenChildNodes() throws VersionException;

    /**
     * Returns the list of frozen properties.
     *
     * @return an array of property states
     */
    public PropertyState[] getFrozenProperties();

    /**
     * Returns the frozen UUID.
     *
     * @return the frozen uuid.
     */
    public String getFrozenUUID();

    /**
     * Returns the name of frozen primary type.
     *
     * @return the name of the frozen primary type.
     */
    public QName getFrozenPrimaryType();

    /**
     * Returns the list of names of the frozen mixin types.
     *
     * @return the list of names of the frozen mixin types.
     */
    public QName[] getFrozenMixinTypes();

    /**
     * Checks if this frozen node has the frozen version history
     * @param uuid
     * @return
     */
    public boolean hasFrozenHistory(String uuid);

}