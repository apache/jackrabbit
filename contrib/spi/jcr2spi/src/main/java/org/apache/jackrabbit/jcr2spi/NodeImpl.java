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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.util.ChildrenCollectorFilter;
import org.apache.jackrabbit.util.IteratorHelper;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.jackrabbit.value.ValueFormat;
import org.apache.jackrabbit.value.QValue;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NodeReferences;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.jcr2spi.operation.SetMixin;
import org.apache.jackrabbit.jcr2spi.operation.AddProperty;
import org.apache.jackrabbit.jcr2spi.operation.AddNode;
import org.apache.jackrabbit.jcr2spi.operation.ReorderNodes;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.Update;
import org.apache.jackrabbit.jcr2spi.lock.LockManager;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ItemId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemVisitor;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.PropertyType;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.InvalidItemStateException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.lock.LockException;
import javax.jcr.lock.Lock;
import javax.jcr.version.VersionException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import java.io.InputStream;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;

/**
 * <code>NodeImpl</code>...
 */
public class NodeImpl extends ItemImpl implements Node {

    private static Logger log = LoggerFactory.getLogger(NodeImpl.class);

    private QName primaryTypeName;
    private NodeDefinition definition;

    protected NodeImpl(ItemManager itemMgr, SessionImpl session,
                       NodeState state, NodeDefinition definition,
                       ItemLifeCycleListener[] listeners) {
        super(itemMgr, session, state, listeners);
        this.definition = definition;
        QName nodeTypeName = state.getNodeTypeName();
        // paranoid sanity check
        if (session.getNodeTypeManager().hasNodeType(nodeTypeName)) {
            primaryTypeName = nodeTypeName;
        } else {
            /**
             * todo need proper way of handling inconsistent/corrupt node type references
             * e.g. 'flag' nodes that refer to non-registered node types
             */
            log.warn("Fallback to nt:unstructured due to unknown node type '" + nodeTypeName + "' of node " + safeGetJCRPath());
            primaryTypeName = QName.NT_UNSTRUCTURED;
        }
    }

    //-----------------------------------------------------< Item interface >---
    /**
     * @see Item#getName()
     */
    public String getName() throws RepositoryException {
        checkStatus();
        QName name = session.getHierarchyManager().getQName(getId());
        try {
            return session.getNamespaceResolver().getJCRName(name);
        } catch (NoPrefixDeclaredException npde) {
            // should never get here...
            String msg = "internal error: encountered unregistered namespace " + name.getNamespaceURI();
            log.debug(msg);
            throw new RepositoryException(msg, npde);
        }
    }

    /**
     * @see Item#getParent()
     */
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        checkStatus();
        // check if root node
        NodeId parentId = getItemState().getParentId();
        if (parentId == null) {
            String msg = "root node doesn't have a parent";
            log.debug(msg);
            throw new ItemNotFoundException(msg);
        }

        return (Node) itemMgr.getItem(parentId);
    }

    /**
     * Implementation of {@link Item#accept(javax.jcr.ItemVisitor)} for nodes.
     *
     * @param visitor
     * @throws RepositoryException
     * @see Item#accept(javax.jcr.ItemVisitor)
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        checkStatus();
        visitor.visit(this);
    }

    /**
     * Returns true
     *
     * @return true
     * @see Item#isNode()
     */
    public boolean isNode() {
        return true;
    }

    //-----------------------------------------------------< Node interface >---
    /**
     * @see Node#addNode(String)
     */
    public Node addNode(String relPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        // validation performed in subsequent method
        return addNode(relPath, null);
    }

    /**
     * @see Node#addNode(String, String)
     */
    public Node addNode(String relPath, String primaryNodeTypeName) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        checkIsWritable();
        // 1. build qualified path and retrieve parent node
        Path nodePath = getQPath(relPath);
        if (nodePath.getNameElement().getIndex() != Path.INDEX_UNDEFINED) {
            String msg = "Illegal subscript specified: " + relPath;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        Path parentPath = nodePath.getAncestor(1);
        NodeImpl parentNode;
        try {
            Item parent = itemMgr.getItem(parentPath);
            if (!parent.isNode()) {
                String msg = "Cannot add a node to property " + parentPath;
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
            parentNode = (NodeImpl) parent;
        } catch (AccessDeniedException ade) {
            throw new PathNotFoundException(relPath);
        }

        // 2. get qualified names for node and nt
        QName nodeName = nodePath.getNameElement().getName();
        QName ntName = (primaryNodeTypeName == null) ? null : getQName(primaryNodeTypeName);

        // 3. create new node (including validation checks)
        return parentNode.createNode(nodeName, ntName);
    }

    /**
     * @see Node#orderBefore(String, String)
     */
    public void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
        checkIsWritable();

        if (!getPrimaryNodeType().hasOrderableChildNodes()) {
            throw new UnsupportedRepositoryOperationException("Child node ordering not supported on node " + safeGetJCRPath());
        }
        // check arguments
        if (srcChildRelPath.equals(destChildRelPath)) {
            // there's nothing to do
            return;
        }
        // check existence
        if (!hasNode(srcChildRelPath)) {
            throw new ItemNotFoundException(safeGetJCRPath() + " has no child node with name " + srcChildRelPath);
        }
        if (destChildRelPath != null && !hasNode(destChildRelPath)) {
            throw new ItemNotFoundException(safeGetJCRPath() + " has no child node with name " + destChildRelPath);
        }

        Path.PathElement srcName = getReorderPath(srcChildRelPath).getNameElement();
        Path.PathElement beforeName = (destChildRelPath == null) ? null : getReorderPath(destChildRelPath).getNameElement();

        NodeState nState = getNodeState();
        Operation op = ReorderNodes.create(nState, srcName, beforeName);
        itemStateMgr.execute(op);
    }

    /**
     * @see Node#setProperty(String, Value)
     */
    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        int type = PropertyType.UNDEFINED;
        if (value != null) {
            type = value.getType();
        }
        return setProperty(name, value, type);
    }

    /**
     * @see Node#setProperty(String, javax.jcr.Value, int)
     */
    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkIsWritable();
        Property prop;
        if (hasProperty(name)) {
            // property already exists: pass call to property
            prop = getProperty(name);
            Value v = (type == PropertyType.UNDEFINED) ? value : ValueHelper.convert(value, type, session.getValueFactory());
            prop.setValue(v);
        } else {
            if (value == null) {
                // create and remove property is a nop.
                // TODO: check if is correct to avoid any validation exception that way
                prop = null;
            } else {
                // new property to be added
                prop = createProperty(getQName(name), value, type);
            }
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, Value[])
     */
    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        int type;
        if (values == null || values.length == 0 || values[0] == null) {
            type = PropertyType.UNDEFINED;
        } else {
            type = values[0].getType();
        }
        return setProperty(name, values, type);
    }

    /**
     * @see Node#setProperty(String, Value[], int)
     */
    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkIsWritable();
        Property prop;
        if (hasProperty(name)) {
            // property already exists: pass call to property
            prop = getProperty(name);
            Value[] vs = (type == PropertyType.UNDEFINED) ? values : ValueHelper.convert(values, type, session.getValueFactory());
            prop.setValue(vs);
        } else {
            if (values == null) {
                // create and remove property is a nop.
                // TODO: check if is correct to avoid any validation exception that way
                prop = null;
            } else {
                // new property to be added
                prop = createProperty(getQName(name), values, type);
            }
        }
        return prop;
    }

    /**
     * @see Node#setProperty(String, String[])
     */
    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        return setProperty(name, values, PropertyType.UNDEFINED);
    }

    /**
     * @see Node#setProperty(String, String[], int)
     */
    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        Value[] vs;
        if (type == PropertyType.UNDEFINED) {
            vs = ValueHelper.convert(values, PropertyType.STRING, session.getValueFactory());
        } else {
            vs = ValueHelper.convert(values, type, session.getValueFactory());
        }
        return setProperty(name, vs, type);
    }

    /**
     * @see Node#setProperty(String, String)
     */
    public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        // best-effort conversion if the target property is not of type STRING
        return setProperty(name, value, PropertyType.STRING);
    }

    /**
     * @see Node#setProperty(String, String, int)
     */
    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        Value v = (value == null) ? null : session.getValueFactory().createValue(value, type);
        return setProperty(name, v);
    }

    /**
     * @see Node#setProperty(String, InputStream)
     */
    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        Value v = (value == null ? null : session.getValueFactory().createValue(value));
        return setProperty(name, v);
    }

    /**
     * @see Node#setProperty(String, boolean)
     */
    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        return setProperty(name, session.getValueFactory().createValue(value));
    }

    /**
     * @see Node#setProperty(String, double)
     */
    public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        return setProperty(name, session.getValueFactory().createValue(value));
    }

    /**
     * @see Node#setProperty(String, long)
     */
    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        return setProperty(name, session.getValueFactory().createValue(value));
    }

    /**
     * @see Node#setProperty(String, Calendar)
     */
    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        Value v = (value == null ? null : session.getValueFactory().createValue(value));
        return setProperty(name, v);
    }

    /**
     * @see Node#setProperty(String, Node)
     */
    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        Value v = (value == null ? null : session.getValueFactory().createValue(value));
        return setProperty(name, v);
    }

    /**
     * @see Node#getNode(String)
     */
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        checkStatus();
        NodeId id = resolveRelativeNodePath(relPath);
        if (id == null) {
            throw new PathNotFoundException(relPath);
        }
        return (Node) itemMgr.getItem(id);
    }

    /**
     * @see Node#getNodes()
     */
    public NodeIterator getNodes() throws RepositoryException {
        checkStatus();
        /**
         * IMPORTANT:
         * an implementation of Node.getNodes()
         * must not use a class derived from TraversingElementVisitor
         * to traverse the hierarchy because this would lead to an infinite
         * recursion!
         */
        try {
            return itemMgr.getChildNodes(getNodeId());
        } catch (ItemNotFoundException infe) {
            String msg = "failed to list the child nodes of " + safeGetJCRPath();
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        } catch (AccessDeniedException ade) {
            String msg = "failed to list the child nodes of " + safeGetJCRPath();
            log.debug(msg);
            throw new RepositoryException(msg, ade);
        }
    }

    /**
     * @see Node#getNodes(String)
     */
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        checkStatus();
        ArrayList nodes = new ArrayList();
        // traverse children using a special filtering 'collector'
        accept(new ChildrenCollectorFilter(namePattern, nodes, true, false, 1));
        return new IteratorHelper(Collections.unmodifiableList(nodes));
    }

    /**
     * @see Node#getProperty(String)
     */
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        checkStatus();
        PropertyId id = resolveRelativePropertyPath(relPath);
        if (id == null) {
            throw new PathNotFoundException(relPath);
        }
        return (Property) itemMgr.getItem(id);
    }

    /**
     * @see Node#getProperties()
     */
    public PropertyIterator getProperties() throws RepositoryException {
        checkStatus();
        try {
            return itemMgr.getChildProperties(getNodeId());
        } catch (ItemNotFoundException infe) {
            String msg = "Failed to list the child properties of " + getPath();
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        } catch (AccessDeniedException ade) {
            String msg = "Failed to list the child properties of " + getPath();
            log.debug(msg);
            throw new RepositoryException(msg, ade);
        }
    }

    /**
     * @see Node#getProperties(String)
     */
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        checkStatus();
        ArrayList properties = new ArrayList();
        // traverse children using a special filtering 'collector'
        accept(new ChildrenCollectorFilter(namePattern, properties, false, true, 1));
        return new IteratorHelper(Collections.unmodifiableList(properties));
    }

    /**
     * @see Node#getPrimaryItem()
     */
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        checkStatus();
        String name = getPrimaryNodeType().getPrimaryItemName();
        if (name == null) {
            throw new ItemNotFoundException("No primary item present on Node " + getPath());
        }
        if (hasProperty(name)) {
            return getProperty(name);
        } else if (hasNode(name)) {
            return getNode(name);
        } else {
            throw new ItemNotFoundException("Primary item " + name + " does not exist on Node " + getPath());
        }
    }

    /**
     * @see Node#getUUID()
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkStatus();
        if (!isNodeType(QName.MIX_REFERENCEABLE)) {
            throw new UnsupportedRepositoryOperationException();
        }
        return getNodeState().getNodeId().getUUID();
    }

    /**
     * @see Node#getIndex()
     */
    public int getIndex() throws RepositoryException {
        checkStatus();
        NodeId parentId = getItemState().getParentId();
        if (parentId == null) {
            // the root node cannot have same-name siblings; always return the
            // default index
            return Path.INDEX_DEFAULT;
        }
        try {
            NodeState parent = (NodeState) itemStateMgr.getItemState(parentId);
            NodeState.ChildNodeEntry parentEntry = parent.getChildNodeEntry(getNodeId());
            return parentEntry.getIndex();
        } catch (ItemStateException ise) {
            // should never get here...
            String msg = "internal error: failed to determine index";
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * @see Node#getReferences()
     */
    public PropertyIterator getReferences() throws RepositoryException {
        checkStatus();
        try {
            if (itemStateMgr.hasNodeReferences(getNodeId())) {
                NodeReferences refs = itemStateMgr.getNodeReferences(getNodeId());
                // refs.getReferences() returns a list of PropertyId's
                List idList = refs.getReferences();
                return new LazyItemIterator(itemMgr, idList);
            } else {
                // there are no references, return empty iterator
                return IteratorHelper.EMPTY;
            }
        } catch (ItemStateException e) {
            String msg = "Unable to retrieve REFERENCE properties that refer to " + getId();
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see Node#hasNode(String)
     */
    public boolean hasNode(String relPath) throws RepositoryException {
        checkStatus();
        NodeId id = resolveRelativeNodePath(relPath);
        return (id != null) ? itemMgr.itemExists(id) : false;
    }

    /**
     * @see Node#hasProperty(String)
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        checkStatus();
        PropertyId pId = resolveRelativePropertyPath(relPath);
        return (pId != null) ? itemMgr.itemExists(pId) : false;
    }

    /**
     * Returns true, if this <code>Node</code> has a property with the given
     * qualified name.
     *
     * @param propertyName
     * @return
     */
    private boolean hasProperty(QName propertyName) {
        return getNodeState().hasPropertyName(propertyName);
    }

    /**
     * @see Node#hasNodes()
     */
    public boolean hasNodes() throws RepositoryException {
        checkStatus();
        return itemMgr.hasChildNodes(getNodeId());
    }

    /**
     * @see Node#hasProperties()
     */
    public boolean hasProperties() throws RepositoryException {
        checkStatus();
        return itemMgr.hasChildProperties(getNodeId());
    }

    /**
     * @see Node#getPrimaryNodeType()
     */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        checkStatus();
        return session.getNodeTypeManager().getNodeType(primaryTypeName);
    }

    /**
     * @see Node#getMixinNodeTypes()
     */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        checkStatus();
        QName[] mixinNames = getNodeState().getMixinTypeNames();
        NodeType[] nta = new NodeType[mixinNames.length];
        for (int i = 0; i < mixinNames.length; i++) {
            nta[i++] = session.getNodeTypeManager().getNodeType(mixinNames[i]);
        }
        return nta;
    }

    /**
     * @see Node#isNodeType(String)
     */
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        // check is performed by isNodeType(QName)
        return isNodeType(getQName(nodeTypeName));
    }

    /**
     * @see Node#addMixin(String)
     */
    public void addMixin(String mixinName) throws NoSuchNodeTypeException,
        VersionException, ConstraintViolationException, LockException, RepositoryException {
        checkIsWritable();
        QName[] mixinQNames = new QName[] {getQName(mixinName)};
        try {
            isValidMixin(mixinQNames);
        } catch (NodeTypeConflictException e) {
            throw new ConstraintViolationException(e.getMessage());
        }

        // merge existing mixins and new mixins to one Array without modifying
        // the node state.
        QName[] currentMixins = getNodeState().getMixinTypeNames();
        QName[] allMixins = new QName[currentMixins.length + mixinQNames.length];
        System.arraycopy(currentMixins, 0, allMixins, 0, currentMixins.length);
        for (int i = 0; i < mixinQNames.length; i++) {
            allMixins[currentMixins.length + i] = mixinQNames[i];
        }
        // perform the operation
        PropertyId mixinPId = session.getIdFactory().createPropertyId(getNodeId(), QName.JCR_MIXINTYPES);
        Operation op = SetMixin.create(mixinPId, allMixins);
        itemStateMgr.execute(op);
    }

    /**
     * @see Node#removeMixin(String)
     */
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException,
        VersionException, ConstraintViolationException, LockException, RepositoryException {
        checkIsWritable();
        QName ntName = getQName(mixinName);
        List remainingMixins = new ArrayList(Arrays.asList(getNodeState().getMixinTypeNames()));
        // remove name of target mixin
        if (!remainingMixins.remove(ntName)) {
            throw new NoSuchNodeTypeException("Cannot remove mixin '" + mixinName + "': Nodetype is not present on this node.");
        }

        // build effective node type of remaining mixin's & primary type
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        EffectiveNodeType entRemaining;

        // build effective node type representing primary type including remaining mixin's
        QName[] allRemaining = (QName[]) remainingMixins.toArray(new QName[remainingMixins.size() + 1]);
        allRemaining[remainingMixins.size()] = primaryTypeName;
        try {
            entRemaining = session.getValidator().getEffectiveNodeType(allRemaining);
        } catch (NodeTypeConflictException e) {
            throw new ConstraintViolationException(e);
        }

        // mix:referenceable needs additional assertion: the mixin cannot be
        // removed, if any references are left to this node.
        NodeTypeImpl mixin = ntMgr.getNodeType(ntName);
        if (mixin.isNodeType(QName.MIX_REFERENCEABLE) && !entRemaining.includesNodeType(QName.MIX_REFERENCEABLE)) {
            PropertyIterator iter = getReferences();
            if (iter.hasNext()) {
                throw new ConstraintViolationException(mixinName + " can not be removed: the node is being referenced through at least one property of type REFERENCE");
            }
        }

        // delegate to operation
        QName[] mixins = (QName[]) remainingMixins.toArray(new QName[remainingMixins.size()]);
        PropertyId mixinPId = session.getIdFactory().createPropertyId(getNodeId(), QName.JCR_MIXINTYPES);
        Operation op = SetMixin.create(mixinPId, mixins);
        itemStateMgr.execute(op);
    }

    /**
     * @see Node#canAddMixin(String)
     */
    public boolean canAddMixin(String mixinName) throws RepositoryException {
        checkStatus();
        if (!isSupportedOption(Repository.LEVEL_2_SUPPORTED)) {
            // shortcut: repository does not support writing anyway.
            return false;
        }
        try {
            // first check if node is writable regarding protection status,
            // locks, versioning, acces restriction.
            session.getValidator().checkIsWritable(getNodeState(), ItemStateValidator.CHECK_ALL);
            // then make sure the new mixin would not conflict.
            return isValidMixin(new QName[] {getQName(mixinName)});
        } catch (RepositoryException e) {
            log.debug("Cannot add mixin '" + mixinName + "' for the following reason: " + e.getMessage());
            return false;
        } catch (NodeTypeConflictException e) {
            log.debug("Cannot add mixin '" + mixinName + "' for the following reason: " + e.getMessage());
            return false;
        }
    }

    /**
     * @see Node#getDefinition()
     */
    public NodeDefinition getDefinition() throws RepositoryException {
        checkStatus();
        return definition;
    }

    /**
     * @see Node#checkin()
     */
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        checkIsVersionable();
        checkHasPendingChanges();
        checkIsLocked();
        // DIFF JR
        if (isCheckedOut()) {
            session.getVersionManager().checkin(getNodeId());
        } else {
            log.debug("Node " + getPath() + " is already checked in.");
        }
        return getBaseVersion();
    }

    /**
     * @see Node#checkout()
     */
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        checkIsVersionable();
        checkIsLocked();
        // DIFF JR
        if (!isCheckedOut()) {
            session.getVersionManager().checkout(getNodeId());
        } else {
            log.debug("Node " + getPath() + " is already checked out.");
        }
    }

    /**
     * @see Node#doneMerge(Version)
     */
    public void doneMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        resolveMergeConflict(version, true);
    }

    /**
     * @see Node#cancelMerge(Version)
     */
    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        resolveMergeConflict(version, false);
    }

    /**
     * Internal method covering both {@link #doneMerge(Version)} and {@link #cancelMerge(Version)}.
     *
     * @param version
     * @param done
     * @throws VersionException
     * @throws InvalidItemStateException
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    private void resolveMergeConflict(Version version, boolean done) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        checkIsVersionable();
        checkHasPendingChanges();
        checkIsLocked();

        // check if checked out
        if (!isCheckedOut()) {
            String msg = "Unable to finish merge. Node is checked-in: " + safeGetJCRPath();
            log.error(msg);
            throw new VersionException(msg);
        }

        // check if version is in mergeFailed list
        boolean isConflicting = false;
        if (hasProperty(QName.JCR_MERGEFAILED)) {
            Value[] vals = getProperty(QName.JCR_MERGEFAILED).getValues();
            for (int i = 0; i < vals.length && !isConflicting; i++) {
                isConflicting = vals[i].getString().equals(version.getUUID());
            }
        }
        if (!isConflicting) {
            String msg = "Unable to finish merge. Specified version is not in jcr:mergeFailed property: " + safeGetJCRPath();
            log.error(msg);
            throw new VersionException(msg);
        }

        ItemId versionId = session.getHierarchyManager().getItemId(version);
        if (versionId.denotesNode()) {
            session.getVersionManager().resolveMergeConflict(getNodeId(), (NodeId) versionId, done);
        } else {
            throw new RepositoryException("Unexpected error: Failed to retrieve a valid ID for version " + version.getPath());
        }
    }

    /**
     * @see Node#update(String)
     */
    public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        checkIsWritable();
        checkSessionHasPendingChanges();

        // if same workspace, ignore
        if (session.getWorkspace().getName().equals(srcWorkspaceName)) {
            return;
        }
        // make sure the specified workspace is visible for the current session.
        session.checkAccessibleWorkspace(srcWorkspaceName);

        Operation op = Update.create(getNodeId(), srcWorkspaceName);
        session.getSessionItemStateManager().execute(op);
    }

    /**
     * @see Node#merge(String, boolean)
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        checkIsWritable();
        checkSessionHasPendingChanges();

        // if same workspace, ignore
        if (session.getWorkspace().getName().equals(srcWorkspace)) {
            return IteratorHelper.EMPTY;
        }
        // make sure the workspace exists and is accessible for this session.
        session.checkAccessibleWorkspace(srcWorkspace);

        Collection failedIds = session.getVersionManager().merge(getNodeId(), srcWorkspace, bestEffort);
        if (failedIds.isEmpty()) {
            return IteratorHelper.EMPTY;
        } else {
            return new LazyItemIterator(itemMgr, failedIds);
        }
    }

    /**
     * @see Node#getCorrespondingNodePath(String)
     */
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        checkStatus();
        SessionImpl srcSession = null;
        try {
            // create session on other workspace for current subject
            // (may throw NoSuchWorkspaceException and AccessDeniedException)
            // DIFF JR: srcSession = rep.createSession(session.getSubject(), workspaceName);
            srcSession = session.switchWorkspace(workspaceName);

            // search nearest ancestor that is referenceable
            NodeImpl referenceableNode = this;
            while (referenceableNode.getDepth() != Path.ROOT_DEPTH && !referenceableNode.isNodeType(QName.MIX_REFERENCEABLE)) {
                referenceableNode = (NodeImpl) referenceableNode.getParent();
            }

            // if root is common ancestor, corresponding path is same as ours
            // otherwise access referenceable ancestor and calcuate correspond. path.
            String correspondingPath;
            if (referenceableNode.getDepth() == Path.ROOT_DEPTH) {
                if (!srcSession.getItemManager().itemExists(getQPath())) {
                    throw new ItemNotFoundException("No corresponding path found in workspace " + workspaceName + "(" + safeGetJCRPath() + ")");
                } else {
                    correspondingPath = getPath();
                }
            } else {
                // get corresponding ancestor
                Node correspNode = srcSession.getNodeByUUID(referenceableNode.getUUID());
                // path of m2 found, if m1 == n1
                if (referenceableNode == this) {
                    correspondingPath = correspNode.getPath();
                } else {
                    Path p = referenceableNode.getQPath().computeRelativePath(getQPath());
                    // use prefix mappings of srcSession
                    String relPath = srcSession.getNamespaceResolver().getJCRPath(p);
                    if (!correspNode.hasNode(relPath)) {
                        throw new ItemNotFoundException("No corresponding path found in workspace " + workspaceName + "(" + safeGetJCRPath() + ")");
                    } else {
                        correspondingPath = correspNode.getNode(relPath).getPath();
                    }
                }
            }
            return correspondingPath;
        } catch (NameException be) {
            // should never get here...
            String msg = "internal error: failed to determine relative path";
            log.error(msg, be);
            throw new RepositoryException(msg, be);
        } finally {
            if (srcSession != null) {
                // we don't need the other session anymore, logout
                srcSession.logout();
            }
        }
    }

    /**
     * @see Node#isCheckedOut()
     */
    public boolean isCheckedOut() throws RepositoryException {
        checkStatus();
        if (!isSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED)) {
            return true;
        }
        // shortcut: if state is new, its ancestor must be checkout
        if (isNew()) {
            return true;
        }
        return session.getVersionManager().isCheckedOut(getNodeId());
    }

    /**
     * @see Node#restore(String, boolean)
     */
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        checkSessionHasPendingChanges();
        // check for version-enabled and lock are performed with subsequent calls.
        Version v = getVersionHistory().getVersion(versionName);
        restore(v, removeExisting);
    }

    /**
     * @see Node#restore(Version, boolean)
     */
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        checkSessionHasPendingChanges();
        checkIsWritable();
        checkIsVersionable();
        checkIsLocked();

        restore(getNodeId(), version, removeExisting);
    }

    /**
     * @see Node#restore(Version, String, boolean)
     */
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        checkSessionHasPendingChanges();
        // additional checks are performed with subsequest calls.
        if (hasNode(relPath)) {
            // node at 'relPath' exists -> call restore on that node
            getNode(relPath).restore(version, removeExisting);
        } else {
            // node at 'relPath' does not yet exist -> build the NodeId
            Path nPath = getQPath(relPath);
            Path parentPath = nPath.getAncestor(1);
            NodeId nId;
            if (session.getItemManager().itemExists(parentPath)) {
                // If the would-be parent of the location relPath is actually a
                // property, or if a node type restriction would be violated,
                // then a ConstraintViolationException is thrown.
                Item parent = session.getItemManager().getItem(parentPath);
                if (parent.isNode()) {
                    try {
                        Path relQPath = parentPath.computeRelativePath(nPath);
                        nId = session.getIdFactory().createNodeId(((NodeImpl)parent).getNodeId(), relQPath);
                    } catch (MalformedPathException e) {
                        // should not occur
                        throw new RepositoryException(e);
                    }
                } else {
                    throw new ConstraintViolationException("Cannot restore to a parent presenting a property (relative path = '" + relPath + "'");
                }
            } else {
                // although the node itself must not exist, is direct ancestor must.
                throw new PathNotFoundException("Cannot restore to relative path '" + relPath + ": Ancestor does not exist.");
            }
            restore(nId, version, removeExisting);
        }
    }

    /**
     * Common internal restore method for the various Node#restore calls.
     *
     * @param nodeId
     * @param version
     * @param removeExisting
     * @throws PathNotFoundException
     * @throws ItemExistsException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws UnsupportedRepositoryOperationException
     * @throws LockException
     * @throws InvalidItemStateException
     * @throws RepositoryException
     */
    private void restore(NodeId nodeId, Version version, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        ItemId versionId = session.getHierarchyManager().getItemId(version);
        if (versionId.denotesNode()) {
            session.getVersionManager().restore(nodeId, (NodeId) versionId, removeExisting);
        } else {
            throw new RepositoryException("Unexpected error: Failed to retrieve a valid ID for the given version " + version.getPath());
        }
    }

    /**
     * @see Node#restoreByLabel(String, boolean)
     */
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        checkSessionHasPendingChanges();
        // check for version-enabled and lock are performed with subsequent calls.
        Version v = getVersionHistory().getVersionByLabel(versionLabel);
        if (v == null) {
            throw new VersionException("No version for label " + versionLabel + " found.");
        }
        restore(getNodeId(), v, removeExisting);
    }

    /**
     * @see Node#getVersionHistory()
     */
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkIsVersionable();
        return (VersionHistory) getProperty(QName.JCR_VERSIONHISTORY).getNode();
    }

    /**
     * @see Node#getBaseVersion()
     */
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkIsVersionable();
        return (Version) getProperty(QName.JCR_BASEVERSION).getNode();
    }

    /**
     * @see Node#lock(boolean, boolean)
     */
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        checkIsLockable();
        checkHasPendingChanges();

        return session.getLockManager().lock(getNodeId(), isDeep, isSessionScoped);
    }

    /**
     * @see Node#getLock()
     */
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        // lock can be inherited from a parent > do not check for node being lockable.
        checkSupportedOption(Repository.OPTION_LOCKING_SUPPORTED);
        checkStatus();

        return session.getLockManager().getLock(getNodeId());
    }

    /**
     * @see javax.jcr.Node#unlock()
     */
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        checkIsLockable();
        checkHasPendingChanges();

        session.getLockManager().unlock(getNodeId());
    }

    /**
     * @see javax.jcr.Node#holdsLock()
     */
    public boolean holdsLock() throws RepositoryException {
        // lock can be inherited from a parent > do not check for node being lockable.
        checkSupportedOption(Repository.OPTION_LOCKING_SUPPORTED);
        checkStatus();

        // DIFF JR: swich check
        if (isNew() || !isNodeType(QName.MIX_LOCKABLE)) {
            // a node that is new or not lockable never holds a lock
            return false;
        } else {
            // DIFF JR: no separate LockManager.holdsLock
            LockManager lMgr = session.getLockManager();
            return (lMgr.isLocked(getNodeId()) && lMgr.getLock(getNodeId()).getNode().isSame(this));
        }
    }

    /**
     * @see javax.jcr.Node#isLocked()
     */
    public boolean isLocked() throws RepositoryException {
        // lock can be inherited from a parent > do not check for node being lockable.
        checkSupportedOption(Repository.OPTION_LOCKING_SUPPORTED);
        checkStatus();

        return session.getLockManager().isLocked(getNodeId());
    }

    //--------------------------------------------------------< public impl >---
    /**
     *
     * @param qName
     * @return
     * @throws RepositoryException
     */
    boolean isNodeType(QName qName) throws RepositoryException {
        checkStatus();
        // first do trivial checks without using type hierarchy
        if (qName.equals(primaryTypeName)) {
            return true;
        }
        // check if contained in mixin types
        QName[] mixins = getNodeState().getMixinTypeNames();
        for (int i = 0; i < mixins.length; i++) {
            if (mixins[i].equals(qName)) {
                return true;
            }
        }
        // check effective node type
        return getEffectiveNodeType().includesNodeType(qName);
    }

    //-----------------------------------------------------------< ItemImpl >---
    /**
     * @see ItemImpl#getQName()
     */
    QName getQName() throws RepositoryException {
        return session.getHierarchyManager().getQName(getId());
    }


    //------------------------------------------------------< check methods >---
    /**
     * Checks if this nodes session has pending changes.
     *
     * @throws InvalidItemStateException if this nodes session has pending changes
     * @throws RepositoryException
     */
    private void checkSessionHasPendingChanges() throws RepositoryException {
        session.checkHasPendingChanges();
    }

    /**
     *
     * @throws InvalidItemStateException
     * @throws RepositoryException
     */
    private void checkHasPendingChanges() throws InvalidItemStateException, RepositoryException {
        if (hasPendingChanges()) {
            String msg = "Unable to lock node. Node has pending changes: " + getPath();
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }
    }

    /**
     *
     * @return
     */
    private boolean hasPendingChanges() {
        return isModified() || isNew();
    }

    /**
     * Checks if this node is lockable, i.e. has 'mix:lockable'.
     *
     * @throws UnsupportedRepositoryOperationException
     *                             if this node is not lockable
     * @throws RepositoryException if another error occurs
     */
    private void checkIsLockable() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkSupportedOption(Repository.OPTION_LOCKING_SUPPORTED);
        checkStatus();
        if (!isNodeType(QName.MIX_LOCKABLE)) {
            String msg = "Unable to perform locking operation on non-lockable node: " + getPath();
            log.debug(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
    }

    /**
     * Check whether this node is locked by somebody else.
     *
     * @throws LockException       if this node is locked by somebody else
     * @throws RepositoryException if some other error occurs
     */
    void checkIsLocked() throws LockException, RepositoryException {
        if (!isSupportedOption(Repository.OPTION_LOCKING_SUPPORTED) || isNew()) {
            // if locking is not support at all or if this node is new, no
            // checks must be performed.
            return;
        }
        // perform check
        session.getLockManager().checkLock(getNodeId());
    }

    /**
     *
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    private void checkIsVersionable() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        checkStatus();
        if (!isNodeType(QName.MIX_VERSIONABLE)) {
            String msg = "Unable to perform versioning operation on non versionable node: " + getPath();
            log.debug(msg);
            throw new UnsupportedRepositoryOperationException(msg);
        }
    }

    //---------------------------------------------< private implementation >---
    /**
     * Create a new <code>NodeState</code> and subsequently retrieves the
     * corresponding <code>Node</code> object.
     *
     * @param nodeName     name of the new node
     * @param nodeTypeName name of the new node's node type or <code>null</code>
     *                     if it should be determined automatically
     * @return the newly added node
     * @throws ItemExistsException
     * @throws NoSuchNodeTypeException
     * @throws VersionException
     * @throws ConstraintViolationException
     * @throws LockException
     * @throws RepositoryException
     */
    private synchronized Node createNode(QName nodeName, QName nodeTypeName)
        throws ItemExistsException, NoSuchNodeTypeException, VersionException,
        ConstraintViolationException, LockException, RepositoryException {

        // DIFF JR: remove check that assert existing nt. this should be done within following statement
        QNodeDefinition definition = session.getValidator().getApplicableNodeDefinition(nodeName, nodeTypeName, getNodeState());
        if (nodeTypeName == null) {
            // use default node type
            nodeTypeName = definition.getDefaultPrimaryType();
        }
        // validation check are performed by item state manager
        // NOTE: uuid is generated while creating new state.
        Operation an = AddNode.create(getNodeState(), nodeName, nodeTypeName, null);
        itemStateMgr.execute(an);

        // TODO: find better solution...
        NodeId childId = AddNode.getLastCreated(getNodeState(), nodeName);
        // finally retrieve the new node
        return (Node) itemMgr.getItem(childId);
    }

    /**
     *
     * @param qName
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    // TODO: protected due to usage within VersionImpl, VersionHistoryImpl (check for alternatives)
    protected Property getProperty(QName qName) throws PathNotFoundException, RepositoryException {
        checkStatus();
        PropertyId propId = getNodeState().getPropertyId(qName);
        try {
            return (Property) itemMgr.getItem(propId);
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException(qName.toString());
        }
    }

    // DIFF JR: instead of 'getORCreate' only create...
    /**
     * Create a new single valued property
     *
     * @param qName
     * @param type
     * @param value
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     * could be found.
     * @throws RepositoryException if another error occurs.
     */
    private Property createProperty(QName qName, Value value, int type)
            throws ConstraintViolationException, RepositoryException {
        QPropertyDefinition def = getApplicablePropertyDefinition(qName, type, false);
        int targetType = def.getRequiredType();
        if (targetType == PropertyType.UNDEFINED) {
            targetType = type;
        }
        QValue qvs;
        if (targetType == PropertyType.UNDEFINED) {
            qvs = ValueFormat.getQValue(value, session.getNamespaceResolver());
            targetType = qvs.getType();
        } else {
            Value targetValue = ValueHelper.convert(value, targetType, session.getValueFactory());
            qvs = ValueFormat.getQValue(targetValue, session.getNamespaceResolver());
        }
        return createProperty(qName, targetType, def, new QValue[] {qvs});
    }

    // DIFF JR: instead of 'getORCreate' only create...
    /**
     * Create a new multivalue property
     *
     * @param qName
     * @param type
     * @param values
     * @return
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    private Property createProperty(QName qName, Value[] values, int type)
        throws ConstraintViolationException, RepositoryException {
        QPropertyDefinition def = getApplicablePropertyDefinition(qName, type, true);
        int targetType = def.getRequiredType();
        if (targetType == PropertyType.UNDEFINED) {
            targetType = type;
        }
        Value[] targetValues = ValueHelper.convert(values, targetType, session.getValueFactory());
        QValue[] qvs = ValueFormat.getQValues(targetValues, session.getNamespaceResolver());
        // make sure, the final type is not set to undefined
        if (targetType == PropertyType.UNDEFINED) {
            targetType = (qvs.length > 0) ? qvs[0].getType() : PropertyType.STRING;
        }
        return createProperty(qName, targetType, def, qvs);
    }

    /**
     *
     * @param qName
     * @param type
     * @param def
     * @param qvs
     * @return
     * @throws PathNotFoundException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    private Property createProperty(QName qName, int type, QPropertyDefinition def,
                                    QValue[] qvs)
        throws PathNotFoundException, ConstraintViolationException, RepositoryException {
        PropertyId newPId = session.getIdFactory().createPropertyId(getNodeId(), qName);
        Operation op = AddProperty.create(newPId, type, def, qvs);
        itemStateMgr.execute(op);
        return getProperty(qName);
    }

    /**
     *
     * @param jcrName
     * @return
     * @throws RepositoryException
     */
    private QName getQName(String jcrName) throws RepositoryException {
        QName qName;
        try {
            qName = session.getNamespaceResolver().getQName(jcrName);
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid name: " + jcrName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid name: "+ jcrName, upe);
        }
        return qName;
    }

    private boolean isValidMixin(QName[] mixinNames) throws RepositoryException, NodeTypeConflictException {
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();

        // get list of existing nodetypes
        QName[] existingNts = getNodeState().getNodeTypeNames();
        // build effective node type representing primary type including existing mixin's
        EffectiveNodeType entExisting = session.getValidator().getEffectiveNodeType(existingNts);

        // first check characteristics of each mixin
        for (int i = 0; i < mixinNames.length; i++) {
            QName mixinName = mixinNames[i];
            NodeType mixin = ntMgr.getNodeType(mixinName);
            if (!mixin.isMixin()) {
                log.error(mixinName + ": not a mixin node type");
                return false;
            }
            NodeTypeImpl primaryType = ntMgr.getNodeType(primaryTypeName);
            // DIFF JR: replaced 'isDerivedFrom' by 'isNodeType'
            if (primaryType.isNodeType(mixinName)) {
                log.error(mixinName + ": already contained in primary node type");
                return false;
            }
            // check if adding new mixin conflicts with existing nodetypes
            if (entExisting.includesNodeType(mixinName)) {
                log.error(mixinName + ": already contained in mixin types");
                return false;
            }
        }

        // second, build new effective node type for nts including the new mixin
        // types, detecting eventual incompatibilities
        QName[] resultingNts = new QName[existingNts.length + mixinNames.length];
        System.arraycopy(existingNts, 0, resultingNts, 0, existingNts.length);
        System.arraycopy(mixinNames, 0, resultingNts, existingNts.length, mixinNames.length);
        session.getValidator().getEffectiveNodeType(resultingNts);

        // all validations succeeded: return true
        return true;
    }

    /**
     * @return <code>NodeState</code> of this <code>Node</code>
     */
    private NodeState getNodeState() {
        return (NodeState) getItemState();
    }

    /**
     * Return the id of this <code>Node</code>.
     *
     * @return the id of this <code>Node</code>
     */
    private NodeId getNodeId() {
        return getNodeState().getNodeId();
    }

    /**
     * 
     * @param relativePath
     * @return
     * @throws RepositoryException
     */
    private Path getReorderPath(String relativePath) throws RepositoryException {
        try {
            Path p = session.getNamespaceResolver().getQPath(relativePath);
            if (p.isAbsolute() || p.getLength() != 1 || p.getDepth() != 1) {
                throw new RepositoryException("Invalid relative path: " + relativePath);
            }
            return p;
        } catch (MalformedPathException e) {
            String msg = "Invalid relative path: " + relativePath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     *
     * @param relativePath
     * @return
     * @throws RepositoryException
     */
    private Path getQPath(String relativePath) throws RepositoryException {
        try {
            Path p = session.getNamespaceResolver().getQPath(relativePath);
            return Path.create(getQPath(), p, true);
        } catch (MalformedPathException e) {
            String msg = "Invalid relative path: " + relativePath;
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
     * @param relPath relative path of a (possible) node.
     * @return the id of the node at <code>relPath</code> or <code>null</code>
     * if no node exists at <code>relPath</code>.
     * @throws RepositoryException if <code>relPath</code> is not a valid
     * relative path.
     */
    private NodeId resolveRelativeNodePath(String relPath) throws RepositoryException {
        NodeId targetId = null;
        try {
            Path p = getQPath(relPath);
            // if relative path is just the last path element -> simply retrieve
            // the corresponding child-node.
            if (p.getLength() == 1) {
                Path.PathElement pe = p.getNameElement();
                if (pe.denotesName()) {
                    // check if node entry exists
                    int index = pe.getNormalizedIndex();
                    NodeState.ChildNodeEntry cne = getNodeState().getChildNodeEntry(pe.getName(), index);
                    if (cne != null) {
                        targetId = cne.getId();
                    } // else: there's no child node with that name
                }
            } else {
                ItemId id = session.getHierarchyManager().getItemId(p.getCanonicalPath());
                if (id.denotesNode()) {
                    targetId = (NodeId) id;
                } // else:  not a node
            }
        } catch (PathNotFoundException e) {
            // item does not exist -> ignore and return null
        } catch (MalformedPathException e) {
            String msg = "Invalid relative path: " + relPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        return targetId;
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
    private PropertyId resolveRelativePropertyPath(String relPath) throws RepositoryException {
        try {
            /**
             * first check if relPath is just a name (in which case we don't
             * have to build & resolve absolute path)
             */
            if (relPath.indexOf('/') == -1) {
                QName propName = session.getNamespaceResolver().getQName(relPath);
                // check if property entry exists
                if (getNodeState().hasPropertyName(propName)) {
                    return getNodeState().getPropertyId(propName);
                } else {
                    // there's no property with that name
                    return null;
                }
            }
            /**
             * build and resolve absolute path
             */
            Path p = getQPath(relPath).getCanonicalPath();
            try {
                ItemId id = session.getHierarchyManager().getItemId(p);
                if (!id.denotesNode()) {
                    return (PropertyId) id;
                } else {
                    // not a property
                    return null;
                }
            } catch (PathNotFoundException pnfe) {
                return null;
            }
        } catch (NameException e) {
            String msg = "failed to resolve path " + relPath + " relative to " + safeGetJCRPath();
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Returns the effective (i.e. merged and resolved) node type representation
     * of this node's primary and mixin node types.
     *
     * @return the effective node type
     * @throws RepositoryException
     */
    private EffectiveNodeType getEffectiveNodeType() throws RepositoryException {
        // build effective node type of mixins & primary type
        ItemStateValidator validator = session.getValidator();
        return validator.getEffectiveNodeType(getNodeState());
    }

    /**
     * Returns the applicable property definition for a property with the
     * specified name and type.
     *
     * @param propertyName
     * @param type
     * @param multiValued
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    private QPropertyDefinition getApplicablePropertyDefinition(QName propertyName,
                                                                int type,
                                                                boolean multiValued)
            throws ConstraintViolationException, RepositoryException {
        return session.getValidator().getApplicablePropertyDefinition(propertyName, type, multiValued, getNodeState());
    }
}
