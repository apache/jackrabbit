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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
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
     * @return the node id of the configuration
     * @throws RepositoryException if an error occurs
     */
    protected NodeId restore(NodeStateEx parent, Name name, InternalBaseline baseline)
            throws RepositoryException {
        NodeStateEx config = parent.getNode(baseline.getVersionHistory().getVersionableId());
        NodeId rootId = config.getPropertyValue(NameConstants.JCR_ROOT).getNodeId();
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
            return config.getNodeId();
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            ops.close();
        }
    }

    /**
     * Performs a configuration restore
     * @param config config to restore
     * @param name name of the baseline version
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     */
//    protected void restore(InternalConfiguration config, Name name, boolean removeExisting)
//            throws RepositoryException {
//        throw new UnsupportedRepositoryOperationException("not implemented, yet");
//    }

    /**
     * Performs a configuration restore
     * @param config config to restore
     * @param name label of the baseline version
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     */
//    protected void restoreByLabel(InternalConfiguration config, Name name, boolean removeExisting)
//            throws RepositoryException {
//        throw new UnsupportedRepositoryOperationException("not implemented, yet");
//    }

    /**
     * Performs a configuration restore
     * @param config config to restore
     * @param version baseline version to restore
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     */
//    protected void restore(InternalConfiguration config, Version version, boolean removeExisting)
//            throws RepositoryException {
//        throw new UnsupportedRepositoryOperationException("not implemented, yet");
//    }

    /**
     * Creates a new configuration node.
     * <p/>
     * The nt:confguration is stored within the nt:configurations storage using
     * the nodeid of the configuration root (rootId) as path.
     *
     * @param state the node of the workspace configuration
     * @return the node id of the created configuration
     * @throws RepositoryException if an error occurs
     */
    protected NodeId createConfiguration(NodeStateEx state)
            throws RepositoryException {

        WriteOperation ops = startWriteOperation();
        try {
            NodeId rootId = state.getNodeId();
            NodeStateEx configRoot = internalGetConfigRoot();
            NodeStateEx configParent = InternalVersionManagerBase.getParentNode(
                    configRoot,
                    rootId.toString(),
                    NameConstants.REP_CONFIGURATIONS);
            Name name = InternalVersionManagerBase.getName(rootId.toString());

            NodeId configId = new NodeId();
            NodeStateEx config = configParent.addNode(name, NameConstants.NT_CONFIGURATION, configId, true);
            config.setPropertyValue(NameConstants.JCR_ROOT, InternalValue.create(rootId));

            // init mix:versionable flags
            VersionHistoryInfo vh = vMgr.getVersionHistory(session, config.getState(), null);

            // and set the base version and history to the config
            InternalValue historyId = InternalValue.create(vh.getVersionHistoryId());
            InternalValue versionId = InternalValue.create(vh.getRootVersionId());

            config.setPropertyValue(NameConstants.JCR_BASEVERSION, versionId);
            config.setPropertyValue(NameConstants.JCR_VERSIONHISTORY, historyId);
            config.setPropertyValue(NameConstants.JCR_ISCHECKEDOUT, InternalValue.create(true));
            config.setPropertyValues(NameConstants.JCR_PREDECESSORS, PropertyType.REFERENCE, new InternalValue[]{versionId});
            configParent.store();

            // set configuration reference in state
            state.setPropertyValue(NameConstants.JCR_CONFIGURATION, InternalValue.create(configId));
            state.store();

            ops.save();

            return configId;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            ops.close();
        }
    }

    /**
     * Returns the root node of the configurations storage located at
     * "/jcr:system/jcr:configurations"
     *
     * @return the root node
     * @throws RepositoryException if an error occurs
     */
    private NodeStateEx internalGetConfigRoot() throws RepositoryException {
        NodeStateEx system = getNodeStateEx(RepositoryImpl.SYSTEM_ROOT_NODE_ID);
        NodeStateEx root = system.getNode(NameConstants.JCR_CONFIGURATIONS, 1);
        if (root == null) {
            root = system.addNode(
                    NameConstants.JCR_CONFIGURATIONS,
                    NameConstants.REP_CONFIGURATIONS,
                    RepositoryImpl.CONFIGURATIONS_NODE_ID, false);
            system.store();
        }
        return root;
    }
}