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
import org.apache.jackrabbit.jcr2spi.nodetype.ValueConstraint;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.jcr2spi.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.security.AccessManager;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.PropertyType;
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
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.value.QValue;

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
    public static final int CHECK_REFERENCES = 16;

    /**
     * option for <code>{@link #checkRemoveItem}</code> method:<p/>
     * check that target node is not being referenced
     */
    public static final int CHECK_COLLISION = 32;

    public static final int CHECK_ALL = CHECK_ACCESS | CHECK_LOCK | CHECK_VERSIONING | CHECK_CONSTRAINTS | CHECK_COLLISION | CHECK_REFERENCES;

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
    public void validate(NodeState nodeState)
            throws ConstraintViolationException, RepositoryException {
        // effective primary node type
        EffectiveNodeType entPrimary = ntReg.getEffectiveNodeType(nodeState.getNodeTypeName());
        // effective node type (primary type incl. mixins)
        EffectiveNodeType entPrimaryAndMixins = getEffectiveNodeType(nodeState);
        QNodeDefinition def = nodeState.getDefinition();

        // check if primary type satisfies the 'required node types' constraint
        QName[] requiredPrimaryTypes = def.getRequiredPrimaryTypes();
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            if (!entPrimary.includesNodeType(requiredPrimaryTypes[i])) {
                String msg = safeGetJCRPath(nodeState.getNodeId())
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
                String msg = safeGetJCRPath(nodeState.getNodeId())
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
            if (!nodeState.hasChildNodeEntry(cnd.getQName())) {
                String msg = safeGetJCRPath(nodeState.getNodeId())
                        + ": mandatory child node " + cnd.getQName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
    }

    /**
     * Checks whether the given property parameters are consistent and satisfy
     * the constraints specified by the given definition. The following
     * validations/checks are performed:
     * <ul>
     * <li>make sure the type is not undefined and matches the type of all
     * values given</li>
     * <li>make sure all values have the same type.</li>
     * <li>check if the type of the property values does comply with the
     * requiredType specified in the property's definition</li>
     * <li>check if the property values satisfy the value constraints
     * specified in the property's definition</li>
     * </ul>
     *
     * @param propertyType
     * @param values
     * @param definition
     * @throws ConstraintViolationException If any of the validations fails.
     * @throws RepositoryException If another error occurs.
     */
    public void validate(int propertyType, QValue[] values, QPropertyDefinition definition)
        throws ConstraintViolationException, RepositoryException {
        if (propertyType == PropertyType.UNDEFINED) {
            throw new RepositoryException("'Undefined' is not a valid property type for existing values.");
        }
        if (definition.getRequiredType() != PropertyType.UNDEFINED && definition.getRequiredType() != propertyType) {
            throw new ConstraintViolationException("RequiredType constraint is not satisfied");
        }
        for (int i = 0; i < values.length; i++) {
            if (propertyType != values[i].getType()) {
                throw new ConstraintViolationException("Inconsistent value types: Required type = " + PropertyType.nameFromValue(propertyType) + "; Found value with type = " + PropertyType.nameFromValue(values[i].getType()));
            }
        }
        ValueConstraint.checkValueConstraints(definition, values);
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
            return getEffectiveNodeType(nodeState.getNodeTypeNames());
        } catch (NodeTypeConflictException ntce) {
            String msg = "Internal error: failed to build effective node type from node types defined with " + safeGetJCRPath(nodeState.getId());
            log.debug(msg);
            throw new RepositoryException(msg, ntce);
        }
    }

    /**
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node's primary and mixin
     * node types.
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
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @param path path to convert
     * @return JCR path
     */
    public String safeGetJCRPath(Path path) {
        return mgrProvider.getHierarchyManager().safeGetJCRPath(path);
    }

    /**
     * Failsafe translation of internal <code>ItemId</code> to JCR path for use
     * in error messages etc.
     *
     * @param id id to translate
     * @return JCR path
     * @see HierarchyManager#safeGetJCRPath(ItemId)
     */
    public String safeGetJCRPath(ItemId id) {
        return mgrProvider.getHierarchyManager().safeGetJCRPath(id);
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
        return entParent.getApplicableNodeDefinition(name, nodeTypeName);
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
    public void checkIsWritable(NodeState parentState, int options) throws VersionException,
        LockException, ItemNotFoundException,
        ItemExistsException, PathNotFoundException, RepositoryException {

        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            // make sure current session is granted read access on parent node
            if (!mgrProvider.getAccessManager().canRead(parentState.getNodeId())) {
                throw new ItemNotFoundException(safeGetJCRPath(parentState.getNodeId()));
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

        checkWriteProperty(getNodeState(propState.getParentId()), propState.getName(), propState.getDefinition(), options);
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

    public void checkWriteProperty(NodeState parentState, QName propertyName, QPropertyDefinition definition, int options)
        throws ConstraintViolationException, AccessDeniedException,
        VersionException, LockException, ItemNotFoundException,
        ItemExistsException, PathNotFoundException, RepositoryException {

        checkIsWritable(parentState, options);

        // access restriction on prop.
        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            PropertyId pId = parentState.getPropertyId(propertyName);
            // make sure current session is granted write access on new prop
            if (!mgrProvider.getAccessManager().isGranted(pId, new String[] {AccessManager.SET_PROPERTY_ACTION})) {
                throw new AccessDeniedException(safeGetJCRPath(parentState.getId()) + ": not allowed to create property with name " + propertyName);
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

    // DIFF JR: copied from BatchedItemOperations
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
            // TODO build Id instead 
            Path relPath = Path.create(nodeName, org.apache.jackrabbit.name.Path.INDEX_UNDEFINED);
            if (!mgrProvider.getAccessManager().isGranted(parentState.getNodeId(), relPath, new String[] {AccessManager.ADD_NODE_ACTION})) {
                throw new AccessDeniedException(safeGetJCRPath(parentState.getNodeId()) + ": not allowed to add child node '" + nodeName +"'");
            }
        }

        // node type constraints
        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            // make sure there's an applicable definition for new child node
            EffectiveNodeType entParent = getEffectiveNodeType(parentState);
            entParent.checkAddNodeConstraints(nodeName, nodeTypeName);
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
    public void checkRemoveItem(ItemState targetState, int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ReferentialIntegrityException, RepositoryException {

        // TODO: missing check if all affected child-states can be removed as well
        // NOTE: referencial integrity should be asserted for all child-nodes.

        ItemId targetId = targetState.getId();
        NodeId parentId = targetState.getParentId();
        if (parentId == null) {
            // root or orphaned node
            throw new ConstraintViolationException("Cannot remove root node");
        }

        // check parent
        checkIsWritable(getNodeState(parentId), options);

        // access rights
        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            try {
                // make sure current session is granted read access on parent node
                if (!mgrProvider.getAccessManager().canRead(targetId)) {
                    throw new PathNotFoundException(safeGetJCRPath(targetId));
                }
                // make sure current session is allowed to remove target node
                if (!mgrProvider.getAccessManager().canRemove(targetId)) {
                    throw new AccessDeniedException(safeGetJCRPath(targetId)
                            + ": not allowed to remove node");
                }
            } catch (ItemNotFoundException infe) {
                String msg = "internal error: failed to check access rights for "
                        + safeGetJCRPath(targetId);
                log.debug(msg);
                throw new RepositoryException(msg, infe);
            }
        }

        // constraints given from the target
        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            // check if target not protected and not mandatory
            checkRemoveConstraints(targetState);
        }

        // check referential integrity of state to be deleted
        if ((options & CHECK_REFERENCES) == CHECK_REFERENCES) {
            checkReferences(targetState);
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
        // shortcut: if state is new, its ancestor must be checkout
        if (itemState.getStatus() == ItemState.STATUS_NEW) {
            return;
        }
        NodeState nodeState = (itemState.isNode()) ? (NodeState)itemState : getNodeState(itemState.getParentId());
        if (!mgrProvider.getVersionManager().isCheckedOut(nodeState.getNodeId())) {
            throw new VersionException(safeGetJCRPath(nodeState.getNodeId()) + " is checked-in");
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
    private void checkLock(ItemState itemState)
            throws LockException, RepositoryException {
        // make sure there's no foreign lock present the node (or the parent node
        // for properties.
        NodeId nodeId = (itemState.isNode()) ? ((NodeState)itemState).getNodeId() : itemState.getParentId();
        mgrProvider.getLockManager().checkLock(nodeId);
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
    private void checkProtection(ItemState itemState) throws ConstraintViolationException {
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
    private void checkRemoveConstraints(ItemState itemState) throws ConstraintViolationException {
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
        // check for name collisions with existing child nodes
        if (parentState.hasChildNodeEntry(propertyName)) {
            String msg = "there's already a child node with name " + propertyName;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        // check for name collisions with existing properties
        if (parentState.hasPropertyName(propertyName)) {
            PropertyId errorId = parentState.getPropertyId(propertyName);
            throw new ItemExistsException(safeGetJCRPath(errorId));
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
                + nodeName.getLocalName() + "' to " + safeGetJCRPath(parentState.getNodeId())
                + ": colliding with same-named existing property");

        } else if (parentState.hasChildNodeEntry(nodeName)) {
            // retrieve the existing node state that ev. conflicts with the new one.
            ChildNodeEntry entry = parentState.getChildNodeEntry(nodeName, 1);
            NodeState conflictingState = getNodeState(entry.getId());

            QNodeDefinition conflictDef = conflictingState.getDefinition();
            QNodeDefinition newDef = getApplicableNodeDefinition(nodeName, nodeTypeName, parentState);

            // check same-name sibling setting of both target and existing node
            if (!(conflictDef.allowsSameNameSiblings() && newDef.allowsSameNameSiblings())) {
                throw new ItemExistsException("Cannot add child node '"
                    + nodeName.getLocalName() + "' to "
                    + safeGetJCRPath(parentState.getNodeId())
                    + ": colliding with same-named existing node.");
            }
        }
    }

    /**
     *
     * @param toDelete
     * @throws ReferentialIntegrityException
     * @throws RepositoryException
     */
    private void checkReferences(ItemState toDelete) throws ReferentialIntegrityException, RepositoryException {
        if (!toDelete.isNode()) {
            // PropertyState: nothing to do.
            return;
        }

        NodeState targetState = (NodeState)toDelete;
        NodeId targetId = targetState.getNodeId();
        EffectiveNodeType ent = getEffectiveNodeType(targetState);
        if (ent.includesNodeType(QName.MIX_REFERENCEABLE)) {
            ItemStateManager stateMgr = mgrProvider.getItemStateManager();
            if (stateMgr.hasNodeReferences(targetId)) {
                try {
                    NodeReferences refs = stateMgr.getNodeReferences(targetId);
                    if (refs.hasReferences()) {
                        throw new ReferentialIntegrityException(safeGetJCRPath(targetId)
                            + ": cannot remove node with references");
                    }
                } catch (ItemStateException ise) {
                    String msg = "internal error: failed to check references on " + safeGetJCRPath(targetId);
                    log.error(msg, ise);
                    throw new RepositoryException(msg, ise);
                }
            }
        }
    }

    //--------------------------------------------------------------------------

    public NodeId getNodeId(Path nodePath) throws PathNotFoundException, RepositoryException {
        try {
            ItemId id = mgrProvider.getHierarchyManager().getItemId(nodePath);
            if (!id.denotesNode()) {
                throw new PathNotFoundException(safeGetJCRPath(nodePath));
            }
            return (NodeId)id;
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(safeGetJCRPath(nodePath));
        }
    }

    /**
     * Retrieves the state of the node at the given path.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param nodePath
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public NodeState getNodeState(Path nodePath) throws PathNotFoundException, RepositoryException {
        try {
            ItemId id = mgrProvider.getHierarchyManager().getItemId(nodePath);
            if (!id.denotesNode()) {
                throw new PathNotFoundException(safeGetJCRPath(nodePath));
            }
            return getNodeState((NodeId) id);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(safeGetJCRPath(nodePath));
        }
    }

    /**
     * Retrieves the state of the item with the specified id using the given
     * item state manager.
     * <p/>
     * Note that access rights are <b><i>not</i></b> enforced!
     *
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public NodeState getNodeState(NodeId id) throws ItemNotFoundException, RepositoryException {
        try {
            return (NodeState) mgrProvider.getItemStateManager().getItemState(id);
        } catch (NoSuchItemStateException nsise) {
            throw new ItemNotFoundException(safeGetJCRPath(id));
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to retrieve state of " + safeGetJCRPath(id);
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    public PropertyState getPropertyState(NodeId parentId, QName propertyName) throws ItemNotFoundException, RepositoryException {
        NodeState nState = getNodeState(parentId);
        return getPropertyState(nState.getPropertyId(propertyName));
    }

    public PropertyState getPropertyState(PropertyId id) throws ItemNotFoundException, RepositoryException {
        try {
            return (PropertyState) mgrProvider.getItemStateManager().getItemState(id);
        } catch (NoSuchItemStateException nsise) {
            throw new ItemNotFoundException(safeGetJCRPath(id));
        } catch (ItemStateException ise) {
            String msg = "internal error: failed to retrieve state of " + safeGetJCRPath(id);
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }
}
