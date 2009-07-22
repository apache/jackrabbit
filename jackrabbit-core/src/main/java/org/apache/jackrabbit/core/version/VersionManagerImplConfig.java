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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * The JCR Version Manager impementation is split in several classes in order to
 * group related methods together.
 * <p/>
 * this class provides methods for the configuration and baselines related operations.
 * <p/>
 * Implementation note: methods starting with "internal" are considered to be
 * executed within a "write operations" block.
 */
abstract public class VersionManagerImplConfig extends VersionManagerImplMerge {

    /**
     * Creates a new version manager for the given session
     * @param session workspace sesion
     * @param stateMgr the underlying state manager
     * @param hierMgr local hierarchy manager
     */
    protected VersionManagerImplConfig(SessionImpl session,
                                        UpdatableItemStateManager stateMgr,
                                        HierarchyManager hierMgr) {
        super(session, stateMgr, hierMgr);
    }

    /**
     * Restores the versions recorded in the given baseline below the specified
     * path.
     * @param parent the parent state
     * @param name the name of the new node (tree)
     * @param baseline the baseline that recorded the versions
     * @return the configuration
     * @throws RepositoryException if an error occurs
     */
    protected InternalConfiguration restore(NodeStateEx parent, Name name, InternalBaseline baseline)
            throws RepositoryException {
        InternalConfiguration config = baseline.getConfiguration();
        NodeId rootId = config.getRootId();
        if (stateMgr.hasItemState(rootId)) {
            NodeStateEx existing = parent.getNode(rootId);
            throw new UnsupportedRepositoryOperationException(
                    "Configuration for the given baseline already exists at: " + safeGetJCRPath(existing));
        }

        // find version for configuration root
        VersionSet versions = baseline.getBaseVersions();
        InternalVersion rootVersion = null;
        for (InternalVersion v: versions.versions().values()) {
            if (v.getVersionHistory().getVersionableId().equals(rootId)) {
                rootVersion = v;
                break;
            }
        }
        if (rootVersion == null) {
            throw new RepositoryException("Internal error: supplied baseline has no version for its configuration root.");
        }

        // create new node below parent
        WriteOperation ops = startWriteOperation();
        try {
            InternalFrozenNode fn = rootVersion.getFrozenNode();
            NodeStateEx state = parent.addNode(name, fn.getFrozenPrimaryType(), fn.getFrozenId());
            state.setMixins(fn.getFrozenMixinTypes());
            parent.store();
            // now just restore all versions
            internalRestore(versions, true);
            ops.save();
            return config;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            ops.close();
        }
    }

    /**
     * Performs a configuration checkin
     * @param config the config
     * @return the id of the new base version
     * @throws RepositoryException if an error occurs
     */
    protected NodeId checkin(InternalConfiguration config) throws RepositoryException {
        NodeStateEx root = getRootNode(config);
        Set<NodeId> baseVersions = new HashSet<NodeId>();
        baseVersions.add(root.getPropertyValue(NameConstants.JCR_BASEVERSION).getNodeId());
        collectBaseVersions(root, baseVersions);
        return vMgr.checkin(session, config, baseVersions).getId();
    }

    /**
     * Recursivly collects all base versions of this configuration tree.
     * @param root node to traverse
     * @param baseVersions set of base versions to fill
     * @throws RepositoryException if an error occurs
     */
    private void collectBaseVersions(NodeStateEx root, Set<NodeId> baseVersions)
            throws RepositoryException {
        for (NodeStateEx child: root.getChildNodes()) {
            if (child.getEffectiveNodeType().includesNodeType(NameConstants.MIX_VERSIONABLE)) {
                if (child.hasProperty(NameConstants.JCR_CONFIGURATION)) {
                    // don't traverse into child nodes that have a jcr:configuration
                    // property as they belong to a different configuration.
                    continue;
                }
                baseVersions.add(child.getPropertyValue(NameConstants.JCR_BASEVERSION).getNodeId());
            }
            collectBaseVersions(child, baseVersions);
        }
    }

    /**
     * Performs a configuration restore
     * @param config config to restore
     * @param name name of the baseline version
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     */
    protected void restore(InternalConfiguration config, Name name, boolean removeExisting)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("not implemented, yet");
    }

    /**
     * Performs a configuration restore
     * @param config config to restore
     * @param name label of the baseline version
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     */
    protected void restoreByLabel(InternalConfiguration config, Name name, boolean removeExisting)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("not implemented, yet");
    }

    /**
     * Performs a configuration restore
     * @param config config to restore
     * @param version baseline version to restore
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     */
    protected void restore(InternalConfiguration config, Version version, boolean removeExisting)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("not implemented, yet");
    }

    /**
     * Returns the configuration root node for the given config.
     * @param config the config
     * @return the root node
     * @throws RepositoryException if an error occurs or the root node does not exist
     */
    private NodeStateEx getRootNode(InternalConfiguration config) throws RepositoryException {
        NodeStateEx root = getNodeStateEx(config.getRootId());
        if (root == null) {
            throw new ItemNotFoundException("Configuration root node for " + config.getId() + " not found.");
        }
        return root;
    }
}