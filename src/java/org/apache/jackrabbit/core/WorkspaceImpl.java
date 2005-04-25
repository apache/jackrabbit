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
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.observation.ObservationManagerFactory;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.query.QueryManagerImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.TransactionalItemStateManager;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.version.GenericVersionSelector;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.VersionImpl;
import org.apache.jackrabbit.core.version.VersionSelector;
import org.apache.jackrabbit.core.xml.ImportHandler;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.WorkspaceImporter;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
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
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
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
import javax.jcr.version.VersionHistory;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * A <code>WorkspaceImpl</code> ...
 */
public class WorkspaceImpl implements Workspace, Constants {

    private static Logger log = Logger.getLogger(WorkspaceImpl.class);

    // flags used by private internalCopy() method
    private static final int COPY = 0;
    private static final int CLONE = 1;
    private static final int CLONE_REMOVE_EXISTING = 2;

    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveNode}</code> methods:<p/>
     * check access rights
     */
    public static final int CHECK_ACCESS = 1;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveNode}</code> methods:<p/>
     * check lock status
     */
    public static final int CHECK_LOCK = 2;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveNode}</code> methods:<p/>
     * check checked-out status
     */
    public static final int CHECK_VERSIONING = 4;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveNode}</code> methods:<p/>
     * check constraints defined in node type
     */
    public static final int CHECK_CONSTRAINTS = 16;
    /**
     * option for <code>{@link #checkRemoveNode}</code> method:<p/>
     * check that target node is not being referenced
     */
    public static final int CHECK_REFERENCES = 8;

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
     * represented by <i>this</i> <code>WorkspaceImpl</code> instance.
     *
     * @return the item state manager of this workspace
     */
    public TransactionalItemStateManager getItemStateManager() {
        return stateMgr;
    }

    /**
     * Dumps the state of this <code>WorkspaceImpl</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     * @throws RepositoryException
     */
    public void dump(PrintStream ps) throws RepositoryException {
        ps.println("Workspace: " + wspConfig.getName() + " (" + this + ")");
        ps.println();
        stateMgr.dump(ps);
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
     * Checks whether the given node state satisfies the constraints implied by
     * its primary and mixin node types. The following validations/checks are
     * performed:
     * <ul>
     * <li>check if its node type satisfies the 'required node types' constraint
     * specified in its definition</li>
     * <li>check if all 'mandatory' child items exist</li>
     * <li>for every property: check if the property value satisfies the
     * value constraints specified in the property's definition</li>
     * </ul>
     *
     * @param nodeState node state to be validated
     * @throws ConstraintViolationException if any of the validations fail
     * @throws RepositoryException          if another error occurs
     */
    public void validate(NodeState nodeState)
            throws ConstraintViolationException, RepositoryException {
        // effective node type (primary type incl. mixins)
        EffectiveNodeType ent = getEffectiveNodeType(nodeState);
        NodeTypeRegistry ntReg = rep.getNodeTypeRegistry();
        NodeDef def = ntReg.getNodeDef(nodeState.getDefinitionId());

        // check if primary type satisfies the 'required node types' constraint
        QName[] requiredPrimaryTypes = def.getRequiredPrimaryTypes();
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            if (!ent.includesNodeType(requiredPrimaryTypes[i])) {
                String msg = hierMgr.safeGetJCRPath(nodeState.getId())
                        + ": missing required primary type "
                        + requiredPrimaryTypes[i];
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        // mandatory properties
        PropDef[] pda = ent.getMandatoryPropDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            if (!nodeState.hasPropertyEntry(pd.getName())) {
                String msg = hierMgr.safeGetJCRPath(nodeState.getId())
                        + ": mandatory property " + pd.getName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        // mandatory child nodes
        NodeDef[] cnda = ent.getMandatoryNodeDefs();
        for (int i = 0; i < cnda.length; i++) {
            NodeDef cnd = cnda[i];
            if (!nodeState.hasChildNodeEntry(cnd.getName())) {
                String msg = hierMgr.safeGetJCRPath(nodeState.getId())
                        + ": mandatory child node " + cnd.getName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
    }

    /**
     * Checks whether the given property state satisfies the constraints
     * implied by its definition. The following validations/checks are
     * performed:
     * <ul>
     * <li>check if the type of the property values does comply with the
     * requiredType specified in the property's definition</li>
     * <li>check if the property values satisfy the value constraints
     * specified in the property's definition</li>
     * </ul>
     *
     * @param propState property state to be validated
     * @throws ConstraintViolationException if any of the validations fail
     * @throws RepositoryException          if another error occurs
     */
    public void validate(PropertyState propState)
            throws ConstraintViolationException, RepositoryException {
        NodeTypeRegistry ntReg = rep.getNodeTypeRegistry();
        PropDef def = ntReg.getPropDef(propState.getDefinitionId());
        InternalValue[] values = propState.getValues();
        int type = PropertyType.UNDEFINED;
        for (int i = 0; i < values.length; i++) {
            if (type == PropertyType.UNDEFINED) {
                type = values[i].getType();
            } else if (type != values[i].getType()) {
                throw new ConstraintViolationException(hierMgr.safeGetJCRPath(propState.getId())
                        + ": inconsistent value types");
            }
            if (def.getRequiredType() != PropertyType.UNDEFINED
                    && def.getRequiredType() != type) {
                throw new ConstraintViolationException(hierMgr.safeGetJCRPath(propState.getId())
                        + ": requiredType constraint is not satisfied");
            }
        }
        EffectiveNodeType.checkSetPropertyValueConstraints(def, values);
    }

    /**
     * Checks if adding if adding a child node called <code>nodeName</code> of
     * node type <code>nodeTypeName</code> to the given parent node is allowed
     * in the current context.
     *
     * @param parentState
     * @param nodeName
     * @param nodeTypeName
     * @param options      bit-wise OR'ed flags specifying the checks that should be
     *                     performed; any combination of the following constants:
     *                     <ul>
     *                     <li><code>{@link #CHECK_ACCESS}</code>: make sure
     *                     current session is granted read & write access on
     *                     parent node</li>
     *                     <li><code>{@link #CHECK_LOCK}</code>: make sure
     *                     there's no foreign lock on parent node</li>
     *                     <li><code>{@link #CHECK_VERSIONING}</code>: make sure
     *                     parent node is checked-out</li>
     *                     <li><code>{@link #CHECK_CONSTRAINTS}</code>:
     *                     make sure no node type constraints would be violated</li>
     *                     <li><code>{@link #CHECK_REFERENCES}</code></li>
     *                     </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ItemExistsException
     * @throws RepositoryException
     */
    public void checkAddNode(NodeState parentState, QName nodeName,
                             QName nodeTypeName, int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ItemExistsException, RepositoryException {

        Path parentPath = hierMgr.getPath(parentState.getId());

        // 1. locking

        if ((options & CHECK_LOCK) == CHECK_LOCK) {
            // make sure there's no foreign lock on parent node
            getLockManager().checkLock(parentPath, session);
        }

        // 2. versioning status

        if ((options & CHECK_VERSIONING) == CHECK_VERSIONING) {
            // make sure parent node is checked-out
            verifyCheckedOut(parentPath);
        }

        // 3. access rights

        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            AccessManager accessMgr = session.getAccessManager();
            // make sure current session is granted read access on parent node
            if (!accessMgr.isGranted(parentState.getId(), AccessManager.READ)) {
                throw new ItemNotFoundException(hierMgr.safeGetJCRPath(parentState.getId()));
            }
            // make sure current session is granted write access on parent node
            if (!accessMgr.isGranted(parentState.getId(), AccessManager.WRITE)) {
                throw new AccessDeniedException(hierMgr.safeGetJCRPath(parentState.getId())
                        + ": not allowed to add child node");
            }
        }

        // 4. node type constraints

        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            NodeTypeRegistry ntReg = rep.getNodeTypeRegistry();
            NodeDef parentDef = ntReg.getNodeDef(parentState.getDefinitionId());
            // make sure parent node is not protected
            if (parentDef.isProtected()) {
                throw new ConstraintViolationException(hierMgr.safeGetJCRPath(parentState.getId())
                        + ": cannot add child node to protected parent node");
            }
            // make sure there's an applicable definition for new child node
            EffectiveNodeType entParent = getEffectiveNodeType(parentState);
            entParent.checkAddNodeConstraints(nodeName, nodeTypeName);
            NodeDef newNodeDef =
                    findApplicableNodeDefinition(nodeName, nodeTypeName,
                            parentState);

            // check for name collisions
            if (parentState.hasPropertyEntry(nodeName)) {
                // there's already a property with that name
                throw new ItemExistsException("cannot add child node '"
                        + nodeName.getLocalName() + "' to "
                        + hierMgr.safeGetJCRPath(parentState.getId())
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
                NodeDef conflictingTargetDef =
                        ntReg.getNodeDef(conflictingState.getDefinitionId());
                // check same-name sibling setting of both target and existing node
                if (!conflictingTargetDef.allowsSameNameSiblings()
                        || !newNodeDef.allowsSameNameSiblings()) {
                    throw new ItemExistsException("cannot add child node '"
                            + nodeName.getLocalName() + "' to "
                            + hierMgr.safeGetJCRPath(parentState.getId())
                            + ": colliding with same-named existing node");
                }
            }
        }
    }

    /**
     * Checks if removing the given target node entirely (i.e. unlinking from
     * all its parents) is allowed in the current context.
     *
     * @param targetState
     * @param options     bit-wise OR'ed flags specifying the checks that should be
     *                    performed; any combination of the following constants:
     *                    <ul>
     *                    <li><code>{@link #CHECK_ACCESS}</code>: make sure
     *                    current session is granted read access on parent
     *                    and remove privilege on target node</li>
     *                    <li><code>{@link #CHECK_LOCK}</code>: make sure
     *                    there's no foreign lock on parent node</li>
     *                    <li><code>{@link #CHECK_VERSIONING}</code>: make sure
     *                    parent node is checked-out</li>
     *                    <li><code>{@link #CHECK_CONSTRAINTS}</code>:
     *                    make sure no node type constraints would be violated</li>
     *                    <li><code>{@link #CHECK_REFERENCES}</code>:
     *                    make sure no references exist on target node</li>
     *                    </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ReferentialIntegrityException
     * @throws RepositoryException
     */
    public void checkRemoveNode(NodeState targetState, int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ReferentialIntegrityException, RepositoryException {
        List parentUUIDs = targetState.getParentUUIDs();
        Iterator iter = parentUUIDs.iterator();
        while (iter.hasNext()) {
            NodeId parentId = new NodeId((String) iter.next());
            checkRemoveNode(targetState, parentId, options);
        }
    }

    /**
     * Checks if removing the given target node from the specifed parent
     * is allowed in the current context.
     *
     * @param targetState
     * @param parentId
     * @param options     bit-wise OR'ed flags specifying the checks that should be
     *                    performed; any combination of the following constants:
     *                    <ul>
     *                    <li><code>{@link #CHECK_ACCESS}</code>: make sure
     *                    current session is granted read access on parent
     *                    and remove privilege on target node</li>
     *                    <li><code>{@link #CHECK_LOCK}</code>: make sure
     *                    there's no foreign lock on parent node</li>
     *                    <li><code>{@link #CHECK_VERSIONING}</code>: make sure
     *                    parent node is checked-out</li>
     *                    <li><code>{@link #CHECK_CONSTRAINTS}</code>:
     *                    make sure no node type constraints would be violated</li>
     *                    <li><code>{@link #CHECK_REFERENCES}</code>:
     *                    make sure no references exist on target node</li>
     *                    </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ReferentialIntegrityException
     * @throws RepositoryException
     */
    public void checkRemoveNode(NodeState targetState, NodeId parentId,
                                int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ReferentialIntegrityException, RepositoryException {

        if (targetState.getParentUUID() == null) {
            // root or orphaned node
            throw new ConstraintViolationException("cannot remove root node");
        }
        NodeId targetId = (NodeId) targetState.getId();
        NodeState parentState = getNodeState(parentId);
        Path parentPath = hierMgr.getPath(parentId);

        // 1. locking

        if ((options & CHECK_LOCK) == CHECK_LOCK) {
            // make sure there's no foreign lock on parent node
            getLockManager().checkLock(parentPath, session);
        }

        // 2. versioning status

        if ((options & CHECK_VERSIONING) == CHECK_VERSIONING) {
            // make sure parent node is checked-out
            verifyCheckedOut(parentPath);
        }

        // 3. access rights

        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            AccessManager accessMgr = session.getAccessManager();
            try {
                // make sure current session is granted read access on parent node
                if (!accessMgr.isGranted(targetId, AccessManager.READ)) {
                    throw new PathNotFoundException(hierMgr.safeGetJCRPath(targetId));
                }
                // make sure current session is allowed to remove target node
                if (!accessMgr.isGranted(targetId, AccessManager.REMOVE)) {
                    throw new AccessDeniedException(hierMgr.safeGetJCRPath(targetId)
                            + ": not allowed to remove node");
                }
            } catch (ItemNotFoundException infe) {
                String msg = "internal error: failed to check access rights for "
                        + hierMgr.safeGetJCRPath(targetId);
                log.debug(msg);
                throw new RepositoryException(msg, infe);
            }
        }

        // 4. node type constraints

        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            NodeTypeRegistry ntReg = rep.getNodeTypeRegistry();
            NodeDef parentDef = ntReg.getNodeDef(parentState.getDefinitionId());
            if (parentDef.isProtected()) {
                throw new ConstraintViolationException(hierMgr.safeGetJCRPath(parentId)
                        + ": cannot remove child node of protected parent node");
            }
            NodeDef targetDef = ntReg.getNodeDef(targetState.getDefinitionId());
            if (targetDef.isMandatory()) {
                throw new ConstraintViolationException(hierMgr.safeGetJCRPath(targetId)
                        + ": cannot remove mandatory node");
            }
            if (targetDef.isProtected()) {
                throw new ConstraintViolationException(hierMgr.safeGetJCRPath(targetId)
                        + ": cannot remove protected node");
            }
        }

        // 5. referential integrity

        if ((options & CHECK_REFERENCES) == CHECK_REFERENCES) {
            EffectiveNodeType ent = getEffectiveNodeType(targetState);
            if (ent.includesNodeType(MIX_REFERENCEABLE)) {
                NodeReferencesId refsId = new NodeReferencesId(targetState.getUUID());
                if (stateMgr.hasNodeReferences(refsId)) {
                    try {
                        NodeReferences refs = stateMgr.getNodeReferences(refsId);
                        if (refs.hasReferences()) {
                            throw new ReferentialIntegrityException(
                                    hierMgr.safeGetJCRPath(targetId)
                                    + ": cannot remove node with references");
                        }
                    } catch (ItemStateException ise) {
                        String msg = "internal error: failed to check references on "
                                + hierMgr.safeGetJCRPath(targetId);
                        log.error(msg, ise);
                        throw new RepositoryException(msg, ise);
                    }
                }
            }
        }
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
        HashSet set = new HashSet(state.getMixinTypeNames());
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
     * Helper method that finds the applicable definition for a child node with
     * the given name and node type in the parent node's node type and
     * mixin types.
     *
     * @param name
     * @param nodeTypeName
     * @param parentState
     * @return a <code>NodeDef</code>
     * @throws ConstraintViolationException if no applicable child node definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    public NodeDef findApplicableNodeDefinition(QName name,
                                                QName nodeTypeName,
                                                NodeState parentState)
            throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicableChildNodeDef(name, nodeTypeName);
    }

    /**
     * Helper method that finds the applicable definition for a property with
     * the given name, type and multiValued characteristic in the parent node's
     * node type and mixin types.
     *
     * @param name
     * @param type
     * @param multiValued
     * @param parentState
     * @return a <code>PropDef</code>
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    public PropDef findApplicablePropertyDefinition(QName name,
                                                    int type,
                                                    boolean multiValued,
                                                    NodeState parentState)
            throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicablePropertyDef(name, type, multiValued);
    }

    /**
     * Unlinks the specified node state from all its parents and recursively
     * removes it including its properties and child nodes.
     * <p/>
     * <b>Precondition:</b> the state manager of this workspace needs to be in
     * edit mode.
     * todo duplicate code in WorkspaceImporter; consolidate in WorkspaceOperations class
     *
     * @param targetState
     * @throws RepositoryException if an error occurs
     */
    private void removeNodeState(NodeState targetState)
            throws RepositoryException {

        // copy list to avoid ConcurrentModificationException
        ArrayList parentUUIDs = new ArrayList(targetState.getParentUUIDs());
        Iterator iter = parentUUIDs.iterator();
        while (iter.hasNext()) {
            String parentUUID = (String) iter.next();
            NodeId parentId = new NodeId(parentUUID);

            // unlink node state from this parent
            unlinkNodeState(targetState, parentUUID);

            // remove child node entries
            NodeState parent = getNodeState(parentId);
            // use temp array to avoid ConcurrentModificationException
            ArrayList tmp =
                    new ArrayList(parent.getChildNodeEntries(targetState.getUUID()));
            // remove from tail to avoid problems with same-name siblings
            for (int i = tmp.size() - 1; i >= 0; i--) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) tmp.get(i);
                parent.removeChildNodeEntry(entry.getName(), entry.getIndex());
            }
            // store parent
            stateMgr.store(parent);
        }
    }

    /**
     * Unlinks the given node state from the specified parent i.e. removes
     * <code>parentUUID</code> from its list of parents. If as a result
     * the given node state would be orphaned it will be recursively removed
     * including its properties and child nodes.
     * <p/>
     * Note that the child node entry refering to <code>targetState</code> is
     * <b><i>not</i></b> automatically removed from <code>targetState</code>'s
     * parent denoted by <code>parentUUID</code>.
     * <p/>
     * <b>Precondition:</b> the state manager of this workspace needs to be in
     * edit mode.
     * todo duplicate code in WorkspaceImporter; consolidate in WorkspaceOperations class
     *
     * @param targetState
     * @param parentUUID
     * @throws RepositoryException if an error occurs
     */
    private void unlinkNodeState(NodeState targetState, String parentUUID)
            throws RepositoryException {

        // check if this node state would be orphaned after unlinking it from parent
        ArrayList parentUUIDs = new ArrayList(targetState.getParentUUIDs());
        parentUUIDs.remove(parentUUID);
        boolean orphaned = parentUUIDs.isEmpty();

        if (orphaned) {
            // remove child nodes
            // use temp array to avoid ConcurrentModificationException
            ArrayList tmp = new ArrayList(targetState.getChildNodeEntries());
            // remove from tail to avoid problems with same-name siblings
            for (int i = tmp.size() - 1; i >= 0; i--) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) tmp.get(i);
                NodeId nodeId = new NodeId(entry.getUUID());
                try {
                    NodeState nodeState = (NodeState) stateMgr.getItemState(nodeId);
                    // check if child node can be removed
                    // (access rights, locking & versioning status)
                    checkRemoveNode(nodeState, (NodeId) targetState.getId(),
                            CHECK_ACCESS | CHECK_LOCK | CHECK_VERSIONING);
                    // unlink child node (recursive)
                    unlinkNodeState(nodeState, targetState.getUUID());
                } catch (ItemStateException ise) {
                    String msg = "internal error: failed to retrieve state of "
                            + nodeId;
                    log.debug(msg);
                    throw new RepositoryException(msg, ise);
                }
                // remove child node entry
                targetState.removeChildNodeEntry(entry.getName(), entry.getIndex());
            }

            // remove properties
            // use temp array to avoid ConcurrentModificationException
            tmp = new ArrayList(targetState.getPropertyEntries());
            for (int i = 0; i < tmp.size(); i++) {
                NodeState.PropertyEntry entry = (NodeState.PropertyEntry) tmp.get(i);
                PropertyId propId =
                        new PropertyId(targetState.getUUID(), entry.getName());
                try {
                    PropertyState propState =
                            (PropertyState) stateMgr.getItemState(propId);
                    // remove property entry
                    targetState.removePropertyEntry(propId.getName());
                    // destroy property state
                    stateMgr.destroy(propState);
                } catch (ItemStateException ise) {
                    String msg = "internal error: failed to retrieve state of "
                            + propId;
                    log.debug(msg);
                    throw new RepositoryException(msg, ise);
                }
            }
        }

        // now actually do unlink target state from specified parent state
        // (i.e. remove uuid of parent state from target state's parent list)
        targetState.removeParentUUID(parentUUID);

        if (orphaned) {
            // destroy target state (pass overlayed state since target state
            // might have been modified during unlinking)
            stateMgr.destroy(targetState.getOverlayedState());
        } else {
            // store target state
            stateMgr.store(targetState);
        }
    }

    /**
     * Recursively copies the specified node state including its properties and
     * child nodes.
     * <p/>
     * <b>Precondition:</b> the state manager of <code>this</code> workspace
     * needs to be in edit mode.
     *
     * @param srcState
     * @param parentUUID
     * @param srcStateMgr
     * @param srcAccessMgr
     * @param flag         one of
     *                     <ul>
     *                     <li><code>COPY</code></li>
     *                     <li><code>CLONE</code></li>
     *                     <li><code>CLONE_REMOVE_EXISTING</code></li>
     *                     </ul>
     * @param refTracker   tracks uuid mappings and processed reference properties
     * @return a deep copy of the given node state and its children
     * @throws RepositoryException if an error occurs
     */
    private NodeState copyNodeState(NodeState srcState,
                                    String parentUUID,
                                    ItemStateManager srcStateMgr,
                                    AccessManager srcAccessMgr,
                                    int flag,
                                    ReferenceChangeTracker refTracker)
            throws RepositoryException {

        NodeState newState;
        try {
            String uuid;
            NodeId id;
            EffectiveNodeType ent = getEffectiveNodeType(srcState);
            boolean referenceable = ent.includesNodeType(MIX_REFERENCEABLE);
            switch (flag) {
                case COPY:
                    // always create new uuid
                    uuid = UUID.randomUUID().toString();    // create new version 4 uuid
                    if (referenceable) {
                        // remember uuid mapping
                        refTracker.mappedUUID(srcState.getUUID(), uuid);
                    }
                    break;
                case CLONE:
                    if (!referenceable) {
                        // non-referenceable node: always create new uuid
                        uuid = UUID.randomUUID().toString();    // create new version 4 uuid
                        break;
                    }
                    // use same uuid as source node
                    uuid = srcState.getUUID();
                    id = new NodeId(uuid);
                    if (stateMgr.hasItemState(id)) {
                        // node with this uuid already exists
                        throw new ItemExistsException(hierMgr.safeGetJCRPath(id));
                    }
                    break;
                case CLONE_REMOVE_EXISTING:
                    if (!referenceable) {
                        // non-referenceable node: always create new uuid
                        uuid = UUID.randomUUID().toString();    // create new version 4 uuid
                        break;
                    }
                    // use same uuid as source node
                    uuid = srcState.getUUID();
                    id = new NodeId(uuid);
                    if (stateMgr.hasItemState(id)) {
                        NodeState existingState = (NodeState) stateMgr.getItemState(id);
                        // make sure existing node is not the parent
                        // or an ancestor thereof
                        NodeId newParentId = new NodeId(parentUUID);
                        Path p0 = hierMgr.getPath(newParentId);
                        Path p1 = hierMgr.getPath(id);
                        try {
                            if (p1.equals(p0) || p1.isAncestorOf(p0)) {
                                String msg = "cannot remove ancestor node";
                                log.debug(msg);
                                throw new RepositoryException(msg);
                            }
                        } catch (MalformedPathException mpe) {
                            // should never get here...
                            String msg = "internal error: failed to determine degree of relationship";
                            log.error(msg, mpe);
                            throw new RepositoryException(msg, mpe);
                        }

                        // check if existing can be removed
                        checkRemoveNode(existingState, CHECK_ACCESS | CHECK_LOCK
                                | CHECK_VERSIONING | CHECK_CONSTRAINTS);

                        // do remove existing
                        removeNodeState(existingState);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unknown flag");
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
                NodeId nodeId = new NodeId(entry.getUUID());
                if (!srcAccessMgr.isGranted(nodeId, AccessManager.READ)) {
                    continue;
                }
                NodeState srcChildState = (NodeState) srcStateMgr.getItemState(nodeId);
                // recursive copying of child node
                NodeState newChildState = copyNodeState(srcChildState, uuid,
                        srcStateMgr, srcAccessMgr, flag, refTracker);
                // store new child node
                stateMgr.store(newChildState);
                // add new child node entry to new node
                newState.addChildNodeEntry(entry.getName(), newChildState.getUUID());
            }
            // copy properties
            iter = srcState.getPropertyEntries().iterator();
            while (iter.hasNext()) {
                NodeState.PropertyEntry entry = (NodeState.PropertyEntry) iter.next();
                PropertyId propId = new PropertyId(srcState.getUUID(), entry.getName());
                if (!srcAccessMgr.isGranted(propId, AccessManager.READ)) {
                    continue;
                }
                PropertyState srcChildState =
                        (PropertyState) srcStateMgr.getItemState(propId);
                PropertyState newChildState =
                        copyPropertyState(srcChildState, uuid, entry.getName());
                if (newChildState.getType() == PropertyType.REFERENCE) {
                    refTracker.processedReference(newChildState);
                }
                // store new property
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

    /**
     * Copies the specified property state.
     * <p/>
     * <b>Precondition:</b> the state manager of this workspace needs to be in
     * edit mode.
     *
     * @param srcState
     * @param parentUUID
     * @param propName
     * @return
     * @throws RepositoryException
     */
    private PropertyState copyPropertyState(PropertyState srcState,
                                            String parentUUID,
                                            QName propName)
            throws RepositoryException {

        // @todo special handling required for properties with special semantics
        // (e.g. those defined by mix:versionable, mix:lockable, et.al.)
        PropertyState newState = stateMgr.createNew(propName, parentUUID);
        PropDefId defId = srcState.getDefinitionId();
        newState.setDefinitionId(defId);
        newState.setType(srcState.getType());
        newState.setMultiValued(srcState.isMultiValued());
        InternalValue[] values = srcState.getValues();
        if (values != null) {
            InternalValue[] newValues = new InternalValue[values.length];
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    newValues[i] = values[i].createCopy();
                } else {
                    newValues[i] = null;
                }
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

    /**
     * @param srcAbsPath
     * @param srcWsp
     * @param destAbsPath
     * @param flag        one of
     *                    <ul>
     *                    <li><code>COPY</code></li>
     *                    <li><code>CLONE</code></li>
     *                    <li><code>CLONE_REMOVE_EXISTING</code></li>
     *                    </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws LockException
     * @throws RepositoryException
     */
    private void internalCopy(String srcAbsPath,
                              WorkspaceImpl srcWsp,
                              String destAbsPath,
                              int flag)
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

        // 2. check access rights, lock status, node type constraints, etc.

        checkAddNode(destParentState, destName.getName(),
                srcState.getNodeTypeName(), CHECK_ACCESS | CHECK_LOCK
                | CHECK_VERSIONING | CHECK_CONSTRAINTS);
        // check read access right on source node
        // use access manager of source workspace/session
        AccessManager srcAccessMgr =
                ((SessionImpl) srcWsp.getSession()).getAccessManager();
        try {
            if (!srcAccessMgr.isGranted(srcState.getId(), AccessManager.READ)) {
                throw new PathNotFoundException(srcAbsPath);
            }
        } catch (ItemNotFoundException infe) {
            String msg = "internal error: failed to check access rights for "
                    + srcAbsPath;
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        }

        // 3. do copy operation (modify and persist affected states)

        boolean succeeded = false;
        try {
            stateMgr.edit();

            ReferenceChangeTracker refTracker = new ReferenceChangeTracker();

            // create deep copy of source node state
            NodeState newState = copyNodeState(srcState, destParentState.getUUID(),
                    srcWsp.getItemStateManager(), srcAccessMgr, flag, refTracker);

            // add to new parent
            destParentState.addChildNodeEntry(destName.getName(), newState.getUUID());

            // change definition (id) of new node
            NodeDef newNodeDef =
                    findApplicableNodeDefinition(destName.getName(),
                            srcState.getNodeTypeName(), destParentState);
            newState.setDefinitionId(newNodeDef.getId());

            // adjust references that refer to uuid's which have been mapped to
            // newly generated uuid's on copy/clone
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
                    String original = ((UUID) val.internalValue()).toString();
                    String adjusted = refTracker.getMappedUUID(original);
                    if (adjusted != null) {
                        newVals[i] = InternalValue.create(UUID.fromString(adjusted));
                        modified = true;
                    } else {
                        // reference doesn't need adjusting, just copy old value
                        newVals[i] = val;
                    }
                }
                if (modified) {
                    prop.setValues(newVals);
                    stateMgr.store(prop);
                }
            }
            refTracker.clear();

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
            int mode = CLONE;
            if (removeExisting) {
                mode = CLONE_REMOVE_EXISTING;
            }
            internalCopy(srcAbsPath, srcWsp, destAbsPath, mode);
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
        internalCopy(srcAbsPath, this, destAbsPath, COPY);
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
            internalCopy(srcAbsPath, srcWsp, destAbsPath, COPY);
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

        // 2. check if target state can be removed from old/added to new parent

        checkRemoveNode(targetState, (NodeId) srcParentState.getId(),
                CHECK_ACCESS | CHECK_LOCK | CHECK_VERSIONING | CHECK_CONSTRAINTS);
        checkAddNode(destParentState, destName.getName(),
                targetState.getNodeTypeName(), CHECK_ACCESS | CHECK_LOCK
                | CHECK_VERSIONING | CHECK_CONSTRAINTS);

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
            NodeDef newTargetDef =
                    findApplicableNodeDefinition(destName.getName(),
                            targetState.getNodeTypeName(), destParentState);
            targetState.setDefinitionId(newTargetDef.getId());

            // remove from old parent
            if (!renameOnly) {
                targetState.removeParentUUID(srcParentState.getUUID());
            }

            int srcNameIndex = srcName.getIndex();
            if (srcNameIndex == 0) {
                srcNameIndex = 1;
            }
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
                ObservationManagerFactory factory =
                    rep.getObservationManagerFactory(wspConfig.getName());
                obsMgr = factory.createObservationManager(session, session.getItemManager());
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

        // todo: perform restore operations direct on the node states

        // check state of this instance
        sanityCheck();

        // add all versions to map of versions to restore
        final HashMap toRestore = new HashMap();
        for (int i = 0; i < versions.length; i++) {
            VersionImpl v = (VersionImpl) versions[i];
            VersionHistory vh = v.getContainingHistory();
            // check for collision
            if (toRestore.containsKey(vh.getUUID())) {
                throw new VersionException("Unable to restore. Two or more versions have same version history.");
            }
            toRestore.put(vh.getUUID(), v);
        }

        // create a version selector to the set of versions
        VersionSelector vsel = new VersionSelector() {
            public Version select(VersionHistory versionHistory) throws RepositoryException {
                // try to select version as specified
                Version v = (Version) toRestore.get(versionHistory.getUUID());
                if (v == null) {
                    // select latest one
                    v = GenericVersionSelector.selectByDate(versionHistory, null);
                }
                return v;
            }
        };

        // check for pending changes
        if (session.hasPendingChanges()) {
            String msg = "Unable to restore version. Session has pending changes.";
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }

        try {
            // now restore all versions that have a node in the ws
            int numRestored = 0;
            while (toRestore.size() > 0) {
                InternalVersion[] restored = null;
                Iterator iter = toRestore.values().iterator();
                while (iter.hasNext()) {
                    VersionImpl v = (VersionImpl) iter.next();
                    try {
                        NodeImpl node = (NodeImpl) session.getNodeByUUID(v.getFrozenNode().getFrozenUUID());
                        restored = node.internalRestore(v.getInternalVersion(), vsel, removeExisting);
                        // remove restored versions from set
                        for (int i = 0; i < restored.length; i++) {
                            toRestore.remove(restored[i].getVersionHistory().getId());
                        }
                        numRestored += restored.length;
                        break;
                    } catch (ItemNotFoundException e) {
                        // ignore
                    }
                }
                if (restored == null) {
                    if (numRestored == 0) {
                        throw new VersionException(
                                "Unable to restore. At least one version needs"
                                + " existing versionable node in workspace.");
                    } else {
                        throw new VersionException(
                                "Unable to restore. All versions with non"
                                + " existing versionable nodes need parent.");
                    }
                }
            }
        } catch (RepositoryException e) {
            // revert session
            try {
                log.error("reverting changes applied during restore...");
                session.refresh(false);
            } catch (RepositoryException e1) {
                // ignore this
            }
            throw e;
        }
        session.save();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return session.getWorkspaceNames();
    }

    /**
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler(String parentAbsPath,
                                                  int uuidBehavior)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {

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

        Importer importer = new WorkspaceImporter(parentState, this,
                rep.getNodeTypeRegistry(), uuidBehavior);
        return new ImportHandler(importer, session.getNamespaceResolver(),
                rep.getNamespaceRegistry());
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
            // being paranoid...
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
            parser.setFeature("http://xml.org/sax/features/namespace-prefixes",
                    false);

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

