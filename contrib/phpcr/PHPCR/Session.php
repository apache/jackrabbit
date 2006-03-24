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


require_once 'PHPCR/Repository.php';
require_once 'PHPCR/Workspace.php';
require_once 'PHPCR/Session.php';
require_once 'PHPCR/Credentials.php';
require_once 'PHPCR/LoginException.php';
require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/Node.php';
require_once 'PHPCR/Item.php';
require_once 'PHPCR/ItemNotFoundException.php';
require_once 'PHPCR/PathNotFoundException.php';
require_once 'PHPCR/ItemExistsException.php';
require_once 'PHPCR/PathNotFoundException.php';
require_once 'PHPCR/version/VersionException.php';
require_once 'PHPCR/nodetype/ConstraintViolationException.php';
require_once 'PHPCR/lock/LockException.php';
require_once 'PHPCR/AccessDeniedException.php';
require_once 'PHPCR/InvalidItemStateException.php';
require_once 'PHPCR/IOException.php';
require_once 'PHPCR/InvalidSerializedDataException.php';
require_once 'PHPCR/SAXException.php';
require_once 'PHPCR/NamespaceException.php';


/**
 * The <code>Session</code> object provides read and (in level 2) write access to the content of a
 * particular workspace in the repository.
 * <p/>
 * The <code>Session</code> object is returned by {@link Repository#login}.
 * It encapsulates both the authorization settings of a particular user (as specified by the
 * passed <code>Credentials</code>)and a binding to the workspace specified by the
 * <code>workspaceName</code> passed on <code>login</code>.
 * <p/>
 * Each <code>Session</code> object is associated one-to-one with a <code>Workspace</code> object.
 * The <code>Workspace</code> object represents a "view" of an actual repository workspace entity
 * as seen through the authorization settings of its associated <code>Session</code>.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
interface Session
{
    /**
     * Returns the <code>Repository</code> object through which this session was
     * acquired.
     *
     * @return a <code>{@link Repository}</code> object.
     */
    public function getRepository();

    /**
     * Gets the user ID that was used to acquire this session. This method is free to return an
     * "anonymous user id" or <code>null</code> if the <code>Credentials</code> used to acquire this session happens not
     * to have provided a real user ID (for example,  if instead of <code>SimpleCredentials</code> some other
     * implementation of <code>Credentials</code> was used).
     *
     * @return the user id from the credentials used to acquire this session.
     */
    public function getUserID();

    /**
     * Returns the value of the named attribute as an <code>Object</code>, or <code>null</code> if no attribute of the
     * given name exists. See {@link Session#getAttributeNames}.
     *
     * @param name the name of an attribute passed in the credentials used to acquire this session.
     *
     * @return the value of the attribute.
     */
    public function getAttribute( $name );

   /**
    * Returns the names of the attributes set in this session as a result of the <code>Credentials</code> that were
    * used to acquire it. Not all <code>Credentials</code> implementations will contain attributes (though, for example,
    * <code>SimpleCredentials</code> does allow for them). This method returns an empty array if the
    * <code>Credentials</code> instance used to acquire this <code>Session</code> did not provide attributes.
    *
    * @return A string array containing the names of all attributes passed in the credentials used to acquire this session.
    */
    public function getAttributeNames();

    /**
     * Returns true if this Session object is usable by the client. Otherwise,
     * returns false. A usable Session is one that is neither logged-out,
     * timed-out nor in any other way disconnected from the repository.
     *
     * @return true if this Session is usable, false otherwise.
     */
    public function isLive();

    /**
     * This method returns a ValueFactory that is used to create Value
     * objects for use in setting of repository properties.
     *
     * If writing to the repository is not supported (because this is
     * a level 1-only implementation, for example) an
     * UnsupportedRepositoryOperationException will be thrown.
     *
     * @return a <code>{@link ValueFactory}</code> object
     * @throws UnsupportedRepositoryOperationException if writing to the
     * repository is not supported.
     * @throws RepositoryException if another error occurs
     */
    public function getValueFactory();

    /**
     * Returns the <code>Workspace</code> attached to this <code>Session</code>.
     *
     * @return a <code>{@link Workspace}</code> object.
     */
    public function getWorkspace();

    /**
     * Returns a new session in accordance with the specified (new) Credentials.
     * Allows the current user to "impersonate" another using incomplete
     * credentials (perhaps including a user name but no password, for example),
     * assuming that their original session gives them that permission.
     * <p/>
     * The new <code>Session</code> is tied to a new <code>Workspace</code> instance.
     * In other words, <code>Workspace</code> instances are not re-used. However,
     * the <code>Workspace</code> instance returned represents the same actual
     * persistent workspace entity in the repository as is represented by the
     * <code>Workspace</code> object tied to this <code>Session</code>.
     * <p/>
     * Throws a <code>LoginException</code> if the current session does not have
     * sufficient rights.
     *
     * @param credentials A <code>Credentials</code> object
     * @return a <code>Session</code> object
     * @throws LoginException if the current session does not have
     * sufficient rights.
     * @throws RepositoryException if another error occurs.
     */
    public function impersonate( Credentials $credentials );

    /**
     * Returns the root node of the workspace.
     * The root node, "/", is the main access point to the content of the
     * workspace.
     *
     * @return The root node of the workspace: a <code>Node</code> object.
     *
     * @throws RepositoryException if an error occurs.
     */
    public function getRootNode();

    /**
     * Returns the node specifed by the given UUID. Only applies to nodes that
     * expose a UUID, in other words, those of mixin node type
     * <code>mix:referenceable</code>
     *
     * @param uuid A universally unique identifier.
     * @return A <code>Node</code>.
     * @throws ItemNotFoundException if the specified UUID is not found.
     * @throws RepositoryException if another error occurs.
     */
    public function getNodeByUUID( $uuid );

    /**
     * Returns the item at the specified absolute path in the workspace.
     *
     * @param absPath An absolute path.
     * @return An <code>Item</code>.
     * @throws PathNotFoundException if the specified path cannot be found.
     * @throws RepositoryException if another error occurs.
     */
    public function getItem( $absPath );

    /**
     * Returns <code>true</code> if an item exists at <code>absPath</code>; otherwise returns <code>false</code>.
     * Also returns <code>false</code> if the specified <code>absPath</code> is malformed.
     *
     * @param absPath an absolute path
     * @return <code>true</code> if an item exists at <code>absPath</code>; otherwise returns <code>false</code>.
     * @throws RepositoryException if an error occurs.
     */
    public function itemExists( $absPath );

    /**
     * Moves the node at <code>srcAbsPath</code> (and its entire subtree) to the new location
     * at <code>destAbsPath</code>.
     * <p>
     * In order to persist the change, a <code>save</code>
     * must be called on the parents of both the source and destination
     * locations.
     * <p/>
     * A <code>ConstraintViolationException</code> is thrown if a node-type
     * or other constraint violation is detected immediately. Otherwise,
     * if the violation is only detected later, at <code>save</code>, then a
     * <code>ConstraintViolationException</code> is thrown by that method.
     * Implementations may differ as to which constraints are enforced immediately,
     * and which on <code>save</code>.
     * <p>
     * As well, a <code>ConstraintViolationException</code> will be thrown on
     * <code>save</code> if an attempt is made to seperately <code>save</code>
     * either the source or destination node.
     * <p>
     * Note that this behaviour differs from that of
     * {@link Workspace#move}, which operates directly in the persistent
     * workspace and does not require a <code>save</code>.
     * <p/>
     * The <code>destAbsPath</code> provided must not
     * have an index on its final element. If it does then a <code>RepositoryException</code>
     * is thrown. Strictly speaking, the <code>destAbsPath</code> parameter is actually an <i>absolute path</i>
     * to the parent node of the new location, appended with the new <i>name</i> desired for the
     * moved node. It does not specify a position within the child node
     * ordering (if such ordering is supported). If ordering is supported by the node type of
     * the parent node of the new location, then the newly moved node is appended to the end of the
     * child node list.
     * <p>
     * This method cannot be used to move just an individual property by itself.
     * It moves an entire node and its subtree (including, of course, any properties
     * contained therein).
     * <p>
     * If no node exists at <code>srcAbsPath</code> or no node exists one level above <code>destAbsPath</code>
     * (i.e. there is no node that will serve as the parent of the moved item) then a
     * <code>PathNotFoundException</code> is thrown.
     * <p>
     * An <code>ItemExistException</code> is thrown if a property already exists at
     * <code>destAbsPath</code> or a node already exist there, and same name
     * siblings are not allowed.
     * <p>
     * A <code>VersionException</code> is thrown if the parent node of <code>destAbsPath</code> or the parent node of
     * <code>srcAbsPath</code> is versionable and checked-in, or is non-verionable and its nearest versionable
     * ancestor is checked-in.
     * <p>
     * A <code>LockException</code> is thrown if the <code>move</code> operation would violate a lock.
     *
     * @param srcAbsPath the root of the subtree to be moved.
     * @param destAbsPath the location to which the subtree is to be moved.
     * @throws ItemExistsException if a property already exists at
     * <code>destAbsPath</code> or a node already exist there, and same name
     * siblings are not allowed.
     * @throws PathNotFoundException if either <code>srcAbsPath</code> or <code>destAbsPath</code> cannot be found.
     * @throws VersionException if the parent node of <code>destAbsPath</code> or the parent node of <code>srcAbsPath</code>
     * is versionable and checked-in, or or is non-verionable and its nearest versionable ancestor is checked-in.
     * @throws ConstraintViolationException if a node-type or other constraint violation is detected immediately.
     * @throws LockException if the move operation would violate a lock.
     * @throws RepositoryException if the last element of <code>destAbsPath</code> has an index or if another error occurs.
     */
    public function move( $srcAbsPath, $destAbsPath );

    /**
     * Validates all pending changes currently recorded in this <code>Session</code>. If validation of all
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
     * A <code>LockException</code> is thrown if any of the changes to be
     * persisted would violate a lock.
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
     * @throws LockException if any of the changes to be persisted would violate a lock.
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
     * currently recorded in this <code>Session</code> and returns all items to reflect the current
     * saved state. Outside a transaction this state is simply the current state of persistent storage.
     * Within a transaction, this state will reflect persistent storage as modified by changes that have
     * been saved but not yet committed.
     * <p>
     * If <code>keepChanges</code> is true then pending change are not discarded but items that do not
     * have changes pending have their state refreshed to reflect the current saved state, thus revealing
     * changes made by other sessions.
     *
     * @throws RepositoryException if an error occurs.
     */
    public function refresh( $keepChanges );

    /**
     * Returns <code>true</code> if this session holds pending (that is, unsaved) changes;
     * otherwise returns <code>false</code>.
     *
     * @return a boolean
     * @throws RepositoryException if an error occurs
     */
    public function hasPendingChanges();

    /**
     * Determines whether this <code>Session</code> has permission to perform the specified actions
     * at the specified <code>absPath</code>. This method quietly returns if the access request is
     * permitted, or throws Exception otherwise.
     * <p/>
     * The <code>actions</code> parameter is a comma separated list of action strings. The following
     * action strings are defined:
     * <ul>
     * <li>
     * <code>add_node</code>: If <code>checkPermission(path, "add_node")</code> returns quietly, then
     * this <code>Session</code> has permission to add a node at <code>path</code>, otherwise permission
     * is denied.
     * </li>
     * <li>
     * <code>set_property</code>: If <code>checkPermission(path, "set_property")</code> returns quietly,
     * then this <code>Session</code> has permission to set (add or change) a property at <code>path</code>,
     * otherwise permission is denied.
     * </li>
     * <li>
     * <code>remove</code>: If <code>checkPermission(path, "remove")</code> returns quietly, then this
     * <code>Session</code> has permission to remove an item at <code>path</code>, otherwise permission is denied.
     * </li>
     * <li>
     * <code>read</code>: If <code>checkPermission(path, "read")</code> returns quietly, then this
     * <code>Session</code> has permission to retrieve (and read the value of, in the case of a property)
     * an item at <code>path</code>, otherwise permission is denied.
     * </li>
     * </ul>
     * When more than one action is specified in the <code>actions</code> parameter, this method will only
     * return quietly if this <code>Session</code> has permission to perform <i>all</i> of the listed
     * actions at the specified path.
     * <p/>
     * The information returned through this method will only reflect access control policies
     * and not other restrictions that may exist. For example, even though <code>checkPermission</code>
     * may indicate that a particular <code>Session</code> may add a property at <code>/A/B/C</code>,
     * the node type of the node at <code>/A/B</code> may prevent the addition of a property called
     * <code>C</code>.
     *
     * @throws Exception
     */
    public function checkPermission( $absPath, $actions );

    /**
     * Returns a <code>ContentHandler</code> which can be used to push SAX events into the repository.
     * If the incoming XML stream (in the form of SAX events) does not appear to be a JCR system view XML document then it is
     * interpreted as a document view XML document.
     * <p>
     * The incoming XML is deserialized into a subtree of items immediately below the node at
     * <code>parentAbsPath</code>.
     * <p>
     * The special properties <code>jcr:primaryType</code> and <code>jcr:mixinTypes</code> are
     * taken into account during deserialization in order to determine the node types of the newly created nodes.
     * <p>
     * This method simply returns the <code>ContentHandler</code> without altering the state of the
     * repository; the actual deserialization is done through the methods of the <code>ContentHandler</code>.
     * <p>
     * As SAX events are fed into the <code>ContentHandler</code>, the tree of new items is built
     * in the transient storage of the <code>Session</code>. In order to persist the new content,
     * <code>save</code> must be called. The advantage of this through-the-session method is that
     * invalid data can be imported, fixed and then saved. The disadvantage is that a large
     * import will result in a large cache of pending nodes in the <code>Session</code>. See
     * {@link Workspace#getImportContentHandler} for a version of this method that <i>does not</i>
     * go through the <code>Session</code>.
     * <p>
     * A <code>PathNotFoundException</code> is thrown if no node exists at <code>parentAbsPath</code>.
     * <p>
     * A <code>ConstraintViolationException</code> is thrown if the new subtree cannot be added to the node at
     * <code>parentAbsPath</code> due to node-type or other implementation-specific constraints, and this can
     * be determined before the first SAX event is sent. Unlike {@link Workspace#getImportContentHandler},
     * this method does not enforce node type constraints by throwing <code>SAXException</code>s during
     * deserialization. These constraints are checked on <code>save</code>.
     * <p>
     * A <code>VersionException</code> is thrown if the node at <code>parentAbsPath</code> is versionable
     * and checked-in, or its nearest versionable ancestor is checked-in.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the addition of the subtree.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param parentAbsPath the absolute path of a node under which (as child) the imported subtree will be built.
     * @param mode a four-value flag that governs how incoming UUIDs are handled.
     *
     * @return an org.xml.sax.ContentHandler whose methods may be called to feed SAX events into the deserializer.
     *
     * @throws PathNotFoundException if no node exists at <code>parentAbsPath</code>.
     * @throws ConstraintViolationException if the new subtree cannot be added to the node at
     * <code>parentAbsPath</code> due to node-type or other implementation-specific constraints, and this can
     * be determined before the first SAX event is sent.
     * @throws VersionException if the node at <code>parentAbsPath</code> is versionable
     * and checked-in, or its nearest versionable ancestor is checked-in.
     * @throws LockException if a lock prevents the addition of the subtree.
     * @throws RepositoryException if another error occurs.
     */
    public function getImportContentHandler( $parentAbsPath, $mode );

    /**
     * Deserializes an XML document and adds the resulting item subtree as a child of the node at
     * <code>parentAbsPath</code>. Requires a <code>save</code> to persist.
     * <p>
     * If the incoming XML stream does not appear to be a JCR system view XML document then it is interpreted as a
     * <b>document view</b> XML document.
     * <p>
     * The special properties <code>jcr:primaryType</code> and <code>jcr:mixinTypes</code> are
     * taken into account during deserialization in order to determine the node types of the newly
     * created nodes.
     * <p>
     * The tree of new items is built in the transient storage of the <code>Session</code>.
     * In order to persist the new content, <code>save</code> must be called. The advantage
     * of this through-the-session method is that invalid data can be imported, fixed and
     * then saved. The disadvantage is that a large import will result in a large cache
     * of pending nodes in the <code>Session</code>. See {@link Workspace#importXML}
     * for a version of this method that does not go through the <code>Session</code>.
     * <p>
     * An <code>IOException</code> is thrown if an I/O error occurs.
     * <p>
     * If no node exists at <code>parentAbsPath</code>, a <code>PathNotFoundException</code>
     * is thrown.
     * <p>
     * If deserialization would overwrite an exisiting item, an <code>ItemExistsException</code>
     * is thrown.
     * <p>
     * A <code>ConstraintViolationException</code> is thrown if the new subtree cannot be added to the node at
     * <code>parentAbsPath</code> due to node-type or other implementation-specific constraints, and this can
     * be determined before deserialization begins. Unlike {@link Workspace#getImportContentHandler},
     * this method does not enforce node type constraints by during
     * deserialization. These constraints are checked on <code>save</code>.
     * <p>
     * A <code>VersionException</code> is thrown if the node at <code>parentAbsPath</code> is versionable
     * and checked-in, or its nearest versionable ancestor is checked-in.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the addition of the subtree.
     * <p>
     * If the incoming XML is not valid, an <code>InvalidSerializedDataException</code> is thrown.
     * <p>
     * If another error occurs a <code>RepositoryException</code> is thrown.
     *
     * @param parentAbsPath the absolute path of the node below which the deserialized subtree is added.
     * @param in The <code>Inputstream</code> from which the XML to be deserilaized is read.
     * @param mode a four-value flag that governs how incoming UUIDs are handled.
     *
     * @throws IOException if an error during an I/O operation occurs.
     * @throws PathNotFoundException if no node exists at <code>parentAbsPath</code>.
     * @throws ItemExistsException if deserialization would overwrite an exisiting item.
     * @throws ConstraintViolationException if the new subtree cannot be added to the node at
     * <code>parentAbsPath</code> due to node-type or other implementation-specific constraints, and this can
     * be determined before deserialization begins.
     * @throws VersionException if the node at <code>parentAbsPath</code> is versionable
     * and checked-in, or its nearest versionable ancestor is checked-in.
     * @throws InvalidSerializedDataException if incoming stream is not a valid XML document.
     * @throws LockException if a lock prevents the addition of the subtree.
     * @throws RepositoryException is another error occurs.
     */
    public function importXML( $parentAbsPath, $in, $mode );

    /**
     * Serializes the node (and if <code>noRecurse</code> is <code>false</code>,
     * the whole subtree) at <code>absPath</code> into a series of SAX events by
     * calling the methods of the supplied <code>org.xml.sax.ContentHandler</code>.
     * The resulting XML is in the system view form. Note that <code>absPath</code>
     * must be the path of a node, not a property.
     * <p>
     * If <code>skipBinary</code> is <code>true</code> then any properties of
     * <code>PropertyType.BINARY</code> will be ignored and will not appear in
     * the serialized output. If <code>skipBinary</code> is false then the actual
     * value of each <code>BINARY</code> property is recorded using Base64 encoding.
     * <p>
     * If <code>noRecurse</code> is true then only the node at
     * <code>absPath</code> and its properties, but not its child nodes, are
     * serialized. If <code>noRecurse</code> is <code>false</code> then the entire subtree
     * rooted at <code>absPath</code> is serialized.
     * <p>
     * If the user lacks read access to some subsection of the specified tree,
     * that section simply does not get serialized, since, from the user's
     * point of view, it is not there.
     * <p>
     * The serialized output will reflect the state of the current workspace as
     * modified by the state of this <code>Session</code>. This means that
     * pending changes (regardless of whether they are valid according to
     * node type constraints) and the current session-mapping of namespaces
     * are reflected in the output.
     * <p>
     * A <code>PathNotFoundException</code> is thrown if no node exists at <code>absPath</code>.
     * <p>
     * A <code>SAXException</code> is thrown if an error occurs while feeding events to the
     * <code>ContentHandler</code>.
     *
     * @param absPath The path of the root of the subtree to be serialized.
     * This must be the path to a node, not a property
     * @param contentHandler The  <code>org.xml.sax.ContentHandler</code> to
     * which the SAX events representing the XML serialization of the subtree
     * will be output.
     * @param skipBinary A <code>boolean</code> governing whether binary
     * properties are to be serialized.
     * @param noRecurse A <code>boolean</code> governing whether the subtree at
     * absPath is to be recursed.
     *
     * @throws PathNotFoundException if no node exists at <code>absPath</code>.
     * @throws org.xml.sax.SAXException if an error occurs while feeding events to the
     * <code>org.xml.sax.ContentHandler</code>.
     * @throws RepositoryException if another error occurs.
     */
    public function exportSystemView( $absPath, $out, $skipBinary, $noRecurse );

    /**
     * Serializes the node (and if <code>noRecurse</code> is <code>false</code>,
     * the whole subtree) at <code>absPath</code> into a series of SAX events by
     * calling the methods of the supplied <code>org.xml.sax.ContentHandler</code>.
     * The resulting XML is in the document view form. Note that <code>absPath</code>
     * must be the path of a node, not a property.
     * <p>
     * If <code>skipBinary</code> is <code>true</code> then any properties of
     * <code>PropertyType.BINARY</code> will be ignored and will not appear in
     * the serialized output. If <code>skipBinary</code> is false then the actual
     * value of each <code>BINARY</code> property is recorded using Base64 encoding.
     * <p>
     * If <code>noRecurse</code> is true then only the node at
     * <code>absPath</code> and its properties, but not its child nodes, are
     * serialized. If <code>noRecurse</code> is <code>false</code> then the entire subtree
     * rooted at <code>absPath</code> is serialized.
     * <p>
     * If the user lacks read access to some subsection of the specified tree,
     * that section simply does not get serialized, since, from the user's
     * point of view, it is not there.
     * <p>
     * The serialized output will reflect the state of the current workspace as
     * modified by the state of this <code>Session</code>. This means that
     * pending changes (regardless of whether they are valid according to
     * node type constraints) and the current session-mapping of namespaces
     * are reflected in the output.
     * <p>
     * A <code>PathNotFoundException</code> is thrown if no node exists at <code>absPath</code>.
     * <p>
     * A <code>SAXException</code> is thrown if an error occurs while feeding events to the
     * <code>ContentHandler</code>.
     *
     * @param absPath The path of the root of the subtree to be serialized.
     * This must be the path to a node, not a property
     * @param out The <code>OutputStream</code> to which the XML
     * serialization of the subtree will be output.
     * @param skipBinary A <code>boolean</code> governing whether binary
     * properties are to be serialized.
     * @param noRecurse A <code>boolean</code> governing whether the subtree at
     * absPath is to be recursed.
     *
     * @throws PathNotFoundException if no node exists at <code>absPath</code>.
     * @throws org.xml.sax.SAXException if an error occurs while feeding events to the
     * <code>org.xml.sax.ContentHandler</code>.
     * @throws RepositoryException if another error occurs.
     */
    public function exportDocumentView( $absPath, $out, $skipBinary, $noRecurse );

    /**
     * Within the scope of this session, rename a persistently registered
     * namespace URI to the new prefix.  The renaming only affects operations
     * done through this session. To clear all renamings the client must acquire
     * a new session.
     * <p>
     * Note that a prefix that is currently mapped in the global namespace registry
     * to some URI cannot be remapped to a new URI using this method, since this would
     * make any content stored using the old URI unreadable. An attempt to do
     * this will throw a <code>NamespaceException</code>.
     * <p>
     * A <code>NamespaceException</code> will be thrown if an attempt is made to remap a URI to a
     * prefix beginning with "<code>xml</code>". These prefixes are reserved by the XML
     * specification.
     * <p>
     * A <code>NamespaceException</code> will also be thrown if
     * the specified uri is not among those registered in the NamespaceRegistry.
     *
     * @param prefix a string
     * @param uri a string
     * @throws NamespaceException if the specified uri is not registered or an attempt is made to remap to an illegal prefix.
     * @throws RepositoryException if another error occurs.
     */
    public function setNamespacePrefix( $prefix, $uri );

    /**
     * Returns all prefixes currently set for this session. This includes all
     * those registered in the <code>NamespaceRegistry</code> but <i>not
     * over-ridden</i> by a <code>Session.setNamespacePrefix</code>, plus those
     * currently set locally by <code>Session.setNamespacePrefix</code>.
     *
     * @throws RepositoryException if an error occurs
     * @return a string array
     */
    public function getNamespacePrefixes();

    /**
     * For a given prefix, returns the URI to which it is mapped as currently
     * set in this <code>Session</code>. If the prefix is unknown, a <code>NamespaceException</code> is thrown.
     *
     * @param prefix a string
     * @return a string
     * @throws NamespaceException if the prefix is unknown.
     * @throws RepositoryException if another error occurs
     */
    public function getNamespaceURI( $prefix );

    /**
     * Returns the prefix to which the given URI is mapped
     *
     * @param uri a string
     * @return a string
     * @throws NamespaceException if the URI is unknown.
     * @throws RepositoryException if another error occurs
     */
    public function getNamespacePrefix( $uri );

    /**
     * Releases all resources associated with this <code>Session</code>. This method should be called when a
     * <code>Session</code> is no longer needed.
     */
    public function logout();

    /**
     * Adds the specified lock token to this session. Holding a lock token allows the <code>Session</code> object of the
     * lock owner to alter nodes that are locked by the lock specified by that particular lock token.
     *
     * @param lt a lock token (a string)
     */
    public function addLockToken( $lt );

    /**
     * Returns an array containing all lock tokens currently held by this session.
     *
     * @return an array of lock tokens (strings)
     */
    public function getLockTokens();

    /**
     * Removes the specified lock token from this session.
     * @param lt a lock token (a string)
     */
    public function removeLockToken( $lt );
}

?>