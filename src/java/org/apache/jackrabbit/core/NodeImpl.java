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

import org.apache.jackrabbit.core.nodetype.*;
import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.util.ChildrenCollectorFilter;
import org.apache.jackrabbit.core.util.IteratorHelper;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.version.*;
import org.apache.log4j.Logger;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.*;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.io.InputStream;
import java.util.*;

/**
 * <code>NodeImpl</code> implements the <code>Node</code> interface.
 */
public class NodeImpl extends ItemImpl implements Node {

    private static Logger log = Logger.getLogger(NodeImpl.class);

    protected final NodeTypeImpl nodeType;

    protected NodeDef definition;

    // flag set in status passed to getOrCreateProperty if property was created
    protected static final short CREATED = 0;

    /**
     * Package private constructor.
     *
     * @param itemMgr    the <code>ItemManager</code> that created this <code>Node</code> instance
     * @param session    the <code>Session</code> through which this <code>Node</code> is acquired
     * @param id         id of this <code>Node</code>
     * @param state      state associated with this <code>Node</code>
     * @param definition definition of <i>this</i> <code>Node</code>
     * @param listeners  listeners on life cylce changes of this <code>NodeImpl</code>
     * @throws RepositoryException
     */
    protected NodeImpl(ItemManager itemMgr, SessionImpl session, NodeId id,
                       NodeState state, NodeDef definition,
                       ItemLifeCycleListener[] listeners)
            throws RepositoryException {
        super(itemMgr, session, id, state, listeners);
        nodeType = session.getNodeTypeManager().getNodeType(state.getNodeTypeName());
        this.definition = definition;
    }

    /**
     * Returns the id of the property at <code>relPath</code> or <code>null</code>
     * if no property exists at <code>relPath</code>.
     * <p/>
     * Note that access rights are not checked.
     *
     * @param relPath relative path of a (possible) property
     * @return the id of the property at <code>relPath</code> or
     *         <code>null</code> if no property exists at <code>relPath</code>
     * @throws RepositoryException if <code>relPath</code> is not a valid
     *                             relative path
     */
    protected PropertyId resolveRelativePropertyPath(String relPath)
            throws RepositoryException {
        try {
            /**
             * first check if relPath is just a name (in which case we don't
             * have to build & resolve absolute path)
             */
            Path p = Path.create(relPath, session.getNamespaceResolver(), false);
            if (p.getLength() == 1) {
                Path.PathElement pe = p.getNameElement();
                if (pe.denotesName()) {
                    if (pe.getIndex() > 0) {
                        // property name can't have subscript
                        String msg = relPath + " is not a valid property path";
                        log.error(msg);
                        throw new RepositoryException(msg);
                    }
                    // check if property entry exists
                    NodeState thisState = (NodeState) state;
                    if (thisState.hasPropertyEntry(pe.getName())) {
                        return new PropertyId(thisState.getUUID(), pe.getName());
                    } else {
                        // there's no property with that name
                        return null;
                    }
                }
            }
            /**
             * build and resolve absolute path
             */
            p = Path.create(getPrimaryPath(), relPath, session.getNamespaceResolver(), true);
            try {
                ItemId id = session.getHierarchyManager().resolvePath(p);
                if (!id.denotesNode()) {
                    return (PropertyId) id;
                } else {
                    // not a property
                    return null;
                }
            } catch (PathNotFoundException pnfe) {
                return null;
            }
        } catch (MalformedPathException e) {
            String msg = "failed to resolve path " + relPath + " relative to " + safeGetJCRPath();
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Returns the id of the node at <code>relPath</code> or <code>null</code>
     * if no node exists at <code>relPath</code>.
     * <p/>
     * Note that access rights are not checked.
     *
     * @param relPath relative path of a (possible) node
     * @return the id of the node at <code>relPath</code> or
     *         <code>null</code> if no node exists at <code>relPath</code>
     * @throws RepositoryException if <code>relPath</code> is not a valid
     *                             relative path
     */
    protected NodeId resolveRelativeNodePath(String relPath)
            throws RepositoryException {
        try {
            /**
             * first check if relPath is just a name (in which case we don't
             * have to build & resolve absolute path)
             */
            Path p = Path.create(relPath, session.getNamespaceResolver(), false);
            if (p.getLength() == 1) {
                Path.PathElement pe = p.getNameElement();
                if (pe.denotesName()) {
                    // check if node entry exists
                    NodeState thisState = (NodeState) state;
                    NodeState.ChildNodeEntry cne =
                            thisState.getChildNodeEntry(pe.getName(),
                                    pe.getIndex() == 0 ? 1 : pe.getIndex());
                    if (cne != null) {
                        return new NodeId(cne.getUUID());
                    } else {
                        // there's no child node with that name
                        return null;
                    }
                }
            }
            /**
             * build and resolve absolute path
             */
            p = Path.create(getPrimaryPath(), relPath, session.getNamespaceResolver(), true);
            try {
                ItemId id = session.getHierarchyManager().resolvePath(p);
                if (id.denotesNode()) {
                    return (NodeId) id;
                } else {
                    // not a node
                    return null;
                }
            } catch (PathNotFoundException pnfe) {
                return null;
            }
        } catch (MalformedPathException e) {
            String msg = "failed to resolve path " + relPath + " relative to " + safeGetJCRPath();
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Determines if there are pending unsaved changes either on <i>this</i>
     * node or on any node or property in the subtree below it.
     * @return <code>true</code> if there are pending unsaved changes,
     * <code>false</code> otherwise.
     * @throws RepositoryException if an error occured
     */
    protected boolean hasPendingChanges() throws RepositoryException {
        if (isTransient()) {
            return true;
        }
        Iterator iter = stateMgr.getDescendantTransientItemStates(id);
        return iter.hasNext();
    }

    protected synchronized ItemState getOrCreateTransientItemState()
            throws RepositoryException {
        if (!isTransient()) {
            try {
                // make transient (copy-on-write)
                NodeState transientState =
                        stateMgr.createTransientNodeState((NodeState) state, ItemState.STATUS_EXISTING_MODIFIED);
                // remove listener on persistent state
                state.removeListener(this);
                // add listener on transient state
                transientState.addListener(this);
                // replace persistent with transient state
                state = transientState;
            } catch (ItemStateException ise) {
                String msg = "failed to create transient state";
                log.error(msg, ise);
                throw new RepositoryException(msg, ise);
            }
        }
        return state;
    }

    protected InternalValue[] computeSystemGeneratedPropertyValues(QName name,
                                                                   PropertyDefImpl def)
            throws RepositoryException {
        InternalValue[] genValues = null;

        /**
         * todo: need to come up with some callback mechanism for applying system generated values
         * (e.g. using a NodeTypeInstanceHandler interface)
         */

        NodeState thisState = (NodeState) state;

        // compute/apply system generated values
        NodeTypeImpl nt = (NodeTypeImpl) def.getDeclaringNodeType();
        if (nt.getQName().equals(NodeTypeRegistry.MIX_REFERENCEABLE)) {
            // mix:referenceable node type
            if (name.equals(PROPNAME_UUID)) {
                // jcr:uuid property
                genValues = new InternalValue[]{InternalValue.create(((NodeState) state).getUUID())};
            }
/*
	todo consolidate version history creation code (currently in NodeImpl.addMixin & ItemImpl.initVersionHistories
	} else if (nt.getQName().equals(NodeTypeRegistry.MIX_VERSIONABLE)) {
	    // mix:versionable node type
	    VersionHistory hist = rep.getVersionManager().getOrCreateVersionHistory(this);
	    if (name.equals(InternalVersion.PROPNAME_VERSION_HISTORY)) {
		// jcr:versionHistory property
		genValues = new InternalValue[]{InternalValue.create(new UUID(hist.getUUID()))};
	    } else if (name.equals(InternalVersion.PROPNAME_BASE_VERSION)) {
		// jcr:baseVersion property
		genValues = new InternalValue[]{InternalValue.create(new UUID(hist.getRootVersion().getUUID()))};
	    } else if (name.equals(InternalVersion.PROPNAME_IS_CHECKED_OUT)) {
		// jcr:isCheckedOut property
		genValues = new InternalValue[]{InternalValue.create(true)};
	    } else if (name.equals(InternalVersion.PROPNAME_PREDECESSORS)) {
		// jcr:predecessors property
		genValues = new InternalValue[]{InternalValue.create(new UUID(hist.getRootVersion().getUUID()))};
	    }
*/
        } else if (nt.getQName().equals(NodeTypeRegistry.NT_HIERARCHYNODE)) {
            // nt:hierarchyNode node type
            if (name.equals(PROPNAME_CREATED)) {
                // jcr:created property
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
        } else if (nt.getQName().equals(NodeTypeRegistry.NT_RESOURCE)) {
            // nt:mimeResource node type
            if (name.equals(PROPNAME_LAST_MODIFIED)) {
                // jcr:lastModified property
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
        } else if (nt.getQName().equals(NodeTypeRegistry.NT_VERSION)) {
            // nt:hierarchyNode node type
            if (name.equals(PROPNAME_CREATED)) {
                // jcr:created property
                genValues = new InternalValue[]{InternalValue.create(Calendar.getInstance())};
            }
        } else if (nt.getQName().equals(NodeTypeRegistry.NT_BASE)) {
            // nt:base node type
            if (name.equals(PROPNAME_PRIMARYTYPE)) {
                // jcr:primaryType property
                genValues = new InternalValue[]{InternalValue.create(nodeType.getQName())};
            } else if (name.equals(PROPNAME_MIXINTYPES)) {
                // jcr:mixinTypes property
                Set mixins = thisState.getMixinTypeNames();
                ArrayList values = new ArrayList(mixins.size());
                Iterator iter = mixins.iterator();
                while (iter.hasNext()) {
                    values.add(InternalValue.create((QName) iter.next()));
                }
                genValues = (InternalValue[]) values.toArray(new InternalValue[values.size()]);
            }
        }

        return genValues;
    }

    protected PropertyImpl getOrCreateProperty(String name, int type,
                                               boolean multiValued,
                                               BitSet status)
            throws RepositoryException {
        QName qName;
        try {
            qName = QName.fromJCRName(name, session.getNamespaceResolver());
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid property name: " + name, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid property name: " + name, upe);
        }
        return getOrCreateProperty(qName, type, multiValued, status);
    }

    protected synchronized PropertyImpl getOrCreateProperty(QName name, int type,
                                                            boolean multiValued,
                                                            BitSet status)
            throws RepositoryException {
        status.clear();

        NodeState thisState = (NodeState) state;
        if (thisState.hasPropertyEntry(name)) {
            /**
             * the following call will throw ItemNotFoundException if the
             * current session doesn't have read access
             */
            return getProperty(name);
        }

        // does not exist yet:
        // find definition for the specified property and create property
        PropertyDefImpl def = getApplicablePropertyDef(name, type, multiValued);
        PropertyImpl prop = createChildProperty(name, type, def);
        status.set(CREATED);
        return prop;
    }

    protected synchronized PropertyImpl createChildProperty(QName name, int type, PropertyDefImpl def)
            throws RepositoryException {
        // check for name collisions with existing child nodes
        if (((NodeState) state).hasChildNodeEntry(name)) {
            String msg = "there's already a child node with name " + name;
            log.error(msg);
            throw new RepositoryException(msg);
        }
        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = "Cannot set the value of a property of a checked-in node " + safeGetJCRPath() + "/" + name.toString();
            log.error(msg);
            throw new VersionException(msg);
        }

        String parentUUID = ((NodeState) state).getUUID();

        // create a new property state
        PropertyState propState = null;
        try {
            propState = stateMgr.createTransientPropertyState(parentUUID, name, ItemState.STATUS_NEW);
            propState.setType(type);
            propState.setMultiValued(def.isMultiple());
            propState.setDefinitionId(new PropDefId(def.unwrap()));
            // compute system generated values if necessary
            InternalValue[] genValues = computeSystemGeneratedPropertyValues(name, def);
            if (genValues != null) {
                propState.setValues(genValues);
            } else if (def.getDefaultValues() != null) {
                Value[] vals = def.getDefaultValues();
                if (vals.length > 0) {
                    int length = (def.isMultiple() ? vals.length : 1);
                    InternalValue[] defVals = new InternalValue[length];
                    for (int i = 0; i < length; i++) {
                        defVals[i] = InternalValue.create(vals[i], session.getNamespaceResolver());
                    }
                    propState.setValues(defVals);
                }
            }
        } catch (ItemStateException ise) {
            String msg = "failed to add property " + name + " to " + safeGetJCRPath();
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }

        // create Property instance wrapping new property state
        PropertyImpl prop = itemMgr.createPropertyInstance(propState, def);

        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        // add new property entry
        thisState.addPropertyEntry(name);

        return prop;
    }

    protected synchronized NodeImpl createChildNode(QName name, NodeDefImpl def,
                                                    NodeTypeImpl nodeType, String uuid)
            throws RepositoryException {
        String parentUUID = ((NodeState) state).getUUID();
        // create a new node state
        NodeState nodeState = null;
        try {
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();	// version 4 uuid
            }
            nodeState = stateMgr.createTransientNodeState(uuid, nodeType.getQName(), parentUUID, ItemState.STATUS_NEW);
            nodeState.setDefinitionId(new NodeDefId(def.unwrap()));
        } catch (ItemStateException ise) {
            String msg = "failed to add child node " + name + " to " + safeGetJCRPath();
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }

        // create Node instance wrapping new node state
        NodeImpl node;
        try {
            node = itemMgr.createNodeInstance(nodeState, def);
        } catch (RepositoryException re) {
            // something went wrong
            stateMgr.disposeTransientItemState(nodeState);
            // re-throw
            throw re;
        }

        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        // add new child node entry
        thisState.addChildNodeEntry(name, nodeState.getUUID());

        // add 'auto-create' properties defined in node type
        PropertyDef[] pda = nodeType.getAutoCreatePropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            PropertyDefImpl pd = (PropertyDefImpl) pda[i];
            node.createChildProperty(pd.getQName(), pd.getRequiredType(), pd);
        }

        // recursively add 'auto-create' child nodes defined in node type
        NodeDef[] nda = nodeType.getAutoCreateNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            NodeDefImpl nd = (NodeDefImpl) nda[i];
            node.createChildNode(nd.getQName(), nd, (NodeTypeImpl) nd.getDefaultPrimaryType(), null);
        }

        return node;
    }

    protected NodeImpl createChildNodeLink(QName nodeName, String targetUUID)
            throws RepositoryException {
        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();

        NodeId targetId = new NodeId(targetUUID);
        NodeImpl targetNode = (NodeImpl) itemMgr.getItem(targetId);
        // notify target of link
        targetNode.onLink(thisState);

        thisState.addChildNodeEntry(nodeName, targetUUID);

        return (NodeImpl) itemMgr.getItem(targetId);
    }


    protected void removeChildProperty(String propName) throws RepositoryException {
        QName qName;
        try {
            qName = QName.fromJCRName(propName, session.getNamespaceResolver());
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid property name: " + propName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid property name: " + propName, upe);
        }
        removeChildProperty(qName);
    }

    protected void removeChildProperty(QName propName) throws RepositoryException {
        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();

        // remove property
        PropertyId propId = new PropertyId(thisState.getUUID(), propName);
        itemMgr.removeItem(propId);

        // remove the property entry
        if (!thisState.removePropertyEntry(propName)) {
            String msg = "failed to remove property " + propName + " of " + safeGetJCRPath();
            log.error(msg);
            throw new RepositoryException(msg);
        }
    }

    protected void removeChildNode(QName nodeName, int index) throws RepositoryException {
        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        index = (index == 0) ? 1 : index;
        NodeState.ChildNodeEntry entry = thisState.getChildNodeEntry(nodeName, index);
        if (entry == null) {
            String msg = "failed to remove child " + nodeName + " of " + safeGetJCRPath();
            log.error(msg);
            throw new RepositoryException(msg);
        }

        NodeId childId = new NodeId(entry.getUUID());
        NodeImpl childNode = (NodeImpl) itemMgr.getItem(childId);
        // notify target of removal/unlink
        childNode.onUnlink(thisState);

        // remove child entry
        if (!thisState.removeChildNodeEntry(nodeName, index)) {
            String msg = "failed to remove child " + nodeName + " of " + safeGetJCRPath();
            log.error(msg);
            throw new RepositoryException(msg);
        }
    }

    protected void onRedefine(NodeDefId defId) throws RepositoryException {
        NodeDefImpl newDef = session.getNodeTypeManager().getNodeDef(defId);
        // modify the state of 'this', i.e. the target node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        // set id of new definition
        thisState.setDefinitionId(defId);
        definition = newDef;
    }

    protected void onLink(NodeState parentState) throws RepositoryException {
        // modify the state of 'this', i.e. the target node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        // add uuid of this node to target's parent list
        thisState.addParentUUID(parentState.getUUID());
    }

    protected void onUnlink(NodeState parentState) throws RepositoryException {
        // modify the state of 'this', i.e. the target node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();

        // check if this node would be orphaned after unlinking it from parent
        ArrayList parentUUIDs = new ArrayList(thisState.getParentUUIDs());
        parentUUIDs.remove(parentState.getUUID());
        boolean orphaned = parentUUIDs.isEmpty();

        if (orphaned) {
            // remove child nodes (recursive)
            // use temp array to avoid ConcurrentModificationException
            ArrayList tmp = new ArrayList(thisState.getChildNodeEntries());
            // remove from tail to avoid problems with same-name siblings
            for (int i = tmp.size() - 1; i >= 0; i--) {
                NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) tmp.get(i);
                removeChildNode(entry.getName(), entry.getIndex());
            }

            // remove properties
            // use temp array to avoid ConcurrentModificationException
            tmp = new ArrayList(thisState.getPropertyEntries());
            // remove from tail to avoid problems with same-name siblings
            for (int i = tmp.size() - 1; i >= 0; i--) {
                NodeState.PropertyEntry entry = (NodeState.PropertyEntry) tmp.get(i);
                removeChildProperty(entry.getName());
            }
        }

        // now actually do unlink this node from specified parent node
        // (i.e. remove uuid of parent node from this node's parent list)
        thisState.removeParentUUID(parentState.getUUID());

        if (orphaned) {
            // remove this node
            itemMgr.removeItem(id);
        }
    }

    protected NodeImpl internalAddNode(String relPath, NodeTypeImpl nodeType)
            throws ItemExistsException, PathNotFoundException,
            ConstraintViolationException, RepositoryException {
        return internalAddNode(relPath, nodeType, null);
    }

    protected NodeImpl internalAddNode(String relPath, NodeTypeImpl nodeType,
                                       String uuid)
            throws ItemExistsException, PathNotFoundException,
            ConstraintViolationException, RepositoryException {
        Path nodePath;
        QName nodeName;
        Path parentPath;
        try {
            nodePath = Path.create(getPrimaryPath(), relPath, session.getNamespaceResolver(), true);
            if (nodePath.getNameElement().getIndex() != 0) {
                String msg = "illegal subscript specified: " + nodePath;
                log.error(msg);
                throw new RepositoryException(msg);
            }
            nodeName = nodePath.getNameElement().getName();
            parentPath = nodePath.getAncestor(1);
        } catch (MalformedPathException e) {
            String msg = "failed to resolve path " + relPath + " relative to " + safeGetJCRPath();
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }

        NodeImpl parentNode;
        try {
            Item parent = itemMgr.getItem(parentPath);
            if (!parent.isNode()) {
                String msg = "cannot add a node to property " + parentPath;
                log.error(msg);
                throw new ConstraintViolationException(msg);
            }
            parentNode = (NodeImpl) parent;
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(relPath);
        }

        // delegate the creation of the child node to the parent node
        return parentNode.internalAddChildNode(nodeName, nodeType, uuid);
    }

    protected NodeImpl internalAddChildNode(QName nodeName, NodeTypeImpl nodeType)
            throws ItemExistsException, ConstraintViolationException, RepositoryException {
        return internalAddChildNode(nodeName, nodeType, null);
    }

    protected NodeImpl internalAddChildNode(QName nodeName, NodeTypeImpl nodeType, String uuid)
            throws ItemExistsException, ConstraintViolationException, RepositoryException {
        Path nodePath;
        try {
            nodePath = Path.create(getPrimaryPath(), nodeName, true);
        } catch (MalformedPathException e) {
            // should never happen
            String msg = "internal error: invalid path " + safeGetJCRPath();
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }

        NodeDefImpl def;
        try {
            def = getApplicableChildNodeDef(nodeName, nodeType == null ? null : nodeType.getQName());
        } catch (RepositoryException re) {
            String msg = "no definition found in parent node's node type for new node";
            log.error(msg, re);
            throw new ConstraintViolationException(msg, re);
        }
        if (nodeType == null) {
            // use default node type
            nodeType = (NodeTypeImpl) def.getDefaultPrimaryType();
        }

        // check for name collisions
/*
        try {
            Item item = itemMgr.getItem(nodePath);
            if (!item.isNode()) {
                // there's already a property with that name
                throw new ItemExistsException(itemMgr.safeGetJCRPath(nodePath));
            } else {
                // there's already a node with that name
                // check same-name sibling setting of both new and existing node
                if (!def.allowSameNameSibs()
                        || !((NodeImpl) item).getDefinition().allowSameNameSibs()) {
                    throw new ItemExistsException(itemMgr.safeGetJCRPath(nodePath));
                }
            }
        } catch (PathNotFoundException pnfe) {
            // no name collision
        }
*/
        NodeState thisState = (NodeState) state;
        if (thisState.hasPropertyEntry(nodeName)) {
            // there's already a property with that name
            throw new ItemExistsException(itemMgr.safeGetJCRPath(nodePath));
        }
        NodeState.ChildNodeEntry cne = thisState.getChildNodeEntry(nodeName, 1);
        if (cne != null) {
            // there's already a child node entry with that name;
            // check same-name sibling setting of new node
            if (!def.allowSameNameSibs()) {
                throw new ItemExistsException(itemMgr.safeGetJCRPath(nodePath));
            }
            // check same-name sibling setting of existing node
            NodeId newId = new NodeId(cne.getUUID());
            if (!((NodeImpl) itemMgr.getItem(newId)).getDefinition().allowSameNameSibs()) {
                throw new ItemExistsException(itemMgr.safeGetJCRPath(nodePath));
            }
        }

        // check protected flag of parent (i.e. this) node
        if (getDefinition().isProtected()) {
            String msg = safeGetJCRPath() + ": cannot add a child to a protected node";
            log.error(msg);
            throw new ConstraintViolationException(msg);
        }

        // now do create the child node
        return createChildNode(nodeName, def, nodeType, uuid);
    }

    private void setMixinTypesProperty(Set mixinNames) throws RepositoryException {
        NodeState thisState = (NodeState) state;
        // get or create jcr:mixinTypes property
        PropertyImpl prop = null;
        if (thisState.hasPropertyEntry(PROPNAME_MIXINTYPES)) {
            prop = (PropertyImpl) itemMgr.getItem(new PropertyId(thisState.getUUID(), PROPNAME_MIXINTYPES));
        } else {
            // find definition for the jcr:mixinTypes property and create property
            PropertyDefImpl def = getApplicablePropertyDef(PROPNAME_MIXINTYPES, PropertyType.NAME, true);
            prop = createChildProperty(PROPNAME_MIXINTYPES, PropertyType.NAME, def);
        }

        if (mixinNames.isEmpty()) {
            // purge empty jcr:mixinTypes property
            removeChildProperty(PROPNAME_MIXINTYPES);
            return;
        }

        // call internalSetValue for setting the jcr:mixinTypes property
        // to avoid checking of the 'protected' flag
        InternalValue[] vals = new InternalValue[mixinNames.size()];
        Iterator iter = mixinNames.iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            vals[cnt++] = InternalValue.create((QName) iter.next());
        }
        prop.internalSetValue(vals, PropertyType.NAME);
    }

    /**
     * Returns the effective (i.e. merged and resolved) node type representation
     * of this node's primary and mixin node types.
     *
     * @return the effective node type
     * @throws RepositoryException
     */
    protected EffectiveNodeType getEffectiveNodeType() throws RepositoryException {
        // build effective node type of mixins & primary type
        NodeTypeRegistry ntReg = session.getNodeTypeManager().getNodeTypeRegistry();
        // existing mixin's
        HashSet set = new HashSet(((NodeState) state).getMixinTypeNames());
        // primary type
        set.add(nodeType.getQName());
        try {
            return ntReg.getEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node " + safeGetJCRPath();
            log.error(msg, ntce);
            throw new RepositoryException(msg, ntce);
        }
    }

    /**
     * Returns the applicable child node definition for a child node with the
     * specified name and node type.
     *
     * @param nodeName
     * @param nodeTypeName
     * @return
     * @throws RepositoryException if no applicable child node definition
     *                             could be found
     */
    protected NodeDefImpl getApplicableChildNodeDef(QName nodeName, QName nodeTypeName)
            throws RepositoryException {
        ChildNodeDef cnd = getEffectiveNodeType().getApplicableChildNodeDef(nodeName, nodeTypeName);
        return session.getNodeTypeManager().getNodeDef(new NodeDefId(cnd));
    }

    /**
     * Returns the applicable property definition for a property with the
     * specified name and type.
     *
     * @param propertyName
     * @param type
     * @param multiValued
     * @return
     * @throws RepositoryException if no applicable property definition
     *                             could be found
     */
    protected PropertyDefImpl getApplicablePropertyDef(QName propertyName,
                                                       int type, boolean multiValued)
            throws RepositoryException {
        PropDef pd = getEffectiveNodeType().getApplicablePropertyDef(propertyName, type, multiValued);
        return session.getNodeTypeManager().getPropDef(new PropDefId(pd));
    }

    protected void makePersistent(UpdateOperation update) {
        if (!isTransient()) {
            log.debug(safeGetJCRPath() + " (" + id + "): there's no transient state to persist");
            return;
        }

        NodeState transientState = (NodeState) state;

        NodeState persistentState = (NodeState) transientState.getOverlayedState();
        if (persistentState == null) {
            // this node is 'new'
            persistentState = update.createNew(transientState.getUUID(),
                    transientState.getNodeTypeName(), transientState.getParentUUID());
        }
        // copy state from transient state:
        // parent uuid's
        persistentState.setParentUUID(transientState.getParentUUID());
        persistentState.setParentUUIDs(transientState.getParentUUIDs());
        // mixin types
        persistentState.setMixinTypeNames(transientState.getMixinTypeNames());
        // id of definition
        persistentState.setDefinitionId(transientState.getDefinitionId());
        // child node entries
        persistentState.setChildNodeEntries(transientState.getChildNodeEntries());
        // property entries
        persistentState.setPropertyEntries(transientState.getPropertyEntries());

        // make state persistent
        update.store(persistentState);
        // remove listener from transient state
        transientState.removeListener(this);
        // add listener to persistent state
        persistentState.addListener(this);
        // swap transient state with persistent state
        state = persistentState;
        // reset status
        status = STATUS_NORMAL;
    }

    /**
     * Checks if this node is the root node.
     * todo: is this the root node of this workspace?
     *
     * @return
     */
    protected boolean isRepositoryRoot() {
        return ((NodeState) state).getUUID().equals(rep.getRootNodeUUID());
    }

    /**
     * Returns the (internal) uuid of this node.
     * @return the uuid of this node
     */
    public String internalGetUUID() {
        return ((NodeState) state).getUUID();
    }

    /**
     * Sets the internal value of a property without checking any constraints.
     * <p/>
     * Note that no type conversion is being performed, i.e. it's the caller's
     * responsibility to make sure that the type of the given value is compatible
     * with the specified property's definition.
     *
     * @param name
     * @param value
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public Property internalSetProperty(QName name, InternalValue value)
            throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        int type;
        if (value == null) {
            type = PropertyType.STRING;
        } else {
            type = value.getType();
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, status);
        try {
            if (value == null) {
                prop.internalSetValue(null, type);
            } else {
                prop.internalSetValue(new InternalValue[]{value}, type);
            }
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * Sets the internal value of a property without checking any constraints.
     * <p/>
     * Note that no type conversion is being performed, i.e. it's the caller's
     * responsibility to make sure that the type of the given values is compatible
     * with the specified property's definition.
     *
     * @param name
     * @param values
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    protected Property internalSetProperty(QName name, InternalValue[] values)
            throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        int type;
        if (values == null || values.length == 0) {
            type = PropertyType.STRING;
        } else {
            type = values[0].getType();
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, status);
        try {
            prop.internalSetValue(values, type);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * Returns the child node of <code>this</code> node with the specified
     * <code>name</code>.
     *
     * @param name The qualified name of the child node to retrieve.
     * @return The child node with the specified <code>name</code>.
     * @throws ItemNotFoundException If no child node exists with the
     *                               specified name.
     * @throws RepositoryException   If another error occurs.
     */
    public NodeImpl getNode(QName name) throws ItemNotFoundException, RepositoryException {
        return getNode(name, 1);
    }

    /**
     * Returns the child node of <code>this</code> node with the specified
     * <code>name</code>.
     *
     * @param name  The qualified name of the child node to retrieve.
     * @param index The index of the child node to retrieve (in the case of same-name siblings).
     * @return The child node with the specified <code>name</code>.
     * @throws ItemNotFoundException If no child node exists with the
     *                               specified name.
     * @throws RepositoryException   If another error occurs.
     */
    public NodeImpl getNode(QName name, int index)
            throws ItemNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();

        NodeState thisState = (NodeState) state;
        NodeState.ChildNodeEntry cne = thisState.getChildNodeEntry(name, index);
        if (cne == null) {
            throw new ItemNotFoundException();
        }
        NodeId nodeId = new NodeId(cne.getUUID());
        try {
            return (NodeImpl) itemMgr.getItem(nodeId);
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException();
        }
    }

    /**
     * Indicates whether a child node with the specified <code>name</code> exists.
     * Returns <code>true</code> if the child node exists and <code>false</code>
     * otherwise.
     *
     * @param name The qualified name of the child node.
     * @return <code>true</code> if the child node exists; <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public boolean hasNode(QName name) throws RepositoryException {
        return hasNode(name, 1);
    }

    /**
     * Indicates whether a child node with the specified <code>name</code> exists.
     * Returns <code>true</code> if the child node exists and <code>false</code>
     * otherwise.
     *
     * @param name  The qualified name of the child node.
     * @param index The index of the child node (in the case of same-name siblings).
     * @return <code>true</code> if the child node exists; <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public boolean hasNode(QName name, int index) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        NodeState thisState = (NodeState) state;
        NodeState.ChildNodeEntry cne = thisState.getChildNodeEntry(name, index);
        if (cne == null) {
            return false;
        }
        NodeId nodeId = new NodeId(cne.getUUID());

        return itemMgr.itemExists(nodeId);
    }

    /**
     * Returns the property of <code>this</code> node with the specified
     * <code>name</code>.
     *
     * @param name The qualified name of the property to retrieve.
     * @return The property with the specified <code>name</code>.
     * @throws ItemNotFoundException If no property exists with the
     *                               specified name.
     * @throws RepositoryException   If another error occurs.
     */
    public PropertyImpl getProperty(QName name)
            throws ItemNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();

        NodeState thisState = (NodeState) state;
        PropertyId propId = new PropertyId(thisState.getUUID(), name);
        try {
            return (PropertyImpl) itemMgr.getItem(propId);
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException(name.toString());
        }
    }

    /**
     * Indicates whether a property with the specified <code>name</code> exists.
     * Returns <code>true</code> if the property exists and <code>false</code>
     * otherwise.
     *
     * @param name The qualified name of the property.
     * @return <code>true</code> if the property exists; <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public boolean hasProperty(QName name) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        NodeState thisState = (NodeState) state;
        if (!thisState.hasPropertyEntry(name)) {
            return false;
        }
        PropertyId propId = new PropertyId(thisState.getUUID(), name);

        return itemMgr.itemExists(propId);
    }

    /**
     * @see Node#addNode(String)
     */
    public synchronized NodeImpl addNode(QName nodeName)
            throws ItemExistsException, ConstraintViolationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        return internalAddChildNode(nodeName, null);
    }

    /**
     * @see Node#addNode(String, String)
     */
    public synchronized NodeImpl addNode(QName nodeName, QName nodeTypeName)
            throws ItemExistsException, NoSuchNodeTypeException,
            ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        NodeTypeImpl nt = session.getNodeTypeManager().getNodeType(nodeTypeName);
        return internalAddChildNode(nodeName, nt);
    }

    /**
     * Same as <code>{@link Node#setProperty(String, String)}</code> except that
     * this method takes a <code>QName</code> instead of a <code>String</code>
     * value.
     *
     * @param name
     * @param value
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public PropertyImpl setProperty(String name, QName value) throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.NAME, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * Same as <code>{@link Node#setProperty(String, String[])}</code> except that
     * this method takes an array of  <code>QName</code> instead of a
     * <code>String</code> values.
     *
     * @param name
     * @param values
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public PropertyImpl setProperty(String name, QName[] values) throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.NAME, true, status);
        try {
            prop.setValue(values);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * Same as <code>{@link Node#setProperty(String, Value[])}</code> except that
     * this method takes a <code>QName</code> name argument instead of a
     * <code>String</code>.
     *
     * @param name
     * @param values
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public PropertyImpl setProperty(QName name, Value[] values)
            throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        int type;
        if (values == null || values.length == 0) {
            type = PropertyType.STRING;
        } else {
            type = values[0].getType();
        }
        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, status);
        try {
            prop.setValue(values);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * Same as <code>{@link Node#setProperty(String, Value)}</code> except that
     * this method takes a <code>QName</code> name argument instead of a
     * <code>String</code>.
     *
     * @param name
     * @param value
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public PropertyImpl setProperty(QName name, Value value)
            throws ValueFormatException, RepositoryException {
        // check state of this instance
        sanityCheck();

        int type = (value == null) ? PropertyType.STRING : value.getType();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see ItemImpl#getQName()
     */
    public QName getQName() throws RepositoryException {
        return session.getHierarchyManager().getName(id);
    }

    //-----------------------------------------------------------------< Item >
    /**
     * @see Item#isNode()
     */
    public boolean isNode() {
        return true;
    }

    /**
     * @see Item#getName
     */
    public String getName() throws RepositoryException {
        if (state.getParentUUID() == null) {
            // this is the root node
            return "";
        }

        //QName name = getPrimaryPath().getNameElement().getName();
        QName name = session.getHierarchyManager().getName(id);
        try {
            return name.toJCRName(session.getNamespaceResolver());
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "internal error: encountered unregistered namespace " + name.getNamespaceURI();
            log.error(msg, npde);
            throw new RepositoryException(msg, npde);
        }
    }

    /**
     * @see Item#accept(ItemVisitor)
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        visitor.visit(this);
    }

    /**
     * @see Item#getParent
     */
    public Node getParent()
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if root node
        NodeState thisState = (NodeState) state;
        if (thisState.getParentUUID() == null) {
            String msg = "root node doesn't have a parent";
            log.error(msg);
            throw new ItemNotFoundException(msg);
        }

        Path path = getPrimaryPath();
        return (Node) getAncestor(path.getAncestorCount() - 1);
    }

    //-----------------------------------------------------------------< Node >
    /**
     * @see Node#addNode(String)
     */
    public synchronized Node addNode(String relPath)
            throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot add a child to a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        return internalAddNode(relPath, null);
    }

    /**
     * @see Node#addNode(String, String)
     */
    public synchronized Node addNode(String relPath, String nodeTypeName)
            throws ItemExistsException, PathNotFoundException,
            NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot add a child to a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        NodeTypeImpl nt = (NodeTypeImpl) session.getNodeTypeManager().getNodeType(nodeTypeName);
        return internalAddNode(relPath, nt);
    }

    /**
     * @see Node#orderBefore(String, String)
     */
    public synchronized void orderBefore(String srcName, String destName)
            throws UnsupportedRepositoryOperationException, VersionException,
            ConstraintViolationException, ItemNotFoundException, LockException,
            RepositoryException {

        if (!nodeType.hasOrderableChildNodes()) {
            throw new UnsupportedRepositoryOperationException("child node ordering not supported on node " + safeGetJCRPath());
        }

        // check arguments
        if (srcName.equals(destName)) {
            throw new ConstraintViolationException("source and destination have to be different");
        }

        Path.PathElement insertName;
        try {
            Path p = Path.create(srcName, session.getNamespaceResolver(), false);
            // p must be a relative path of length==depth==1 (to eliminate e.g. "..")
            if (p.isAbsolute() || p.getLength() != 1 || p.getDepth() != 1) {
                throw new RepositoryException("invalid name: " + srcName);
            }
            insertName = p.getNameElement();
        } catch (MalformedPathException e) {
            String msg = "invalid name: " + srcName;
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }

        Path.PathElement beforeName;
        if (destName != null) {
            try {
                Path p = Path.create(destName, session.getNamespaceResolver(), false);
                // p must be a relative path of length==depth==1 (to eliminate e.g. "..")
                if (p.isAbsolute() || p.getLength() != 1 || p.getDepth() != 1) {
                    throw new RepositoryException("invalid name: " + destName);
                }
                beforeName = p.getNameElement();
            } catch (MalformedPathException e) {
                String msg = "invalid name: " + destName;
                log.error(msg, e);
                throw new RepositoryException(msg, e);
            }
        } else {
            beforeName = null;
        }

        // check existence
        if (!hasNode(srcName)) {
            throw new ItemNotFoundException(safeGetJCRPath() + " has no child node with name " + srcName);
        }
        if (destName != null && !hasNode(destName)) {
            throw new ItemNotFoundException(safeGetJCRPath() + " has no child node with name " + destName);
        }

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot change child node ordering of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        // check protected flag
        if (getDefinition().isProtected()) {
            String msg = safeGetJCRPath() + ": cannot change child node ordering of a protected node";
            log.error(msg);
            throw new ConstraintViolationException(msg);
        }

        ArrayList list = new ArrayList(((NodeState) state).getChildNodeEntries());
        int srcInd = -1, destInd = -1;
        for (int i = 0; i < list.size(); i++) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) list.get(i);
            if (srcInd == -1) {
                if (entry.getName().equals(insertName.getName())
                        && (entry.getIndex() == insertName.getIndex()
                        || insertName.getIndex() == 0 && entry.getIndex() == 1)) {
                    srcInd = i;
                }
            }
            if (destInd == -1 && beforeName != null) {
                if (entry.getName().equals(beforeName.getName())
                        && (entry.getIndex() == beforeName.getIndex()
                        || beforeName.getIndex() == 0 && entry.getIndex() == 1)) {
                    destInd = i;
                    if (srcInd != -1) {
                        break;
                    }
                }
            } else {
                if (srcInd != -1) {
                    break;
                }
            }
        }

        // check if resulting order would be different to current order
        if (destInd == -1) {
            if (srcInd == list.size() - 1) {
                // no change, we're done
                return;
            }
        } else {
            if ((destInd - srcInd) == 1) {
                // no change, we're done
                return;
            }
        }

        // reorder list
        if (destInd == -1) {
            list.add(list.remove(srcInd));
        } else {
            if (srcInd < destInd) {
                list.add(destInd, list.get(srcInd));
                list.remove(srcInd);
            } else {
                list.add(destInd, list.remove(srcInd));
            }
        }

        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        thisState.setChildNodeEntries(list);
    }

    /**
     * @see Node#setProperty(String, Value[])
     */
    public Property setProperty(String name, Value[] values)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        int type;
        if (values == null || values.length == 0) {
            type = PropertyType.UNDEFINED;
        } else {
            type = values[0].getType();
        }
        return setProperty(name, values, type);
    }

    /**
     * @see Node#setProperty(String, Value[], int)
     */
    public Property setProperty(String name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot set property of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, status);
        try {
            prop.setValue(values);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, String[])
     */
    public Property setProperty(String name, String[] values)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        return setProperty(name, values, PropertyType.STRING);
    }

    /**
     * @see Node#setProperty(String, String[], int)
     */
    public Property setProperty(String name, String[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot set property of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, status);
        try {
            prop.setValue(values);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, String)
     */
    public Property setProperty(String name, String value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot set property of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.STRING, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, Value)
     */
    public Property setProperty(String name, Value value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot set property of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        int type = (value == null) ? PropertyType.STRING : value.getType();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, InputStream)
     */
    public Property setProperty(String name, InputStream value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot set property of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.BINARY, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, boolean)
     */
    public Property setProperty(String name, boolean value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot set property of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.BOOLEAN, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, double)
     */
    public Property setProperty(String name, double value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot set property of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.DOUBLE, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, long)
     */
    public Property setProperty(String name, long value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot set property of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.LONG, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, Calendar)
     */
    public Property setProperty(String name, Calendar value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot set property of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.DATE, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, Node)
     */
    public Property setProperty(String name, Node value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write (only cheap call)
        if (!isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": cannot set property of a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, PropertyType.REFERENCE, false, status);
        try {
            prop.setValue(value);
        } catch (RepositoryException re) {
            if (status.get(CREATED)) {
                // setting value failed, get rid of newly created property
                removeChildProperty(name);
            }
            // rethrow
            throw re;
        }
        return prop;
    }

    /**
     * @see Node#getNode(String)
     */
    public Node getNode(String relPath)
            throws PathNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();
/*
        Path nodePath;
        try {
            nodePath = Path.create(getPrimaryPath(), relPath, session.getNamespaceResolver(), true);
        } catch (MalformedPathException e) {
            String msg = "failed to resolve path " + relPath + " relative to " + safeGetJCRPath();
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }

        try {
            Item item = itemMgr.getItem(nodePath);
            if (item.isNode()) {
                return (Node) item;
            } else {
                // there's a property with that name, no child node though
                throw new PathNotFoundException(relPath);
            }
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(relPath);
        }
*/
        NodeId id = resolveRelativeNodePath(relPath);
        if (id == null) {
            throw new PathNotFoundException(relPath);
        }
        try {
            return (Node) itemMgr.getItem(id);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(relPath);
        }
    }

    /**
     * @see Node#getNodes()
     */
    public NodeIterator getNodes() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        /**
         * IMPORTANT:
         * an implementation of Node.getNodes()
         * must not use a class derived from TraversingElementVisitor
         * to traverse the hierarchy because this would lead to an infinite
         * recursion!
         */
        try {
            return itemMgr.getChildNodes((NodeId) id);
        } catch (ItemNotFoundException infe) {
            String msg = "failed to list the child nodes of " + safeGetJCRPath();
            log.error(msg, infe);
            throw new RepositoryException(msg, infe);
        } catch (AccessDeniedException ade) {
            String msg = "failed to list the child nodes of " + safeGetJCRPath();
            log.error(msg, ade);
            throw new RepositoryException(msg, ade);
        }
    }

    /**
     * @see Node#getProperties()
     */
    public PropertyIterator getProperties() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        /**
         * IMPORTANT:
         * an implementation of Node.getProperties()
         * must not use a class derived from TraversingElementVisitor
         * to traverse the hierarchy because this would lead to an infinite
         * recursion!
         */
        try {
            return itemMgr.getChildProperties((NodeId) id);
        } catch (ItemNotFoundException infe) {
            String msg = "failed to list the child properties of " + safeGetJCRPath();
            log.error(msg, infe);
            throw new RepositoryException(msg, infe);
        } catch (AccessDeniedException ade) {
            String msg = "failed to list the child properties of " + safeGetJCRPath();
            log.error(msg, ade);
            throw new RepositoryException(msg, ade);
        }
    }

    /**
     * @see Node#getProperty(String)
     */
    public Property getProperty(String relPath)
            throws PathNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();
/*
        Path propPath;
        try {
            propPath = Path.create(getPrimaryPath(), relPath, session.getNamespaceResolver(), true);
        } catch (MalformedPathException e) {
            String msg = "failed to resolve path " + relPath + " relative to " + safeGetJCRPath();
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }

        try {
            Item item = itemMgr.getItem(propPath);
            if (!item.isNode()) {
                return (Property) item;
            } else {
                // there's a child node with that name, no property though
                throw new PathNotFoundException(relPath);
            }
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(relPath);
        }
*/
        PropertyId id = resolveRelativePropertyPath(relPath);
        if (id == null) {
            throw new PathNotFoundException(relPath);
        }
        try {
            return (Property) itemMgr.getItem(id);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(relPath);
        }
    }

    /**
     * @see Node#hasNode(String)
     */
    public boolean hasNode(String relPath) throws RepositoryException {
/*
        try {
            getNode(relPath);
            return true;
        } catch (PathNotFoundException pnfe) {
            return false;
        }
*/
        // check state of this instance
        sanityCheck();

        NodeId id = resolveRelativeNodePath(relPath);
        return (id != null) ? itemMgr.itemExists(id) : false;
    }

    /**
     * @see Node#hasNodes()
     */
    public boolean hasNodes() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        /**
         * hasNodes respects the access rights
         * of this node's session, i.e. it will
         * return false if child nodes exist
         * but the session is not granted read-access
         */
        return itemMgr.hasChildNodes((NodeId) id);
    }

    /**
     * @see Node#hasProperties()
     */
    public boolean hasProperties() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        /**
         * hasProperties respects the access rights
         * of this node's session, i.e. it will
         * return false if properties exist
         * but the session is not granted read-access
         */
        return itemMgr.hasChildProperties((NodeId) id);
    }

    /**
     * @see Node#isNodeType(String)
     */
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        QName ntName;
        try {
            ntName = QName.fromJCRName(nodeTypeName, session.getNamespaceResolver());
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid node type name: " + nodeTypeName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid node type name: " + nodeTypeName, upe);
        }
        return isNodeType(ntName);
    }

    /**
     * @see Node#isNodeType(String)
     */
    public boolean isNodeType(QName ntName) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        if (ntName.equals(nodeType.getQName())) {
            return true;
        }

        if (nodeType.isDerivedFrom(ntName)) {
            return true;
        }

        // check mixin types
        Set mixinNames = ((NodeState) state).getMixinTypeNames();
        if (mixinNames.isEmpty()) {
            return false;
        }
        NodeTypeRegistry ntReg = session.getNodeTypeManager().getNodeTypeRegistry();
        try {
            EffectiveNodeType ent = ntReg.getEffectiveNodeType((QName[]) mixinNames.toArray(new QName[mixinNames.size()]));
            return ent.includesNodeType(ntName);
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: invalid mixin node type(s)";
            log.error(msg, ntce);
            throw new RepositoryException(msg, ntce);
        }
    }


    /**
     * @see Node#getPrimaryNodeType()
     */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return nodeType;
    }

    /**
     * @see Node#getMixinNodeTypes()
     */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        Set mixinNames = ((NodeState) state).getMixinTypeNames();
        if (mixinNames.isEmpty()) {
            return new NodeType[0];
        }
        NodeType[] nta = new NodeType[mixinNames.size()];
        Iterator iter = mixinNames.iterator();
        int i = 0;
        while (iter.hasNext()) {
            nta[i++] = session.getNodeTypeManager().getNodeType((QName) iter.next());
        }
        return nta;
    }

    /**
     * @see Node#addMixin(String)
     */
    public void addMixin(String mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write
        if (!isCheckedOut(true)) {
            String msg = safeGetJCRPath() + ": cannot add a mixin node type to a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        // check protected flag
        if (definition.isProtected()) {
            String msg = safeGetJCRPath() + ": cannot add a mixin node type to a protected node";
            log.error(msg);
            throw new ConstraintViolationException(msg);
        }

        QName ntName;
        try {
            ntName = QName.fromJCRName(mixinName, session.getNamespaceResolver());
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid mixin type name: " + mixinName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid mixin type name: " + mixinName, upe);
        }

        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeImpl mixin = ntMgr.getNodeType(ntName);
        if (!mixin.isMixin()) {
            throw new RepositoryException(mixinName + ": not a mixin node type");
        }
        if (nodeType.isDerivedFrom(ntName)) {
            throw new RepositoryException(mixinName + ": already contained in primary node type");
        }

        // build effective node type of mixin's & primary type in order to detect conflicts
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        EffectiveNodeType entExisting;
        try {
            // existing mixin's
            HashSet set = new HashSet(((NodeState) state).getMixinTypeNames());
            // primary type
            set.add(nodeType.getQName());
            // build effective node type representing primary type including existing mixin's
            entExisting = ntReg.getEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
            if (entExisting.includesNodeType(ntName)) {
                throw new RepositoryException(mixinName + ": already contained in mixin types");
            }
            // add new mixin
            set.add(ntName);
            // try to build new effective node type (will throw in case of conflicts)
            ntReg.getEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            throw new ConstraintViolationException(ntce.getMessage());
        }

        // do the actual modifications implied by the new mixin;
        // try to revert the changes in case an excpetion occurs
        try {
            // modify the state of this node
            NodeState thisState = (NodeState) getOrCreateTransientItemState();
            // add mixin name
            Set mixins = new HashSet(thisState.getMixinTypeNames());
            mixins.add(ntName);
            thisState.setMixinTypeNames(mixins);

            // set jcr:mixinTypes property
            setMixinTypesProperty(mixins);

            // add 'auto-create' properties defined in mixin type
            PropertyDef[] pda = mixin.getAutoCreatePropertyDefs();
            for (int i = 0; i < pda.length; i++) {
                PropertyDefImpl pd = (PropertyDefImpl) pda[i];
                // make sure that the property is not already defined by primary type
                // or existing mixin's
                NodeTypeImpl declaringNT = (NodeTypeImpl) pd.getDeclaringNodeType();
                if (!entExisting.includesNodeType(declaringNT.getQName())) {
                    createChildProperty(pd.getQName(), pd.getRequiredType(), pd);
                }
            }

            // recursively add 'auto-create' child nodes defined in mixin type
            NodeDef[] nda = mixin.getAutoCreateNodeDefs();
            for (int i = 0; i < nda.length; i++) {
                NodeDefImpl nd = (NodeDefImpl) nda[i];
                // make sure that the child node is not already defined by primary type
                // or existing mixin's
                NodeTypeImpl declaringNT = (NodeTypeImpl) nd.getDeclaringNodeType();
                if (!entExisting.includesNodeType(declaringNT.getQName())) {
                    createChildNode(nd.getQName(), nd, (NodeTypeImpl) nd.getDefaultPrimaryType(), null);
                }
            }
        } catch (RepositoryException re) {
            // try to undo the modifications by removing the mixin
            try {
                removeMixin(mixinName);
            } catch (RepositoryException re1) {
                // silently ignore & fall through
            }
            throw re;
        }
    }

    /**
     * @see Node#removeMixin(String)
     */
    public void removeMixin(String mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write
        if (!isCheckedOut(true)) {
            String msg = safeGetJCRPath() + ": cannot remove a mixin node type from a checked-in node";
            log.error(msg);
            throw new VersionException(msg);
        }

        // check protected flag
        if (definition.isProtected()) {
            String msg = safeGetJCRPath() + ": cannot remove a mixin node type from a protected node";
            log.error(msg);
            throw new ConstraintViolationException(msg);
        }

        QName ntName;
        try {
            ntName = QName.fromJCRName(mixinName, session.getNamespaceResolver());
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid mixin type name: " + mixinName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid mixin type name: " + mixinName, upe);
        }

        // check if mixin is assigned
        if (!((NodeState) state).getMixinTypeNames().contains(ntName)) {
            throw new NoSuchNodeTypeException(mixinName);
        }

        if (NodeTypeRegistry.MIX_REFERENCEABLE.equals(ntName)) {
            /**
             * mix:referenceable needs special handling because it has
             * special semantics:
             * it can only be removed if there no more references to this node
             */
            PropertyIterator iter = getReferences();
            if (iter.hasNext()) {
                throw new ConstraintViolationException(mixinName + " can not be removed: the node is being referenced through at least one property of type REFERENCE");
            }
        }

        // modify the state of this node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        // remove mixin name
        Set mixins = new HashSet(thisState.getMixinTypeNames());
        mixins.remove(ntName);
        thisState.setMixinTypeNames(mixins);

        // set jcr:mixinTypes property
        setMixinTypesProperty(mixins);

        // build effective node type of remaining mixin's & primary type
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        EffectiveNodeType entRemaining;
        try {
            // remaining mixin's
            HashSet set = new HashSet(mixins);
            // primary type
            set.add(nodeType.getQName());
            // build effective node type representing primary type including remaining mixin's
            entRemaining = ntReg.getEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            throw new ConstraintViolationException(ntce.getMessage());
        }

        NodeTypeImpl mixin = session.getNodeTypeManager().getNodeType(ntName);

        // shortcut
        if (mixin.getChildNodeDefs().length == 0
                && mixin.getPropertyDefs().length == 0) {
            // the node type has neither property nor child node definitions,
            // i.e. we're done
            return;
        }

        // walk through properties and child nodes and remove those that have been
        // defined by the specified mixin type

        // use temp array to avoid ConcurrentModificationException
        ArrayList tmp = new ArrayList(thisState.getPropertyEntries());
        Iterator iter = tmp.iterator();
        while (iter.hasNext()) {
            NodeState.PropertyEntry entry = (NodeState.PropertyEntry) iter.next();
            PropertyImpl prop = (PropertyImpl) itemMgr.getItem(new PropertyId(thisState.getUUID(), entry.getName()));
            // check if property has been defined by mixin type (or one of its supertypes)
            NodeTypeImpl declaringNT = (NodeTypeImpl) prop.getDefinition().getDeclaringNodeType();
            if (!entRemaining.includesNodeType(declaringNT.getQName())) {
                // the remaining effective node type doesn't include the
                // node type that declared this property, it is thus safe
                // to remove it
                removeChildProperty(entry.getName());
            }
        }
        // use temp array to avoid ConcurrentModificationException
        tmp = new ArrayList(thisState.getChildNodeEntries());
        iter = tmp.iterator();
        while (iter.hasNext()) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            NodeImpl node = (NodeImpl) itemMgr.getItem(new NodeId(entry.getUUID()));
            // check if node has been defined by mixin type (or one of its supertypes)
            NodeTypeImpl declaringNT = (NodeTypeImpl) node.getDefinition().getDeclaringNodeType();
            if (!entRemaining.includesNodeType(declaringNT.getQName())) {
                // the remaining effective node type doesn't include the
                // node type that declared this child node, it is thus safe
                // to remove it
                removeChildNode(entry.getName(), entry.getIndex());
            }
        }
    }

    /**
     * @see Node#canAddMixin(String)
     */
    public boolean canAddMixin(String mixinName) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versioning allows write
        if (!isCheckedOut(true)) {
            return false;
        }

        // check protected flag
        if (definition.isProtected()) {
            return false;
        }

        QName ntName;
        try {
            ntName = QName.fromJCRName(mixinName, session.getNamespaceResolver());
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid mixin type name: " + mixinName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid mixin type name: " + mixinName, upe);
        }

        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeImpl mixin = ntMgr.getNodeType(ntName);
        if (!mixin.isMixin()) {
            return false;
        }
        if (nodeType.isDerivedFrom(ntName)) {
            return false;
        }

        // build effective node type of mixins & primary type in order to detect conflicts
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        EffectiveNodeType entExisting;
        try {
            // existing mixin's
            HashSet set = new HashSet(((NodeState) state).getMixinTypeNames());
            // primary type
            set.add(nodeType.getQName());
            // build effective node type representing primary type including existing mixin's
            entExisting = ntReg.getEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
            if (entExisting.includesNodeType(ntName)) {
                return false;
            }
            // add new mixin
            set.add(ntName);
            // try to build new effective node type (will throw in case of conflicts)
            ntReg.getEffectiveNodeType((QName[]) set.toArray(new QName[set.size()]));
        } catch (NodeTypeConflictException ntce) {
            return false;
        }

        return true;
    }

    /**
     * @see Node#hasProperty(String)
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
/*
        try {
            getProperty(relPath);
            return true;
        } catch (PathNotFoundException pnfe) {
            return false;
        }
*/
        // check state of this instance
        sanityCheck();

        PropertyId id = resolveRelativePropertyPath(relPath);
        return (id != null) ? itemMgr.itemExists(id) : false;
    }

    /**
     * @see Node#getReferences()
     */
    public PropertyIterator getReferences() throws RepositoryException {
        try {
            NodeReferences refs = stateMgr.getNodeReferences((NodeId) id);
            // refs.getReferences returns a list of PropertyId's
            List idList = refs.getReferences();
            return new LazyItemIterator(itemMgr, idList);
        } catch (ItemStateException e) {
            String msg = "Unable to retrieve node references for: " + id;
            log.error(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see Node#getDefinition()
     */
    public NodeDef getDefinition() throws RepositoryException {
        return definition;
    }

    /**
     * @see Node#getNodes(String)
     */
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        ArrayList nodes = new ArrayList();
        // traverse children using a special filtering 'collector'
        accept(new ChildrenCollectorFilter(namePattern, nodes, true, false, 1));
        return new IteratorHelper(Collections.unmodifiableList(nodes));
    }

    /**
     * @see Node#getProperties(String)
     */
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        ArrayList properties = new ArrayList();
        // traverse children using a special filtering 'collector'
        accept(new ChildrenCollectorFilter(namePattern, properties, false, true, 1));
        return new IteratorHelper(Collections.unmodifiableList(properties));
    }

    /**
     * @see Node#getPrimaryItem()
     */
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();

        String name = nodeType.getPrimaryItemName();
        if (name == null) {
            throw new ItemNotFoundException();
        }
        if (hasProperty(name)) {
            return getProperty(name);
        } else if (hasNode(name)) {
            return getProperty(name);
        } else {
            throw new ItemNotFoundException();
        }
    }

    /**
     * @see Node#getUUID()
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        if (!isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
            throw new UnsupportedRepositoryOperationException();
        }

        return ((NodeState) state).getUUID();
    }

    /**
     * @see Node#getCorrespondingNodePath(String)
     */
    public String getCorrespondingNodePath(String workspaceName)
            throws ItemNotFoundException, NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        if (isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
            String uuid = ((NodeState) state).getUUID();
        }
/*
        // @todo FIXME need to get session with same credentials as current
        SessionImpl srcSession = rep.getSystemSession(srcWorkspaceName);
        Node root = session.getRootNode();
        // if (isRepositoryRoot()) [don't know, if this works correctly with workspaces]
        if (isSame(root)) {
            return (NodeImpl) srcSession.getRootNode();
        }

        // if this node is referenceable, return the corresponding one
        if (isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
            try {
                return (NodeImpl) srcSession.getNodeByUUID(getUUID());
            } catch (ItemNotFoundException e) {
                return null;
            }
        }

        // search nearest ancestor that is referenceable
        NodeImpl m1 = this;
        while (!m1.isSame(root) && !m1.isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
            m1 = (NodeImpl) m1.getParent();
        }
        // special treatment for root
        if (m1.isSame(root)) {
            return (NodeImpl) srcSession.getItem(getPath());
        }

        // calculate relative path. please note, that this cannot be done
        // iteratively in the 'while' loop above, since getName() does not
        // return the relative path, but just the name (without path indices)
        // n1.getPath() = /foo/bar/something[1]
        // m1.getPath() = /foo
        //      relpath = bar/something[1]
        String relPath = getPath().substring(m1.getPath().length() + 1);
        try {
            return (NodeImpl) srcSession.getNodeByUUID(m1.getUUID()).getNode(relPath);
        } catch (ItemNotFoundException e) {
            return null;
        }
*/
        // @todo implement Node#getCorrespondingNodePath
        throw new RepositoryException("not yet implemented");
    }

    /**
     * @see Node#getIndex()
     */
    public int getIndex() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // @todo optimize, no need to build entire path just to find this node's index
        int index = getPrimaryPath().getNameElement().getIndex();
        return (index == 0) ? 1 : index;
    }

    //------------------------------< versioning support: public Node methods >
    /**
     * @see Node#checkin()
     */
    public Version checkin()
            throws VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versionable
        checkVersionable();

        // check if checked out
        // @todo FIXME semantics of isCheckedOut() have changed!
        if (!isCheckedOut()) {
            String msg = safeGetJCRPath() + ": Node is already checked-in. ignoring.";
            log.debug(msg);
            return getBaseVersion();
        }

        // check for pending changes
        if (hasPendingChanges()) {
            String msg = "Unable to checkin node. Node has pending changes: " + safeGetJCRPath();
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        // check if not merge failed
        if (hasProperty(ItemImpl.PROPNAME_MERGE_FAILED) && getProperty(ItemImpl.PROPNAME_MERGE_FAILED).getValues().length>0) {
            String msg = "Unable to checkin node. Clear 'jcr:mergeFailed' first. " + safeGetJCRPath();
            log.debug(msg);
            throw new VersionException(msg);
        }

        Version v = session.versionMgr.checkin(this);
        Property prop = internalSetProperty(VersionManager.PROPNAME_IS_CHECKED_OUT, InternalValue.create(false));
        prop.save();
        prop = internalSetProperty(VersionManager.PROPNAME_BASE_VERSION, InternalValue.create(new UUID(v.getUUID())));
        prop.save();
        prop = internalSetProperty(VersionManager.PROPNAME_PREDECESSORS, new InternalValue[0]);
        prop.save();
        return v;
    }

    /**
     * @see Node#checkout()
     */
    public void checkout()
            throws UnsupportedRepositoryOperationException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if versionable
        checkVersionable();

        // check if already checked out
        if (isCheckedOut(false)) {
            String msg = safeGetJCRPath() + ": Node is already checked-out. ignoring.";
            log.debug(msg);
            return;
        }

        Property prop = internalSetProperty(VersionManager.PROPNAME_IS_CHECKED_OUT, InternalValue.create(true));
        prop.save();
        prop = internalSetProperty(VersionManager.PROPNAME_PREDECESSORS,
                new InternalValue[]{
                    InternalValue.create(new UUID(getBaseVersion().getUUID()))
                });
        prop.save();
    }

    /**
     * @see Node#update(String)
     */
    public void update(String srcWorkspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException,
            LockException, InvalidItemStateException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check for pending changes
        if (session.hasPendingChanges()) {
            String msg = "Unable to checkin node. Session has pending changes.";
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        NodeImpl srcNode = getCorrespondingNode(srcWorkspaceName);
        if (srcNode == null) {
            throw new ItemNotFoundException("No corresponding node for " + safeGetJCRPath());
        }
        // not sure, if clone overrides 'this' node.
        session.getWorkspace().clone(srcWorkspaceName, srcNode.getPath(), getPath(), true);
    }

    /**
     * @see Node#merge(String, boolean)
     */
    public void merge(String srcWorkspace, boolean bestEffort)
            throws UnsupportedRepositoryOperationException,
            NoSuchWorkspaceException, AccessDeniedException, VersionException,
            LockException, InvalidItemStateException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check for pending changes
        if (session.hasPendingChanges()) {
            String msg = "Unable to merge. Session has pending changes.";
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        NodeImpl srcNode = doMergeTest(srcWorkspace, bestEffort);
        if (srcNode != null) {
            // remove properties
            PropertyIterator pi = getProperties();
            while (pi.hasNext()) {
                Property p = pi.nextProperty();
                if (!srcNode.hasProperty(p.getName())) {
                    p.setValue((Value) null);
                }
            }
            // copy properties
            pi = srcNode.getProperties();
            while (pi.hasNext()) {
                PropertyImpl p = (PropertyImpl) pi.nextProperty();
                internalCopyPropertyFrom(p);
            }

            // remove subnodes
            NodeIterator ni = getNodes();
            while (ni.hasNext()) {
                // if the subnode does not exist in the src, and this is update,
                // so delete here as well?
                Node n = ni.nextNode();
                if (!srcNode.hasNode(n.getName())) {
                    // todo: how does this work for same name siblings?
                    n.remove();
                }
            }
            // 'clone' nodes that do not exist
            ni = srcNode.getNodes();
            while (ni.hasNext()) {
                Node n = ni.nextNode();
                if (!hasNode(n.getName())) {
                    // todo: probably need some internal stuff
                    // todo: how does this work for same name siblings?
                    // todo: since clone is a ws operation, 'save' does not work later
                    session.getWorkspace().clone(srcWorkspace, n.getPath(), getPath() + "/" + n.getName(), true);
                } else {
                    // do recursive merge
                    n.merge(srcWorkspace, bestEffort);
                }
            }
        } else {
            // do not change this node, but recuse merge
            NodeIterator ni = srcNode.getNodes();
            while (ni.hasNext()) {
                ni.nextNode().merge(srcWorkspace, bestEffort);
            }
        }

        save();
    }

    /**
     * @see Node#cancelMerge(Version)
     */
    public void cancelMerge(Version version)
            throws VersionException, InvalidItemStateException,
            UnsupportedRepositoryOperationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check for pending changes
        if (hasPendingChanges()) {
            String msg = "Unable to checkin node. Node has pending changes: " + safeGetJCRPath();
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        // @todo implement Node#cancelMerge(Version)
        throw new UnsupportedRepositoryOperationException("not yet implemented");
    }

    /**
     * @see Node#doneMerge(Version)
     */
    public void doneMerge(Version version) throws VersionException,
            InvalidItemStateException, UnsupportedRepositoryOperationException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check for pending changes
        if (hasPendingChanges()) {
            String msg = "Unable to checkin node. Node has pending changes: " + safeGetJCRPath();
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        // @todo implement Node#doneMerge(Version)
        throw new UnsupportedRepositoryOperationException("not yet implemented");
    }

    /**
     * @see Node#isCheckedOut()
     */
    public boolean isCheckedOut() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return isCheckedOut(true);
    }

    /**
     * @see Node#restore(String, boolean)
     */
    public void restore(String versionName, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check for pending changes
        if (session.hasPendingChanges()) {
            String msg = "Unable to restore version. Session has pending changes.";
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        GenericVersionSelector gvs = new GenericVersionSelector();
        gvs.setName(versionName);
        internalRestore(getVersionHistory().getVersion(versionName), gvs, removeExisting);
        save();
    }

    /**
     * @see Node#restore(Version, boolean)
     */
    public void restore(Version version, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check for pending changes
        if (session.hasPendingChanges()) {
            String msg = "Unable to restore version. Session has pending changes.";
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        // check if 'own' version
        if (!version.getParent().getUUID().equals(getVersionHistory().getUUID())) {
            throw new VersionException("Unable to restore version. Not same version history.");
        }
        internalRestore(version, new GenericVersionSelector(version.getCreated()), removeExisting);
        save();
    }

    /**
     * @see Node#restore(Version, String, boolean)
     */
    public void restore(Version version, String relPath, boolean removeExisting)
            throws PathNotFoundException, ItemExistsException, VersionException,
            ConstraintViolationException, UnsupportedRepositoryOperationException,
            LockException, InvalidItemStateException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check for pending changes
        if (session.hasPendingChanges()) {
            String msg = "Unable to restore version. Session has pending changes.";
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        // if node exists, do a 'normal' restore
        if (hasNode(relPath)) {
            getNode(relPath).restore(version, removeExisting);
        } else {
            // recreate node from frozen state
            NodeImpl node = addNode(relPath, ((VersionImpl) version).getFrozenNode());
            node.internalRestore(version, new GenericVersionSelector(version.getCreated()), removeExisting);
            node.getParent().save();
        }
    }

    /**
     * @see Node#restoreByLabel(String, boolean)
     */
    public void restoreByLabel(String versionLabel, boolean removeExisting)
            throws VersionException, ItemExistsException,
            UnsupportedRepositoryOperationException, LockException,
            InvalidItemStateException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check for pending changes
        if (session.hasPendingChanges()) {
            String msg = "Unable to restore version. Session has pending changes.";
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        Version v = getVersionHistory().getVersionByLabel(versionLabel);
        if (v == null) {
            throw new VersionException("No version for label " + versionLabel + " found.");
        }
        internalRestore(v, new GenericVersionSelector(versionLabel), removeExisting);
        save();
    }

    /**
     * @see Node#getVersionHistory()
     */
    public VersionHistory getVersionHistory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        checkVersionable();
        return (VersionHistory) getProperty(VersionManager.PROPNAME_VERSION_HISTORY).getNode();
    }

    /**
     * @see Node#getBaseVersion()
     */
    public Version getBaseVersion()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        checkVersionable();
        return (Version) getProperty(VersionManager.PROPNAME_BASE_VERSION).getNode();
    }

    //-----------------------------------< versioning support: implementation >
    /**
     * Checks if this node is versionable, i.e. has 'mix:versionable'.
     *
     * @throws UnsupportedRepositoryOperationException
     *          if this node is not versionable
     */
    private void checkVersionable()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (!isNodeType(NodeTypeRegistry.MIX_VERSIONABLE)) {
            String msg = "Unable to perform versioning operation on non versionable node: " + safeGetJCRPath();
            log.debug(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
    }

    /**
     * Returns the corresponding node in the <code>scrWorkspaceName</code> of
     * this node.
     * <p/>
     * Given a node N1 in workspace W1, its corresponding node N2 in workspace
     * W2 is defined as follows:
     * <ul>
     * <li>If N1 is the root node of W1 then N2 is the root node of W2.
     * <li>If N1 is referenceable (has a UUID) then N2 is the node in W2 with
     * the same UUID.
     * <li>If N1 is not referenceable (does not have a UUID) then there is some
     * node M1 which is either the nearest ancestor of N1 that is
     * referenceable, or is the root node of W1. If the corresponding node
     * of M1 is M2 in W2, then N2 is the node with the same relative path
     * from M2 as N1 has from M1.
     * </ul>
     *
     * @param srcWorkspaceName
     * @return the corresponding node or <code>null</code> if no corresponding
     *         node exists.
     * @throws NoSuchWorkspaceException If <code>srcWorkspace</code> does not exist.
     * @throws AccessDeniedException    If the current session does not have sufficient rights to perform the operation.
     * @throws RepositoryException      If another error occurs.
     */
    private NodeImpl getCorrespondingNode(String srcWorkspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException,
            RepositoryException {

        // @todo FIXME need to get session with same credentials as current
        SessionImpl srcSession = rep.getSystemSession(srcWorkspaceName);
        Node root = session.getRootNode();
        // if (isRepositoryRoot()) [don't know, if this works correctly with workspaces]
        if (isSame(root)) {
            return (NodeImpl) srcSession.getRootNode();
        }

        // if this node is referenceable, return the corresponding one
        if (isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
            try {
                return (NodeImpl) srcSession.getNodeByUUID(getUUID());
            } catch (ItemNotFoundException e) {
                return null;
            }
        }

        // search nearest ancestor that is referenceable
        NodeImpl m1 = this;
        while (!m1.isSame(root) && !m1.isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
            m1 = (NodeImpl) m1.getParent();
        }
        // special treatment for root
        if (m1.isSame(root)) {
            return (NodeImpl) srcSession.getItem(getPath());
        }

        // calculate relative path. please note, that this cannot be done
        // iteratively in the 'while' loop above, since getName() does not
        // return the relative path, but just the name (without path indices)
        // n1.getPath() = /foo/bar/something[1]
        // m1.getPath() = /foo
        //      relpath = bar/something[1]
        String relPath = getPath().substring(m1.getPath().length() + 1);
        try {
            return (NodeImpl) srcSession.getNodeByUUID(m1.getUUID()).getNode(relPath);
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    /**
     * Performs the merge test. If the result is 'update', then the corresponding
     * source node is returned. if the result is 'leave' or 'besteffort-fail'
     * then <code>null</code> is returned. If the result of the merge test is
     * 'fail' with bestEffort set to <code>false</code> a MergeException is
     * thrown.
     *
     * @param srcWorkspace
     * @param bestEffort
     * @return
     * @throws RepositoryException
     * @throws AccessDeniedException
     */
    private NodeImpl doMergeTest(String srcWorkspace, boolean bestEffort)
            throws RepositoryException, AccessDeniedException {

        // If N does not have a corresponding node then the merge result for N is leave.
        NodeImpl srcNode = getCorrespondingNode(srcWorkspace);
        if (srcNode == null) {
            return null;
        }

        // if not versionable, update
        if (!isNodeType(NodeTypeRegistry.MIX_VERSIONABLE)) {
            return srcNode;
        }
        // if source node is not versionable, leave
        if (!srcNode.isNodeType(NodeTypeRegistry.MIX_VERSIONABLE)) {
            return null;
        }
        // test versions
        InternalVersion v = ((VersionImpl) getBaseVersion()).getInternalVersion();
        InternalVersion vp = ((VersionImpl) srcNode.getBaseVersion()).getInternalVersion();
        if (vp.isMoreRecent(v) && !isCheckedOut()) {
            // I f V' is a successor (to any degree) of V, then the merge result for
            // N is update. This case can be thought of as the case where N' is
            // “newer” than N and therefore N should be updated to reflect N'.
            return srcNode;
        } else if (v.equals(vp) || v.isMoreRecent(vp)) {
            // If V' is a predecessor (to any degree) of V or if V and V' are
            // identical (i.e., are actually the same version), then the merge
            // result for N is leave. This case can be thought of as the case where
            // N' is “older” or the “same age” as N and therefore N should be left alone.
            return null;
        } else {
            // If V is neither a successor of, predecessor of, nor identical
            // with V', then the merge result for N is failed. This is the case
            // where N and N' represent divergent branches of the version graph,
            // thus determining the result of a merge is non-trivial.
            if (bestEffort) {
                // add 'offending' version to jcr:mergeFailed property
                List values = hasProperty(ItemImpl.PROPNAME_MERGE_FAILED)
                        ? Arrays.asList(getProperty(ItemImpl.PROPNAME_MERGE_FAILED).getValues())
                        : new ArrayList();
                values.add(new ReferenceValue(srcNode.getBaseVersion()));
                setProperty(ItemImpl.PROPNAME_MERGE_FAILED, (Value[]) values.toArray(new Value[values.size()]));
                return null;
            } else {
                String msg = "Unable to merge nodes. Violating versions. " + safeGetJCRPath();
                log.debug(msg);
                throw new MergeException(msg);
            }
        }
    }

    /**
     * Same as {@link Node#isCheckedOut()} but if <code>inherit</code>
     * is <code>true</code>, a non-versionable node will return the checked out
     * state of its parent.
     *
     * @param inherit
     * @see Node#isCheckedOut()
     */
    protected boolean isCheckedOut(boolean inherit) throws RepositoryException {
        // search nearest ancestor that is versionable
        NodeImpl node = this;
        while (!node.hasProperty(VersionManager.PROPNAME_IS_CHECKED_OUT)) {
            if (!inherit || node.isRepositoryRoot()) {
                return true;
            }
            node = (NodeImpl) node.getParent();
        }
        return node.getProperty(VersionManager.PROPNAME_IS_CHECKED_OUT).getBoolean();
    }

    /**
     * Creates a new node at <code>relPath</code> of the node type, uuid and
     * eventual mixin types stored in the frozen node. The same as
     * <code>{@link #addNode(String relPath)}</code> except that the primary
     * node type type, the uuid and evt. mixin type of the new node is
     * explictly specified in the nt:frozen node.
     * <p/>
     *
     * @param name   The name of the new <code>Node</code> that is to be created.
     * @param frozen The frozen node that contains the creation information
     * @return The node that was added.
     * @throws ItemExistsException          If an item at the
     *                                      specified path already exists(and same-name siblings are not allowed).
     * @throws PathNotFoundException        If specified path implies intermediary
     *                                      <code>Node</code>s that do not exist.
     * @throws NoSuchNodeTypeException      If the specified <code>nodeTypeName</code>
     *                                      is not recognized.
     * @throws ConstraintViolationException If an attempt is made to add a node as the
     *                                      child of a <code>Property</code>
     * @throws RepositoryException          if another error occurs.
     */
    private NodeImpl addNode(QName name, InternalFrozenNode frozen)
            throws ItemExistsException, PathNotFoundException,
            ConstraintViolationException, NoSuchNodeTypeException,
            RepositoryException {

        // get frozen node type
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeImpl nt = ntMgr.getNodeType(frozen.getFrozenPrimaryType());

        // get frozen uuid
        String uuid = frozen.getFrozenUUID();

        NodeImpl node = internalAddChildNode(name, nt, uuid);

        // get frozen mixin
        // todo: also respect mixing types on creation?
        QName[] mxNames = frozen.getFrozenMixinTypes();
        for (int i = 0; i < mxNames.length; i++) {
            try {
                node.addMixin(mxNames[i].toJCRName(session.getNamespaceResolver()));
            } catch (NoPrefixDeclaredException e) {
                throw new NoSuchNodeTypeException("Unable to resolve mixin: " + e.toString());
            }
        }
        return node;
    }

    /**
     * Creates a new node at <code>relPath</code> of the node type, uuid and
     * eventual mixin types stored in the frozen node. The same as
     * <code>{@link #addNode(String relPath)}</code> except that the primary
     * node type type, the uuid and evt. mixin type of the new node is
     * explictly specified in the nt:frozen node.
     * <p/>
     *
     * @param relPath The path of the new <code>Node</code> that is to be created.
     * @param frozen  The frozen node that contains the creation information
     * @return The node that was added.
     * @throws ItemExistsException          If an item at the
     *                                      specified path already exists(and same-name siblings are not allowed).
     * @throws PathNotFoundException        If specified path implies intermediary
     *                                      <code>Node</code>s that do not exist.
     * @throws NoSuchNodeTypeException      If the specified <code>nodeTypeName</code>
     *                                      is not recognized.
     * @throws ConstraintViolationException If an attempt is made to add a node as the
     *                                      child of a <code>Property</code>
     * @throws RepositoryException          if another error occurs.
     */
    private NodeImpl addNode(String relPath, InternalFrozenNode frozen)
            throws ItemExistsException, PathNotFoundException,
            ConstraintViolationException, NoSuchNodeTypeException,
            RepositoryException {

        // get frozen node type
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeImpl nt = ntMgr.getNodeType(frozen.getFrozenPrimaryType());

        // get frozen uuid
        String uuid = frozen.getFrozenUUID();

        NodeImpl node = internalAddNode(relPath, nt, uuid);

        // get frozen mixin
        // todo: also respect mixing types on creation?
        QName[] mxNames = frozen.getFrozenMixinTypes();
        for (int i = 0; i < mxNames.length; i++) {
            try {
                node.addMixin(mxNames[i].toJCRName(session.getNamespaceResolver()));
            } catch (NoPrefixDeclaredException e) {
                throw new NoSuchNodeTypeException("Unable to resolve mixin: " + e.toString());
            }
        }
        return node;
    }


    /**
     * Internal method to restore a version.
     *
     * @param version
     * @param vsel    the version selector that will select the correct version for
     *                OPV=Version childnodes.
     * @throws UnsupportedRepositoryOperationException
     *
     * @throws RepositoryException
     */
    private void internalRestore(Version version, VersionSelector vsel, boolean removeExisting)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        internalRestore(((VersionImpl) version).getInternalVersion(), vsel, removeExisting);
    }

    /**
     * Checks if any frozen uuid in the given frozen node or its descendants
     * collides with the one in the workspace. if 'removeExisting' is true,
     * collisions will be removed, otherwise an ItemExistsException is thrown.
     * If a frozen version history is already restored outside this nodes
     * subtree, a exception is thrown, too, if the removeExisting is true.
     * @param f
     * @param removeExisting
     * @throws RepositoryException
     */
    private void checkUUIDCollisions(InternalFrozenNode f, boolean removeExisting)
            throws RepositoryException {

        if (itemMgr.itemExists(new NodeId(f.getFrozenUUID()))) {
            NodeImpl node = (NodeImpl) session.getNodeByUUID(f.getFrozenUUID());
            if (removeExisting) {
                node.remove();
            } else {
                throw new ItemExistsException("Unable to restore. UUID collides with " + node.safeGetJCRPath());
            }
        }
        InternalFreeze[] fs = f.getFrozenChildNodes();
        for (int i=0; i<fs.length; i++) {
            if (fs[i] instanceof InternalFrozenNode) {
                checkUUIDCollisions((InternalFrozenNode) fs[i], removeExisting);
            } else if (!removeExisting) {
                InternalFrozenVersionHistory fh = (InternalFrozenVersionHistory) fs[i];
                VersionHistory history = (VersionHistory) session.getNodeByUUID(fh.getVersionHistoryId());
                String nodeId = history.getName(); // this is implementation detail!

                // check if representing vh already exists somewhere
                if (itemMgr.itemExists(new NodeId(nodeId))) {
                    NodeImpl n = (NodeImpl) session.getNodeByUUID(nodeId);
                    try {
                        if (!n.getPrimaryPath().isDescendantOf(getPrimaryPath())) {
                            throw new ItemExistsException("Unable to restore. Same node already restored at " + n.safeGetJCRPath());
                        }
                    } catch (MalformedPathException e) {
                        throw new RepositoryException(e);
                    }
                }
            }
        }
    }

    /**
     * Internal method to restore a version.
     *
     * @param version
     * @param vsel    the version selector that will select the correct version for
     *                OPV=Version childnodes.
     * @param removeExisting
     *
     * @throws RepositoryException
     */
    private void internalRestore(InternalVersion version, VersionSelector vsel, boolean removeExisting)
            throws RepositoryException {

        // first check, if any uuid conflicts would occurr
        checkUUIDCollisions(version.getFrozenNode(), removeExisting);

        // set jcr:isCheckedOut property to true, in order to avoid any conflicts
        internalSetProperty(VersionManager.PROPNAME_IS_CHECKED_OUT, InternalValue.create(true));

        // 1. The child node and properties of N will be changed, removed or
        //    added to, depending on their corresponding copies in V and their
        //    own OnParentVersion attributes (see 7.2.8, below, for details).
        restoreFrozenState(version.getFrozenNode(), vsel, removeExisting);

        // 2. N’s jcr:baseVersion property will be changed to point to V.
        internalSetProperty(VersionManager.PROPNAME_BASE_VERSION, InternalValue.create(new UUID(version.getId())));

        // 4. N's jcr:predecessor property is set to null
        internalSetProperty(VersionManager.PROPNAME_PREDECESSORS, new InternalValue[0]);

        // 3. N’s jcr:isCheckedOut property is set to false.
        internalSetProperty(VersionManager.PROPNAME_IS_CHECKED_OUT, InternalValue.create(false));
    }

    /**
     * Creates the frozen state from a node
     *
     * @param freeze
     * @throws RepositoryException
     */
    void restoreFrozenState(InternalFrozenNode freeze, VersionSelector vsel, boolean removeExisting)
            throws RepositoryException {
        // check uuid
        if (isNodeType(NodeTypeRegistry.MIX_REFERENCEABLE)) {
            String uuid = freeze.getFrozenUUID();
            if (uuid != null && !uuid.equals(getUUID())) {
                throw new ItemExistsException("Unable to restore version of " + safeGetJCRPath() + ". UUID changed.");
            }
        }
        // check primarty type
        if (!freeze.getFrozenPrimaryType().equals(nodeType.getQName())) {
            // todo: check with spec what should happen here
            throw new ItemExistsException("Unable to restore version of " + safeGetJCRPath() + ". PrimaryType changed.");
        }
        // adjust mixins
        QName[] values = freeze.getFrozenMixinTypes();
        NodeType[] mixins = getMixinNodeTypes();
        for (int i = 0; i < values.length; i++) {
            boolean found = false;
            for (int j = 0; j < mixins.length; j++) {
                if (values[i].equals(((NodeTypeImpl) mixins[j]).getQName())) {
                    // clear
                    mixins[j] = null;
                    found = true;
                    break;
                }
            }
            if (!found) {
                try {
                    addMixin(values[i].toJCRName(session.getNamespaceResolver()));
                } catch (NoPrefixDeclaredException e) {
                    String msg = "Unable to add mixin for restored node: " + e.getMessage();
                    log.error(msg);
                    throw new RepositoryException(msg);
                }
            }
        }
        // remove additional
        for (int i = 0; i < mixins.length; i++) {
            if (mixins[i] != null) {
                removeMixin(mixins[i].getName());
            }
        }

        // copy frozen properties
        PropertyState[] props = freeze.getFrozenProperties();
        HashSet propNames = new HashSet();
        for (int i = 0; i < props.length; i++) {
            PropertyState prop = props[i];
            propNames.add(prop.getName());
            if (prop.isMultiValued()) {
                internalSetProperty(props[i].getName(), prop.getValues());
            } else {
                internalSetProperty(props[i].getName(), prop.getValues()[0]);
            }
        }
        // remove properties that do not exist the the frozen representation
        PropertyIterator piter = getProperties();
        while (piter.hasNext()) {
            PropertyImpl prop = (PropertyImpl) piter.nextProperty();
            // ignore some props that are not well guarded by the OPV
            if (prop.getQName().equals(VersionManager.PROPNAME_VERSION_HISTORY)) {
                continue;
            } else if (prop.getQName().equals(VersionManager.PROPNAME_PREDECESSORS)) {
                continue;
            }
            if (prop.getDefinition().getOnParentVersion() == OnParentVersionAction.COPY
                    || prop.getDefinition().getOnParentVersion() == OnParentVersionAction.VERSION) {
                if (!propNames.contains(prop.getQName())) {
                    removeChildProperty(prop.getQName());
                }
            }
        }

        // restore the frozen nodes
        InternalFreeze[] frozenNodes = freeze.getFrozenChildNodes();

        // first delete all non frozen version histories
        NodeIterator iter = getNodes();
        while (iter.hasNext()) {
            NodeImpl n = (NodeImpl) iter.nextNode();
            // this is a bit lousy
            boolean found = false;
            for (int i=0; i<frozenNodes.length; i++) {
                InternalFreeze child = frozenNodes[i];
                if (child instanceof InternalFrozenVersionHistory) {
                    if (n.internalGetUUID().equals(child.getId())) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                n.remove();
            }
        }
        for (int i = 0; i < frozenNodes.length; i++) {
            InternalFreeze child = frozenNodes[i];
            if (child instanceof InternalFrozenNode) {
                InternalFrozenNode f = (InternalFrozenNode) child;
                NodeImpl n = addNode(f.getName(), f);
                n.restoreFrozenState(f, vsel, removeExisting);
            } else if (child instanceof InternalFrozenVersionHistory) {
                InternalFrozenVersionHistory f = (InternalFrozenVersionHistory) child;
                VersionHistory history = (VersionHistory) session.getNodeByUUID(f.getVersionHistoryId());
                String nodeId = history.getName(); // this is implementation detail!

                // check if representing vh already exists somewhere
                if (itemMgr.itemExists(new NodeId(nodeId))) {
                    NodeImpl n = (NodeImpl) session.getNodeByUUID(nodeId);
                    if (hasNode(n.getQName())) {
                        // so order at end
                        orderBefore(n.getName(), "");
                    } else {
                        session.move(n.getPath(), getPath()+ "/" + n.getName());
                    }
                } else {
                    // get desired version from version selector
                    InternalVersion v = ((VersionImpl) vsel.select(history)).getInternalVersion();
                    NodeImpl node = addNode(child.getName(), v.getFrozenNode());
                    node.internalRestore(v, vsel, removeExisting);
                }
            }
        }
    }

    /**
     * Copies a property to this node
     *
     * @param prop
     * @throws RepositoryException
     */
    protected void internalCopyPropertyFrom(PropertyImpl prop) throws RepositoryException {
        if (prop.getDefinition().isMultiple()) {
            Value[] values = prop.getValues();
            InternalValue[] ivalues = new InternalValue[values.length];
            for (int i = 0; i < values.length; i++) {
                ivalues[i] = InternalValue.create(values[i], session.getNamespaceResolver());
            }
            internalSetProperty(prop.getQName(), ivalues);
        } else {
            InternalValue value = InternalValue.create(prop.getValue(), session.getNamespaceResolver());
            internalSetProperty(prop.getQName(), value);
        }
    }

    //------------------------------------------------------< locking support >
    /**
     * @see Node#lock(boolean, boolean)
     */
    public Lock lock(boolean isDeep, boolean isSessionScoped)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check for pending changes
        if (hasPendingChanges()) {
            String msg = "Unable to checkin node. Node has pending changes: " + safeGetJCRPath();
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        checkLockable();

        // @todo implement locking support
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @see Node#getLock()
     */
    public Lock getLock()
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        checkLockable();

        // @todo implement locking support
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @see Node#unlock()
     */
    public void unlock()
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        // check state of this instance
        sanityCheck();

        // check for pending changes
        if (hasPendingChanges()) {
            String msg = "Unable to checkin node. Node has pending changes: " + safeGetJCRPath();
            log.error(msg);
            throw new InvalidItemStateException(msg);
        }

        checkLockable();

        // @todo implement locking support
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * @see Node#holdsLock()
     */
    public boolean holdsLock() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // @todo implement locking support
        return false;
    }

    /**
     * @see Node#isLocked()
     */
    public boolean isLocked() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // @todo implement locking support
        return false;
    }

    /**
     * Checks if this node is lockable, i.e. has 'mix:lockable'.
     *
     * @throws UnsupportedRepositoryOperationException
     *          if this node is not lockable
     * @throws RepositoryException if another error occurs
     */
    private void checkLockable()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (!isNodeType(NodeTypeRegistry.MIX_LOCKABLE)) {
            String msg = "Unable to perform locking operation on non-lockable node: " + safeGetJCRPath();
            log.debug(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
    }
}
