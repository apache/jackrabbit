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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.MalformedPathException;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.nodetype.ChildNodeDef;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;
import org.apache.jackrabbit.core.util.Base64;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.log4j.Logger;

import javax.jcr.ItemExistsException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * <code>WorkspaceImporter</code> ...
 */
public class WorkspaceImporter implements Importer, Constants {

    private static Logger log = Logger.getLogger(WorkspaceImporter.class);

    private final NodeState importTarget;
    private final WorkspaceImpl wsp;
    private final NodeTypeRegistry ntReg;
    private final HierarchyManager hierMgr;
    private final UpdatableItemStateManager stateMgr;

    private final int uuidBehavior;

    private boolean aborted;
    private Stack parents;

    /**
     * helper object that keeps track of remapped uuid's and imported reference
     * properties that might need correcting depending on the uuid mappings
     */
    private final ReferenceChangeTracker refTracker;

    /**
     * Creates a <code>WorkspaceImporter</code> instance.
     *
     * @param importTarget
     * @param wsp
     * @param ntReg
     * @param uuidBehavior
     */
    public WorkspaceImporter(NodeState importTarget,
                             WorkspaceImpl wsp,
                             NodeTypeRegistry ntReg,
                             int uuidBehavior) {
        this.importTarget = importTarget;
        this.wsp = wsp;
        this.ntReg = ntReg;

        hierMgr = wsp.getHierarchyManager();
        stateMgr = wsp.getItemStateManager();

        this.uuidBehavior = uuidBehavior;

        aborted = false;

        refTracker = new ReferenceChangeTracker();

        parents = new Stack();
        parents.push(importTarget);
    }

    /**
     * Failsafe translation of internal <code>ItemId</code> to JCR path for
     * use in error messages etc.
     *
     * @param id id to translate
     * @return JCR path
     */
    private String resolveJCRPath(ItemId id) {
        Path path;
        try {
            path = hierMgr.getPath(id);
        } catch (RepositoryException re) {
            log.error(id + ": failed to determine path to");
            // return string representation if id as a fallback
            return id.toString();
        }
        try {
            return path.toJCRPath(((SessionImpl) wsp.getSession()).getNamespaceResolver());
        } catch (NoPrefixDeclaredException npde) {
            log.error("failed to convert " + path.toString() + " to JCR path.");
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }

    protected NodeState createNode(NodeState parent,
                                   QName nodeName,
                                   QName nodeTypeName,
                                   QName[] mixinNames,
                                   String uuid)
            throws RepositoryException {
        ChildNodeDef def =
                wsp.findApplicableNodeDefinition(nodeName, nodeTypeName, parent);
        return createNode(parent, nodeName, nodeTypeName, mixinNames, uuid, def);
    }

    protected NodeState createNode(NodeState parent,
                                   QName nodeName,
                                   QName nodeTypeName,
                                   QName[] mixinNames,
                                   String uuid,
                                   ChildNodeDef def)
            throws RepositoryException {
        // check for name collisions with existing properties
        if (parent.hasPropertyEntry(nodeName)) {
            String msg = "there's already a property with name " + nodeName;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // check for name collisions with existing nodes
        if (!def.allowSameNameSibs() && parent.hasChildNodeEntry(nodeName)) {
            NodeId id = new NodeId(parent.getChildNodeEntry(nodeName, 1).getUUID());
            throw new ItemExistsException(resolveJCRPath(id));
        }
        if (uuid == null) {
            // create new uuid
            uuid = UUID.randomUUID().toString();    // create new version 4 uuid
        }
        if (nodeTypeName == null) {
            // no primary node type specified,
            // try default primary type from definition
            nodeTypeName = def.getDefaultPrimaryType();
            if (nodeTypeName == null) {
                String msg = "an applicable node type could not be determined for "
                        + nodeName;
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        NodeState node = stateMgr.createNew(uuid, nodeTypeName, parent.getUUID());
        if (mixinNames != null && mixinNames.length > 0) {
            node.setMixinTypeNames(new HashSet(Arrays.asList(mixinNames)));
        }
        node.setDefinitionId(new NodeDefId(def));

        // now add new child node entry to parent
        parent.addChildNodeEntry(nodeName, node.getUUID());

        EffectiveNodeType ent = wsp.getEffectiveNodeType(node);

        if (!node.getMixinTypeNames().isEmpty()) {
            // create jcr:mixinTypes property
            PropDef pd = ent.getApplicablePropertyDef(JCR_MIXINTYPES,
                    PropertyType.NAME, true);
            createProperty(node, pd.getName(), pd.getRequiredType(), pd);
        }

        // add 'auto-create' properties defined in node type
        PropDef[] pda = ent.getAutoCreatePropDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            createProperty(node, pd.getName(), pd.getRequiredType(), pd);
        }

        // recursively add 'auto-create' child nodes defined in node type
        ChildNodeDef[] nda = ent.getAutoCreateNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            ChildNodeDef nd = nda[i];
            createNode(node, nd.getName(), nd.getDefaultPrimaryType(),
                    null, null, nd);
        }

        return node;
    }

    protected PropertyState createProperty(NodeState parent,
                                           QName propName,
                                           int type,
                                           int numValues)
            throws RepositoryException {
        // find applicable definition
        PropDef def;
        // multi- or single-valued property?
        if (numValues == 1) {
            // could be single- or multi-valued (n == 1)
            try {
                // try single-valued
                def = wsp.findApplicablePropertyDefinition(propName,
                        type, false, parent);
            } catch (ConstraintViolationException cve) {
                // try multi-valued
                def = wsp.findApplicablePropertyDefinition(propName,
                        type, true, parent);
            }
        } else {
            // can only be multi-valued (n == 0 || n > 1)
            def = wsp.findApplicablePropertyDefinition(propName,
                    type, true, parent);
        }
        return createProperty(parent, propName, type, def);
    }

    protected PropertyState createProperty(NodeState parent,
                                           QName propName,
                                           int type,
                                           PropDef def)
            throws RepositoryException {
        // check for name collisions with existing child nodes
        if (parent.hasChildNodeEntry(propName)) {
            String msg = "there's already a child node with name " + propName;
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // create property
        PropertyState prop = stateMgr.createNew(propName, parent.getUUID());

        prop.setDefinitionId(new PropDefId(def));
        if (def.getRequiredType() != PropertyType.UNDEFINED) {
            prop.setType(def.getRequiredType());
        } else if (type != PropertyType.UNDEFINED) {
            prop.setType(type);
        } else {
            prop.setType(PropertyType.STRING);
        }
        prop.setMultiValued(def.isMultiple());

        // compute system generated values if necessary
        InternalValue[] genValues =
                computeSystemGeneratedPropertyValues(parent, propName, def);
        if (genValues != null) {
            prop.setValues(genValues);
        } else if (def.getDefaultValues() != null) {
            prop.setValues(def.getDefaultValues());
        }

        // now add new property entry to parent
        parent.addPropertyEntry(propName);

        return prop;
    }

    /**
     * Computes the values of well-known system (i.e. protected) properties.
     * todo: duplicate code in NodeImpl: consolidate and delegate to NodeTypeInstanceHandler
     *
     * @param parent
     * @param name
     * @param def
     * @return
     * @throws RepositoryException
     */
    protected InternalValue[] computeSystemGeneratedPropertyValues(NodeState parent,
                                                                   QName name,
                                                                   PropDef def)
            throws RepositoryException {
        InternalValue[] genValues = null;

        /**
         * todo: need to come up with some callback mechanism for applying system generated values
         * (e.g. using a NodeTypeInstanceHandler interface)
         */

        // compute system generated values
        QName declaringNT = def.getDeclaringNodeType();
        if (MIX_REFERENCEABLE.equals(declaringNT)) {
            // mix:referenceable node type
            if (JCR_UUID.equals(name)) {
                // jcr:uuid property
                genValues = new InternalValue[]{InternalValue.create(parent.getUUID())};
            }
        } else if (NT_BASE.equals(declaringNT)) {
            // nt:base node type
            if (JCR_PRIMARYTYPE.equals(name)) {
                // jcr:primaryType property
                genValues = new InternalValue[]{InternalValue.create(parent.getNodeTypeName())};
            } else if (JCR_MIXINTYPES.equals(name)) {
                // jcr:mixinTypes property
                Set mixins = parent.getMixinTypeNames();
                ArrayList values = new ArrayList(mixins.size());
                Iterator iter = mixins.iterator();
                while (iter.hasNext()) {
                    values.add(InternalValue.create((QName) iter.next()));
                }
                genValues = (InternalValue[]) values.toArray(new InternalValue[values.size()]);
            }
        } else if (NT_HIERARCHYNODE.equals(declaringNT)) {
            // nt:hierarchyNode node type
            if (JCR_CREATED.equals(name)) {
                // jcr:created property
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
        } else if (NT_RESOURCE.equals(declaringNT)) {
            // nt:resource node type
            if (JCR_LASTMODIFIED.equals(name)) {
                // jcr:lastModified property
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
        } else if (NT_VERSION.equals(declaringNT)) {
            // nt:version node type
            if (JCR_CREATED.equals(name)) {
                // jcr:created property
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
/*
        // FIXME delegate to NodeTypeInstanceHandler
        } else if (MIX_VERSIONABLE.equals(declaringNT)) {
	    // mix:versionable node type
	    if (JCR_VERSIONHISTORY.equals(name)) {
		// jcr:versionHistory property
		genValues = new InternalValue[]{InternalValue.create(new UUID(hist.getUUID()))};
	    } else if (JCR_BASEVERSION.equals(name)) {
		// jcr:baseVersion property
		genValues = new InternalValue[]{InternalValue.create(new UUID(hist.getRootVersion().getUUID()))};
	    } else if (JCR_ISCHECKEDOUT.equals(name)) {
		// jcr:isCheckedOut property
		genValues = new InternalValue[]{InternalValue.create(true)};
	    } else if (JCR_PREDECESSORS.equals(name)) {
		// jcr:predecessors property
		genValues = new InternalValue[]{InternalValue.create(new UUID(hist.getRootVersion().getUUID()))};
	    }
*/
        }

        return genValues;
    }

    /**
     * Recursively removes the specified node state including its properties and
     * child nodes.
     * <p/>
     * todo duplicate code in WorkspaceImpl; consolidate in WorkspaceOperations class
     *
     * @param target
     * @param parentUUID
     * @throws RepositoryException if an error occurs
     */
    protected void removeNode(NodeState target, String parentUUID)
            throws RepositoryException {
        // check if this node state would be orphaned after unlinking it from parent
        ArrayList parentUUIDs = new ArrayList(target.getParentUUIDs());
        parentUUIDs.remove(parentUUID);
        boolean orphaned = parentUUIDs.isEmpty();

        if (orphaned) {
            // remove child nodes
            // use temp array to avoid ConcurrentModificationException
            ArrayList tmp = new ArrayList(target.getChildNodeEntries());
            // remove from tail to avoid problems with same-name siblings
            for (int i = tmp.size() - 1; i >= 0; i--) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) tmp.get(i);
                NodeId nodeId = new NodeId(entry.getUUID());
                try {
                    NodeState node = (NodeState) stateMgr.getItemState(nodeId);
                    // check if existing can be removed
                    // (access rights, locking & versioning status)
                    wsp.checkRemoveNode(node,
                            WorkspaceImpl.CHECK_ACCESS | WorkspaceImpl.CHECK_LOCK
                            | WorkspaceImpl.CHECK_VERSIONING);
                    // remove child node (recursive)
                    removeNode(node, target.getUUID());
                } catch (ItemStateException ise) {
                    String msg = "internal error: failed to retrieve state of "
                            + nodeId;
                    log.debug(msg);
                    throw new RepositoryException(msg, ise);
                }
                // remove child node entry
                target.removeChildNodeEntry(entry.getName(), entry.getIndex());
            }

            // remove properties
            // use temp array to avoid ConcurrentModificationException
            tmp = new ArrayList(target.getPropertyEntries());
            // remove from tail to avoid problems with same-name siblings
            for (int i = tmp.size() - 1; i >= 0; i--) {
                NodeState.PropertyEntry entry = (NodeState.PropertyEntry) tmp.get(i);
                PropertyId propId =
                        new PropertyId(target.getUUID(), entry.getName());
                try {
                    PropertyState prop = (PropertyState) stateMgr.getItemState(propId);
                    // remove property entry
                    target.removePropertyEntry(propId.getName());
                    // destroy property state
                    stateMgr.destroy(prop);
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
        target.removeParentUUID(parentUUID);

        if (orphaned) {
            // destroy target state
            stateMgr.destroy(target);
        } else {
            // store target state
            stateMgr.store(target);
        }
    }

    protected NodeState resolveUUIDConflict(NodeState parent,
                                            NodeState conflicting,
                                            NodeInfo nodeInfo)
            throws RepositoryException {

        NodeState node;
        if (uuidBehavior == IMPORT_UUID_CREATE_NEW) {
            // create new with new uuid:
            // check if new node can be added (check access rights &
            // node type constraints only, assume locking & versioning status
            // has already been checked on ancestor)
            wsp.checkAddNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(),
                    WorkspaceImpl.CHECK_ACCESS
                    | WorkspaceImpl.CHECK_CONSTRAINTS);
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(), null);
            // remember uuid mapping
            EffectiveNodeType ent = wsp.getEffectiveNodeType(node);
            if (ent.includesNodeType(MIX_REFERENCEABLE)) {
                refTracker.mappedUUID(nodeInfo.getUUID(), node.getUUID());
            }
        } else if (uuidBehavior == IMPORT_UUID_COLLISION_THROW) {
            String msg = "a node with uuid " + nodeInfo.getUUID()
                    + " already exists!";
            log.debug(msg);
            throw new ItemExistsException(msg);
        } else if (uuidBehavior == IMPORT_UUID_COLLISION_REMOVE_EXISTING) {
            // make sure conflicting node is not importTarget or an ancestor thereof
            Path p0 = hierMgr.getPath(importTarget.getId());
            Path p1 = hierMgr.getPath(conflicting.getId());
            try {
                if (p0.equals(p1) || p0.isAncestorOf(p1)) {
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
            // remove conflicting:
            // check if conflicting can be removed
            // (access rights, node type constraints, locking & versioning status)
            wsp.checkRemoveNode(conflicting,
                    WorkspaceImpl.CHECK_ACCESS
                    | WorkspaceImpl.CHECK_LOCK
                    | WorkspaceImpl.CHECK_VERSIONING
                    | WorkspaceImpl.CHECK_CONSTRAINTS);
            // do remove conflicting (recursive)
            removeNode(conflicting, conflicting.getParentUUID());
            // create new with given uuid:
            // check if new node can be added (check access rights &
            // node type constraints only, assume locking & versioning status
            // has already been checked on ancestor)
            wsp.checkAddNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(),
                    WorkspaceImpl.CHECK_ACCESS
                    | WorkspaceImpl.CHECK_CONSTRAINTS);
            // do create new node
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getUUID());
        } else if (uuidBehavior == IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
            if (conflicting.getParentUUID() == null) {
                String msg = "root node cannot be replaced";
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            // 'replace' current parent with parent of conflicting
            NodeId parentId = new NodeId(conflicting.getParentUUID());
            try {
                parent = (NodeState) stateMgr.getItemState(parentId);
            } catch (ItemStateException ise) {
                // should never get here...
                String msg = "internal error: failed to retrieve parent state";
                log.error(msg, ise);
                throw new RepositoryException(msg, ise);
            }
            // remove conflicting:
            // check if conflicting can be removed
            wsp.checkRemoveNode(conflicting,
                    WorkspaceImpl.CHECK_ACCESS
                    | WorkspaceImpl.CHECK_LOCK
                    | WorkspaceImpl.CHECK_VERSIONING
                    | WorkspaceImpl.CHECK_CONSTRAINTS);
            // do remove conflicting (recursive)
            removeNode(conflicting, conflicting.getParentUUID());
            // create new with given uuid at same location as conflicting:
            // check if new node can be added at other location
            // (access rights, node type constraints, locking & versioning status)
            wsp.checkAddNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(),
                    WorkspaceImpl.CHECK_ACCESS
                    | WorkspaceImpl.CHECK_LOCK
                    | WorkspaceImpl.CHECK_VERSIONING
                    | WorkspaceImpl.CHECK_CONSTRAINTS);
            // do create new node
            node = createNode(parent, nodeInfo.getName(),
                    nodeInfo.getNodeTypeName(), nodeInfo.getMixinNames(),
                    nodeInfo.getUUID());
        } else {
            String msg = "unknown uuidBehavior: " + uuidBehavior;
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        return node;
    }

    //-------------------------------------------------------------< Importer >
    /**
     * {@inheritDoc}
     */
    public void start() throws RepositoryException {
        try {
            // start update operation
            stateMgr.edit();
        } catch (ItemStateException ise) {
            aborted = true;
            String msg = "internal error: failed to start update operation";
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startNode(NodeInfo nodeInfo, List propInfos,
                          NamespaceResolver nsContext)
            throws RepositoryException {
        if (aborted) {
            // the import has been aborted, get outta here...
            return;
        }

        boolean succeeded = false;
        NodeState parent = null;
        try {
            // check sanity of workspace/session first
            wsp.sanityCheck();

            parent = (NodeState) parents.peek();

            // process node

            NodeState node = null;
            String uuid = nodeInfo.getUUID();
            QName nodeName = nodeInfo.getName();
            QName ntName = nodeInfo.getNodeTypeName();
            QName[] mixins = nodeInfo.getMixinNames();

            if (parent == null) {
                // parent node was skipped, skip this child node also
                parents.push(null); // push null onto stack for skipped node
                succeeded = true;
                log.debug("skipping node " + nodeName);
                return;
            }
            if (parent.hasChildNodeEntry(nodeName)) {
                // a node with that name already exists...
                NodeState.ChildNodeEntry entry =
                        parent.getChildNodeEntry(nodeName, 1);
                NodeId idExisting = new NodeId(entry.getUUID());
                NodeState existing = (NodeState) stateMgr.getItemState(idExisting);
                ChildNodeDef def = ntReg.getNodeDef(existing.getDefinitionId());

                if (!def.allowSameNameSibs()) {
                    // existing doesn't allow same-name siblings,
                    // check for potential conflicts
                    EffectiveNodeType entExisting =
                            wsp.getEffectiveNodeType(existing);
                    if (def.isProtected() && entExisting.includesNodeType(ntName)) {
                        // skip protected node
                        parents.push(null); // push null onto stack for skipped node
                        succeeded = true;
                        log.debug("skipping protected node "
                                + resolveJCRPath(existing.getId()));
                        return;
                    }
                    if (def.isAutoCreate() && entExisting.includesNodeType(ntName)) {
                        // this node has already been auto-created,
                        // no need to create it
                        node = existing;
                    } else {
                        throw new ItemExistsException(resolveJCRPath(existing.getId()));
                    }
                }
            }

            if (node == null) {
                // there's no node with that name...
                if (uuid == null) {
                    // no potential uuid conflict, always create new node

                    ChildNodeDef def =
                            wsp.findApplicableNodeDefinition(nodeName, ntName, parent);
                    if (def.isProtected()) {
                        // skip protected node
                        parents.push(null); // push null onto stack for skipped node
                        succeeded = true;
                        log.debug("skipping protected node " + nodeName);
                        return;
                    }

                    // check if new node can be added (check access rights &
                    // node type constraints only, assume locking & versioning status
                    // has already been checked on ancestor)
                    wsp.checkAddNode(parent, nodeName, ntName,
                            WorkspaceImpl.CHECK_ACCESS
                            | WorkspaceImpl.CHECK_CONSTRAINTS);
                    // do create new node
                    node = createNode(parent, nodeName, ntName, mixins, null, def);
                } else {
                    // potential uuid conflict
                    NodeState conflicting;

                    try {
                        conflicting =
                                (NodeState) stateMgr.getItemState(new NodeId(uuid));
                    } catch (NoSuchItemStateException nsise) {
                        conflicting = null;
                    }
                    if (conflicting != null) {
                        // resolve uuid conflict
                        node = resolveUUIDConflict(parent, conflicting, nodeInfo);
                    } else {
                        // create new with given uuid

                        ChildNodeDef def =
                                wsp.findApplicableNodeDefinition(nodeName, ntName, parent);
                        if (def.isProtected()) {
                            // skip protected node
                            parents.push(null); // push null onto stack for skipped node
                            succeeded = true;
                            log.debug("skipping protected node " + nodeName);
                            return;
                        }

                        // check if new node can be added (check access rights &
                        // node type constraints only, assume locking & versioning status
                        // has already been checked on ancestor)
                        wsp.checkAddNode(parent, nodeName, ntName,
                                WorkspaceImpl.CHECK_ACCESS
                                | WorkspaceImpl.CHECK_CONSTRAINTS);
                        // do create new node
                        node = createNode(parent, nodeName, ntName, mixins, uuid, def);
                    }
                }
            }

            // process properties

            Iterator iter = propInfos.iterator();
            while (iter.hasNext()) {
                PropInfo pi = (PropInfo) iter.next();
                QName propName = pi.getName();
                TextValue[] tva = pi.getValues();
                int type = pi.getType();

                PropertyState prop = null;
                PropDef def = null;

                if (node.hasPropertyEntry(propName)) {
                    // a property with that name already exists...
                    PropertyId idExisting = new PropertyId(node.getUUID(), propName);
                    PropertyState existing =
                            (PropertyState) stateMgr.getItemState(idExisting);
                    def = ntReg.getPropDef(existing.getDefinitionId());
                    if (def.isProtected()) {
                        // skip protected property
                        log.debug("skipping protected property "
                                + resolveJCRPath(idExisting));
                        continue;
                    }
                    if (def.isAutoCreate() && (existing.getType() == type
                            || type == PropertyType.UNDEFINED)
                            && def.isMultiple() == existing.isMultiValued()) {
                        // this property has already been auto-created,
                        // no need to create it
                        prop = existing;
                    } else {
                        throw new ItemExistsException(resolveJCRPath(existing.getId()));
                    }
                }
                if (prop == null) {
                    // there's no property with that name,
                    // find applicable definition

                    // multi- or single-valued property?
                    if (tva.length == 1) {
                        // could be single- or multi-valued (n == 1)
                        try {
                            // try single-valued
                            def = wsp.findApplicablePropertyDefinition(propName,
                                    type, false, node);
                        } catch (ConstraintViolationException cve) {
                            // try multi-valued
                            def = wsp.findApplicablePropertyDefinition(propName,
                                    type, true, node);
                        }
                    } else {
                        // can only be multi-valued (n == 0 || n > 1)
                        def = wsp.findApplicablePropertyDefinition(propName,
                                type, true, node);
                    }

                    if (def.isProtected()) {
                        // skip protected property
                        log.debug("skipping protected property " + propName);
                        continue;
                    }

                    // create new property
                    prop = createProperty(node, propName, type, def);
                }

                // check multi-valued characteristic
                if ((tva.length == 0 || tva.length > 1) && !def.isMultiple()) {
                    throw new ConstraintViolationException(resolveJCRPath(prop.getId())
                            + " is not multi-valued");
                }

                // convert serialized values to InternalValue objects
                InternalValue[] iva = new InternalValue[tva.length];
                int targetType = def.getRequiredType();
                if (targetType == PropertyType.UNDEFINED) {
                    if (type == PropertyType.UNDEFINED) {
                        targetType = PropertyType.STRING;
                    } else {
                        targetType = type;
                    }
                }
                for (int i = 0; i < tva.length; i++) {
                    TextValue tv = tva[i];
                    if (targetType == PropertyType.BINARY) {
                        // base64 encoded BINARY type;
                        // decode using Reader
/*
                        // @todo decode to temp file and pass FileInputStream to InternalValue factory method
                        File tmpFile = null;
                        try {
                            tmpFile = File.createTempFile("bin", null);
                            FileOutputStream out = new FileOutputStream(tmpFile);
                            Base64.decode(tv.reader(), out);
                            out.close();
                            iva[i] = InternalValue.create(new FileInputStream(tmpFile));
                        } catch (IOException ioe) {
                            String msg = "failed to decode binary value";
                            log.debug(msg, ioe);
                            throw new RepositoryException(msg, ioe);
                        } finally {
                            // the temp file can be deleted because
                            // the InternalValue instance has spooled
                            // its contents
                            if (tmpFile != null) {
                                tmpFile.delete();
                            }
                        }
*/
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try {
                            Base64.decode(tv.reader(), baos);
                            // no need to close ByteArrayOutputStream
                            //baos.close();
                            iva[i] = InternalValue.create(new ByteArrayInputStream(baos.toByteArray()));
                        } catch (IOException ioe) {
                            String msg = "failed to decode binary value";
                            log.debug(msg, ioe);
                            throw new RepositoryException(msg, ioe);
                        }
                    } else {
                        // retrieve serialized value
                        String serValue;
                        try {
                            serValue = tv.retrieve();
                        } catch (IOException ioe) {
                            String msg = "failed to retrieve serialized value";
                            log.debug(msg, ioe);
                            throw new RepositoryException(msg, ioe);
                        }

                        // convert serialized value to InternalValue using
                        // current namespace context of xml document
                        iva[i] = InternalValue.create(serValue, targetType,
                                nsContext);
                    }

                }

                // set values
                prop.setValues(iva);

                // make sure node is valid according to its definition
                wsp.validate(prop);

                if (prop.getType() == PropertyType.REFERENCE) {
                    // store reference for later resolution
                    refTracker.processedReference(prop);
                }

                // store property
                stateMgr.store(prop);
            }

            // store affected nodes
            stateMgr.store(node);
            stateMgr.store(parent);

            // push current node onto stack of parents
            parents.push(node);

            succeeded = true;
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to store state of "
                    + resolveJCRPath(parent.getId());
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                aborted = true;
                stateMgr.cancel();
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
        NodeState node = (NodeState) parents.pop();
        if (node == null) {
            // node was skipped, nothing to do here
            return;
        }
        boolean succeeded = false;
        try {
            // check sanity of workspace/session first
            wsp.sanityCheck();

            // make sure node is valid according to its definition
            wsp.validate(node);

            // we're done with that node, now store its state
            stateMgr.store(node);
            succeeded = true;
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                aborted = true;
                stateMgr.cancel();
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

            // make sure import target is valid according to its definition
            wsp.validate(importTarget);

            // finally store the state of the import target
            // (the parent of the imported subtree)
            stateMgr.store(importTarget);
            succeeded = true;
        } finally {
            if (!succeeded) {
                // update operation failed, cancel all modifications
                aborted = true;
                stateMgr.cancel();
            }
        }

        if (!aborted) {
            try {
                // finish update
                stateMgr.update();
            } catch (ItemStateException ise) {
                aborted = true;
                String msg = "internal error: failed to finish update operation";
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }
        }
    }
}
