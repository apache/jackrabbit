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

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.core.BatchedItemOperations;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.uuid.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorkspaceImporter. It imports the content submitted to it
 * by the Content Handler
 *
 */
public class WorkspaceImporter implements Importer {

    private static Logger log = LoggerFactory.getLogger(WorkspaceImporter.class);
    private final NodeState importTarget;
    private final NodeTypeRegistry ntReg;
    private final HierarchyManager hierMgr;
    private final BatchedItemOperations itemOps;
    private final int uuidBehavior;

    //It is not useful anymore: we never abort: we raise an exception.
    // I suggest to delete it. Do you see any issue with this?
    private boolean aborted = false;
    private Stack parents;

    /**
     * helper object that keeps track of remapped uuid's and imported reference
     * properties that might need correcting depending on the uuid mappings
     */
    private final ReferenceChangeTracker refTracker;

    // Unused for now. It will be used in the next iteration on JIRA
    private boolean raw = false;

    /**
     * True if we skip the tree with current node as root
     */
    private boolean skip = false;

    /**
     * Used to find when stopping skipping
     */
    private NodeInfo skipNode;

    /**
     * True if this node already exist
     */
    private NodeState existing = null;
    private WorkspaceImpl wsp;

    /**
     * Creates a new <code>sWorkspaceImporter</code> instance.
     *
     * @param parentPath   target path where to add the imported subtree
     * @param wsp the workspace we want to import content to
     * @param ntReg the NodeTypeRegistry of the repository
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
            NodeTypeRegistry ntReg,
            int uuidBehavior)
    throws PathNotFoundException, ConstraintViolationException,
    VersionException, LockException, RepositoryException {
        SessionImpl ses = (SessionImpl) wsp.getSession();
        itemOps = new BatchedItemOperations(wsp.getItemStateManager(),
                ntReg, ses.getLockManager(), ses, wsp.getHierarchyManager(),
                ses.getNamespaceResolver());

        this.hierMgr = wsp.getHierarchyManager();
        //Perform preliminary checks
        itemOps.verifyCanWrite(parentPath);
        importTarget = itemOps.getNodeState(parentPath);
        this.wsp = wsp;
        this.ntReg = ntReg;
        this.uuidBehavior = uuidBehavior;
        aborted = false;
        refTracker = new ReferenceChangeTracker();
        parents = new Stack();
        parents.push(importTarget);
    }

    /**
     * Performs some checks to know if the node is importable or not.
     * If it is a serious issue, raises an exception, else return false.
     * this subtree.
     * <br/>
     * Performs also if needed some remapping.
     *
     * @param parent the parent NodeState
     * @param nodeInfo NodeInfo passed by the ContentHandler
     * @param propInfo PropInfo passed by the ContentHandler
     * @return true if the node is fine; else false
     * @throws RepositoryException if some constraints checks are not OK
     * @throws ItemExistsException if the item exist
     * @throws ItemNotFoundException if some constraints checks are not OK (shouldn't happen)
     * @throws LockException if some constraints checks are not OK
     * @throws VersionException if some constraints checks are not OK
     * @throws AccessDeniedException if some constraints checks are not OK
     * @throws ConstraintViolationException if some constraints checks are not OK
     */
    private boolean checkNode(NodeState parent, NodeInfo nodeInfo, List propInfo)
        throws ConstraintViolationException, AccessDeniedException, VersionException,
        LockException, ItemNotFoundException, ItemExistsException, RepositoryException {
        itemOps.checkAddNode(parent, nodeInfo.getName(), nodeInfo.getNodeTypeName(),
                BatchedItemOperations.CHECK_ACCESS
                | BatchedItemOperations.CHECK_CONSTRAINTS
                | BatchedItemOperations.CHECK_LOCK
                | BatchedItemOperations.CHECK_VERSIONING);

        QName nodeName = nodeInfo.getName();
        QName ntName = nodeInfo.getNodeTypeName();

        if (parent.hasChildNodeEntry(nodeName)) {
            // a node with that name already exists...
            //No need to check for more than one, since if it
            //is the case we can import it.
            NodeState.ChildNodeEntry entry =
                parent.getChildNodeEntry(nodeName, 1);
            NodeId idExisting = entry.getId();
            NodeState existing = (NodeState) itemOps.getItemState(idExisting);
            NodeDef def = ntReg.getNodeDef(existing.getDefinitionId());
            if (!def.allowsSameNameSiblings()) {
                // existing doesn't allow same-name siblings,
                // check for potential conflicts
                EffectiveNodeType entExisting =
                    itemOps.getEffectiveNodeType(existing);
                if (!raw && def.isProtected() && entExisting.includesNodeType(ntName)) {
                    return false;
                }

                if (def.isAutoCreated() && entExisting.includesNodeType(ntName)) {
                    // this node has already been auto-created,
                    // no need to create it
                    this.existing = existing;
                } else {
                    throw new ItemExistsException(itemOps.safeGetJCRPath(existing.getNodeId()));
                }
            }
        }

        if (parent.hasPropertyName(nodeName)) {
            /**
             * a property with the same name already exists; if this property
             * has been imported as well (e.g. through document view import
             * where an element can have the same name as one of the attributes
             * of its parent element) we have to rename the onflicting property;
             *
             * see http://issues.apache.org/jira/browse/JCR-61
             */
            PropertyId propId = new PropertyId(parent.getNodeId(), nodeName);
            PropertyState conflicting = itemOps.getPropertyState(propId);
            if (conflicting.getStatus() == ItemState.STATUS_NEW) {
                // assume this property has been imported as well;
                // rename conflicting property
                // @todo use better reversible escaping scheme to create unique name
                QName newName = new QName(nodeName.getNamespaceURI(), nodeName.getLocalName() + "_");
                if (parent.hasPropertyName(newName)) {
                    newName = new QName(newName.getNamespaceURI(), newName.getLocalName() + "_");
                }
                PropertyState newProp =
                    itemOps.createPropertyState(parent, newName,
                            conflicting.getType(), conflicting.getValues().length);
                newProp.setValues(conflicting.getValues());
                parent.removePropertyName(nodeName);
                itemOps.store(parent);
                itemOps.destroy(conflicting);
            }
        }

        return true;
    }

    /**
     * Create propoerties on a specific NodeState
     * @param myNode the NodeState
     * @param propInfos PropInfo
     * @throws ItemNotFoundException if issue in the NodeState
     * @throws ItemExistsException if issue in the NodeState
     * @throws ConstraintViolationException if issue in the NodeState
     * @throws ValueFormatException if issue in the NodeState
     * @throws RepositoryException if issue in the NodeState
     */
    private void createProperties(NodeState myNode, List propInfos)
        throws ItemNotFoundException, ItemExistsException, ConstraintViolationException,
                                                ValueFormatException, RepositoryException {
        // process properties
        Iterator iter = propInfos.iterator();
        while (iter.hasNext()) {
            PropInfo pi = (PropInfo) iter.next();
            pi.apply(myNode, itemOps, ntReg, refTracker);
        }
    }

    /**
     * Create the specific NodeState
     * @param parent NodeState
     * @param nodeInfo NodeInfo
     * @return newly create NodeState
     * @throws ConstraintViolationException if we cannot create the NodeState
     * @throws RepositoryException if we cannot create the NodeState
     */
    private NodeState createNode(NodeState parent, NodeInfo nodeInfo) throws ConstraintViolationException, RepositoryException {

        NodeDef def =
            itemOps.findApplicableNodeDefinition(nodeInfo.getName(), nodeInfo.getNodeTypeName(), parent);

        // potential uuid conflict
        NodeState conflicting = null;
        NodeState node;

        try {
            if (nodeInfo.getId() != null) {
                conflicting = itemOps.getNodeState(nodeInfo.getId());
            }
        } catch (ItemNotFoundException infe) {
            conflicting = null;
        }
        if (conflicting != null) {
            // resolve uuid conflict
            node = resolveUUIDConflict(parent, conflicting, nodeInfo);
        }
        else {
            // do create new node
            node = itemOps.createNodeState(parent, nodeInfo.getName(), nodeInfo.getNodeTypeName(),
                                                                nodeInfo.getMixinNames(), null, def);
        }
        return node;
    }

    /**
     * Resolve UUID conflict if any.
     *
     * @param parent NodeState
     * @param conflicting NodeState
     * @param nodeInfo NodeInfo
     * @return the new conflicting NodeState
     * @throws ItemExistsException
     * @throws ConstraintViolationException
     * @throws IllegalStateException
     * @throws RepositoryException
     */
    private NodeState resolveUUIDConflict(NodeState parent, NodeState conflicting, NodeInfo nodeInfo)
        throws ItemExistsException, ConstraintViolationException, IllegalStateException, RepositoryException {
        NodeState node = null;
        switch (uuidBehavior) {

        case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING:
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
                    | BatchedItemOperations.CHECK_VERSIONING
                    | BatchedItemOperations.CHECK_CONSTRAINTS);
            // do remove conflicting (recursive)
            itemOps.removeNodeState(conflicting);
            // do create new node
            node = itemOps.createNodeState(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getId());
            break;

        case ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING:
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
                    | BatchedItemOperations.CHECK_VERSIONING
                    | BatchedItemOperations.CHECK_CONSTRAINTS);
            // do remove conflicting (recursive)
            itemOps.removeNodeState(conflicting);

            // create new with given uuid:
            // do create new node
            node = itemOps.createNodeState(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getId());
            break;

        case ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW:
            String msg = "a node with uuid " + nodeInfo.getId()
            + " already exists!";
            log.debug(msg);
            throw new ItemExistsException(msg);

        case ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW:
            // create new with new uuid:
            // check if new node can be added (check access rights &
            // node type constraints only, assume locking & versioning status
            // has already been checked on ancestor)
            node = itemOps.createNodeState(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
            // remember uuid mapping
            EffectiveNodeType ent = itemOps.getEffectiveNodeType(node);
            if (ent.includesNodeType(QName.MIX_REFERENCEABLE)) {
                refTracker.mappedUUID(nodeInfo.getId().getUUID(), node.getNodeId().getUUID());
            }
            break;
         //No need for default case.
        }
        return node;
    }

    /**
     * @return true if skip mode is on.
     */
    protected boolean isSkipped() {
        return skip;
    }

    /**
     * Post-process imported node (initialize properties with special
     * semantics etc.)
     *
     * @param node NodeState to postprocess
     * @throws RepositoryException if issue when postprocessing a node
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
        if (ent.includesNodeType(QName.MIX_VERSIONABLE)) {
            PropDef def;
            PropertyState prop;
            SessionImpl session = (SessionImpl) wsp.getSession();
            VersionManager vMgr = session.getVersionManager();
            /**
             * check if there's already a version history for that
             * node; this would e.g. be the case if a versionable node
             * had been exported, removed and re-imported with either
             * IMPORT_UUID_COLLISION_REMOVE_EXISTING or
             * IMPORT_UUID_COLLISION_REPLACE_EXISTING;
             * otherwise create a new version history
             */
            VersionHistory vh = vMgr.getVersionHistory(session, node);
            if (vh == null) {
                vh = vMgr.createVersionHistory(session, node);
            }

            // jcr:versionHistory
            if (!node.hasPropertyName(QName.JCR_VERSIONHISTORY)) {
                def = itemOps.findApplicablePropertyDefinition(QName.JCR_VERSIONHISTORY,
                        PropertyType.REFERENCE, false, node);
                prop = itemOps.createPropertyState(node, QName.JCR_VERSIONHISTORY,
                        PropertyType.REFERENCE, def);
                prop.setValues(new InternalValue[]{InternalValue.create(new UUID(vh.getUUID()))});
            }

            // jcr:baseVersion
            if (!node.hasPropertyName(QName.JCR_BASEVERSION)) {
                def = itemOps.findApplicablePropertyDefinition(QName.JCR_BASEVERSION,
                        PropertyType.REFERENCE, false, node);
                prop = itemOps.createPropertyState(node, QName.JCR_BASEVERSION,
                        PropertyType.REFERENCE, def);
                prop.setValues(new InternalValue[]{InternalValue.create(new UUID(vh.getRootVersion().getUUID()))});
            }

            // jcr:predecessors
            if (!node.hasPropertyName(QName.JCR_PREDECESSORS)) {
                def = itemOps.findApplicablePropertyDefinition(QName.JCR_PREDECESSORS,
                        PropertyType.REFERENCE, true, node);
                prop = itemOps.createPropertyState(node, QName.JCR_PREDECESSORS,
                        PropertyType.REFERENCE, def);
                prop.setValues(new InternalValue[]{InternalValue.create(new UUID(vh.getRootVersion().getUUID()))});
            }

            // jcr:isCheckedOut
            if (!node.hasPropertyName(QName.JCR_ISCHECKEDOUT)) {
                def = itemOps.findApplicablePropertyDefinition(QName.JCR_ISCHECKEDOUT,
                        PropertyType.BOOLEAN, false, node);
                prop = itemOps.createPropertyState(node, QName.JCR_ISCHECKEDOUT,
                        PropertyType.BOOLEAN, def);
                prop.setValues(new InternalValue[]{InternalValue.create(true)});
            }
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
    public void startNode(NodeInfo nodeInfo, List propInfos)
    throws RepositoryException {
        if (aborted) {
            return;
        }

        NodeState parent = (NodeState) parents.peek();

        if (raw && !checkNode(parent, nodeInfo, propInfos)) {
            skip = true;
        }

        if (skip) {
            return;
        }

        NodeState myNode;
        if (existing == null) {
            myNode = createNode(parent, nodeInfo);
        }
        else {
            myNode = existing;
            existing = null;
        }
        createProperties(myNode, propInfos);
        parents.push(myNode);
    }

    /**
     * {@inheritDoc}
     */
    public void endNode(NodeInfo nodeInfo) throws RepositoryException {
        //End of skip mode
        if (skipNode != null && skipNode.equals(nodeInfo)) {
            skip = false;
            skipNode = null;
            return;
        }

        if (aborted || skip) {
            return;
        }

        try {
            NodeState node = (NodeState) parents.pop();

            if (!raw) {
                this.postProcessNode(node);
            }
            itemOps.store(node);
        } catch (IllegalStateException e) {
            itemOps.cancel();
            aborted = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void end() throws RepositoryException {
        if (aborted) {
            itemOps.cancel();
            return;
        }

        wsp.sanityCheck();
        /**
         * adjust references that refer to uuid's which have been mapped to
         * newly gererated uuid's on import
         */
        Iterator iter = refTracker.getProcessedReferences();
        while (iter.hasNext()) {
            PropertyState prop = (PropertyState) iter.next();
            // being paranoid...
            if (prop.getType() != PropertyType.REFERENCE) {
                continue;
            }
            boolean modified = false;
            InternalValue[] values = prop.getValues();
            InternalValue[] newVals = new InternalValue[values.length];
            for (int i = 0; i < values.length; i++) {
                InternalValue val = values[i];
                UUID original = (UUID) val.internalValue();
                UUID adjusted = refTracker.getMappedUUID(original);
                if (adjusted != null) {
                    newVals[i] = InternalValue.create(adjusted);
                    modified = true;
                } else {
                    // reference doesn't need adjusting, just copy old value
                    newVals[i] = val;
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

        // finish update
        itemOps.update();
    }

}

