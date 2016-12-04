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
 * Simple data object that carries the identifiers of a version history node
 * and the related root version node.
 *
 * @since Apache Jackrabbit 1.5
 * @see <a href="https://issues.apache.org/jira/browse/JCR-1775">JCR-1775</a>
 */
public class VersionHistoryInfo {

    /**
     * Identifier of the version history node.
     */
    private final NodeId versionHistoryId;

    /**
     * Identifier of the root version node.
     */
    private final NodeId rootVersionId;

    /**
     * Creates an object that carries the given version history information.
     *
     * @param versionHistoryId identifier of the version history node
     * @param rootVersionId identifier of the root version node
     */
    public VersionHistoryInfo(NodeId versionHistoryId, NodeId rootVersionId) {
        this.versionHistoryId = versionHistoryId;
        this.rootVersionId = rootVersionId;
    }

    /**
     * Returns the identifier of the version history node.
     */
    public NodeId getVersionHistoryId() {
        return versionHistoryId;
    }

    /**
     * Returns the identifier of the root version node.
     */
    public NodeId getRootVersionId() {
        return rootVersionId;
    }

}
