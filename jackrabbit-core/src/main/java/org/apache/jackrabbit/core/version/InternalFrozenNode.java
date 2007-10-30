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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;

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
    InternalFreeze[] getFrozenChildNodes() throws VersionException;

    /**
     * Returns the list of frozen properties.
     *
     * @return an array of property states
     */
    PropertyState[] getFrozenProperties();

    /**
     * Returns the frozen UUID.
     *
     * @return the frozen uuid.
     */
    UUID getFrozenUUID();

    /**
     * Returns the name of frozen primary type.
     *
     * @return the name of the frozen primary type.
     */
    Name getFrozenPrimaryType();

    /**
     * Returns the list of names of the frozen mixin types.
     *
     * @return the list of names of the frozen mixin types.
     */
    Name[] getFrozenMixinTypes();

    /**
     * Checks if this frozen node has the frozen version history
     * @param uuid
     * @return <code>true</code> if this node has the history;
     *         <code>false</code> otherwise.
     */
    boolean hasFrozenHistory(UUID uuid);

}
