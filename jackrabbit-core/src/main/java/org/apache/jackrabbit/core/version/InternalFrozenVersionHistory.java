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

import org.apache.jackrabbit.core.id.NodeId;

import javax.jcr.version.VersionException;

/**
 * This interface defines a frozen versionable child node, that was created
 * during a {@link javax.jcr.Node#checkin()} with a OPV==Version node.
 */
public interface InternalFrozenVersionHistory extends InternalFreeze {

    /**
     * Returns the id of the version history that was assigned to the node at
     * the time it was versioned.
     *
     * @return the id of the version history
     */
    NodeId getVersionHistoryId();

    /**
     * Returns the version history that was assigned to the node at
     * the time it was versioned.
     *
     * @return the internal version history.
     * @throws VersionException if the history cannot be retrieved.
     */
    InternalVersionHistory getVersionHistory()
            throws VersionException;

    /**
     * Returns the id of the base version that was assigned to the node at
     * the time it was versioned.
     *
     * @return the id of the base version
     */
    NodeId getBaseVersionId();

    /**
     * @deprecated use {@link #getBaseVersion()} instead
     */
    InternalVersion getBaseVesion() throws VersionException;

    /**
     * Returns the base version that was assigned to the node at
     * the time it was versioned.
     *
     * @return the internal base version
     * @throws VersionException if the version could not be retrieved
     */
    InternalVersion getBaseVersion() throws VersionException;
}
