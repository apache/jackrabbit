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
package org.apache.jackrabbit.api.jsr283;

import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.retention.RetentionManager;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Item;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import java.util.Map;

/**
 * This interface holds extensions made in JCR 2.0 while work
 * is in progress implementing JCR 2.0.
 *
 * @since JCR 2.0
 */
public interface Session extends javax.jcr.Session {

    /**
     * A constant representing the <code>read</code> action string, used to
     * determine if this <code>Session</code> has permission to retrieve an
     * item (and read the value, in the case of a property).
     *
     * @see #hasPermission(String, String)
     * @see #checkPermission(String, String)
     * @since JCR 2.0
     */
    public static final String ACTION_READ = "read";

    /**
     * A constant representing the <code>add_node</code> action string, used to
     * determine if this <code>Session</code> has permission to add a new node.
     *
     * @see #hasPermission(String, String)
     * @see #checkPermission(String, String)
     * @since JCR 2.0
     */
    public static final String ACTION_ADD_NODE = "add_node";

    /**
     * A constant representing the <code>set_property</code> action string,
     * used to determine if this <code>Session</code> has permission to set
     * (add or modify) a property.
     *
     * @see #hasPermission(String, String)
     * @see #checkPermission(String, String)
     * @since JCR 2.0
     */
    public static final String ACTION_SET_PROPERTY = "set_property";

    /**
     * A constant representing the <code>remove</code> action string,
     * used to determine if this <code>Session</code> has permission to remove
     * an item.
     *
     * @see #hasPermission(String, String)
     * @see #checkPermission(String, String)
     * @since JCR 2.0
     */
    public static final String ACTION_REMOVE = "remove";

    /**
     * Returns the node specified by the given identifier. Applies to both
     * referenceable and non-referenceable nodes.
     * <p/>
     * An <code>ItemNotFoundException</code> is thrown if no node with the
     * specified identifier exists. This exception is also thrown if this
     * <code>Session<code> does not have read access to the node with the
     * specified identifier.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param id An identifier.
     * @return A <code>Node</code>.
     * @throws ItemNotFoundException if the specified identifier is not found.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public javax.jcr.Node getNodeByIdentifier(String id) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the node at the specified absolute path in the workspace.
     * If no node exists, then a <code>PathNotFoundException</code> is thrown.
     *
     * @param absPath An absolute path.
     * @return the specified <code>Node</code>.
     * @throws PathNotFoundException If no node exists.
     * @throws RepositoryException If another error occurs.
     * @since JCR 2.0
     */
    public javax.jcr.Node getNode(String absPath) throws PathNotFoundException, RepositoryException;

    /**
     * Returns the property at the specified absolute path in the workspace.
     * If no property exists, then a <code>PathNotFoundException</code> is thrown.
     *
     * @param absPath An absolute path.
     * @return the specified <code>Property</code>.
     * @throws PathNotFoundException If no property exists.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public Property getProperty(String absPath) throws PathNotFoundException, RepositoryException;

    /**
     * Returns <code>true</code> if a node exists at <code>absPath</code>
     * and this <code>Session</code> has read access to it; otherwise returns
     * <code>false</code>.
     * <p/>
     * Throws a <code>RepositoryException</code> if <code>absPath</code>
     * is not a well-formed absolute path.
     *
     * @param absPath An absolute path.
     * @return a <code>boolean</code>
     * @throws RepositoryException if <code>absPath</code> is not a well-formed
     *         absolute path.
     * @since JCR 2.0
     */
    public boolean nodeExists(String absPath) throws RepositoryException;

    /**
     * Returns <code>true</code> if a property exists at <code>absPath</code>
     * and this <code>Session</code> has read access to it; otherwise returns
     * <code>false</code>.
     * <p/>
     * Throws a <code>RepositoryException</code> if <code>absPath</code>
     * is not a well-formed absolute path.
     *
     * @param absPath An absolute path.
     * @return a <code>boolean</code>
     * @throws RepositoryException if <code>absPath</code> is not a well-formed
     *         absolute path.
     * @since JCR 2.0
     */
    boolean propertyExists(String absPath) throws RepositoryException;

    /**
     * Removes the specified item (and its subtree).
     * <p/>
     * To persist a removal, a <code>save</code> must be
     * performed.
     * <p/>
     * If a node with same-name siblings is removed, this decrements by one the
     * indices of all the siblings with indices greater than that of the removed
     * node. In other words, a removal compacts the array of same-name siblings
     * and causes the minimal re-numbering required to maintain the original
     * order but leave no gaps in the numbering.
     * <p/>
     * A <code>ReferentialIntegrityException</code> will be thrown on <code>save</code>
     * if the specified item or an item in its subtree is currently the target of a <code>REFERENCE</code>
     * property located in this workspace but outside the specified item's subtree and the current <code>Session</code>
     * has read access to that <code>REFERENCE</code> property.
     * <p/>
     * An <code>AccessDeniedException</code> will be thrown on <code>save</code>
     * if the specified item or an item in its subtree is currently the target of a <code>REFERENCE</code>
     * property located in this workspace but outside the specified item's subtree and the current <code>Session</code>
     * <i>does not</i> have read access to that <code>REFERENCE</code> property.
     * <p/>
     * A <code>ConstraintViolationException</code> will be thrown either immediately
     * or on <code>save</code>, if removing the specified item would violate a node type or implementation-specific
     * constraint. Implementations may differ on when this validation is performed.
     * <p/>
     * A <code>VersionException</code> will be thrown either immediately
     * or on <code>save</code>, if the parent node of the specified item is versionable and checked-in
     * or is non-versionable but its nearest versionable ancestor is checked-in. Implementations
     * may differ on when this validation is performed.
     * <p/>
     * A <code>LockException</code> will be thrown either immediately or on <code>save</code>
     * if a lock prevents the removal of the specified item. Implementations may differ on when this validation is performed.
     *
     * @param absPath the absolute path of the item to be removed.
     * @throws VersionException if the parent node of the item at absPath is versionable and checked-in
     * or is non-versionable but its nearest versionable ancestor is checked-in and this
     * implementation performs this validation immediately instead of waiting until <code>save</code>.
     * @throws LockException if a lock prevents the removal of the specified item and this
     * implementation performs this validation immediately instead of waiting until <code>save</code>.
     * @throws ConstraintViolationException if removing the specified item would violate a node type or
     * implementation-specific constraint and this implementation performs this validation immediately
     * instead of waiting until <code>save</code>.
     * @throws RepositoryException if another error occurs.
     * @see Item#remove()
     * @since JCR 2.0
     */
    public void removeItem(String absPath) throws VersionException, LockException, ConstraintViolationException, RepositoryException;

    /**
     * Returns <code>true</code> if this <code>Session</code> has permission to
     * perform the specified actions at the specified <code>absPath</code> and
     * <code>false</code> otherwise.
     * <p/>
     * The <code>actions</code> parameter is a comma separated list of action strings.
     * The following action strings are defined:
     * <ul>
     * <li>
     * {@link #ACTION_ADD_NODE <code>add_node</code>}: If
     * <code>hasPermission(path, "add_node")</code> returns <code>true</code>,
     * then this <code>Session</code> has permission to add a node at
     * <code>path</code>.
     * </li>
     * <li>
     * {@link #ACTION_SET_PROPERTY <code>set_property</code>}: If
     * <code>hasPermission(path, "set_property")</code>
     * returns <code>true</code>, then this <code>Session</code> has permission
     * to set (add or change) a property at <code>path</code>.
     * </li>
     * <li>
     * {@link #ACTION_REMOVE <code>remove</code>}: If
     * <code>hasPermission(path, "remove")</code> returns <code>true</code>,
     * then this <code>Session</code> has permission to remove an item at
     * <code>path</code>.
     * </li>
     * <li>
     * {@link #ACTION_READ <code>read</code>}: If
     * <code>hasPermission(path, "read")</code> returns <code>true</code>, then
     * this <code>Session</code> has permission to retrieve (and read the value
     * of, in the case of a property) an item at <code>path</code>.
     * </li>
     * </ul>
     * When more than one action is specified in the <code>actions</code>
     * parameter, this method will only return <code>true</code> if this
     * <code>Session</code> has permission to perform <i>all</i> of the listed
     * actions at the specified path.
     * <p/>
     * The information returned through this method will only reflect the access
     * control status (both JCR defined and implementation-specific) and not other
     * restrictions that may exist, such as node type constraints. For example,
     * even though <code>hasPermission</code> may indicate that a particular
     * <code>Session</code> may add a property at <code>/A/B/C</code>,
     * the node type of the node at <code>/A/B</code> may prevent the addition
     * of a property called <code>C</code>.
     *
     * @param absPath an absolute path.
     * @param actions a comma separated list of action strings.
     * @return boolean <code>true</code> if this <code>Session</code> has permission to
     * perform the specified actions at the specified <code>absPath</code>.
     * @throws RepositoryException if an error occurs.
     * @since JCR 2.0
     */
    public boolean hasPermission(String absPath, String actions) throws RepositoryException;

    /**
     * Checks whether an operation can be performed given as much context as can be determined
     * by the repository, including:
     * <ul>
     * <li>
     * Permissions granted to the current user, including access control privileges.
     * </li>
     * <li>
     * Current state of the target object (reflecting locks, checkin/checkout status, retention and hold status etc.).
     * </li>
     * <li>
     * Repository capabilities.
     * </li>
     * <li>
     * Node type-enforced restrictions.
     * </li>
     * <li>
     * Repository configuration-specific restrictions.
     * </li>
     * </ul>
     * The implementation of this method is best effort: returning <code>false</code> guarantees
     * that the operation cannot be performed, but returning <code>true</code> does not guarantee
     * the opposite. The repository implementation should use this to give priority to
     * performance over completeness. An exception should be thrown only for important
     * failures such as loss of connectivity to the back-end.
     * <p>
     * The implementation of this method is best effort: returning false guarantees that the operation cannot be
     * performed, but returning true does not guarantee the opposite.
     * <p>
     * The <code>methodName</code> parameter identifies the method in question by its name
     * as defined in the Javadoc.
     * <p>
     * The <code>target</code> parameter identifies the object on which the specified method is called.
     * <p>
     * The <code>arguments</code> parameter contains a <code>Map</code> object consisting of
     * name/value pairs where the name is a String holding the parameter name of
     * the method as defined in the Javadoc and the value is an Object holding
     * the value to be passed. In cases where the value is a Java primitive type
     * it must be converted to its corresponding Java object form before being passed.
     * <p>
     * For example, given a <code>Session</code> <code>S</code> and <code>Node</code>
     * <code>N</code> then
     * <p>
     * <code>
     * Map p = new HashMap();
     * p.put("relPath", "foo");
     * boolean b = S.hasCapability("addNode", N, p);
     * </code>
     * <p>
     * will result in b == false if a child node called foo cannot be added to the node
     * <code>N</code> within the session <code>S</code>.
     *
     * @param methodName the nakme of the method.
     * @param target the target object of the operation.
     * @param arguments the arguments of the operation.
     * @return boolean <code>false</code> if the operation cannot be performed,
     * <code>true</code> if the operation can be performed or if the repository
     * cannot determine whether the operation can be performed.
     * @throws RepositoryException if an error occurs
     * @since JCR 2.0
     */
    public boolean hasCapability(String methodName, Object target, Map arguments) throws RepositoryException;

    /**
     * Returns the access control manager for this <code>Session</code>.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * access control is not supported.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return the access control manager for this <code>Session</code>
     * @throws UnsupportedRepositoryOperationException if access control
     *         is not supported.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public AccessControlManager getAccessControlManager()
            throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns the retention and hold manager for this <code>Session</code>.
     * <p/>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * retention and hold are not supported.
     * <p/>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return the retention manager for this <code>Session</code>.
     * @throws UnsupportedRepositoryOperationException if retention and hold
     * are not supported.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public RetentionManager getRetentionManager()
            throws UnsupportedRepositoryOperationException, RepositoryException;
}