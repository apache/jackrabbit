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

import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.jcr2spi.security.AccessManager;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

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
     * <code>{@link #checkRemoveItem}</code> methods:
     * <p>
     * check access rights
     */
    public static final int CHECK_ACCESS = 1;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveItem}</code> methods:
     * <p>
     * check lock status
     */
    public static final int CHECK_LOCK = 2;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveItem}</code> methods:
     * <p>
     * check checked-out status
     */
    public static final int CHECK_VERSIONING = 4;
    /**
     * option for <code>{@link #checkAddNode}</code> and
     * <code>{@link #checkRemoveItem}</code> methods:
     * <p>
     * check constraints defined in node type
     */
    public static final int CHECK_CONSTRAINTS = 8;

    /**
     * option for <code>{@link #checkRemoveItem}</code> method:
     * <p>
     * check that target node is not being referenced
     */
    public static final int CHECK_COLLISION = 32;

    public static final int CHECK_NONE = 0;
    public static final int CHECK_ALL = CHECK_ACCESS | CHECK_LOCK | CHECK_VERSIONING | CHECK_CONSTRAINTS | CHECK_COLLISION;

    /**
     * manager provider
     */
    private final ManagerProvider mgrProvider;
    private final PathFactory pathFactory;

    /**
     * Creates a new <code>ItemStateValidator</code> instance.
     *
     * @param mgrProvider manager provider
     */
    public ItemStateValidator(ManagerProvider mgrProvider, PathFactory pathFactory) {
        this.mgrProvider = mgrProvider;
        this.pathFactory = pathFactory;
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
        EffectiveNodeType entPrimary = mgrProvider.getEffectiveNodeTypeProvider().getEffectiveNodeType(nodeState.getNodeTypeName());
        QNodeDefinition def = nodeState.getDefinition();

        // check if primary type satisfies the 'required node types' constraint
        Name[] requiredPrimaryTypes = def.getRequiredPrimaryTypes();
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
        // effective node type (primary type incl. mixins)
        Name[] ntNames = nodeState.getAllNodeTypeNames();
        EffectiveNodeType entPrimaryAndMixins = mgrProvider.getEffectiveNodeTypeProvider().getEffectiveNodeType(ntNames);
        QPropertyDefinition[] pda = entPrimaryAndMixins.getMandatoryQPropertyDefinitions();
        for (int i = 0; i < pda.length; i++) {
            QPropertyDefinition pd = pda[i];
            if (!nodeState.hasPropertyName(pd.getName())) {
                String msg = safeGetJCRPath(nodeState)
                        + ": mandatory property " + pd.getName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
        // mandatory child nodes
        QNodeDefinition[] cnda = entPrimaryAndMixins.getMandatoryQNodeDefinitions();
        for (int i = 0; i < cnda.length; i++) {
            QNodeDefinition cnd = cnda[i];
            if (!nodeState.getNodeEntry().hasNodeEntry(cnd.getName())) {
                String msg = safeGetJCRPath(nodeState)
                        + ": mandatory child node " + cnd.getName()
                        + " does not exist";
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            }
        }
    }

    //-------------------------------------------------< misc. helper methods >
    /**
     * Failsafe translation of internal <code>ItemState</code> to JCR path for use
     * in error messages etc.
     *
     * @param itemState
     * @return JCR path
     * @see LogUtil#safeGetJCRPath(ItemState,org.apache.jackrabbit.spi.commons.conversion.PathResolver)
     */
    private String safeGetJCRPath(ItemState itemState) {
        return LogUtil.safeGetJCRPath(itemState, mgrProvider.getPathResolver());
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

        NodeState parent = propState.getParent();
        QPropertyDefinition def = propState.getDefinition();
        checkWriteProperty(parent, propState.getName(), def, options);
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
    public void checkAddProperty(NodeState parentState, Name propertyName, QPropertyDefinition definition, int options)
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
    private void checkWriteProperty(NodeState parentState, Name propertyName, QPropertyDefinition definition, int options)
        throws ConstraintViolationException, AccessDeniedException,
        VersionException, LockException, ItemNotFoundException,
        ItemExistsException, PathNotFoundException, RepositoryException {

        checkIsWritable(parentState, options);

        // access restriction on prop.
        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            // make sure current session is granted write access on new prop
            Path relPath = pathFactory.create(propertyName);
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
    public void checkAddNode(NodeState parentState, Name nodeName,
                             Name nodeTypeName, int options)
            throws ConstraintViolationException, AccessDeniedException,
            VersionException, LockException, ItemNotFoundException,
            ItemExistsException, RepositoryException {

        checkIsWritable(parentState, options);

        // access restrictions on new node
        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            // make sure current session is granted write access on parent node
            Path relPath = pathFactory.create(nodeName);
            if (!mgrProvider.getAccessManager().isGranted(parentState, relPath, new String[] {AccessManager.ADD_NODE_ACTION})) {
                throw new AccessDeniedException(safeGetJCRPath(parentState) + ": not allowed to add child node '" + nodeName +"'");
            }
        }
        // node type constraints
        if ((options & CHECK_CONSTRAINTS) == CHECK_CONSTRAINTS) {
            // make sure there's an applicable definition for new child node
            Name[] ntNames = parentState.getAllNodeTypeNames();
            EffectiveNodeType entParent = mgrProvider.getEffectiveNodeTypeProvider().getEffectiveNodeType(ntNames);
            QNodeTypeDefinition def = mgrProvider.getNodeTypeDefinitionProvider().getNodeTypeDefinition(nodeTypeName);
            entParent.checkAddNodeConstraints(nodeName, def, mgrProvider.getItemDefinitionProvider());
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

        if (targetState.isNode() && ((NodeState)targetState).isRoot()) {
            // root node
            throw new ConstraintViolationException("Cannot remove root node.");
        }
        // check parent
        checkIsWritable(targetState.getParent(), options);

        // access rights
        if ((options & CHECK_ACCESS) == CHECK_ACCESS) {
            try {
                // make sure current session is allowed to remove target node
                if (!mgrProvider.getAccessManager().canRemove(targetState)) {
                    throw new AccessDeniedException(safeGetJCRPath(targetState) + ": not allowed to remove node");
                }
            } catch (ItemNotFoundException e) {
                String msg = "internal error: failed to check access rights for " + safeGetJCRPath(targetState);
                log.debug(msg);
                throw new RepositoryException(msg, e);
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
     * <p>
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

        NodeState nodeState = (itemState.isNode()) ? (NodeState)itemState : itemState.getParent();
        mgrProvider.getVersionStateManager().checkIsCheckedOut(nodeState);
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
        // make sure there's no foreign lock present the node (or the parent node
        // in case the state represents a PropertyState).
        NodeState nodeState = (itemState.isNode()) ? ((NodeState)itemState) : itemState.getParent();
        mgrProvider.getLockStateManager().checkLock(nodeState);
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
     * An item state cannot be removed if it is protected.
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
    }

    /**
     *
     * @param parentState
     * @param propertyName
     * @throws ItemExistsException
     * @throws RepositoryException
     */
    private void checkCollision(NodeState parentState, Name propertyName) throws ItemExistsException, RepositoryException {
        NodeEntry parentEntry = (NodeEntry) parentState.getHierarchyEntry();
         // NOTE: check for name collisions with existing child node has been
         // removed as with JSR 283 having same-named node and property can be
         // allowed. thus delegate the corresponding validation to the underlying
         // SPI implementation.

        // check for name collisions with an existing property
        PropertyEntry pe = parentEntry.getPropertyEntry(propertyName);
        if (pe != null) {
            try {
                pe.getPropertyState();
                throw new ItemExistsException("Property '" + pe.getName() + "' already exists.");
            } catch (ItemNotFoundException e) {
                // apparently conflicting entry does not exist any more
                // ignore and return
            }
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
    private void checkCollision(NodeState parentState, Name nodeName, Name nodeTypeName) throws RepositoryException, ConstraintViolationException, NoSuchNodeTypeException {
         // NOTE: check for name collisions with existing child property has been
         // removed as with JSR 283 having same-named node and property may be
         // allowed. thus delegate the corresponding validation to the underlying
         // SPI implementation.

         // check for conflict with existing same-name sibling node.
         if (parentState.hasChildNodeEntry(nodeName, Path.INDEX_DEFAULT)) {
            // retrieve the existing node state that ev. conflicts with the new one.
            try {
                NodeState conflictingState = parentState.getChildNodeState(nodeName, Path.INDEX_DEFAULT);
                QNodeDefinition conflictDef = conflictingState.getDefinition();
                QNodeDefinition newDef = mgrProvider.getItemDefinitionProvider().getQNodeDefinition(parentState.getAllNodeTypeNames(), nodeName, nodeTypeName);

                // check same-name sibling setting of both target and existing node
                if (!(conflictDef.allowsSameNameSiblings() && newDef.allowsSameNameSiblings())) {
                    throw new ItemExistsException("Cannot add child node '"
                            + nodeName.getLocalName() + "' to "
                            + safeGetJCRPath(parentState)
                            + ": colliding with same-named existing node.");
                }
            } catch (ItemNotFoundException e) {
                // ignore: conflicting doesn't exist any more
            }
        }
    }
}
