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


require_once 'PHPCR/Item.php';
require_once 'PHPCR/Node.php';
require_once 'PHPCR/Session.php';
require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/AccessDeniedException.php';
require_once 'PHPCR/ItemNotFoundException.php';
require_once 'PHPCR/ReferentialIntegrityException.php';
require_once 'PHPCR/nodetype/ConstraintViolationException.php';
require_once 'PHPCR/InvalidItemStateException.php';
require_once 'PHPCR/version/VersionException.php';
require_once 'PHPCR/lock/LockException.php';
require_once 'PHPCR/ItemVisitor.php';


/**
 * The <code>Item</code> is the base interface of <code>Node</code>
 * and <code>Property</code>.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
interface Item
{
    /**
     * Returns the absolute path to this item.
     * If the path includes items that are same-name sibling nodes properties
     * then those elements in the path will include the appropriate
     * "square bracket" index notation (for example, <code>/a/b[3]/c</code>).
     *
     * @return the path of this <code>Item</code>.
     * @throws RepositoryException if an error occurs.
     */
    public function getPath();

    /**
     * Returns the name of this <code>Item</code>. The name of an item is the
     * last element in its path, minus any square-bracket index that may exist.
     * If this <code>Item</code> is the root node of the workspace (i.e., if
     * <code>this.getDepth() == 0</code>), an empty string will be returned.
     * <p>
     *
     * @return the (or a) name of this <code>Item</code> or an empty string
     * if this <code>Item</code> is the root node.
     *
     * @throws RepositoryException if an error occurs.
     */
    public function getName();

    /**
     * Returns the ancestor of the specified depth.
     * An ancestor of depth <i>x</i> is the <code>Item</code> that is <i>x</i>
     * levels down along the path from the root node to <i>this</i>
     * <code>Item</code>.
     * <ul>
     * <li><i>depth</i> = 0 returns the root node.
     * <li><i>depth</i> = 1 returns the child of the root node along the path
     * to <i>this</i> <code>Item</code>.
     * <li><i>depth</i> = 2 returns the grandchild of the root node along the
     * path to <i>this</i> <code>Item</code>.
     * <li>And so on to <i>depth</i> = <i>n</i>, where <i>n</i> is the depth
     * of <i>this</i> <code>Item</code>, which returns <i>this</i>
     * <code>Item</code> itself.
     * </ul>
     * If <i>depth</i> &gt; <i>n</i> is specified then a
     * <code>ItemNotFoundException</code> is thrown.
     * <p>
     *
     * @param depth An integer, 0 &lt;= <i>depth</i> &lt;= <i>n</i> where <i>n</i> is the depth
     * of <i>this</i> <code>Item</code>.
     *
     * @return The ancestor of this
     * <code>Item</code> at the specified <code>depth</code>.
     *
     * @throws ItemNotFoundException if <i>depth</i> &lt; 0 or
     * <i>depth</i> &gt; <i>n</i> where <i>n</i> is the is the depth of
     * this item.
     *
     * @throws AccessDeniedException if the current session does not have
     * sufficient access rights to retrieve the specified node.
     *
     * @throws RepositoryException if another error occurs.
     */
    public function getAncestor( $depth );

    /**
     * Returns the parent of this <code>Item</code>.
     *
     * @return The parent of this <code>Item</code>.
     *
     * @throws ItemNotFoundException if there is no parent.  This only happens
     * if this item is the root node of a workspace.
     *
     * @throws AccessDeniedException if the current session does not have
     * sufficient access rights to retrieve the parent of this item.
     *
     * @throws RepositoryException if another error occurs.
     */
    public function getParent();

    /**
     * Returns the depth of this <code>Item</code> in the workspace tree.
     * Returns the depth below the root node of <i>this</i> <code>Item</code>
     * (counting <i>this</i> <code>Item</code> itself).
     * <ul>
     * <li>The root node returns 0.
     * <li>A property or child node of the root node returns 1.
     * <li>A property or child node of a child node of the root returns 2.
     * <li>And so on to <i>this</i> <code>Item</code>.
     * </ul>
     *
     * @return The depth of this <code>Item</code> in the workspace hierarchy.
     * @throws RepositoryException if an error occurs.
     */
    public function getDepth();

    /**
     * Returns the <code>Session</code> through which this <code>Item</code>
     * was acquired.
     * Every <code>Item</code> can ultimately be traced back through a series
     * of method calls to the call <code>{@link Session#getRootNode}</code>,
     * <code>{@link Session#getItem}</code> or
     * <code>{@link Session#getNodeByUUID}</code>. This method returns that
     * <code>Session</code> object.
     *
     * @return the <code>Session</code> through which this <code>Item</code> was
     * acquired.
     * @throws RepositoryException if an error occurs.
     */
    public function getSession();

    /**
     * Indicates whether this <code>Item</code> is a <code>Node</code> or a
     * <code>Property</code>.
     * Returns <code>true</code> if this <code>Item</code> is a <code>Node</code>;
     * Returns <code>false</code> if this <code>Item</code> is a <code>Property</code>.
     *
     * @return <code>true</code> if this <code>Item</code> is a
     * <code>Node</code>, <code>false</code> if it is a <code>Property</code>.
     */
    public function isNode();

    /**
     * Returns <code>true</code> if this is a new item, meaning that it exists only in transient
     * storage on the <code>Session</code> and has not yet been saved. Within a transaction,
     * <code>isNew</code> on an <code>Item</code> may return <code>false</code> (because the item
     * has been saved) even if that <code>Item</code> is not in persistent storage (because the
     * transaction has not yet been committed).
     * <p>
     * Note that if an item returns <code>true</code> on <code>isNew</code>,
     * then by definition is parent will return <code>true</code> on <code>isModified</code>.
     * <p>
     * Note that in level 1 (that is, read-only) implementations,
     * this method will always return <code>false</code>.
     *
     * @return <code>true</code> if this item is new; <code>false</code> otherwise.
     */
    public function isNew();

    /**
     * Returns <code>true</code> if this <code>Item</code> has been saved but has subsequently
     * been modified through the current session and therefore the state of this item as recorded
     * in the session differs from the state of this item as saved. Within a transaction,
     * <code>isModified</code> on an <code>Item</code> may return <code>false</code> (because the
     * <code>Item</code> has been saved since the modification) even if the modification in question
     * is not in persistent storage (because the transaction has not yet been committed).
     * <p>
     * Note that in level 1 (that is, read-only) implementations,
     * this method will always return <code>false</code>.
     *
     * @return <code>true</code> if this item is modified; <code>false</code> otherwise.
     */
    public function isModified();

    /**
     * Returns <code>true</code> if this <code>Item</code> object
     * (the Java object instance) represents the same actual repository item as the
     * object <code>otherItem</code>.
     * <p>
     * This method does not compare the <i>states</i> of the two items. For example,
     * if two <code>Item</code> objects representing the same actual repository item
     * have been retrieved through two different sessions and one has been modified,
     * then this method will still return <code>true</code> for these two objects.
     * Note that if two <code>Item</code> objects representing the same repository item
     * are retrieved through the <i>same</i> session they will always reflect the
     * same state (see section 6.3.1 <i>Reflecting Item State</i> in the specification
     * document) so comparing state is not an issue.
     *
     * @param otherItem the <code>Item</code> object to be tested for identity with this <code>Item</code>.
     *
     * @return <code>true</code> if this <code>Item</code> object and <code>otherItem</code> represent the same actual repository
     * item; <code>false</code> otherwise.
     */
    public function isSame( $otherItem );

    /**
     * Accepts an <code>ItemVistor</code>.
     * Calls the appropriate <code>ItemVistor</code>
     * <code>visit</code> method of the according to whether <i>this</i>
     * <code>Item</code> is a <code>Node</code> or a <code>Property</code>.
     *
     * @param visitor The ItemVisitor to be accepted.
     *
     * @throws RepositoryException if an error occurs.
     */
    public function accept( ItemVisitor $visitor );

    /**
     * Validates all pending changes currently recorded in this <code>Session</code> that apply to this <code>Item</code>
     * or any of its descendants (that is, the subtree rooted at this Item). If validation of <i>all</i>
     * pending changes succeeds, then this change information is cleared from the <code>Session</code>.
     * If the <code>save</code> occurs outside a transaction, the changes are persisted and thus
     * made visible to other <code>Sessions</code>. If the <code>save</code> occurs within a transaction,
     * the changes are not persisted until the transaction is committed.
     * <p>
     * If validation fails, then no pending changes are saved and they remain recorded on the <code>Session</code>.
     * There is no best-effort or partial save.
     * <p>
     * When an item is saved the item in persistent storage to which pending changes are written is
     * determined as follows:
     * <ul>
     *   <li>
     *     If the transient item has a UUID, then the changes are written to the persistent item with the same UUID.
     *   </li>
     *   <li>
     *     If the transient item does not have a UUID then its nearest ancestor with a UUID, or the root node
     *     (whichever occurs first) is found, and the relative path from the node in persistent node with that UUID is
     *     used to determine the item in persistent storage to which the changes are to be written.
     *   </li>
     * </ul>
     * As a result of these rules, a <code>save</code> of an item that has a UUID will succeed even if that item has,
     * in the meantime, been moved in persistent storage to a new location (that is, its path has changed). However, a
     * <code>save</code> of a non-UUID item will fail (throwing an <code>InvalidItemStateException</code>) if it has,
     * in the meantime, been moved in persistent storage to a new location. A <code>save</code> of a non-UUID item will
     * also fail if it has, in addition to being moved, been replaced in its original position by a UUID-bearing item.
     * <p>
     * Note that <code>save</code> uses the same rules to match items between transient storage and persistent storage
     * as {@link Node#update} does to match nodes between two workspaces.
     * <p>
     * An <code>AccessDeniedException</code> will be thrown if any of the changes
     * to be persisted would violate the access privileges of this
     * <code>Session</code> or if. Also thrown if  any of the
     * changes to be persisted would cause the removal of a node that is currently
     * referenced by a <code>REFERENCE</code> property that this Session
     * <i>does not</i> have read access to.
     * <p>
     * A <code>ConstraintViolationException</code> will be thrown if any of the
     * changes to be persisted would violate a node type restriction.
     * Additionally, a repository may use this exception to enforce
     * implementation- or configuration-dependant restrictions.
     * <p>
     * An <code>InvalidItemStateException</code> is thrown if any of the
     * changes to be persisted conflicts with a change already persisted
     * through another session and the implementation is such that this
     * conflict can only be detected at save-time and therefore was not
     * detected earlier, at change-time.
     * <p>
     * A <code>ReferentialIntegrityException</code> is thrown if any of the
     * changes to be persisted would cause the removal of a node that is currently
     * referenced by a <code>REFERENCE</code> property that this <code>Session</code>
     * has read access to.
     * <p>
     * A <code>VersionException</code> is thrown if the <code>save</code> would make a result in
     * a change to persistent storage that would violate the read-only status of a
     * checked-in node.
     * <p>
     * A <code>LockException</code> is thrown if the <code>save</code> would result in a
     * change to persistent storage that would violate a lock.
     * <p>
     * A <code>RepositoryException</code> will be thrown if another error
     * occurs.
     *
     * @throws AccessDeniedException if any of the changes to be persisted would violate
     * the access privileges of the this <code>Session</code>. Also thrown if  any of the
     * changes to be persisted would cause the removal of a node that is currently
     * referenced by a <code>REFERENCE</code> property that this Session
     * <i>does not</i> have read access to.
     * @throws ConstraintViolationException if any of the changes to be persisted would
     * violate a node type or restriction. Additionally, a repository may use this
     * exception to enforce implementation- or configuration-dependent restrictions.
     * @throws InvalidItemStateException if any of the
     * changes to be persisted conflicts with a change already persisted
     * through another session and the implementation is such that this
     * conflict can only be detected at save-time and therefore was not
     * detected earlier, at change-time.
     * @throws ReferentialIntegrityException if any of the
     * changes to be persisted would cause the removal of a node that is currently
     * referenced by a <code>REFERENCE</code> property that this <code>Session</code>
     * has read access to.
     * @throws VersionException if the <code>save</code> would make a result in
     * a change to persistent storage that would violate the read-only status of a
     * checked-in node.
     * @throws LockException if the <code>save</code> would result in a
     * change to persistent storage that would violate a lock.
     * @throws RepositoryException if another error occurs.
     */
    public function save();

    /**
     * If <code>keepChanges</code> is <code>false</code>, this method discards all pending changes
     * currently recorded in this <code>Session</code> that apply to this Item or any of its descendants
     * (that is, the subtree rooted at this Item)and returns all items to reflect the current
     * saved state. Outside a transaction this state is simple the current state of persistent storage.
     * Within a transaction, this state will reflect persistent storage as modified by changes that have
     * been saved but not yet committed.
     * <p>
     * If <code>keepChanges</code> is true then pending change are not discarded but items that do not
     * have changes pending have their state refreshed to reflect the current saved state, thus revealing
     * changes made by other sessions.
     * <p>
     * An <code>InvalidItemStateException</code> is thrown if this <code>Item</code> object represents a
     * workspace item that has been removed (either by this session or another).
     *
     * @throws InvalidItemStateException if this
     * <code>Item</code> object represents a workspace item that has been
     * removed (either by this session or another).
     *
     * @throws RepositoryException if another error occurs.
     */
    public function refresh( $keepChanges );

    /**
     * Removes <code>this</code> item (and its subtree).
     * <p/>
     * To persist a removal, a <code>save</code> must be
     * performed that includes the (former) parent of the
     * removed item within its scope.
     * <p/>
     * A <code>ConstraintViolationException</code> will be thrown immediately if
     * removing the specified item would violate a node type or implementation-specific
     * constraint and that violation can be detected at the time that the <code>remove</code>
     * method is called. Otherwise, a <code>ConstraintViolationException</code> will be
     * thrown on <code>save</code> if the violation can only be detected later, at the time that
     * the <code>save</code> method is called. Note that implementations may differ as to which
     * constraints are enforced immediately and which on <code>save</code>.
     * <p/>
     * A <code>ReferentialIntegrityException</code> will be thrown on <code>save</code>
     * if the item in question is currently the target of a <code>REFERENCE</code>
     * property elsewhere in the workspace and the current <code>Session</code>
     * has read access to that <code>REFERENCE</code> property.
     * <p/>
     * An <code>AccessDeniedException</code> will be thrown on <code>save</code>
     * if the item in question is currently the target of a <code>REFERENCE</code>
     * property elsewhere in the workspace and the current <code>Session</code>
     * <i>does not</i> have read access to that REFERENCE property.
     * <p/>
     * A <code>VersionException</code> is thrown immediately (not on <code>save</code>)
     * if the parent node of this item is versionable and
     * currently checked-in or is non-versionable and its nearest versionable ancestor is checked in.
     * Note that if this item itself is checked-in
     * (but not its parent) then it can be removed.
     * <p/>
     * A <code>LockException</code> is thrown if a lock prevents the removal of this item.
     *
     * @throws VersionException if the parent of this node is versionable and
     * currently checked-in (and therefore read-only).
     * @throws LockException if a lock prevents the removal of this item.
     * @throws ConstraintViolationException if removing the specified item would violate a node type or
     * implementation-specific constraint and that violation is detected at the
     * time of calling the <code>remove</code> method.
     * @throws RepositoryException if an error occurs.
     */
    public function remove();
}

?>