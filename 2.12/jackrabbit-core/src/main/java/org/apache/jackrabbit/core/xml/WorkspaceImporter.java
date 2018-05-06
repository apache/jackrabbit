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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.BatchedItemOperations;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.config.ImportConfig;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.version.InternalVersionManager;
import org.apache.jackrabbit.core.version.VersionHistoryInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * <code>WorkspaceImporter</code> ...
 */
public class WorkspaceImporter implements Importer {

    private static Logger log = LoggerFactory.getLogger(WorkspaceImporter.class);

    private final NodeState importTarget;
    private final WorkspaceImpl wsp;
    private final SessionImpl session;
    private final InternalVersionManager versionManager;
    private final HierarchyManager hierMgr;
    private final BatchedItemOperations itemOps;

    private final int uuidBehavior;

    private boolean aborted;
    private final Stack<NodeState> parents;

    /**
     * helper object that keeps track of remapped uuid's and imported reference
     * properties that might need correcting depending on the uuid mappings
     */
    private final ReferenceChangeTracker refTracker;

    /**
     * Creates a new <code>WorkspaceImporter</code> instance.
     *
     * @param parentPath   target path where to add the imported subtree
     * @param wsp the workspace to operate on
     * @param sessionContext the session context
     * @param uuidBehavior flag that governs how incoming UUIDs are handled
     * @throws PathNotFoundException        if no node exists at
     *                                      <code>parentPath</code> or if the
     *                                      current session is not granted read
     *                                      access.
     * @throws ConstraintViolationException if the node at
     *                                      <code>parentPath</code> is protected
     * @throws VersionException             if the node at
     *                                      <code>parentPath</code> is not
     *                                      checked-out
     * @throws LockException                if a lock prevents the addition of
     *                                      the subtree
     * @throws RepositoryException          if another error occurs
     */
    public WorkspaceImporter(Path parentPath,
                             WorkspaceImpl wsp,
                             SessionContext sessionContext,
                             int uuidBehavior)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        this(parentPath, wsp, sessionContext, uuidBehavior, null);
    }

    /**
     * Creates a new <code>WorkspaceImporter</code> instance.
     *
     * @param parentPath   target path where to add the imported subtree
     * @param wsp the workspace to operate on
     * @param sessionContext the session context
     * @param uuidBehavior flag that governs how incoming UUIDs are handled
     * @param config import configuration.
     * @throws PathNotFoundException        if no node exists at
     *                                      <code>parentPath</code> or if the
     *                                      current session is not granted read
     *                                      access.
     * @throws ConstraintViolationException if the node at
     *                                      <code>parentPath</code> is protected
     * @throws VersionException             if the node at
     *                                      <code>parentPath</code> is not
     *                                      checked-out
     * @throws LockException                if a lock prevents the addition of
     *                                      the subtree
     * @throws RepositoryException          if another error occurs
     */
    public WorkspaceImporter(
            Path parentPath, WorkspaceImpl wsp, SessionContext sessionContext,
            int uuidBehavior, ImportConfig config)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        this.wsp = wsp;
        this.session = sessionContext.getSessionImpl();
        this.versionManager = session.getInternalVersionManager();
        this.uuidBehavior = uuidBehavior;

        itemOps = new BatchedItemOperations(
                wsp.getItemStateManager(), sessionContext);
        hierMgr = wsp.getHierarchyManager();

        // perform preliminary checks
        itemOps.verifyCanWrite(parentPath);
        importTarget = itemOps.getNodeState(parentPath);

        aborted = false;

        refTracker = new ReferenceChangeTracker();

        parents = new Stack<NodeState>();
        parents.push(importTarget);

        // TODO: TOBEFIXED importer doesn't yet pass protected items to the configured importers.
        // for the time being log an exception if an importer is configured that
        // is expected to work with workspace import. see JCR-2521
        if (config != null) {
            List<? extends ProtectedItemImporter> pi = config.getProtectedItemImporters();
            for (ProtectedItemImporter ppi : pi) {
                if (ppi.init(session, session, true, uuidBehavior, refTracker)) {
                    log.warn("Protected item importer configured is not supported by workspace import.");
                    //throw new UnsupportedOperationException("Workspace-Import of protected nodes: Not yet implement. ");
                }
            }
        }
    }

    /**
     * @param parent parent node state
     * @param conflicting conflicting node state
     * @param nodeInfo the node info
     * @return the resolved node state
     * @throws RepositoryException if an error occurs
     */
    protected NodeState resolveUUIDConflict(NodeState parent,
                                            NodeState conflicting,
                                            NodeInfo nodeInfo)
            throws RepositoryException {

        NodeState node;
        if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW) {
            // create new with new uuid:
            // check if new node can be added (check access rights &
            // node type constraints only, assume locking & versioning status
            // and retention/hold has already been checked on ancestor)
            itemOps.checkAddNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(),
                    BatchedItemOperations.CHECK_ACCESS
                    | BatchedItemOperations.CHECK_CONSTRAINTS);
            node = itemOps.createNodeState(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
            // remember uuid mapping
            EffectiveNodeType ent = itemOps.getEffectiveNodeType(node);
            if (ent.includesNodeType(NameConstants.MIX_REFERENCEABLE)) {
                refTracker.mappedId(nodeInfo.getId(), node.getNodeId());
            }
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW) {
            // if existing node is shareable, then instead of failing, create
            // new node and share with existing
            if (conflicting.isShareable()) {
                itemOps.clone(conflicting, parent, nodeInfo.getName());
                return null;
            }
            String msg = "a node with uuid " + nodeInfo.getId()
                    + " already exists!";
            log.debug(msg);
            throw new ItemExistsException(msg);
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING) {
            // make sure conflicting node is not importTarget or an ancestor thereof
            Path p0 = hierMgr.getPath(importTarget.getNodeId());
            Path p1 = hierMgr.getPath(conflicting.getNodeId());
            try {
                if (p1.equals(p0) || p1.isAncestorOf(p0)) {
                    String msg = "cannot remove ancestor node";
                    log.debug(msg);
                    throw new ConstraintViolationException(msg);
                }
            } catch (MalformedPathException mpe) {
                // should never get here...
                String msg = "internal error: failed to determine degree of relationship";
                log.error(msg, mpe);
                throw new RepositoryException(msg, mpe);
            }
            // remove conflicting:
            // check if conflicting can be removed
            // (access rights, node type constraints, locking & versioning status)
            itemOps.checkRemoveNode(conflicting,
                    BatchedItemOperations.CHECK_ACCESS
                    | BatchedItemOperations.CHECK_LOCK
                    | BatchedItemOperations.CHECK_CHECKED_OUT
                    | BatchedItemOperations.CHECK_CONSTRAINTS
                    | BatchedItemOperations.CHECK_HOLD
                    | BatchedItemOperations.CHECK_RETENTION);
            // do remove conflicting (recursive)
            itemOps.removeNodeState(conflicting);

            // create new with given uuid:
            // check if new node can be added (check access rights &
            // node type constraints only, assume locking & versioning status
            // and retention/hold has already been checked on ancestor)
            itemOps.checkAddNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(),
                    BatchedItemOperations.CHECK_ACCESS
                    | BatchedItemOperations.CHECK_CONSTRAINTS);
            // do create new node
            node = itemOps.createNodeState(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getId());
        } else if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
            NodeId parentId = conflicting.getParentId();
            if (parentId == null) {
                String msg = "root node cannot be replaced";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            // 'replace' current parent with parent of conflicting
            try {
                parent = itemOps.getNodeState(parentId);
            } catch (ItemNotFoundException infe) {
                // should never get here...
                String msg = "internal error: failed to retrieve parent state";
                log.error(msg, infe);
                throw new RepositoryException(msg, infe);
            }
            // remove conflicting:
            // check if conflicting can be removed
            // (access rights, node type constraints, locking & versioning status)
            itemOps.checkRemoveNode(conflicting,
                    BatchedItemOperations.CHECK_ACCESS
                    | BatchedItemOperations.CHECK_LOCK
                    | BatchedItemOperations.CHECK_CHECKED_OUT
                    | BatchedItemOperations.CHECK_CONSTRAINTS
                    | BatchedItemOperations.CHECK_HOLD
                    | BatchedItemOperations.CHECK_RETENTION);

            // 'replace' is actually a 'remove existing/add new' operation;
            // this unfortunately changes the order of the parent's
            // child node entries (JCR-1055);
            // => backup list of child node entries beforehand in order
            // to restore it afterwards
            ChildNodeEntry cneConflicting = parent.getChildNodeEntry(nodeInfo.getId());
            List<ChildNodeEntry> cneList = new ArrayList<ChildNodeEntry>(parent.getChildNodeEntries());
            // do remove conflicting (recursive)
            itemOps.removeNodeState(conflicting);
            // create new with given uuid at same location as conflicting:
            // check if new node can be added at other location
            // (access rights, node type constraints, locking & versioning
            // status and retention/hold)
            itemOps.checkAddNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(),
                    BatchedItemOperations.CHECK_ACCESS
                    | BatchedItemOperations.CHECK_LOCK
                    | BatchedItemOperations.CHECK_CHECKED_OUT
                    | BatchedItemOperations.CHECK_CONSTRAINTS
                    | BatchedItemOperations.CHECK_HOLD
                    | BatchedItemOperations.CHECK_RETENTION);
            // do create new node
            node = itemOps.createNodeState(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getId());
            // restore list of child node entries (JCR-1055)
            if (cneConflicting.getName().equals(nodeInfo.getName())) {
                // restore original child node list
                parent.setChildNodeEntries(cneList);
            } else {
                // replace child node entry with different name
                // but preserving original position
                parent.removeAllChildNodeEntries();
                for (ChildNodeEntry cne : cneList) {
                    if (cne.getId().equals(nodeInfo.getId())) {
                        // replace entry with different name
                        parent.addChildNodeEntry(nodeInfo.getName(), nodeInfo.getId());
                    } else {
                        parent.addChildNodeEntry(cne.getName(), cne.getId());
                    }
                }
            }
        } else {
            String msg = "unknown uuidBehavior: " + uuidBehavior;
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        return node;
    }

    /**
     * Post-process imported node (initialize properties with special
     * semantics etc.)
     *
     * @param node the node state
     * @throws RepositoryException if an error occurs
     */
    protected void postProcessNode(NodeState node) throws RepositoryException {
        /**
         * special handling required for properties with special semantics
         * (e.g. those defined by mix:referenceable, mix:versionable,
         * mix:lockable, et.al.)
         *
         * todo FIXME delegate to 'node type instance handler'
         */
        EffectiveNodeType ent = itemOps.getEffectiveNodeType(node);
        if (ent.includesNodeType(NameConstants.MIX_SIMPLE_VERSIONABLE)) {
            /**
             * check if there's already a version history for that
             * node; this would e.g. be the case if a versionable node
             * had been exported, removed and re-imported with either
             * IMPORT_UUID_COLLISION_REMOVE_EXISTING or
             * IMPORT_UUID_COLLISION_REPLACE_EXISTING;
             * otherwise create a new version history
             */
            VersionHistoryInfo history =
                versionManager.getVersionHistory(session, node, null);
            InternalValue historyId = InternalValue.create(
                    history.getVersionHistoryId());
            InternalValue versionId = InternalValue.create(
                    history.getRootVersionId());

            // jcr:isCheckedOut
            conditionalAddProperty(
                    node, NameConstants.JCR_ISCHECKEDOUT,
                    PropertyType.BOOLEAN, false, InternalValue.create(true));

            // set extra properties only for full versionable nodes
            if (ent.includesNodeType(NameConstants.MIX_VERSIONABLE)) {
                // jcr:versionHistory
                conditionalAddProperty(
                        node, NameConstants.JCR_VERSIONHISTORY,
                        PropertyType.REFERENCE, false, historyId);

                // jcr:baseVersion
                conditionalAddProperty(
                        node, NameConstants.JCR_BASEVERSION,
                        PropertyType.REFERENCE, false, versionId);

                // jcr:predecessors
                conditionalAddProperty(
                        node, NameConstants.JCR_PREDECESSORS,
                        PropertyType.REFERENCE, true, versionId);
            }
        }
    }

    protected void processProperty(NodeState node, PropInfo pInfo) throws RepositoryException {
        PropertyState prop;
        QPropertyDefinition def;

        Name name = pInfo.getName();
        int type = pInfo.getType();

        if (node.hasPropertyName(name)) {
            // a property with that name already exists...
            PropertyId idExisting = new PropertyId(node.getNodeId(), name);
            prop = (PropertyState) itemOps.getItemState(idExisting);
            def = itemOps.findApplicablePropertyDefinition(prop.getName(), prop.getType(), prop.isMultiValued(), node);
            if (def.isProtected()) {
                // skip protected property
                log.debug("skipping protected property "
                        + itemOps.safeGetJCRPath(idExisting));
                return;
            }
            if (!def.isAutoCreated()
                    || (prop.getType() != type && type != PropertyType.UNDEFINED)
                    || def.isMultiple() != prop.isMultiValued()) {
                throw new ItemExistsException(itemOps.safeGetJCRPath(prop.getPropertyId()));
            }
        } else {
            // there's no property with that name,
            // find applicable definition
            def = pInfo.getApplicablePropertyDef(itemOps.getEffectiveNodeType(node));
            if (def.isProtected()) {
                // skip protected property
                log.debug("skipping protected property " + name);
                return;
            }

            // create new property
            prop = itemOps.createPropertyState(node, name, type, def);
        }

        // check multi-valued characteristic
        TextValue[] values = pInfo.getTextValues();
        if (values.length != 1 && !def.isMultiple()) {
            throw new ConstraintViolationException(itemOps.safeGetJCRPath(prop.getPropertyId())
                    + " is not multi-valued");
        }

        // convert serialized values to InternalValue objects
        int targetType = pInfo.getTargetType(def);
        InternalValue[] iva = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            iva[i] = values[i].getInternalValue(targetType);
        }

        // set values
        prop.setValues(iva);

        // make sure property is valid according to its definition
        itemOps.validate(prop);

        if (prop.getType() == PropertyType.REFERENCE
                || prop.getType() == PropertyType.WEAKREFERENCE) {
            // store reference for later resolution
            refTracker.processedReference(prop);
        }

        // store property
        itemOps.store(prop);
    }

    /**
     * Adds the the given property to a node unless the property already
     * exists.
     *
     * @param node the node to which the property is added
     * @param name name of the property
     * @param type property type (see {@link PropertyType})
     * @param multiple whether the property is multivalued
     * @param value initial value of the property, if it needs to be added
     * @throws RepositoryException if the property could not be added
     */
    private void conditionalAddProperty(
            NodeState node, Name name, int type, boolean multiple,
            InternalValue value)
            throws RepositoryException {
        if (!node.hasPropertyName(name)) {
            QPropertyDefinition def = itemOps.findApplicablePropertyDefinition(
                    name, type, multiple, node);
            PropertyState prop = itemOps.createPropertyState(
                    node, name, type, def);
            prop.setValues(new InternalValue[] { value });
        }
    }

    //-------------------------------------------------------------< Importer >
    /**
     * {@inheritDoc}
     */
    public void start() throws RepositoryException {
        try {
            // start update operation
            itemOps.edit();
        } catch (IllegalStateException ise) {
            aborted = true;
            String msg = "internal error: failed to start update operation";
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startNode(NodeInfo nodeInfo, List<PropInfo> propInfos)
            throws RepositoryException {
        if (aborted) {
            // the import has been aborted, get outta here...
            return;
        }

        boolean succeeded = false;
        NodeState parent;
        try {
            // check sanity of workspace/session first
            wsp.sanityCheck();

            parent = parents.peek();

            // process node

            NodeState node = null;
            NodeId id = nodeInfo.getId();
            Name nodeName = nodeInfo.getName();
            Name ntName = nodeInfo.getNodeTypeName();
            Name[] mixins = nodeInfo.getMixinNames();

            if (parent == null) {
                // parent node was skipped, skip this child node too
                parents.push(null); // push null onto stack for skipped node
                succeeded = true;
                log.debug("skipping node " + nodeName);
                return;
            }
            if (parent.hasChildNodeEntry(nodeName)) {
                // a node with that name already exists...
                ChildNodeEntry entry =
                        parent.getChildNodeEntry(nodeName, 1);
                NodeId idExisting = entry.getId();
                NodeState existing = (NodeState) itemOps.getItemState(idExisting);
                QNodeDefinition def = itemOps.findApplicableNodeDefinition(
                        nodeName, existing.getNodeTypeName(), parent);

                if (!def.allowsSameNameSiblings()) {
                    // existing doesn't allow same-name siblings,
                    // check for potential conflicts
                    EffectiveNodeType entExisting =
                            itemOps.getEffectiveNodeType(existing);
                    if (def.isProtected() && entExisting.includesNodeType(ntName)) {
                        // skip protected node
                        parents.push(null); // push null onto stack for skipped node
                        succeeded = true;
                        log.debug("skipping protected node "
                                + itemOps.safeGetJCRPath(existing.getNodeId()));
                        return;
                    }
                    if (def.isAutoCreated() && entExisting.includesNodeType(ntName)) {
                        // this node has already been auto-created,
                        // no need to create it
                        node = existing;
                    } else {
                        // edge case: colliding node does have same uuid
                        // (see http://issues.apache.org/jira/browse/JCR-1128)
                        if (!(idExisting.equals(id)
                                && (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING
                                || uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING))) {
                            throw new ItemExistsException(itemOps.safeGetJCRPath(existing.getNodeId()));
                        }
                        // fall through
                    }
                }
            }

            if (node == null) {
                // there's no node with that name...
                if (id == null) {
                    // no potential uuid conflict, always create new node
                    QNodeDefinition def = itemOps.findApplicableNodeDefinition(
                            nodeName, ntName, parent);
                    if (def.isProtected()) {
                        // skip protected node
                        parents.push(null); // push null onto stack for skipped node
                        succeeded = true;
                        log.debug("skipping protected node " + nodeName);
                        return;
                    }

                    // check if new node can be added (check access rights &
                    // node type constraints only, assume locking & versioning status
                    // and retention/hold has already been checked on ancestor)
                    itemOps.checkAddNode(parent, nodeName, ntName,
                            BatchedItemOperations.CHECK_ACCESS
                            | BatchedItemOperations.CHECK_CONSTRAINTS);
                    // do create new node
                    node = itemOps.createNodeState(parent, nodeName, ntName, mixins, null, def);
                } else {
                    // potential uuid conflict
                    try {
                        NodeState conflicting = itemOps.getNodeState(id);
                        // resolve uuid conflict
                        node = resolveUUIDConflict(parent, conflicting, nodeInfo);
                        if (node == null) {
                            // no new node has been created, so skip this node
                            parents.push(null); // push null onto stack for skipped node
                            succeeded = true;
                            log.debug("skipping existing node: " + nodeName);
                            return;
                        }
                    } catch (ItemNotFoundException e) {
                        // create new with given uuid
                        QNodeDefinition def = itemOps.findApplicableNodeDefinition(
                                nodeName, ntName, parent);
                        if (def.isProtected()) {
                            // skip protected node
                            parents.push(null); // push null onto stack for skipped node
                            succeeded = true;
                            log.debug("skipping protected node " + nodeName);
                            return;
                        }

                        // check if new node can be added (check access rights &
                        // node type constraints only, assume locking & versioning status
                        // and retention/hold has already been checked on ancestor)
                        itemOps.checkAddNode(parent, nodeName, ntName,
                                BatchedItemOperations.CHECK_ACCESS
                                | BatchedItemOperations.CHECK_CONSTRAINTS);
                        // do create new node
                        node = itemOps.createNodeState(parent, nodeName, ntName, mixins, id, def);
                    }
                }
            }

            // process properties
            for (PropInfo propInfo : propInfos) {
                processProperty(node, propInfo);
            }

            // store affected nodes
            itemOps.store(node);
            itemOps.store(parent);

            // push current node onto stack of parents
            parents.push(node);

            succeeded = true;
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                aborted = true;
                itemOps.cancel();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void endNode(NodeInfo nodeInfo) throws RepositoryException {
        if (aborted) {
            // the import has been aborted, get outta here...
            return;
        }
        NodeState node = parents.pop();
        if (node == null) {
            // node was skipped, nothing to do here
            return;
        }
        boolean succeeded = false;
        try {
            // check sanity of workspace/session first
            wsp.sanityCheck();

            // post-process node (initialize properties with special semantics etc.)
            postProcessNode(node);

            // make sure node is valid according to its definition
            itemOps.validate(node);

            // we're done with that node, now store its state
            itemOps.store(node);
            succeeded = true;
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                aborted = true;
                itemOps.cancel();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void end() throws RepositoryException {
        if (aborted) {
            // the import has been aborted, get outta here...
            return;
        }

        boolean succeeded = false;
        try {
            // check sanity of workspace/session first
            wsp.sanityCheck();

            /**
             * adjust references that refer to uuids which have been mapped to
             * newly generated uuids on import
             */
            Iterator<Object> iter = refTracker.getProcessedReferences();
            while (iter.hasNext()) {
                PropertyState prop = (PropertyState) iter.next();
                // being paranoid...
                if (prop.getType() != PropertyType.REFERENCE
                    && prop.getType() != PropertyType.WEAKREFERENCE) {
                    continue;
                }
                boolean modified = false;
                InternalValue[] values = prop.getValues();
                InternalValue[] newVals = new InternalValue[values.length];
                for (int i = 0; i < values.length; i++) {
                    NodeId adjusted =
                        refTracker.getMappedId(values[i].getNodeId());
                    if (adjusted != null) {
                        newVals[i] = InternalValue.create(
                                adjusted,
                                prop.getType() != PropertyType.REFERENCE);
                        modified = true;
                    } else {
                        // reference doesn't need adjusting, just copy old value
                        newVals[i] = values[i];
                    }
                }
                if (modified) {
                    prop.setValues(newVals);
                    itemOps.store(prop);
                }
            }
            refTracker.clear();

            // make sure import target is valid according to its definition
            itemOps.validate(importTarget);

            // finally store the state of the import target
            // (the parent of the imported subtree)
            itemOps.store(importTarget);
            succeeded = true;
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                aborted = true;
                itemOps.cancel();
            }
        }

        if (!aborted) {
            // finish update
            itemOps.update();
        }
    }

}
