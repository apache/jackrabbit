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

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;

/**
 * This Class implements a version selector that is based on a set of versions.
 */
public class VersionSet implements VersionSelector {

    /**
     * the internal changeset
     */
    private final Map<NodeId, InternalVersion> versions;

    /**
     * fallback flag
     */
    private final boolean dateFallback;

    /**
     * Creates a <code>ChangeSetVersionSelector</code> that will try to select a
     * version within the given set of versions.
     *
     * @param versions the set of versions
     */
    public VersionSet(Map<NodeId, InternalVersion> versions) {
        this(versions, false);
    }

    /**
     * Creates a <code>ChangeSetVersionSelector</code> that will try to select a
     * version in the given set. 
     *
     * @param versions the set of versions
     * @param dateFallback if <code>true</code> date fallback is enabled.
     */
    public VersionSet(Map<NodeId, InternalVersion> versions, boolean dateFallback) {
        this.versions = versions;
        this.dateFallback = dateFallback;
    }

    /**
     * Returns the (modifiable) changeset of this selector. the keys of the
     * map are the node ids of the version histories.
     * @return the change set
     */
    public Map<NodeId, InternalVersion> versions() {
        return versions;
    }

    /**
     * {@inheritDoc}
     *
     * Selects a version from set having the given version history.
     */
    public InternalVersion select(InternalVersionHistory versionHistory)
            throws RepositoryException {
        InternalVersion v = versions.get(versionHistory.getId());
        if (v == null && dateFallback) {
            // select latest one
            v = DateVersionSelector.selectByDate(versionHistory, null);
        }
        return v;
    }

}