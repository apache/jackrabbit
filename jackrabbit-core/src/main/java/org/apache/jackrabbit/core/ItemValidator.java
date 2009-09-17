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

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.lock.LockManager;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.retention.RetentionRegistry;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for validating an item against constraints
 * specified by its definition.
 */
public class ItemValidator {

    /**
     * check access permissions
     */
    public static final int CHECK_ACCESS = 1;

    /**
     * option to check lock status
     */
    public static final int CHECK_LOCK = 2;

    /**
     * option to check checked-out status
     */
    public static final int CHECK_CHECKED_OUT = 4;

    /**
     * check for referential integrity upon removal
     */
    public static final int CHECK_REFERENCES = 8;

    /**
     * option to check if the item is protected by it's nt definition
     */
    public static final int CHECK_CONSTRAINTS = 16;

    /**
     * option to check for pending changes on the session
     */
    public static final int CHECK_PENDING_CHANGES = 32;

    /**
     * option to check for pending changes on the specified node
     */
    public static final int CHECK_PENDING_CHANGES_ON_NODE = 64;

    /**
     * option to check for effective holds
     */
    public static final int CHECK_HOLD = 128;

    /**
     * option to check for effective retention policies
     */
    public static final int CHECK_RETENTION = 256;
    
    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(ItemValidator.class);

    /**
     * node type registry
     */
    protected final NodeTypeRegistry ntReg;

    /**
     * hierarchy manager used for generating error msg's
     * that contain human readable paths
     *
     * @see #safeGetJCRPath(ItemId)
     */
    protected final HierarchyManager hierMgr;

    /**
     * Path resolver for outputting user-friendly error messages.
     */
    protected final PathResolver resolver;

    /**
     *
     */
    protected final LockManager lockMgr;

    protected final AccessManager accessMgr;

    protected final RetentionRegistry retentionReg;

    protected final ItemManager itemMgr;

    /**
     * Creates a new <code>ItemValidator</code> instance.
     *
     * @param ntReg      node type registry
     * @param hierMgr    hierarchy manager
     * @param session    session
     */
    public ItemValidator(NodeTypeRegistry ntReg,
                         HierarchyManager hierMgr,
                         SessionImpl session) throws RepositoryException {
        this(ntReg, hierMgr, session, session.getLockManager(),
                session.getAccessManager(), session.getRetentionRegistry(),
                session.getItemManager());
    }

    /**
     * Creates a new <code>ItemValidator</code> instance.
     *
     * @param ntReg      node type registry
     * @param hierMgr    hierarchy manager
     * @param resolver   resolver
     * @param lockMgr    lockMgr
     * @param accessMgr  accessMgr
     * @param retentionReg
     * @param itemMgr    the item manager
     */
    public ItemValidator(NodeTypeRegistry ntReg,
                         HierarchyManager hierMgr,
                         PathResolver resolver,
                         LockManager lockMgr,
                         AccessManager accessMgr,
                         RetentionRegistry retentionReg,
                         ItemManager itemMgr) {
        this.ntReg = ntReg;
        this.hierMgr = hierMgr;
        this.resolver = resolver;
        this.lockMgr = lockMgr;
        this.accessMgr = accessMgr;
        this.retentionReg = retentionReg;
        this.itemMgr = itemMgr;
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
        EffectiveNodeType entPrimary =
                ntReg.getEffectiveNodeType(nodeState.getNodeTypeName());
        // effective node type (primary type incl. mixins)
        EffectiveNodeType entPrimaryAndMixins = getEffectiveNodeType(nodeState);
        QNodeDefinition def = itemMgr.getDefinition(nodeState).unwrap();

        // check if primary type satisfies the 'required node types' constraint
        Name[] requiredPrimaryTypes = def.getRequiredPrimaryTypes();
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
            if (!nodeState.hasPropertyName(pd.getName())) {
                String msg = safeGetJCRPath(nodeState.getNodeId())
                        + ": mandatory property " + pd.getName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        // mandatory child nodes
        QItemDefinition[] cnda = entPrimaryAndMixins.getMandatoryNodeDefs();
        for (int i = 0; i < cnda.length; i++) {
            QItemDefinition cnd = cnda[i];
            if (!nodeState.hasChildNodeEntry(cnd.getName())) {
                String msg = safeGetJCRPath(nodeState.getNodeId())
                        + ": mandatory child node " + cnd.getName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
    }

    /**
     * Checks whether the given property state satisfies the constraints
     * specified by its definition. The following validations/checks are
     * performed:
     * <ul>
     * <li>check if the type of the property values does comply with the
     * requiredType specified in the property's definition</li>
     * <li>check if the property values satisfy the value constraints
     * specified in the property's definition</li>
     * </ul>
     *
     * @param propState state of property to be validated
     * @throws ConstraintViolationException if any of the validations fail
     * @throws RepositoryException          if another error occurs
     */
    public void validate(PropertyState propState)
            throws ConstraintViolationException, RepositoryException {
        QPropertyDefinition def = itemMgr.getDefinition(propState).unwrap();
        InternalValue[] values = propState.getValues();
        int type = PropertyType.UNDEFINED;
        for (int i = 0; i < values.length; i++) {
            if (type == PropertyType.UNDEFINED) {
                type = values[i].getType();
            } else if (type != values[i].getType()) {
                throw new ConstraintViolationException(safeGetJCRPath(propState.getPropertyId())
                        + ": inconsistent value types");
            }
            if (def.getRequiredType() != PropertyType.UNDEFINED
                    && def.getRequiredType() != type) {
                throw new ConstraintViolationException(safeGetJCRPath(propState.getPropertyId())
                        + ": requiredType constraint is not satisfied");
            }
        }
        EffectiveNodeType.checkSetPropertyValueConstraints(def, values);
    }

    public void checkModify(ItemImpl item, int options, int permissions) throws RepositoryException {
        checkCondition(item, options, permissions, false);
    }

    public void checkRemove(ItemImpl item, int options, int permissions) throws RepositoryException {
        checkCondition(item, options, permissions, true);
    }

    private void checkCondition(ItemImpl item, int options, int permissions, boolean isRemoval) throws RepositoryException {
        if ((options & CHECK_PENDING_CHANGES) == CHECK_PENDING_CHANGES) {
            if (item.getSession().hasPendingChanges()) {
                String msg = "Unable to perform operation. Session has pending changes.";
                log.debug(msg);
                throw new InvalidItemStateException(msg);
            }
        }
        if ((options & CHECK_PENDING_CHANGES_ON_NODE) == CHECK_PENDING_CHANGES_ON_NODE) {
            if (item.isNode() && ((NodeImpl) item).hasPendingChanges()) {
                String msg = "Unable to perform operation. Session has pending changes.";
                log.debug(msg);
                throw new InvalidItemStateException(msg);
            }
        }
        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            if (isProtected(item)) {
                String msg = "Unable to perform operation. Node is protected.";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        if ((options & CHECK_CHECKED_OUT) == CHECK_CHECKED_OUT) {
            NodeImpl node = (item.isNode()) ? (NodeImpl) item : (NodeImpl) item.getParent();
            if (!node.isCheckedOut()) {
                String msg = "Unable to perform operation. Node is checked-in.";
                log.debug(msg);
                throw new VersionException(msg);
            }
        }
        if ((options & CHECK_LOCK) == CHECK_LOCK) {
            checkLock(item);
        }

        if (permissions > Permission.NONE) {
            Path path = item.getPrimaryPath();
            accessMgr.checkPermission(path, permissions);
        }
        if ((options & CHECK_HOLD) == CHECK_HOLD) {
            if (hasHold(item, isRemoval)) {
                throw new RepositoryException("Unable to perform operation. Node is affected by a hold.");
            }
        }
        if ((options & CHECK_RETENTION) == CHECK_RETENTION) {
            if (hasRetention(item, isRemoval)) {
                throw new RepositoryException("Unable to perform operation. Node is affected by a retention.");
            }
        }
    }

    public boolean canModify(ItemImpl item, int options, int permissions) throws RepositoryException {
        return hasCondition(item, options, permissions, false);
    }

    private boolean hasCondition(ItemImpl item, int options, int permissions, boolean isRemoval) throws RepositoryException {
        if ((options & CHECK_PENDING_CHANGES) == CHECK_PENDING_CHANGES) {
            if (item.getSession().hasPendingChanges()) {
                return false;
            }
        }
        if ((options & CHECK_PENDING_CHANGES_ON_NODE) == CHECK_PENDING_CHANGES_ON_NODE) {
            if (item.isNode() && ((NodeImpl) item).hasPendingChanges()) {
                return false;
            }
        }
        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            if (isProtected(item)) {
                return false;
            }
        }
        if ((options & CHECK_CHECKED_OUT) == CHECK_CHECKED_OUT) {
            NodeImpl node = (item.isNode()) ? (NodeImpl) item : (NodeImpl) item.getParent();
            if (!node.isCheckedOut()) {
                return false;
            }
        }
        if ((options & CHECK_LOCK) == CHECK_LOCK) {
            try {
                checkLock(item);
            } catch (LockException e) {
                return false;
            }
        }
        if (permissions > Permission.NONE) {
            Path path = item.getPrimaryPath();
            if (!accessMgr.isGranted(path, permissions)) {
                return false;
            }
        }
        if ((options & CHECK_HOLD) == CHECK_HOLD) {
            if (hasHold(item, isRemoval)) {
                return false;
            }
        }
        if ((options & CHECK_RETENTION) == CHECK_RETENTION) {
            if (hasRetention(item, isRemoval)) {
                return false;
            }
        }
        return true;
    }

    private void checkLock(ItemImpl item) throws LockException, RepositoryException {
        if (item.isNew()) {
            // a new item needs no check
            return;
        }
        NodeImpl node = (item.isNode()) ? (NodeImpl) item : (NodeImpl) item.getParent();
        lockMgr.checkLock(node);
    }

    private boolean isProtected(ItemImpl item) throws RepositoryException {
        ItemDefinition def;
        if (item.isNode()) {
            def = ((Node) item).getDefinition();
        } else {
            def = ((Property) item).getDefinition();
        }
        return def.isProtected();
    }

    private boolean hasHold(ItemImpl item, boolean isRemoval) throws RepositoryException {
        if (item.isNew()) {
            return false;
        }
        Path path = item.getPrimaryPath();
        if (!item.isNode()) {
            path = path.getAncestor(1);
        }
        boolean checkParent = (item.isNode() && isRemoval);
        return retentionReg.hasEffectiveHold(path, checkParent);
    }

    private boolean hasRetention(ItemImpl item, boolean isRemoval) throws RepositoryException {
        if (item.isNew()) {
            return false;
        }
        Path path = item.getPrimaryPath();
        if (!item.isNode()) {
            path = path.getAncestor(1);
        }
        boolean checkParent = (item.isNode() && isRemoval);
        return retentionReg.hasEffectiveRetention(path, checkParent);
    }


    
    //-------------------------------------------------< misc. helper methods >
    /**
     * Helper method that builds the effective (i.e. merged and resolved)
     * node type representation of the specified node's primary and mixin
     * node types.
     *
     * @param state
     * @return the effective node type
     * @throws RepositoryException
     */
    public EffectiveNodeType getEffectiveNodeType(NodeState state)
            throws RepositoryException {
        try {
            return ntReg.getEffectiveNodeType(
                    state.getNodeTypeName(), state.getMixinTypeNames());
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node "
                    + safeGetJCRPath(state.getNodeId());
            log.debug(msg);
            throw new RepositoryException(msg, ntce);
        }
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
     * @throws ConstraintViolationException if no applicable child node definition
     *                                      could be found
     * @throws RepositoryException          if another error occurs
     */
    public QNodeDefinition findApplicableNodeDefinition(Name name,
                                                Name nodeTypeName,
                                                NodeState parentState)
            throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicableChildNodeDef(name, nodeTypeName, ntReg);
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
    public QPropertyDefinition findApplicablePropertyDefinition(Name name,
                                                    int type,
                                                    boolean multiValued,
                                                    NodeState parentState)
            throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicablePropertyDef(name, type, multiValued);
    }

    /**
     * Helper method that finds the applicable definition for a property with
     * the given name, type in the parent node's node type and mixin types.
     * Other than <code>{@link #findApplicablePropertyDefinition(Name, int, boolean, NodeState)}</code>
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
    public QPropertyDefinition findApplicablePropertyDefinition(Name name,
                                                    int type,
                                                    NodeState parentState)
            throws RepositoryException, ConstraintViolationException {
        EffectiveNodeType entParent = getEffectiveNodeType(parentState);
        return entParent.getApplicablePropertyDef(name, type);
    }

    /**
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @param path path to convert
     * @return JCR path
     */
    public String safeGetJCRPath(Path path) {
        try {
            return resolver.getJCRPath(path);
        } catch (NamespaceException e) {
            log.error("failed to convert {} to a JCR path", path);
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }

    /**
     * Failsafe translation of internal <code>ItemId</code> to JCR path for use
     * in error messages etc.
     *
     * @param id id to translate
     * @return JCR path
     */
    public String safeGetJCRPath(ItemId id) {
        try {
            return safeGetJCRPath(hierMgr.getPath(id));
        } catch (ItemNotFoundException e) {
            // return string representation of id as a fallback
            return id.toString();
        } catch (RepositoryException e) {
            log.error(id + ": failed to build path");
            // return string representation of id as a fallback
            return id.toString();
        }
    }
}
