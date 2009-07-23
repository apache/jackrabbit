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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.core.BatchedItemOperations;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemValidator;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JCR Version Manager impementation is split in several classes in order to
 * group related methods together.
 * <p/>
 * this class provides methods for the restore operations.
 * <p/>
 * Implementation note: methods starting with "internal" are considered to be
 * executed within a "write operations" block.
 */
abstract public class VersionManagerImplRestore extends VersionManagerImplBase {

    /**
     * default logger
     */
    private static final Logger log = LoggerFactory.getLogger(VersionManagerImplRestore.class);

    /**
     * Creates a new version manager for the given session
     * @param session workspace sesion
     * @param stateMgr the underlying state manager
     * @param hierMgr local hierarchy manager
     */
    protected VersionManagerImplRestore(SessionImpl session,
                                        UpdatableItemStateManager stateMgr,
                                        HierarchyManager hierMgr) {
        super(session, stateMgr, hierMgr);
    }

    /**
     * @param state the state to restore
     * @param version the version to restore
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     *
     * @see javax.jcr.version.VersionManager#restore(String, Version, boolean)
     */
    protected void restore(NodeStateEx state, Version version, boolean removeExisting)
            throws RepositoryException {
        checkVersionable(state);
        InternalVersion v = getVersion(version);
        // check if 'own' version
        if (!v.getVersionHistory().equals(getVersionHistory(state))) {
            String msg = "Unable to restore version. Not same version history.";
            log.error(msg);
            throw new VersionException(msg);
        }
        WriteOperation ops = startWriteOperation();
        try {
            internalRestore(state, v, new DateVersionSelector(version.getCreated()), removeExisting);
            ops.save();
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            ops.close();
        }
    }

    /**
     * @param state the state to restore
     * @param versionName the name of the version to restore
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     *
     * @see VersionManager#restore(String, String, boolean)
     */
    protected void restore(NodeStateEx state, Name versionName, boolean removeExisting)
            throws RepositoryException {
        checkVersionable(state);
        InternalVersion v = getVersionHistory(state).getVersion(versionName);
        DateVersionSelector gvs = new DateVersionSelector(v.getCreated());
        WriteOperation ops = startWriteOperation();
        try {
            internalRestore(state, v, gvs, removeExisting);
            ops.save();
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            ops.close();
        }
    }

    /**
     * @param state the state to restore
     * @param versionLabel the name of the version to restore
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     *
     * @see VersionManager#restoreByLabel(String, String, boolean)
     */
    protected void restoreByLabel(NodeStateEx state, Name versionLabel, boolean removeExisting)
            throws RepositoryException {
        checkVersionable(state);
        InternalVersion v = getVersionHistory(state).getVersionByLabel(versionLabel);
        if (v == null) {
            String msg = "No version for label " + versionLabel + " found.";
            log.error(msg);
            throw new VersionException(msg);
        }
        WriteOperation ops = startWriteOperation();
        try {
            internalRestore(state, v, new LabelVersionSelector(versionLabel), removeExisting);
            ops.save();
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            ops.close();
        }
    }

    /**
     * Restores the <code>version</code> below the <code>parent</code> node
     * using the indicated <code>name</code>
     *
     * @param parent parent node
     * @param name desired name
     * @param version version to restore
     * @param removeExisting remove exiting flag
     * @throws RepositoryException if an error occurs
     */
    protected void restore(NodeStateEx parent, Name name, Version version, boolean removeExisting)
            throws RepositoryException {
        // check if versionable node exists
        InternalFrozenNode fn = ((VersionImpl) version).getInternalFrozenNode();
        if (stateMgr.hasItemState(fn.getFrozenId())) {
            if (removeExisting) {
                NodeStateEx existing = parent.getNode(fn.getFrozenId());
                checkVersionable(existing);
                InternalVersion v = getVersion(version);

                // move versionable node below this one using the given "name"
                WriteOperation ops = startWriteOperation();
                try {
                    NodeStateEx exParent = existing.getParent();
                    NodeStateEx state = parent.moveFrom(existing, name, false);
                    exParent.store();
                    parent.store();
                    // and restore it
                    internalRestore(state, v, new DateVersionSelector(v.getCreated()), removeExisting);
                    ops.save();
                } catch (ItemStateException e) {
                    throw new RepositoryException(e);
                } finally {
                    ops.close();
                }
            } else {
                String msg = "Unable to restore version. Versionable node already exists.";
                log.error(msg);
                throw new ItemExistsException(msg);
            }
        } else {
            // create new node below parent
            NodeStateEx state = parent.addNode(name, fn.getFrozenPrimaryType(), fn.getFrozenId());
            state.setMixins(fn.getFrozenMixinTypes());
            restore(state, version, removeExisting);
        }
    }

    /**
     * @param versions Versions to restore
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     * @throws ItemStateException if an error occurs
     *
     * @see VersionManager#restore(Version[], boolean)
     * @see VersionManager#restore(Version, boolean)
     */
    protected void internalRestore(VersionSet versions, boolean removeExisting)
            throws RepositoryException, ItemStateException {
        // now restore all versions that have a node in the workspace
        int numRestored = 0;
        while (versions.versions().size() > 0) {
            Set<InternalVersion> restored = null;
            for (InternalVersion v : versions.versions().values()) {
                NodeStateEx state = getNodeStateEx(v.getFrozenNode().getFrozenId());
                if (state != null) {
                    // todo: check should operate on workspace states, too
                    int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_HOLD;
                    checkModify(state, options, Permission.NONE);
                    restored = internalRestore(state, v, versions, removeExisting);
                    // remove restored versions from set
                    for (InternalVersion r : restored) {
                        versions.versions().remove(r.getVersionHistory().getId());
                    }
                    numRestored += restored.size();
                    break;
                }
            }
            if (restored == null) {
                String msg = numRestored == 0
                        ? "Unable to restore. At least one version needs existing versionable node in workspace."
                        : "Unable to restore. All versions with non existing versionable nodes need parent.";
                log.error(msg);
                throw new VersionException(msg);
            }
        }
    }
    
    /**
     * Internal method to restore a version.
     *
     * @param state the state to restore
     * @param version version to restore
     * @param vsel the version selector that will select the correct version for
     * OPV=Version child nodes.
     * @param removeExisting remove existing flag
     * @return set of restored versions
     * @throws RepositoryException if an error occurs
     * @throws ItemStateException if an error occurs
     */
    protected Set<InternalVersion> internalRestore(NodeStateEx state,
                                             InternalVersion version,
                                             VersionSelector vsel,
                                             boolean removeExisting)
            throws RepositoryException, ItemStateException {

        // fail if root version
        if (version.isRootVersion()) {
            String msg = "Restore of root version not allowed.";
            log.error(msg);
            throw new VersionException(msg);
        }

        boolean isFull = checkVersionable(state);

        // check permission
        Path path = hierMgr.getPath(state.getNodeId());
        session.getAccessManager().checkPermission(path, Permission.VERSION_MNGMT);

        // 1. The child node and properties of N will be changed, removed or
        //    added to, depending on their corresponding copies in V and their
        //    own OnParentVersion attributes (see 7.2.8, below, for details).
        Set<InternalVersion> restored = new HashSet<InternalVersion>();
        internalRestoreFrozen(state, version.getFrozenNode(), vsel, restored, removeExisting);
        restored.add(version);

        if (isFull) {
            // 2. N's jcr:baseVersion property will be changed to point to V.
            state.setPropertyValue(
                    NameConstants.JCR_BASEVERSION, InternalValue.create(version.getId()));

            // 4. N's jcr:predecessor property is set to null
            state.setPropertyValues(NameConstants.JCR_PREDECESSORS, PropertyType.REFERENCE, InternalValue.EMPTY_ARRAY);

            // also clear mergeFailed
            state.removeProperty(NameConstants.JCR_MERGEFAILED);

        } else {
            // with simple versioning, the node is checked in automatically,
            // thus not allowing any branches
            vMgr.checkin(session, state);
        }
        // 3. N's jcr:isCheckedOut property is set to false.
        state.setPropertyValue(NameConstants.JCR_ISCHECKEDOUT, InternalValue.create(false));
        state.store();
        
        // check if a baseline is restored
        if (version instanceof InternalBaseline) {
            // just restore all base versions
            InternalBaseline baseline = (InternalBaseline) version;
            internalRestore(baseline.getBaseVersions(), true);

            // ensure that the restored root node has a jcr:configuration property
            // since it might not have been recorded by the initial checkin of the
            // configuration
            NodeId configId = baseline.getConfigurationId();
            NodeId rootId = baseline.getConfigurationRootId();
            NodeStateEx rootNode = state.getNode(rootId);
            rootNode.setPropertyValue(NameConstants.JCR_CONFIGURATION, InternalValue.create(configId));
            rootNode.store();
        }

        return restored;
    }

    /**
     * Restores the properties and child nodes from the frozen state.
     *
     * @param state state to restore
     * @param freeze the frozen node
     * @param vsel version selector
     * @param restored set of restored versions
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     * @throws ItemStateException if an error occurs
     */
    protected void internalRestoreFrozen(NodeStateEx state, InternalFrozenNode freeze, VersionSelector vsel,
                                   Set<InternalVersion> restored, boolean removeExisting)
            throws RepositoryException, ItemStateException {

        // check uuid
        if (state.getEffectiveNodeType().includesNodeType(NameConstants.MIX_REFERENCEABLE)) {
            if (!state.getNodeId().equals(freeze.getFrozenId())) {
                String msg = "Unable to restore version of " + safeGetJCRPath(state) + ". UUID changed.";
                log.error(msg);
                throw new ItemExistsException(msg);
            }
        }

        // check primary type
        if (!freeze.getFrozenPrimaryType().equals(state.getState().getNodeTypeName())) {
            // todo: implement
            String msg = "Unable to restore version of " + safeGetJCRPath(state) + ". PrimaryType change not supported yet.";
            log.error(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }

        // adjust mixins
        state.setMixins(freeze.getFrozenMixinTypes());

        // copy frozen properties
        PropertyState[] props = freeze.getFrozenProperties();
        HashSet<Name> propNames = new HashSet<Name>();
        for (PropertyState prop : props) {
            // skip properties that should not to be reverted back
            if (prop.getName().equals(NameConstants.JCR_ACTIVITY)) {
                continue;
            }
            propNames.add(prop.getName());
            state.copyFrom(prop);
        }
        // remove properties that do not exist in the frozen representation
        for (PropertyState prop: state.getProperties()) {
            // ignore some props that are not well guarded by the OPV
            Name propName = prop.getName();
            if (propName.equals(NameConstants.JCR_VERSIONHISTORY)) {
                // ignore
            } else if (propName.equals(NameConstants.JCR_PREDECESSORS)) {
                // ignore
            } else if (!propNames.contains(propName)) {
                int opv = state.getDefinition(prop).getOnParentVersion();
                if (opv == OnParentVersionAction.COPY || opv == OnParentVersionAction.VERSION) {
                    state.removeProperty(propName);
                }
            }
        }

        // add 'auto-create' properties that do not exist yet
        for (PropDef def: state.getEffectiveNodeType().getAutoCreatePropDefs()) {
            if (!state.hasProperty(def.getName())) {
                // compute system generated values if necessary
                // todo: use NodeTypeInstanceHandler
                InternalValue[] values =
                        BatchedItemOperations.computeSystemGeneratedPropertyValues(state.getState(), def);
                if (values == null) {
                    values = def.getDefaultValues();
                }
                if (values != null) {
                    state.setPropertyValues(def.getName(), def.getRequiredType(), values, def.isMultiple());
                }
            }
        }

        // first delete some of the child nodes. this is a bit tricky, in case
        // the child node index changed. mark an sweep
        LinkedList<ChildNodeEntry> toDelete = new LinkedList<ChildNodeEntry>();
        for (ChildNodeEntry entry: state.getState().getChildNodeEntries()) {
            NodeStateEx child = state.getNode(entry.getName(), entry.getIndex());
            int opv = child.getDefinition().getOnParentVersion();
            if (opv == OnParentVersionAction.COPY) {
                // only remove OPV=Copy nodes
                toDelete.addFirst(entry);
            } else if (opv == OnParentVersionAction.VERSION) {
                // only remove, if node to be restored does not contain child,
                // or if restored child is not versionable
                NodeId vhId = child.hasProperty(NameConstants.JCR_VERSIONHISTORY)
                        ? child.getPropertyValue(NameConstants.JCR_VERSIONHISTORY).getNodeId()
                        : null;
                if (vhId == null || !freeze.hasFrozenHistory(vhId)) {
                    toDelete.addFirst(entry);
                }
            }
        }
        for (ChildNodeEntry entry: toDelete) {
            state.removeNode(entry.getName(), entry.getIndex());
        }
        // need to sync with state manager
        state.store();

        // restore the frozen nodes
        InternalFreeze[] frozenNodes = freeze.getFrozenChildNodes();
        for (InternalFreeze child : frozenNodes) {
            NodeStateEx restoredChild = null;
            if (child instanceof InternalFrozenNode) {
                InternalFrozenNode f = (InternalFrozenNode) child;
                // check for existing
                if (f.getFrozenId() != null) {
                    if (stateMgr.hasItemState(f.getFrozenId())) {
                        NodeStateEx existing = state.getNode(f.getFrozenId());
                        if (removeExisting) {
                            NodeStateEx parent = existing.getParent();
                            parent.removeNode(existing);
                            parent.store();
                        } else if (existing.getState().isShareable()) {
                            // if existing node is shareable, then clone it
                            restoredChild = state.moveFrom(existing, existing.getName(), true);
                        } else {
                            // since we delete the OPV=Copy children beforehand, all
                            // found nodes must be outside of this tree
                            String msg = "Unable to restore node, item already exists " +
                                    "outside of restored tree: " + safeGetJCRPath(existing);
                            log.error(msg);
                            throw new ItemExistsException(msg);
                        }

                    }
                }
                if (restoredChild == null) {
                    restoredChild = state.addNode(f.getName(), f.getFrozenPrimaryType(), f.getFrozenId());
                    restoredChild.setMixins(f.getFrozenMixinTypes());
                    internalRestoreFrozen(restoredChild, f, vsel, restored, removeExisting);
                }

            } else if (child instanceof InternalFrozenVersionHistory) {
                InternalFrozenVersionHistory fh = (InternalFrozenVersionHistory) child;
                InternalVersionHistory vh = vMgr.getVersionHistory(fh.getVersionHistoryId());
                Name oldVersion = null;

                // check if representing versionable already exists somewhere
                NodeId nodeId = vh.getVersionableId();
                if (stateMgr.hasItemState(nodeId)) {
                    NodeStateEx existing = state.getNode(nodeId);
                    if (existing.getParentId() == state.getNodeId()) {
                        // remove
                        state.removeNode(existing);
                    } else if (removeExisting) {
                        NodeStateEx parent = existing.getNode(existing.getNodeId());
                        state.moveFrom(existing, fh.getName(), false);
                        parent.store();

                        // get old version name
                        oldVersion = getBaseVersion(existing).getName();
                    } else {
                        // since we delete the OPV=Copy children beforehand, all
                        // found nodes must be outside of this tree
                        String msg = "Unable to restore node, item already exists " +
                                "outside of restored tree: " + safeGetJCRPath(existing);
                        log.error(msg);
                        throw new ItemExistsException(msg);
                    }
                }
                // get desired version from version selector
                InternalVersion v = vsel.select(vh);

                // check existing version of item exists
                if (!stateMgr.hasItemState(nodeId)) {
                    if (v == null) {
                        // if version selector was unable to select version,
                        // choose the initial one
                        InternalVersion[] vs = vh.getRootVersion().getSuccessors();
                        if (vs.length == 0) {
                            String msg = "Unable to select appropariate version for "
                                    + child.getName() + " using " + vsel;
                            log.error(msg);
                            throw new VersionException(msg);
                        }
                        v = vs[0];
                    }
                    InternalFrozenNode f = v.getFrozenNode();
                    restoredChild = state.addNode(fh.getName(), f.getFrozenPrimaryType(), f.getFrozenId());
                    restoredChild.setMixins(f.getFrozenMixinTypes());
                } else {
                    restoredChild = state.getNode(nodeId);
                    if (v == null || oldVersion == null || v.getName().equals(oldVersion)) {
                        v = null;
                    }
                }
                if (v != null) {
                    try {
                        internalRestore(restoredChild, v, vsel, removeExisting);
                    } catch (RepositoryException e) {
                        log.error("Error while restoring node: " + e);
                        log.error("  child path: " + restoredChild);
                        log.error("  selected version: " + v.getName());
                        StringBuffer avail = new StringBuffer();
                        for (Name name: vh.getVersionNames()) {
                            avail.append(name);
                            avail.append(", ");
                        }
                        log.error("  available versions: " + avail);
                        log.error("  versionselector: " + vsel);
                        throw e;
                    }
                    // add this version to set
                    restored.add(v);
                }
            }
            // ensure proper ordering (issue JCR-469)
            if (restoredChild != null && state.getEffectiveNodeType().hasOrderableChildNodes()) {
                // order at end
                ArrayList<ChildNodeEntry> list = new ArrayList<ChildNodeEntry>(state.getState().getChildNodeEntries());
                ChildNodeEntry toReorder = null;
                boolean isLast = true;
                for (ChildNodeEntry e: list) {
                    if (e.getId().equals(restoredChild.getNodeId())) {
                        toReorder = e;
                    } else if (toReorder != null) {
                        isLast = false;
                    }
                }
                if (toReorder != null && !isLast) {
                    list.remove(toReorder);
                    list.add(toReorder);
                    state.getState().setChildNodeEntries(list);
                }
            }
        }
    }
}