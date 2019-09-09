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

import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.lock.LockStateManager;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.jcr2spi.operation.AddNode;
import org.apache.jackrabbit.jcr2spi.operation.AddProperty;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.ReorderNodes;
import org.apache.jackrabbit.jcr2spi.operation.SetMixin;
import org.apache.jackrabbit.jcr2spi.operation.SetPrimaryType;
import org.apache.jackrabbit.jcr2spi.operation.Update;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.jcr2spi.util.StateUtility;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.util.ChildrenCollectorFilter;
import org.apache.jackrabbit.value.ValueHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * <code>NodeImpl</code>...
 */
public class NodeImpl extends ItemImpl implements Node {

    private static Logger log = LoggerFactory.getLogger(NodeImpl.class);

    protected NodeImpl(SessionImpl session, NodeState state, ItemLifeCycleListener[] listeners) {
        super(session, state, listeners);
        Name nodeTypeName = state.getNodeTypeName();
        // make sure the nodetype name is valid
        if (!session.getNodeTypeManager().hasNodeType(nodeTypeName)) {
            // should not occur. Since nodetypes are defined by the 'server'
            // its not possible to determine a fallback nodetype that is
            // always available.
            throw new IllegalArgumentException("Unknown nodetype " + LogUtil.saveGetJCRName(nodeTypeName, session.getNameResolver()));
        }
    }

    //---------------------------------------------------------------< Item >---
    /**
     * @see Item#getName()
     */
    @Override
    public String getName() throws RepositoryException {
        checkStatus();
        return session.getNameResolver().getJCRName(getQName());
    }

    /**
     * Implementation of {@link Item#accept(javax.jcr.ItemVisitor)} for nodes.
     *
     * @param visitor
     * @throws RepositoryException
     * @see Item#accept(javax.jcr.ItemVisitor)
     */
    @Override
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
    @Override
    public boolean isNode() {
        return true;
    }

    //---------------------------------------------------------------< Node >---
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
        // build path object and retrieve parent node
        Path nodePath = getPath(relPath).getNormalizedPath();
        if (nodePath.getIndex() != Path.INDEX_UNDEFINED) {
            String msg = "Illegal subscript specified: " + relPath;
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        NodeImpl parentNode;
        if (nodePath.getLength() == 1) {
            parentNode = this;
        } else {
            Path parentPath = nodePath.getAncestor(1);
            ItemManager itemMgr = getItemManager();
            if (itemMgr.nodeExists(parentPath)) {
                parentNode = (NodeImpl) itemMgr.getNode(parentPath);
            } else if (itemMgr.propertyExists(parentPath)) {
                String msg = "Cannot add a node to property " + LogUtil.safeGetJCRPath(parentPath, session.getPathResolver());
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            } else {
                throw new PathNotFoundException("Cannot add a new node to a non-existing parent at " + LogUtil.safeGetJCRPath(parentPath, session.getPathResolver()));
            }
        }

        // get names objects for node and nt
        Name nodeName = nodePath.getName();
        Name ntName = (primaryNodeTypeName == null) ? null : getQName(primaryNodeTypeName);

        // create new node (including validation checks)
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

        Path srcPath = getReorderPath(srcChildRelPath);
        Path beforePath = null;
        if (destChildRelPath != null) {
            beforePath = getReorderPath(destChildRelPath);
        }

        Operation op = ReorderNodes.create(getNodeState(), srcPath, beforePath);
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
        Name propName = getQName(name);
        Property prop;
        if (hasProperty(propName)) {
            // property already exists: pass call to property
            prop = getProperty(propName);
            Value v = (type == PropertyType.UNDEFINED) ? value : ValueHelper.convert(value, type, session.getValueFactory());
            prop.setValue(v);
        } else {
            if (value == null) {
                return new StaleProperty();
            } else {
                // new property to be added
                prop = createProperty(propName, value, type);
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
        Name propName = getQName(name);
        Property prop;
        if (hasProperty(propName)) {
            // property already exists: pass call to property
            prop = getProperty(propName);
            Value[] vs = (type == PropertyType.UNDEFINED) ? values : ValueHelper.convert(values, type, session.getValueFactory());
            prop.setValue(vs);
        } else {
            if (values == null) {
                // create and remove property is a nop.
                throw new ItemNotFoundException("Cannot remove a non-existing property.");
            } else {
                // new property to be added
                prop = createProperty(propName, values, type);
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
        Value v = (value == null) ? null : session.getValueFactory().createValue(value, PropertyType.STRING);
        return setProperty(name, v, PropertyType.UNDEFINED);
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
     * @see javax.jcr.Node#setProperty(String, Binary)
     */
    public Property setProperty(String name, Binary value) throws RepositoryException {
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
     * @see javax.jcr.Node#setProperty(String, BigDecimal)
     */
    public Property setProperty(String name, BigDecimal value) throws RepositoryException {
        // validation performed in subsequent method
        Value v = (value == null ? null : session.getValueFactory().createValue(value));
        return setProperty(name, v, PropertyType.DECIMAL);
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
        // duplicate check to make sure, property can be written before value
        // validation below.
        checkIsWritable();
        Value v;
        if (value == null) {
            v = null;
        } else {
            PropertyImpl.checkValidReference(value, PropertyType.REFERENCE, session.getNameResolver());
            v = session.getValueFactory().createValue(value);
        }
        return setProperty(name, v, PropertyType.REFERENCE);
    }

    /**
     * @see Node#getNode(String)
     */
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        checkStatus();
        NodeEntry nodeEntry = resolveRelativeNodePath(relPath);
        if (nodeEntry == null) {
            throw new PathNotFoundException(relPath);
        }
        try {
            return (Node) getItemManager().getItem(nodeEntry);
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(relPath, e);
        }
    }

    /**
     * @see Node#getNodes()
     */
    public NodeIterator getNodes() throws RepositoryException {
        checkStatus();
        // NOTE: Don't use a class derived from TraversingElementVisitor to traverse
        // the child nodes because this would lead to an infinite recursion.
        try {
            return getItemManager().getChildNodes(getNodeEntry());
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

        return ChildrenCollectorFilter.collectChildNodes(this, namePattern);
    }

    /**
     * @see javax.jcr.Node#getNodes(String[])
     */
    public NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {
        checkStatus();

        return ChildrenCollectorFilter.collectChildNodes(this, nameGlobs);
    }

    /**
     * @see Node#getProperty(String)
     */
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        checkStatus();
        PropertyEntry entry = resolveRelativePropertyPath(relPath);
        if (entry == null) {
            throw new PathNotFoundException(relPath);
        }
        try {
            return (Property) getItemManager().getItem(entry);
        } catch (AccessDeniedException e) {
            throw new PathNotFoundException(relPath);
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(relPath);
        }
    }

    /**
     * @see Node#getProperties()
     */
    public PropertyIterator getProperties() throws RepositoryException {
        checkStatus();
        try {
            return getItemManager().getChildProperties(getNodeEntry());
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

        return ChildrenCollectorFilter.collectProperties(this, namePattern);
    }

    /**
     * @see javax.jcr.Node#getProperties(String)
     */
    public PropertyIterator getProperties(String[] nameGlobs)
            throws RepositoryException {
        checkStatus();

        return ChildrenCollectorFilter.collectProperties(this, nameGlobs);
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
        String uuid = getNodeState().getUniqueID();
        if (uuid == null || !isNodeType(NameConstants.MIX_REFERENCEABLE)) {
            throw new UnsupportedRepositoryOperationException();
        }
        // Node is referenceable -> NodeId must contain a UUID part
        return uuid;
    }

    /**
     * @see Node#getIdentifier()
     */
    public String getIdentifier() throws RepositoryException {
        checkStatus();
        return session.getIdFactory().toJcrIdentifier(getNodeEntry().getId());
    }

    /**
     * @see Node#getIndex()
     */
    public int getIndex() throws RepositoryException {
        checkStatus();
        int index = getNodeEntry().getIndex();
        if (index == Path.INDEX_UNDEFINED) {
            throw new RepositoryException("Error while retrieving index.");
        }
        return index;
    }

    /**
     * @see Node#getReferences()
     */
    public PropertyIterator getReferences() throws RepositoryException {
        return getReferences(null);
    }

    /**
     * @see javax.jcr.Node#getReferences(String)
     */
    public PropertyIterator getReferences(String name) throws RepositoryException {
        return getReferences(name, false);
    }

    /**
     * @see javax.jcr.Node#getWeakReferences()
     */
    public PropertyIterator getWeakReferences() throws RepositoryException {
        return getWeakReferences(null);
    }

    /**
     * @see javax.jcr.Node#getWeakReferences()
     */
    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        return getReferences(name, true);
    }

    /**
     * @see Node#hasNode(String)
     */
    public boolean hasNode(String relPath) throws RepositoryException {
        checkStatus();
        NodeEntry nodeEntry = resolveRelativeNodePath(relPath);
        return (nodeEntry != null) && getItemManager().itemExists(nodeEntry);
    }

    /**
     * @see Node#hasProperty(String)
     */
    public boolean hasProperty(String relPath) throws RepositoryException {
        checkStatus();
        PropertyEntry childEntry = resolveRelativePropertyPath(relPath);
        return (childEntry != null) && getItemManager().itemExists(childEntry);
    }

    /**
     * Returns true, if this <code>Node</code> has a property with the given name.
     *
     * @param propertyName
     * @return <code>true</code>, if this <code>Node</code> has a property with
     * the given name.
     */
    private boolean hasProperty(Name propertyName) {
        return getNodeEntry().hasPropertyEntry(propertyName);
    }

    /**
     * @see Node#hasNodes()
     */
    public boolean hasNodes() throws RepositoryException {
        checkStatus();
        return getItemManager().hasChildNodes(getNodeEntry());
    }

    /**
     * @see Node#hasProperties()
     */
    public boolean hasProperties() throws RepositoryException {
        checkStatus();
        return getItemManager().hasChildProperties(getNodeEntry());
    }

    /**
     * @see Node#getPrimaryNodeType()
     */
    public NodeType getPrimaryNodeType() throws RepositoryException {
        checkStatus();
        return session.getNodeTypeManager().getNodeType(getPrimaryNodeTypeName());
    }

    /**
     * @see javax.jcr.Node#setPrimaryType(String)
     */
    public void setPrimaryType(String nodeTypeName) throws RepositoryException {
        checkStatus();

        if (getNodeState().isRoot()) {
            String msg = "The primary type of the root node may not be changed.";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        Name ntName = getQName(nodeTypeName);
        if (ntName.equals(getPrimaryNodeTypeName())) {
            log.debug("Changing the primary type has no effect: '" + nodeTypeName + "' already is the primary node type.");
            return;
        }

        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
        NodeType nt = ntMgr.getNodeType(ntName);
        if (nt.isMixin() || nt.isAbstract()) {
            throw new ConstraintViolationException("Cannot change the primary type: '" + nodeTypeName + "' is a mixin type or abstract.");
        }

        // perform the operation
        Operation op = SetPrimaryType.create(getNodeState(), ntName);
        session.getSessionItemStateManager().execute(op);
    }

    /**
     * @see Node#getMixinNodeTypes()
     */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        checkStatus();
        Name[] mixinNames = getNodeState().getMixinTypeNames();
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
        checkStatus();
        // try shortcut first (avoids parsing of name)
        if (session.getNameResolver().getJCRName(getPrimaryNodeTypeName()).equals(nodeTypeName)) {
            return true;
        }
        // parse to Name and check against effective nodetype
        return isNodeType(getQName(nodeTypeName));
    }

    /**
     * @see Node#addMixin(String)
     */
    public void addMixin(String mixinName) throws NoSuchNodeTypeException,
        VersionException, ConstraintViolationException, LockException, RepositoryException {
        checkIsWritable();
        Name mixinQName = getQName(mixinName);

        // get mixin types present in the jcr:mixinTypes property without
        // modifying the NodeState.
        List<Name> mixinValue = getMixinTypes();
        if (!mixinValue.contains(mixinQName) && !isNodeType(mixinQName)) {
            if (!canAddMixin(mixinQName)) {
                throw new ConstraintViolationException("Cannot add '" + mixinName + "' mixin type.");
            }

            mixinValue.add(mixinQName);
            // perform the operation
            Operation op = SetMixin.create(getNodeState(), mixinValue.toArray(new Name[mixinValue.size()]));
            session.getSessionItemStateManager().execute(op);
        }
    }

    /**
     * @see Node#removeMixin(String)
     */
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException,
        VersionException, ConstraintViolationException, LockException, RepositoryException {
        checkIsWritable();
        Name ntName = getQName(mixinName);
        List<Name> mixinValue = getMixinTypes();
        // remove name of target mixin
        if (!mixinValue.remove(ntName)) {
            throw new NoSuchNodeTypeException("Cannot remove mixin '" + mixinName + "': Nodetype is not present on this node.");
        }

        // mix:referenceable needs additional assertion: the mixin cannot be
        // removed, if any references are left to this node.
        NodeTypeImpl mixin = session.getNodeTypeManager().getNodeType(ntName);
        if (mixin.isNodeType(NameConstants.MIX_REFERENCEABLE)) {
            EffectiveNodeType entRemaining = getRemainingENT(mixinValue);
            if (!entRemaining.includesNodeType(NameConstants.MIX_REFERENCEABLE)) {
                PropertyIterator iter = getReferences();
                if (iter.hasNext()) {
                    throw new ConstraintViolationException("Mixin type " + mixinName + " can not be removed: the node is being referenced through at least one property of type REFERENCE");
                }
            }
        }

        /*
         * mix:lockable: the mixin cannot be removed if the node is currently
         * locked even if the editing session is the lock holder.
         */
        if (mixin.isNodeType((NameConstants.MIX_LOCKABLE))) {
            EffectiveNodeType entRemaining = getRemainingENT(mixinValue);
            if (!entRemaining.includesNodeType(NameConstants.MIX_LOCKABLE) && isLocked()) {
                throw new ConstraintViolationException(mixinName + " can not be removed: the node is locked.");
            }
        }

        // delegate to operation
        Name[] mixins = mixinValue.toArray(new Name[mixinValue.size()]);
        Operation op = SetMixin.create(getNodeState(), mixins);
        session.getSessionItemStateManager().execute(op);
    }

    /**
     * Retrieves the value of the jcr:mixinTypes property present with this
     * Node including those that have been transiently added and excluding
     * those, that have been transiently removed.<br>
     * NOTE, that the result of this method, does NOT represent the list of
     * mixin-types that currently affect this node.
     *
     * @return mixin names present with the jcr:mixinTypes property.
     */
    private List<Name> getMixinTypes() {
        Name[] mixinValue;
        if (getNodeState().getStatus() == Status.EXISTING) {
            // jcr:mixinTypes must correspond to the mixins present on the nodestate.
            mixinValue = getNodeState().getMixinTypeNames();
        } else {
            try {
                PropertyEntry pe = getNodeEntry().getPropertyEntry(NameConstants.JCR_MIXINTYPES);
                if (pe != null) {
                    // prop entry exists (and ev. has been transiently mod.)
                    // -> retrieve mixin types from prop
                    mixinValue = StateUtility.getMixinNames(pe.getPropertyState());
                } else {
                    // prop entry has not been loaded yet -> not modified
                    mixinValue = getNodeState().getMixinTypeNames();
                }
            } catch (RepositoryException e) {
                // should never occur
                log.warn("Internal error", e);
                mixinValue = Name.EMPTY_ARRAY;
            }
        }
        List<Name> l = new ArrayList<Name>();
        l.addAll(Arrays.asList(mixinValue));
        return l;
    }

    /**
     * Build the effective node type of remaining mixin's & primary type
     *
     * @param remainingMixins
     * @return effective node type
     * @throws ConstraintViolationException
     * @throws NoSuchNodeTypeException
     */
    private EffectiveNodeType getRemainingENT(List<Name> remainingMixins)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        Name[] allRemaining = remainingMixins.toArray(new Name[remainingMixins.size() + 1]);
        allRemaining[remainingMixins.size()] = getPrimaryNodeTypeName();
        return session.getEffectiveNodeTypeProvider().getEffectiveNodeType(allRemaining);
    }

    /**
     * @see Node#canAddMixin(String)
     */
    public boolean canAddMixin(String mixinName) throws RepositoryException {
        if (!isWritable()) {
            // shortcut: repository does not support writing anyway.
            return false;
        }
        try {
            // first check if node is writable regarding protection status,
            // locks, versioning, access restriction.
            session.getValidator().checkIsWritable(getNodeState(), ItemStateValidator.CHECK_ALL);
            // then make sure the new mixin would not conflict.
            return canAddMixin(getQName(mixinName));
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
        QNodeDefinition qnd = getNodeState().getDefinition();
        return session.getNodeTypeManager().getNodeDefinition(qnd);
    }

    /**
     * @see Node#checkin()
     */
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        checkIsVersionable();
        checkHasPendingChanges();
        checkIsLocked();
        if (isCheckedOut()) {
            NodeEntry newVersion = session.getVersionStateManager().checkin(getNodeState());
            return (Version) getItemManager().getItem(newVersion);
        } else {
            // nothing to do
            log.debug("Node " + safeGetJCRPath() + " is already checked in.");
            return getBaseVersion();
        }
    }

    /**
     * @see Node#checkout()
     */
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        checkIsVersionable();
        checkIsLocked();
        if (!isCheckedOut()) {
            if (session.isSupportedOption(Repository.OPTION_ACTIVITIES_SUPPORTED)) {
                NodeImpl activity = (NodeImpl) session.getWorkspace().getVersionManager().getActivity();
                NodeId activityId = (activity == null) ? null : activity.getNodeState().getNodeId();
                session.getVersionStateManager().checkout(getNodeState(), activityId);
            } else {
                session.getVersionStateManager().checkout(getNodeState());
            }
        } else {
            // nothing to do
            log.debug("Node " + safeGetJCRPath() + " is already checked out.");
        }
    }

    Version checkpoint() throws RepositoryException {
        checkIsVersionable();
        checkHasPendingChanges();
        checkIsLocked();
        if (!isCheckedOut()) {
            checkout();
            return getBaseVersion();
        } else {
            NodeEntry newVersion;
            if (session.isSupportedOption(Repository.OPTION_ACTIVITIES_SUPPORTED)) {
                NodeImpl activity = (NodeImpl) session.getWorkspace().getVersionManager().getActivity();
                NodeId activityId = (activity == null) ? null : activity.getNodeState().getNodeId();
                newVersion = session.getVersionStateManager().checkpoint(getNodeState(), activityId);
            } else {
                newVersion = session.getVersionStateManager().checkpoint(getNodeState());
            }
            return (Version) getItemManager().getItem(newVersion);
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
        if (hasProperty(NameConstants.JCR_MERGEFAILED)) {
            Value[] vals = getProperty(NameConstants.JCR_MERGEFAILED).getValues();
            for (int i = 0; i < vals.length && !isConflicting; i++) {
                isConflicting = vals[i].getString().equals(version.getUUID());
            }
        }
        if (!isConflicting) {
            String msg = "Unable to resolve merge conflict. Specified version is not in jcr:mergeFailed property: " + safeGetJCRPath();
            log.error(msg);
            throw new VersionException(msg);
        }

        NodeState versionState = session.getVersionState(version);
        session.getVersionStateManager().resolveMergeConflict(getNodeState(), versionState, done);
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
        // test if the corresponding node exists in the src-workspace which includes
        // a check if the specified source workspace is accessible
        try {
            getCorrespondingNodePath(srcWorkspaceName);
        } catch (ItemNotFoundException e) {
            // no corresponding node exists -> method has not effect
            return;
        }

        Operation op = Update.create(getNodeState(), srcWorkspaceName);
        ((WorkspaceImpl)session.getWorkspace()).getUpdatableItemStateManager().execute(op);
    }

    /**
     * @see Node#merge(String, boolean)
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        return session.getWorkspace().getVersionManager().merge(getPath(), srcWorkspace, bestEffort);
    }


    /**
     * TODO: Issue 728 of the pfd... this method is a leftover and will be removed in the final version.
     * -&gt; change to package protected then
     */
    public NodeIterator merge(String srcWorkspace, boolean bestEffort, boolean isShallow) throws RepositoryException {
        return session.getWorkspace().getVersionManager().merge(getPath(), srcWorkspace, bestEffort, isShallow);
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
                && !referenceableNode.isNodeType(NameConstants.MIX_REFERENCEABLE)) {
                referenceableNode = (NodeImpl) referenceableNode.getParent();
            }

            // if root is common ancestor, corresponding path is same as ours
            // otherwise access referenceable ancestor and calculate correspond. path.
            String correspondingPath;
            if (referenceableNode.getDepth() == Path.ROOT_DEPTH) {
                if (!srcSession.getItemManager().nodeExists(getQPath())) {
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
                    String relPath = session.getPathResolver().getJCRPath(p);
                    if (!correspNode.hasNode(relPath)) {
                        throw new ItemNotFoundException("No corresponding path found in workspace " + workspaceName + "(" + safeGetJCRPath() + ")");
                    } else {
                        correspondingPath = correspNode.getNode(relPath).getPath();
                    }
                }
            }
            return correspondingPath;
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
        // shortcut: if state is new, its ancestor must be checkout
        if (isNew()) {
            return true;
        }
        return session.getVersionStateManager().isCheckedOut(getNodeState());
    }

    /**
     * @see Node#restore(String, boolean)
     */
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        checkSessionHasPendingChanges();
        // check for version-enabled and lock are performed with subsequent calls.
        Version v = getVersionHistory().getVersion(versionName);
        restore(this, null, v, removeExisting);
    }

    /**
     * @see Node#restore(Version, boolean)
     */
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        checkSessionHasPendingChanges();
        restore(this, null, version, removeExisting);
    }

    /**
     * @see Node#restore(Version, String, boolean)
     */
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        checkSessionHasPendingChanges();

        // additional checks are performed with subsequent calls.
        if (hasNode(relPath)) {
            // node at 'relPath' exists -> call restore on the target Node
            getNode(relPath).restore(version, removeExisting);
        } else {
            // node at 'relPath' does not yet exist -> build the NodeId
            Path nPath = getPath(relPath);
            Path parentPath = nPath.getAncestor(1);
            ItemManager itemMgr = getItemManager();
            if (itemMgr.nodeExists(parentPath)) {
                Node parent = itemMgr.getNode(parentPath);
                Path relQPath = parentPath.computeRelativePath(nPath);
                NodeImpl parentNode = ((NodeImpl)parent);
                // call the restore
                restore(parentNode, relQPath, version, removeExisting);
            } else if (itemMgr.propertyExists(parentPath)) {
                // the item at parentParentPath is Property
                throw new ConstraintViolationException("Cannot restore to a parent presenting a property (relative path = '" + relPath + "'");
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
                    + LogUtil.safeGetJCRPath(relQPath, session.getPathResolver())
                    + "' must be checked out.");
            }
            targetNode.checkIsLocked();
            // NOTE: check for nodetype constraint violation is left to the 'server'
        }

        NodeState versionState = session.getVersionState(version);
        session.getVersionStateManager().restore(targetNode.getNodeState(), relQPath, versionState, removeExisting);
    }

    /**
     * @see Node#getVersionHistory()
     */
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkIsVersionable();
        return (VersionHistory) getProperty(NameConstants.JCR_VERSIONHISTORY).getNode();
    }

    /**
     * @see Node#getBaseVersion()
     */
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkIsVersionable();
        return (Version) getProperty(NameConstants.JCR_BASEVERSION).getNode();
    }

    /**
     * @see Node#lock(boolean, boolean)
     */
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        return lock(isDeep, isSessionScoped, Long.MAX_VALUE, null);
    }

    public Lock lock(boolean isDeep, boolean isSessionScoped, long timeoutHint, String ownerHint) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        checkIsLockable();
        checkHasPendingChanges();

        return session.getLockStateManager().lock(getNodeState(), isDeep, isSessionScoped, timeoutHint, ownerHint);
    }

    /**
     * @see Node#getLock()
     */
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        // lock can be inherited from a parent > do not check for node being lockable.
        checkStatus();
        return session.getLockStateManager().getLock(getNodeState());
    }

    /**
     * @see javax.jcr.Node#unlock()
     */
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        checkIsLockable();
        checkHasPendingChanges();

        session.getLockStateManager().unlock(getNodeState());
    }

    /**
     * @see javax.jcr.Node#holdsLock()
     */
    public boolean holdsLock() throws RepositoryException {
        // lock can be inherited from a parent > do not check for node being lockable.
        checkStatus();
        if (isNew() || !isNodeType(NameConstants.MIX_LOCKABLE)) {
            // a node that is new or not lockable never holds a lock
            return false;
        } else {
            LockStateManager lMgr = session.getLockStateManager();
            return (lMgr.isLocked(getNodeState()) && lMgr.getLock(getNodeState()).getNode().isSame(this));
        }
    }

    /**
     * @see javax.jcr.Node#isLocked()
     */
    public boolean isLocked() throws RepositoryException {
        // lock can be inherited from a parent > do not check for node being lockable.
        checkStatus();
        return session.getLockStateManager().isLocked(getNodeState());
    }

    /**
     * @see javax.jcr.Node#followLifecycleTransition(String)
     */
    public void followLifecycleTransition(String transition) throws RepositoryException {
        session.checkSupportedOption(Repository.OPTION_LIFECYCLE_SUPPORTED);

        // TODO: implementation missing
        throw new UnsupportedRepositoryOperationException("JCR-1104");
    }

    /**
     * @see javax.jcr.Node#getAllowedLifecycleTransistions()
     */
    public String[] getAllowedLifecycleTransistions() throws RepositoryException {
        session.checkSupportedOption(Repository.OPTION_LIFECYCLE_SUPPORTED);
        
        // TODO: implementation missing
        throw new UnsupportedRepositoryOperationException("JCR-1104");
    }

    /**
     * @see javax.jcr.Node#getSharedSet()
     */
    public NodeIterator getSharedSet() throws RepositoryException {
        // TODO: implementation missing
        throw new UnsupportedRepositoryOperationException("JCR-1104");
    }

    /**
     * @see javax.jcr.Node#removeShare()
     */
    public void removeShare() throws RepositoryException {
        // TODO: implementation missing
        throw new UnsupportedRepositoryOperationException("JCR-1104");
    }

    /**
     * @see javax.jcr.Node#removeSharedSet()
     */
    public void removeSharedSet() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("JCR-1104");
    }

    //--------------------------------------------------------< public impl >---
    /**
     *
     * @param qName
     * @return
     * @throws RepositoryException
     */
    boolean isNodeType(Name qName) throws RepositoryException {
        // first do trivial checks without using type hierarchy
        if (qName.equals(getPrimaryNodeTypeName())) {
            return true;
        }
        // check if contained in mixin types
        for (Name mixin : getNodeState().getMixinTypeNames()) {
            if (mixin.equals(qName)) {
                return true;
            }
        }
        // NEW nodes with inherited-mixins -> mixin not yet active
        if (getNodeState().getStatus() == Status.NEW &&
            session.getNodeTypeManager().getNodeType(qName).isMixin()) {
            return false;
        }

        // check effective node type
        EffectiveNodeType effnt = session.getEffectiveNodeTypeProvider().getEffectiveNodeType(getNodeState().getNodeTypeNames());
        return effnt.includesNodeType(qName);
    }

    //-----------------------------------------------------------< ItemImpl >---
    /**
     * @see ItemImpl#getName()
     */
    @Override
    Name getQName() throws RepositoryException {
        if (getNodeState().isRoot()) {
            // shortcut. the given state represents the root or an orphaned node
            return NameConstants.ROOT;
        }

        return getNodeState().getName();
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
     * @return true if this <code>Node</code> is modified or new.
     */
    private boolean hasPendingChanges() {
        return isModified() || isNew();
    }

    /**
     * Checks if this node is lockable, i.e. has 'mix:lockable'.
     *
     * @throws UnsupportedRepositoryOperationException if this node is not lockable.
     * @throws RepositoryException if another error occurs.
     */
    private void checkIsLockable() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkStatus();
        if (!isNodeType(NameConstants.MIX_LOCKABLE)) {
            String msg = "Unable to perform locking operation on non-lockable node: " + getPath();
            log.debug(msg);
            throw new LockException(msg);
        }
    }

    /**
     * Check whether this node is locked by somebody else.
     *
     * @throws LockException if this node is locked by somebody else.
     * @throws RepositoryException if some other error occurs.
     */
    void checkIsLocked() throws LockException, RepositoryException {
        if (isNew()) {
            // if this node is new, no checks must be performed.
            return;
        }
        // perform check
        session.getLockStateManager().checkLock(getNodeState());
    }

    /**
     * Check if this node is versionable.
     *
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    private void checkIsVersionable() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkStatus();
        if (!isNodeType(NameConstants.MIX_VERSIONABLE)) {
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
    private synchronized Node createNode(Name nodeName, Name nodeTypeName)
        throws ItemExistsException, NoSuchNodeTypeException, VersionException,
        ConstraintViolationException, LockException, RepositoryException {

        QNodeDefinition definition = session.getItemDefinitionProvider().getQNodeDefinition(getNodeState().getAllNodeTypeNames(), nodeName, nodeTypeName);
        if (nodeTypeName == null) {
            // use default node type
            nodeTypeName = definition.getDefaultPrimaryType();
        }
        // validation check are performed by item state manager
        // NOTE: uuid is generated while creating new state.
        Operation an = AddNode.create(getNodeState(), nodeName, nodeTypeName, null);
        session.getSessionItemStateManager().execute(an);

        // finally retrieve the new node
        List<ItemState> addedStates = ((AddNode) an).getAddedStates();
        ItemState nState = addedStates.get(0);
        return (Node) getItemManager().getItem(nState.getHierarchyEntry());
    }

    // TODO: protected due to usage within VersionImpl, VersionHistoryImpl (check for alternatives)
    /**
     *
     * @param nodeName
     * @param index
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected Node getNode(Name nodeName, int index) throws PathNotFoundException, RepositoryException {
        checkStatus();
        try {
            NodeEntry nEntry = getNodeEntry().getNodeEntry(nodeName, index);
            if (nEntry == null) {
                throw new PathNotFoundException(LogUtil.saveGetJCRName(nodeName, session.getNameResolver()));
            }
            return (Node) getItemManager().getItem(nEntry);
        } catch (AccessDeniedException e) {
            throw new PathNotFoundException(LogUtil.saveGetJCRName(nodeName, session.getNameResolver()));
        }
    }

    /**
     *
     * @param qName
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    // TODO: protected due to usage within VersionImpl, VersionHistoryImpl (check for alternatives)
    protected Property getProperty(Name qName) throws PathNotFoundException, RepositoryException {
        checkStatus();
        try {
            PropertyEntry pEntry = getNodeEntry().getPropertyEntry(qName, true);
            if (pEntry == null) {
                throw new PathNotFoundException(LogUtil.saveGetJCRName(qName, session.getNameResolver()));
            }
            return (Property) getItemManager().getItem(pEntry);
        } catch (AccessDeniedException e) {
            throw new PathNotFoundException(LogUtil.saveGetJCRName(qName, session.getNameResolver()));
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
    private Property createProperty(Name qName, Value value, int type)
            throws ConstraintViolationException, RepositoryException {
        QPropertyDefinition def = getApplicablePropertyDefinition(qName, type, false);
        int targetType = def.getRequiredType();
        if (targetType == PropertyType.UNDEFINED) {
            targetType = type;
        }
        QValue qvs;
        if (targetType == PropertyType.UNDEFINED) {
            qvs = ValueFormat.getQValue(value, session.getNamePathResolver(), session.getQValueFactory());
            targetType = qvs.getType();
        } else {
            Value targetValue = ValueHelper.convert(value, targetType, session.getValueFactory());
            qvs = ValueFormat.getQValue(targetValue, session.getNamePathResolver(), session.getQValueFactory());
        }
        return createProperty(qName, targetType, def, new QValue[] {qvs});
    }

    /**
     * Create a new multi valued property
     *
     * @param qName
     * @param type
     * @param values
     * @return
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    private Property createProperty(Name qName, Value[] values, int type)
        throws ConstraintViolationException, RepositoryException {
        QPropertyDefinition def = getApplicablePropertyDefinition(qName, type, true);
        int targetType = def.getRequiredType();
        // make sure, the final type is not set to undefined
        if (targetType == PropertyType.UNDEFINED) {
            if (type == PropertyType.UNDEFINED) {
                // try to retrieve type from the values array
                if (values.length > 0) {
                    for (Value value : values) {
                        if (value != null) {
                            targetType = value.getType();
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
        QValue[] qvs = ValueFormat.getQValues(targetValues, session.getNamePathResolver(), session.getQValueFactory());
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
    private Property createProperty(Name qName, int type, QPropertyDefinition def,
                                    QValue[] qvs)
        throws ConstraintViolationException, RepositoryException {
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
    private Name getQName(String jcrName) throws RepositoryException {
        Name qName;
        try {
            qName = session.getNameResolver().getQName(jcrName);
        } catch (NameException upe) {
            throw new RepositoryException("invalid name: "+ jcrName, upe);
        }
        return qName;
    }

    /**
     * @return the primary node type name.
     */
    private Name getPrimaryNodeTypeName() {
        return getNodeState().getNodeTypeName();
    }

    /**
     *
     * @param name
     * @param weak
     * @return
     * @throws RepositoryException
     */
    private LazyItemIterator getReferences(String name, boolean weak) throws RepositoryException {
        checkStatus();
        Name propName = (name == null) ? null : getQName(name);
        Iterator<PropertyId> itr = getNodeState().getNodeReferences(propName, weak);
        return new LazyItemIterator(getItemManager(), session.getHierarchyManager(), itr);
    }

    /**
     *
     * @param mixinName
     * @return
     * @throws NoSuchNodeTypeException
     * @throws ConstraintViolationException
     */
    private boolean canAddMixin(Name mixinName) throws NoSuchNodeTypeException,
        ConstraintViolationException {
        NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();

        // first check characteristics of each mixin
        NodeType mixin = ntMgr.getNodeType(mixinName);
        if (!mixin.isMixin()) {
            log.error(mixin.getName() + ": not a mixin node type");
            return false;
        }

        // get list of existing nodetypes
        Name[] existingNts = getNodeState().getNodeTypeNames();
        // build effective node type representing primary type including existing mixins
        EffectiveNodeType entExisting = session.getEffectiveNodeTypeProvider().getEffectiveNodeType(existingNts);

        // check if the base type supports adding this mixin
        if (!entExisting.supportsMixin(mixinName)) {
            log.debug(mixin.getName() + ": not supported on node type " + getPrimaryNodeTypeName());
            return false;
        }

        // second, build new effective node type for nts including the new mixin
        // types, detecting eventual incompatibilities
        Name[] resultingNts = new Name[existingNts.length + 1];
        System.arraycopy(existingNts, 0, resultingNts, 0, existingNts.length);
        resultingNts[existingNts.length] = mixinName;
        session.getEffectiveNodeTypeProvider().getEffectiveNodeType(resultingNts);

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
     * @return <code>NodeEntry</code> of this <code>Node</code>
     */
    private NodeEntry getNodeEntry() {
        return (NodeEntry) getItemState().getHierarchyEntry();
    }

    /**
     *
     * @param relativePath
     * @return
     * @throws RepositoryException
     */
    private Path getReorderPath(String relativePath) throws RepositoryException {
        try {
            Path p = session.getPathResolver().getQPath(relativePath);
            if (p.isAbsolute() || p.getLength() != 1 || p.getDepth() != 1) {
                throw new RepositoryException("Invalid relative path: " + relativePath);
            }
            return p;
        } catch (NameException e) {
            String msg = "Invalid relative path: " + relativePath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @param relativeJcrPath
     * @return Path object for the specified relative JCR path string.
     * @throws RepositoryException
     */
    private Path getPath(String relativeJcrPath) throws RepositoryException {
        try {
            Path p = session.getPathResolver().getQPath(relativeJcrPath);
            return getPath(p);
        } catch (NameException e) {
            String msg = "Invalid relative path: " + relativeJcrPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @param relativePath
     * @return normalized absolute path calculated from the given relative
     * path and the path of this node.
     * @throws RepositoryException
     */
    private Path getPath(Path relativePath) throws RepositoryException {
        // shortcut
        if (relativePath.getLength() == 1 && relativePath.denotesCurrent()) {
            return getQPath();
        }
        return session.getPathFactory().create(getQPath(), relativePath, true);
    }

    /**
     * Returns the <code>NodeEntry</code> at <code>relPath</code> or
     * <code>null</code> if no node exists at <code>relPath</code>.
     * <p>
     * Note that access rights are not checked.
     *
     * @param relPath relative path of a (possible) node.
     * @return the HierarchyEntry of the node at <code>relPath</code> or
     * <code>null</code> if no node exists at <code>relPath</code>.
     * @throws RepositoryException if <code>relPath</code> is not a valid
     * relative path.
     */
    private NodeEntry resolveRelativeNodePath(String relPath) throws RepositoryException {
        NodeEntry targetEntry = null;
        try {
            Path rp = session.getPathResolver().getQPath(relPath);
            // shortcut
            if (rp.getLength() == 1) {
                if (rp.denotesCurrent()) {
                    targetEntry = getNodeEntry();
                } else if (rp.denotesParent()) {
                    targetEntry = getNodeEntry().getParent();
                } else {
                    // try to get child entry + force loading of not known yet
                    targetEntry = getNodeEntry().getNodeEntry(
                            rp.getName(), rp.getNormalizedIndex(), true);
                }
            } else {
                // rp length > 1
                Path p = getPath(rp);
                targetEntry = session.getHierarchyManager().getNodeEntry(p.getCanonicalPath());
            }
        } catch (PathNotFoundException e) {
            // item does not exist -> ignore and return null
        } catch (NameException e) {
            String msg = "Invalid relative path: " + relPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        return targetEntry;
    }

    /**
     * Returns the id of the property at <code>relPath</code> or <code>null</code>
     * if no property exists at <code>relPath</code>.
     * <p>
     * Note that access rights are not checked.
     *
     * @param relPath relative path of a (possible) property
     * @return the PropertyEntry of the property at <code>relPath</code> or
     * <code>null</code> if no property exists at <code>relPath</code>
     * @throws RepositoryException if <code>relPath</code> is not a valid
     * relative path
     */
    private PropertyEntry resolveRelativePropertyPath(String relPath) throws RepositoryException {
        PropertyEntry targetEntry = null;
        try {
            Path rp = session.getPathResolver().getQPath(relPath);
            if (rp.getLength() == 1 && rp.denotesName()) {
                // a single path element must always denote a name. '.' and '..'
                // will never point to a property. If the NodeEntry does not
                // contain such a property entry, the targetEntry is 'null;
                Name propName = rp.getName();
                // check if property entry exists
                targetEntry = getNodeEntry().getPropertyEntry(propName, true);
            } else {
                // build and resolve absolute path
                Path p = getPath(rp).getCanonicalPath();
                try {
                    targetEntry = session.getHierarchyManager().getPropertyEntry(p);
                } catch (PathNotFoundException e) {
                    // ignore -> return null;
                }
            }
        } catch (NameException e) {
            String msg = "failed to resolve property path " + relPath + " relative to " + safeGetJCRPath();
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        return targetEntry;
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
    private QPropertyDefinition getApplicablePropertyDefinition(Name propertyName,
                                                                int type,
                                                                boolean multiValued)
            throws ConstraintViolationException, RepositoryException {
        return session.getItemDefinitionProvider().getQPropertyDefinition(getNodeState().getAllNodeTypeNames(), propertyName, type, multiValued);
    }
}
