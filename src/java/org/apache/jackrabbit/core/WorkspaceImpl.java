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
import org.apache.jackrabbit.core.lock.LockManager;
import org.apache.jackrabbit.core.nodetype.ChildNodeDef;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.search.QueryManagerImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.TransactionalItemStateManager;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A <code>WorkspaceImpl</code> ...
 */
public class WorkspaceImpl implements Workspace, Constants {

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
    protected ObservationManagerImpl obsMgr;

    /**
     * The <code>QueryManager</code> for this <code>Workspace</code>.
     */
    protected QueryManagerImpl queryManager;

    /**
     * the session that was used to acquire this <code>Workspace</code>
     */
    protected final SessionImpl session;

    /**
     * The <code>LockManager</code> for this <code>Workspace</code>
     */
    protected LockManager lockMgr;

    /**
     * Package private constructor.
     *
     * @param wspConfig
     * @param stateMgr
     * @param rep
     * @param session
     */
    WorkspaceImpl(WorkspaceConfig wspConfig,
                  SharedItemStateManager stateMgr,
                  RepositoryImpl rep,
                  SessionImpl session) {
        this.wspConfig = wspConfig;
        this.rep = rep;
        this.stateMgr = new TransactionalItemStateManager(stateMgr, this);
        this.hierMgr = new HierarchyManagerImpl(rep.getRootNodeUUID(),
                this.stateMgr, session.getNamespaceResolver());
        this.session = session;
    }

    /**
     * The hierarchy manager that reflects workspace state only
     * (i.e. that is isolated from transient changes made through
     * the session)
     *
     * @return the hierarchy manager of this workspace
     */
    public HierarchyManager getHierarchyManager() {
        return hierMgr;
    }

    /**
     * Returns the item state manager associated with the workspace
     * represented by <i>this</i> <code>Workspace</code> instance.
     *
     * @return the item state manager of this workspace
     */
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
        if (obsMgr != null) {
            obsMgr.dispose();
            obsMgr = null;
        }
    }

    /**
     * Performs a sanity check on this workspace and the associated session.
     *
     * @throws RepositoryException if this workspace has been rendered invalid
     *                             for some reason
     */
    public void sanityCheck() throws RepositoryException {
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

    /**
     * @param parentId
     * @param nodeName
     * @param nodeTypeName
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws ItemNotFoundException
     * @throws ItemExistsException
     * @throws RepositoryException
     */
    public void checkAddNode(NodeId parentId, QName nodeName, QName nodeTypeName)
            throws ConstraintViolationException, AccessDeniedException,
            ItemNotFoundException, ItemExistsException, RepositoryException {

        NodeState parentState = getNodeState(parentId);

        // 1. access rights

        AccessManager accessMgr = session.getAccessManager();
        if (!accessMgr.isGranted(parentId, AccessManager.READ)) {
            throw new ItemNotFoundException(hierMgr.safeGetJCRPath(parentId));
        }
        if (!accessMgr.isGranted(parentId, AccessManager.WRITE)) {
            throw new AccessDeniedException(hierMgr.safeGetJCRPath(parentId)
                    + ": not allowed to add child node");
        }

        // 2. check node type constraints

        NodeTypeRegistry ntReg = rep.getNodeTypeRegistry();
        ChildNodeDef parentDef = ntReg.getNodeDef(parentState.getDefinitionId());
        if (parentDef.isProtected()) {
            throw new ConstraintViolationException(hierMgr.safeGetJCRPath(parentId)
                    + ": cannot add child node to protected parent node");
        }
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        entParent.checkAddNodeConstraints(nodeName, nodeTypeName);
        ChildNodeDef newNodeDef =
                findApplicableDefinition(nodeName, nodeTypeName, parentState);

        // 3. check for name collisions

        if (parentState.hasPropertyEntry(nodeName)) {
            // there's already a property with that name
            throw new ItemExistsException("cannot add child node '"
                    + nodeName.getLocalName() + "' to "
                    + hierMgr.safeGetJCRPath(parentId)
                    + ": colliding with same-named existing property");
        }
        if (parentState.hasChildNodeEntry(nodeName)) {
            // there's already a node with that name...

            // get definition of existing conflicting node
            NodeState.ChildNodeEntry entry = parentState.getChildNodeEntry(nodeName, 1);
            NodeState conflictingState;
            NodeId conflictingId = new NodeId(entry.getUUID());
            try {
                conflictingState = (NodeState) stateMgr.getItemState(conflictingId);
            } catch (ItemStateException ise) {
                String msg = "internal error: failed to retrieve state of "
                        + hierMgr.safeGetJCRPath(conflictingId);
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }
            ChildNodeDef conflictingTargetDef =
                    ntReg.getNodeDef(conflictingState.getDefinitionId());
            // check same-name sibling setting of both target and existing node
            if (!conflictingTargetDef.allowSameNameSibs()
                    || !newNodeDef.allowSameNameSibs()) {
                throw new ItemExistsException("cannot add child node '"
                        + nodeName.getLocalName() + "' to "
                        + hierMgr.safeGetJCRPath(parentId)
                        + ": colliding with same-named existing node");
            }
        }
    }

    /**
     * @param parentPath
     * @param nodeName
     * @param nodeTypeName
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws RepositoryException
     */
    public void checkAddNode(Path parentPath, QName nodeName, QName nodeTypeName)
            throws ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, RepositoryException {
        NodeState parentState = getNodeState(parentPath);
        checkAddNode((NodeId) parentState.getId(), nodeName, nodeTypeName);
    }

    /**
     * @param nodeId
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public void checkRemoveNode(NodeId nodeId)
            throws ConstraintViolationException, AccessDeniedException,
            ItemNotFoundException, RepositoryException {

        NodeState targetState = getNodeState(nodeId);
        if (targetState.getParentUUID() == null) {
            // root or orphaned node
            throw new ConstraintViolationException("cannot remove root node");
        }
        NodeId parentId = new NodeId(targetState.getParentUUID());
        NodeState parentState = getNodeState(parentId);

        // 1. access rights

        AccessManager accessMgr = session.getAccessManager();
        try {
            if (!accessMgr.isGranted(targetState.getId(), AccessManager.READ)) {
                throw new PathNotFoundException(hierMgr.safeGetJCRPath(nodeId));
            }
            if (!accessMgr.isGranted(targetState.getId(), AccessManager.REMOVE)) {
                throw new AccessDeniedException(hierMgr.safeGetJCRPath(parentId)
                        + ": not allowed to remove node");
            }
        } catch (ItemNotFoundException infe) {
            String msg = "internal error: failed to check access rights for "
                    + hierMgr.safeGetJCRPath(nodeId);
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        }

        // 2. check node type constraints

        NodeTypeRegistry ntReg = rep.getNodeTypeRegistry();
        ChildNodeDef parentDef = ntReg.getNodeDef(parentState.getDefinitionId());
        if (parentDef.isProtected()) {
            throw new ConstraintViolationException(hierMgr.safeGetJCRPath(parentId)
                    + ": cannot remove child node of protected parent node");
        }
        ChildNodeDef targetDef = ntReg.getNodeDef(targetState.getDefinitionId());
        if (targetDef.isMandatory()) {
            throw new ConstraintViolationException(hierMgr.safeGetJCRPath(nodeId)
                    + ": cannot remove mandatory node");
        }
        if (targetDef.isProtected()) {
            throw new ConstraintViolationException(hierMgr.safeGetJCRPath(nodeId)
                    + ": cannot remove protected node");
        }
    }

    /**
     * @param nodePath
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public void checkRemoveNode(Path nodePath)
            throws ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, RepositoryException {
        NodeState targetState = getNodeState(nodePath);
        checkRemoveNode((NodeId) targetState.getId());
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
     * @throws VersionException
     * @throws RepositoryException
     */
    protected void verifyCheckedOut(Path nodePath)
            throws VersionException, RepositoryException {
        // search nearest ancestor that is versionable, start with node at nodePath
        /**
         * FIXME should not only rely on existence of jcr:isCheckedOut property
         * but also verify that node.isNodeType("mix:versionable")==true;
         * this would have a negative impact on performance though...
         */
        NodeState nodeState = getNodeState(nodePath);
        while (!nodeState.hasPropertyEntry(JCR_ISCHECKEDOUT)) {
            if (nodePath.denotesRoot()) {
                return;
            }
            nodePath = nodePath.getAncestor(1);
            nodeState = getNodeState(nodePath);
        }
        PropertyId propId =
                new PropertyId(nodeState.getUUID(), JCR_ISCHECKEDOUT);
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
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected NodeState getNodeState(Path nodePath)
            throws PathNotFoundException, RepositoryException {
        try {
            ItemId id = hierMgr.resolvePath(nodePath);
            if (!id.denotesNode()) {
                throw new PathNotFoundException(hierMgr.safeGetJCRPath(nodePath));
            }
            return (NodeState) stateMgr.getItemState(id);
        } catch (NoSuchItemStateException nsise) {
            throw new PathNotFoundException(hierMgr.safeGetJCRPath(nodePath));
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to retrieve state of "
                    + hierMgr.safeGetJCRPath(nodePath);
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    protected NodeState getNodeState(NodeId id)
            throws ItemNotFoundException, RepositoryException {
        try {
            return (NodeState) stateMgr.getItemState(id);
        } catch (NoSuchItemStateException nsise) {
            throw new ItemNotFoundException(hierMgr.safeGetJCRPath(id));
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to retrieve state of "
                    + hierMgr.safeGetJCRPath(id);
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node's primary and mixin
     * node types.
     *
     * @param state
     * @return the effective node type
     * @throws RepositoryException
     */
    public EffectiveNodeType getEffectiveNodeType(NodeState state)
            throws RepositoryException {
        // build effective node type of mixins & primary type:
        // existing mixin's
        HashSet set = new HashSet((state).getMixinTypeNames());
        // primary type
        set.add(state.getNodeTypeName());
        NodeTypeRegistry ntReg = rep.getNodeTypeRegistry();
        try {
            return ntReg.getEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            String msg =
                    "internal error: failed to build effective node type for node "
                    + state.getUUID();
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
     * @return a <code>ChildNodeDef</code>
     * @throws ConstraintViolationException if no applicable child node definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    public ChildNodeDef findApplicableDefinition(QName name,
                                                 QName nodeTypeName,
                                                 NodeState parentState)
            throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicableChildNodeDef(name, nodeTypeName);
    }

    private NodeState copyNodeState(NodeState srcState,
                                    String parentUUID,
                                    ItemStateManager srcStateMgr,
                                    boolean clone)
            throws RepositoryException {

        NodeState newState;
        try {
            String uuid;
            if (clone) {
                uuid = srcState.getUUID();
            } else {
                /**
                 * todo FIXME check mix:referenceable
                 * make sure that copied reference properties are
                 * refering to new uuid
                 */
                uuid = UUID.randomUUID().toString();	// create new version 4 uuid
            }
            newState = stateMgr.createNew(uuid, srcState.getNodeTypeName(), parentUUID);
            // copy node state
            // @todo special handling required for nodes with special semantics (e.g. those defined by mix:versionable, et.al.)
            // FIXME delegate to 'node type instance handler'
            newState.setMixinTypeNames(srcState.getMixinTypeNames());
            newState.setDefinitionId(srcState.getDefinitionId());
            // copy child nodes
            Iterator iter = srcState.getChildNodeEntries().iterator();
            while (iter.hasNext()) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
                NodeState srcChildState =
                        (NodeState) srcStateMgr.getItemState(new NodeId(entry.getUUID()));
                // recursive copying of child node
                NodeState newChildState = copyNodeState(srcChildState, uuid,
                        srcStateMgr, clone);
                // persist new child node
                stateMgr.store(newChildState);
                // add new child node entry to new node
                newState.addChildNodeEntry(entry.getName(), newChildState.getUUID());
            }
            // copy properties
            iter = srcState.getPropertyEntries().iterator();
            while (iter.hasNext()) {
                NodeState.PropertyEntry entry = (NodeState.PropertyEntry) iter.next();
                PropertyState srcChildState =
                        (PropertyState) srcStateMgr.getItemState(new PropertyId(srcState.getUUID(), entry.getName()));
                PropertyState newChildState =
                        copyPropertyState(srcChildState, uuid, entry.getName());
                // persist new property
                stateMgr.store(newChildState);
                // add new property entry to new node
                newState.addPropertyEntry(entry.getName());
            }
            return newState;
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to copy state of " + srcState.getId();
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    private PropertyState copyPropertyState(PropertyState srcState,
                                            String parentUUID,
                                            QName propName)
            throws RepositoryException {

        // @todo special handling required for properties with special semantics (e.g. those defined by mix:versionable, mix:lockable, et.al.)
        PropertyState newState = stateMgr.createNew(propName, parentUUID);
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
                PropDef def = rep.getNodeTypeRegistry().getPropDef(defId);
                if (def.getDeclaringNodeType().equals(MIX_REFERENCEABLE)) {
                    if (propName.equals(JCR_UUID)) {
                        // set correct value of jcr:uuid property
                        newState.setValues(new InternalValue[]{InternalValue.create(parentUUID)});
                    }
                }
            }
        }
        return newState;
    }

    private void internalCopy(String srcAbsPath,
                              WorkspaceImpl srcWsp,
                              String destAbsPath,
                              boolean clone)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {

        // 1. check paths & retrieve state

        Path srcPath;
        NodeState srcState;
        try {
            srcPath = Path.create(srcAbsPath, session.getNamespaceResolver(), true);
            srcState = srcWsp.getNodeState(srcPath);
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
            destName = destPath.getNameElement();
            destParentPath = destPath.getAncestor(1);
            destParentState = getNodeState(destParentPath);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = destAbsPath
                    + ": invalid destination path (subscript in name element is not allowed)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // make sure destination parent node is checked-out
        verifyCheckedOut(destParentPath);

        // check lock status
        getLockManager().checkLock(destParentPath, session);

        // 2. check access rights & node type constraints

        try {
            // check read access right on source node
            if (!session.getAccessManager().isGranted(srcState.getId(), AccessManager.READ)) {
                throw new PathNotFoundException(srcAbsPath);
            }
        } catch (ItemNotFoundException infe) {
            String msg = "internal error: failed to check access rights for " + srcAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        }
        // check node type constraints
        checkAddNode(destParentPath, destName.getName(), srcState.getNodeTypeName());

        // 3. do copy operation (modify and persist affected states)

        boolean succeeded = false;
        try {
            stateMgr.edit();

            // create deep copy of source node state
            NodeState newState = copyNodeState(srcState, destParentState.getUUID(),
                    srcWsp.getItemStateManager(), clone);

            // add to new parent
            destParentState.addChildNodeEntry(destName.getName(), newState.getUUID());

            // change definition (id) of new node
            ChildNodeDef newNodeDef =
                    findApplicableDefinition(destName.getName(),
                            srcState.getNodeTypeName(), destParentState);
            newState.setDefinitionId(new NodeDefId(newNodeDef));

            // store states
            stateMgr.store(newState);
            stateMgr.store(destParentState);

            // finish update operations
            stateMgr.update();
            succeeded = true;
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to persist state of " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                stateMgr.cancel();
            }
        }
    }

    /**
     * Return the lock manager for this workspace. If not already done, creates
     * a new instance.
     *
     * @return lock manager for this workspace
     * @throws RepositoryException if an error occurs
     */
    public synchronized LockManager getLockManager() throws RepositoryException {

        // check state of this instance
        sanityCheck();

        if (lockMgr == null) {
            lockMgr = rep.getLockManager(wspConfig.getName());
        }
        return lockMgr;
    }

    //------------------------------------------------------------< Workspace >
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return wspConfig.getName();
    }

    /**
     * {@inheritDoc}
     */
    public Session getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return rep.getNamespaceRegistry();
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return session.getNodeTypeManager();
    }

    /**
     * {@inheritDoc}
     */
    public void clone(String srcWorkspace, String srcAbsPath,
                      String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // check workspace name
        if (getName().equals(srcWorkspace)) {
            // same as current workspace
            String msg = srcWorkspace + ": illegal workspace (same as current)";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // @todo re-implement Workspace#clone (respect new removeExisting flag, etc)

        // check authorization for specified workspace
        if (!session.getAccessManager().canAccess(srcWorkspace)) {
            throw new AccessDeniedException("not authorized to access " + srcWorkspace);
        }

        // clone (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath

        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            srcSession = rep.createSession(session.getSubject(), srcWorkspace);
            WorkspaceImpl srcWsp = (WorkspaceImpl) srcSession.getWorkspace();

            // do cross-workspace copy
            internalCopy(srcAbsPath, srcWsp, destAbsPath, true);
        } finally {
            if (srcSession != null) {
                // we don't need the other session anymore, logout
                srcSession.logout();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void copy(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // do intra-workspace copy
        internalCopy(srcAbsPath, this, destAbsPath, false);
    }

    /**
     * {@inheritDoc}
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // check workspace name
        if (getName().equals(srcWorkspace)) {
            // same as current workspace, delegate to intra-workspace copy method
            copy(srcAbsPath, destAbsPath);
            return;
        }

        // check authorization for specified workspace
        if (!session.getAccessManager().canAccess(srcWorkspace)) {
            throw new AccessDeniedException("not authorized to access " + srcWorkspace);
        }

        // copy (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath

        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            srcSession = rep.createSession(session.getSubject(), srcWorkspace);
            WorkspaceImpl srcWsp = (WorkspaceImpl) srcSession.getWorkspace();

            // do cross-workspace copy
            internalCopy(srcAbsPath, srcWsp, destAbsPath, false);
        } finally {
            if (srcSession != null) {
                // we don't need the other session anymore, logout
                srcSession.logout();
            }
        }
    }

    /**
     * {@inheritDoc}
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
            targetState = getNodeState(srcPath);
            srcParentState = getNodeState(srcParentPath);
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
            destParentState = getNodeState(destParentPath);
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
        verifyCheckedOut(srcParentPath);
        verifyCheckedOut(destParentPath);

        // check locked-status
        getLockManager().checkLock(destParentPath, session);

        // 2. check node type constraints & access rights

        checkRemoveNode(srcPath);
        checkAddNode(destParentPath, destName.getName(), targetState.getNodeTypeName());

        // 3. do move operation (modify and persist affected states)

        boolean succeeded = false;
        try {
            stateMgr.edit();
            boolean renameOnly = srcParentState.getUUID().equals(destParentState.getUUID());

            // add to new parent
            if (!renameOnly) {
                targetState.addParentUUID(destParentState.getUUID());
            }
            destParentState.addChildNodeEntry(destName.getName(), targetState.getUUID());

            // change definition (id) of target node
            ChildNodeDef newTargetDef = findApplicableDefinition(destName.getName(), targetState.getNodeTypeName(), destParentState);
            targetState.setDefinitionId(new NodeDefId(newTargetDef));

            // remove from old parent
            if (!renameOnly) {
                targetState.removeParentUUID(srcParentState.getUUID());
            }

            int srcNameIndex = srcName.getIndex() == 0 ? 1 : srcName.getIndex();
            srcParentState.removeChildNodeEntry(srcName.getName(), srcNameIndex);

            // store states
            stateMgr.store(targetState);
            if (renameOnly) {
                stateMgr.store(srcParentState);
            } else {
                stateMgr.store(destParentState);
                stateMgr.store(srcParentState);
            }

            // finish update
            stateMgr.update();
            succeeded = true;
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to persist state of " + destAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                stateMgr.cancel();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public ObservationManager getObservationManager()
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
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // filter workspaces according to access rights
        ArrayList list = new ArrayList();
        String names[] = session.getWorkspaceNames();
        for (int i = 0; i < names.length; i++) {
            if (session.getAccessManager().canAccess(names[i])) {
                list.add(names[i]);
            }
        }

        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler(String parentAbsPath,
                                                  int uuidBehavior)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {

        /**
         * the following code (importing through temporary session) is
         * just a work-around; a specialized WorkspaceImporter that by-passes
         * the transient layer should be used instead.
         *
         * todo replace with specialized WorkspaceImporter once fully implemented
         */

        // create temporary session in order to prevent state changes
        // of the current session
        final SessionImpl tmpSession = rep.createSession(session.getSubject(), getName());
        final ContentHandler handler =
                tmpSession.getImportContentHandler(parentAbsPath);
        return new ContentHandler() {
            public void endDocument() throws SAXException {
                handler.endDocument();
                // save changes & logout
                try {
                    tmpSession.save();
                } catch (RepositoryException re) {
                    throw new SAXException(re);
                } finally {
                    tmpSession.logout();
                }
            }

            public void startDocument() throws SAXException {
                handler.startDocument();
            }

            public void characters(char ch[], int start, int length) throws SAXException {
                handler.characters(ch, start, length);
            }

            public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
                handler.ignorableWhitespace(ch, start, length);
            }

            public void endPrefixMapping(String prefix) throws SAXException {
                handler.endPrefixMapping(prefix);
            }

            public void skippedEntity(String name) throws SAXException {
                handler.endPrefixMapping(name);
            }

            public void setDocumentLocator(Locator locator) {
                handler.setDocumentLocator(locator);
            }

            public void processingInstruction(String target, String data) throws SAXException {
                handler.processingInstruction(target, data);
            }

            public void startPrefixMapping(String prefix, String uri) throws SAXException {
                handler.startPrefixMapping(prefix, uri);
            }

            public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
                handler.endElement(namespaceURI, localName, qName);
            }

            public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
                handler.startElement(namespaceURI, localName, qName, atts);
            }
        };
/*
        // check state of this instance
        sanityCheck();

        // check path & retrieve state
        Path parentPath;
        NodeState parentState;

        try {
            parentPath = Path.create(parentAbsPath, session.getNamespaceResolver(), true);
            parentState = getNodeState(parentPath);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + parentAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }

        // make sure import target node is checked-out
        verifyCheckedOut(parentPath);

        // check locked-status
        getLockManager().checkLock(parentPath, session);

        Importer importer = new WorkspaceImporter(parentState, this, uuidBehavior);
        return new ImportHandler(importer, session.getNamespaceResolver(),
                rep.getNamespaceRegistry());
*/
    }

    /**
     * {@inheritDoc}
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

