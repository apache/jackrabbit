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


require_once 'PHPCR/InvalidItemStateException.php';
require_once 'PHPCR/NoSuchWorkspaceException.php';
require_once 'PHPCR/nodetype/ConstraintViolationException.php';
require_once 'PHPCR/version/VersionException.php';
require_once 'PHPCR/AccessDeniedException.php';
require_once 'PHPCR/PathNotFoundException.php';
require_once 'PHPCR/ItemExistsException.php';
require_once 'PHPCR/lock/LockException.php';
require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/UnsupportedRepositoryOperationException.php';
/* require_once 'PHPCR/version/Version.php'; */
require_once 'PHPCR/IOException.php';
require_once 'PHPCR/InvalidSerializedDataException.php';
require_once 'PHPCR/query/QueryManager.php';
require_once 'PHPCR/NamespaceRegistry.php';
require_once 'PHPCR/nodetype/NodeTypeManager.php';
require_once 'PHPCR/observation/ObservationManager.php';


/**
 * Represents a view onto the the content repository.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
interface Workspace
{
    /**
     * A constant used as the value of the flag <code>uuidBehavior</code> in
     * the methods {@link #getImportContentHandler} and {@link #importXML}.
     * See those methods for details.
     */
    const IMPORT_UUID_CREATE_NEW = 0;

    /**
     * A constant used as the value of the flag <code>uuidBehavior</code> in
     * the methods {@link #getImportContentHandler} and {@link #importXML}.
     * See those methods for details.
     */
    const IMPORT_UUID_COLLISION_REMOVE_EXISTING = 1;

    /**
     * A constant used as the value of the flag <code>uuidBehavior</code> in
     * the methods {@link #getImportContentHandler} and {@link #importXML}.
     * See those methods for details.
     */
    const IMPORT_UUID_COLLISION_REPLACE_EXISTING = 2;

    /**
     * A constant used as the value of the flag <code>uuidBehavior</code> in
     * the methods {@link #getImportContentHandler} and {@link #importXML}.
     * See those methods for details.
     */
    const IMPORT_UUID_COLLISION_THROW = 3;


    /**
     * Returns the <code>Session</code> object through which this <code>Workspace</code>
     * object was acquired.
     *
     * @return a <code>{@link Session}</code> object.
     */
    public function getSession();

    /**
     * Returns the name of the actual persistent workspace represented by this
     * <code>Workspace</code> object.
     *
     * @return the name of this workspace.
     */
    public function getName();

    /**
     * This method copies the subtree at <code>srcAbsPath</code> in <code>srcWorkspace</code>
     * to <code>destAbsPath</code> in <code>this</code> workspace. Unlike <code>clone</code>,
     * this method <i>does</i> assign new UUIDs to the new copies of referenceable nodes.
     * This operation is performed entirely within the persistent workspace, it does not involve
     * transient storage and therefore does not require a <code>save</code>.
     * <p/>
     * The <code>destAbsPath</code> provided must not
     * have an index on its final element. If it does then a <code>RepositoryException</code>
     * is thrown. Strictly speaking, the <code>destAbsPath</code> parameter is actually an <i>absolute path</i>
     * to the parent node of the new location, appended with the new <i>name</i> desired for the
     * copied node. It does not specify a position within the child node
     * ordering. If ordering is supported by the node type of
     * the parent node of the new location, then the new copy of the node is appended to the end of the
     * child node list.
     * <p/>
     * This method cannot be used to copy just an individual property by itself.
     * It copies an entire node and its subtree (including, of course, any properties contained therein).
     * <p/>
     * A <code>NoSuchWorkspaceException</code> is thrown if <code>srcWorkspace</code> does not
     * exist or if the current Session does not have permission to access it.
     * <p/>
     * A <code>ConstraintViolationException</code> is thrown if the operation would violate a node-type
     * or other implementation-specific constraint.
     * <p/>
     * A <code>VersionException</code> is thrown if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session (i.e. the session that
     * was used to acquire this <code>Workspace</code> object) does not have sufficient access rights
     * to complete the operation.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if the node at <code>srcAbsPath</code> or the
     * parent of the new node at <code>destAbsPath</code> does not exist.
     * <p/>
     * An <code>ItemExistException</code> is thrown if a property already exists at
     * <code>destAbsPath</code> or a node already exist there, and same name
     * siblings are not allowed.
     * <p/>
     * A <code>LockException</code> is thrown if a lock prevents the copy.
     *
     * @param srcWorkspace the name of the worksapce from which the copy is to be made.
     * @param srcAbsPath the path of the node to be copied.
     * @param destAbsPath the location to which the node at <code>srcAbsPath</code>
     * is to be copied in <code>this</code> workspace.
     * @throws NoSuchWorkspaceException if <code>srcWorkspace</code> does not
     * exist or if the current <code>Session</code> does not have permission to access it.
     * @throws ConstraintViolationException if the operation would violate a
     * node-type or other implementation-specific constraint
     * @throws VersionException if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in.
     * @throws AccessDeniedException if the current session does have permission to access
     * <code>srcWorkspace</code> but otherwise does not have sufficient access rights to
     * complete the operation.
     * @throws PathNotFoundException if the node at <code>srcAbsPath</code> or
     * the parent of the new node at <code>destAbsPath</code> does not exist.
     * @throws ItemExistsException if a property already exists at
     * <code>destAbsPath</code> or a node already exist there, and same name
     * siblings are not allowed.
     * @throws LockException if a lock prevents the copy.
     * @throws RepositoryException if the last element of <code>destAbsPath</code>
     * has an index or if another error occurs.
     */
    public function copy( $srcWorkspace, $srcAbsPath, $destAbsPath );

    /**
     * Clones the subtree at the node <code>srcAbsPath</code> in <code>srcWorkspace</code> to the new location at
     * <code>destAbsPath</code> in <code>this</code> workspace. This method does not assign new UUIDs to
     * the new nodes but preserves the UUIDs (if any) of their respective source nodes.
     * <p/>
     * If <code>removeExisting</code> is true and an existing node in this workspace
     * (the destination workspace) has the same UUID as a node being cloned from
     * <code>srcWorkspace</code>, then the incoming node takes precedence, and the
     * existing node (and its subtree) is removed. If <code>removeExisting</code>
     * is false then a UUID collision causes this method to throw a
     * <code>ItemExistsException</code> and no changes are made.
     * <p/>
     * If successful, the change is persisted immediately, there is no need to call <code>save</code>.
     * <p/>
     * The <code>destAbsPath</code> provided must not
     * have an index on its final element. If it does then a <code>RepositoryException</code>
     * is thrown. Strictly speaking, the <code>destAbsPath</code> parameter is actually an <i>absolute path</i>
     * to the parent node of the new location, appended with the new <i>name</i> desired for the
     * cloned node. It does not specify a position within the child node
     * ordering. If ordering is supported by the node type of the parent node of the new
     * location, then the new clone of the node is appended to the end of the child node list.
     * <p/>
     * This method cannot be used to clone just an individual property by itself. It clones an
     * entire node and its subtree (including, of course, any properties contained therein).
     * <p/>
     * A <code>NoSuchWorkspaceException</code> is thrown if <code>srcWorkspace</code> does not
     * exist or if the current <code>Session</code> does not have permission to access it.
     * <p/>
     * A <code>ConstraintViolationException</code> is thrown if the operation would violate a node-type
     * or other implementation-specific constraint.
     * <p/>
     * A <code>VersionException</code> is thrown if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in. This exception will also be thrown if <code>removeExisting</code> is <code>true</code>,
     * and a UUID conflict occurs that would require the moving and/or altering of a node that is checked-in.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session (i.e. the session that
     * was used to acquire this <code>Workspace</code> object) does not have sufficient access rights
     * to complete the operation.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if the node at <code>srcAbsPath</code> or the
     * parent of the new node at <code>destAbsPath</code> does not exist.
     * <p/>
     * An <code>ItemExistsException</code> is thrown if a node or property already exists at
     * <code>destAbsPath</code>
     * <p/>
     * An <code>ItemExistException</code> is thrown if a property already exists at
     * <code>destAbsPath</code> or a node already exist there, and same name
     * siblings are not allowed or if <code>removeExisting</code> is false and a
     * UUID conflict occurs.
     * <p/>
     * A <code>LockException</code> is thrown if a lock prevents the clone.
     *
     * @param srcWorkspace The name of the workspace from which the node is to be copied.
     * @param srcAbsPath the path of the node to be copied in <code>srcWorkspace</code>.
     * @param destAbsPath the location to which the node at <code>srcAbsPath</code>
     * is to be copied in <code>this</code> workspace.
     * @param removeExisting if <code>false</code> then this method throws an
     * <code>ItemExistsException</code> on UUID conflict with an incoming node.
     * If <code>true</code> then a UUID conflict is resolved by removing the existing node
     * from its location in this workspace and cloning (copying in) the one from
     * <code>srcWorkspace</code>.
     *
     * @throws NoSuchWorkspaceException if <code>destWorkspace</code> does not exist.
     * @throws ConstraintViolationException if the operation would violate a
     * node-type or other implementation-specific constraint.
     * @throws VersionException if the parent node of <code>destAbsPath</code> is
     * versionable and checked-in, or is non-versionable but its nearest versionable ancestor is
     * checked-in. This exception will also be thrown if <code>removeExisting</code> is <code>true</code>,
     * and a UUID conflict occurs that would require the moving and/or altering of a node that is checked-in.
     * @throws AccessDeniedException if the current session does not have
     * sufficient access rights to complete the operation.
     * @throws PathNotFoundException if the node at <code>srcAbsPath</code> or
     * the parent of the new node at <code>destAbsPath</code> does not exist.
     * @throws ItemExistsException if a property already exists at
     * <code>destAbsPath</code> or a node already exist there, and same name
     * siblings are not allowed or if <code>removeExisting</code> is false and a
     * UUID conflict occurs.
     * @throws LockException if a lock prevents the clone.
     * @throws RepositoryException if the last element of <code>destAbsPath</code>
     * has an index or if another error occurs.
     */
    public function clone_( $srcAbsPath, $destAbsPath, $srcWorkspace = null, $removeExisting = null );

    /**
     * Moves the node at <code>srcAbsPath</code> (and its entire subtree) to the
     * new location at <code>destAbsPath</code>. If successful,
     * the change is persisted immediately, there is no need to call <code>save</code>.
     * Note that this is in contrast to {@link Session#move} which operates within the
     * transient space and hence requires a <code>save</code>.
     * <p/>
     * The <code>destAbsPath</code> provided must not
     * have an index on its final element. If it does then a <code>RepositoryException</code>
     * is thrown. Strictly speaking, the <code>destAbsPath</code> parameter is actually an <i>absolute path</i>
     * to the parent node of the new location, appended with the new <i>name</i> desired for the
     * moved node. It does not specify a position within the child node
     * ordering. If ordering is supported by the node type of
     * the parent node of the new location, then the newly moved node is appended to the end of the
     * child node list.
     * <p/>
     * This method cannot be used to move just an individual property by itself.
     * It moves an entire node and its subtree (including, of course, any properties contained therein).
     * <p/>
     * A <code>ConstraintViolationException</code> is thrown if the operation would violate a node-type
     * or other implementation-specific constraint.
     * <p/>
     * A <code>VersionException</code> is thrown if the parent node of <code>destAbsPath</code>
     * or the parent node of <code>srcAbsPath</code> is versionable and checked-in, or is
     * non-versionable but its nearest versionable ancestor is checked-in.
     * <p/>
     * An <code>AccessDeniedException</code> is thrown if the current session (i.e. the session that
     * was used to acquire this <code>Workspace</code> object) does not have sufficient access rights
     * to complete the operation.
     * <p/>
     * A <code>PathNotFoundException</code> is thrown if the node at <code>srcAbsPath</code> or the
     * parent of the new node at <code>destAbsPath</code> does not exist.
     * <p/>
     * An <code>ItemExistException</code> is thrown if a property already exists at
     * <code>destAbsPath</code> or a node already exist there, and same name
     * siblings are not allowed.
     * <p/>
     * A <code>LockException</code> if a lock prevents the move.
     *
     * @param srcAbsPath the path of the node to be moved.
     * @param destAbsPath the location to which the node at <code>srcAbsPath</code>
     * is to be moved.
     * @throws ConstraintViolationException if the operation would violate a
     * node-type or other implementation-specific constraint
     * @throws VersionException if the parent node of <code>destAbsPath</code>
     * or the parent node of <code>srcAbsPath</code> is versionable and checked-in,
     * or is non-versionable but its nearest versionable ancestor is checked-in.
     * @throws AccessDeniedException if the current session (i.e. the session that
     * was used to aqcuire this <code>Workspace</code> object) does not have
     * sufficient access rights to complete the operation.
     * @throws PathNotFoundException if the node at <code>srcAbsPath</code> or
     * the parent of the new node at <code>destAbsPath</code> does not exist.
     * @throws ItemExistsException if a property already exists at
     * <code>destAbsPath</code> or a node already exist there, and same name
     * siblings are not allowed.
     * @throws LockException if a lock prevents the move.
     * @throws RepositoryException if the last element of <code>destAbsPath</code>
     * has an index or if another error occurs.
     */
    public function move( $srcAbsPath, $destAbsPath );

    /**
     * Restores a set of versions at once. Used in cases where a "chicken and egg" problem of
     * mutually referring <code>REFERENCE</code> properties would prevent the restore in any
     * serial order.
     * <p>
     * If the restore succeeds the changes made to <code>this</code> node are
     * persisted immediately, there is no need to call <code>save</code>.
     * <p>
     * The following restrictions apply to the set of versions specified:
     * <p>
     * If <code>S</code> is the set of versions being restored simultaneously,
     * <ol>
     *   <li>
     *    For every version <code>V</code> in <code>S</code> that corresponds to
     *     a <i>missing</i> node, there must also be a parent of V in S.
     *   </li>
     *   <li>
     *     <code>S</code> must contain at least one version that corresponds to
     *     an existing node in the workspace.
     *   </li>
     * </ol>
     * If either of these restrictions does not hold, the restore will fail
     * because the system will be unable to determine the path locations to which
     * one or more versions are to be restored. In this case a
     * <code>VersionException</code> is thrown.
     * <p/>
     * The versionable nodes in this workspace that correspond to the versions being restored
     * define a set of (one or more) subtrees. A UUID collision occurs when this workspace
     * contains a node <i>outside these subtrees</i> that has the same UUID as one of the nodes
     * that would be introduced by the <code>restore</code> operation <i>into one of these subtrees</i>.
     * The result in such a case is governed by the <code>removeExisting</code> flag.
     * If <code>removeExisting</code> is <code>true</code> then the incoming node takes precedence,
     * and the existing node (and its subtree) is removed. If <code>removeExisting</code>
     * is <code>false</code> then a <code>ItemExistsException</code> is thrown and no changes are made.
     * Note that this applies not only to cases where the restored
     * node itself conflicts with an existing node but also to cases where a conflict occurs with any
     * node that would be introduced into the workspace by the restore operation. In particular, conflicts
     * involving subnodes of the restored node that have <code>OnParentVersion</code> settings of
     * <code>COPY</code> or <code>VERSION</code> are also governed by the <code>removeExisting</code> flag.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if versioning is not supported.
     * <p/>
     * An <code>InvalidItemStateException</code> is thrown if this <code>Session</code> (not necessarily this <code>Node</code>)
     * has pending unsaved changes.
     * <p/>
     * A <code>LockException</code> is thrown if a lock prevents the restore.
     *
     * @param versions The set of versions to be restored
     * @param removeExisting governs what happens on UUID collision.
     *
     * @throws ItemExistsException if <code>removeExisting</code> is <code>false</code>
     * and a UUID collision occurs with a node being restored.
     * @throws UnsupportedRepositoryOperationException if versioning is not supported.
     * @throws VersionException if the set of versions to be restored is such that the
     * original path location of one or more of the versions cannot be determined or
     * if the <code>restore</code> would change the state of a existing verisonable
     * node that is currently checked-in.
     * @throws LockException if a lock prevents the restore.
     * @throws InvalidItemStateException if this <code>Session</code> (not necessarily this <code>Node</code>) has pending unsaved changes.
     * @throws RepositoryException if another error occurs.
     */
    public function restore( /* Version */ $versions, $removeExisting );

    /**
     * Gets the <code>QueryManager</code>.
     * Returns the <code>QueryManager</code> object, through search methods are accessed.
     *
     * @throws RepositoryException if an error occurs.
     * @return the <code>QueryManager</code> object.
     */
    public function getQueryManager();

    /**
     * Returns the <code>NamespaceRegistry</code> object, which is used to access information
     * and (in level 2) set the mapping between namespace prefixes and URIs.
     *
     * @throws RepositoryException if an error occurs.
     * @return the <code>NamespaceRegistry</code>.
     */
    public function getNamespaceRegistry();

    /**
     * Returns the <code>NodeTypeManager</code> through which node type
     * information can be queried. There is one node type registry per
     * repository, therefore the <code>NodeTypeManager</code> is not
     * workspace-specific; it provides introspection methods for the
     * global, repository-wide set of available node types.
     *
     * @throws RepositoryException if an error occurs.
     * @return a <code>NodeTypeManager</code> object.
     */
    public function getNodeTypeManager();

    /**
     * If the the implemention supports observation
     * this method returns the <code>ObservationManager</code> object;
     * otherwise it throws an <code>UnsupportedRepositoryOperationException</code>.
     *
     * @throws UnsupportedRepositoryOperationException if the implementation does not support observation.
     * @throws RepositoryException if an error occurs.
     *
     * @return an <code>ObservationManager</code> object.
     */
    public function getObservationManager();

    /**
     * Returns an string array containing the names of all workspaces
     * in this repository that are accessible to this user, given the
     * <code>Credentials</code> that were used to get the <code>Session</code>
     * tied to this <code>Workspace</code>.
     * <p/>
     * In order to access one of the listed workspaces, the user performs another
     * <code>Repository.login</code>, specifying the name of the desired workspace,
     * and receives a new <code>Session</code> object.
     *
     * @return string array of names of accessible workspaces.
     * @throws RepositoryException
     */
    public function getAccessibleWorkspaceNames();

    /**
     * Returns a <code>org.xml.sax.ContentHandler</code> which can be used to push SAX events into the repository.
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
     * repsoitory; the actual deserialization is done through the methods of the <code>ContentHandler</code>.
     * Invalid XML data will cause the <code>ContentHandler</code> to throw a <code>SAXException</code>.
     * <p>
     * As SAX events are fed into the <code>ContentHandler</code>, changes are made directly at the
     * workspace level, without going through the <code>Session</code>. As a result, there is not need
     * to call <code>save</code>. The advantage of this
     * direct-to-workspace method is that a large import will not result in a large cache of pending
     * nodes in the <code>Session</code>. The disadvantage is that structures that violate node type constraints
     * cannot be imported, fixed and then saved. Instead, a constraint violation will cause the
     * <code>ContentHandler</code> to throw a <code>SAXException</code>. See <code>Session.getImportContentHandler</code> for a version of
     * this method that <i>does</i> go through the <code>Session</code>.
     * <p>
     * The flag <code>uuidBehavior</code> governs how the UUIDs of incoming (deserialized) nodes are
     * handled. There are four options:
     * <ul>
     * <li>{@link #IMPORT_UUID_CREATE_NEW}: Incoming referenceable nodes are assigned newly
     * created UUIDs upon additon to the workspace. As a result UUID collisions never occur.
     * <li>{@link #IMPORT_UUID_COLLISION_REMOVE_EXISTING}: If an incoming referenceable node
     * has the same UUID as a node already existing in the workspace, then the already exisitng node
     * (and its subtree) is removed from wherever it may be in the workspace before the incoming node
     * is added. Note that this can result in nodes "disappearing" from locations in the worksapce that
     * are remote from the location to which the incoming subtree is being written.
     * <li>{@link #IMPORT_UUID_COLLISION_REPLACE_EXISTING}: If an incoming referenceable node
     * has the same UUID as a node already existing in the workspace then the already existing node
     * is replaced by the incoming node in the same position as the existing node. Note that this may
     * result in the incoming subtree being disaggregated and "spread around" to different locations
     * in the workspace. In the most extreme case this behavior may result in no node at all
     * being added as child of <code>parentAbsPath</code>. This will occur if the topmost element
     * of the incoming XML has the same UUID as an existing node elsewhere in the workspace.
     * <li>{@link #IMPORT_UUID_COLLISION_THROW}: If an incoming referenceable node
     * has the same UUID as a node already existing in the workspace then a SAXException
     * is thrown by the returned <code>ContentHandler</code> during deserialization.
     * </ul>
     * A <code>SAXException</code> will be thrown by the returned <code>ContentHandler</code>
     * during deserialization if the top-most element of the incoming XML would deserialize to
     * a node with the same name as an existing child of <code>parentAbsPath</code> and that
     * child does not allow same-name siblings.
     * <p>
     * A <code>SAXException</code> will also be thrown by the returned <code>ContentHandler</code>
     * during deserialzation if <code>uuidBehavior</code> is set to
     * <code>IMPORT_UUID_COLLISION_REMOVE_EXISTING</code> and an incoming node has the same UUID as
     * the node at <code>parentAbsPath</code> or one of its ancestors.
     * <p>
     * A <code>PathNotFoundException</code> is thrown if no node exists at <code>parentAbsPath</code>.
     * <p>
     * A <code>ConstraintViolationException</code> is thrown if the new subtree cannot be added to the node at
     * <code>parentAbsPath</code> due to node-type or other implementation-specific constraints, and this can
     * be determined before the first SAX event is sent. Unlike {@link Session#getImportContentHandler},
     * this method also enforces node type constraints by throwing <code>SAXException</code>s during
     * deserialization.
     * <p/>
     * A <code>VersionException</code> is thrown if the node at <code>parentAbsPath</code> is versionable
     * and checked-in, or is non-versionable but its nearest versionable ancestor is checked-in.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the addition ofthe subtree.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param parentAbsPath the absolute path of a node under which (as child) the imported subtree will be built.
     * @param uuidBehavior a four-value flag that governs how incoming UUIDs are handled.
     * @return an org.xml.sax.ContentHandler whose methods may be called to feed SAX events into the deserializer.
     *
     * @throws PathNotFoundException if no node exists at <code>parentAbsPath</code>.
     * @throws ConstraintViolationException if the new subtree cannot be added to the node at
     * <code>parentAbsPath</code> due to node-type or other implementation-specific constraints,
     * and this can be determined before the first SAX event is sent.
     * @throws VersionException if the node at <code>parentAbsPath</code> is versionable
     * and checked-in, or is non-versionable but its nearest versionable ancestor is checked-in.
     * @throws LockException if a lock prevents the addition of the subtree.
     * @throws RepositoryException if another error occurs.
     */
    public function getImportContentHandler( $parentAbsPath, $uuidBehavior );

    /**
     * Deserializes an XML document and adds the resulting item subtree as a child of the node at
     * <code>parentAbsPath</code>.
     * <p>
     * If the incoming XML stream does not appear to be a JCR system view XML document then it is interpreted as a
     * <b>document view</b> XML document.
     * <p>
     * The special properties <code>jcr:primaryType</code> and <code>jcr:mixinTypes</code> are
     * taken into account during deserialization in order to determine the node types of the newly
     * created nodes.
     * <p>
     * Changes are made directly at the workspace level, without going through the <code>Session</code>.
     * As a result, there is not need to call <code>save</code>. The advantage of this
     * direct-to-workspace method is that a large import will not result in a large cache of
     * pending nodes in the <code>Session</code>. The disadvantage is that invalid data cannot
     * be imported, fixed and then saved. Instead, invalid data will cause this method to throw an
     * <code>InvalidSerializedDataException</code>. See <code>Session.importXML</code> for
     * a version of this method that <i>does</i> go through the <code>Session</code>.
     * <p/>
     * The flag <code>uuidBehavior</code> governs how the UUIDs of incoming (deserialized) nodes are
     * handled. There are four options:
     * <ul>
     * <li>{@link #IMPORT_UUID_CREATE_NEW}: Incoming referenceable nodes are assigned newly
     * created UUIDs upon additon to the workspace. As a result UUID collisions never occur.
     * <li>{@link #IMPORT_UUID_COLLISION_REMOVE_EXISTING}: If an incoming referenceable node
     * has the same UUID as a node already existing in the workspace then the already exisitng node
     * (and its subtree) is removed from wherever it may be in the workspace before the incoming node
     * is added. Note that this can result in nodes "disappearing" from locations in the worksapce that
     * are remote from the location to which the incoming subtree is being written. If an incoming node
     * has the same UUID as the existing root node of this workspace then
     * <li>{@link #IMPORT_UUID_COLLISION_REPLACE_EXISTING}: If an incoming referenceable node
     * has the same UUID as a node already existing in the workspace then the already existing node
     * is replaced by the incoming node in the same position as the existing node. Note that this may
     * result in the incoming subtree being disaggregated and "spread around" to different locations
     * in the workspace. In the most extreme edge case this behavior may result in no node at all
     * being added as child of <code>parentAbsPath</code>. This will occur if the topmost element
     * of the incoming XML has the same UUID as an existing node elsewhere in the workspace.
     * <li>{@link #IMPORT_UUID_COLLISION_THROW}: If an incoming referenceable node
     * has the same UUID as a node already existing in the workspace then an <code>ItemExistsException</code>
     * is thrown.
     * </ul>
     * An <code>ItemExistsException</code> will be thrown if <code>uuidBehavior</code>
     * is set to <code>IMPORT_UUID_CREATE_NEW</code> or <code>IMPORT_UUID_COLLISION_THROW</code>
     * and the import would would overwrite an existing child of <code>parentAbsPath</code>.
     * <p>
     * An IOException is thrown if an I/O error occurs.
     * <p>
     * If no node exists at <code>parentAbsPath</code>, a <code>PathNotFoundException</code> is thrown.
     * <p>
     * An ItemExisitsException is thrown if the top-most element of the incoming XML would deserialize
     * to a node with the same name as an existing child of <code>parentAbsPath</code> and that
     * child does not allow same-name siblings, or if a <code>uuidBehavior</code> is set to
     * <code>IMPORT_UUID_COLLISION_THROW</code> and a UUID collision occurs.
     * <p>
     * If node-type or other implementation-specific constraints
     * prevent the addition of the subtree, a <code>ConstraintViolationException</code> is thrown.
     * <p>
     * A <code>ConstraintViolationException</code> will also be thrown if <code>uuidBehavior</code>
     * is set to <code>IMPORT_UUID_COLLISION_REMOVE_EXISTING</code> and an incoming node has the same
     * UUID as the node at <code>parentAbsPath</code> or one of its ancestors.
     * <p>
     * A <code>VersionException</code> is thrown if the node at <code>parentAbsPath</code> is versionable
     * and checked-in, or is non-versionable but its nearest versionable ancestor is checked-in.
     * <p>
     * A <code>LockException</code> is thrown if a lock prevents the addition of the subtree.
     *
     * @param parentAbsPath the absolute path of the node below which the deserialized subtree is added.
     * @param in The <code>Inputstream</code> from which the XML to be deserilaized is read.
     * @param uuidBehavior a four-value flag that governs how incoming UUIDs are handled.
     *
     * @throws IOException if an error during an I/O operation occurs.
     * @throws PathNotFoundException if no node exists at <code>parentAbsPath</code>.
     * @throws ConstraintViolationException if node-type or other implementation-specific constraints
     * prevent the addition of the subtree or if <code>uuidBehavior</code>
     * is set to <code>IMPORT_UUID_COLLISION_REMOVE_EXISTING</code> and an incoming node has the same
     * UUID as the node at <code>parentAbsPath</code> or one of its ancestors.
     * @throws VersionException if the node at <code>parentAbsPath</code> is versionable
     * and checked-in, or is non-versionable but its nearest versionable ancestor is checked-in.
     * @throws InvalidSerializedDataException if incoming stream is not a valid XML document.
     * @throws ItemExistsException if the top-most element of the incoming XML would deserialize
     * to a node with the same name as an existing child of <code>parentAbsPath</code> and that
     * child does not allow same-name siblings, or if a <code>uuidBehavior</code> is set to
     * <code>IMPORT_UUID_COLLISION_THROW</code> and a UUID collision occurs.
     * @throws LockException if a lock prevents the addition of the subtree.
     * @throws RepositoryException is another error occurs.
     */
    public function importXML( $parentAbsPath, $in, $uuidBehavior );
}

?>
