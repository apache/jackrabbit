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

/**
 * The <code>InconsistentVersionControlState</code> is used to signal
 * inconsistencies in the versioning related state of a node, such
 * as missing mandatory properties, missing version nodes, etc.
 */
public class InconsistentVersioningState extends RuntimeException {

    private final NodeId versionHistoryNodeId;
    
    /**
     * Constructs a new instance of this class with the specified detail
     * message.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public InconsistentVersioningState(String message) {
        super(message);
        this.versionHistoryNodeId = null;
    }

    /**
     * Constructs a new instance of this class with the specified detail
     * message.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     * @param rootCause root cause (or otherwise <code>null</code>)
     * @param versionHistoryNodeId NodeId of the version history that has problems (or otherwise <code>null</code>
     */
    public InconsistentVersioningState(String message, NodeId versionHistoryNodeId, Throwable rootCause) {
        super(message, rootCause);
        this.versionHistoryNodeId = versionHistoryNodeId;
    }

    /**
     * @return the NodeId of the version history having problems or <code>null</code>
     * when unknown.
     */
    public NodeId getVersionHistoryNodeId() {
        return this.versionHistoryNodeId;
    }
}
