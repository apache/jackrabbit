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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.jcr2spi.security.AccessManager;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.PathNotFoundException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.NamespaceResolver;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * Utility class for validating an item state against constraints
 * specified by its definition.
 */
public class ItemStateValidator {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(ItemStateValidator.class);

    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveItem}</code> methods:<p/>
     * check access rights
     */
    public static final int CHECK_ACCESS = 1;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveItem}</code> methods:<p/>
     * check lock status
     */
    public static final int CHECK_LOCK = 2;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveItem}</code> methods:<p/>
     * check checked-out status
     */
    public static final int CHECK_VERSIONING = 4;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveItem}</code> methods:<p/>
     * check constraints defined in node type
     */
    public static final int CHECK_CONSTRAINTS = 8;

    /**
     * option for <code>{@link #checkRemoveItem}</code> method:<p/>
     * check that target node is not being referenced
     */
    public static final int CHECK_COLLISION = 32;

    public static final int CHECK_NONE = 0;
    public static final int CHECK_ALL = CHECK_ACCESS | CHECK_LOCK | CHECK_VERSIONING | CHECK_CONSTRAINTS | CHECK_COLLISION;

    /**
     * node type registry
     */
    private final NodeTypeRegistry ntReg;

    /**
     * manager provider
     */
    private final ManagerProvider mgrProvider;

    /**
     * Creates a new <code>ItemStateValidator</code> instance.
     *
     * @param ntReg      node type registry
     * @param mgrProvider manager provider
     */
    public ItemStateValidator(NodeTypeRegistry ntReg, ManagerProvider mgrProvider) {
        this.ntReg = ntReg;
        this.mgrProvider = mgrProvider;
    }

    /**
     * Checks whether the given node state satisfies the constraints specified
     * by its primary and mixin node types. The following validations/checks are
     * performed:
     * <ul>
     * <li>check if its node type satisfies the 'required node types' constraint
     * specified in its definition</li>
     * <li>check if all 'mandatory' child items exist</li>
     * <li>for every property: check if the property value satisfies the
     * value constraints specified in the property's definition</li>
     * </ul>
     *
     * @param nodeState state of node to be validated
     * @throws ConstraintViolationException if any of the validations fail
     * @throws RepositoryException          if another error occurs
     */
    public void validate(NodeState nodeState) throws ConstraintViolationException,
        RepositoryException {
        // effective primary node type
        EffectiveNodeType entPrimary = ntReg.getEffectiveNodeType(nodeState.getNodeTypeName());
        // effective node type (primary type incl. mixins)
        EffectiveNodeType entPrimaryAndMixins = getEffectiveNodeType(nodeState);
        QNodeDefinition def = nodeState.getDefinition();

        // check if primary type satisfies the 'required node types' constraint
        QName[] requiredPrimaryTypes = def.getRequiredPrimaryTypes();
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            if (!entPrimary.includesNodeType(requiredPrimaryTypes[i])) {
                String msg = safeGetJCRPath(nodeState)
                        + ": missing required primary type "
                        + requiredPrimaryTypes[i];
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        // mandatory properties
        QPropertyDefinition[] pda = entPrimaryAndMixins.getMandatoryPropDefs();
        for (int i = 0; i < pda.length; i++) {
            QPropertyDefinition pd = pda[i];
            if (!nodeState.hasPropertyName(pd.getQName())) {
                String msg = safeGetJCRPath(nodeState)
                        + ": mandatory property " + pd.getQName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        // mandatory child nodes
        QNodeDefinition[] cnda = entPrimaryAndMixins.getMandatoryNodeDefs();
        for (int i = 0; i < cnda.length; i++) {
            QNodeDefinition cnd = cnda[i];
            if (!nodeState.getNodeEntry().hasNodeEntry(cnd.getQName())) {
                String msg = safeGetJCRPath(nodeState)
                        + ": mandatory child node " + cnd.getQName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
    }

    //-------------------------------------------------< misc. helper methods >
    /**
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node's primary and mixin
     * node types.
     *
     * @param nodeState
     * @return the effective node type
     * @throws RepositoryException
     */
    public EffectiveNodeType getEffectiveNodeType(NodeState nodeState)
            throws RepositoryException {
        try {
            QName[] allNtNames;
            if (nodeState.getStatus() == Status.EXISTING) {
                allNtNames = nodeState.getNodeTypeNames();
            } else {
                // TODO: check if correct (and only used for creating new)
                QName primaryType = nodeState.getNodeTypeName();
                allNtNames = new QName[] { primaryType }; // default
                PropertyEntry mixins = nodeState.getNodeEntry().getPropertyEntry(QName.JCR_MIXINTYPES);
                if (mixins != null) {
                    try {
                        QValue[] values = mixins.getPropertyState().getValues();
                        allNtNames = new QName[values.length + 1];
                        for (int i = 0; i < values.length; i++) {
                            allNtNames[i] = values[i].getQName();
                        }
                        allNtNames[values.length] = primaryType;
                    } catch (ItemStateException e) {
                        // ignore
                    }
                }
            }

            return getEffectiveNodeType(allNtNames);
        } catch (NodeTypeConflictException ntce) {
            String msg = "Internal error: failed to build effective node type from node types defined with " + safeGetJCRPath(nodeState);
            log.debug(msg);
            throw new RepositoryException(msg, ntce);
        }
    }

    /**
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node types.
     *
     * @param nodeTypeNames
     * @return the effective node type
     * @throws NodeTypeConflictException
     * @throws NoSuchNodeTypeException
     */
    public EffectiveNodeType getEffectiveNodeType(QName[] nodeTypeNames)
            throws NodeTypeConflictException, NoSuchNodeTypeException  {
        return ntReg.getEffectiveNodeType(nodeTypeNames);
    }

    /**
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node type.
     *
     * @param nodeTypeName
     * @return the effective node type
     * @throws NoSuchNodeTypeException
     */
    public EffectiveNodeType getEffectiveNodeType(QName nodeTypeName) throws NoSuchNodeTypeException  {
        return ntReg.getEffectiveNodeType(nodeTypeName);
    }

    /**
     * Failsafe translation of internal <code>ItemState</code> to JCR path for use
     * in error messages etc.
     *
     * @param itemState
     * @return JCR path
     * @see LogUtil#safeGetJCRPath(ItemState,NamespaceResolver)
     */
    private String safeGetJCRPath(ItemState itemState) {
        return LogUtil.safeGetJCRPath(itemState, mgrProvider.getNamespaceResolver());
    }

    /**
     * Helper method that finds the applicable definition for a child node with
     * the given name and node type in the parent node's node type and
     * mixin types.
     *
     * @param name
     * @param nodeTypeName
     * @param parentState
     * @return a <code>QNodeDefinition</code>
     *
     * @throws ConstraintViolationException if no applicable child node definition
     * could be found
     * @throws NoSuchNodeTypeException if the given nodeTypeName does not exist.
     * @throws RepositoryException if another error occurs
     */
    public QNodeDefinition getApplicableNodeDefinition(QName name,
                                                       QName nodeTypeName,
                                                       NodeState parentState)
        throws NoSuchNodeTypeException, ConstraintViolationException, RepositoryException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicableNodeDefinition(name, nodeTypeName, ntReg);
    }

    /**
     * Helper method that finds the applicable definition for a property with
     * the given name, type and multiValued characteristic in the parent node's
     * node type and mixin types. If there more than one applicable definitions
     * then the following rules are applied:
     * <ul>
     * <li>named definitions are preferred to residual definitions</li>
     * <li>definitions with specific required type are preferred to definitions
     * with required type UNDEFINED</li>
     * </ul>
     *
     * @param name
     * @param type
     * @param multiValued
     * @param parentState
     * @return a <code>QPropertyDefinition</code>
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    public QPropertyDefinition getApplicablePropertyDefinition(QName name,
                                                               int type,
                                                               boolean multiValued,
                                                               NodeState parentState)
        throws ConstraintViolationException, RepositoryException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicablePropertyDefinition(name, type, multiValued);
    }

    /**
     * Helper method that finds the applicable definition for a property with
     * the given name, type in the parent node's node type and mixin types.
     * Other than <code>{@link #getApplicablePropertyDefinition(QName, int, boolean, NodeState)}</code>
     * this method does not take the multiValued flag into account in the
     * selection algorithm. If there more than one applicable definitions then
     * the following rules are applied:
     * <ul>
     * <li>named definitions are preferred to residual definitions</li>
     * <li>definitions with specific required type are preferred to definitions
     * with required type UNDEFINED</li>
     * <li>single-value definitions are preferred to multiple-value definitions</li>
     * </ul>
     *
     * @param name
     * @param type
     * @param parentState
     * @return a <code>QPropertyDefinition</code>
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    public QPropertyDefinition getApplicablePropertyDefinition(QName name,
                                                               int type,
                                                               NodeState parentState)
        throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicablePropertyDefinition(name, type);
    }

    //------------------------------------------------------< check methods >---
    /**
     *
     * @param parentState
     * @param options
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ItemExistsException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public void checkIsWritable(NodeState parentState, int options) throws VersionException,
        LockException, ItemNotFoundException, ItemExistsException, PathNotFoundException, RepositoryException {

        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            // make sure current session is granted read access on parent node
            if (!mgrProvider.getAccessManager().canRead(parentState)) {
                throw new ItemNotFoundException(safeGetJCRPath(parentState));
            }
        }
        // make sure there's no foreign lock on parent node
        if ((options & CHECK_LOCK) == CHECK_LOCK) {
            checkLock(parentState);
        }
        // make sure parent node is checked-out
        if ((options & CHECK_VERSIONING) == CHECK_VERSIONING) {
            checkIsCheckedOut(parentState);
        }
        // constraints
        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            // make sure parent node is not protected
            checkProtection(parentState);
        }
    }

    /**
     *
     * @param propState
     * @param options bit-wise OR'ed flags specifying the checks that should be
     * performed; any combination of the following constants:
     * <ul>
     * <li><code>{@link #CHECK_ACCESS}</code>: make sure current session is
     * granted read access on parent node and can add a child node with the
     * given name.</li>
     * <li><code>{@link #CHECK_LOCK}</code>: make sure there's no foreign lock
     * on parent node</li>
     * <li><code>{@link #CHECK_VERSIONING}</code>: make sure parent node is
     * checked-out</li>
     * <li><code>{@link #CHECK_CONSTRAINTS}</code>: make sure no node type
     * constraints would be violated</li>
     * <li><code>{@link #CHECK_COLLISION}</code>: check for collision with
     * existing properties or nodes</li>
     * </ul>
     *
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ItemExistsException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public void checkSetProperty(PropertyState propState, int options)
        throws ConstraintViolationException, AccessDeniedException,
        VersionException, LockException, ItemNotFoundException,
        ItemExistsException, PathNotFoundException, RepositoryException {

        try {
            NodeState parent = propState.getParent();
            QPropertyDefinition def = propState.getDefinition();
            checkWriteProperty(parent, propState.getQName(), def, options);
        } catch (NoSuchItemStateException e) {
            throw new ItemNotFoundException(e);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     *
     * @param parentState
     * @param propertyName
     * @param options bit-wise OR'ed flags specifying the checks that should be
     * performed; any combination of the following constants:
     * <ul>
     * <li><code>{@link #CHECK_ACCESS}</code>: make sure current session is
     * granted read access on parent node and can add a child node with the
     * given name.</li>
     * <li><code>{@link #CHECK_LOCK}</code>: make sure there's no foreign lock
     * on parent node</li>
     * <li><code>{@link #CHECK_VERSIONING}</code>: make sure parent node is
     * checked-out</li>
     * <li><code>{@link #CHECK_CONSTRAINTS}</code>: make sure no node type
     * constraints would be violated</li>
     * <li><code>{@link #CHECK_COLLISION}</code>: check for collision with
     * existing properties or nodes</li>
     * </ul>
     *
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ItemExistsException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public void checkAddProperty(NodeState parentState, QName propertyName, QPropertyDefinition definition, int options)
        throws ConstraintViolationException, AccessDeniedException,
        VersionException, LockException, ItemNotFoundException,
        ItemExistsException, PathNotFoundException, RepositoryException {

        checkWriteProperty(parentState, propertyName, definition, options);
    }

    /**
     * 
     * @param parentState
     * @param propertyName
     * @param definition
     * @param options
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ItemExistsException
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    private void checkWriteProperty(NodeState parentState, QName propertyName, QPropertyDefinition definition, int options)
        throws ConstraintViolationException, AccessDeniedException,
        VersionException, LockException, ItemNotFoundException,
        ItemExistsException, PathNotFoundException, RepositoryException {

        checkIsWritable(parentState, options);

        // access restriction on prop.
        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            // make sure current session is granted write access on new prop
            Path relPath = Path.create(propertyName, Path.INDEX_UNDEFINED);
            if (!mgrProvider.getAccessManager().isGranted(parentState, relPath, new String[] {AccessManager.SET_PROPERTY_ACTION})) {
                throw new AccessDeniedException(safeGetJCRPath(parentState) + ": not allowed to create property with name " + propertyName);
            }
        }
        // constraints on property
        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            // if definition is available check if prop-def is not protected either.
            checkProtection(definition);
        }
        // collisions
        if ((options & CHECK_COLLISION) == CHECK_COLLISION) {
            checkCollision(parentState, propertyName);
        }
    }

    /**
     * Checks if adding a child node called <code>nodeName</code> of node type
     * <code>nodeTypeName</code> to the given parent node is allowed in the
     * current context.
     *
     * @param parentState
     * @param nodeName
     * @param nodeTypeName
     * @param options      bit-wise OR'ed flags specifying the checks that should be
     *                     performed; any combination of the following constants:
     * <ul>
     * <li><code>{@link #CHECK_ACCESS}</code>: make sure current session is
     * granted read access on parent node and can add a child node with the
     * given name.</li>
     * <li><code>{@link #CHECK_LOCK}</code>: make sure there's no foreign lock
     * on parent node</li>
     * <li><code>{@link #CHECK_VERSIONING}</code>: make sure parent node is
     * checked-out</li>
     * <li><code>{@link #CHECK_CONSTRAINTS}</code>: make sure no node type
     * constraints would be violated</li>
     * <li><code>{@link #CHECK_COLLISION}</code>: check for collision with
     * existing properties or nodes</li>
     * </ul>
     *
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

        checkIsWritable(parentState, options);

        // access restrictions on new node
        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            // make sure current session is granted write access on parent node
            Path relPath = Path.create(nodeName, Path.INDEX_UNDEFINED);
            if (!mgrProvider.getAccessManager().isGranted(parentState, relPath, new String[] {AccessManager.ADD_NODE_ACTION})) {
                throw new AccessDeniedException(safeGetJCRPath(parentState) + ": not allowed to add child node '" + nodeName +"'");
            }
        }
        // node type constraints
        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            // make sure there's an applicable definition for new child node
            EffectiveNodeType entParent = getEffectiveNodeType(parentState);
            entParent.checkAddNodeConstraints(nodeName, nodeTypeName, ntReg);
        }
        // collisions
        if ((options & CHECK_COLLISION) == CHECK_COLLISION) {
            checkCollision(parentState, nodeName, nodeTypeName);
        }
    }

    /**
     * Checks if removing the given target state is allowed in the current context.
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
     *                    </ul>
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws VersionException
     * @throws LockException
     * @throws ItemNotFoundException
     * @throws ReferentialIntegrityException
     * @throws RepositoryException
     */
    public void checkRemoveItem(ItemState targetState, int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ReferentialIntegrityException, RepositoryException {

        // TODO: missing check if all affected child-states can be removed as well
        if (targetState.isNode() && ((NodeState)targetState).isRoot()) {
            // root node
            throw new ConstraintViolationException("Cannot remove root node.");
        }
        // check parent
        try {
            checkIsWritable(targetState.getParent(), options);
        } catch (NoSuchItemStateException e) {
            throw new ItemNotFoundException(e);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }

        // access rights
        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            try {
                // make sure current session is allowed to remove target node
                if (!mgrProvider.getAccessManager().canRemove(targetState)) {
                    throw new AccessDeniedException(safeGetJCRPath(targetState) + ": not allowed to remove node");
                }
            } catch (ItemNotFoundException infe) {
                String msg = "internal error: failed to check access rights for " + safeGetJCRPath(targetState);
                log.debug(msg);
                throw new RepositoryException(msg, infe);
            }
        }

        // constraints given from the target
        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            // check if target not protected and not mandatory
            checkRemoveConstraints(targetState);
        }
    }

    /**
     * Verifies that the item represented by the given state is checked-out;
     * throws a <code>VersionException</code> if that's not the case.
     * <p/>
     * A node is considered <i>checked-out</i> if it is versionable and
     * checked-out, or is non-versionable but its nearest versionable ancestor
     * is checked-out, or is non-versionable and there are no versionable
     * ancestors.
     *
     * @param itemState state to check
     * @throws PathNotFoundException
     * @throws VersionException
     * @throws RepositoryException
     */
    private void checkIsCheckedOut(ItemState itemState)
            throws PathNotFoundException, VersionException, RepositoryException {
        try {
            NodeState nodeState = (itemState.isNode()) ? (NodeState)itemState : itemState.getParent();
            mgrProvider.getVersionManager().checkIsCheckedOut(nodeState);
        } catch (NoSuchItemStateException e) {
            throw new ItemNotFoundException(e);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Verifies that the given item state is not locked by
     * somebody else than the current session.
     *
     * @param itemState state to be checked
     * @throws PathNotFoundException
     * @throws LockException         if write access to the specified path is not allowed
     * @throws RepositoryException   if another error occurs
     */
    private void checkLock(ItemState itemState) throws LockException, RepositoryException {
        try {
            // make sure there's no foreign lock present the node (or the parent node
            // in case the state represents a PropertyState).
            NodeState nodeState = (itemState.isNode()) ? ((NodeState)itemState) : itemState.getParent();
            mgrProvider.getLockManager().checkLock(nodeState);
        } catch (NoSuchItemStateException e) {
            throw new ItemNotFoundException(e);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Checks if the definition of the given item state indicates a protected
     * status.
     *
     * @param itemState
     * @throws ConstraintViolationException If the definition of the given
     * item state indicates that the state is protected.
     * @see QItemDefinition#isProtected()
     */
    private void checkProtection(ItemState itemState)
        throws ConstraintViolationException, RepositoryException {
        QItemDefinition def;
        if (itemState.isNode()) {
            def = ((NodeState)itemState).getDefinition();
        } else {
            def = ((PropertyState)itemState).getDefinition();
        }
        checkProtection(def);
    }

    /**
     * Checks if the given {@link QItemDefinition#isProtected()} is true.
     *
     * @param definition
     * @throws ConstraintViolationException If {@link QItemDefinition#isProtected()}
     * returns true.
     */
    private void checkProtection(QItemDefinition definition) throws ConstraintViolationException {
        if (definition.isProtected()) {
            throw new ConstraintViolationException("Item is protected");
        }
    }

    /**
     * An item state cannot be removed if it is either protected or mandatory.
     *
     * @param itemState
     * @throws ConstraintViolationException
     * @see #checkProtection(ItemState)
     */
    private void checkRemoveConstraints(ItemState itemState)
        throws ConstraintViolationException, RepositoryException {
        QItemDefinition definition;
        if (itemState.isNode()) {
            definition = ((NodeState)itemState).getDefinition();
        } else {
            definition = ((PropertyState)itemState).getDefinition();
        }
        checkProtection(definition);
        if (definition.isMandatory()) {
            throw new ConstraintViolationException("Cannot remove mandatory item");
        }
    }

    /**
     *
     * @param parentState
     * @param propertyName
     * @throws ItemExistsException
     * @throws RepositoryException
     */
    private void checkCollision(NodeState parentState, QName propertyName) throws ItemExistsException, RepositoryException {
        NodeEntry parentEntry = (NodeEntry) parentState.getHierarchyEntry();
        // check for name collisions with existing child nodes
        if (parentEntry.hasNodeEntry(propertyName)) {
            String msg = "Child node with name '" + propertyName + "' already exists.";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // check for name collisions with existing properties
        PropertyEntry pe = parentEntry.getPropertyEntry(propertyName);
        if (pe != null) {
            try {
                pe.getPropertyState();
            } catch (ItemStateException e) {
                // should not occur. existance has been asserted before
                throw new RepositoryException(e);
            }
            throw new ItemExistsException("Property '" + pe.getQName() + "' already exists.");
        }
    }

    /**
     *
     * @param parentState
     * @param nodeName
     * @param nodeTypeName
     * @throws RepositoryException
     * @throws ConstraintViolationException
     * @throws NoSuchNodeTypeException
     */
    private void checkCollision(NodeState parentState, QName nodeName, QName nodeTypeName) throws RepositoryException, ConstraintViolationException, NoSuchNodeTypeException {
        if (parentState.hasPropertyName(nodeName)) {
            // there's already a property with that name
            throw new ItemExistsException("cannot add child node '"
                + nodeName.getLocalName() + "' to " + safeGetJCRPath(parentState)
                + ": colliding with same-named existing property");

        } else if (parentState.hasChildNodeEntry(nodeName, Path.INDEX_DEFAULT)) {
            // retrieve the existing node state that ev. conflicts with the new one.
            try {
                NodeState conflictingState = parentState.getChildNodeState(nodeName, Path.INDEX_DEFAULT);
                QNodeDefinition conflictDef = conflictingState.getDefinition();
                QNodeDefinition newDef = getApplicableNodeDefinition(nodeName, nodeTypeName, parentState);

                // check same-name sibling setting of both target and existing node
                if (!(conflictDef.allowsSameNameSiblings() && newDef.allowsSameNameSiblings())) {
                    throw new ItemExistsException("Cannot add child node '"
                        + nodeName.getLocalName() + "' to "
                        + safeGetJCRPath(parentState)
                        + ": colliding with same-named existing node.");
                }
            } catch (NoSuchItemStateException e) {
                // ignore: conflicting doesn't exist any more
            } catch (ItemStateException e) {
                // should not occur, since existence has been asserted before
                throw new RepositoryException(e);
            }
        }
    }
}
