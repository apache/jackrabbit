<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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


require_once 'PHPCR/AccessDeniedException.php';
require_once 'PHPCR/nodetype/ConstraintViolationException.php';
require_once 'PHPCR/nodetype/NoSuchNodeTypeException.php';
require_once 'PHPCR/nodetype/NodeDefinition.php';
require_once 'PHPCR/nodetype/NodeType.php';
require_once 'PHPCR/version/OnParentVersionAction.php';
/* require_once 'PHPCR/version/Version.php'; */
require_once 'PHPCR/version/VersionException.php';
/* require_once 'PHPCR/version/VersionHistory.php'; */
/* require_once 'PHPCR/version/VersionIterator.php'; */
require_once 'PHPCR/ItemExistsException.php';
require_once 'PHPCR/PathNotFoundException.php';
require_once 'PHPCR/nodetype/NoSuchNodeTypeException.php';
require_once 'PHPCR/nodetype/ConstraintViolationException.php';
require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/ValueFormatException.php';
require_once 'PHPCR/NodeIterator.php';
require_once 'PHPCR/Property.php';
require_once 'PHPCR/PropertyIterator.php';
require_once 'PHPCR/Item.php';
require_once 'PHPCR/ItemNotFoundException.php';
require_once 'PHPCR/NoSuchWorkspaceException.php';
require_once 'PHPCR/UnsupportedRepositoryOperationException.php';
require_once 'PHPCR/MergeException.php';
require_once 'PHPCR/lock/Lock.php';
require_once 'PHPCR/lock/LockException.php';
require_once 'PHPCR/InvalidItemStateException.php';


/**
 * The <code>Node</code> interface represents a node in the hierarchy that
 * makes up the repository.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
interface Node extends Item
{
    /**
     * Creates a new node at <code>relPath</code>. The new node will only be
     * persisted in the workspace when <code>save()</code> and if the structure
     * of the new node (its child nodes and properties) meets the constraint
     * criteria of the parent node's node type.
     * <p/>
     * If <code>relPath</code> implies intermediary nodes that do not
     * exist then a <code>PathNotFoundException</code> is thrown.
     * <p/>
     * If an item already exists at <code>relPath</code> then an
     * <code>ItemExistsException</code> is thrown.
     * <p/>
     * If an attempt is made to add a node as a child of a
     * property then a <code>ConstraintViolationException</code> is
     * thrown immediately (not on <code>save</code>).
     * <p/>
     * Since this signature does not allow explicit node type assignment, the
     * new node's node types (primary and mixin, if applicable) will be
     * determined immediately (not on save) by the <code>NodeDefinition</code>s
     * in the node types of its parent. If there is no <code>NodeDefinition</code>
     * corresponding to the name specified for this new node, then a
     * <code>ConstraintViolationException</code> is thrown immediately (not on
     * <code>save</code>).
     *
     * @param  relPath The path of the new node to be created.
     * @return The node that was added.
     * @param primaryNodeTypeName The name of the primary node type of the new node.
     * @throws ItemExistsException If an item at the specified path already exists.
     * @throws PathNotFoundException If the specified path implies intermediary
     * nodes that do not exist.
     * @throws ConstraintViolationException if If there is no NodeDefinition
     * corresponding to the name specified for this new node in the parent
     * node's node type, or if an attempt is made to add a node as a child of a
     * property.
     * @throws RepositoryException If another error occurs.
     */
    public function addNode( $relPath, $primaryNodeTypeName = null );

    /**
     * If this node supports child node ordering, this method inserts the child node at
     * <code>srcChildRelPath</code> before its sibling, the child node at <code>destChildRelPath</code>,
     * in the child node list.
     * <p/>
     * To place the node <code>srcChildRelPath</code> at the end of the list, a <code>destChildRelPath</code>
     * of <code>null</code> is used.
     * <p/>
     * Note that (apart from the case where <code>destChildRelPath</code> is <code>null</code>) both of these
     * arguments must be relative paths of depth one, in other words they are the names of the child nodes,
     * possibly suffixed with an index.
     * <p/>
     * Changes to ordering of child nodes are persisted on <code>save</code> of the parent node. But, if this node
     * does not support child node ordering, then a <code>UnsupportedRepositoryOperationException</code>
     * thrown immediately (i.e., not on <code>save</code>).
     * <p/>
     * If <code>srcChildRelPath</code> and <code>destChildRelPath</code> are the same,
     * then a <code>ConstraintViolationException</code> is thrown.
     * <p/>
     * A <code>ConstraintViolationException</code> is also thrown if a node-type or implementation-specific
     * constraint violation is detected immediately. Otherwise, if the violation can only be detected later,
     * on <code>save</code>, then that method throws a <code>ConstraintViolationException</code>. Implementations
     * may differ as to which constraints are enforced immediately, and which on <code>save</code>.
     * <p/>
     * If either parameter is not the relative path to a child node of this node, then an
     * <code>ItemNotFoundException</code> is thrown.
     * <p/>
     * A <code>VersionException</code> is thrown if this node is versionable and checked-in or is non-versionable
     * but its nearest versionable ancestor is checked-in.
     * <p/>
     * A <code>LockException</code> is thrown if a lock prevents the re-ordering.
     *
     * @param srcChildRelPath  the relative path to the child node (i.e., name plus possible index)
     *                         to be moved in the ordering
     * @param destChildRelPath the the relative path to the child node (i.e., name plus possible index)
     *                         before which the node <code>srcChildRelPath</code> will be placed.
     * @throws UnsupportedRepositoryOperationException
     *                                      if ordering is not supported.
     * @throws ConstraintViolationException if <code>srcChildRelPath</code> and
     *                                      <code>destChildRelPath</code> are the same or some implementation-specific ordering restriction is violated.
     * @throws ItemNotFoundException        if either parameter is not the relative path of a child node of this node.
     * @throws VersionException             if this node is versionable and checked-in or is non-versionable but its
     *                                      nearest versionable ancestor is checked-in.
     * @throws LockException                if a lock prevents the re-ordering.
     * @throws RepositoryException          if another error occurs.
     */
    public function orderBefore( $srcChildRelPath, $destChildRelPath );

    /**
     * This method returns the index of this node within the ordered set of its same-name
     * sibling nodes. This index is the one used to address same-name siblings using the
     * square-bracket notation, e.g., <code>/a[3]/b[4]</code>. Note that the index always starts
     * at 1 (not 0), for compatibility with XPath. As a result, for nodes that do not have
     * same-name-siblings, this method will always return 1.
     *
     * @return The index of this node within the ordered set of its same-name sibling nodes.
     * @throws RepositoryException if an error occurs.
     */
    public function getIndex();

    /**
     * Adds the existing node at <code>absPath</code> as child of this node, thus adding
     * <code>this</code> node as an addional parent of the node at <code>absPath</code>.
     * <p/>
     * This change will be persisted (if valid) on <code>save</code>.
     * <p/>
     * If the node at <code>absPath</code> is not of mixin type
     * <code>mix:referenceable</code>, a <code>ConstraintViolationException</code>
     * will be thrown on <code>save</code>.
     * <p/>
     * The name of the new child node as accessed from <code>this</code> node
     * will be the same as its current name in <code>absPath</code> (that is, the last path
     * segment in that <code>absPath</code>).
     *
     * @param absPath The absolute path of the new child node.
     * @return The new child node.
     * @param newName The new name for this node when referenced as a child of this node.
     * @throws PathNotFoundException If no node exists at <code>absPath</code>.
     * @throws RepositoryException   In level 2: If another error occurs.
     */
    public function addExistingNode( $absPath, $newName = null );

    /**
     * Sets the specified property to the specified value. If the property does
     * not yet exist, it is created. The property type of the property will be
     * that specified by the node type of <code>this</code> node (the one on
     * which this method is being called). If the </code>PropertyType</code>
     * of the supplied <code>Value</code> object (recall that a
     * <code>Value</code> object records the property type of its
     * contained value) is different from that required, a best-effort
     * conversion is attempted.
     * <p/>
     * If the node type of the parent node does not specify a
     * specific property type for the property being set, then
     * the property type of the supplied <code>Value</code> object
     * is used.
     * <p/>
     * If the property already exists (has previously been set) it assumes
     * the new value. If the node type of the parent node does not specify a
     * specific property type for the property being set, then the property
     * will also assume the new type (if different).
     * <p/>
     * To erase a property, use <code>#remove(String relPath)</code>.
     * <p/>
     * To persist the addition or change of a property to the workspace
     * <code>#save</code> must be called on
     * this node (the parent of the property being set) or a higher-order
     * ancestor of the property.
     *
     * @param name  The name of a property of this node
     * @param value The Value to be assigned, an array of <code>Value</code> objects,
     * an string representing path to a resource
     * @param type  The type of the property
     * @return The updated <code>Property</code> object
     * @throws ValueFormatException if <code>value</code> is incompatible with
     * (i.e. can not be converted to) the type of the specified property.
     * @throws ConstraintViolationException  if the change would violate
     * a node-type or other constraint and this implementation performs
     * this validation immediately instead of waiting until save.
     * @throws RepositoryException  If another error occurs.
     */
    public function setProperty( $name, $val, $type = null );

    /**
     * Returns the node at <code>relPath</code> relative to <code>this</code> node.
     * The properties and child nodes of the returned node can then be read
     * (and if permissions allow) changed and written. However, any changes
     * made to this node, its properties or its child nodes
     * (and their properties and child nodes, etc.) will only be persisted to
     * the repository upon calling save.
     *
     * @param  relPath The relative path of the node to retrieve.
     * @return The node at <code>relPath</code>.
     * @throws PathNotFoundException If no node exists at the
     * specified path.
     * @throws RepositoryException   If another error occurs.
     */
    public function getNode( $relPath );

    /**
     * Returns a <code>NodeIterator</code> over all child <code>Node</code>s of
     * this <code>Node</code>. Does <i>not</i> include properties of this
     * <code>Node</code>. The same <code>save</code> and re-acquisition
     * semantics apply as with <code>#getNode(String)</code>.
     *
     * @param namePattern a name pattern
     * @return A <code>NodeIterator</code> over all child <code>Node</code>s of
     * this <code>Node</code>.
     * @throws RepositoryException If an unexpected error occurs.
     */
    public function getNodes( $namePattern = null );

    /**
     * Returns the property at <code>relPath</code> relative to <code>this</code>
     * node. The same <code>save</code> and re-acquisition
     * semantics apply as with <code>#getNode(String)</code>.
     *
     * @param relPath The relative path of the property to retrieve.
     * @return The property at <code>relPath</code>.
     * @throws PathNotFoundException If no property exists at the
     *                               specified path.
     * @throws RepositoryException   If another error occurs.
     */
    public function getProperty( $relPath );

    /**
     * Returns all properties of this node.
     * Returns a <code>PropertyIterator</code> over all properties
     * of this node. Does <i>not</i> include child <i>nodes</i> of this
     * node. The same <code>save</code> and re-acquisition
     * semantics apply as with <code>#getNode(String)</code>.
     *
     * @param namePattern a name pattern
     * @return A <code>PropertyIterator</code>.
     * @throws RepositoryException If an error occurs.
     */
    public function getProperties( $namePattern = null );

    /**
     * Returns all <code>REFERENCE</code> properties that refer to this node.
     * <p/>
     * Note that in level 2 implementations, this method returns only saved properties (in a transactional setting
     * this includes both those properties that have been saved but not yet committed,
     * as well as properties that have been committed). This method does not return <code>REFERENCE</code>
     * properties that have been added but not yet saved.
     * <p/>
     * In implementaions that support versioing, this method does not return <code>REFERENCE</code> properties
     * that are part of the frozen state of a version in version storage.
     * <p/>
     * If this node has no references, an empty iterator is returned.
     *
     * @return A <code>PropertyIterator</code>.
     * @throws RepositoryException if an error occurs
     */
    public function getReferences();

    /**
     * Returns the first property of <i>this</i> node found with the specified value.
     * What makes a particular property "first" (that is, the search order) is
     * implementaion dependent. If the specified value and the value of a
     * property are of different types then a conversion is attempted before the
     * equality test is made. Returns <code>null</code> if no such property is
     * found. In the case of multivalue properties, a property qualifies as
     * having the specified value if and only if at least one of its values matches.
     * The same <code>save</code> and re-acquisition
     * semantics apply as with <code>#getNode(String)</code>.
     *
     * @param value A <code>Value</code> object.
     * @return The first property of <i>this</i> node found with the specified value.
     * @throws RepositoryException If an unexpected error occurs.
     */
    public function findProperty( Value $value );

    /**
     * Returns all properties of <i>this</i> node with the specified value.
     * If the spedified value and the value of a property are of different types
     * then a conversion is attempted before the equality test is made. Returns
     * an empty iterator if no such property could be found. In the case of
     * multivalue properties, a property qualifies as having the specified value
     * if and only if at least one of its values matches.
     * The same <code>save</code> and re-acquisition
     * semantics apply as with <code>#getNode(String)</code>.
     *
     * @param value A <code>Value</code> object.
     * @return A PropertyIterator holding all properties of this node with the
     * specified value. Returns an empty iterator if no such property could be found.
     * @throws RepositoryException If an unexpected error occurs.
     */
    public function findProperties( Value $value );

    /**
     * Returns the deepest primary child item accessible via a chain of
     * primary child items from this node.
     * A node's type can specifiy a maximum of one of
     * its child items (child node or property) as its <i>primary child item</i>.
     * This method traverses the chain of primary child items of this node
     * until it either encounters a property or encounters a node that does not
     * have a primary child item. It then returns that property or node. If
     * this node itself (the one that this method is being called on) has no
     * primary child item then this method throws a
     * <code>ItemNotFoundException</code>. The same <code>save</code> and re-acquisition
     * semantics apply as with <code>#getNode(String)</code>.
     *
     * @return the deepest primary child item accessible from this node via
     * a chain of primary child items.
     * @throws ItemNotFoundException if this node does not have a primary
     * child item.
     * @throws RepositoryException If another error occurs.
     */
    public function getPrimaryItem();

    /**
     * Returns the UUID of this node as recorded in the node's jcr:UUID
     * property. This method only works on nodes of mixin node type
     * <code>mix:referenceable</code>. On nonreferenceable nodes, this method
     * throws an <code>UnsupportedRepositoryOperationException</code>.
     *
     * @return the UUID of this node
     * @throws UnsupportedRepositoryOperationException If this node nonreferenceable.
     * @throws RepositoryException If another error occurs.
     */
    public function getUUID();

    /**
     * Indicates whether a node exists at <code>relPath</code>
     * Returns <code>true</code> if a node exists at <code>relPath</code> and
     * <code>false</code> otherwise.
     *
     * @param relPath The path of a (possible) node.
     * @return <code>true</code> if a node exists at <code>relPath</code>;
     * <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public function hasNode( $relPath );

    /**
     * Indicates whether a property exists at <code>relPath</code>
     * Returns <code>true</code> if a property exists at <code>relPath</code> and
     * <code>false</code> otherwise.
     *
     * @param relPath The path of a (possible) property.
     * @return <code>true</code> if a property exists at <code>relPath</code>;
     * <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public function hasProperty( $relPath );

    /**
     * Indicates whether this node has child nodes.
     * Returns <code>true</code> if this node has one or more child nodes;
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if this node has one or more child nodes;
     * <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public function hasNodes();

    /**
     * Indicates whether this node has properties.
     * Returns <code>true</code> if this node has one or more properties;
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if this node has one or more properties;
     * <code>false</code> otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public function hasProperties();

    /**
     * Returns the primary node type of this node.
     *
     * @return a <code>NodeType</code> object.
     */
    public function getPrimaryNodeType();

    /**
     * Returns an array of NodeType objects representing the mixin node types
     * assigned to this node.
     *
     * @return a <code>NodeType</code> object.
     */
    public function getMixinNodeTypes();

    /**
     * Indicates whether this node is of the specified node type.
     * Returns <code>true</code> if this node is of the specified node type
     * or a subtype of the specified node type. Returns <code>false</code> otherwise.
     * This method provides a quick method for determining whether
     * a particular node is of a particular type without having to
     * manually search the inheritance hierarchy (which, in some implementations
     * may be a multiple-inhertiance hierarchy, making a manual search even
     * more complex). This method works for both perimary node types and mixin
     * node types.
     *
     * @param nodeTypeName the name of a node type.
     * @return true if this node is of the specified node type
     * or a subtype of the specified node type; returns false otherwise.
     * @throws RepositoryException If an unspecified error occurs.
     */
    public function isNodeType( $nodeTypeName );

    /**
     * Adds the specified mixin node type to this node. If a conflict with
     * another assigned mixin or the main node type results, then an exception
     * is thrown on save. Adding a mixin node type to a node immediately adds
     * the name of that type to the list held in that node�s
     * <code>jcr:mixinTypes</code> property.
     *
     * @param mixinName
     */
    public function addMixin( $mixinName );

    /**
     * Removes the specified mixin node type from this node. Also removes <code>mixinName</code>
     * from this node's <code>jcr:mixinTypes</code> property. The mixin node type removal
     * takes effect on <code>save</code>.
     * <p/>
     * If this node does not have the specified mixin, a <code>NoSuchNodeTypeException</code> is thrown.
     * <p/>
     * A <code>ConstraintViolationException</code> will be thrown if the removal of a mixin is not allowed
     * (implementations are free to enforce any policy they like with regard to mixin removal).
     * <p/>
     * A <code>VersionException</code> is thrown if this node is versionable and checked-in or is
     * non-versionable but its nearest versionable ancestor is checked-in.
     * <p/>
     * A <code>LockException</code> is thrown if a lock prevents the removal of the mixin.
     *
     * @param mixinName the name of the mixin node type to be removed.
     * @throws NoSuchNodeTypeException      If the specified <code>mixinName</code>
     *                                      is not currently assigned to this node.
     * @throws ConstraintViolationException If the specified mixin node type
     *                                      is prevented from being removed.
     * @throws VersionException             if this node is versionable and checked-in or is non-versionable but its
     *                                      nearest versionable ancestor is checked-in.
     * @throws LockException                if a lock prevents the removal of the mixin.
     * @throws RepositoryException          If another error occurs.
     */
    public function removeMixin( $mixinName );

    /**
     * Returns <code>true</code> if the specified mixin node type,
     * <code>mixinName</code>, can be added to this node. Returns
     * <code>false</code> otherwise. Addition of a mixin can be
     * prevented for any of the following reasons:
     * <ul>
     * <li>
     * The mixin's definition conflicts with the existing primary node type or one of the
     * existing mixin node types.
     * </li>
     * <li>
     * The node is versionable and checked-in or is non-versionable and its nearest
     * versionable ancestor is checked-in.
     * </li>
     * <li>
     * The addition is prevented because this node is protected
     * (as defined in this node's NodeDefinition, found in this node's parent's node type).
     * </li>
     * <li>
     * The addition is prevented due to access control restrictions.
     * </li>
     * <li>
     * The addition is prevented due to a lock.
     * </li>
     * <li>
     * The addition is prevented for implementation-specific reasons.
     * </li>
     * </ul>
     *
     * @param mixinName
     * @return <code>true</code> if the specified mixin node type,
     *         <code>mixinName</code>, can be added to this node; <code>false</code> otherwise.
     * @throws RepositoryException if an error occurs.
     */
    public function canAddMixin( $mixinName );

    /**
     * Returns the definition of <i>this</i> <code>Node</code>. This method is
     * actually a shortcut to searching through this node's parent's node type
     * (and its supertypes) for the child node definition applicable to this
     * node.
     *
     * @return a <code>NodeDefinition</code> object.
     * @see NodeType#getChildNodeDefinitions
     */
    public function getDefinition();

    /**
     * Creates a new version with a system generated version name and returns that version.
     *
     * @return a <code>Version</code> object
     * @throws UnsupportedRepositoryOperationException if versioning is not supported.
     * @throws RepositoryException If another error occurs.
     */
    public function checkin();

    /**
     * Sets this versionable node to checked-out status by setting its
     * <code>jcr:isCheckedOut</code> property to true.
     *
     * @throws UnsupportedRepositoryOperationException if versioning is not supported.
     * @throws RepositoryException If another error occurs.
     */
    public function checkout();

    /**
     * Updates this node to reflect the state (i.e., have the same properties
     * and child nodes) of this node's corresponding node in srcWorkspace (that
     * is, the node in srcWorkspace with the same UUID as this node). If
     * shallow is set to false, then only this node is updated. If shallow is
     * set to true then every node with a UUID in the subtree rooted at this
     * node is updated. If the current ticket does not have sufficient rights
     * to perform the update or the specified workspace does not exist, a
     * <code>NoSuchWorkspaceException</code> is thrown. If another error occurs
     * then a RepositoryException is thrown. If the update succeeds the changes
     * made to this node are persisted immediately, there is no need to call
     * save.
     *
     * @param srcWorkspaceName the name of the source workspace.
     * @param shallow a boolean
     * @throws RepositoryException If another error occurs.
     */
    public function update( $srcWorkspaceName, $shallow );

    /**
     * This method can be thought of as a version-sensitive update
     * (see 7.1.7 Updating and Cloning Nodes across Workspaces in the
     * specification).
     *
     * It recursively tests each versionable node in the subtree of this
     * node against its corresponding node in srcWorkspace with respect to
     * the relation between their respective base versions and either updates
     * the node in question or not, depending on the outcome of the test.
     * For details see 8.2.10 Merge in the specification. A MergeException
     * is thrown if bestEffort is false and a versionable node is encountered
     * whose corresponding node's base version is on a divergent branch from
     * this node's base version.
     *
     * If successful, the changes are persisted immediately, there is no need
     * to call save.
     *
     * This method returns a NodeIterator over all versionable nodes in the
     * subtree that received a merge result of fail.
     *
     * If bestEffort is false, this iterator will be empty (since if it merge
     * returns successfully, instead of throwing an exception, it will be
     * because no failures were encountered).
     *
     * If bestEffort is true, this iterator will contain all nodes that
     * received a fail during the course of this merge operation.
     *
     * If the specified srcWorkspace does not exist, a NoSuchWorkspaceException
     * is thrown.
     *
     * If the current session does not have sufficient permissions to perform
     * the operation, then an AccessDeniedException is thrown.
     *
     * An InvalidItemStateException is thrown if this session (not necessarily
     * this node) has pending unsaved changes.
     *
     * A LockException is thrown if a lock prevents the merge.
     *
     * @param srcWorkspace the name of the source workspace.
     * @param shallow a boolean
     * @return iterator over all nodes that received a merge result of "fail"
     * in the course of this operation.
     * @throws UnsupportedRepositoryOperationException if versioning is not supported.
     * @throws MergeException succeeds if the base version of the corresponding
     * node in srcWorkspace is not a successor of the base version of this node.
     * @throws NoSuchWorkspaceException If the current
     * ticket does not have sufficient rights to perform the <code>merge</code> or the
     * specified workspace does not exist.
     * @throws RepositoryException If another error occurs.
     */
    public function merge( $srcWorkspace, $shallow );

    /**
     * Returns <code>true</code> if this node is currently checked-out and <code>false</code> otherwise.
     *
     * @return a boolean
     * @throws UnsupportedRepositoryOperationException if versioning is not supported.
     * @throws RepositoryException If another error occurs.
     */
    public function isCheckedOut();

    /**
     * Restores this node to the state recorded in the specified version.
     *
     * @param versionName, or Version Object
     * @throws UnsupportedRepositoryOperationException if versioning is not supported.
     * @throws RepositoryException If another error occurs.
     */
    public function restore( $in );

    /**
     * Restores this node to the state recorded in the version specified by
     * <code>versionLabel</code>.
     *
     * @param versionLabel a String
     * @throws UnsupportedRepositoryOperationException if versioning is not supported.
     * @throws RepositoryException If another error occurs.
     */
    public function restoreByLabel( $versionLabel );

    /**
     * Returns the <code>VersionHistory</code> object of this node. This object
     * is simply a wrapper for the <code>nt:versionHistory</code> node holding
     * this node's versions.
     *
     * @return a <code>VersionHistory</code> object
     * @throws UnsupportedRepositoryOperationException if versioning is not supported.
     * @throws RepositoryException If another error occurs.
     */
    public function getVersionHistory();

    /**
     * Returns the current base version of this versionable node.
     *
     * @return a <code>Version</code> object.
     * @throws UnsupportedRepositoryOperationException if versioning is not supported.
     * @throws RepositoryException If another error occurs.
     */
    public function getBaseVersion();

    /**
     * Completes the merge process with respect to this node and the specified <code>version</code>.
     * <p/>
     * When the {@link #merge} method is called on a node, every versionable node in that
     * subtree is compared with its corresponding node in the indicated other workspace and
     * a "merge test result" is determined indicating one of the following:
     * <ol>
     * <li>
     * This node will be updated to the state of its correspondee (if the base version
     * of the correspondee is more recent in terms of version history)
     * </li>
     * <li>
     * This node will be left alone (if this node's base version is more recent in terms of
     * version history).
     * </li>
     * <li>
     * This node will be marked as having failed the merge test (if this node's base version
     * is on a different branch of the version history from the base version of its
     * corresponding node in the other workspace, thus preventing an automatic determination
     * of which is more recent).
     * </li>
     * </ol>
     * (See {@link #merge} for more details)
     * <p/>
     * In the last case the merge of the non-versionable subtree
     * (the "content") of this node must be done by the application (for example, by
     * providing a merge tool for the user).
     * <p/>
     * Additionally, once the content of the nodes has been merged, their version graph
     * branches must also be merged. The JCR versioning system provides for this by
     * keeping a record, for each versionable node that fails the merge test, of the
     * base verison of the corresponding node that caused the merge failure. This record
     * is kept in the <code>jcr:mergeFailed</code> property of this node. After a
     * <code>merge</code>, this property will contain one or more (if
     * multiple merges have been performed) <code>REFERENCE</code>s that point
     * to the "offending versions".
     * <p/>
     * To complete the merge process, the client calls <code>doneMerge(Version v)</code>
     * passing the version object referred to be the <code>jcr:mergeFailed</code> property
     * that the client wishes to connect to <code>this</code> node in the version graph.
     * This has the effect of moving the reference to the indicated version from the
     * <code>jcr:mergeFailed</code> property of <code>this</code> node to the
     * <code>jcr:predecessors</code>.
     * <p/>
     * If the client chooses not to connect this node to a particular version referenced in
     * the <code>jcr:mergeFailed</code> property, he calls {@link #cancelMerge(Version version)}.
     * This has the effect of removing the reference to the specified <code>version</code> from
     * <code>jcr:mergeFailed</code> <i>without</i> adding it to <code>jcr:predecessors</code>.
     * <p/>
     * Once the last reference in <code>jcr:mergeFailed</code> has been either moved to
     * <code>jcr:predecessors</code> (with <code>doneMerge</code>) or just removed
     * from <code>jcr:mergeFailed</code> (with <code>cancelMerge</code>) the <code>jcr:mergeFailed</code>
     * property is automatically removed, thus enabling <code>this</code>
     * node to be checked-in, creating a new version (note that before the <code>jcr:mergeFailed</code>
     * is removed, its <code>OnParentVersion</code> setting of <code>ABORT</code> prevents checkin).
     * This new version will have a predecessor connection to each version for which <code>doneMerge</code>
     * was called, thus joining those branches of the version graph.
     * <p/>
     * If successful, these changes are persisted immediately,
     * there is no need to call <code>save</code>.
     * <p/>
     * A <code>VersionException</code> is thrown if the <code>version</code> specified is
     * not among those referecned in this node's <code>jcr:mergeFailed</code> property.
     * <p/>
     * If there are unsaved changes pending on this node, an <code>InvalidItemStateException</code> is thrown.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if this repsoitory does not
     * support versioning.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param version a version referred to by this node's <code>jcr:mergeFailed</code>
     *                property.
     * @throws VersionException          if the version specifed is
     *                                   not among those referenced in this node's <code>jcr:mergeFailed</code>
     *                                   or if this node is currently checked-in.
     * @throws InvalidItemStateException if there are unsaved changes pending on this node.
     * @throws UnsupportedRepositoryOperationException
     *                                   if versioning is not supported in
     *                                   this repository.
     * @throws RepositoryException       if another error occurs.
     */
    public function doneMerge( /* Version */ $version );

    /**
     * Cancels the merge process with respect to this node and specified <code>version</code>.
     * <p/>
     * See {@link #doneMerge} for a full explanation. Also see {@link #merge} for
     * more details.
     * <p/>
     * If successful, these changes are persisted immediately,
     * there is no need to call <code>save</code>.
     * <p/>
     * A <code>VersionException</code> is thrown if the <code>version</code> specified is
     * not among those referecned in this node's <code>jcr:mergeFailed</code>.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if this repsoitory does not
     * support versioning.
     * <p/>
     * If there are unsaved changes pending on this node, an <code>InvalidItemStateException</code> is thrown.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param version a version referred to by this node's <code>jcr:mergeFailed</code>
     *                property.
     * @throws VersionException          if the version specifed is
     *                                   not among those referenced in this node's <code>jcr:mergeFailed</code> or if this node is currently checked-in.
     * @throws InvalidItemStateException if there are unsaved changes pending on this node.
     * @throws UnsupportedRepositoryOperationException
     *                                   if versioning is not supported in
     *                                   this repository.
     * @throws RepositoryException       if another error occurs.
     */
    public function cancelMerge( /* Version */ $version );

    /**
     * Returns the absolute path of the node in the specified workspace that
     * corresponds to <code>this</code> node.
     * <p/>
     * The <i>corresponding node</i> is defined as the node in <code>srcWorkspace</code>
     * with the same UUID as this node or, if this node has no UUID, the same
     * path relative to the nearest ancestor that <i>does</i>  have a UUID,
     * or the root node, whichever comes first. This is qualified by the requirment that
     * referencable nodes only correspond with other referencables and non-referenceables
     * with other non-referenceables.
     * <p/>
     * If no corresponding node exists then an <code>ItemNotFoundException</code> is thrown.
     * <p/>
     * If the specified workspace does not exist then a <code>NoSuchWorkspaceException</code> is thrown.
     * <p/>
     * If the current <code>Session</code> does not have sufficent rights to perform this operation,
     * an <code>AccessDeniedException</code> is thrown.
     *
     * @param workspaceName
     * @return the absolute path to the corresponding node.
     * @throws ItemNotFoundException    if no corresponding node is found.
     * @throws NoSuchWorkspaceException if the worksapce is unknown.
     * @throws AccessDeniedException    if the current <code>session</code> has insufficent rights to perform this operation.
     * @throws RepositoryException      if another error occurs.
     */
    public function getCorrespondingNodePath( $workspaceName );

    /**
     * Places a lock on this node. If successful, this node is said to <i>hold</i> the lock.
     * <p/>
     * If <code>isDeep</code> is <code>true</code> then the lock applies to this node and all its descendant nodes;
     * if <code>false</code>, the lock applies only to this, the holding node.
     * <p/>
     * If <code>isSessionScoped</code> is <code>true</code> then this lock will expire upon the expiration of the current
     * session (either through an automatic or explicit <code>Session.logout</code>); if <code>false</code>, this lock
     * does not expire until explicitly unlocked or automatically unlocked due to a implementation-specific limitation,
     * such as a timeout.
     * <p/>
     * Returns a <code>Lock</code> object reflecting the state of the new lock and including a lock token. See, in
     * contrast, {@link Node#getLock}, which returns the <code>Lock</code> <i>without</i> the lock token.
     * <p/>
     * The lock token is also automatically added to the set of lock tokens held by the current <code>Session</code>.
     * <p/>
     * If successful, then the property <code>jcr:lockOwner</code> is created and set to the value of
     * <code>Session.getUserId</code> for the current session and the property <code>jcr:lockIsDeep</code> is set to the
     * value passed in as <code>isDeep</code>. These changes are persisted automatically; there is no need to call
     * <code>save</code>.
     * <p/>
     * Note that it is possible to lock a node even if it is checked-in (the lock-related properties will be changed
     * despite the checked-in status).
     * <p/>
     * If this node is not of mixin node type <code>mix:lockable</code> then an
     * <code>UnsupportedRepositoryOperationException</code> is thrown.
     * <p/>
     * If this node is already locked (either because it holds a lock or a lock above it applies to it),
     * a <code>LockException</code> is thrown.
     * <p/>
     * If <code>isDeep</code> is <code>true</code> and a descendant node of this node already holds a lock, then a
     * <code>LockException</code> is thrown.
     * <p/>
     * If the current session does not have sufficient privileges to place the lock, an
     * <code>AccessDeniedException</code> is thrown.
     * <p/>
     * An InvalidItemStateException is thrown if this node has pending unsaved changes.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param isDeep          if <code>true</code> this lock will apply to this node and all its descendants; if
     *                        <code>false</code>, it applies only to this node.
     * @param isSessionScoped if <code>true</code>, this lock expires with the current session; if <code>false</code> it
     *                        expires when explicitly or automatically unlocked for some other reason.
     * @return A <code>Lock</code> object containing a lock token.
     * @throws UnsupportedRepositoryOperationException
     *                                   if this node is not <code>mix:lockable</code>.
     * @throws LockException             if this node is already locked.
     * @throws AccessDeniedException     if this session does not have permission to lock this node.
     * @throws InvalidItemStateException if this node has pending unsaved changes.
     * @throws RepositoryException       is another error occurs.
     */
    public function lock( $isDeep, $isSessionScoped );

    /**
     * Returns the <code>Lock</code> object that applies to this node. This may be either a lock on this node itself
     * or a deep lock on a node above this node.
     * <p/>
     * If this <code>Session</code> (the one through which this <code>Node</code> was acquired)
     * holds the lock token for this lock, then the returned <code>Lock</code> object contains
     * that lock token (accessible through <code>Lock.getLockToken</code>). If this <code>Session</code>
     * does not hold the applicable lock token, then the returned <code>Lock</code> object will not
     * contain the lock token (its <code>Lock.getLockToken</code> method will return <code>null</code>).
     * <p/>
     * If this node is not of mixin node type <code>mix:lockable</code> then
     * an <code>UnsupportedRepositoryOperationException</code> is thrown.
     * <p/>
     * If no lock applies to this node, a <code>LockException</code> is thrown.
     * <p/>
     * If the current session does not have sufficient privileges to get the lock, an <code>AccessDeniedException</code>
     * is thrown.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return The applicable <code>Lock</code> object, without a contained lock token.
     * @throws UnsupportedRepositoryOperationException
     *                               If this node is not of mixin node type <code>mix:lockable</code>.
     * @throws LockException         if no lock applies to this node.
     * @throws AccessDeniedException if the curent session does not have pernmission to get the lock.
     * @throws RepositoryException   is another error occurs.
     */
    public function getLock();

    /**
     * Removes the lock on this node. Also removes the properties <code>jcr:lockOwner</code> and
     * <code>jcr:lockIsDeep</code> from this node. These changes are persisted automatically; there is no need to call
     * <code>save</code>.
     * <p/>
     * Note that it is possible to unlock a node even if it is checked-in (the lock-related properties will be changed
     * despite the checked-in status).
     * <p/>
     * If this node is <code>mix:lockable</code> but either does not currently hold a lock or
     * holds a lock for which this Session does not have the correct lock token,
     * then a <code>LockException</code> is thrown.
     * <p/>
     * If this node is not <code>mix:lockable</code> then an <code>UnsupportedRepositoryOperationException</code> is
     * thrown.
     * <p/>
     * Note that either of these exceptions may be thrown when an attempt is made to unlock a node that is locked due
     * to a deep lock above it.
     * <p/>
     * In such cases the unlock method fails because the lock is not held by this node. If the current session does not
     * have sufficient privileges to remove the lock, an <code>AccessDeniedException</code> is thrown.
     * <p/>
     * An <code>InvalidItemStateException</code> is thrown if this node has pending unsaved changes.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @throws UnsupportedRepositoryOperationException
     *                                   if this node is not <code>mix:lockable</code>.
     * @throws LockException             i If this node is <code>mix:lockable</code> but either does not currently hold a lock or
     *                                   holds a lock for which this Session does not have the correct lock token
     * @throws AccessDeniedException     if the current session does not have permission to unlock this node.
     * @throws InvalidItemStateException if this node has pending unsaved changes.
     * @throws RepositoryException       if another error occurs.
     */
    public function unlock();

    /**
     * Returns <code>true</code> if this node holds a lock; otherwise returns <code>false</code>. To <i>hold</i> a
     * lock means that this node has actually had a lock placed on it specifically, as opposed to just having a lock
     * <i>apply</i> to it due to a deep lock held by a node above.
     *
     * @return a <code>boolean</code>.
     * @throws RepositoryException if an error occurs.
     */
    public function holdsLock();

    /**
     * Returns <code>true</code> if this node is locked either as a result of a lock held by this node or by a deep
     * lock on a node above this node; otherwise returns <code>false</code>.
     *
     * @return a <code>boolean</code>.
     * @throws RepositoryException if an error occurs.
     */
    public function isLocked();
}

?>
