/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.nodetype.*;
import org.apache.jackrabbit.core.search.QueryManagerImpl;
import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A <code>WorkspaceImpl</code> ...
 */
public class WorkspaceImpl implements Workspace {

    private static Logger log = Logger.getLogger(WorkspaceImpl.class);

    /**
     * The configuration of this <code>Workspace</code>
     */
    protected final WorkspaceConfig wspConfig;

    /**
     * The repository that created this workspace instance
     */
    protected final RepositoryImpl rep;

    /**
     * The persistent state mgr associated with the workspace represented by <i>this</i>
     * <code>Workspace</code> instance.
     */
    protected final TransactionalItemStateManager stateMgr;

    /**
     * The hierarchy mgr that reflects persistent state only
     * (i.e. that is isolated from transient changes made through
     * the session).
     */
    protected final HierarchyManagerImpl hierMgr;

    /**
     * The <code>ObservationManager</code> instance for this session.
     */
    protected ObservationManager obsMgr;

    /**
     * The <code>QueryManager</code> for this <code>Workspace</code>.
     */
    protected QueryManagerImpl queryManager;

    /**
     * the session that was used to acquire this <code>Workspace</code>
     */
    protected final SessionImpl session;

    /**
     * Package private constructor.
     *
     * @param wspConfig
     * @param stateMgr
     * @param rep
     * @param session
     */
    WorkspaceImpl(WorkspaceConfig wspConfig, SharedItemStateManager stateMgr,
                  RepositoryImpl rep, SessionImpl session) {
        this.wspConfig = wspConfig;
        this.rep = rep;
        this.stateMgr = new TransactionalItemStateManager(stateMgr);
        this.hierMgr = new HierarchyManagerImpl(rep.getRootNodeUUID(),
                this.stateMgr, session.getNamespaceResolver());
        this.session = session;
    }

    RepositoryImpl getRepository() {
        return rep;
    }

    public TransactionalItemStateManager getItemStateManager() {
        return stateMgr;
    }

    /**
     * Dumps the state of this <code>Workspace</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     * @throws RepositoryException
     */
    public void dump(PrintStream ps) throws RepositoryException {
        ps.println("Workspace: " + wspConfig.getName() + " (" + this + ")");
        ps.println();
        //persistentStateMgr.dump(ps);
    }

    /**
     * Disposes this <code>WorkspaceImpl</code> and frees resources.
     */
    void dispose() {
        try {
            ObservationManager om = getObservationManager();
            EventListenerIterator it = om.getRegisteredEventListeners();
            while (it.hasNext()) {
                EventListener l = it.nextEventListener();
                log.debug("removing EventListener: " + l);
                om.removeEventListener(l);
            }
        } catch (RepositoryException e) {
            log.error("Exception while disposing Workspace:", e);
        }
    }

    /**
     * Performs a sanity check on this workspace and the associated session.
     *
     * @throws RepositoryException if this workspace has been rendered invalid
     *                             for some reason
     */
    protected void sanityCheck() throws RepositoryException {
        // check session status
        session.sanityCheck();
    }

    /**
     * Creates a workspace with the given name.
     *
     * @param workspaceName name of the new workspace
     * @throws AccessDeniedException if the current session is not allowed to
     *                               create the workspace
     * @throws RepositoryException   if a workspace with the given name
     *                               already exists or if another error occurs
     * @see #getAccessibleWorkspaceNames()
     */
    public void createWorkspace(String workspaceName)
            throws AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        session.createWorkspace(workspaceName);
    }

    //-----------< misc. static helper methods for cross-workspace operations >
    /**
     * @param nodePath
     * @param nsResolver
     * @param hierMgr
     * @param stateMgr
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected static NodeState getNodeState(String nodePath,
                                            NamespaceResolver nsResolver,
                                            HierarchyManagerImpl hierMgr,
                                            ItemStateManager stateMgr)
            throws PathNotFoundException, RepositoryException {
        try {
            return getNodeState(Path.create(nodePath, nsResolver, true), hierMgr, stateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + nodePath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
    }

    /**
     * @param path
     * @param nsResolver
     * @param hierMgr
     * @param stateMgr
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected static NodeState getParentNodeState(String path,
                                                  NamespaceResolver nsResolver,
                                                  HierarchyManagerImpl hierMgr,
                                                  ItemStateManager stateMgr)

            throws PathNotFoundException, RepositoryException {
        try {
            return getNodeState(Path.create(path, nsResolver, true).getAncestor(1), hierMgr, stateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + path;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
    }

    /**
     * @param nodePath
     * @param hierMgr
     * @param stateMgr
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected static NodeState getNodeState(Path nodePath,
                                            HierarchyManagerImpl hierMgr,
                                            ItemStateManager stateMgr)
            throws PathNotFoundException, RepositoryException {
        try {
            ItemId id = hierMgr.resolvePath(nodePath);
            if (!id.denotesNode()) {
                throw new PathNotFoundException(hierMgr.safeGetJCRPath(nodePath));
            }
            return getNodeState((NodeId) id, stateMgr);
        } catch (NoSuchItemStateException nsise) {
            throw new PathNotFoundException(hierMgr.safeGetJCRPath(nodePath));
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to retrieve state of " + hierMgr.safeGetJCRPath(nodePath);
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * @param id
     * @param stateMgr
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected static NodeState getNodeState(NodeId id,
                                            ItemStateManager stateMgr)
            throws NoSuchItemStateException, ItemStateException {
        return (NodeState) stateMgr.getItemState(id);
    }

    /**
     * Verifies that the node at <code>nodePath</code> is checked-out; throws a
     * <code>VersionException</code> if that's not the case.
     * <p/>
     * A node is considered <i>checked-out</i> if it is versionable and
     * checked-out, or is non-versionable but its nearest versionable ancestor
     * is checked-out, or is non-versionable and there are no versionable
     * ancestors.
     *
     * @param nodePath
     * @param hierMgr
     * @param stateMgr
     * @throws VersionException
     * @throws RepositoryException
     */
    protected static void verifyCheckedOut(Path nodePath,
                                           HierarchyManagerImpl hierMgr,
                                           ItemStateManager stateMgr)
            throws VersionException, RepositoryException {
        // search nearest ancestor that is versionable, start with node at nodePath
        /**
         * FIXME should not only rely on existence of jcr:isCheckedOut property
         * but also verify that node.isNodeType("mix:versionable")==true;
         * this would have a negative impact on performance though...
         */
        NodeState nodeState = getNodeState(nodePath, hierMgr, stateMgr);
        while (!nodeState.hasPropertyEntry(VersionManager.PROPNAME_IS_CHECKED_OUT)) {
            if (nodePath.denotesRoot()) {
                return;
            }
            nodePath = nodePath.getAncestor(1);
            nodeState = getNodeState(nodePath, hierMgr, stateMgr);
        }
        PropertyId propId =
                new PropertyId(nodeState.getUUID(), VersionManager.PROPNAME_IS_CHECKED_OUT);
        PropertyState propState;
        try {
            propState = (PropertyState) stateMgr.getItemState(propId);
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to retrieve state of "
                    + hierMgr.safeGetJCRPath(propId);
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
        boolean checkedOut = ((Boolean) propState.getValues()[0].internalValue()).booleanValue();
        if (!checkedOut) {
            throw new VersionException(hierMgr.safeGetJCRPath(nodePath) + " is checked-in");
        }
    }

    /**
     * @param nodePath
     * @param nodeTypeName
     * @param ntReg
     * @param accessMgr
     * @param hierMgr
     * @param stateMgr
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws RepositoryException
     */
    protected static void checkAddNode(Path nodePath, QName nodeTypeName,
                                       NodeTypeRegistry ntReg,
                                       AccessManagerImpl accessMgr,
                                       HierarchyManagerImpl hierMgr,
                                       ItemStateManager stateMgr)
            throws ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, RepositoryException {

        Path parentPath = nodePath.getAncestor(1);
        NodeState parentState = getNodeState(parentPath, hierMgr, stateMgr);

        // 1. check path & access rights

        Path.PathElement nodeName = nodePath.getNameElement();
        try {
            // check access rights
            if (!accessMgr.isGranted(parentState.getId(), AccessManager.READ)) {
                throw new PathNotFoundException(hierMgr.safeGetJCRPath(parentPath));
            }
            if (!accessMgr.isGranted(parentState.getId(), AccessManager.WRITE)) {
                throw new AccessDeniedException(hierMgr.safeGetJCRPath(parentPath) + ": not allowed to add child node");
            }
        } catch (ItemNotFoundException infe) {
            String msg = "internal error: failed to check access rights for " + hierMgr.safeGetJCRPath(parentPath);
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        }

        // 2. check node type constraints

        ChildNodeDef parentDef = ntReg.getNodeDef(parentState.getDefinitionId());
        if (parentDef.isProtected()) {
            throw new ConstraintViolationException(hierMgr.safeGetJCRPath(parentPath) + ": cannot add child node to protected parent node");
        }
        EffectiveNodeType entParent = getEffectiveNodeType(parentState, ntReg);
        entParent.checkAddNodeConstraints(nodeName.getName(), nodeTypeName);
        ChildNodeDef newNodeDef = findApplicableDefinition(nodeName.getName(), nodeTypeName, parentState, ntReg);

        // 3. check for name collisions

        if (parentState.hasPropertyEntry(nodeName.getName())) {
            // there's already a property with that name
            throw new ItemExistsException(hierMgr.safeGetJCRPath(nodePath));
        }
        if (parentState.hasChildNodeEntry(nodeName.getName())) {
            // there's already a node with that name...

            // get definition of existing conflicting node
            NodeState.ChildNodeEntry entry = parentState.getChildNodeEntry(nodeName.getName(), 1);
            NodeState conflictingState;
            NodeId conflictingId = new NodeId(entry.getUUID());
            try {
                conflictingState = (NodeState) stateMgr.getItemState(conflictingId);
            } catch (ItemStateException ise) {
                String msg = "internal error: failed to retrieve state of " + hierMgr.safeGetJCRPath(conflictingId);
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }
            ChildNodeDef conflictingTargetDef = ntReg.getNodeDef(conflictingState.getDefinitionId());
            // check same-name sibling setting of both target and existing node
            if (!conflictingTargetDef.allowSameNameSibs()
                    || !newNodeDef.allowSameNameSibs()) {
                throw new ItemExistsException(hierMgr.safeGetJCRPath(nodePath));
            }
        }
    }

    /**
     * @param nodePath
     * @param ntReg
     * @param accessMgr
     * @param hierMgr
     * @param stateMgr
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected static void checkRemoveNode(Path nodePath,
                                          NodeTypeRegistry ntReg,
                                          AccessManagerImpl accessMgr,
                                          HierarchyManagerImpl hierMgr,
                                          ItemStateManager stateMgr)
            throws ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, RepositoryException {

        NodeState targetState = getNodeState(nodePath, hierMgr, stateMgr);
        Path parentPath = nodePath.getAncestor(1);
        NodeState parentState = getNodeState(parentPath, hierMgr, stateMgr);

        // 1. check path & access rights

        try {
            // check access rights
            if (!accessMgr.isGranted(targetState.getId(), AccessManager.READ)) {
                throw new PathNotFoundException(hierMgr.safeGetJCRPath(nodePath));
            }
            if (!accessMgr.isGranted(parentState.getId(), AccessManager.WRITE)) {
                throw new AccessDeniedException(hierMgr.safeGetJCRPath(parentPath) + ": not allowed to remove node");
            }
        } catch (ItemNotFoundException infe) {
            String msg = "internal error: failed to check access rights for " + hierMgr.safeGetJCRPath(nodePath);
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        }

        // 2. check node type constraints

        ChildNodeDef parentDef = ntReg.getNodeDef(parentState.getDefinitionId());
        if (parentDef.isProtected()) {
            throw new ConstraintViolationException(hierMgr.safeGetJCRPath(parentPath) + ": cannot remove child node of protected parent node");
        }
        ChildNodeDef targetDef = ntReg.getNodeDef(targetState.getDefinitionId());
        if (targetDef.isMandatory()) {
            throw new ConstraintViolationException(hierMgr.safeGetJCRPath(nodePath) + ": cannot remove mandatory node");
        }
        if (targetDef.isProtected()) {
            throw new ConstraintViolationException(hierMgr.safeGetJCRPath(nodePath) + ": cannot remove protected node");
        }
    }

    /**
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node's primary and mixin
     * node types.
     *
     * @param state
     * @param ntReg
     * @return the effective node type
     * @throws RepositoryException
     */
    protected static EffectiveNodeType getEffectiveNodeType(NodeState state,
                                                            NodeTypeRegistry ntReg)
            throws RepositoryException {
        // build effective node type of mixins & primary type:
        // existing mixin's
        HashSet set = new HashSet((state).getMixinTypeNames());
        // primary type
        set.add(state.getNodeTypeName());
        try {
            return ntReg.getEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node " + state.getUUID();
            log.debug(msg);
            throw new RepositoryException(msg, ntce);
        }
    }

    /**
     * Helper method that finds the applicable definition for the
     * a child node with the given name and node type in the parent node's
     * node type and mixin types.
     *
     * @param name
     * @param nodeTypeName
     * @param parentState
     * @param ntReg
     * @return a <code>ChildNodeDef</code>
     * @throws ConstraintViolationException if no applicable child node definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    protected static ChildNodeDef findApplicableDefinition(QName name,
                                                           QName nodeTypeName,
                                                           NodeState parentState,
                                                           NodeTypeRegistry ntReg)
            throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState, ntReg);
        return entParent.getApplicableChildNodeDef(name, nodeTypeName);
    }

    private static NodeState copyNodeState(NodeState srcState,
                                           String parentUUID,
                                           NodeTypeRegistry ntReg,
                                           HierarchyManagerImpl srcHierMgr,
                                           ItemStateManager srcStateMgr,
                                           UpdatableItemStateManager destStateMgr,
                                           boolean clone)
            throws RepositoryException {

        NodeState newState;
        try {
            String uuid;
            if (clone) {
                uuid = srcState.getUUID();
            } else {
                uuid = UUID.randomUUID().toString();	// create new version 4 uuid
            }
            newState = destStateMgr.createNew(uuid, srcState.getNodeTypeName(), parentUUID);
            // copy node state
            // @todo special handling required for nodes with special semantics (e.g. those defined by mix:versionable, et.al.)
            // FIXME delegate to 'node type instance handler'
            newState.setMixinTypeNames(srcState.getMixinTypeNames());
            newState.setDefinitionId(srcState.getDefinitionId());
            // copy child nodes
            Iterator iter = srcState.getChildNodeEntries().iterator();
            while (iter.hasNext()) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
                NodeState srcChildState = (NodeState) srcStateMgr.getItemState(new NodeId(entry.getUUID()));
                // recursive copying of child node
                NodeState newChildState = copyNodeState(srcChildState, uuid,
                        ntReg, srcHierMgr, srcStateMgr, destStateMgr, clone);
                // persist new child node
                destStateMgr.store(newChildState);
                // add new child node entry to new node
                newState.addChildNodeEntry(entry.getName(), newChildState.getUUID());
            }
            // copy properties
            iter = srcState.getPropertyEntries().iterator();
            while (iter.hasNext()) {
                NodeState.PropertyEntry entry = (NodeState.PropertyEntry) iter.next();
                PropertyState srcChildState = (PropertyState) srcStateMgr.getItemState(new PropertyId(srcState.getUUID(), entry.getName()));
                PropertyState newChildState = copyPropertyState(srcChildState, uuid, entry.getName(),
                        ntReg, srcHierMgr, srcStateMgr, destStateMgr);
                // persist new property
                destStateMgr.store(newChildState);
                // add new property entry to new node
                newState.addPropertyEntry(entry.getName());
            }
            return newState;
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to copy state of " + srcHierMgr.safeGetJCRPath(srcState.getId());
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    private static PropertyState copyPropertyState(PropertyState srcState,
                                                   String parentUUID,
                                                   QName propName,
                                                   NodeTypeRegistry ntReg,
                                                   HierarchyManagerImpl srcHierMgr,
                                                   ItemStateManager srcStateMgr,
                                                   UpdatableItemStateManager destStateMgr)
            throws RepositoryException {

        // @todo special handling required for properties with special semantics (e.g. those defined by mix:versionable, mix:lockable, et.al.)
        PropertyState newState = destStateMgr.createNew(propName, parentUUID);
        PropDefId defId = srcState.getDefinitionId();
        newState.setDefinitionId(defId);
        newState.setType(srcState.getType());
        newState.setMultiValued(srcState.isMultiValued());
        InternalValue[] values = srcState.getValues();
        if (values != null) {
            InternalValue[] newValues = new InternalValue[values.length];
            for (int i = 0; i < values.length; i++) {
                newValues[i] = values[i] != null ? values[i].createCopy() : null;
            }
            newState.setValues(values);
            // FIXME delegate to 'node type instance handler'
            if (defId != null) {
                PropDef def = ntReg.getPropDef(defId);
                if (def.getDeclaringNodeType().equals(NodeTypeRegistry.MIX_REFERENCEABLE)) {
                    if (propName.equals(ItemImpl.PROPNAME_UUID)) {
                        // set correct value of jcr:uuid property
                        newState.setValues(new InternalValue[]{InternalValue.create(parentUUID)});
                    }
                }
            }
        }
        return newState;
    }

    private static void internalCopy(String srcAbsPath,
                                     ItemStateManager srcStateMgr,
                                     HierarchyManagerImpl srcHierMgr,
                                     String destAbsPath,
                                     UpdatableItemStateManager destStateMgr,
                                     HierarchyManagerImpl destHierMgr,
                                     AccessManagerImpl accessMgr,
                                     NamespaceResolver nsResolver,
                                     NodeTypeRegistry ntReg,
                                     boolean clone)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {

        // 1. check paths & retrieve state

        Path srcPath;
        NodeState srcState;
        try {
            srcPath = Path.create(srcAbsPath, nsResolver, true);
            srcState = getNodeState(srcPath, srcHierMgr, srcStateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + srcAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }

        Path destPath;
        Path.PathElement destName;
        Path destParentPath;
        NodeState destParentState;
        try {
            destPath = Path.create(destAbsPath, nsResolver, true);
            destName = destPath.getNameElement();
            destParentPath = destPath.getAncestor(1);
            destParentState = getNodeState(destParentPath, destHierMgr, destStateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = destAbsPath + ": invalid destination path (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // make sure destination parent node is checked-out
        verifyCheckedOut(destParentPath, destHierMgr, destStateMgr);

        // @todo check locked-status

        // 2. check access rights & node type constraints

        try {
            // check read access right on source node
            if (!accessMgr.isGranted(srcState.getId(), AccessManager.READ)) {
                throw new PathNotFoundException(srcAbsPath);
            }
        } catch (ItemNotFoundException infe) {
            String msg = "internal error: failed to check access rights for " + srcAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        }
        // check node type constraints
        checkAddNode(destPath, srcState.getNodeTypeName(), ntReg, accessMgr, destHierMgr, destStateMgr);

        // 3. do copy operation (modify and persist affected states)
        try {
            destStateMgr.edit();

            // create deep copy of source node state
            NodeState newState = copyNodeState(srcState, destParentState.getUUID(),
                    ntReg, srcHierMgr, srcStateMgr, destStateMgr, clone);

            // add to new parent
            destParentState.addChildNodeEntry(destName.getName(), newState.getUUID());

            // change definition (id) of new node
            ChildNodeDef newNodeDef = findApplicableDefinition(destName.getName(), srcState.getNodeTypeName(), destParentState, ntReg);
            newState.setDefinitionId(new NodeDefId(newNodeDef));

            // persist states
            destStateMgr.store(newState);
            destStateMgr.store(destParentState);

            // finish update operations
            destStateMgr.update();
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to persist state of " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    //------------------------------------------------------------< Workspace >
    /**
     * @see Workspace#getName
     */
    public String getName() {
        return wspConfig.getName();
    }

    /**
     * @see Workspace#getSession
     */
    public Session getSession() {
        return session;
    }

    /**
     * @see Workspace#getNamespaceRegistry
     */
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return rep.getNamespaceRegistry();
    }

    /**
     * @see Workspace#getNodeTypeManager
     */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return session.getNodeTypeManager();
    }

    /**
     * @see Workspace#clone(String, String, String, boolean)
     */
    public void clone(String srcWorkspace, String srcAbsPath,
                      String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // @todo reimplement Workspace#clone according to new spec

        // clone (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath
        ItemStateManager srcStateMgr = rep.getWorkspaceStateManager(srcWorkspace);
        // FIXME need to setup a hierarchy manager for source workspace
        HierarchyManagerImpl srcHierMgr =
                new HierarchyManagerImpl(rep.getRootNodeUUID(), srcStateMgr, session.getNamespaceResolver());
        // do cross-workspace copy
        internalCopy(srcAbsPath, srcStateMgr, srcHierMgr,
                destAbsPath, stateMgr, hierMgr,
                session.getAccessManager(), session.getNamespaceResolver(),
                rep.getNodeTypeRegistry(), true);
    }

    /**
     * @see Workspace#copy(String, String)
     */
    public void copy(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // do intra-workspace copy
        internalCopy(srcAbsPath, stateMgr, hierMgr,
                destAbsPath, stateMgr, hierMgr,
                session.getAccessManager(), session.getNamespaceResolver(),
                rep.getNodeTypeRegistry(), false);
    }

    /**
     * @see Workspace#copy(String, String, String)
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // copy (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath
        ItemStateManager srcStateMgr = rep.getWorkspaceStateManager(srcWorkspace);
        // FIXME need to setup a hierarchy manager for source workspace
        HierarchyManagerImpl srcHierMgr = new HierarchyManagerImpl(rep.getRootNodeUUID(), srcStateMgr, session.getNamespaceResolver());
        // do cross-workspace copy
        internalCopy(srcAbsPath, srcStateMgr, srcHierMgr,
                destAbsPath, stateMgr, hierMgr,
                session.getAccessManager(), session.getNamespaceResolver(),
                rep.getNodeTypeRegistry(), false);
    }

    /**
     * @see Workspace#move
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // intra-workspace move...

        // 1. check paths & retrieve state
        Path srcPath;
        Path.PathElement srcName;
        Path srcParentPath;
        NodeState targetState;
        NodeState srcParentState;

        try {
            srcPath = Path.create(srcAbsPath, session.getNamespaceResolver(), true);
            srcName = srcPath.getNameElement();
            srcParentPath = srcPath.getAncestor(1);
            targetState = getNodeState(srcPath, hierMgr, stateMgr);
            srcParentState = getNodeState(srcParentPath, hierMgr, stateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + srcAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }

        Path destPath;
        Path.PathElement destName;
        Path destParentPath;
        NodeState destParentState;

        try {
            destPath = Path.create(destAbsPath, session.getNamespaceResolver(), true);
            if (srcPath.isAncestorOf(destPath)) {
                String msg = destAbsPath + ": invalid destination path (cannot be descendant of source path)";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            destName = destPath.getNameElement();
            destParentPath = destPath.getAncestor(1);
            destParentState = getNodeState(destParentPath, hierMgr, stateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }

        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = destAbsPath + ": invalid destination path (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // make sure both source & destination parent nodes are checked-out
        verifyCheckedOut(srcParentPath, hierMgr, stateMgr);
        verifyCheckedOut(destParentPath, hierMgr, stateMgr);

        // @todo check locked-status

        // 2. check node type constraints & access rights

        checkRemoveNode(srcPath, rep.getNodeTypeRegistry(),
                session.getAccessManager(), hierMgr, stateMgr);
        checkAddNode(destPath, targetState.getNodeTypeName(),
                rep.getNodeTypeRegistry(), session.getAccessManager(),
                hierMgr, stateMgr);

        // 3. do move operation (modify and persist affected states)
        try {
            stateMgr.edit();
            boolean renameOnly = srcParentState.getUUID().equals(destParentState.getUUID());

            // add to new parent
            if (!renameOnly) {
                targetState.addParentUUID(destParentState.getUUID());
            }
            destParentState.addChildNodeEntry(destName.getName(), targetState.getUUID());

            // change definition (id) of target node
            ChildNodeDef newTargetDef = findApplicableDefinition(destName.getName(), targetState.getNodeTypeName(), destParentState, rep.getNodeTypeRegistry());
            targetState.setDefinitionId(new NodeDefId(newTargetDef));

            // remove from old parent
            if (!renameOnly) {
                targetState.removeParentUUID(srcParentState.getUUID());
            }

            int srcNameIndex = srcName.getIndex() == 0 ? 1 : srcName.getIndex();
            srcParentState.removeChildNodeEntry(srcName.getName(), srcNameIndex);

            // persist states
            stateMgr.store(targetState);
            if (renameOnly) {
                stateMgr.store(srcParentState);
            } else {
                stateMgr.store(destParentState);
                stateMgr.store(srcParentState);
            }

            // finish update
            stateMgr.update();
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to persist state of " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * @see Workspace#getObservationManager
     */
    public synchronized ObservationManager getObservationManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {

        // check state of this instance
        sanityCheck();

        if (obsMgr == null) {
            try {
                obsMgr = rep.getObservationManagerFactory(wspConfig.getName()).createObservationManager(session, session.getItemManager());
            } catch (NoSuchWorkspaceException nswe) {
                // should never get here
                String msg = "internal error: failed to instantiate observation manager";
                log.debug(msg);
                throw new RepositoryException(msg, nswe);
            }
        }
        return obsMgr;
    }

    /**
     * @see Workspace#getQueryManager
     */
    public synchronized QueryManager getQueryManager() throws RepositoryException {

        // check state of this instance
        sanityCheck();

        if (queryManager == null) {
            SearchManager searchManager;
            try {
                searchManager = rep.getSearchManager(wspConfig.getName());
                if (searchManager == null) {
                    String msg = "no search manager configured for this workspace";
                    log.debug(msg);
                    throw new RepositoryException(msg);
                }
            } catch (NoSuchWorkspaceException nswe) {
                // should never get here
                String msg = "internal error: failed to instantiate query manager";
                log.debug(msg);
                throw new RepositoryException(msg, nswe);
            }
            queryManager = new QueryManagerImpl(session, session.getItemManager(), searchManager);
        }
        return queryManager;
    }

    /**
     * @see Workspace#restore(Version[], boolean)
     */
    public void restore(Version[] versions, boolean removeExisting)
            throws ItemExistsException, UnsupportedRepositoryOperationException,
            VersionException, LockException, InvalidItemStateException,
            RepositoryException {

        // check state of this instance
        sanityCheck();

        // @todo implement Workspace#restore
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @see Workspace#getAccessibleWorkspaceNames
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return session.getWorkspaceNames();
    }

    /**
     * @see Workspace#getImportContentHandler(String, int)
     */
    public ContentHandler getImportContentHandler(String parentAbsPath,
                                                  int uuidBehavior)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // @todo implement Workspace#getImportContentHandler
        throw new RepositoryException("not yet implemented");
    }

    /**
     * @see Workspace#importXML(String, InputStream, int)
     */
    public void importXML(String parentAbsPath, InputStream in,
                          int uuidBehavior)
            throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, InvalidSerializedDataException,
            LockException, RepositoryException {

        ImportHandler handler =
                (ImportHandler) getImportContentHandler(parentAbsPath, uuidBehavior);
        try {
            XMLReader parser =
                    XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            parser.setContentHandler(handler);
            parser.setErrorHandler(handler);
            parser.parse(new InputSource(in));
        } catch (SAXException se) {
            // check for wrapped repository exception
            Exception e = se.getException();
            if (e != null && e instanceof RepositoryException) {
                throw (RepositoryException) e;
            } else {
                String msg = "failed to parse XML stream";
                log.debug(msg);
                throw new InvalidSerializedDataException(msg, se);
            }
        }
    }
}
