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
package org.apache.jackrabbit.core;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidLifecycleTransitionException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.query.QueryManagerImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.*;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.util.ChildrenCollectorFilter;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.value.ValueHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>NodeImpl</code> implements the <code>Node</code> interface.
 */
public class NodeImpl extends ItemImpl implements Node {

    private static Logger log = LoggerFactory.getLogger(NodeImpl.class);

    // flag set in status passed to getOrCreateProperty if property was created
    protected static final short CREATED = 0;

    /** node data (avoids casting <code>ItemImpl.data</code>) */
    private final AbstractNodeData data;

    /**
     * Protected constructor.
     *
     * @param itemMgr    the <code>ItemManager</code> that created this <code>Node</code> instance
     * @param session    the <code>Session</code> through which this <code>Node</code> is acquired
     * @param data       the node data
     */
    protected NodeImpl(ItemManager itemMgr, SessionImpl session, AbstractNodeData data) {
        super(itemMgr, session, data);
        this.data = data;
        // paranoid sanity check
        NodeTypeRegistry ntReg = session.getNodeTypeManager().getNodeTypeRegistry();
        final NodeState state = data.getNodeState();
        if (!ntReg.isRegistered(state.getNodeTypeName())) {
            /**
             * todo need proper way of handling inconsistent/corrupt node type references
             * e.g. 'flag' nodes that refer to non-registered node types
             */
            log.warn("Fallback to nt:unstructured due to unknown node type '"
                    + state.getNodeTypeName() + "' of " + this);
            data.getNodeState().setNodeTypeName(NameConstants.NT_UNSTRUCTURED);
        }
        List<Name> unknown = null;
        for (Name mixinName : state.getMixinTypeNames()) {
            if (!ntReg.isRegistered(mixinName)) {
                if (unknown == null) {
                    unknown = new ArrayList<Name>();
                }
                unknown.add(mixinName);
                log.warn("Ignoring unknown mixin type '" + mixinName +
                        "' of " + this);
            }
        }
        if (unknown != null) {
            // ignore unknown mixin type names
            Set<Name> known = new HashSet<Name>(state.getMixinTypeNames());
            known.removeAll(unknown);
            state.setMixinTypeNames(known);
        }
    }

    /**
     * Returns the node-state associated with this node.
     *
     * @return state associated with this node
     */
    NodeState getNodeState() {
        return data.getNodeState();
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
            if (relPath.indexOf('/') == -1) {
                Name propName = session.getQName(relPath);
                // check if property entry exists
                NodeState thisState = data.getNodeState();
                if (thisState.hasPropertyName(propName)) {
                    return new PropertyId(thisState.getNodeId(), propName);
                } else {
                    // there's no property with that name
                    return null;
                }
            }
            /**
             * build and resolve absolute path
             */
            Path p = PathFactoryImpl.getInstance().create(
                    getPrimaryPath(), session.getQPath(relPath), true);
            return session.getHierarchyManager().resolvePropertyPath(p);
        } catch (NameException e) {
            String msg = "failed to resolve path " + relPath + " relative to " + this;
            log.debug(msg);
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

        Path p = resolveRelativePath(relPath);
        return getNodeId(p);
    }

    /**
     * Resolve a relative path given as string into a <code>Path</code>. If
     * a <code>NameException</code> occurs, it will be rethrown embedded
     * into a <code>RepositoryException</code>
     *
     * @param relPath relative path
     * @return <code>Path</code> object
     * @throws RepositoryException if an error occurs
     */
    private Path resolveRelativePath(String relPath) throws RepositoryException {
        try {
            return session.getQPath(relPath);
        } catch (NameException e) {
            String msg = "failed to resolve path " + relPath + " relative to " + this;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Returns the id of the node at <code>p</code> or <code>null</code>
     * if no node exists at <code>p</code>.
     * <p/>
     * Note that access rights are not checked.
     *
     * @param p relative path of a (possible) node
     * @return the id of the node at <code>p</code> or
     *         <code>null</code> if no node exists at <code>p</code>
     * @throws RepositoryException if <code>relPath</code> is not a valid
     *                             relative path
     */
    private NodeId getNodeId(Path p) throws RepositoryException {
        if (p.getLength() == 1) {
            Path.Element pe = p.getNameElement();
            if (pe.denotesName()) {
                // check if node entry exists
                NodeState thisState = data.getNodeState();
                int index = pe.getIndex();
                if (index == 0) {
                    index = 1;
                }
                ChildNodeEntry cne =
                        thisState.getChildNodeEntry(pe.getName(), index);
                if (cne != null) {
                    return cne.getId();
                } else {
                    // there's no child node with that name
                    return null;
                }
            }
        }
        /**
         * build and resolve absolute path
         */
        p = PathFactoryImpl.getInstance().create(getPrimaryPath(), p, true);
        return session.getHierarchyManager().resolveNodePath(p);
    }

    /**
     * Determines if there are pending unsaved changes either on <i>this</i>
     * node or on any node or property in the subtree below it.
     *
     * @return <code>true</code> if there are pending unsaved changes,
     *         <code>false</code> otherwise.
     * @throws RepositoryException if an error occurred
     */
    protected boolean hasPendingChanges() throws RepositoryException {
        if (isTransient()) {
            return true;
        }
        Iterator<ItemState> iter = stateMgr.getDescendantTransientItemStates((NodeId) id);
        return iter.hasNext();
    }

    protected synchronized ItemState getOrCreateTransientItemState()
            throws RepositoryException {

        synchronized (data) {
            if (!isTransient()) {
                try {
                    // make transient (copy-on-write)
                    NodeState transientState =
                            stateMgr.createTransientNodeState(
                                    data.getNodeState(), ItemState.STATUS_EXISTING_MODIFIED);
                    // replace persistent with transient state
                    data.setState(transientState);
                } catch (ItemStateException ise) {
                    String msg = "failed to create transient state";
                    log.debug(msg);
                    throw new RepositoryException(msg, ise);
                }
            }
            return getItemState();
        }
    }

    /**
     * @param name
     * @param type
     * @param multiValued
     * @param exactTypeMatch
     * @param status
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    protected PropertyImpl getOrCreateProperty(String name, int type,
                                               boolean multiValued,
                                               boolean exactTypeMatch,
                                               BitSet status)
            throws ConstraintViolationException, RepositoryException {
        try {
            return getOrCreateProperty(
                    session.getQName(name), type,
                    multiValued, exactTypeMatch, status);
        } catch (NameException e) {
            throw new RepositoryException("invalid property name: " + name, e);
        }
    }

    /**
     * @param name
     * @param type
     * @param multiValued
     * @param exactTypeMatch
     * @param status
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    protected synchronized PropertyImpl getOrCreateProperty(Name name, int type,
                                                            boolean multiValued,
                                                            boolean exactTypeMatch,
                                                            BitSet status)
            throws ConstraintViolationException, RepositoryException {
        status.clear();

        if (isNew() && !hasProperty(name)) {
            // this is a new node and the property does not exist yet
            // -> no need to check item manager
            PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                    name, type, multiValued, exactTypeMatch);
            PropertyImpl prop = createChildProperty(name, type, def);
            status.set(CREATED);
            return prop;
        }


        /*
         * Please note, that this implementation does not win a price for beauty
         * or speed. It's never a good idea to use exceptions for semantical
         * control flow.
         * However, compared to the previous version, this one is thread save
         * and makes the test/get block atomic in respect to transactional
         * commits. the test/set can still fail.
         *
         * Old Version:

            NodeState thisState = (NodeState) state;
            if (thisState.hasPropertyName(name)) {
                /**
                 * the following call will throw ItemNotFoundException if the
                 * current session doesn't have read access
                 /
                return getProperty(name);
            }
            [...create block...]

        */
        try {
            PropertyId propId = new PropertyId(getNodeId(), name);
            return (PropertyImpl) itemMgr.getItem(propId);
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException(name.toString());
        } catch (ItemNotFoundException e) {
            // does not exist yet:
            // find definition for the specified property and create property
            PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                    name, type, multiValued, exactTypeMatch);
            PropertyImpl prop = createChildProperty(name, type, def);
            status.set(CREATED);
            return prop;
        }
    }

    /**
     * Creates a new property with the given name and <code>type</code> hint and
     * property definition. If the given property definition is not of type
     * <code>UNDEFINED</code>, then it takes precendence over the
     * <code>type</code> hint.
     *
     * @param name the name of the property to create.
     * @param type the type hint.
     * @param def  the associated property definition.
     * @return the property instance.
     * @throws RepositoryException if the property cannot be created.
     */
    protected synchronized PropertyImpl createChildProperty(Name name, int type,
                                                            PropertyDefinitionImpl def)
            throws RepositoryException {

        // create a new property state
        PropertyState propState;
        try {
            QPropertyDefinition propDef = def.unwrap();
            if (def.getRequiredType() != PropertyType.UNDEFINED) {
                type = def.getRequiredType();
            }
            propState =
                    stateMgr.createTransientPropertyState(getNodeId(), name,
                            ItemState.STATUS_NEW);
            propState.setType(type);
            propState.setMultiValued(propDef.isMultiple());
            // compute system generated values if necessary
            InternalValue[] genValues = session.getNodeTypeInstanceHandler()
                    .computeSystemGeneratedPropertyValues(data.getNodeState(), propDef);
            if (genValues == null) {
                genValues = InternalValue.create(propDef.getDefaultValues());
            }
            if (genValues != null) {
                propState.setValues(genValues);
            }
        } catch (ItemStateException ise) {
            String msg = "failed to add property " + name + " to " + this;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }

        // create Property instance wrapping new property state
        // NOTE: since the property is not yet connected to its parent, avoid
        // calling ItemManager#getItem(ItemId) which may include a permission
        // check (with subsequent usage of the hierarachy-mgr -> error).
        // just let the mgr create the new property that is known to exist and
        // which has not been accessed before.
        PropertyImpl prop = (PropertyImpl) itemMgr.createItemInstance(propState);

        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        // add new property entry
        thisState.addPropertyName(name);

        return prop;
    }

    protected synchronized NodeImpl createChildNode(Name name,
                                                    NodeTypeImpl nodeType,
                                                    NodeId id)
            throws RepositoryException {
        // create a new node state
        NodeState nodeState;
        try {
            if (id == null) {
                id = new NodeId();
            }
            nodeState =
                    stateMgr.createTransientNodeState(id, nodeType.getQName(),
                            getNodeId(), ItemState.STATUS_NEW);
        } catch (ItemStateException ise) {
            String msg = "failed to add child node " + name + " to " + this;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }

        // create Node instance wrapping new node state
        NodeImpl node;
        try {
            // NOTE: since the node is not yet connected to its parent, avoid
            // calling ItemManager#getItem(ItemId) which may include a permission
            // check (with subsequent usage of the hierarachy-mgr -> error).
            // just let the mgr create the new node that is known to exist and
            // which has not been accessed before.
            node = (NodeImpl) itemMgr.createItemInstance(nodeState);
        } catch (RepositoryException re) {
            // something went wrong
            stateMgr.disposeTransientItemState(nodeState);
            // re-throw
            throw re;
        }

        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        // add new child node entry
        thisState.addChildNodeEntry(name, nodeState.getNodeId());

        // add 'auto-create' properties defined in node type
        for (PropertyDefinition aPda : nodeType.getAutoCreatedPropertyDefinitions()) {
            PropertyDefinitionImpl pd = (PropertyDefinitionImpl) aPda;
            node.createChildProperty(pd.unwrap().getName(), pd.getRequiredType(), pd);
        }

        // recursively add 'auto-create' child nodes defined in node type
        for (NodeDefinition aNda : nodeType.getAutoCreatedNodeDefinitions()) {
            NodeDefinitionImpl nd = (NodeDefinitionImpl) aNda;
            node.createChildNode(nd.unwrap().getName(), (NodeTypeImpl) nd.getDefaultPrimaryType(), null);
        }

        return node;
    }

    protected void renameChildNode(Name oldName, int index, NodeId id,
                                   Name newName)
            throws RepositoryException {
        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        thisState.renameChildNodeEntry(oldName, index, newName);
    }

    protected void removeChildProperty(String propName) throws RepositoryException {
        try {
            removeChildProperty(session.getQName(propName));
        } catch (NameException e) {
            throw new RepositoryException(
                    "invalid property name: " + propName, e);
        }
    }

    protected void removeChildProperty(Name propName) throws RepositoryException {
        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();

        // remove the property entry
        if (!thisState.removePropertyName(propName)) {
            String msg = "failed to remove property " + propName + " of " + this;
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // remove property
        PropertyId propId = new PropertyId(thisState.getNodeId(), propName);
        itemMgr.getItem(propId).setRemoved();
    }

    protected void removeChildNode(Name nodeName, int index)
            throws RepositoryException {
        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        if (index == 0) {
            index = 1;
        }
        ChildNodeEntry entry =
                thisState.getChildNodeEntry(nodeName, index);
        if (entry == null) {
            String msg = "failed to remove child " + nodeName + " of " + this;
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // notify target of removal
        NodeId childId = entry.getId();
        NodeImpl childNode = itemMgr.getNode(childId, getNodeId());
        childNode.onRemove(getNodeId());

        // remove the child node entry
        if (!thisState.removeChildNodeEntry(nodeName, index)) {
            String msg = "failed to remove child " + nodeName + " of " + this;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
    }

    protected void onRedefine(QNodeDefinition def) throws RepositoryException {
        NodeDefinitionImpl newDef =
                session.getNodeTypeManager().getNodeDefinition(def);
        // modify the state of 'this', i.e. the target node
        getOrCreateTransientItemState();
        // set new definition
        data.setDefinition(newDef);
    }

    protected void onRemove(NodeId parentId) throws RepositoryException {
        // modify the state of 'this', i.e. the target node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();

        // remove this node from its shared set
        if (thisState.isShareable()) {
            if (thisState.removeShare(parentId) > 0) {
                // this state is still connected to some parents, so
                // leave the child node entries and properties

                // set state of this instance to 'invalid'
                data.setStatus(STATUS_INVALIDATED);
                // notify the item manager that this instance has been
                // temporarily invalidated
                itemMgr.itemInvalidated(id, data);
                return;
            }
        }

        if (thisState.hasChildNodeEntries()) {
            // remove child nodes
            // use temp array to avoid ConcurrentModificationException
            ArrayList<ChildNodeEntry> tmp = new ArrayList<ChildNodeEntry>(thisState.getChildNodeEntries());
            // remove from tail to avoid problems with same-name siblings
            for (int i = tmp.size() - 1; i >= 0; i--) {
                ChildNodeEntry entry =
                        tmp.get(i);
                // recursively remove child node
                NodeId childId = entry.getId();
                //NodeImpl childNode = (NodeImpl) itemMgr.getItem(childId);
                NodeImpl childNode = itemMgr.getNode(childId, getNodeId());
                childNode.onRemove(thisState.getNodeId());
                // remove the child node entry
                thisState.removeChildNodeEntry(entry.getName(), entry.getIndex());
            }
        }

        // remove properties
        // use temp set to avoid ConcurrentModificationException
        HashSet<Name> tmp = new HashSet<Name>(thisState.getPropertyNames());
        for (Name propName : tmp) {
            // remove the property entry
            thisState.removePropertyName(propName);
            // remove property
            PropertyId propId = new PropertyId(thisState.getNodeId(), propName);
            itemMgr.getItem(propId).setRemoved();
        }

        // finally remove this node
        thisState.setParentId(null);
        setRemoved();
    }

    private void setMixinTypesProperty(Set<Name> mixinNames) throws RepositoryException {
        NodeState thisState = data.getNodeState();
        // get or create jcr:mixinTypes property
        PropertyImpl prop;
        if (thisState.hasPropertyName(NameConstants.JCR_MIXINTYPES)) {
            prop = (PropertyImpl) itemMgr.getItem(new PropertyId(thisState.getNodeId(), NameConstants.JCR_MIXINTYPES));
        } else {
            // find definition for the jcr:mixinTypes property and create property
            PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                    NameConstants.JCR_MIXINTYPES, PropertyType.NAME, true, true);
            prop = createChildProperty(NameConstants.JCR_MIXINTYPES, PropertyType.NAME, def);
        }

        if (mixinNames.isEmpty()) {
            // purge empty jcr:mixinTypes property
            removeChildProperty(NameConstants.JCR_MIXINTYPES);
            return;
        }

        // call internalSetValue for setting the jcr:mixinTypes property
        // to avoid checking of the 'protected' flag
        InternalValue[] vals = new InternalValue[mixinNames.size()];
        Iterator<Name> iter = mixinNames.iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            vals[cnt++] = InternalValue.create(iter.next());
        }
        prop.internalSetValue(vals, PropertyType.NAME);
    }

    /**
     * Returns the <code>Name</code>s of this node's mixin types.
     *
     * @return a set of the <code>Name</code>s of this node's mixin types.
     */
    public Set<Name> getMixinTypeNames() {
        return data.getNodeState().getMixinTypeNames();
    }

    /**
     * Returns the effective (i.e. merged and resolved) node type representation
     * of this node's primary and mixin node types.
     *
     * @return the effective node type
     * @throws RepositoryException if an error occurs
     */
    public EffectiveNodeType getEffectiveNodeType() throws RepositoryException {
        try {
            NodeTypeRegistry registry =
                session.getNodeTypeManager().getNodeTypeRegistry();
            return registry.getEffectiveNodeType(
                    data.getNodeState().getNodeTypeName(),
                    data.getNodeState().getMixinTypeNames());
        } catch (NodeTypeConflictException ntce) {
            String msg = "Failed to build effective node type for " + this;
            log.debug(msg);
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
     * @throws ConstraintViolationException if no applicable child node definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    protected NodeDefinitionImpl getApplicableChildNodeDefinition(Name nodeName,
                                                                  Name nodeTypeName)
            throws ConstraintViolationException, RepositoryException {
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        QNodeDefinition cnd = getEffectiveNodeType().getApplicableChildNodeDef(
                nodeName, nodeTypeName, ntMgr.getNodeTypeRegistry());
        return ntMgr.getNodeDefinition(cnd);
    }

    /**
     * Returns the applicable property definition for a property with the
     * specified name and type.
     *
     * @param propertyName
     * @param type
     * @param multiValued
     * @param exactTypeMatch
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    protected PropertyDefinitionImpl getApplicablePropertyDefinition(Name propertyName,
                                                                     int type,
                                                                     boolean multiValued,
                                                                     boolean exactTypeMatch)
            throws ConstraintViolationException, RepositoryException {
        QPropertyDefinition pd;
        if (exactTypeMatch || type == PropertyType.UNDEFINED) {
            pd = getEffectiveNodeType().getApplicablePropertyDef(
                    propertyName, type, multiValued);
        } else {
            try {
                // try to find a definition with matching type first
                pd = getEffectiveNodeType().getApplicablePropertyDef(
                        propertyName, type, multiValued);
            } catch (ConstraintViolationException cve) {
                // none found, now try by ignoring the type
                pd = getEffectiveNodeType().getApplicablePropertyDef(
                        propertyName, PropertyType.UNDEFINED, multiValued);
            }
        }
        return session.getNodeTypeManager().getPropertyDefinition(pd);
    }

    protected void makePersistent() throws InvalidItemStateException {
        if (!isTransient()) {
            log.debug(this + " (" + id + "): there's no transient state to persist");
            return;
        }

        NodeState transientState = data.getNodeState();

        NodeState persistentState = (NodeState) transientState.getOverlayedState();
        if (persistentState == null) {
            // this node is 'new'
            persistentState = stateMgr.createNew(transientState);
        }

        synchronized (persistentState) {
            // check staleness of transient state first
            if (transientState.isStale()) {
                String msg =
                    this + ": the node cannot be saved because it has been"
                    + " modified externally.";
                log.debug(msg);
                throw new InvalidItemStateException(msg);
            }
            // copy state from transient state:
            // parent id's
            persistentState.setParentId(transientState.getParentId());
            // primary type
            persistentState.setNodeTypeName(transientState.getNodeTypeName());
            // mixin types
            persistentState.setMixinTypeNames(transientState.getMixinTypeNames());
            // child node entries
            persistentState.setChildNodeEntries(transientState.getChildNodeEntries());
            // property entries
            persistentState.setPropertyNames(transientState.getPropertyNames());
            // shared set
            persistentState.setSharedSet(transientState.getSharedSet());

            // make state persistent
            stateMgr.store(persistentState);
        }

        // tell state manager to disconnect item state
        stateMgr.disconnectTransientItemState(transientState);
        // swap transient state with persistent state
        data.setState(persistentState);
        // reset status
        data.setStatus(STATUS_NORMAL);

        if (isShareable() && data.getPrimaryParentId() == null) {
            data.setPrimaryParentId(persistentState.getParentId());
        }
    }

    protected void restoreTransient(NodeState transientState)
            throws RepositoryException {
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        if (transientState.getStatus() == ItemState.STATUS_NEW
                && thisState.getStatus() != ItemState.STATUS_NEW) {
            thisState.setStatus(ItemState.STATUS_NEW);
            stateMgr.disconnectTransientItemState(thisState);
        }
        // re-apply transient changes
        thisState.setParentId(transientState.getParentId());
        thisState.setNodeTypeName(transientState.getNodeTypeName());
        thisState.setMixinTypeNames(transientState.getMixinTypeNames());
        thisState.setChildNodeEntries(transientState.getChildNodeEntries());
        thisState.setPropertyNames(transientState.getPropertyNames());
        thisState.setSharedSet(transientState.getSharedSet());
    }

    /**
     * Same as {@link Node#addMixin(String)} except that it takes a
     * <code>Name</code> instead of a <code>String</code>.
     *
     * @see Node#addMixin(String)
     */
    public void addMixin(Name mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_CHECKED_OUT
                | ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD;
        int permissions = Permission.NODE_TYPE_MNGMT;
        // special handling of mix:(simple)versionable. since adding the mixin alters
        // the version storage jcr:versionManagement privilege is required
        // in addition.
        if (NameConstants.MIX_VERSIONABLE.equals(mixinName)
                || NameConstants.MIX_SIMPLE_VERSIONABLE.equals(mixinName)) {
            permissions |= Permission.VERSION_MNGMT;
        }
        session.getValidator().checkModify(this, options, permissions);

        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeImpl mixin = ntMgr.getNodeType(mixinName);
        if (!mixin.isMixin()) {
            throw new RepositoryException(mixinName + ": not a mixin node type");
        }

        final Name primaryTypeName = data.getNodeState().getNodeTypeName();
        NodeTypeImpl primaryType = ntMgr.getNodeType(primaryTypeName);
        if (primaryType.isDerivedFrom(mixinName)) {
            // new mixin is already included in primary type
            return;
        }

        // build effective node type of mixin's & primary type in order to detect conflicts
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        EffectiveNodeType entExisting;
        try {
            // existing mixin's
            Set<Name> mixins = new HashSet<Name>(data.getNodeState().getMixinTypeNames());

            // build effective node type representing primary type including existing mixin's
            entExisting = ntReg.getEffectiveNodeType(primaryTypeName, mixins);
            if (entExisting.includesNodeType(mixinName)) {
                // new mixin is already included in existing mixin type(s)
                return;
            }

            // add new mixin
            mixins.add(mixinName);
            // try to build new effective node type (will throw in case of conflicts)
            ntReg.getEffectiveNodeType(primaryTypeName, mixins);
        } catch (NodeTypeConflictException e) {
            throw new ConstraintViolationException(e.getMessage(), e);
        }

        // do the actual modifications implied by the new mixin;
        // try to revert the changes in case an exception occurs
        try {
            // modify the state of this node
            NodeState thisState = (NodeState) getOrCreateTransientItemState();
            // add mixin name
            Set<Name> mixins = new HashSet<Name>(thisState.getMixinTypeNames());
            mixins.add(mixinName);
            thisState.setMixinTypeNames(mixins);

            // set jcr:mixinTypes property
            setMixinTypesProperty(mixins);

            // add 'auto-create' properties defined in mixin type
            for (PropertyDefinition aPda : mixin.getAutoCreatedPropertyDefinitions()) {
                PropertyDefinitionImpl pd = (PropertyDefinitionImpl) aPda;
                // make sure that the property is not already defined by primary type
                // or existing mixin's
                NodeTypeImpl declaringNT = (NodeTypeImpl) pd.getDeclaringNodeType();
                if (!entExisting.includesNodeType(declaringNT.getQName())) {
                    createChildProperty(pd.unwrap().getName(), pd.getRequiredType(), pd);
                }
            }

            // recursively add 'auto-create' child nodes defined in mixin type
            for (NodeDefinition aNda : mixin.getAutoCreatedNodeDefinitions()) {
                NodeDefinitionImpl nd = (NodeDefinitionImpl) aNda;
                // make sure that the child node is not already defined by primary type
                // or existing mixin's
                NodeTypeImpl declaringNT = (NodeTypeImpl) nd.getDeclaringNodeType();
                if (!entExisting.includesNodeType(declaringNT.getQName())) {
                    createChildNode(nd.unwrap().getName(), (NodeTypeImpl) nd.getDefaultPrimaryType(), null);
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
     * Same as {@link Node#removeMixin(String)} except that it takes a
     * <code>Name</code> instead of a <code>String</code>.
     *
     * @see Node#removeMixin(String)
     */
    public void removeMixin(Name mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_CHECKED_OUT
                | ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD;
        int permissions = Permission.NODE_TYPE_MNGMT;
        session.getValidator().checkModify(this, options, permissions);

        // check if mixin is assigned
        final NodeState state = data.getNodeState();
        if (!state.getMixinTypeNames().contains(mixinName)) {
            throw new NoSuchNodeTypeException();
        }

        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();

        // build effective node type of remaining mixin's & primary type
        Set<Name> remainingMixins = new HashSet<Name>(state.getMixinTypeNames());
        // remove name of target mixin
        remainingMixins.remove(mixinName);
        EffectiveNodeType entResulting;
        try {
            // build effective node type representing primary type including remaining mixin's
            entResulting = ntReg.getEffectiveNodeType(
                    state.getNodeTypeName(), remainingMixins);
        } catch (NodeTypeConflictException e) {
            throw new ConstraintViolationException(e.getMessage(), e);
        }

        /**
         * mix:referenceable needs special handling because it has
         * special semantics:
         * it can only be removed if there no more references to this node
         */
        NodeTypeImpl mixin = ntMgr.getNodeType(mixinName);
        if ((NameConstants.MIX_REFERENCEABLE.equals(mixinName)
                || mixin.isDerivedFrom(NameConstants.MIX_REFERENCEABLE))
                && !entResulting.includesNodeType(NameConstants.MIX_REFERENCEABLE)) {
            // removing this mixin would effectively remove mix:referenceable:
            // make sure no references exist
            PropertyIterator iter = getReferences();
            if (iter.hasNext()) {
                throw new ConstraintViolationException(mixinName + " can not be removed: the node is being referenced"
                        + " through at least one property of type REFERENCE");
            }
        }

        /*
         * mix:lockable: the mixin cannot be removed if the node is currently
         * locked even if the editing session is the lock holder.
         */
        if ((NameConstants.MIX_LOCKABLE.equals(mixinName)
                || mixin.isDerivedFrom(NameConstants.MIX_LOCKABLE))
                && !entResulting.includesNodeType(NameConstants.MIX_LOCKABLE)
                && isLocked()) {
            throw new ConstraintViolationException(mixinName + " can not be removed: the node is locked.");
        }

        NodeState thisState = (NodeState) getOrCreateTransientItemState();

        // collect information about properties and nodes which require
        // further action as a result of the mixin removal;
        // we need to do this *before* actually changing the assigned the mixin types,
        // otherwise we wouldn't be able to retrieve the current definition
        // of an item.
        Map<PropertyId, PropertyDefinition> affectedProps = new HashMap<PropertyId, PropertyDefinition>();
        Map<ChildNodeEntry, NodeDefinition> affectedNodes = new HashMap<ChildNodeEntry, NodeDefinition>();
        try {
            Set<Name> names = thisState.getPropertyNames();
            for (Name propName : names) {
                PropertyId propId = new PropertyId(thisState.getNodeId(), propName);
                PropertyState propState = (PropertyState) stateMgr.getItemState(propId);
                PropertyDefinition oldDef = itemMgr.getDefinition(propState);
                // check if property has been defined by mixin type (or one of its supertypes)
                NodeTypeImpl declaringNT = (NodeTypeImpl) oldDef.getDeclaringNodeType();
                if (!entResulting.includesNodeType(declaringNT.getQName())) {
                    // the resulting effective node type doesn't include the
                    // node type that declared this property
                    affectedProps.put(propId, oldDef);
                }
            }

            List<ChildNodeEntry> entries = thisState.getChildNodeEntries();
            for (ChildNodeEntry entry : entries) {
                NodeState nodeState = (NodeState) stateMgr.getItemState(entry.getId());
                NodeDefinition oldDef = itemMgr.getDefinition(nodeState);
                // check if node has been defined by mixin type (or one of its supertypes)
                NodeTypeImpl declaringNT = (NodeTypeImpl) oldDef.getDeclaringNodeType();
                if (!entResulting.includesNodeType(declaringNT.getQName())) {
                    // the resulting effective node type doesn't include the
                    // node type that declared this child node
                    affectedNodes.put(entry, oldDef);
                }
            }
        } catch (ItemStateException e) {
            throw new RepositoryException("Internal Error: Failed to determine effect of removing mixin " + session.getJCRName(mixinName), e);
        }

        // modify the state of this node
        thisState.setMixinTypeNames(remainingMixins);
        // set jcr:mixinTypes property
        setMixinTypesProperty(remainingMixins);

        // process affected nodes & properties:
        // 1. try to redefine item based on the resulting
        //    new effective node type (see JCR-2130)
        // 2. remove item if 1. fails
        boolean success = false;
        try {
            for (PropertyId id : affectedProps.keySet()) {
                PropertyImpl prop = (PropertyImpl) itemMgr.getItem(id);
                PropertyDefinition oldDef = affectedProps.get(id);

                if (oldDef.isProtected()) {
                    // remove 'orphaned' protected properties immediately
                    removeChildProperty(id.getName());
                    continue;
                }
                // try to find new applicable definition first and
                // redefine property if possible (JCR-2130)
                try {
                    PropertyDefinitionImpl newDef = getApplicablePropertyDefinition(
                            id.getName(), prop.getType(),
                            oldDef.isMultiple(), false);
                    if (newDef.getRequiredType() != PropertyType.UNDEFINED
                            && newDef.getRequiredType() != prop.getType()) {
                        // value conversion required
                        if (oldDef.isMultiple()) {
                            // convert value
                            Value[] values =
                                    ValueHelper.convert(
                                            prop.getValues(),
                                            newDef.getRequiredType(),
                                            session.getValueFactory());
                            // redefine property
                            prop.onRedefine(newDef.unwrap());
                            // set converted values
                            prop.setValue(values);
                        } else {
                            // convert value
                            Value value =
                                    ValueHelper.convert(
                                            prop.getValue(),
                                            newDef.getRequiredType(),
                                            session.getValueFactory());
                            // redefine property
                            prop.onRedefine(newDef.unwrap());
                            // set converted values
                            prop.setValue(value);
                        }
                    } else {
                        // redefine property
                        prop.onRedefine(newDef.unwrap());
                    }
                } catch (ValueFormatException vfe) {
                    // value conversion failed, remove it
                    removeChildProperty(id.getName());
                } catch (ConstraintViolationException cve) {
                    // no suitable definition found for this property,
                    // remove it
                    removeChildProperty(id.getName());
                }
            }

            for (ChildNodeEntry entry : affectedNodes.keySet()) {
                NodeState nodeState = (NodeState) stateMgr.getItemState(entry.getId());
                NodeImpl node = (NodeImpl) itemMgr.getItem(entry.getId());
                NodeDefinition oldDef = affectedNodes.get(entry);

                if (oldDef.isProtected()) {
                    // remove 'orphaned' protected child node immediately
                    removeChildNode(entry.getName(), entry.getIndex());
                    continue;
                }

                // try to find new applicable definition first and
                // redefine node if possible (JCR-2130)
                try {
                    NodeDefinitionImpl newDef = getApplicableChildNodeDefinition(
                            entry.getName(),
                            nodeState.getNodeTypeName());
                    // redefine node
                    node.onRedefine(newDef.unwrap());
                } catch (ConstraintViolationException cve) {
                    // no suitable definition found for this child node,
                    // remove it
                    removeChildNode(entry.getName(), entry.getIndex());
                }
            }
            success = true;
        } catch (ItemStateException e) {
            throw new RepositoryException("Failed to clean up child items defined by removed mixin " + session.getJCRName(mixinName), e);
        } finally {
            if (!success) {
                // TODO JCR-1914: revert any changes made so far
            }
        }
    }

    /**
     * Same as {@link Node#isNodeType(String)} except that it takes a
     * <code>Name</code> instead of a <code>String</code>.
     *
     * @param ntName name of node type
     * @return <code>true</code> if this node is of the specified node type;
     *         otherwise <code>false</code>
     */
    public boolean isNodeType(Name ntName) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // first do trivial checks without using type hierarchy
        Name primary = data.getNodeState().getNodeTypeName();
        if (ntName.equals(primary)) {
            return true;
        }
        Set<Name> mixins = data.getNodeState().getMixinTypeNames();
        if (mixins.contains(ntName)) {
            return true;
        }

        // check effective node type
        try {
            NodeTypeRegistry registry =
                session.getNodeTypeManager().getNodeTypeRegistry();
            EffectiveNodeType type =
                registry.getEffectiveNodeType(primary, mixins);
            return type.includesNodeType(ntName);
        } catch (NodeTypeConflictException e) {
            String msg = "Failed to build effective node type for " + this;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Checks various pre-conditions that are common to all
     * <code>setProperty()</code> methods. The checks performed are:
     * <ul>
     * <li>this node must be checked-out</li>
     * <li>this node must not be locked by somebody else</li>
     * </ul>
     * Note that certain checks are performed by the respective
     * <code>Property.setValue()</code> methods.
     *
     * @throws VersionException    if this node is not checked-out
     * @throws LockException       if this node is locked by somebody else
     * @throws RepositoryException if another error occurs
     * @see javax.jcr.Node#setProperty
     */
    protected void checkSetProperty()
            throws VersionException, LockException, RepositoryException {
        // make sure this node is checked-out and is not locked
        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_CHECKED_OUT;
        session.getValidator().checkModify(this, options, Permission.NONE);
    }

    /**
     * Sets the internal value of a property without checking any constraints.
     * <p/>
     * Note that no type conversion is being performed, i.e. it's the caller's
     * responsibility to make sure that the type of the given value is compatible
     * with the specified property's definition.
     * @param name
     * @param value
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    protected Property internalSetProperty(Name name, InternalValue value)
            throws ValueFormatException, RepositoryException {
        int type;
        if (value == null) {
            type = PropertyType.UNDEFINED;
        } else {
            type = value.getType();
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, true, status);
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
    protected Property internalSetProperty(Name name, InternalValue[] values)
            throws ValueFormatException, RepositoryException {
        int type;
        if (values == null || values.length == 0
                || values[0] == null) {
            type = PropertyType.UNDEFINED;
        } else {
            type = values[0].getType();
        }
        return internalSetProperty(name, values, type);
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
     * @param type
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    protected Property internalSetProperty(Name name, InternalValue[] values,
                                           int type)
            throws ValueFormatException, RepositoryException {
        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, true, status);
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
     * @param name The name of the child node to retrieve.
     * @return The child node with the specified <code>name</code>.
     * @throws ItemNotFoundException If no child node exists with the
     *                               specified name.
     * @throws RepositoryException   If another error occurs.
     */
    public NodeImpl getNode(Name name) throws ItemNotFoundException, RepositoryException {
        return getNode(name, 1);
    }

    /**
     * Returns the child node of <code>this</code> node with the specified
     * <code>name</code>.
     *
     * @param name The name of the child node to retrieve.
     * @param index The index of the child node to retrieve (in the case of same-name siblings).
     * @return The child node with the specified <code>name</code>.
     * @throws ItemNotFoundException If no child node exists with the
     *                               specified name.
     * @throws RepositoryException   If another error occurs.
     */
    public NodeImpl getNode(Name name, int index)
            throws ItemNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();

        NodeState thisState = data.getNodeState();
        if (index == 0) {
            index = 1;
        }
        ChildNodeEntry cne = thisState.getChildNodeEntry(name, index);
        if (cne == null) {
            throw new ItemNotFoundException();
        }
        try {
            return itemMgr.getNode(cne.getId(), getNodeId());
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException();
        }
    }

    /**
     * Indicates whether a child node with the specified <code>name</code> exists.
     * Returns <code>true</code> if the child node exists and <code>false</code>
     * otherwise.
     *
     * @param name The name of the child node.
     * @return <code>true</code> if the child node exists; <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public boolean hasNode(Name name) throws RepositoryException {
        return hasNode(name, 1);
    }

    /**
     * Indicates whether a child node with the specified <code>name</code> exists.
     * Returns <code>true</code> if the child node exists and <code>false</code>
     * otherwise.
     *
     * @param name The name of the child node.
     * @param index The index of the child node (in the case of same-name siblings).
     * @return <code>true</code> if the child node exists; <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public boolean hasNode(Name name, int index) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        NodeState thisState = data.getNodeState();
        if (index == 0) {
            index = 1;
        }
        ChildNodeEntry cne = thisState.getChildNodeEntry(name, index);
        if (cne == null) {
            return false;
        }
        return itemMgr.itemExists(cne.getId());
    }

    /**
     * Returns the property of <code>this</code> node with the specified
     * <code>name</code>.
     *
     * @param name The name of the property to retrieve.
     * @return The property with the specified <code>name</code>.
     * @throws ItemNotFoundException If no property exists with the
     *                               specified name.
     * @throws RepositoryException   If another error occurs.
     */
    public PropertyImpl getProperty(Name name)
            throws ItemNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();

        PropertyId propId = new PropertyId(getNodeId(), name);
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
     * @param name The name of the property.
     * @return <code>true</code> if the property exists; <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public boolean hasProperty(Name name) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        NodeState thisState = data.getNodeState();
        if (!thisState.hasPropertyName(name)) {
            return false;
        }
        PropertyId propId = new PropertyId(thisState.getNodeId(), name);

        return itemMgr.itemExists(propId);
    }

    /**
     * Same as <code>{@link Node#addNode(String, String)}</code> except that
     * this method takes <code>Name</code> arguments instead of
     * <code>String</code>s and has an additional <code>uuid</code> argument.
     * <p/>
     * <b>Important Notice:</b> This method is for internal use only! Passing
     * already assigned uuid's might lead to unexpected results and
     * data corruption in the worst case.
     *
     * @param nodeName     name of the new node
     * @param nodeTypeName name of the new node's node type or <code>null</code>
     *                     if it should be determined automatically
     * @param id           id of the new node or <code>null</code> if a new
     *                     id should be assigned
     * @return the newly added node
     * @throws RepositoryException if the node can not added
     */
    public synchronized NodeImpl addNode(
            Name nodeName, Name nodeTypeName, NodeId id)
            throws RepositoryException {
        // check state of this instance
        sanityCheck();

        Path nodePath = PathFactoryImpl.getInstance().create(
                getPrimaryPath(), nodeName, true);

        // Check the explicitly specified node type (if any)
        NodeTypeImpl nt = null;
        if (nodeTypeName != null) {
            nt = session.getNodeTypeManager().getNodeType(nodeTypeName);
            if (nt.isMixin()) {
                throw new ConstraintViolationException(
                        "Unable to add a node with a mixin node type: "
                        + session.getJCRName(nodeTypeName));
            } else if (nt.isAbstract()) {
                throw new ConstraintViolationException(
                        "Unable to add a node with an abstract node type: "
                        + session.getJCRName(nodeTypeName));
            } else {
                // adding a node with explicit specifying the node type name
                // requires the editing session to have nt_management privilege.
                session.getAccessManager().checkPermission(
                        nodePath, Permission.NODE_TYPE_MNGMT);
            }
        }

        // Get the applicable child node definition for this node.
        NodeDefinitionImpl def;
        try {
            def = getApplicableChildNodeDefinition(nodeName, nodeTypeName);
        } catch (RepositoryException e) {
            throw new ConstraintViolationException(
                    "No child node definition for "
                    + session.getJCRName(nodeName) + " found in " + this, e);
        }

        // Use default node type from child node definition if needed
        if (nt == null) {
            nt = (NodeTypeImpl) def.getDefaultPrimaryType();
        }

        // check for name collisions
        NodeState thisState = data.getNodeState();
        ChildNodeEntry cne = thisState.getChildNodeEntry(nodeName, 1);
        if (cne != null) {
            // there's already a child node entry with that name;
            // check same-name sibling setting of new node
            if (!def.allowsSameNameSiblings()) {
                throw new ItemExistsException(
                        "This node already exists: "
                        + itemMgr.safeGetJCRPath(nodePath));
            }
            // check same-name sibling setting of existing node
            NodeImpl existing = itemMgr.getNode(cne.getId(), getNodeId());
            if (!existing.getDefinition().allowsSameNameSiblings()) {
                throw new ItemExistsException(
                        "Same-name siblings not allowed for " + existing);
            }
        }

        // check protected flag of parent (i.e. this) node and retention/hold
        // make sure this node is checked-out and not locked by another session.
        int options =
            ItemValidator.CHECK_LOCK | ItemValidator.CHECK_CHECKED_OUT
            | ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD
            | ItemValidator.CHECK_RETENTION;
        session.getValidator().checkModify(this, options, Permission.NONE);

        // now do create the child node
        return createChildNode(nodeName, nt, id);
    }

    /**
     * Same as <code>{@link Node#setProperty(String, Value[])}</code> except that
     * this method takes a <code>Name</code> name argument instead of a
     * <code>String</code>.
     *
     * @param name
     * @param values
     * @return
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public PropertyImpl setProperty(Name name, Value[] values)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        int type = PropertyType.UNDEFINED;
        if (values != null) {
            for (Value v : values) {
                // use the type of the first value
                if (v != null) {
                    type = v.getType();
                    break;
                }
            }
        }

        return setProperty(name, values, type, false);
    }

    /**
     * Same as <code>{@link Node#setProperty(String, Value[], int)}</code> except
     * that this method takes a <code>Name</code> name argument instead of a
     * <code>String</code>.
     *
     * @param name
     * @param values
     * @param type
     * @return
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public PropertyImpl setProperty(Name name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        return setProperty(name, values, type, true);
    }

    /**
     * Same as <code>{@link Node#setProperty(String, Value)}</code> except that
     * this method takes a <code>Name</code> name argument instead of a
     * <code>String</code>.
     *
     * @param name
     * @param value
     * @return
     * @throws ValueFormatException
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public PropertyImpl setProperty(Name name, Value value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        return setProperty(name, value, false);
    }

    /**
     * @see ItemImpl#getQName()
     */
    public Name getQName() throws RepositoryException {
        HierarchyManager hierMgr = session.getHierarchyManager();
        Name name;

        if (!isShareable()) {
            name = hierMgr.getName(id);
        } else {
            name = hierMgr.getName(getNodeId(), getParentId());
        }
        return name;
    }

    /**
     * Returns the identifier of this <code>Node</code>.
     *
     * @return the id of this <code>Node</code>
     */
    public NodeId getNodeId() {
        return (NodeId) id;
    }

    /**
     * Same as <code>{@link Node#orderBefore(String, String)}</code> except that
     * this method takes a <code>Path.Element</code> arguments instead of
     * <code>String</code>s.
     *
     * @param srcName
     * @param dstName
     * @throws UnsupportedRepositoryOperationException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws ItemNotFoundException
     * @throws LockException
     * @throws RepositoryException
     */
    public synchronized void orderBefore(Path.Element srcName,
                                         Path.Element dstName)
            throws UnsupportedRepositoryOperationException, VersionException,
            ConstraintViolationException, ItemNotFoundException, LockException,
            RepositoryException {

        // check state of this instance
        sanityCheck();

        if (!getPrimaryNodeType().hasOrderableChildNodes()) {
            throw new UnsupportedRepositoryOperationException(
                    "child node ordering not supported on " + this);
        }

        // check arguments
        if (srcName.equals(dstName)) {
            // there's nothing to do
            return;
        }

        // check existence
        if (!hasNode(srcName.getName(), srcName.getIndex())) {
            String name;
            try {
                Path.Element[] path = new Path.Element[] { srcName };
                name = session.getJCRPath(new PathBuilder(path).getPath());
            } catch (NameException e) {
                name = srcName.toString();
            } catch (NamespaceException e) {
                name = srcName.toString();
            }
            throw new ItemNotFoundException(
                    this + " has no child node with name " + name);
        }

        if (dstName != null && !hasNode(dstName.getName(), dstName.getIndex())) {
            String name;
            try {
                Path.Element[] path = new Path.Element[] { dstName };
                name = session.getJCRPath(new PathBuilder(path).getPath());
            } catch (NameException e) {
                name = dstName.toString();
            } catch (NamespaceException e) {
                name = dstName.toString();
            }
            throw new ItemNotFoundException(
                    this + " has no child node with name " + name);
        }

        // make sure this node is checked-out and neither protected nor locked
        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_CHECKED_OUT
                | ItemValidator.CHECK_CONSTRAINTS;
        session.getValidator().checkModify(this, options, Permission.NONE);

        /*
        make sure the session is allowed to reorder child nodes.
        since there is no specific privilege for reordering child nodes,
        test if the the node to be reordered can be removed and added,
        i.e. treating reorder similar to a move.
        TODO: properly deal with sns in which case the index would change upon reorder.
        */
        AccessManager acMgr = session.getAccessManager();
        PathBuilder pb = new PathBuilder(getPrimaryPath());
        pb.addLast(srcName.getName(), srcName.getIndex());
        Path childPath = pb.getPath();
        if (!acMgr.isGranted(childPath, Permission.ADD_NODE | Permission.REMOVE_NODE)) {
            String msg = "Not allowed to reorder child node " + session.getJCRPath(childPath) + ".";
            log.debug(msg);
            throw new AccessDeniedException(msg);
        }
        
        ArrayList<ChildNodeEntry> list = new ArrayList<ChildNodeEntry>(data.getNodeState().getChildNodeEntries());
        int srcInd = -1, destInd = -1;
        for (int i = 0; i < list.size(); i++) {
            ChildNodeEntry entry = list.get(i);
            if (srcInd == -1) {
                if (entry.getName().equals(srcName.getName())
                        && (entry.getIndex() == srcName.getIndex()
                        || srcName.getIndex() == 0 && entry.getIndex() == 1)) {
                    srcInd = i;
                }
            }
            if (destInd == -1 && dstName != null) {
                if (entry.getName().equals(dstName.getName())
                        && (entry.getIndex() == dstName.getIndex()
                        || dstName.getIndex() == 0 && entry.getIndex() == 1)) {
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
     * Replaces the child node with the specified <code>id</code>
     * by a new child node with the same id and specified <code>nodeName</code>,
     * <code>nodeTypeName</code> and <code>mixinNames</code>.
     *
     * @param id           id of the child node to be replaced
     * @param nodeName     name of the new node
     * @param nodeTypeName name of the new node's node type
     * @param mixinNames   name of the new node's mixin types
     *
     * @return the new child node replacing the existing child
     * @throws ItemNotFoundException
     * @throws NoSuchNodeTypeException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws LockException
     * @throws RepositoryException
     */
    public synchronized NodeImpl replaceChildNode(NodeId id, Name nodeName,
                                                  Name nodeTypeName,
                                                  Name[] mixinNames)
            throws ItemNotFoundException, NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        Node existing = (Node) itemMgr.getItem(id);

        // 'replace' is actually a 'remove existing/add new' operation;
        // this unfortunately changes the order of this node's
        // child node entries (JCR-1055);
        // => backup list of child node entries beforehand in order
        // to restore it afterwards
        NodeState state = data.getNodeState();
        ChildNodeEntry cneExisting = state.getChildNodeEntry(id);
        if (cneExisting == null) {
            throw new ItemNotFoundException(
                    this + ": no child node entry with id " + id);
        }
        List<ChildNodeEntry> cneList = new ArrayList<ChildNodeEntry>(state.getChildNodeEntries());

        // remove existing
        existing.remove();

        // create new child node
        NodeImpl node = addNode(nodeName, nodeTypeName, id);
        if (mixinNames != null) {
            for (Name mixinName : mixinNames) {
                node.addMixin(mixinName);
            }
        }

        // fetch <code>state</code> again, as it changed while removing child
        state = data.getNodeState();

        // restore list of child node entries (JCR-1055)
        if (cneExisting.getName().equals(nodeName)) {
            // restore original child node list
            state.setChildNodeEntries(cneList);
        } else {
            // replace child node entry with different name
            // but preserving original position
            state.removeAllChildNodeEntries();
            for (ChildNodeEntry cne : cneList) {
                if (cne.getId().equals(id)) {
                    // replace entry with different name
                    state.addChildNodeEntry(nodeName, id);
                } else {
                    state.addChildNodeEntry(cne.getName(), cne.getId());
                }
            }
        }

        return node;
    }

    /**
     * Create a child node that is a clone of a shareable node.
     *
     * @param src shareable source node
     * @param name name of new node
     * @return child node
     * @throws ItemExistsException if there already is a child node with the
     *             name given and the definition does not allow creating another one
     * @throws VersionException if this node is not checked out
     * @throws ConstraintViolationException if no definition is found in this
     *             node that would allow creating the child node
     * @throws LockException if this node is locked
     * @throws RepositoryException if some other error occurs
     */
    public synchronized NodeImpl clone(NodeImpl src, Name name)
            throws ItemExistsException, VersionException,
                   ConstraintViolationException, LockException,
                   RepositoryException {

        Path nodePath;
        try {
            nodePath = PathFactoryImpl.getInstance().create(getPrimaryPath(), name, true);
        } catch (MalformedPathException e) {
            // should never happen
            String msg = "internal error: invalid path " + this;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        // (1) make sure that parent node is checked-out
        // (2) check lock status
        // (3) check protected flag of parent (i.e. this) node
        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_CHECKED_OUT | ItemValidator.CHECK_CONSTRAINTS;
        session.getValidator().checkModify(this, options, Permission.NONE);

        // (4) check for name collisions
        NodeDefinitionImpl def;
        try {
            def = getApplicableChildNodeDefinition(name, null);
        } catch (RepositoryException re) {
            String msg = "no definition found in parent node's node type for new node";
            log.debug(msg);
            throw new ConstraintViolationException(msg, re);
        }
        NodeState thisState = data.getNodeState();
        ChildNodeEntry cne = thisState.getChildNodeEntry(name, 1);
        if (cne != null) {
            // there's already a child node entry with that name;
            // check same-name sibling setting of new node
            if (!def.allowsSameNameSiblings()) {
                throw new ItemExistsException(itemMgr.safeGetJCRPath(nodePath));
            }
            // check same-name sibling setting of existing node
            NodeId newId = cne.getId();
            if (!((NodeImpl) itemMgr.getItem(newId)).getDefinition().allowsSameNameSiblings()) {
                throw new ItemExistsException(itemMgr.safeGetJCRPath(nodePath));
            }
        }

        // (5) do clone operation
        NodeId parentId = getNodeId();
        src.addShareParent(parentId);

        // (6) modify the state of 'this', i.e. the parent node
        NodeId srcId = src.getNodeId();
        thisState = (NodeState) getOrCreateTransientItemState();
        // add new child node entry
        thisState.addChildNodeEntry(name, srcId);

        return itemMgr.getNode(srcId, parentId);
    }

    // -----------------------------------------------------------------< Item >
    /**
     * {@inheritDoc}
     */
    public boolean isNode() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        final NodeState state = data.getNodeState();
        if (state.getParentId() == null) {
            // this is the root node
            return "";
        }

        HierarchyManager hierMgr = session.getHierarchyManager();
        Name name;

        if (!isShareable()) {
            name = hierMgr.getName(id);
        } else {
            name = hierMgr.getName(getNodeId(), getParentId());
        }
        return session.getJCRName(name);
    }

    /**
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    public Node getParent()
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check if root node
        NodeId parentId = getParentId();
        if (parentId == null) {
            String msg = "root node doesn't have a parent";
            log.debug(msg);
            throw new ItemNotFoundException(msg);
        }
        return (Node) itemMgr.getItem(parentId);
    }

    //----------------------------------------------------------------< Node >

    /**
     * {@inheritDoc}
     */
    public Node addNode(String relPath) throws RepositoryException {
        return addNodeWithUuid(relPath, null, null);
    }

    /**
     * {@inheritDoc}
     */
    public Node addNode(String relPath, String nodeTypeName)
            throws RepositoryException {
        return addNodeWithUuid(relPath, nodeTypeName, null);
    }

    /**
     * Adds a node with the given UUID. You can only add a node with a UUID
     * that is not already assigned to another node in this workspace.
     *
     * @since Apache Jackrabbit 1.6
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1972">JCR-1972</a>
     * @see Node#addNode(String)
     * @param relPath path of the new node
     * @param uuid    UUID of the new node,
     *                or <code>null</code> for a random new UUID
     * @return the newly added node
     * @throws RepositoryException if the node can not be added
     */
    public Node addNodeWithUuid(String relPath, String uuid)
            throws RepositoryException {
        return addNodeWithUuid(relPath, null, uuid);
    }

    /**
     * Adds a node with the given node type and UUID. You can only add a node
     * with a UUID that is not already assigned to another node in this
     * workspace.
     *
     * @since Apache Jackrabbit 1.6
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1972">JCR-1972</a>
     * @see Node#addNode(String, String)
     * @param relPath      path of the new node
     * @param nodeTypeName name of the new node's node type,
     *                     or <code>null</code> for automatic type assignment
     * @param uuid         UUID of the new node,
     *                     or <code>null</code> for a random new UUID
     * @return the newly added node
     * @throws RepositoryException if the node can not be added
     */
    public synchronized Node addNodeWithUuid(
            String relPath, String nodeTypeName, String uuid)
            throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // Get the canonical path of the new node
        Path path;
        try {
            path = PathFactoryImpl.getInstance().create(
                    getPrimaryPath(), session.getQPath(relPath), true);
        } catch (NameException e) {
            throw new RepositoryException(
                    "Failed to resolve path " + relPath
                    + " relative to " + this, e);
        }

        // Get the last path element and check that it's a simple name
        Path.Element last = path.getNameElement();
        if (!last.denotesName() || last.getIndex() != 0) {
            throw new RepositoryException(
                    "Invalid last path element for adding node "
                    + relPath + " relative to " + this);
        }

        // Get the parent node instance
        NodeImpl parentNode;
        Path parentPath = path.getAncestor(1);
        try {
            parentNode = itemMgr.getNode(parentPath);
        } catch (PathNotFoundException e) {
            if (itemMgr.propertyExists(parentPath)) {
                throw new ConstraintViolationException(
                        "Unable to add a child node to property "
                        + session.getJCRPath(parentPath));
            }
            throw e;
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(
                    "Failed to resolve path " + relPath + " relative to " + this);
        }

        // Resolve node type name (if any)
        Name typeName = null;
        if (nodeTypeName != null) {
            typeName = session.getQName(nodeTypeName);
        }

        // Check that the given UUID (if any) does not already exist
        NodeId id = null;
        if (uuid != null) {
            id = new NodeId(uuid);
            if (itemMgr.itemExists(id)) {
                throw new ItemExistsException(
                        "A node with this UUID already exists: " + uuid);
            }
        }

        return parentNode.addNode(last.getName(), typeName, id);
    }

    /**
     * {@inheritDoc}
     */
    public void orderBefore(String srcName, String destName)
            throws UnsupportedRepositoryOperationException, VersionException,
            ConstraintViolationException, ItemNotFoundException, LockException,
            RepositoryException {

        Path.Element insertName;
        try {
            Path p = session.getQPath(srcName);
            // p must be a relative path of length==depth==1 (to eliminate e.g. "..")
            if (p.isAbsolute() || p.getLength() != 1 || p.getDepth() != 1) {
                throw new RepositoryException("invalid name: " + srcName);
            }
            insertName = p.getNameElement();
        } catch (NameException e) {
            String msg = "invalid name: " + srcName;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }

        Path.Element beforeName;
        if (destName != null) {
            try {
                Path p = session.getQPath(destName);
                // p must be a relative path of length==depth==1 (to eliminate e.g. "..")
                if (p.isAbsolute() || p.getLength() != 1 || p.getDepth() != 1) {
                    throw new RepositoryException("invalid name: " + destName);
                }
                beforeName = p.getNameElement();
            } catch (NameException e) {
                String msg = "invalid name: " + destName;
                log.debug(msg);
                throw new RepositoryException(msg, e);
            }
        } else {
            beforeName = null;
        }

        orderBefore(insertName, beforeName);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, Value[] values)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        return setProperty(session.getQName(name), values);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        return setProperty(session.getQName(name), values, type);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, String[] values)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value[] v = null;
        if (values != null) {
            v = ValueHelper.convert(values, PropertyType.STRING, session.getValueFactory());
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, String[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value[] v = null;
        if (values != null) {
            v = ValueHelper.convert(values, type, session.getValueFactory());
        }
        return setProperty(session.getQName(name), v, type, true);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, String value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            v = session.getValueFactory().createValue(value);
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, String value, int type)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            v = session.getValueFactory().createValue(value, type);
        }
        return setProperty(session.getQName(name), v, true);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, Value value, int type)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        if (value != null) {
            value = ValueHelper.convert(value, type, session.getValueFactory());
        }
        return setProperty(session.getQName(name), value, true);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, Value value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        return setProperty(session.getQName(name), value);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, InputStream value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            v = session.getValueFactory().createValue(value);
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, boolean value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = session.getValueFactory().createValue(value);
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, double value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = session.getValueFactory().createValue(value);
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, long value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = session.getValueFactory().createValue(value);
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, Calendar value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            try {
                v = session.getValueFactory().createValue(value);
            } catch (IllegalArgumentException e) {
                // thrown if calendar cannot be formatted as ISO8601
                throw new ValueFormatException(e.getMessage());
            }
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, Node value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            try {
                v = session.getValueFactory().createValue(value);
            } catch (UnsupportedRepositoryOperationException e) {
                // happens when node is not referenceable
                throw new ValueFormatException("node is not of type mix:referenceable");
            }
        }
        return setProperty(name, v);
    }

    /**
     * Implementation for <code>setProperty()</code> using a single {@link
     * Value}. The type of the returned property is enforced based on the
     * <code>enforceType</code> flag. If set to <code>true</code>, the returned
     * property is of the passed type if it didn't exist before. If set to
     * <code>false</code>, then the returned property may be of some other type,
     * but still must be based on an existing property definition for the given
     * name and single-valued flag. The resulting type is taken from that
     * definition and the implementation tries to convert the passed value to
     * that type. If that fails, then a {@link ValueFormatException} is thrown.
     *
     * @param name        the name of the property to set.
     * @param value       the value to set. If <code>null</code> the property is
     *                    removed.
     * @param enforceType if the type of <code>value</code> is enforced.
     * @return the <code>Property</code> object set, or <code>null</code> if
     *         this method was used to remove a property (by setting its value
     *         to <code>null</code>).
     * @throws ValueFormatException         if <code>value</code> cannot be
     *                                      converted to the specified type or
     *                                      if the property already exists and
     *                                      is multi-valued.
     * @throws VersionException             if this node is read-only due to a
     *                                      checked-in node and this implementation
     *                                      performs this validation immediately.
     * @throws LockException                if a lock prevents the setting of
     *                                      the property and this implementation
     *                                      performs this validation immediately.
     * @throws ConstraintViolationException if the change would violate a
     *                                      node-type or other constraint and
     *                                      this implementation performs this
     *                                      validation immediately.
     * @throws RepositoryException          if another error occurs.
     */
    protected PropertyImpl setProperty(Name name,
                                       Value value,
                                       boolean enforceType) throws
            ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        int type = PropertyType.UNDEFINED;
        if (value != null) {
            type = value.getType();
        }

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, false, enforceType, status);
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
     * Implementation for <code>setProperty()</code> using a {@link Value}
     * array. The type of the returned property is enforced based on the
     * <code>enforceType</code> flag. If set to <code>true</code>, the returned
     * property is of the passed type if it didn't exist before. If set to
     * <code>false</code>, then the returned property may be of some other type,
     * but still must be based on an existing property definition for the given
     * name and multi-valued flag. The resulting type is taken from that
     * definition and the implementation tries to convert the passed values to
     * that type. If that fails, then a {@link ValueFormatException} is thrown.
     *
     * @param name        the name of the property to set.
     * @param values      the values to set. If <code>null</code> the property
     *                    is removed.
     * @param type        the target type of the values to set.
     * @param enforceType if the target type is enforced.
     * @return the <code>Property</code> object set, or <code>null</code> if
     *         this method was used to remove a property (by setting its value
     *         to <code>null</code>).
     * @throws ValueFormatException         if a value cannot be converted to
     *                                      the specified type or if the
     *                                      property already exists and is not
     *                                      multi-valued.
     * @throws VersionException             if this node is read-only due to a
     *                                      checked-in node and this implementation
     *                                      performs this validation immediately.
     * @throws LockException                if a lock prevents the setting of
     *                                      the property and this implementation
     *                                      performs this validation immediately.
     * @throws ConstraintViolationException if the change would violate a
     *                                      node-type or other constraint and
     *                                      this implementation performs this
     *                                      validation immediately.
     * @throws RepositoryException          if another error occurs.
     */
    protected PropertyImpl setProperty(Name name,
                                       Value[] values,
                                       int type,
                                       boolean enforceType) throws
            ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // check pre-conditions for setting property
        checkSetProperty();

        BitSet status = new BitSet();
        PropertyImpl prop = getOrCreateProperty(name, type, true, enforceType, status);
        try {
            prop.setValue(values, type);
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
     * {@inheritDoc}
     */
    public Node getNode(String relPath)
            throws PathNotFoundException, RepositoryException {

        // check state of this instance
        sanityCheck();

        Path p = resolveRelativePath(relPath);
        NodeId id = getNodeId(p);
        if (id == null) {
            throw new PathNotFoundException(relPath);
        }

        // determine parent as mandated by path
        NodeId parentId = null;
        if (!p.denotesRoot()) {
            parentId = getNodeId(p.getAncestor(1));
        }
        try {
            if (parentId == null) {
                return (NodeImpl) itemMgr.getItem(id);
            }
            // if the node is shareable, it now returns the node with the right
            // parent
            return itemMgr.getNode(id, parentId);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(relPath);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(relPath);
        }
    }

    /**
     * {@inheritDoc}
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
            String msg = "failed to list the child nodes of " + this;
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        } catch (AccessDeniedException ade) {
            String msg = "failed to list the child nodes of " + this;
            log.debug(msg);
            throw new RepositoryException(msg, ade);
        }
    }

    /**
     * {@inheritDoc}
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
            String msg = "failed to list the child properties of " + this;
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        } catch (AccessDeniedException ade) {
            String msg = "failed to list the child properties of " + this;
            log.debug(msg);
            throw new RepositoryException(msg, ade);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Property getProperty(String relPath)
            throws PathNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();

        PropertyId id = resolveRelativePropertyPath(relPath);
        if (id == null) {
            throw new PathNotFoundException(relPath);
        }
        try {
            return (Property) itemMgr.getItem(id);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(relPath);
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(relPath);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNode(String relPath) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        NodeId id = resolveRelativeNodePath(relPath);
        if (id != null) {
            return itemMgr.itemExists(id);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        try {
            return isNodeType(session.getQName(nodeTypeName));
        } catch (NameException e) {
            throw new RepositoryException(
                    "invalid node type name: " + nodeTypeName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return session.getNodeTypeManager().getNodeType(
                data.getNodeState().getNodeTypeName());
    }

    /**
     * {@inheritDoc}
     */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        Set<Name> mixinNames = data.getNodeState().getMixinTypeNames();
        if (mixinNames.isEmpty()) {
            return new NodeType[0];
        }
        NodeType[] nta = new NodeType[mixinNames.size()];
        Iterator<Name> iter = mixinNames.iterator();
        int i = 0;
        while (iter.hasNext()) {
            nta[i++] = session.getNodeTypeManager().getNodeType(iter.next());
        }
        return nta;
    }

    /**
     * {@inheritDoc}
     */
    public void addMixin(String mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        try {
            addMixin(session.getQName(mixinName));
        } catch (NameException e) {
            throw new RepositoryException(
                    "invalid mixin type name: " + mixinName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeMixin(String mixinName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        try {
            removeMixin(session.getQName(mixinName));
        } catch (NameException e) {
            throw new RepositoryException(
                    "invalid mixin type name: " + mixinName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAddMixin(String mixinName)
            throws NoSuchNodeTypeException, RepositoryException {
        // check state of this instance
        sanityCheck();

        Name ntName = session.getQName(mixinName);
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeTypeImpl mixin = ntMgr.getNodeType(ntName);
        if (!mixin.isMixin()) {
            return false;
        }

        int options = ItemValidator.CHECK_LOCK | ItemValidator.CHECK_CHECKED_OUT
                | ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD;
        int permissions = Permission.NODE_TYPE_MNGMT;
        // special handling of mix:(simple)versionable. since adding the mixin alters
        // the version storage jcr:versionManagement privilege is required
        // in addition.
        if (NameConstants.MIX_VERSIONABLE.equals(ntName)
                || NameConstants.MIX_SIMPLE_VERSIONABLE.equals(ntName)) {
            permissions |= Permission.VERSION_MNGMT;
        }
        if (!session.getValidator().canModify(this, options, permissions)) {
            return false;
        }

        final Name primaryTypeName = data.getNodeState().getNodeTypeName();

        NodeTypeImpl primaryType = ntMgr.getNodeType(primaryTypeName);
        if (primaryType.isDerivedFrom(ntName)) {
            // mixin already inherited -> addMixin is allowed but has no effect.
            return true;
        }

        // build effective node type of mixins & primary type
        // in order to detect conflicts
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        EffectiveNodeType entExisting;
        try {
            // existing mixin's
            Set<Name> mixins = new HashSet<Name>(data.getNodeState().getMixinTypeNames());

            // build effective node type representing primary type including existing mixin's
            entExisting = ntReg.getEffectiveNodeType(primaryTypeName, mixins);
            if (entExisting.includesNodeType(ntName)) {
                // the existing mixins already include the mixin to be added.
                // addMixin would succeed without modifying the node.
                return true;
            }

            // add new mixin
            mixins.add(ntName);
            // try to build new effective node type (will throw in case of conflicts)
            ntReg.getEffectiveNodeType(primaryTypeName, mixins);
        } catch (NodeTypeConflictException ntce) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        PropertyId id = resolveRelativePropertyPath(relPath);
        if (id != null) {
            return itemMgr.itemExists(id);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public PropertyIterator getReferences() throws RepositoryException {
        return getReferences(null);
    }

    /**
     * {@inheritDoc}
     */
    public NodeDefinition getDefinition() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return data.getNodeDefinition();
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return ChildrenCollectorFilter.collectChildNodes(this, namePattern);
    }

    /**
     * {@inheritDoc}
     */
    public PropertyIterator getProperties(String namePattern)
            throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return ChildrenCollectorFilter.collectProperties(this, namePattern);
    }

    /**
     * {@inheritDoc}
     */
    public Item getPrimaryItem()
            throws ItemNotFoundException, RepositoryException {
        // check state of this instance
        sanityCheck();

        String name = getPrimaryNodeType().getPrimaryItemName();
        if (name == null) {
            throw new ItemNotFoundException();
        }
        if (hasProperty(name)) {
            return getProperty(name);
        } else if (hasNode(name)) {
            return getNode(name);
        } else {
            throw new ItemNotFoundException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getUUID()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // check state of this instance
        sanityCheck();

        if (!isNodeType(NameConstants.MIX_REFERENCEABLE)) {
            throw new UnsupportedRepositoryOperationException();
        }

        return getNodeId().toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getCorrespondingNodePath(String workspaceName)
            throws ItemNotFoundException, NoSuchWorkspaceException,
            AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();

        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            srcSession = rep.createSession(session.getSubject(), workspaceName);

            // search nearest ancestor that is referenceable
            NodeImpl m1 = this;
            while (m1.getDepth() != 0 && !m1.isNodeType(NameConstants.MIX_REFERENCEABLE)) {
                m1 = (NodeImpl) m1.getParent();
            }

            // if root is common ancestor, corresponding path is same as ours
            if (m1.getDepth() == 0) {
                // check existence
                if (!srcSession.getItemManager().nodeExists(getPrimaryPath())) {
                    throw new ItemNotFoundException("Node not found: " + this);
                } else {
                    return getPath();
                }
            }

            // get corresponding ancestor
            Node m2 = srcSession.getNodeByUUID(m1.getUUID());

            // return path of m2, if m1 == n1
            if (m1 == this) {
                return m2.getPath();
            }

            String relPath;
            try {
                Path p = m1.getPrimaryPath().computeRelativePath(getPrimaryPath());
                // use prefix mappings of srcSession
                relPath = session.getJCRPath(p);
            } catch (NameException be) {
                // should never get here...
                String msg = "internal error: failed to determine relative path";
                log.error(msg, be);
                throw new RepositoryException(msg, be);
            }

            if (!m2.hasNode(relPath)) {
                throw new ItemNotFoundException();
            } else {
                return m2.getNode(relPath).getPath();
            }
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
    public int getIndex() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        NodeId parentId = getParentId();
        if (parentId == null) {
            // the root node cannot have same-name siblings; always return 1
            return 1;
        }

        try {
            NodeState parent =
                    (NodeState) stateMgr.getItemState(parentId);
            ChildNodeEntry parentEntry =
                    parent.getChildNodeEntry(getNodeId());
            return parentEntry.getIndex();
        } catch (ItemStateException ise) {
            // should never get here...
            String msg = "internal error: failed to determine index";
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }
    }

    //-------------------------------------------------------< shareable nodes >

    /**
     * Returns an iterator over all nodes that are in the shared set of this
     * node. If this node is not shared then the returned iterator contains
     * only this node.
     *
     * @return a <code>NodeIterator</code>
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public NodeIterator getSharedSet() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        ArrayList<NodeImpl> list = new ArrayList<NodeImpl>();

        if (!isShareable()) {
            list.add(this);
        } else {
            NodeState state = data.getNodeState();
            for (NodeId parentId : state.getSharedSet()) {
                list.add(itemMgr.getNode(getNodeId(), parentId));
            }
        }
        return new NodeIteratorAdapter(list);
    }

    /**
     * A special kind of <code>remove()</code> that removes this node and every
     * other node in the shared set of this node.
     * <p/>
     * This removal must be done atomically, i.e., if one of the nodes cannot be
     * removed, the function throws the exception <code>remove()</code> would
     * have thrown in that case, and none of the nodes are removed.
     * <p/>
     * If this node is not shared this method removes only this node.
     *
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @see #removeShare()
     * @see Item#remove()
     * @since JCR 2.0
     */
    public void removeSharedSet() throws VersionException, LockException,
            ConstraintViolationException, RepositoryException {

        // check state of this instance
        sanityCheck();

        NodeIterator iter = getSharedSet();
        while (iter.hasNext()) {
            iter.nextNode().removeShare();
        }
    }

    /**
     * A special kind of <code>remove()</code> that removes this node, but does
     * not remove any other node in the shared set of this node.
     * <p/>
     * All of the exceptions defined for <code>remove()</code> apply to this
     * function. In addition, a <code>RepositoryException</code> is thrown if
     * this node cannot be removed without removing another node in the shared
     * set of this node.
     * <p/>
     * If this node is not shared this method removes only this node.
     *
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     * @see #removeSharedSet()
     * @see Item#remove()
     * @since JCR 2.0
     */
    public void removeShare() throws VersionException, LockException,
            ConstraintViolationException, RepositoryException {

        // check state of this instance
        sanityCheck();

        // Standard remove() will remove just this node
        remove();
    }

    /**
     * Helper method, returning a flag that indicates whether this node is
     * shareable.
     *
     * @return <code>true</code> if this node is shareable;
     *         <code>false</code> otherwise.
     * @see NodeState#isShareable()
     */
    boolean isShareable() {
       return data.getNodeState().isShareable();
    }

    /**
     * Helper method, returning the parent id this node is attached to. If this
     * node is shareable, it returns the primary parent id (which remains
     * fixed since shareable nodes are not moveable). Otherwise returns the
     * underlying state's parent id.
     *
     * @return parent id
     */
    NodeId getParentId() {
        return data.getParentId();
    }

    /**
     * Helper method, returning a flag indicating whether this node has
     * the given share-parent.
     *
     * @param parentId parent id
     * @return <code>true</code> if the node has the given shared parent;
     *         <code>false</code> otherwise.
     */
    boolean hasShareParent(NodeId parentId) {
        return data.getNodeState().containsShare(parentId);
    }

    /**
     * Add a share-parent to this node. This method checks, whether:
     * <ul>
     * <li>this node is shareable</li>
     * <li>adding the given would create a share cycle</li>
     * <li>the given parent is already a share-parent</li>
     * </ul>
     * @param parentId parent to add to the shared set
     * @throws RepositoryException if an error occurs
     */
    void addShareParent(NodeId parentId) throws RepositoryException {
        // verify that we're shareable
        if (!isShareable()) {
            String msg = this + " is not shareable.";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // detect share cycle
        NodeId srcId = getNodeId();
        HierarchyManager hierMgr = session.getHierarchyManager();
        if (parentId.equals(srcId) || hierMgr.isAncestor(srcId, parentId)) {
            String msg = "This would create a share cycle.";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // quickly verify whether the share is already contained before creating
        // a transient state in vain
        NodeState state = data.getNodeState();
        if (!state.containsShare(parentId)) {
            state = (NodeState) getOrCreateTransientItemState();
            if (state.addShare(parentId)) {
                return;
            }
        }
        String msg = "Adding a shareable node twice to the same parent is not supported.";
        log.debug(msg);
        throw new UnsupportedRepositoryOperationException(msg);
    }

    /**
     * {@inheritDoc}
     *
     * Overridden to return a different path for shareable nodes.
     *
     * TODO SN: copies functionality in that is already available in
     *          HierarchyManagerImpl, namely composing a path by
     *          concatenating the parent path + this node's name and index:
     *          rather use hierarchy manager to do this
     */
    public Path getPrimaryPath() throws RepositoryException {
        if (!isShareable()) {
            return super.getPrimaryPath();
        }

        NodeId parentId = getParentId();
        NodeImpl parentNode = (NodeImpl) getParent();
        Path parentPath = parentNode.getPrimaryPath();
        PathBuilder builder = new PathBuilder(parentPath);

        ChildNodeEntry entry =
            parentNode.getNodeState().getChildNodeEntry(getNodeId());
        if (entry == null) {
            String msg = "failed to build path of " + id + ": "
                    + parentId + " has no child entry for "
                    + id;
            log.debug(msg);
            throw new ItemNotFoundException(msg);
        }
        // add to path
        if (entry.getIndex() == 1) {
            builder.addLast(entry.getName());
        } else {
            builder.addLast(entry.getName(), entry.getIndex());
        }
        return builder.getPath();
    }

    //------------------------------< versioning support: public Node methods >

    /**
     * {@inheritDoc}
     */
    public boolean isCheckedOut() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // try shortcut first:
        // if current node is 'new' we can safely consider it checked-out since
        // otherwise it would had been impossible to add it in the first place
        if (isNew()) {
            return true;
        }

        // search nearest ancestor that is versionable
        // FIXME should not only rely on existence of jcr:isCheckedOut property
        // but also verify that node.isNodeType("mix:versionable")==true;
        // this would have a negative impact on performance though...
        try {
            NodeState state = getNodeState();
            while (!state.hasPropertyName(JCR_ISCHECKEDOUT)) {
                ItemId parentId = state.getParentId();
                if (parentId == null) {
                    // root reached or out of hierarchy
                    return true;
                }
                state = (NodeState)
                    session.getItemStateManager().getItemState(parentId);
            }
            PropertyId id = new PropertyId(state.getNodeId(), JCR_ISCHECKEDOUT);
            PropertyState ps =
                (PropertyState) session.getItemStateManager().getItemState(id);
            return ps.getValues()[0].getBoolean();
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Returns the version manager of this workspace.
     */
    private VersionManagerImpl getVersionManagerImpl() {
        return session.getWorkspaceImpl().getVersionManagerImpl();
    }

    /**
     * {@inheritDoc}
     */
    public void update(String srcWorkspaceName) throws RepositoryException {
        getVersionManagerImpl().update(this, srcWorkspaceName);
    }

    /**
     * Use {@link VersionManager#checkin(String)} instead
     */
    @Deprecated
    public Version checkin() throws RepositoryException {
        return getVersionManagerImpl().checkin(getPath());
    }

    /**
     * Use {@link VersionManagerImpl#checkin(String, Calendar)} instead
     *
     * @since Apache Jackrabbit 1.6
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1972">JCR-1972</a>
     */
    @Deprecated
    public Version checkin(Calendar created) throws RepositoryException {
        return getVersionManagerImpl().checkin(getPath(), created);
    }

    /**
     * Use {@link VersionManager#checkout(String)} instead
     */
    @Deprecated
    public void checkout() throws RepositoryException {
        getVersionManagerImpl().checkout(getPath());
    }

    /**
     * Use {@link VersionManager#merge(String, String, boolean)} instead
     */
    @Deprecated
    public NodeIterator merge(String srcWorkspace, boolean bestEffort)
            throws RepositoryException {
        return getVersionManagerImpl().merge(
                getPath(), srcWorkspace, bestEffort);
    }

    /**
     * Use {@link VersionManager#cancelMerge(String, Version)} instead
     */
    @Deprecated
    public void cancelMerge(Version version) throws RepositoryException {
        getVersionManagerImpl().cancelMerge(getPath(), version);
    }

    /**
     * Use {@link VersionManager#doneMerge(String, Version)} instead
     */
    @Deprecated
    public void doneMerge(Version version) throws RepositoryException {
        getVersionManagerImpl().doneMerge(getPath(), version);
    }

    /**
     * Use {@link VersionManager#restore(String, String, boolean)} instead
     */
    @Deprecated
    public void restore(String versionName, boolean removeExisting)
            throws RepositoryException {
        getVersionManagerImpl().restore(getPath(), versionName, removeExisting);
    }

    /**
     * Use {@link VersionManager#restore(String, Version, boolean)} instead
     */
    @Deprecated
    public void restore(Version version, boolean removeExisting)
            throws RepositoryException {
        getVersionManagerImpl().restore(this, version, removeExisting);
    }

    /**
     * Use {@link VersionManager#restore(String, Version, boolean)} instead
     */
    @Deprecated
    public void restore(Version version, String relPath, boolean removeExisting)
            throws RepositoryException {
        if (hasNode(relPath)) {
            getVersionManagerImpl().restore((NodeImpl) getNode(relPath), version, removeExisting);
        } else {
            getVersionManagerImpl().restore(
                getPath() + "/" + relPath, version, removeExisting);
        }
    }

    /**
     * Use {@link VersionManager#restoreByLabel(String, String, boolean)}
     * instead
     */
    @Deprecated
    public void restoreByLabel(String versionLabel, boolean removeExisting)
            throws RepositoryException {
        getVersionManagerImpl().restoreByLabel(
                getPath(), versionLabel, removeExisting);
    }

    /**
     * Use {@link VersionManager#getVersionHistory(String)} instead
     */
    @Deprecated
    public VersionHistory getVersionHistory() throws RepositoryException {
        return getVersionManagerImpl().getVersionHistory(getPath());
    }

    /**
     * Use {@link VersionManager#getBaseVersion(String)} instead
     */
    @Deprecated
    public Version getBaseVersion() throws RepositoryException {
        return getVersionManagerImpl().getBaseVersion(getPath());
    }

    //------------------------------------------------------< locking support >
    /**
     * {@inheritDoc}
     */
    public Lock lock(boolean isDeep, boolean isSessionScoped)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        // check state of this instance
        sanityCheck();
        LockManager lockMgr = session.getWorkspace().getLockManager();
        return lockMgr.lock(getPath(), isDeep, isSessionScoped, Long.MAX_VALUE, null);
    }

    /**
     * {@inheritDoc}
     */
    public Lock getLock()
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();
        LockManager lockMgr = session.getWorkspace().getLockManager();
        return lockMgr.getLock(getPath());
    }

    /**
     * {@inheritDoc}
     */
    public void unlock()
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {
        // check state of this instance
        sanityCheck();
        LockManager lockMgr = session.getWorkspace().getLockManager();
        lockMgr.unlock(getPath());
    }

    /**
     * {@inheritDoc}
     */
    public boolean holdsLock() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        LockManager lockMgr = session.getWorkspace().getLockManager();
        return lockMgr.holdsLock(getPath());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocked() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        LockManager lockMgr = session.getWorkspace().getLockManager();
        return lockMgr.isLocked(getPath());
    }

    /**
     * Check whether this node is locked by somebody else.
     *
     * @throws LockException       if this node is locked by somebody else
     * @throws RepositoryException if some other error occurs
     * @deprecated
     */
    protected void checkLock() throws LockException, RepositoryException {
        if (isNew()) {
            // a new node needs no check
            return;
        }
        session.getLockManager().checkLock(this);
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * {@inheritDoc}
     */
    public String getIdentifier() throws RepositoryException {
        return ((NodeId) id).toString();
    }

    /**
     * {@inheritDoc}
     */
    public PropertyIterator getReferences(String name)
            throws RepositoryException {
        // check state of this instance
        sanityCheck();

        try {
            if (stateMgr.hasNodeReferences(getNodeId())) {
                NodeReferences refs = stateMgr.getNodeReferences(getNodeId());
                // refs.getReferences() returns a list of PropertyId's
                List<PropertyId> idList = refs.getReferences();
                if (name != null) {
                    Name qName;
                    try {
                        qName = session.getQName(name);
                    } catch (NameException e) {
                        throw new RepositoryException("invalid property name: " + name, e);
                    }
                    ArrayList<PropertyId> filteredList = new ArrayList<PropertyId>(idList.size());
                    for (PropertyId propId : idList) {
                        if (propId.getName().equals(qName)) {
                            filteredList.add(propId);
                        }
                    }
                    idList = filteredList;
                }
                return new LazyItemIterator(itemMgr, idList);
            } else {
                // there are no references, return empty iterator
                return PropertyIteratorAdapter.EMPTY;
            }
        } catch (ItemStateException e) {
            String msg = "Unable to retrieve REFERENCE properties that refer to " + id;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public PropertyIterator getWeakReferences() throws RepositoryException {
        // check state of this instance
        sanityCheck();

        // shortcut if node isn't referenceable
        if (!isNodeType(NameConstants.MIX_REFERENCEABLE)) {
            return PropertyIteratorAdapter.EMPTY;
        }

        Value ref = getSession().getValueFactory().createValue(this, true);
        List<Property> props = new ArrayList<Property>();
        QueryManagerImpl qm = (QueryManagerImpl) session.getWorkspace().getQueryManager();
        for (Node n : qm.getWeaklyReferringNodes(this)) {
            for (PropertyIterator it = n.getProperties(); it.hasNext(); ) {
                Property p = it.nextProperty();
                if (p.getType() == PropertyType.WEAKREFERENCE) {
                    Collection<Value> refs;
                    if (p.isMultiple()) {
                        refs = Arrays.asList(p.getValues());
                    } else {
                        refs = Collections.singleton(p.getValue());
                    }
                    if (refs.contains(ref)) {
                        props.add(p);
                    }
                }
            }
        }
        return new PropertyIteratorAdapter(props);
    }

    /**
     * {@inheritDoc}
     */
    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        if (name == null) {
            return getWeakReferences();
        }

        // check state of this instance
        sanityCheck();

        // shortcut if node isn't referenceable
        if (!isNodeType(NameConstants.MIX_REFERENCEABLE)) {
            return PropertyIteratorAdapter.EMPTY;
        }

        try {
            StringBuilder stmt = new StringBuilder();
            stmt.append("//*[@").append(ISO9075.encode(name));
            stmt.append(" = '").append(data.getId()).append("']");
            Query q = session.getWorkspace().getQueryManager().createQuery(
                    stmt.toString(), Query.XPATH);
            QueryResult result = q.execute();
            ArrayList<Property> l = new ArrayList<Property>();
            for (NodeIterator nit = result.getNodes(); nit.hasNext();) {
                Node n = nit.nextNode();
                l.add(n.getProperty(name));
            }
            if (l.isEmpty()) {
                return PropertyIteratorAdapter.EMPTY;
            } else {
                return new PropertyIteratorAdapter(l);
            }
        } catch (RepositoryException e) {
            String msg = "Unable to retrieve WEAKREFERENCE properties that refer to " + id;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator getNodes(String[] nameGlobs)
            throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return ChildrenCollectorFilter.collectChildNodes(this, nameGlobs);
    }

    /**
     * {@inheritDoc}
     */
    public PropertyIterator getProperties(String[] nameGlobs)
            throws RepositoryException {
        // check state of this instance
        sanityCheck();

        return ChildrenCollectorFilter.collectProperties(this, nameGlobs);
    }

    /**
     * {@inheritDoc}
     */
    public void setPrimaryType(String nodeTypeName)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        // check state of this instance
        sanityCheck();

        // make sure this node is checked-out, neither protected nor locked and
        // the editing session has sufficient permission to change the primary type.
        int options = ItemValidator.CHECK_CHECKED_OUT | ItemValidator.CHECK_LOCK
                | ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD;
        session.getValidator().checkModify(this, options, Permission.NODE_TYPE_MNGMT);

        final NodeState state = data.getNodeState();
        if (state.getParentId() == null) {
            String msg = "changing the primary type of the root node is not supported";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        Name ntName = session.getQName(nodeTypeName);
        if (ntName.equals(state.getNodeTypeName())) {
            log.debug("Node already has " + nodeTypeName + " as primary node type.");
            return;
        }

        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeType nt = ntMgr.getNodeType(ntName);
        if (nt.isMixin()) {
            throw new ConstraintViolationException(nodeTypeName + ": not a primary node type.");
        } else if (nt.isAbstract()) {
            throw new ConstraintViolationException(nodeTypeName + ": is an abstract node type.");
        }

        // build effective node type of new primary type & existing mixin's
        // in order to detect conflicts
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        EffectiveNodeType entNew, entOld, entAll;
        try {
            entNew = ntReg.getEffectiveNodeType(ntName);
            entOld = ntReg.getEffectiveNodeType(state.getNodeTypeName());

            // try to build new effective node type (will throw in case of conflicts)
            entAll = ntReg.getEffectiveNodeType(ntName, state.getMixinTypeNames());
        } catch (NodeTypeConflictException ntce) {
            throw new ConstraintViolationException(ntce.getMessage());
        }

        // get applicable definition for this node using new primary type
        QNodeDefinition nodeDef;
        try {
            NodeImpl parent = (NodeImpl) getParent();
            nodeDef = parent.getApplicableChildNodeDefinition(getQName(), ntName).unwrap();
        } catch (RepositoryException re) {
            String msg = this + ": no applicable definition found in parent node's node type";
            log.debug(msg);
            throw new ConstraintViolationException(msg, re);
        }

        if (!nodeDef.equals(itemMgr.getDefinition(state).unwrap())) {
            onRedefine(nodeDef);
        }

        Set<QItemDefinition> oldDefs = new HashSet<QItemDefinition>(Arrays.asList(entOld.getAllItemDefs()));
        Set<QItemDefinition> newDefs = new HashSet<QItemDefinition>(Arrays.asList(entNew.getAllItemDefs()));
        Set<QItemDefinition> allDefs = new HashSet<QItemDefinition>(Arrays.asList(entAll.getAllItemDefs()));

        // added child item definitions
        Set<QItemDefinition> addedDefs = new HashSet<QItemDefinition>(newDefs);
        addedDefs.removeAll(oldDefs);

        // referential integrity check
        boolean referenceableOld = entOld.includesNodeType(NameConstants.MIX_REFERENCEABLE);
        boolean referenceableNew = entNew.includesNodeType(NameConstants.MIX_REFERENCEABLE);
        if (referenceableOld && !referenceableNew) {
            // node would become non-referenceable;
            // make sure no references exist
            PropertyIterator iter = getReferences();
            if (iter.hasNext()) {
                throw new ConstraintViolationException(
                        "the new primary type cannot be set as it would render "
                                + "this node 'non-referenceable' while it is still being "
                                + "referenced through at least one property of type REFERENCE");
            }
        }

        // do the actual modifications in content as mandated by the new primary type

        // modify the state of this node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        thisState.setNodeTypeName(ntName);

        // set jcr:primaryType property
        internalSetProperty(NameConstants.JCR_PRIMARYTYPE, InternalValue.create(ntName));

        // walk through properties and child nodes and change definition as necessary

        // use temp set to avoid ConcurrentModificationException
        HashSet<Name> set = new HashSet<Name>(thisState.getPropertyNames());
        for (Name propName : set) {
            try {
                PropertyState propState =
                        (PropertyState) stateMgr.getItemState(
                                new PropertyId(thisState.getNodeId(), propName));
                if (!allDefs.contains(itemMgr.getDefinition(propState).unwrap())) {
                    // try to find new applicable definition first and
                    // redefine property if possible
                    try {
                        PropertyImpl prop = (PropertyImpl) itemMgr.getItem(propState.getId());
                        if (prop.getDefinition().isProtected()) {
                            // remove 'orphaned' protected properties immediately
                            removeChildProperty(propName);
                            continue;
                        }
                        PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                                propName, propState.getType(),
                                propState.isMultiValued(), false);
                        if (pdi.getRequiredType() != PropertyType.UNDEFINED
                                && pdi.getRequiredType() != propState.getType()) {
                            // value conversion required
                            if (propState.isMultiValued()) {
                                // convert value
                                Value[] values =
                                        ValueHelper.convert(
                                                prop.getValues(),
                                                pdi.getRequiredType(),
                                                session.getValueFactory());
                                // redefine property
                                prop.onRedefine(pdi.unwrap());
                                // set converted values
                                prop.setValue(values);
                            } else {
                                // convert value
                                Value value =
                                        ValueHelper.convert(
                                                prop.getValue(),
                                                pdi.getRequiredType(),
                                                session.getValueFactory());
                                // redefine property
                                prop.onRedefine(pdi.unwrap());
                                // set converted values
                                prop.setValue(value);
                            }
                        } else {
                            // redefine property
                            prop.onRedefine(pdi.unwrap());
                        }
                        // update collection of added definitions
                        addedDefs.remove(pdi.unwrap());
                    } catch (ValueFormatException vfe) {
                        // value conversion failed, remove it
                        removeChildProperty(propName);
                    } catch (ConstraintViolationException cve) {
                        // no suitable definition found for this property,
                        // remove it
                        removeChildProperty(propName);
                    }
                }
            } catch (ItemStateException ise) {
                String msg = propName + ": failed to retrieve property state";
                log.error(msg, ise);
                throw new RepositoryException(msg, ise);
            }
        }

        // use temp array to avoid ConcurrentModificationException
        ArrayList<ChildNodeEntry> list = new ArrayList<ChildNodeEntry>(thisState.getChildNodeEntries());
        // start from tail to avoid problems with same-name siblings
        for (int i = list.size() - 1; i >= 0; i--) {
            ChildNodeEntry entry = list.get(i);
            try {
                NodeState nodeState = (NodeState) stateMgr.getItemState(entry.getId());
                if (!allDefs.contains(itemMgr.getDefinition(nodeState).unwrap())) {
                    // try to find new applicable definition first and
                    // redefine node if possible
                    try {
                        NodeImpl node = (NodeImpl) itemMgr.getItem(nodeState.getId());
                        if (node.getDefinition().isProtected()) {
                            // remove 'orphaned' protected child node immediately
                            removeChildNode(entry.getName(), entry.getIndex());
                            continue;
                        }
                        NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                                entry.getName(),
                                nodeState.getNodeTypeName());
                        // redefine node
                        node.onRedefine(ndi.unwrap());
                        // update collection of added definitions
                        addedDefs.remove(ndi.unwrap());
                    } catch (ConstraintViolationException cve) {
                        // no suitable definition found for this child node,
                        // remove it
                        removeChildNode(entry.getName(), entry.getIndex());
                    }
                }
            } catch (ItemStateException ise) {
                String msg = entry.getName() + ": failed to retrieve node state";
                log.error(msg, ise);
                throw new RepositoryException(msg, ise);
            }
        }

        // create items that are defined as auto-created by the new primary node
        // type and at the same time were not present with the old nt
        for (QItemDefinition def : addedDefs) {
            if (def.isAutoCreated()) {
                if (def.definesNode()) {
                    NodeDefinitionImpl ndi = ntMgr.getNodeDefinition((QNodeDefinition) def);
                    createChildNode(def.getName(), (NodeTypeImpl) ndi.getDefaultPrimaryType(), null);
                } else {
                    PropertyDefinitionImpl pdi = ntMgr.getPropertyDefinition((QPropertyDefinition) def);
                    createChildProperty(pdi.unwrap().getName(), pdi.getRequiredType(), pdi);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, BigDecimal value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            v = session.getValueFactory().createValue(value);
        }
        return setProperty(name, v);
    }

    /**
     * {@inheritDoc}
     */
    public Property setProperty(String name, Binary value)
            throws ValueFormatException, VersionException, LockException,
            ConstraintViolationException, RepositoryException {
        Value v = null;
        if (value != null) {
            v = session.getValueFactory().createValue(value);
        }
        return setProperty(name, v);
    }

    /**
     * Returns all allowed transitions from the current lifecycle state of
     * this node.
     * <p>
     * The lifecycle policy node referenced by the "jcr:lifecyclePolicy"
     * property is expected to contain a "transitions" node with a list of
     * child nodes, one for each transition. These transition nodes must
     * have single-valued string "from" and "to" properties that identify
     * the allowed source and target states of each transition.
     * <p>
     * Note that future versions of Apache Jackrabbit may well use different
     * lifecycle policy implementations.
     *
     * @since Apache Jackrabbit 2.0
     * @return allowed transitions for the current lifecycle state of this node
     * @throws UnsupportedRepositoryOperationException
     *             if this node does not have the mix:lifecycle mixin node type
     * @throws RepositoryException if a repository error occurs
     */
    public String[] getAllowedLifecycleTransistions()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        if (isNodeType(NameConstants.MIX_LIFECYCLE)) {
            Node policy = getProperty(JCR_LIFECYCLE_POLICY).getNode();
            String state = getProperty(JCR_CURRENT_LIFECYCLE_STATE).getString();

            List<String> targetStates = new ArrayList<String>();
            if (policy.hasNode("transitions")) {
                Node transitions = policy.getNode("transitions");
                for (Node transition : JcrUtils.getChildNodes(transitions)) {
                    String from = transition.getProperty("from").getString();
                    if (from.equals(state)) {
                        String to = transition.getProperty("to").getString();
                        targetStates.add(to);
                    }
                }
            }

            return targetStates.toArray(new String[targetStates.size()]);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Only nodes with mixin node type mix:lifecycle"
                    + " may participate in a lifecycle: " + this);
        }
    }

    /**
     * Transitions this node through its lifecycle to the given target state.
     *
     * @since Apache Jackrabbit 2.0
     * @see #getAllowedLifecycleTransistions()
     * @param transition target lifecycle state
     * @throws UnsupportedRepositoryOperationException
     *             if this node does not have the mix:lifecycle mixin node type
     * @throws InvalidLifecycleTransitionException
     *             if the given target state is not among the allowed
     *             transitions from the current lifecycle state of this node
     * @throws RepositoryException if a repository error occurs
     */
    public void followLifecycleTransition(String transition)
            throws UnsupportedRepositoryOperationException,
            InvalidLifecycleTransitionException, RepositoryException {
        // getAllowedLifecycleTransitions checks for the mix:lifecycle mixin
        for (String target : getAllowedLifecycleTransistions()) {
            if (target.equals(transition)) {
                PropertyImpl property = getProperty(JCR_CURRENT_LIFECYCLE_STATE);
                property.internalSetValue(
                        new InternalValue[] { InternalValue.create(target) },
                        PropertyType.STRING);
                property.save();
                return;
            }
        }

        // No valid transition found
        throw new InvalidLifecycleTransitionException(
                "Invalid lifecycle transition \""
                + transition  + "\" for " + this);
    }

    /**
     * Assigns the given lifecycle policy to this node and sets the
     * current state to the one given.
     * <p>
     * Note that currently no special checks are made against the given
     * arguments, and that you will need to explicitly persist these changes
     * by calling save().
     * <p>
     * Note that future versions of Apache Jackrabbit may well use different
     * lifecycle policy implementations.
     *
     * @param policy lifecycle policy node
     * @param state current lifecycle state
     * @throws RepositoryException if a repository error occurs
     */
    public void assignLifecyclePolicy(Node policy, String state)
            throws RepositoryException {
        if (!(policy instanceof NodeImpl)
                || !((NodeImpl) policy).isNodeType(MIX_REFERENCEABLE)) {
            throw new RepositoryException(
                    policy + " is not referenceable, so it can not be"
                    + " used as a lifecycle policy");
        }

        addMixin(MIX_LIFECYCLE);
        internalSetProperty(
                JCR_LIFECYCLE_POLICY,
                InternalValue.create(((NodeImpl) policy).getNodeId()));
        internalSetProperty(
                JCR_CURRENT_LIFECYCLE_STATE,
                InternalValue.create(state));
    }

    //--------------------------------------------------------------< Object >

    /**
     * Return a string representation of this node for diagnostic purposes.
     *
     * @return "node /path/to/item"
     */
    public String toString() {
        return "node " + super.toString();
    }

}
