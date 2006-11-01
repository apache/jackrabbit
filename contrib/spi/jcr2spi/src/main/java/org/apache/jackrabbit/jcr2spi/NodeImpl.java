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
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildNodeEntry;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateManager;
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
import org.apache.jackrabbit.jcr2spi.version.VersionImpl;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.NodeId;
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
import java.util.Iterator;

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
        // make sure the nodetype name is valid
        if (session.getNodeTypeManager().hasNodeType(nodeTypeName)) {
            primaryTypeName = nodeTypeName;
        } else {
            // should not occur. Since nodetypes are defined by the 'server'
            // its not possible to determine a fallback nodetype that is
            // always available.
            throw new IllegalArgumentException("Unknown nodetype " + LogUtil.saveGetJCRName(nodeTypeName, session.getNamespaceResolver()));
        }
    }

    //-----------------------------------------------------< Item interface >---
    /**
     * @see Item#getName()
     */
    public String getName() throws RepositoryException {
        checkStatus();
        QName qName = getQName();
        try {
            return NameFormat.format(getQName(), session.getNamespaceResolver());
        } catch (NoPrefixDeclaredException e) {
            // should never get here...
            String msg = "Internal error while resolving qualified name " + qName.toString();
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see Item#getParent()
     */
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        checkStatus();

        NodeState parentState = getItemState().getParent();
        // special treatment for root node
        if (parentState == null) {
            String msg = "Root node doesn't have a parent.";
            log.debug(msg);
            throw new ItemNotFoundException(msg);
        }

        return (Node) itemMgr.getItem(parentState);
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
                String msg = "Cannot add a node to property " + LogUtil.safeGetJCRPath(parentPath, session.getNamespaceResolver());
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            } else if (!(parent instanceof NodeImpl)) {
                // should never occur
                String msg = "Incompatible Node object: " + parent + "(" + safeGetJCRPath() + ")";
                log.debug(msg);
                throw new RepositoryException(msg);
            } else {
                parentNode = (NodeImpl) parent;
            }
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
    public synchronized void orderBefore(String srcChildRelPath,
                                         String destChildRelPath)
        throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
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
            throw new ItemNotFoundException("Node " + safeGetJCRPath() + " has no child node with name " + srcChildRelPath);
        }
        if (destChildRelPath != null && !hasNode(destChildRelPath)) {
            throw new ItemNotFoundException("Node " + safeGetJCRPath() + " has no child node with name " + destChildRelPath);
        }

        Path.PathElement srcName = getReorderPath(srcChildRelPath).getNameElement();
        Path.PathElement beforeName = (destChildRelPath == null) ? null : getReorderPath(destChildRelPath).getNameElement();

        Operation op;
        try {
            op = ReorderNodes.create(getNodeState(), srcName, beforeName);
        } catch (NoSuchItemStateException e) {
            throw new ItemNotFoundException(e.getMessage(), e);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to reorder nodes: " + e.getMessage(), e);
        }
        session.getSessionItemStateManager().execute(op);
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
                // TODO: check if is correct to avoid any validation exception
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
                // TODO: check if is correct to avoid any validation exception
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
        return setProperty(name, v, type);
    }

    /**
     * @see Node#setProperty(String, InputStream)
     */
    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        Value v = (value == null ? null : session.getValueFactory().createValue(value));
        return setProperty(name, v, PropertyType.BINARY);
    }

    /**
     * @see Node#setProperty(String, boolean)
     */
    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        return setProperty(name, session.getValueFactory().createValue(value), PropertyType.BOOLEAN);
    }

    /**
     * @see Node#setProperty(String, double)
     */
    public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        return setProperty(name, session.getValueFactory().createValue(value), PropertyType.DOUBLE);
    }

    /**
     * @see Node#setProperty(String, long)
     */
    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        return setProperty(name, session.getValueFactory().createValue(value), PropertyType.LONG);
    }

    /**
     * @see Node#setProperty(String, Calendar)
     */
    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // validation performed in subsequent method
        Value v = (value == null ? null : session.getValueFactory().createValue(value));
        return setProperty(name, v, PropertyType.DATE);
    }

    /**
     * @see Node#setProperty(String, Node)
     */
    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        // duplicate check to make sure, writability is asserted before value
        // validation below.
        checkIsWritable();
        Value v;
        if (value == null) {
            v = null;
        } else {
            PropertyImpl.checkValidReference(value, PropertyType.REFERENCE, this);
            v = session.getValueFactory().createValue(value);
        }
        return setProperty(name, v, PropertyType.REFERENCE);
    }

    /**
     * @see Node#getNode(String)
     */
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        checkStatus();
        NodeState state = resolveRelativeNodePath(relPath);
        if (state == null) {
            throw new PathNotFoundException(relPath);
        }
        return (Node) itemMgr.getItem(state);
    }

    /**
     * @see Node#getNodes()
     */
    public NodeIterator getNodes() throws RepositoryException {
        checkStatus();
        // NOTE: Don't use a class derived from TraversingElementVisitor to traverse
        // the child nodes because this would lead to an infinite recursion.
        try {
            return itemMgr.getChildNodes(getNodeState());
        } catch (ItemNotFoundException infe) {
            String msg = "Failed to list the child nodes of " + safeGetJCRPath();
            log.debug(msg);
            throw new RepositoryException(msg, infe);
        } catch (AccessDeniedException ade) {
            String msg = "Failed to list the child nodes of " + safeGetJCRPath();
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
        PropertyState state = resolveRelativePropertyPath(relPath);
        if (state == null) {
            throw new PathNotFoundException(relPath);
        }
        return (Property) itemMgr.getItem(state);
    }

    /**
     * @see Node#getProperties()
     */
    public PropertyIterator getProperties() throws RepositoryException {
        checkStatus();
        try {
            return itemMgr.getChildProperties(getNodeState());
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
            throw new ItemNotFoundException("No primary item present on Node " + safeGetJCRPath());
        }
        if (hasProperty(name)) {
            return getProperty(name);
        } else if (hasNode(name)) {
            return getNode(name);
        } else {
            throw new ItemNotFoundException("Primary item " + name + " does not exist on Node " + safeGetJCRPath());
        }
    }

    /**
     * @see Node#getUUID()
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkStatus();
        String uuid = getNodeState().getUUID();
        if (uuid == null || !isNodeType(QName.MIX_REFERENCEABLE)) {
            throw new UnsupportedRepositoryOperationException();
        }
        // Node is referenceable -> NodeId must contain a UUID part
        return uuid;
    }

    /**
     * @see Node#getIndex()
     */
    public int getIndex() throws RepositoryException {
        checkStatus();

        if (getNodeState().getDefinition().allowsSameNameSiblings()) {
            NodeState parentState = getItemState().getParent();
            if (parentState == null) {
                // the root node cannot have same-name siblings; always return the
                // default index
                return Path.INDEX_DEFAULT;
            }
            ChildNodeEntry entry = parentState.getChildNodeEntry(getNodeState());
            if (entry == null) {
                String msg = "Unable to retrieve index for: " + safeGetJCRPath();
                throw new RepositoryException(msg);
            }
            return entry.getIndex();
        } else {
            return Path.INDEX_DEFAULT;
        }
    }

    /**
     * @see Node#getReferences()
     */
    public PropertyIterator getReferences() throws RepositoryException {
        checkStatus();
        try {
            ItemStateManager itemStateMgr = session.getItemStateManager();
            Collection refStates = itemStateMgr.getReferingStates(getNodeState());
            if (refStates.isEmpty()) {
                // there are no references, return empty iterator
                return IteratorHelper.EMPTY;
            } else {
                return new LazyItemIterator(itemMgr, refStates);
            }
        } catch (ItemStateException e) {
            String msg = "Unable to retrieve REFERENCE properties that refer to " + safeGetJCRPath();
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see Node#hasNode(String)
     */
    public boolean hasNode(String relPath) throws RepositoryException {
        checkStatus();
        NodeState childState = resolveRelativeNodePath(relPath);
        return (childState != null) ? itemMgr.itemExists(childState) : false;
    }

    /**
     * @see Node#hasProperty(String)
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        checkStatus();
        PropertyState childState = resolveRelativePropertyPath(relPath);
        return (childState != null) ? itemMgr.itemExists(childState) : false;
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
        return itemMgr.hasChildNodes(getNodeState());
    }

    /**
     * @see Node#hasProperties()
     */
    public boolean hasProperties() throws RepositoryException {
        checkStatus();
        return itemMgr.hasChildProperties(getNodeState());
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
            nta[i] = session.getNodeTypeManager().getNodeType(mixinNames[i]);
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
        QName mixinQName = getQName(mixinName);
        try {
            if (!isValidMixin(mixinQName)) {
                throw new ConstraintViolationException("Cannot add '" + mixinName + "' mixin type.");
            }
        } catch (NodeTypeConflictException e) {
            throw new ConstraintViolationException(e.getMessage());
        }

        // merge existing mixins and new mixins to one Array without modifying
        // the node state.
        QName[] currentMixins = getNodeState().getMixinTypeNames();
        QName[] allMixins = new QName[currentMixins.length + 1];
        System.arraycopy(currentMixins, 0, allMixins, 0, currentMixins.length);
        allMixins[currentMixins.length] = mixinQName;
        // perform the operation
        Operation op = SetMixin.create(getNodeState(), allMixins);
        session.getSessionItemStateManager().execute(op);
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
                throw new ConstraintViolationException("Mixin type " + mixinName + " can not be removed: the node is being referenced through at least one property of type REFERENCE");
            }
        }

        // delegate to operation
        QName[] mixins = (QName[]) remainingMixins.toArray(new QName[remainingMixins.size()]);
        Operation op = SetMixin.create(getNodeState(), mixins);
        session.getSessionItemStateManager().execute(op);
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
            return isValidMixin(getQName(mixinName));
        } catch (NodeTypeConflictException e) {
            log.debug("Cannot add mixin '" + mixinName + "': " + e.getMessage());
            return false;
        } catch (LockException e) {
            log.debug("Cannot add mixin '" + mixinName + "': " + e.getMessage());
            return false;
        } catch (VersionException e) {
            log.debug("Cannot add mixin '" + mixinName + "': " + e.getMessage());
            return false;
        } catch (ConstraintViolationException e) {
            log.debug("Cannot add mixin '" + mixinName + "': " + e.getMessage());
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
        if (isCheckedOut()) {
            session.getVersionManager().checkin(getNodeState());
        } else {
            // nothing to do
            log.debug("Node " + safeGetJCRPath() + " is already checked in.");
        }
        return getBaseVersion();
    }

    /**
     * @see Node#checkout()
     */
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        checkIsVersionable();
        checkIsLocked();
        if (!isCheckedOut()) {
            session.getVersionManager().checkout(getNodeState());
        } else {
            // nothing to do
            log.debug("Node " + safeGetJCRPath() + " is already checked out.");
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
            String msg = "Unable to resolve merge conflict. Node is checked-in: " + safeGetJCRPath();
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
            String msg = "Unable to resolve merge conflict. Specified version is not in jcr:mergeFailed property: " + safeGetJCRPath();
            log.error(msg);
            throw new VersionException(msg);
        }

        if (version instanceof VersionImpl) {
            NodeState versionState = ((NodeImpl)version).getNodeState();
            session.getVersionManager().resolveMergeConflict(getNodeState(), versionState, done);
        } else {
            throw new RepositoryException("Incompatible Version object: " + version.getPath());
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

        Operation op = Update.create((NodeState) getNodeState().getWorkspaceState(), srcWorkspaceName);
        ((WorkspaceImpl)session.getWorkspace()).getUpdatableItemStateManager().execute(op);
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

        Collection failedIds = session.getVersionManager().merge(getNodeState(), srcWorkspace, bestEffort);
        if (failedIds.isEmpty()) {
            return IteratorHelper.EMPTY;
        } else {
            List failedStates = new ArrayList();
            Iterator it = failedIds.iterator();
            while (it.hasNext()) {
                try {
                    ItemState state = session.getItemStateManager().getItemState((NodeId) it.next());
                    if (state.isNode()) {
                        failedStates.add(state);
                    } else {
                        // should not occur
                        throw new RepositoryException("Merge failed with internal error: NodeState expected.");
                    }
                } catch (ItemStateException e) {
                    // should not occur
                    throw new RepositoryException(e);
                }
            }
            return new LazyItemIterator(itemMgr, failedStates);
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
            srcSession = session.switchWorkspace(workspaceName);

            // search nearest ancestor that is referenceable
            NodeImpl referenceableNode = this;
            while (referenceableNode.getDepth() != Path.ROOT_DEPTH
                && !referenceableNode.isNodeType(QName.MIX_REFERENCEABLE)) {
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
                    String relPath = PathFormat.format(p, session.getNamespaceResolver());
                    if (!correspNode.hasNode(relPath)) {
                        throw new ItemNotFoundException("No corresponding path found in workspace " + workspaceName + "(" + safeGetJCRPath() + ")");
                    } else {
                        correspondingPath = correspNode.getNode(relPath).getPath();
                    }
                }
            }
            return correspondingPath;
        } catch (NameException e) {
            // should never get here...
            String msg = "Internal error: failed to determine relative path";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
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
        return session.getVersionManager().isCheckedOut(getNodeState());
    }

    /**
     * @see Node#restore(String, boolean)
     */
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        checkSessionHasPendingChanges();
        // check for version-enabled and lock are performed with subsequent calls.
        Version v = getVersionHistory().getVersion(versionName);
        restore(this, null, v, removeExisting);
    }

    /**
     * @see Node#restore(Version, boolean)
     */
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        checkSessionHasPendingChanges();

        restore(this, null, version, removeExisting);
    }

    /**
     * @see Node#restore(Version, String, boolean)
     */
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        checkSupportedOption(Repository.OPTION_VERSIONING_SUPPORTED);
        checkSessionHasPendingChanges();

        // additional checks are performed with subsequest calls.
        if (hasNode(relPath)) {
            // node at 'relPath' exists -> call restore on the target Node
            getNode(relPath).restore(version, removeExisting);
        } else {
            // node at 'relPath' does not yet exist -> build the NodeId
            Path nPath = getQPath(relPath);
            Path parentPath = nPath.getAncestor(1);
            if (itemMgr.itemExists(parentPath)) {
                Item parent = itemMgr.getItem(parentPath);
                if (parent.isNode()) {
                    try {
                        Path relQPath = parentPath.computeRelativePath(nPath);
                        NodeImpl parentNode = ((NodeImpl)parent);
                        // call the restore
                        restore(parentNode, relQPath, version, removeExisting);
                    } catch (MalformedPathException e) {
                        // should not occur
                        throw new RepositoryException(e);
                    }
                } else {
                    // the item at parentParentPath is Property
                    throw new ConstraintViolationException("Cannot restore to a parent presenting a property (relative path = '" + relPath + "'");
                }
            } else {
                // although the node itself must not exist, is direct ancestor must.
                throw new PathNotFoundException("Cannot restore to relative path '" + relPath + ": Ancestor does not exist.");
            }
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
        restore(this, null, v, removeExisting);
    }

    /**
     * Common internal restore method for the various Node#restore calls.
     *
     * @param targetNode
     * @param relQPath
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
    private void restore(NodeImpl targetNode, Path relQPath, Version version, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        if (relQPath == null) {
            /* restore target already exists. */
            // target must be versionable
            targetNode.checkIsVersionable();

            VersionHistory vH = targetNode.getVersionHistory();
            // version must be a version of the target node
            if (!vH.isSame(version.getContainingHistory())) {
                throw new VersionException("Version " + version + " does not correspond to the restore target.");
            }
            // version must not be the root version
            if (vH.getRootVersion().isSame(version)) {
                throw new VersionException("Attempt to restore root version.");
            }
            targetNode.checkIsWritable();
            targetNode.checkIsLocked();
        } else {
            /* If no node exists at relPath then a VersionException is thrown if
               the parent node is not checked out. */
            if (!targetNode.isCheckedOut()) {
                throw new VersionException("Parent " + targetNode.safeGetJCRPath()
                    + " for non-existing restore target '"
                    + LogUtil.safeGetJCRPath(relQPath, session.getNamespaceResolver())
                    + "' must be checked out.");
            }
            targetNode.checkIsLocked();
            // NOTE: check for nodetype constraint violation is left to the 'server'
        }

        if (version instanceof VersionImpl) {
            NodeState versionState = ((NodeImpl)version).getNodeState();
            session.getVersionManager().restore(targetNode.getNodeState(), relQPath, versionState, removeExisting);
        } else {
            throw new RepositoryException("Incompatible Version object: " + version.getPath());
        }
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

        return session.getLockManager().lock(getNodeState(), isDeep, isSessionScoped);
    }

    /**
     * @see Node#getLock()
     */
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        // lock can be inherited from a parent > do not check for node being lockable.
        checkSupportedOption(Repository.OPTION_LOCKING_SUPPORTED);
        checkStatus();

        return session.getLockManager().getLock(getNodeState());
    }

    /**
     * @see javax.jcr.Node#unlock()
     */
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        checkIsLockable();
        checkHasPendingChanges();

        session.getLockManager().unlock(getNodeState());
    }

    /**
     * @see javax.jcr.Node#holdsLock()
     */
    public boolean holdsLock() throws RepositoryException {
        // lock can be inherited from a parent > do not check for node being lockable.
        checkSupportedOption(Repository.OPTION_LOCKING_SUPPORTED);
        checkStatus();

        if (isNew() || !isNodeType(QName.MIX_LOCKABLE)) {
            // a node that is new or not lockable never holds a lock
            return false;
        } else {
            LockManager lMgr = session.getLockManager();
            return (lMgr.isLocked(getNodeState()) && lMgr.getLock(getNodeState()).getNode().isSame(this));
        }
    }

    /**
     * @see javax.jcr.Node#isLocked()
     */
    public boolean isLocked() throws RepositoryException {
        // lock can be inherited from a parent > do not check for node being lockable.
        checkSupportedOption(Repository.OPTION_LOCKING_SUPPORTED);
        checkStatus();

        return session.getLockManager().isLocked(getNodeState());
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
        if (getNodeState().getParent() == null) {
            // shortcut. the given state represents the root or an orphaned node
            return QName.ROOT;
        }

        return getNodeState().getQName();
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
            String msg = "Node has pending changes: " + getPath();
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
            throw new LockException(msg);
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
        session.getLockManager().checkLock(getNodeState());
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

        QNodeDefinition definition = session.getValidator().getApplicableNodeDefinition(nodeName, nodeTypeName, getNodeState());
        if (nodeTypeName == null) {
            // use default node type
            nodeTypeName = definition.getDefaultPrimaryType();
        }
        // validation check are performed by item state manager
        // NOTE: uuid is generated while creating new state.
        Operation an = AddNode.create(getNodeState(), nodeName, nodeTypeName, null);
        session.getSessionItemStateManager().execute(an);

        // retrieve id of state that has been created during execution of AddNode
        NodeState childState;
        try {
            List cne = getNodeState().getChildNodeEntries(nodeName);
            if (definition.allowsSameNameSiblings()) {
                // TODO: find proper solution. problem with same-name-siblings
                childState = ((ChildNodeEntry)cne.get(cne.size()-1)).getNodeState();
            } else {
                childState = ((ChildNodeEntry)cne.get(0)).getNodeState();
            }
        } catch (ItemStateException e) {
            // should not occur
            throw new RepositoryException(e);
        }
        // finally retrieve the new node
        return (Node) itemMgr.getItem(childState);
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
        try {
            PropertyState pState = getNodeState().getPropertyState(qName);
            return (Property) itemMgr.getItem(pState);
        } catch (AccessDeniedException ade) {
            throw new ItemNotFoundException(qName.toString());
        } catch (NoSuchItemStateException e) {
            throw new PathNotFoundException(qName.toString());
        } catch (ItemStateException e) {
            String msg = "Error while accessing property " + qName.toString();
            throw new RepositoryException(msg, e);
        }
    }

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
        // make sure, the final type is not set to undefined        
        if (targetType == PropertyType.UNDEFINED) {
            if (type == PropertyType.UNDEFINED) {
                // try to retrieve type from the values array
                if (values.length > 0) {
                    for (int i = 0; i < values.length; i++) {
                        if (values[i] != null) {
                            targetType = values[i].getType();
                            break;
                        }
                    }
                }
                if (targetType == PropertyType.UNDEFINED) {
                    // fallback
                    targetType = PropertyType.STRING;
                }
            } else {
                targetType = type;
            }
        }
        Value[] targetValues = ValueHelper.convert(values, targetType, session.getValueFactory());
        QValue[] qvs = ValueFormat.getQValues(targetValues, session.getNamespaceResolver());
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
        Operation op = AddProperty.create(getNodeState(), qName, type, def, qvs);
        session.getSessionItemStateManager().execute(op);
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
            qName = NameFormat.parse(jcrName, session.getNamespaceResolver());
        } catch (IllegalNameException ine) {
            throw new RepositoryException("invalid name: " + jcrName, ine);
        } catch (UnknownPrefixException upe) {
            throw new RepositoryException("invalid name: "+ jcrName, upe);
        }
        return qName;
    }

    private boolean isValidMixin(QName mixinName) throws NoSuchNodeTypeException, NodeTypeConflictException {
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();

        // get list of existing nodetypes
        QName[] existingNts = getNodeState().getNodeTypeNames();
        // build effective node type representing primary type including existing mixin's
        EffectiveNodeType entExisting = session.getValidator().getEffectiveNodeType(existingNts);

        // first check characteristics of each mixin
        NodeType mixin = ntMgr.getNodeType(mixinName);
        if (!mixin.isMixin()) {
            log.error(mixin.getName() + ": not a mixin node type");
            return false;
        }
        NodeTypeImpl primaryType = ntMgr.getNodeType(primaryTypeName);
        if (primaryType.isNodeType(mixinName)) {
            log.error(mixin.getName() + ": already contained in primary node type");
            return false;
        }
        // check if adding new mixin conflicts with existing nodetypes
        if (entExisting.includesNodeType(mixinName)) {
            log.error(mixin.getName() + ": already contained in mixin types");
            return false;
        }

        // second, build new effective node type for nts including the new mixin
        // types, detecting eventual incompatibilities
        QName[] resultingNts = new QName[existingNts.length + 1];
        System.arraycopy(existingNts, 0, resultingNts, 0, existingNts.length);
        resultingNts[existingNts.length] = mixinName;
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
            Path p = PathFormat.parse(relativePath, session.getNamespaceResolver());
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
            Path p = PathFormat.parse(relativePath, session.getNamespaceResolver());
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
     * @return the state of the node at <code>relPath</code> or <code>null</code>
     * if no node exists at <code>relPath</code>.
     * @throws RepositoryException if <code>relPath</code> is not a valid
     * relative path.
     */
    private NodeState resolveRelativeNodePath(String relPath) throws RepositoryException {
        NodeState targetState = null;
        try {
            Path p = getQPath(relPath);
            // if relative path is just the last path element -> simply retrieve
            // the corresponding child-node.
            if (p.getLength() == 1) {
                Path.PathElement pe = p.getNameElement();
                if (pe.denotesName()) {
                    // check if node entry exists
                    int index = pe.getNormalizedIndex();
                    ChildNodeEntry cne = getNodeState().getChildNodeEntry(pe.getName(), index);
                    if (cne != null) {
                        targetState = cne.getNodeState();
                    } // else: there's no child node with that name
                }
            } else {
                ItemState itemState = session.getHierarchyManager().getItemState(p.getCanonicalPath());
                if (itemState.isNode()) {
                    targetState = (NodeState) itemState;
                } // else:  not a node
            }
        } catch (PathNotFoundException e) {
            // item does not exist -> ignore and return null
        } catch (MalformedPathException e) {
            String msg = "Invalid relative path: " + relPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        } catch (ItemStateException e) {
            // should not occure
            String msg = "Invalid relative path: " + relPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        return targetState;
    }

    /**
     * Returns the id of the property at <code>relPath</code> or <code>null</code>
     * if no property exists at <code>relPath</code>.
     * <p/>
     * Note that access rights are not checked.
     *
     * @param relPath relative path of a (possible) property
     * @return the state of the property at <code>relPath</code> or
     *         <code>null</code> if no property exists at <code>relPath</code>
     * @throws RepositoryException if <code>relPath</code> is not a valid
     *                             relative path
     */
    private PropertyState resolveRelativePropertyPath(String relPath) throws RepositoryException {
        try {
            /**
             * first check if relPath is just a name (in which case we don't
             * have to build & resolve absolute path)
             */
            if (relPath.indexOf('/') == -1) {
                QName propName = NameFormat.parse(relPath, session.getNamespaceResolver());
                // check if property entry exists
                if (getNodeState().hasPropertyName(propName)) {
                    try {
                        return getNodeState().getPropertyState(propName);
                    } catch (ItemStateException e) {
                        // should not occur due, since existance has been checked
                        throw new RepositoryException(e);
                    }
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
                ItemState itemState = session.getHierarchyManager().getItemState(p);
                if (!itemState.isNode()) {
                    return (PropertyState) itemState;
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
        ItemStateValidator validator = session.getValidator();
        return validator.getApplicablePropertyDefinition(propertyName, type, multiValued, getNodeState());
    }
}
