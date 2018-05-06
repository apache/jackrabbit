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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * Implements a internal representation of an activity node.
 * this is only for the {@link InternalXAVersionManager}.
 */
class InternalActivityImpl extends InternalVersionItemImpl implements InternalActivity {

    /**
     * Creates a new VersionHistory object for the given node state.
     * @param vMgr version manager
     * @param node version history node state
     * @throws RepositoryException if an error occurs
     */
    public InternalActivityImpl(InternalVersionManagerBase vMgr, NodeStateEx node)
            throws RepositoryException {
        super(vMgr, node);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeId getId() {
        return node.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InternalVersionItem getParent() {
        return null;
    }

    /**
     * Creates a new activity history below the given parent node and with
     * the given name.
     *
     * @param parent parent node
     * @param name activity name
     * @param activityId node id for the new activity
     * @param title title of the activity
     * @return new node state
     * @throws RepositoryException if an error occurs
     */
    static NodeStateEx create(NodeStateEx parent, Name name, NodeId activityId,
                              String title)
            throws RepositoryException {

        // create new activity node in the persistent state
        NodeStateEx pNode = parent.addNode(name, NameConstants.NT_ACTIVITY, activityId, true);
        Set<Name> mix = new HashSet<Name>();
        mix.add(NameConstants.REP_VERSION_REFERENCE);
        pNode.setMixins(mix);

        // set the title
        pNode.setPropertyValue(NameConstants.JCR_ACTIVITY_TITLE, InternalValue.create(title));

        parent.store();

        return pNode;
    }

    /**
     * Adds a version reference
     * @param v the version
     * @throws RepositoryException if an error occurs
     */
    public void addVersion(InternalVersionImpl v) throws RepositoryException {
        InternalValue[] versions;
        if (node.hasProperty(NameConstants.REP_VERSIONS)) {
            InternalValue[] vs = node.getPropertyValues(NameConstants.REP_VERSIONS);
            versions = new InternalValue[vs.length+1];
            System.arraycopy(vs, 0, versions, 0, vs.length);
            versions[vs.length] = InternalValue.create(v.getId());
        } else {
            versions = new InternalValue[]{InternalValue.create(v.getId())};
        }
        node.setPropertyValues(NameConstants.REP_VERSIONS, PropertyType.REFERENCE, versions);
        node.store();
    }

    /**
     * Removes the given version from the list of references
     * @param v the version
     * @throws RepositoryException if an error occurs
     */
    public void removeVersion(InternalVersionImpl v) throws RepositoryException {
        List<InternalValue> versions = new LinkedList<InternalValue>();
        if (node.hasProperty(NameConstants.REP_VERSIONS)) {
            NodeId vId = v.getId();
            for (InternalValue ref: node.getPropertyValues(NameConstants.REP_VERSIONS)) {
                if (!vId.equals(ref.getNodeId())) {
                    versions.add(ref);
                }
            }
        }
        node.setPropertyValues(NameConstants.REP_VERSIONS, PropertyType.REFERENCE, versions.toArray(new InternalValue[versions.size()]));
        node.store();

    }

    /**
     * Returns the latest version of the given history that is referenced in this activity.
     * @param history the history
     * @return the version
     * @throws RepositoryException if an error occurs
     */
    public InternalVersion getLatestVersion(InternalVersionHistory history)
            throws RepositoryException {
        if (node.hasProperty(NameConstants.REP_VERSIONS)) {
            InternalVersion best = null;
            for (InternalValue ref: node.getPropertyValues(NameConstants.REP_VERSIONS)) {
                InternalVersion v = history.getVersion(ref.getNodeId());
                if (v != null) {
                    // currently we assume that the last version is the best
                    best = v;
                }
            }
            return best;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public VersionSet getChangeSet() throws RepositoryException {
        Map<NodeId, InternalVersion> changeset = new HashMap<NodeId, InternalVersion>();
        if (node.hasProperty(NameConstants.REP_VERSIONS)) {
            for (InternalValue ref: node.getPropertyValues(NameConstants.REP_VERSIONS)) {
                InternalVersion v = vMgr.getVersion(ref.getNodeId());
                changeset.put(v.getVersionHistory().getId(), v);
            }
        }
        return new VersionSet(changeset);
    }
}