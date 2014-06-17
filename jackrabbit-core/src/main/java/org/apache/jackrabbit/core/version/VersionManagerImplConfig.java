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

import java.util.Set;
import java.util.HashSet;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * The JCR Version Manager implementation is split in several classes in order to
 * group related methods together.
 * <p>
 * this class provides methods for the configuration and baselines related operations.
 * <p>
 * Implementation note: methods starting with "internal" are considered to be
 * executed within a "write operations" block.
 */
abstract public class VersionManagerImplConfig extends VersionManagerImplMerge {

    /**
     * default logger
     */
    private static final Logger log = LoggerFactory.getLogger(VersionManagerImplConfig.class);

    /**
     * Creates a new version manager for the given session
     *
     * @param context component context of the current session
     * @param stateMgr the underlying state manager
     * @param hierMgr local hierarchy manager
     */
    protected VersionManagerImplConfig(
            SessionContext context, UpdatableItemStateManager stateMgr,
            HierarchyManager hierMgr) {
        super(context, stateMgr, hierMgr);
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
    protected NodeId restore(NodeStateEx parent, Name name,
                             InternalBaseline baseline)
            throws RepositoryException {
        // check if nt:configuration exists
        NodeId configId = baseline.getConfigurationId();
        NodeId rootId = baseline.getConfigurationRootId();
        if (stateMgr.hasItemState(rootId)) {
            NodeStateEx existing = parent.getNode(rootId);
            String msg = "Configuration for the given baseline already exists at: " + safeGetJCRPath(existing);
            log.error(msg);
            throw new UnsupportedRepositoryOperationException(msg);
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
            String msg = "Internal error: supplied baseline has no version for its configuration root.";
            log.error(msg);
            throw new RepositoryException(msg);
        }

        // create new node below parent
        WriteOperation ops = startWriteOperation();
        try {
            if (!stateMgr.hasItemState(configId)) {
                // create if nt:configuration node is not exists
                internalCreateConfiguration(rootId, configId, baseline.getId());
            }
            NodeStateEx config = parent.getNode(configId);

            // create the root node so that the restore works
            InternalFrozenNode fn = rootVersion.getFrozenNode();
            NodeStateEx state = parent.addNode(name, fn.getFrozenPrimaryType(), fn.getFrozenId());
            state.setMixins(fn.getFrozenMixinTypes());
            parent.store();

            // and finally restore the config and root
            internalRestore(config, baseline, null, false);

            ops.save();
            return configId;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            ops.close();
        }
    }

    /**
     * Creates a new configuration node.
     * <p>
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
            NodeId configId = internalCreateConfiguration(state.getNodeId(), null, null);

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
     * Creates a new configuration node.
     * <p>
     * The nt:confguration is stored within the nt:configurations storage using
     * the nodeid of the configuration root (rootId) as path.
     *
     * @param rootId the id of the configuration root node
     * @param configId the id of the configuration node
     * @param baseLine id of the baseline version or <code>null</code>
     * @return the node id of the created configuration
     * @throws RepositoryException if an error occurs
     */
    private NodeId internalCreateConfiguration(NodeId rootId,
                                              NodeId configId, NodeId baseLine)
            throws RepositoryException {

        NodeStateEx configRoot = internalGetConfigRoot();
        NodeStateEx configParent = InternalVersionManagerBase.getParentNode(
                configRoot,
                rootId.toString(),
                NameConstants.REP_CONFIGURATIONS);
        Name name = InternalVersionManagerBase.getName(rootId.toString());

        if (configId == null) {
            configId = context.getNodeIdFactory().newNodeId();
        }
        NodeStateEx config = configParent.addNode(name, NameConstants.NT_CONFIGURATION, configId, true);
        Set<Name> mix = new HashSet<Name>();
        mix.add(NameConstants.REP_VERSION_REFERENCE);
        config.setMixins(mix);
        config.setPropertyValue(NameConstants.JCR_ROOT, InternalValue.create(rootId));

        // init mix:versionable flags
        VersionHistoryInfo vh = vMgr.getVersionHistory(session, config.getState(), null);

        // and set the base version and history to the config
        InternalValue historyId = InternalValue.create(vh.getVersionHistoryId());
        InternalValue versionId = InternalValue.create(
                baseLine == null ? vh.getRootVersionId() : baseLine);

        config.setPropertyValue(NameConstants.JCR_BASEVERSION, versionId);
        config.setPropertyValue(NameConstants.JCR_VERSIONHISTORY, historyId);
        config.setPropertyValue(NameConstants.JCR_ISCHECKEDOUT, InternalValue.create(true));
        config.setPropertyValues(NameConstants.JCR_PREDECESSORS, PropertyType.REFERENCE, new InternalValue[]{versionId});
        configParent.store();

        return configId;
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