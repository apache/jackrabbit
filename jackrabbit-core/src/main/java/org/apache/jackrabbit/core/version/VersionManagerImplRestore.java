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

import java.util.*;

import javax.jcr.ItemExistsException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemValidator;
import org.apache.jackrabbit.core.NodeTypeInstanceHandler;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JCR Version Manager implementation is split in several classes in order to
 * group related methods together.
 * <p>
 * this class provides methods for the restore operations.
 * <p>
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
     *
     * @param context component context of the current session
     * @param stateMgr the underlying state manager
     * @param hierMgr local hierarchy manager
     */
    protected VersionManagerImplRestore(
            SessionContext context, UpdatableItemStateManager stateMgr,
            HierarchyManager hierMgr) {
        super(context, stateMgr, hierMgr);
    }

    /**
     * @param state the state to restore
     * @param v the version to restore
     * @param removeExisting remove existing flag
     * @throws RepositoryException if an error occurs
     *
     * @see javax.jcr.version.VersionManager#restore(String, Version, boolean)
     */
    protected void restore(NodeStateEx state, InternalVersion v, boolean removeExisting)
            throws RepositoryException {
        checkVersionable(state);

        // check if 'own' version
        if (!v.getVersionHistory().equals(getVersionHistory(state))) {
            String msg = "Unable to restore version. Not same version history.";
            log.error(msg);
            throw new VersionException(msg);
        }
        WriteOperation ops = startWriteOperation();
        try {
            internalRestore(state, v, new DateVersionSelector(v.getCreated()), removeExisting);
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
     * @param v version to restore
     * @param removeExisting remove exiting flag
     * @throws RepositoryException if an error occurs
     */
    protected void restore(NodeStateEx parent, Name name, InternalVersion v,
                           boolean removeExisting)
            throws RepositoryException {
        // check if versionable node exists
        InternalFrozenNode fn = v.getFrozenNode();
        if (stateMgr.hasItemState(fn.getFrozenId())) {
            if (removeExisting) {
                NodeStateEx existing = parent.getNode(fn.getFrozenId());
                checkVersionable(existing);

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
            WriteOperation ops = startWriteOperation();
            try {
                // create new node below parent
                NodeStateEx state = parent.addNode(name, fn.getFrozenPrimaryType(), fn.getFrozenId());
                state.setMixins(fn.getFrozenMixinTypes());
                internalRestore(state, v, new DateVersionSelector(v.getCreated()), removeExisting);
                parent.store();
                ops.save();
            } catch (ItemStateException e) {
                throw new RepositoryException(e);
            } finally {
                ops.close();
            }
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
        internalRestoreFrozen(state, version.getFrozenNode(), vsel, restored, removeExisting, false);
        restored.add(version);

        if (isFull) {
            // 2. N's jcr:baseVersion property will be changed to point to V.
            state.setPropertyValue(
                    NameConstants.JCR_BASEVERSION, InternalValue.create(version.getId()));

            // 4. N's jcr:predecessor property is set to null
            state.setPropertyValues(NameConstants.JCR_PREDECESSORS, PropertyType.REFERENCE, InternalValue.EMPTY_ARRAY);

            // set version history
            state.setPropertyValue(NameConstants.JCR_VERSIONHISTORY, InternalValue.create(version.getVersionHistory().getId()));

            // also clear mergeFailed
            state.removeProperty(NameConstants.JCR_MERGEFAILED);

        } else {
            // with simple versioning, the node is checked in automatically,
            // thus not allowing any branches
            vMgr.checkin(session, state, null);
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
     * @param copy if <code>true</code> a pure copy is performed
     * @throws RepositoryException if an error occurs
     * @throws ItemStateException if an error occurs
     */
    protected void internalRestoreFrozen(NodeStateEx state,
                                         InternalFrozenNode freeze,
                                         VersionSelector vsel,
                                         Set<InternalVersion> restored,
                                         boolean removeExisting,
                                         boolean copy)
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

        // For each property P present on F (other than jcr:frozenPrimaryType,
        // jcr:frozenMixinTypes and jcr:frozenUuid): 
        // - If P has an OPV of COPY or VERSION then F/P is copied to N/P,
        //   replacing any existing N/P.
        // - F will never have a property with an OPV of IGNORE, INITIALIZE, COMPUTE
        //   or ABORT (see 15.2 Check-In: Creating a Version).
        Set<Name> propNames = new HashSet<Name>();
        PropertyState[] props = freeze.getFrozenProperties();
        for (PropertyState prop : props) {
            // don't restore jcr:activity
            Name name = prop.getName();
            if (!name.equals(NameConstants.JCR_ACTIVITY)) {
                state.copyFrom(prop);
                propNames.add(name);
            }
        }

        // For each property P present on N but not on F:
        // - If P has an OPV of COPY, VERSION or ABORT then N/P is removed. Note that
        //   while a node with a child item of OPV ABORT cannot be versioned, it is
        //   legal for a previously versioned node to have such a child item added to it
        //   and then for it to be restored to the state that it had before that item was
        //   added, as this step indicates.
        // - If P has an OPV of IGNORE then no change is made to N/P.
        // - If P has an OPV of INITIALIZE then, if N/P has a default value (either
        //   defined in the node type of N or implementation-defined) its value is
        //   changed to that default value. If N/P has no default value then it is left
        //   unchanged.
        // - If P has an OPV of COMPUTE then the value of N/P may be changed
        //   according to an implementation-specific mechanism.

        // remove properties that do not exist in the frozen representation
        for (PropertyState prop: state.getProperties()) {
            Name propName = prop.getName();
            if (!propNames.contains(propName)) {
                int opv;
                try {
                    opv = state.getDefinition(prop).getOnParentVersion();
                } catch (ConstraintViolationException ignore) {
                    // N/P definition no longer exists (like from a mixin on N but not on F, already removed above)
                    opv = OnParentVersionAction.ABORT;
                }
                if (opv == OnParentVersionAction.COPY
                        || opv == OnParentVersionAction.VERSION
                        || opv == OnParentVersionAction.ABORT) {
                    state.removeProperty(propName);
                } else if (opv == OnParentVersionAction.INITIALIZE) {
                    InternalValue[] values = computeAutoValues(state, state.getDefinition(prop), true);
                    if (values != null) {
                        state.setPropertyValues(propName, prop.getType(), values, prop.isMultiValued());
                    }
                } else if (opv == OnParentVersionAction.COMPUTE) {
                    InternalValue[] values = computeAutoValues(state, state.getDefinition(prop), false);
                    if (values != null) {
                        state.setPropertyValues(propName, prop.getType(), values, prop.isMultiValued());
                    }
                }
            }
        }

        // add 'auto-create' properties that do not exist yet
        for (QPropertyDefinition def: state.getEffectiveNodeType().getAutoCreatePropDefs()) {
            if (!state.hasProperty(def.getName())) {
                InternalValue[] values = computeAutoValues(state, def, true);
                if (values != null) {
                    state.setPropertyValues(def.getName(), def.getRequiredType(), values, def.isMultiple());
                }
            }
        }

        // For each child node C present on N but not on F:
        // - If C has an OPV of COPY, VERSION or ABORT then N/C is removed.
        //   Note that while a node with a child item of OPV ABORT cannot be
        //   versioned, it is legal for a previously versioned node to have such
        //   a child item added to it and then for it to be restored to the state
        //   that it had before that item was added, as this step indicates.
        // - If C has an OPV of IGNORE then no change is made to N/C.
        // - If C has an OPV of INITIALIZE then N/C is re-initialized as if it
        //   were newly created, as defined in its node type.
        // - If C has an OPV of COMPUTE then N/C may be re-initialized according
        //   to an implementation-specific mechanism.
        LinkedList<ChildNodeEntry> toDelete = new LinkedList<ChildNodeEntry>();
        for (ChildNodeEntry entry: state.getState().getChildNodeEntries()) {
            if (!freeze.hasFrozenChildNode(entry.getName(), entry.getIndex())) {
                NodeStateEx child = state.getNode(entry.getName(), entry.getIndex());
                int opv = child.getDefinition().getOnParentVersion();
                if (copy || opv == OnParentVersionAction.COPY
                        || opv == OnParentVersionAction.VERSION
                        || opv == OnParentVersionAction.ABORT) {
                    toDelete.addFirst(entry);
                } else if (opv == OnParentVersionAction.INITIALIZE) {
                    log.warn("OPV.INITIALIZE not supported yet on restore of existing child nodes: " + safeGetJCRPath(child));
                } else if (opv == OnParentVersionAction.COMPUTE) {
                    log.warn("OPV.COMPUTE not supported yet on restore of existing child nodes: " + safeGetJCRPath(child));
                }
            }
        }
        for (ChildNodeEntry entry: toDelete) {
            state.removeNode(entry.getName(), entry.getIndex());
        }
        // need to sync with state manager
        state.store();

        // create a map that contains a int->NodeStateEx mapping for each child name
        Map<Name, Map<Integer, NodeStateEx>> entryToNodeStateExMapping = new HashMap<Name, Map<Integer, NodeStateEx>>();
        for (ChildNodeEntry entry : state.getState().getChildNodeEntries()) {
            Map<Integer, NodeStateEx> id2stateMap = entryToNodeStateExMapping
                    .get(entry.getName());
            if (id2stateMap == null) {
                id2stateMap = new HashMap<Integer, NodeStateEx>();
            }
            id2stateMap.put(entry.getIndex(),
                    state.getNode(entry.getName(), entry.getIndex()));
            entryToNodeStateExMapping.put(entry.getName(), id2stateMap);
        }

        // For each child node C present on F:
        // - F will never have a child node with an OPV of IGNORE, INITIALIZE,
        //   COMPUTE or ABORT (see 15.2 Check-In: Creating a Version).
        for (ChildNodeEntry entry : freeze.getFrozenChildNodes()) {
            InternalFreeze child = freeze.getFrozenChildNode(entry.getName(), entry.getIndex());
            NodeStateEx restoredChild = null;
            if (child instanceof InternalFrozenNode) {
                // - If C has an OPV of COPY or VERSION:
                //   - B is true, then F/C and its subgraph is copied to N/C, replacing
                //     any existing N/C and its subgraph and any node in the workspace
                //     with the same identifier as C or a node in the subgraph of C is
                //     removed.
                //   - B is false, then F/C and its subgraph is copied to N/C, replacing
                //     any existing N/C and its subgraph unless there exists a node in the
                //     workspace with the same identifier as C, or a node in the subgraph
                //     of C, in which case an ItemExistsException is thrown , all
                //     changes made by the restore are rolled back leaving N unchanged.

                InternalFrozenNode f = (InternalFrozenNode) child;

                // if node is present, remove it
                Map<Integer, NodeStateEx> id2stateMap = entryToNodeStateExMapping
                        .get(entry.getName());
                if (id2stateMap != null
                        && id2stateMap.containsKey(entry.getIndex())) {
                    state.removeNode(id2stateMap.get(entry.getIndex()));
                }

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
                        } else if (!existing.hasAncestor(state.getNodeId())){
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
                }
                internalRestoreFrozen(restoredChild, f, vsel, restored, removeExisting, true);

            } else if (child instanceof InternalFrozenVersionHistory) {
                //   Each child node C of N where C has an OPV of VERSION and C is
                //   mix:versionable, is represented in F not as a copy of N/C but as
                //   special node containing a reference to the version history of
                //   C. On restore, the following occurs:
                //   - If the workspace currently has an already existing node corresponding
                //     to C's version history and the removeExisting flag of the restore is
                //     set to true, then that instance of C becomes the child of the restored N.
                //   - If the workspace currently has an already existing node corresponding
                //     to C's version history and the removeExisting flag of the restore is
                //     set to false then an ItemExistsException is thrown.
                //   - If the workspace does not have an instance of C then one is restored from
                //     C's version history:
                //     - If the restore was initiated through a restoreByLabel where L is
                //       the specified label and there is a version of C with the label L then
                //       that version is restored.
                //     - If the version history of C does not contain a version with the label
                //       L or the restore was initiated by a method call that does not specify
                //       a label then the workspace in which the restore is being performed
                //       will determine which particular version of C will be restored. This
                //       determination depends on the configuration of the workspace and
                //       is outside the scope of this specification.
                InternalFrozenVersionHistory fh = (InternalFrozenVersionHistory) child;
                InternalVersionHistory vh = vMgr.getVersionHistory(fh.getVersionHistoryId());
                // get desired version from version selector
                InternalVersion v = vsel.select(vh);
                Name oldVersion = null;

                // check if representing versionable already exists somewhere
                NodeId nodeId = vh.getVersionableId();
                if (stateMgr.hasItemState(nodeId)) {
                    restoredChild = state.getNode(nodeId);
                    if (restoredChild.getParentId().equals(state.getNodeId())) {
                        // if same parent, ignore
                    } else if (removeExisting) {
                        NodeStateEx parent = restoredChild.getNode(restoredChild.getParentId());
                        state.moveFrom(restoredChild, fh.getName(), false);
                        parent.store();

                        // get old version name
                        oldVersion = getBaseVersion(restoredChild).getName();
                    } else {
                        // since we delete the OPV=Copy children beforehand, all
                        // found nodes must be outside of this tree
                        String msg = "Unable to restore node, item already exists " +
                                "outside of restored tree: " + safeGetJCRPath(restoredChild);
                        log.error(msg);
                        throw new ItemExistsException(msg);
                    }
                }

                // check existing version of item exists
                if (restoredChild == null) {
                    if (v == null) {
                        // if version selector was unable to select version,
                        // choose the initial one
                        List<InternalVersion> vs = vh.getRootVersion().getSuccessors();
                        if (vs.isEmpty()) {
                            String msg = "Unable to select appropriate version for "
                                    + child.getName() + " using " + vsel;
                            log.error(msg);
                            throw new VersionException(msg);
                        }
                        v = vs.get(0);
                    }
                    InternalFrozenNode f = v.getFrozenNode();
                    restoredChild = state.addNode(fh.getName(), f.getFrozenPrimaryType(), f.getFrozenId());
                    restoredChild.setMixins(f.getFrozenMixinTypes());
                } else {
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
                        StringBuilder avail = new StringBuilder();
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
            if (restoredChild != null && state.getEffectiveNodeType().hasOrderableChildNodes()) {
                //   In a repository that supports orderable child nodes, the relative
                //   ordering of the set of child nodes C that are copied from F is
                //   preserved.

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

    /**
     * Computes the auto generated values and falls back to the default values
     * specified in the property definition
     * @param state parent state
     * @param def property definition
     * @param useDefaultValues if <code>true</code> the default values are respected
     * @return the values or <code>null</code>
     * @throws RepositoryException if the values cannot be computed.
     */
    private InternalValue[] computeAutoValues(NodeStateEx state, QPropertyDefinition def,
                                              boolean useDefaultValues)
            throws RepositoryException {
        // compute system generated values if necessary
        InternalValue[] values =
            new NodeTypeInstanceHandler(session.getUserID()).
            computeSystemGeneratedPropertyValues(state.getState(), def);
        if (values == null && useDefaultValues) {
            values = InternalValue.create(def.getDefaultValues());
        }
        // avoid empty value array for single value property
        if (values != null && values.length == 0 && !def.isMultiple()) {
            values = null;
        }
        return values;
    }
}
