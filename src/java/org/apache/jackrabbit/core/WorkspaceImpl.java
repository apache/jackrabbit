/*
 * Copyright 2004 The Apache Software Foundation.
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
import org.apache.jackrabbit.core.xml.DocViewSAXEventGenerator;
import org.apache.jackrabbit.core.xml.SysViewSAXEventGenerator;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.*;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import java.io.IOException;
import java.io.OutputStream;
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
    protected final PersistentItemStateProvider persistentStateMgr;

    /**
     * The reference mgr associated with the workspace represented by <i>this</i>
     * <code>Workspace</code> instance.
     */
    protected final ReferenceManager refMgr;

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
     * @param persistentStateMgr
     * @param rep
     * @param session
     */
    WorkspaceImpl(WorkspaceConfig wspConfig, PersistentItemStateProvider persistentStateMgr,
                  ReferenceManager refMgr, RepositoryImpl rep, SessionImpl session) {
        this.wspConfig = wspConfig;
        this.rep = rep;
        this.persistentStateMgr = persistentStateMgr;
        this.refMgr = refMgr;
        hierMgr = new HierarchyManagerImpl(rep.getRootNodeUUID(), persistentStateMgr, session.getNamespaceResolver());
        this.session = session;
    }

    RepositoryImpl getRepository() {
        return rep;
    }

    public PersistentItemStateProvider getPersistentStateManager() {
        return persistentStateMgr;
    }

    ReferenceManager getReferenceManager() {
        return refMgr;
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
     * @throws RepositoryException if this workspace has been rendered invalid
     * for some reason
     */
    protected void sanityCheck() throws RepositoryException {
        // check session status
        session.sanityCheck();
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
    protected static PersistentNodeState getNodeState(String nodePath,
                                                      NamespaceResolver nsResolver,
                                                      HierarchyManagerImpl hierMgr,
                                                      PersistentItemStateProvider stateMgr)
            throws PathNotFoundException, RepositoryException {
        try {
            return getNodeState(Path.create(nodePath, nsResolver, true), hierMgr, stateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + nodePath;
            log.error(msg, mpe);
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
    protected static PersistentNodeState getParentNodeState(String path,
                                                            NamespaceResolver nsResolver,
                                                            HierarchyManagerImpl hierMgr,
                                                            PersistentItemStateProvider stateMgr)

            throws PathNotFoundException, RepositoryException {
        try {
            return getNodeState(Path.create(path, nsResolver, true).getAncestor(1), hierMgr, stateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + path;
            log.error(msg, mpe);
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
    protected static PersistentNodeState getNodeState(Path nodePath,
                                                      HierarchyManagerImpl hierMgr,
                                                      PersistentItemStateProvider stateMgr)
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
            log.error(msg, ise);
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
    protected static PersistentNodeState getNodeState(NodeId id,
                                                      PersistentItemStateProvider stateMgr)
            throws NoSuchItemStateException, ItemStateException {
        return (PersistentNodeState) stateMgr.getItemState(id);
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
                                       PersistentItemStateProvider stateMgr)
            throws ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, RepositoryException {

        Path parentPath = nodePath.getAncestor(1);
        PersistentNodeState parentState = getNodeState(parentPath, hierMgr, stateMgr);

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
            log.error(msg, infe);
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
                log.error(msg, ise);
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
                                          PersistentItemStateProvider stateMgr)
            throws ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, RepositoryException {

        PersistentNodeState targetState = getNodeState(nodePath, hierMgr, stateMgr);
        Path parentPath = nodePath.getAncestor(1);
        PersistentNodeState parentState = getNodeState(parentPath, hierMgr, stateMgr);

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
            log.error(msg, infe);
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
        HashSet set = new HashSet(((NodeState) state).getMixinTypeNames());
        // primary type
        set.add(state.getNodeTypeName());
        try {
            return ntReg.buildEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node " + state.getUUID();
            log.error(msg, ntce);
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

    private static PersistentNodeState copyNodeState(NodeState srcState,
                                                     String parentUUID,
                                                     NodeTypeRegistry ntReg,
                                                     HierarchyManagerImpl srcHierMgr,
                                                     PersistentItemStateProvider srcStateMgr,
                                                     PersistentItemStateProvider destStateMgr,
                                                     boolean clone)
            throws RepositoryException {
        PersistentNodeState newState;
        try {
            String uuid;
            if (clone) {
                uuid = srcState.getUUID();
            } else {
                uuid = UUID.randomUUID().toString();	// create new version 4 uuid
            }
            newState = destStateMgr.createNodeState(uuid, srcState.getNodeTypeName(), parentUUID);
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
                PersistentNodeState newChildState = copyNodeState(srcChildState, uuid,
                        ntReg, srcHierMgr, srcStateMgr, destStateMgr, clone);
                // persist new child node
                newChildState.store();
                // add new child node entry to new node
                newState.addChildNodeEntry(entry.getName(), newChildState.getUUID());
            }
            // copy properties
            iter = srcState.getPropertyEntries().iterator();
            while (iter.hasNext()) {
                NodeState.PropertyEntry entry = (NodeState.PropertyEntry) iter.next();
                PropertyState srcChildState = (PropertyState) srcStateMgr.getItemState(new PropertyId(srcState.getUUID(), entry.getName()));
                PersistentPropertyState newChildState = copyPropertyState(srcChildState, uuid, entry.getName(),
                        ntReg, srcHierMgr, srcStateMgr, destStateMgr);
                // persist new property
                newChildState.store();
                // add new property entry to new node
                newState.addPropertyEntry(entry.getName());
            }
            return newState;
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to copy state of " + srcHierMgr.safeGetJCRPath(srcState.getId());
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }
    }

    private static PersistentPropertyState copyPropertyState(PropertyState srcState,
                                                             String parentUUID,
                                                             QName propName,
                                                             NodeTypeRegistry ntReg,
                                                             HierarchyManagerImpl srcHierMgr,
                                                             PersistentItemStateProvider srcStateMgr,
                                                             PersistentItemStateProvider destStateMgr)
            throws RepositoryException {
        // @todo special handling required for properties with special semantics (e.g. those defined by mix:versionable, mix:lockable, et.al.)
        PersistentPropertyState newState;
        try {
            newState = destStateMgr.createPropertyState(parentUUID, propName);
            PropDefId defId = srcState.getDefinitionId();
            newState.setDefinitionId(defId);
            newState.setType(srcState.getType());
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
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to copy state of " + srcHierMgr.safeGetJCRPath(srcState.getId());
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }
    }

    private static void internalCopy(String srcAbsPath,
                                     PersistentItemStateProvider srcStateMgr,
                                     HierarchyManagerImpl srcHierMgr,
                                     String destAbsPath,
                                     PersistentItemStateProvider destStateMgr,
                                     HierarchyManagerImpl destHierMgr,
                                     AccessManagerImpl accessMgr,
                                     NamespaceResolver nsResolver,
                                     NodeTypeRegistry ntReg,
                                     boolean clone)
            throws ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, RepositoryException {

        // 1. check paths & retrieve state

        Path srcPath;
        PersistentNodeState srcState;
        try {
            srcPath = Path.create(srcAbsPath, nsResolver, true);
            srcState = getNodeState(srcPath, srcHierMgr, srcStateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + srcAbsPath;
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }

        Path destPath;
        Path.PathElement destName;
        Path destParentPath;
        PersistentNodeState destParentState;
        try {
            destPath = Path.create(destAbsPath, nsResolver, true);
            destName = destPath.getNameElement();
            destParentPath = destPath.getAncestor(1);
            destParentState = getNodeState(destParentPath, destHierMgr, destStateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + destAbsPath;
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }
        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = destAbsPath + ": invalid destination path (subscript in name element is not allowed)";
            log.error(msg);
            throw new RepositoryException(msg);
        }

        // 2. check access rights & node type constraints

        try {
            // check read access right on source node
            if (!accessMgr.isGranted(srcState.getId(), AccessManager.READ)) {
                throw new PathNotFoundException(srcAbsPath);
            }
        } catch (ItemNotFoundException infe) {
            String msg = "internal error: failed to check access rights for " + srcAbsPath;
            log.error(msg, infe);
            throw new RepositoryException(msg, infe);
        }
        // check node type constraints
        checkAddNode(destPath, srcState.getNodeTypeName(), ntReg, accessMgr, destHierMgr, destStateMgr);
/*
	// check if target node needs to be inserted at specific location in child node entries list
	boolean insertTargetEntry = false;
	int ind = destName.getIndex();
	if (ind > 0) {
	    // target name contains subscript:
	    // validate subscript
	    List sameNameSibs = destParentState.getChildNodeEntries(destName.getName());
	    if (ind > sameNameSibs.size() + 1) {
		String msg = "invalid subscript in name: " + destAbsPath;
		log.error(msg);
		throw new RepositoryException(msg);
	    }
	    insertTargetEntry = (ind < sameNameSibs.size() + 1) ? true : false;
	}
	if (insertTargetEntry) {
	    // check hasOrderableChildNodes flag
	    if (!ntReg.getNodeTypeDef(destParentState.getNodeTypeName()).hasOrderableChildNodes()) {
		throw new ConstraintViolationException(destAbsPath + ": parent node's node type does not allow explicit ordering of child nodes");
	    }
	}
*/
        // 3. do copy operation (modify and persist affected states)

        // create deep copy of source node state
        PersistentNodeState newState = copyNodeState(srcState, destParentState.getUUID(),
                ntReg, srcHierMgr, srcStateMgr, destStateMgr, clone);

        // add to new parent
        destParentState.addChildNodeEntry(destName.getName(), newState.getUUID());
/*
	if (!insertTargetEntry) {
	    // append target entry
	    destParentState.addChildNodeEntry(destName.getName(), newState.getUUID());
	} else {
	    // insert target entry at specified position
	    Iterator iter = new ArrayList(destParentState.getChildNodeEntries()).iterator();
	    destParentState.removeAllChildNodeEntries();
	    while (iter.hasNext()) {
		NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
		if (entry.getName().equals(destName.getName()) &&
			entry.getIndex() == destName.getIndex()) {
		    destParentState.addChildNodeEntry(destName.getName(), newState.getUUID());
		}
		destParentState.addChildNodeEntry(entry.getName(), entry.getUUID());
	    }
	}
*/
        // change definition (id) of new node
        ChildNodeDef newNodeDef = findApplicableDefinition(destName.getName(), srcState.getNodeTypeName(), destParentState, ntReg);
        newState.setDefinitionId(new NodeDefId(newNodeDef));

        // persist states
        try {
            newState.store();
            destParentState.store();
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to persist state of " + destAbsPath;
            log.error(msg, ise);
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
        return rep.getNamespaceRegistry();
    }

    /**
     * @see Workspace#getNodeTypeManager
     */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        return session.getNodeTypeManager();
    }

    /**
     * @see Workspace#clone(String, String, String)
     */
    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            AccessDeniedException, PathNotFoundException,
            ItemExistsException, RepositoryException {
        // clone (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath
        PersistentItemStateProvider srcStateMgr = rep.getWorkspaceStateManager(srcWorkspace);
        // FIXME need to setup a hierarchy manager for source workspace
        HierarchyManagerImpl srcHierMgr = new HierarchyManagerImpl(rep.getRootNodeUUID(), srcStateMgr, session.getNamespaceResolver());
        // do cross-workspace copy
        internalCopy(srcAbsPath, srcStateMgr, srcHierMgr,
                destAbsPath, persistentStateMgr, hierMgr,
                session.getAccessManager(), session.getNamespaceResolver(),
                rep.getNodeTypeRegistry(), true);
    }

    /**
     * @see Workspace#copy(String, String)
     */
    public void copy(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, RepositoryException {
        // do intra-workspace copy
        internalCopy(srcAbsPath, persistentStateMgr, hierMgr,
                destAbsPath, persistentStateMgr, hierMgr,
                session.getAccessManager(), session.getNamespaceResolver(),
                rep.getNodeTypeRegistry(), false);
    }

    /**
     * @see Workspace#copy(String, String, String)
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            RepositoryException {
        // copy (i.e. pull) subtree at srcAbsPath from srcWorkspace
        // to 'this' workspace at destAbsPath
        PersistentItemStateProvider srcStateMgr = rep.getWorkspaceStateManager(srcWorkspace);
        // FIXME need to setup a hierarchy manager for source workspace
        HierarchyManagerImpl srcHierMgr = new HierarchyManagerImpl(rep.getRootNodeUUID(), srcStateMgr, session.getNamespaceResolver());
        // do cross-workspace copy
        internalCopy(srcAbsPath, srcStateMgr, srcHierMgr,
                destAbsPath, persistentStateMgr, hierMgr,
                session.getAccessManager(), session.getNamespaceResolver(),
                rep.getNodeTypeRegistry(), false);
    }

    /**
     * @see Workspace#move
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, RepositoryException {

        // intra-workspace move...

        // 1. check paths & retrieve state

        Path srcPath;
        Path.PathElement srcName;
        Path srcParentPath;
        PersistentNodeState targetState;
        PersistentNodeState srcParentState;
        try {
            srcPath = Path.create(srcAbsPath, session.getNamespaceResolver(), true);
            srcName = srcPath.getNameElement();
            srcParentPath = srcPath.getAncestor(1);
            targetState = getNodeState(srcPath, hierMgr, persistentStateMgr);
            srcParentState = getNodeState(srcParentPath, hierMgr, persistentStateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + srcAbsPath;
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }

        Path destPath;
        Path.PathElement destName;
        Path destParentPath;
        PersistentNodeState destParentState;
        try {
            destPath = Path.create(destAbsPath, session.getNamespaceResolver(), true);
            destName = destPath.getNameElement();
            destParentPath = destPath.getAncestor(1);
            destParentState = getNodeState(destParentPath, hierMgr, persistentStateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + destAbsPath;
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }
        int ind = destName.getIndex();
        if (ind > 0) {
            // subscript in name element
            String msg = destAbsPath + ": invalid destination path (subscript in name element is not allowed)";
            log.error(msg);
            throw new RepositoryException(msg);
        }

        // 2. check node type constraints & access rights

        checkRemoveNode(srcPath, rep.getNodeTypeRegistry(), session.getAccessManager(), hierMgr, persistentStateMgr);
        checkAddNode(destPath, targetState.getNodeTypeName(),
                rep.getNodeTypeRegistry(), session.getAccessManager(),
                hierMgr, persistentStateMgr);
/*
	// check if target node needs to be inserted at specific location in child node entries list
	boolean insertTargetEntry = false;
	int ind = destName.getIndex();
	if (ind > 0) {
	    // target name contains subscript:
	    // validate subscript
	    List sameNameSibs = destParentState.getChildNodeEntries(destName.getName());
	    if (ind > sameNameSibs.size() + 1) {
		String msg = "invalid subscript in name: " + destAbsPath;
		log.error(msg);
		throw new RepositoryException(msg);
	    }
	    insertTargetEntry = (ind < sameNameSibs.size() + 1) ? true : false;
	}
	if (insertTargetEntry) {
	    // check hasOrderableChildNodes flag
	    if (!rep.getNodeTypeRegistry().getNodeTypeDef(destParentState.getNodeTypeName()).hasOrderableChildNodes()) {
		throw new ConstraintViolationException(destAbsPath + ": parent node's node type does not allow explicit ordering of child nodes");
	    }
	}
*/
        // 3. do move operation (modify and persist affected states)

        boolean renameOnly = srcParentState.getUUID().equals(destParentState.getUUID());

        // add to new parent
        if (!renameOnly) {
            targetState.addParentUUID(destParentState.getUUID());
        }
        destParentState.addChildNodeEntry(destName.getName(), targetState.getUUID());
/*
	if (!insertTargetEntry) {
	    // append target entry
	    destParentState.addChildNodeEntry(destName.getName(), targetState.getUUID());
	} else {
	    // insert target entry at specified position
	    Iterator iter = new ArrayList(destParentState.getChildNodeEntries()).iterator();
	    destParentState.removeAllChildNodeEntries();
	    while (iter.hasNext()) {
		NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
		if (entry.getName().equals(destName.getName()) &&
			entry.getIndex() == destName.getIndex()) {
		    destParentState.addChildNodeEntry(destName.getName(), targetState.getUUID());
		}
		destParentState.addChildNodeEntry(entry.getName(), entry.getUUID());
	    }
	}
*/
        // change definition (id) of target node
        ChildNodeDef newTargetDef = findApplicableDefinition(destName.getName(), targetState.getNodeTypeName(), destParentState, rep.getNodeTypeRegistry());
        targetState.setDefinitionId(new NodeDefId(newTargetDef));

        // remove from old parent
        if (!renameOnly) {
            targetState.removeParentUUID(srcParentState.getUUID());
        }
        int srcNameIndex = srcName.getIndex() == 0 ? 1 : srcName.getIndex();
/*
	// if the net result of the move is changing the position of a child node
	// among its same-same siblings, the subscript of the child node entry
	// to be removed might need adjustment
	if (renameOnly && srcName.getName().equals(destName.getName()) &&
		insertTargetEntry && destName.getIndex() <= srcNameIndex) {
	    srcNameIndex++;
	}
*/
        srcParentState.removeChildNodeEntry(srcName.getName(), srcNameIndex);

        // persist states
        try {
            targetState.store();
            if (renameOnly) {
                srcParentState.store();
            } else {
                destParentState.store();
                srcParentState.store();
            }
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to persist state of " + destAbsPath;
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * @see Workspace#getObservationManager
     */
    public synchronized ObservationManager getObservationManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (obsMgr == null) {
            try {
                obsMgr = rep.getObservationManagerFactory(wspConfig.getName()).createObservationManager(session, session.getItemManager());
            } catch (NoSuchWorkspaceException nswe) {
                // should never get here
                String msg = "internal error: failed to instantiate observation manager";
                log.error(msg, nswe);
                throw new RepositoryException(msg, nswe);
            }
        }
        return obsMgr;
    }

    /**
     * @see Workspace#getQueryManager
     */
    public QueryManager getQueryManager() throws RepositoryException {
        if (queryManager == null) {
            try {
                SearchManager searchManager = rep.getSearchManager(wspConfig.getName());
                if (searchManager == null) {
                    throw new UnsupportedOperationException("No search manager configured for this workspace.");
                }
                queryManager = new QueryManagerImpl(session, session.getItemManager(), searchManager);
            } catch (NoSuchWorkspaceException e) {
                String msg = "internal error: failed to instantiate query manager";
                log.error(msg, e);
                return null;
            } catch (RepositoryException e) {
                String msg = "internal error: failed to instantiate query manager";
                log.error(msg, e);
                return null;
            }
        }
        return queryManager;
    }

    /**
     * @see Workspace#restore(Version[])
     */
    public void restore(Version[] versions) throws UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        // @todo implement versioning support
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @see Workspace#exportDocView(String, ContentHandler, boolean, boolean)
     */
    public void exportDocView(String absPath, ContentHandler contentHandler, boolean binaryAsLink, boolean noRecurse)
            throws InvalidSerializedDataException, PathNotFoundException, SAXException, RepositoryException {
        // check path & retrieve state
        Path path;
        Path.PathElement name;
        PersistentNodeState state;
        try {
            path = Path.create(absPath, session.getNamespaceResolver(), true);
            name = path.getNameElement();
            state = getNodeState(path, hierMgr, persistentStateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + absPath;
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }

        // check read access
        if (!session.getAccessManager().isGranted(state.getId(), AccessManager.READ)) {
            throw new PathNotFoundException(absPath);
        }

        new DocViewSAXEventGenerator(state, name.getName(), noRecurse, binaryAsLink,
                persistentStateMgr, (NamespaceRegistryImpl) rep.getNamespaceRegistry(),
                session.getAccessManager(), hierMgr, contentHandler).serialize();
    }

    /**
     * @see Workspace#exportDocView(String, OutputStream, boolean, boolean)
     */
    public void exportDocView(String absPath, OutputStream out, boolean binaryAsLink, boolean noRecurse)
            throws InvalidSerializedDataException, IOException, PathNotFoundException, RepositoryException {
        OutputFormat format = new OutputFormat("xml", "UTF-8", true);
        XMLSerializer serializer = new XMLSerializer(out, format);
        try {
            exportDocView(absPath, serializer.asContentHandler(), binaryAsLink, noRecurse);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        }
    }

    /**
     * @see Workspace#exportSysView(String, ContentHandler, boolean, boolean)
     */
    public void exportSysView(String absPath, ContentHandler contentHandler, boolean binaryAsLink, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        // check path & retrieve state
        Path path;
        Path.PathElement name;
        PersistentNodeState state;
        try {
            path = Path.create(absPath, session.getNamespaceResolver(), true);
            name = path.getNameElement();
            state = getNodeState(path, hierMgr, persistentStateMgr);
        } catch (MalformedPathException mpe) {
            String msg = "invalid path: " + absPath;
            log.error(msg, mpe);
            throw new RepositoryException(msg, mpe);
        }

        // check read access
        if (!session.getAccessManager().isGranted(state.getId(), AccessManager.READ)) {
            throw new PathNotFoundException(absPath);
        }

        new SysViewSAXEventGenerator(state, name.getName(), noRecurse, binaryAsLink,
                persistentStateMgr, (NamespaceRegistryImpl) rep.getNamespaceRegistry(),
                session.getAccessManager(), hierMgr, contentHandler).serialize();
    }

    /**
     * @see Workspace#exportSysView(String, OutputStream, boolean, boolean)
     */
    public void exportSysView(String absPath, OutputStream out, boolean binaryAsLink, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        OutputFormat format = new OutputFormat("xml", "UTF-8", true);
        XMLSerializer serializer = new XMLSerializer(out, format);
        try {
            exportSysView(absPath, serializer.asContentHandler(), binaryAsLink, noRecurse);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        }
    }
}
