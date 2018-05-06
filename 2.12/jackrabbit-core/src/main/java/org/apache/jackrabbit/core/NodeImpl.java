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

import static javax.jcr.PropertyType.STRING;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_CURRENT_LIFECYCLE_STATE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_ISCHECKEDOUT;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_LIFECYCLE_POLICY;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_LIFECYCLE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_REFERENCEABLE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_SIMPLE_VERSIONABLE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_VERSIONABLE;

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
import javax.jcr.nodetype.ItemDefinition;
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

import org.apache.jackrabbit.api.JackrabbitNode;
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
import org.apache.jackrabbit.core.session.AddNodeOperation;
import org.apache.jackrabbit.core.session.NodeNameNormalizer;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.apache.jackrabbit.core.session.SessionWriteOperation;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
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
public class NodeImpl extends ItemImpl implements Node, JackrabbitNode {

    private static Logger log = LoggerFactory.getLogger(NodeImpl.class);

    // flag set in status passed to getOrCreateProperty if property was created
    protected static final short CREATED = 0;

    /** node data (avoids casting <code>ItemImpl.data</code>) */
    private final AbstractNodeData data;

    /**
     * Protected constructor.
     *
     * @param itemMgr    the <code>ItemManager</code> that created this <code>Node</code> instance
     * @param sessionContext the component context of the associated session
     * @param data       the node data
     */
    protected NodeImpl(
            ItemManager itemMgr, SessionContext sessionContext,
            AbstractNodeData data) {
        super(itemMgr, sessionContext, data);
        this.data = data;
        // paranoid sanity check
        NodeTypeRegistry ntReg = sessionContext.getNodeTypeRegistry();
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
     * <p>
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
        Path p = resolveRelativePath(relPath);
        return getPropertyId(p);
    }

    /**
     * Returns the id of the node at <code>relPath</code> or <code>null</code>
     * if no node exists at <code>relPath</code>.
     * <p>
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
            return sessionContext.getQPath(relPath);
        } catch (NameException e) {
            throw new RepositoryException(
                    "Failed to resolve path " + relPath
                    + " relative to " + this, e);
        }
    }

    /**
     * Returns the id of the node at <code>p</code> or <code>null</code>
     * if no node exists at <code>p</code>.
     * <p>
     * Note that access rights are not checked.
     *
     * @param p relative path of a (possible) node
     * @return the id of the node at <code>p</code> or
     *         <code>null</code> if no node exists at <code>p</code>
     * @throws RepositoryException if <code>relPath</code> is not a valid
     *                             relative path
     */
    private NodeId getNodeId(Path p) throws RepositoryException {
        if (p.getLength() == 1 && p.denotesName()) {
            // check if node entry exists
            ChildNodeEntry cne = data.getNodeState().getChildNodeEntry(
                    p.getName(), p.getNormalizedIndex());
            if (cne != null) {
                return cne.getId();
            } else {
                return null; // there's no child node with that name
            }
        } else {
            // build and resolve absolute path
            try {
                p = PathFactoryImpl.getInstance().create(
                        getPrimaryPath(), p, true);
            } catch (RepositoryException re) {
                // failed to build canonical path
                return null;
            }
            return sessionContext.getHierarchyManager().resolveNodePath(p);
        }
    }

    /**
     * Returns the id of the property at <code>p</code> or <code>null</code>
     * if no node exists at <code>p</code>.
     * <p>
     * Note that access rights are not checked.
     *
     * @param p relative path of a (possible) node
     * @return the id of the node at <code>p</code> or
     *         <code>null</code> if no node exists at <code>p</code>
     * @throws RepositoryException if <code>relPath</code> is not a valid
     *                             relative path
     */
    private PropertyId getPropertyId(Path p) throws RepositoryException {
        if (p.getLength() == 1 && p.denotesName()) {
            // check if property entry exists
            NodeState thisState = data.getNodeState();
            if (p.getIndex() == Path.INDEX_UNDEFINED
                    && thisState.hasPropertyName(p.getName())) {
                return new PropertyId(thisState.getNodeId(), p.getName());
            } else {
                return null; // there's no property with that name
            }
        } else {
            // build and resolve absolute path
            try {
                p = PathFactoryImpl.getInstance().create(
                        getPrimaryPath(), p, true);
            } catch (RepositoryException re) {
                // failed to build canonical path
                return null;
            }
            return sessionContext.getHierarchyManager().resolvePropertyPath(p);
        }
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
        return !stateMgr.getDescendantTransientItemStates(id).isEmpty();
    }

    @Override
    protected synchronized ItemState getOrCreateTransientItemState()
            throws RepositoryException {

        synchronized (data) {
            if (!isTransient()) {
                try {
                    // make transient (copy-on-write)
                    NodeState transientState =
                            stateMgr.createTransientNodeState(
                                    (NodeState) stateMgr.getItemState(getId()), ItemState.STATUS_EXISTING_MODIFIED);
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
                    sessionContext.getQName(name), type,
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
        PropertyId propId = new PropertyId(getNodeId(), name);
        try {
            return (PropertyImpl) itemMgr.getItem(propId);
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException(name.toString());
        } catch (ItemNotFoundException e) {
            // does not exist yet or has been removed transiently:
            // find definition for the specified property and (re-)create property
            PropertyDefinitionImpl def = getApplicablePropertyDefinition(
                    name, type, multiValued, exactTypeMatch);
            PropertyImpl prop;
            if (stateMgr.hasTransientItemStateInAttic(propId)) {
                // remove from attic
                try {
                    stateMgr.disposeTransientItemStateInAttic(stateMgr.getAttic().getItemState(propId));
                } catch (ItemStateException ise) {
                    // shouldn't happen because we checked if it is in the attic
                    throw new RepositoryException(ise);
                }
                prop = (PropertyImpl) itemMgr.getItem(propId);
                PropertyState state = (PropertyState) prop.getOrCreateTransientItemState();
                state.setMultiValued(multiValued);
                state.setType(type);
                getNodeState().addPropertyName(name);
            } else {
                prop = createChildProperty(name, type, def);
            }
            status.set(CREATED);
            return prop;
        }
    }

    /**
     * Creates a new property with the given name and <code>type</code> hint and
     * property definition. If the given property definition is not of type
     * <code>UNDEFINED</code>, then it takes precedence over the
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
            String userId = sessionContext.getSessionImpl().getUserID();
            new NodeTypeInstanceHandler(userId).setDefaultValues(
                    propState, data.getNodeState(), propDef);
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
        NodeState nodeState = stateMgr.createTransientNodeState(
                id, nodeType.getQName(), getNodeId(), ItemState.STATUS_NEW);

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

    /**
     *
     * @param oldName
     * @param index
     * @param id
     * @param newName
     * @throws RepositoryException
     * @deprecated use #renameChildNode(NodeId, Name, boolean)
     */
    protected void renameChildNode(Name oldName, int index, NodeId id,
                                   Name newName)
            throws RepositoryException {
        renameChildNode(id, newName, false);
    }

    /**
     *
     * @param id
     * @param newName
     * @param replace
     * @throws RepositoryException
     */
    protected void renameChildNode(NodeId id, Name newName, boolean replace)
            throws RepositoryException {
        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        if (replace) {
            // rename the specified child node by replacing the old
            // child node entry with a new one at the same relative position
            thisState.replaceChildNodeEntry(id, newName, id);
        } else {
            // rename the specified child node by removing the old and adding
            // a new child node entry.
            thisState.renameChildNodeEntry(id, newName);
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

    protected void removeChildNode(NodeId childId) throws RepositoryException {
        // modify the state of 'this', i.e. the parent node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        ChildNodeEntry entry = thisState.getChildNodeEntry(childId);
        if (entry == null) {
            String msg = "failed to remove child " + childId + " of " + this;
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // notify target of removal
        try {
            NodeImpl childNode = itemMgr.getNode(childId, getNodeId());
            childNode.onRemove(getNodeId());
        } catch (ItemNotFoundException e) {
            boolean ignoreError = false;
            if (sessionContext.getSessionImpl().autoFixCorruptions()) {
                // it might be an access right problem
                // we need to check if the item doesn't exist in the ism
                ItemStateManager ism = sessionContext.getItemStateManager();
                if (!ism.hasItemState(childId)) {
                    log.warn("Node " + childId + " not found, ignore", e);
                    ignoreError = true;
                }
            }
            if (!ignoreError) {
                throw e;
            }
        }

        // remove the child node entry
        if (!thisState.removeChildNodeEntry(childId)) {
            String msg = "failed to remove child " + childId + " of " + this;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
    }

    protected void onRedefine(QNodeDefinition def) throws RepositoryException {
        NodeDefinitionImpl newDef =
            sessionContext.getNodeTypeManager().getNodeDefinition(def);
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
                try {
                    /* omit the read-permission check upon retrieving the
                       child item as this is an internal call to remove the
                       subtree which may contain (protected) child items which
                       are not visible to the caller of the removal. the actual
                       validation of the remove permission however is only
                       executed during Item.save(). 
                     */
                    NodeImpl childNode = itemMgr.getNode(childId, getNodeId(), false);
                    childNode.onRemove(thisState.getNodeId());
                    // remove the child node entry
                } catch (ItemNotFoundException e) {
                    boolean ignoreError = false;
                    if (parentId != null && sessionContext.getSessionImpl().autoFixCorruptions()) {
                        // it might be an access right problem
                        // we need to check if the item doesn't exist in the ism
                        ItemStateManager ism = sessionContext.getItemStateManager();
                        if (!ism.hasItemState(childId)) {
                            log.warn("Child named " + entry.getName() + " (index " + entry.getIndex() + ", " +
                                    "node id " + childId + ") " +
                                    "not found when trying to remove " + getPath() + " " +
                                    "(node id " + getNodeId() + ") - ignored", e);
                            ignoreError = true;
                        }
                    }
                    if (!ignoreError) {
                        throw e;
                    }
                }
                thisState.removeChildNodeEntry(childId);
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
            /* omit the read-permission check upon retrieving the
               child item as this is an internal call to remove the
               subtree which may contain (protected) child items which
               are not visible to the caller of the removal. the actual
               validation of the remove permission however is only
               executed during Item.save().
             */
            itemMgr.getItem(propId, false).setRemoved();
        }

        // finally remove this node
        thisState.setParentId(null);
        setRemoved();
    }

    void setMixinTypesProperty(Set<Name> mixinNames) throws RepositoryException {
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
            return sessionContext.getNodeTypeRegistry().getEffectiveNodeType(
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
        NodeTypeManagerImpl ntMgr = sessionContext.getNodeTypeManager();
        QNodeDefinition cnd = getEffectiveNodeType().getApplicableChildNodeDef(
                nodeName, nodeTypeName, sessionContext.getNodeTypeRegistry());
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
        return sessionContext.getNodeTypeManager().getPropertyDefinition(pd);
    }

    @Override
    protected void makePersistent() throws RepositoryException {
        if (!isTransient()) {
            log.debug(this + " (" + id + "): there's no transient state to persist");
            return;
        }

        NodeState transientState = data.getNodeState();
        NodeState localState = stateMgr.makePersistent(transientState);

        // swap transient state with local state
        data.setState(localState);
        // reset status
        data.setStatus(STATUS_NORMAL);

        if (isShareable() && data.getPrimaryParentId() == null) {
            data.setPrimaryParentId(localState.getParentId());
        }
    }

    protected void restoreTransient(NodeState transientState)
            throws RepositoryException {
        NodeState thisState = null;

        if (!isTransient()) {
            thisState = (NodeState) getOrCreateTransientItemState();
            if (transientState.getStatus() == ItemState.STATUS_NEW
                    && thisState.getStatus() != ItemState.STATUS_NEW) {
                thisState.setStatus(ItemState.STATUS_NEW);
                stateMgr.disconnectTransientItemState(thisState);
            }
            thisState.setParentId(transientState.getParentId());
            thisState.setNodeTypeName(transientState.getNodeTypeName());
        } else {
            // JCR-2503: Re-create transient state in the state manager,
            // because it was removed
            synchronized (data) {
                thisState = stateMgr.createTransientNodeState(
                        (NodeId) transientState.getId(),
                        transientState.getNodeTypeName(),
                        transientState.getParentId(),
                        NodeState.STATUS_NEW);
                data.setState(thisState);
            }
        }

        // re-apply transient changes
        thisState.setMixinTypeNames(transientState.getMixinTypeNames());
        thisState.setChildNodeEntries(transientState.getChildNodeEntries());
        thisState.setPropertyNames(transientState.getPropertyNames());
        thisState.setSharedSet(transientState.getSharedSet());
        thisState.setModCount(transientState.getModCount());
    }

    /**
     * Same as {@link Node#addMixin(String)} except that it takes a
     * <code>Name</code> instead of a <code>String</code>.
     *
     * @see Node#addMixin(String)
     */
    public void addMixin(Name mixinName) throws RepositoryException {
        perform(new AddMixinOperation(this, mixinName));
    }

    /**
     * Same as {@link Node#removeMixin(String)} except that it takes a
     * <code>Name</code> instead of a <code>String</code>.
     *
     * @see Node#removeMixin(String)
     */
    public void removeMixin(Name mixinName) throws RepositoryException {
        perform(new RemoveMixinOperation(this, mixinName));
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
            NodeTypeRegistry registry = sessionContext.getNodeTypeRegistry();
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
        sessionContext.getItemValidator().checkModify(this, options, Permission.NONE);
    }

    /**
     * Sets the internal value of a property without checking any constraints.
     * <p>
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
     * <p>
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
     * <p>
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
    public NodeImpl getNode(final Name name, final int index)
            throws ItemNotFoundException, RepositoryException {
        return perform(new SessionOperation<NodeImpl>() {
            public NodeImpl perform(SessionContext context)
                    throws RepositoryException {
                ChildNodeEntry cne = data.getNodeState().getChildNodeEntry(
                        name, index != 0 ? index : 1);
                if (cne != null) {
                    try {
                        return context.getItemManager().getNode(
                                cne.getId(), getNodeId());
                    } catch (AccessDeniedException e) {
                        throw new ItemNotFoundException();
                    }
                } else {
                    throw new ItemNotFoundException();
                }
            }
            public String toString() {
                return "node.getNode(" + name + "[" + index + "])";
            }
        });
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
    public boolean hasNode(final Name name, final int index)
            throws RepositoryException {
        return perform(new SessionOperation<Boolean>() {
            public Boolean perform(SessionContext context)
                    throws RepositoryException {
                ChildNodeEntry cne = data.getNodeState().getChildNodeEntry(
                        name, index != 0 ? index : 1);
                return cne != null
                    && context.getItemManager().itemExists(cne.getId());
            }
            public String toString() {
                return "node.hasNode(" + name + "[" + index + "])";
            }
        });
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
    public PropertyImpl getProperty(final Name name)
            throws ItemNotFoundException, RepositoryException {
        return perform(new SessionOperation<PropertyImpl>() {
            public PropertyImpl perform(SessionContext context)
                    throws RepositoryException {
                try {
                    return (PropertyImpl) context.getItemManager().getItem(
                            new PropertyId(getNodeId(), name));
                } catch (AccessDeniedException ade) {
                    String n = context.getJCRName(name);
                    throw new ItemNotFoundException(
                            "Property " + n + " not found");
                }
            }
            public String toString() {
                return "node.getProperty(" + name + ")";
            }
        });
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
    public boolean hasProperty(final Name name) throws RepositoryException {
        return perform(new SessionOperation<Boolean>() {
            public Boolean perform(SessionContext context)
                    throws RepositoryException {
                return data.getNodeState().hasPropertyName(name)
                    && context.getItemManager().itemExists(
                            new PropertyId(getNodeId(), name));
            }
            public String toString() {
                return "node.hasProperty(" + name + ")";
            }
        });
    }

    /**
     * Same as <code>{@link Node#addNode(String, String)}</code> except that
     * this method takes <code>Name</code> arguments instead of
     * <code>String</code>s and has an additional <code>uuid</code> argument.
     * <p>
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
    // FIXME: This method should not be public
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
            nt = sessionContext.getNodeTypeManager().getNodeType(nodeTypeName);
            if (nt.isMixin()) {
                throw new ConstraintViolationException(
                        "Unable to add a node with a mixin node type: "
                        + sessionContext.getJCRName(nodeTypeName));
            } else if (nt.isAbstract()) {
                throw new ConstraintViolationException(
                        "Unable to add a node with an abstract node type: "
                        + sessionContext.getJCRName(nodeTypeName));
            } else {
                // adding a node with explicit specifying the node type name
                // requires the editing session to have nt_management privilege.
                sessionContext.getAccessManager().checkPermission(
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
                    + sessionContext.getJCRName(nodeName) + " found in " + this, e);
        }

        // Use default node type from child node definition if needed
        if (nt == null) {
            nt = (NodeTypeImpl) def.getDefaultPrimaryType();
        }

        // check the new name
        NodeNameNormalizer.check(nodeName);

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
        sessionContext.getItemValidator().checkModify(this, options, Permission.NONE);

        // now do create the child node
        return createChildNode(nodeName, nt, id);
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
     */
    public PropertyImpl setProperty(Name name, Value value)
            throws RepositoryException {
        return sessionContext.getSessionState().perform(
                new SetPropertyOperation(name, value, false));
    }

    /**
     * @see ItemImpl#getQName()
     */
    @Override
    public Name getQName() throws RepositoryException {
        HierarchyManager hierMgr = sessionContext.getHierarchyManager();
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
     * Returns the name of the primary node type as exposed on the node state
     * without retrieving the node type.
     *
     * @return the name of the primary node type.
     */
    public Name getPrimaryNodeTypeName() {
        return data.getNodeState().getNodeTypeName();
    }

    /**
     * Test if this node is access controlled. The node is access controlled if
     * it is of node type
     * {@link org.apache.jackrabbit.core.security.authorization.AccessControlConstants#NT_REP_ACCESS_CONTROLLABLE "rep:AccessControllable"}
     * and if it has a child node named
     * {@link org.apache.jackrabbit.core.security.authorization.AccessControlConstants#N_POLICY}.
     *
     * @return <code>true</code> if this node is access controlled and has a
     * rep:policy child; <code>false</code> otherwise.
     * @throws RepositoryException if an error occurs
     */
    public boolean isAccessControllable() throws RepositoryException {
        return data.getNodeState().hasChildNodeEntry(NameConstants.REP_POLICY, 1)
                    && isNodeType(NameConstants.REP_ACCESS_CONTROLLABLE);
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
                name = sessionContext.getJCRPath(new PathBuilder(path).getPath());
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
                name = sessionContext.getJCRPath(new PathBuilder(path).getPath());
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
        sessionContext.getItemValidator().checkModify(this, options, Permission.NONE);

        /*
        make sure the session is allowed to reorder child nodes.
        since there is no specific privilege for reordering child nodes,
        test if the the node to be reordered can be removed and added,
        i.e. treating reorder similar to a move.
        TODO: properly deal with sns in which case the index would change upon reorder.
        */
        AccessManager acMgr = sessionContext.getAccessManager();
        PathBuilder pb = new PathBuilder(getPrimaryPath());
        pb.addLast(srcName.getName(), srcName.getIndex());
        Path childPath = pb.getPath();
        if (!acMgr.isGranted(childPath, Permission.MODIFY_CHILD_NODE_COLLECTION)) {
            String msg = "Not allowed to reorder child node " + sessionContext.getJCRPath(childPath) + ".";
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
        sessionContext.getItemValidator().checkModify(this, options, Permission.NONE);

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
    @Override
    public boolean isNode() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() throws RepositoryException {
        return perform(new SessionOperation<String>() {
            public String perform(SessionContext context)
                    throws RepositoryException {
                NodeId parentId = data.getNodeState().getParentId();
                if (parentId == null) {
                    return ""; // this is the root node
                }

                Name name;
                if (!isShareable()) {
                    name = context.getHierarchyManager().getName(id);
                } else {
                    name = context.getHierarchyManager().getName(
                            getNodeId(), parentId);
                }
                return context.getJCRName(name);
            }
            public String toString() {
                return "node.getName()";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(ItemVisitor visitor) throws RepositoryException {
        // check state of this instance
        sanityCheck();

        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getParent() throws RepositoryException {
        return perform(new SessionOperation<Node>() {
            public Node perform(SessionContext context)
                    throws RepositoryException {
                NodeId parentId = getParentId();
                if (parentId != null) {
                    return (Node) context.getItemManager().getItem(parentId);
                } else {
                    throw new ItemNotFoundException(
                            "Root node doesn't have a parent");
                }
            }
            public String toString() {
                return "node.getParent()";
            }
        });
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
    public Node addNodeWithUuid(
            String relPath, String nodeTypeName, String uuid)
            throws RepositoryException {
        return perform(new AddNodeOperation(this, relPath, nodeTypeName, uuid));
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
            Path p = sessionContext.getQPath(srcName);
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
                Path p = sessionContext.getQPath(destName);
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

    /** Wrapper around {@link #setProperty(Name, Value[], int, boolean)} */
    public Property setProperty(String name, Value[] values)
            throws RepositoryException {
        return setProperty(getQName(name), values, getType(values), false);
    }

    /** Wrapper around {@link #setProperty(Name, Value[], int, boolean)} */
    public Property setProperty(String name, Value[] values, int type)
            throws RepositoryException {
        return setProperty(getQName(name), values, type, true);
    }

    /** Wrapper around {@link #setProperty(Name, Value[], int, boolean)} */
    public Property setProperty(String name, String[] strings)
            throws RepositoryException {
        Value[] values = getValues(strings, STRING);
        return setProperty(getQName(name), values, STRING, false);
    }

    /** Wrapper around {@link #setProperty(Name, Value[], int, boolean)} */
    public Property setProperty(String name, String[] values, int type)
            throws RepositoryException {
        Value[] converted = getValues(values, type);
        return setProperty(sessionContext.getQName(name), converted, type, true);
    }

    /** Wrapper around {@link #setProperty(String, Value)} */
    public Property setProperty(String name, String value)
            throws RepositoryException {
        if (value != null) {
            return setProperty(name, getValueFactory().createValue(value));
        } else {
            return setProperty(name, (Value) null);
        }
    }

    /** Wrapper around {@link #setProperty(String, Value, int)} */
    public Property setProperty(String name, String value, int type)
            throws RepositoryException {
        if (value != null) {
            return setProperty(
                    name, getValueFactory().createValue(value, type), type);
        } else {
            return setProperty(name, (Value) null, type);
        }
    }

    /** Wrapper around {@link SetPropertyOperation} */
    public Property setProperty(String name, Value value, int type)
            throws RepositoryException {
        if (value != null && value.getType() != type) {
            value = ValueHelper.convert(value, type, getValueFactory());
        }
        return sessionContext.getSessionState().perform(
                new SetPropertyOperation(sessionContext.getQName(name), value, true));
    }

    /** Wrapper around {@link SetPropertyOperation} */
    public Property setProperty(String name, Value value)
            throws RepositoryException {
        return sessionContext.getSessionState().perform(
                new SetPropertyOperation(sessionContext.getQName(name), value, false));
    }

    /** Wrapper around {@link #setProperty(String, Value)} */
    public Property setProperty(String name, InputStream value)
            throws RepositoryException {
        if (value != null) {
            Binary binary = getValueFactory().createBinary(value);
            try {
                return setProperty(name, getValueFactory().createValue(binary));
            } finally {
                binary.dispose();
            }
        } else {
            return setProperty(name, (Value) null);
        }
    }

    /** Wrapper around {@link #setProperty(String, Value)} */
    public Property setProperty(String name, boolean value)
            throws RepositoryException {
        return setProperty(name, getValueFactory().createValue(value));
    }

    /** Wrapper around {@link #setProperty(String, Value)} */
    public Property setProperty(String name, double value)
            throws RepositoryException {
        return setProperty(name, getValueFactory().createValue(value));
    }

    /** Wrapper around {@link #setProperty(String, Value)} */
    public Property setProperty(String name, long value)
            throws RepositoryException {
        return setProperty(name, getValueFactory().createValue(value));
    }

    /** Wrapper around {@link #setProperty(String, Value)} */
    public Property setProperty(String name, Calendar value)
            throws RepositoryException {
        if (value != null) {
            try {
                return setProperty(name, getValueFactory().createValue(value));
            } catch (IllegalArgumentException e) {
                throw new ValueFormatException(
                        "Value is not an ISO8601 date: " + value, e);
            }
        } else {
            return setProperty(name, (Value) null);
        }
    }

    /** Wrapper around {@link #setProperty(String, Value)} */
    public Property setProperty(String name, Node value)
            throws RepositoryException {
        if (value != null) {
            try {
                return setProperty(name, getValueFactory().createValue(value));
            } catch (UnsupportedRepositoryOperationException e) {
                throw new ValueFormatException(
                        "Node is not referenceable: " + value, e);
            }
        } else {
            return setProperty(name, (Value) null);
        }
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
     */
    private class SetPropertyOperation implements SessionWriteOperation<PropertyImpl> {

        private final Name name;

        private final Value value;

        private final boolean enforceType;

        /**
         * @param name  property name
         * @param value new value of the property,
         *              or <code>null</code> to remove the property
         * @param enforceType <code>true</code> to enforce the value type
         */
        public SetPropertyOperation(
                Name name, Value value, boolean enforceType) {
            this.name = name;
            this.value = value;
            this.enforceType = enforceType;
        }

        /**
         * @return the <code>Property</code> object set,
         *         or <code>null</code> if this operation was used to remove
         *         a property (by setting its value to <code>null</code>)
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
        public PropertyImpl perform(SessionContext context)
                throws RepositoryException {
            itemSanityCheck();
            // check pre-conditions for setting property
            checkSetProperty();

            int type = PropertyType.UNDEFINED;
            if (value != null) {
                type = value.getType();
            }

            BitSet status = new BitSet();
            PropertyImpl property =
                getOrCreateProperty(name, type, false, enforceType, status);
            try {
                property.setValue(value);
            } catch (RepositoryException e) {
                if (status.get(CREATED)) {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                }
                throw e; // rethrow
            } catch (RuntimeException e) {
                if (status.get(CREATED)) {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                }
                throw e; // rethrow
            } catch (Error e) {
                if (status.get(CREATED)) {
                    // setting value failed, get rid of newly created property
                    removeChildProperty(name);
                }
                throw e; // rethrow
            }
            return property;
        }

        //--------------------------------------------------------------< Object >

        /**
         * Returns a string representation of this operation.
         */
        public String toString() {
            return "node.setProperty(" + name + ", " + value + ")";
        }

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
    protected PropertyImpl setProperty(
            final Name name, final Value[] values, final int type,
            final boolean enforceType) throws RepositoryException {
        return perform(new SessionOperation<PropertyImpl>() {
            public PropertyImpl perform(SessionContext context)
                    throws RepositoryException {
                // check pre-conditions for setting property
                checkSetProperty();

                BitSet status = new BitSet();
                PropertyImpl prop = getOrCreateProperty(
                        name, type, true, enforceType, status);
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
            public String toString() {
                return "node.setProperty(...)";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public Node getNode(final String relPath) throws RepositoryException {
        return perform(new SessionOperation<Node>() {
            public Node perform(SessionContext context)
                    throws RepositoryException {
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
                    // if the node is shareable, it now returns the node
                    // with the right parent
                    if (parentId != null) {
                        return itemMgr.getNode(id, parentId);
                    } else {
                        return (NodeImpl) itemMgr.getItem(id);
                    }
                } catch (AccessDeniedException e) {
                    throw new PathNotFoundException(relPath);
                } catch (ItemNotFoundException e) {
                    throw new PathNotFoundException(relPath);
                }
            }
            public String toString() {
                return "node.getNode(" + relPath + ")";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator getNodes() throws RepositoryException {
        // IMPORTANT: an implementation of Node.getNodes() must not use
        // a class derived from TraversingElementVisitor to traverse the
        // hierarchy because this would lead to an infinite recursion!
        return perform(new SessionOperation<NodeIterator>() {
            public NodeIterator perform(SessionContext context)
                    throws RepositoryException {
                try {
                    return itemMgr.getChildNodes((NodeId) id);
                } catch (ItemNotFoundException e) {
                    throw new RepositoryException(
                            "Failed to list child nodes of " + NodeImpl.this, e);
                } catch (AccessDeniedException e) {
                    throw new RepositoryException(
                            "Failed to list child nodes of " + NodeImpl.this, e);
                }
            }
            public String toString() {
                return "node.getNodes()";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public PropertyIterator getProperties() throws RepositoryException {
        // IMPORTANT: an implementation of Node.getProperties() must not use
        // a class derived from TraversingElementVisitor to traverse the
        // hierarchy because this would lead to an infinite recursion!
        return perform(new SessionOperation<PropertyIterator>() {
            public PropertyIterator perform(SessionContext context)
                    throws RepositoryException {
                try {
                    return itemMgr.getChildProperties((NodeId) id);
                } catch (ItemNotFoundException e) {
                    throw new RepositoryException(
                            "Failed to list properties of " + NodeImpl.this, e);
                } catch (AccessDeniedException e) {
                    throw new RepositoryException(
                            "Failed to list properties of " + NodeImpl.this, e);
                }
            }
            public String toString() {
                return "node.getProperties()";
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public Property getProperty(final String relPath)
            throws PathNotFoundException, RepositoryException {
        return perform(new SessionOperation<Property>() {
            public Property perform(SessionContext context)
                    throws RepositoryException {
                PropertyId id = resolveRelativePropertyPath(relPath);
                if (id != null) {
                    try {
                        return (Property) itemMgr.getItem(id);
                    } catch (ItemNotFoundException e) {
                        throw new PathNotFoundException(relPath);
                    } catch (AccessDeniedException e) {
                        throw new PathNotFoundException(relPath);
                    }
                } else {
                    throw new PathNotFoundException(relPath);
                }
            }
            public String toString() {
                return "node.getProperty(" + relPath + ")";
            }
        });
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
        // check state of this instance
        sanityCheck();

        try {
            return isNodeType(sessionContext.getQName(nodeTypeName));
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

        return sessionContext.getNodeTypeManager().getNodeType(
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
            nta[i++] = sessionContext.getNodeTypeManager().getNodeType(iter.next());
        }
        return nta;
    }

    /** Wrapper around {@link #addMixin(Name)}. */
    public void addMixin(String mixinName) throws RepositoryException {
        try {
            addMixin(sessionContext.getQName(mixinName));
        } catch (NameException e) {
            throw new RepositoryException(
                    "Invalid mixin type name: " + mixinName, e);
        }
    }

    /** Wrapper around {@link #removeMixin(Name)}. */
    public void removeMixin(String mixinName) throws RepositoryException {
        try {
            removeMixin(sessionContext.getQName(mixinName));
        } catch (NameException e) {
            throw new RepositoryException(
                    "Invalid mixin type name: " + mixinName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAddMixin(String mixinName)
            throws NoSuchNodeTypeException, RepositoryException {
        // check state of this instance
        sanityCheck();

        Name ntName = sessionContext.getQName(mixinName);
        NodeTypeManagerImpl ntMgr = sessionContext.getNodeTypeManager();
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
        if (!sessionContext.getItemValidator().canModify(this, options, permissions)) {
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
            RepositoryImpl rep = (RepositoryImpl) getSession().getRepository();
            srcSession = rep.createSession(
                    sessionContext.getSessionImpl().getSubject(), workspaceName);

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
                relPath = sessionContext.getJCRPath(p);
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
     * <p>
     * This removal must be done atomically, i.e., if one of the nodes cannot be
     * removed, the function throws the exception <code>remove()</code> would
     * have thrown in that case, and none of the nodes are removed.
     * <p>
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
     * <p>
     * All of the exceptions defined for <code>remove()</code> apply to this
     * function. In addition, a <code>RepositoryException</code> is thrown if
     * this node cannot be removed without removing another node in the shared
     * set of this node.
     * <p>
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
    public NodeId getParentId() {
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
        HierarchyManager hierMgr = sessionContext.getHierarchyManager();
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
    @Override
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
                    sessionContext.getItemStateManager().getItemState(parentId);
            }
            PropertyId id = new PropertyId(state.getNodeId(), JCR_ISCHECKEDOUT);
            PropertyState ps =
                (PropertyState) sessionContext.getItemStateManager().getItemState(id);
            InternalValue[] values = ps.getValues();
            if (values == null || values.length != 1) {
                // the property is not fully set, or it is a multi-valued property
                // in which case it's probably not mix:versionable
                return true;
            }
            return values[0].getBoolean();
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Returns the version manager of this workspace.
     */
    private VersionManagerImpl getVersionManagerImpl() {
        return sessionContext.getWorkspace().getVersionManagerImpl();
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
        LockManager lockMgr = getSession().getWorkspace().getLockManager();
        return lockMgr.lock(getPath(), isDeep, isSessionScoped,
                sessionContext.getWorkspace().getConfig().getDefaultLockTimeout(), null);
    }

    /**
     * {@inheritDoc}
     */
    public Lock getLock()
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, RepositoryException {
        // check state of this instance
        sanityCheck();
        LockManager lockMgr = getSession().getWorkspace().getLockManager();
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
        LockManager lockMgr = getSession().getWorkspace().getLockManager();
        lockMgr.unlock(getPath());
    }

    /**
     * {@inheritDoc}
     */
    public boolean holdsLock() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        LockManager lockMgr = getSession().getWorkspace().getLockManager();
        return lockMgr.holdsLock(getPath());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocked() throws RepositoryException {
        // check state of this instance
        sanityCheck();
        LockManager lockMgr = getSession().getWorkspace().getLockManager();
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
        sessionContext.getWorkspace().getInternalLockManager().checkLock(this);
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * {@inheritDoc}
     */
    public String getIdentifier() throws RepositoryException {
        return id.toString();
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
                        qName = sessionContext.getQName(name);
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
                return new LazyItemIterator(sessionContext, idList);
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
        QueryManagerImpl qm = (QueryManagerImpl) getSession().getWorkspace().getQueryManager();
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
            Query q = getSession().getWorkspace().getQueryManager().createQuery(
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
        sessionContext.getItemValidator().checkModify(this, options, Permission.NODE_TYPE_MNGMT);

        final NodeState state = data.getNodeState();
        if (state.getParentId() == null) {
            String msg = "changing the primary type of the root node is not supported";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        Name ntName = sessionContext.getQName(nodeTypeName);
        if (ntName.equals(state.getNodeTypeName())) {
            log.debug("Node already has " + nodeTypeName + " as primary node type.");
            return;
        }

        NodeTypeManagerImpl ntMgr = sessionContext.getNodeTypeManager();
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
                                                getSession().getValueFactory());
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
                                                getSession().getValueFactory());
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
                            removeChildNode(entry.getId());
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
                        removeChildNode(entry.getId());
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
            v = getSession().getValueFactory().createValue(value);
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
            v = getSession().getValueFactory().createValue(value);
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

    //-------------------------------------------------------< JackrabbitNode >

    /**
     * {@inheritDoc}
     */
    public void rename(String newName) throws RepositoryException {
        // check if this is the root node
        if (getDepth() == 0) {
            throw new RepositoryException("Cannot rename the root node");
        }

        Name qName;
        try {
            qName = sessionContext.getQName(newName);
        } catch (NameException e) {
            throw new RepositoryException("invalid node name: " + newName, e);
        }

        NodeImpl parent = (NodeImpl) getParent();

        // check for name collisions
        NodeImpl existing = null;
        try {
            existing = parent.getNode(qName);
            // there's already a node with that name:
            // check same-name sibling setting of existing node
            if (!existing.getDefinition().allowsSameNameSiblings()) {
                throw new ItemExistsException(
                        "Same name siblings are not allowed: " + existing);
            }
        } catch (AccessDeniedException ade) {
            // FIXME by throwing ItemExistsException we're disclosing too much information
            throw new ItemExistsException();
        } catch (ItemNotFoundException infe) {
            // no name collision, fall through
        }

        // verify that parent node
        // - is checked-out
        // - is not protected neither by node type constraints nor by retention/hold
        int options = ItemValidator.CHECK_CHECKED_OUT | ItemValidator.CHECK_LOCK |
        ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD | ItemValidator.CHECK_RETENTION;
        sessionContext.getItemValidator().checkRemove(parent, options, Permission.NONE);
        sessionContext.getItemValidator().checkModify(parent, options, Permission.NONE);

        // check constraints
        // get applicable definition of renamed target node
        NodeTypeImpl nt = (NodeTypeImpl) getPrimaryNodeType();
        org.apache.jackrabbit.spi.commons.nodetype.NodeDefinitionImpl newTargetDef;
        try {
            newTargetDef = parent.getApplicableChildNodeDefinition(qName, nt.getQName());
        } catch (RepositoryException re) {
            String msg = safeGetJCRPath() + ": no definition found in parent node's node type for renamed node";
            log.debug(msg);
            throw new ConstraintViolationException(msg, re);
        }
        // if there's already a node with that name also check same-name sibling
        // setting of new node; just checking same-name sibling setting on
        // existing node is not sufficient since same-name sibling nodes don't
        // necessarily have identical definitions
        if (existing != null && !newTargetDef.allowsSameNameSiblings()) {
            throw new ItemExistsException(
                    "Same name siblings not allowed: " + existing);
        }

        // check permissions:
        // 1. on the parent node the session must have permission to manipulate the child-entries
        AccessManager acMgr = sessionContext.getAccessManager();
        if (!acMgr.isGranted(parent.getPrimaryPath(), qName, Permission.MODIFY_CHILD_NODE_COLLECTION)) {
            String msg = "Not allowed to rename node " + safeGetJCRPath() + " to " + newName;
            log.debug(msg);
            throw new AccessDeniedException(msg);
        }
        // 2. in case of nt-changes the session must have permission to change
        //    the primary node type on this node itself.
        if (!nt.getName().equals(newTargetDef.getName()) && !(acMgr.isGranted(getPrimaryPath(), Permission.NODE_TYPE_MNGMT))) {
            String msg = "Not allowed to rename node " + safeGetJCRPath() + " to " + newName;
            log.debug(msg);
            throw new AccessDeniedException(msg);
        }

        // change definition
        onRedefine(newTargetDef.unwrap());

        // delegate to parent
        parent.renameChildNode(getNodeId(), qName, true);
    }

    /**
     * {@inheritDoc}
     */
    public void setMixins(String[] mixinNames)
            throws NoSuchNodeTypeException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {

        // check state of this instance
        sanityCheck();

        NodeTypeManagerImpl ntMgr = sessionContext.getNodeTypeManager();

        Set<Name> newMixins = new HashSet<Name>();
        for (String name : mixinNames) {
            Name qName = sessionContext.getQName(name);
            if (! ntMgr.getNodeType(qName).isMixin()) {
                throw new RepositoryException(
                        sessionContext.getJCRName(qName) + " is not a mixin node type");
            }
            newMixins.add(qName);
        }

        // make sure this node is checked-out, neither protected nor locked and
        // the editing session has sufficient permission to change the mixin types.

        // special handling of mix:(simple)versionable. since adding the
        // mixin alters the version storage jcr:versionManagement privilege
        // is required in addition.
        int permissions = Permission.NODE_TYPE_MNGMT;
        if (newMixins.contains(MIX_VERSIONABLE)
                || newMixins.contains(MIX_SIMPLE_VERSIONABLE)) {
            permissions |= Permission.VERSION_MNGMT;
        }
        int options = ItemValidator.CHECK_CHECKED_OUT | ItemValidator.CHECK_LOCK
                | ItemValidator.CHECK_CONSTRAINTS | ItemValidator.CHECK_HOLD;
        sessionContext.getItemValidator().checkModify(this, options, permissions);

        final NodeState state = data.getNodeState();

        // build effective node type of primary type & new mixin's
        // in order to detect conflicts
        NodeTypeRegistry ntReg = ntMgr.getNodeTypeRegistry();
        EffectiveNodeType entNew, entOld, entAll;
        try {
            entNew = ntReg.getEffectiveNodeType(newMixins);
            entOld = ntReg.getEffectiveNodeType(state.getMixinTypeNames());

            // try to build new effective node type (will throw in case of conflicts)
            entAll = ntReg.getEffectiveNodeType(state.getNodeTypeName(), newMixins);
        } catch (NodeTypeConflictException ntce) {
            throw new ConstraintViolationException(ntce.getMessage());
        }

        // added child item definitions
        Set<QItemDefinition> addedDefs = new HashSet<QItemDefinition>(Arrays.asList(entNew.getAllItemDefs()));
        addedDefs.removeAll(Arrays.asList(entOld.getAllItemDefs()));

        // referential integrity check
        boolean referenceableOld = getEffectiveNodeType().includesNodeType(NameConstants.MIX_REFERENCEABLE);
        boolean referenceableNew = entAll.includesNodeType(NameConstants.MIX_REFERENCEABLE);
        if (referenceableOld && !referenceableNew) {
            // node would become non-referenceable;
            // make sure no references exist
            PropertyIterator iter = getReferences();
            if (iter.hasNext()) {
                throw new ConstraintViolationException(
                        "the new mixin types cannot be set as it would render "
                                + "this node 'non-referenceable' while it is still being "
                                + "referenced through at least one property of type REFERENCE");
            }
        }

        // gather currently assigned definitions *before* doing actual modifications
        Map<ItemId, ItemDefinition> oldDefs = new HashMap<ItemId, ItemDefinition>();
        for (Name name : getNodeState().getPropertyNames()) {
            PropertyId id = new PropertyId(getNodeId(), name);
            try {
                PropertyState propState = (PropertyState) stateMgr.getItemState(id);
                oldDefs.put(id, itemMgr.getDefinition(propState));
            } catch (ItemStateException ise) {
                String msg = name + ": failed to retrieve property state";
                log.error(msg, ise);
                throw new RepositoryException(msg, ise);
            }
        }
        for (ChildNodeEntry cne : getNodeState().getChildNodeEntries()) {
            try {
                NodeState nodeState = (NodeState) stateMgr.getItemState(cne.getId());
                oldDefs.put(cne.getId(), itemMgr.getDefinition(nodeState));
            } catch (ItemStateException ise) {
                String msg = cne + ": failed to retrieve node state";
                log.error(msg, ise);
                throw new RepositoryException(msg, ise);
            }
        }

        // now do the actual modifications in content as mandated by the new mixins

        // modify the state of this node
        NodeState thisState = (NodeState) getOrCreateTransientItemState();
        thisState.setMixinTypeNames(newMixins);

        // set jcr:mixinTypes property
        setMixinTypesProperty(newMixins);

        // walk through properties and child nodes and change definition as necessary

        // use temp set to avoid ConcurrentModificationException
        HashSet<Name> set = new HashSet<Name>(thisState.getPropertyNames());
        for (Name propName : set) {
            PropertyState propState = null;
            try {
                propState = (PropertyState) stateMgr.getItemState(
                                new PropertyId(thisState.getNodeId(), propName));
                // the following call triggers ConstraintViolationException
                // if there isn't any suitable definition anymore
                itemMgr.getDefinition(propState);
            } catch (ConstraintViolationException cve) {
                // no suitable definition found for this property
                // try to find new applicable definition first and
                // redefine property if possible
                try {
                    if (oldDefs.get(propState.getId()).isProtected()) {
                        // remove 'orphaned' protected properties immediately
                        removeChildProperty(propName);
                        continue;
                    }
                    PropertyDefinitionImpl pdi = getApplicablePropertyDefinition(
                            propName, propState.getType(),
                            propState.isMultiValued(), false);
                    PropertyImpl prop = (PropertyImpl) itemMgr.getItem(propState.getId());
                    if (pdi.getRequiredType() != PropertyType.UNDEFINED
                            && pdi.getRequiredType() != propState.getType()) {
                        // value conversion required
                        if (propState.isMultiValued()) {
                            // convert value
                            Value[] values =
                                    ValueHelper.convert(
                                            prop.getValues(),
                                            pdi.getRequiredType(),
                                            getSession().getValueFactory());
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
                                            getSession().getValueFactory());
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
                } catch (ConstraintViolationException cve1) {
                    // no suitable definition found for this property,
                    // remove it
                    removeChildProperty(propName);
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
            NodeState nodeState = null;
            try {
                nodeState = (NodeState) stateMgr.getItemState(entry.getId());
                // the following call triggers ConstraintViolationException
                // if there isn't any suitable definition anymore
                itemMgr.getDefinition(nodeState);
            } catch (ConstraintViolationException cve) {
                // no suitable definition found for this child node
                // try to find new applicable definition first and
                // redefine node if possible
                try {
                    if (oldDefs.get(nodeState.getId()).isProtected()) {
                        // remove 'orphaned' protected child node immediately
                        removeChildNode(entry.getId());
                        continue;
                    }
                    NodeDefinitionImpl ndi = getApplicableChildNodeDefinition(
                            entry.getName(),
                            nodeState.getNodeTypeName());
                    NodeImpl node = (NodeImpl) itemMgr.getItem(nodeState.getId());
                    // redefine node
                    node.onRedefine(ndi.unwrap());
                    // update collection of added definitions
                    addedDefs.remove(ndi.unwrap());
                } catch (ConstraintViolationException cve1) {
                    // no suitable definition found for this child node,
                    // remove it
                    removeChildNode(entry.getId());
                }
            } catch (ItemStateException ise) {
                String msg = entry + ": failed to retrieve node state";
                log.error(msg, ise);
                throw new RepositoryException(msg, ise);
            }
        }

        // create items that are defined as auto-created by the new mixins
        // and at the same time were not present with the old mixins
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
