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
package org.apache.jackrabbit.api.security.user;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.security.Principal;
import java.util.Iterator;

/**
 * The <code>UserManager</code> provides access to and means to maintain
 * {@link Authorizable authoriable objects} i.e. {@link User users} and
 * {@link Group groups}. The <code>UserManager</code> is bound to a particular
 * <code>Session</code>.
 */
public interface UserManager {

    /**
     * Filter flag indicating that only <code>User</code>s should be searched
     * and returned.
     */
    int SEARCH_TYPE_USER = 1;

    /**
     * Filter flag indicating that only <code>Group</code>s should be searched
     * and returned.
     */
    int SEARCH_TYPE_GROUP = 2;

    /**
     * Filter flag indicating that all <code>Authorizable</code>s should be
     * searched.
     */
    int SEARCH_TYPE_AUTHORIZABLE = 3;

    /**
     * Get the Authorizable by its id.
     *
     * @param id The user or group id.
     * @return Authorizable or <code>null</code>, if not present.
     * @throws RepositoryException If an error occurs.
     * @see Authorizable#getID()
     */
    Authorizable getAuthorizable(String id) throws RepositoryException;

    /**
     * Get the Authorizable by its main Principal.
     *
     * @param principal
     * @return Authorizable or <code>null</code>, if not present.
     * @throws RepositoryException If an error occurs.
     */
    Authorizable getAuthorizable(Principal principal) throws RepositoryException;

    /**
     * Returns all <code>Authorizable</code>s that have a
     * {@link Authorizable#getProperty(String) property} with the given relative
     * path (or name) that matches the specified value.
     * <p/>
     * If a relative path with more than one segment is specified only properties
     * exactly matching that patch will be returned. If, however, a name is
     * specified all properties that may be retrieved using
     * {@link Authorizable#getProperty(String)} will be searched for a match.
     *
     * @param relPath A relative property path or name.
     * @param value
     * @return All <code>Authorizable</code>s that have a property with the given
     * name exactly matching the given value.
     * @throws RepositoryException If an error occurs.
     * @see Authorizable#getProperty(String)
     */
    Iterator<Authorizable> findAuthorizables(String relPath, String value) throws RepositoryException;

    /**
     * Returns all <code>Authorizable</code>s that have a
     * {@link Authorizable#getProperty(String) property} with the given relative
     * path (or name) that matches the specified value. In contrast to
     * {@link #findAuthorizables(String, String)} the type of authorizable is
     * respected while executing the search.
     * <p/>
     * If a relative path with more than one segment is specified only properties
     * exactly matching that path will be returned. If, however, a name is
     * specified all properties that may be retrieved using
     * {@link Authorizable#getProperty(String)} will be searched for a match.
     *
     * @param relPath A relative property path or name.
     * @param value
     * @param searchType Any of the following constants:
     * <ul>
     * <li>{@link #SEARCH_TYPE_AUTHORIZABLE}</li>
     * <li>{@link #SEARCH_TYPE_GROUP}</li>
     * <li>{@link #SEARCH_TYPE_USER}</li>
     * </ul>
     * @return An iterator of <code>Authorizable</code>.
     * @throws RepositoryException If an error occurs.
     */
    Iterator<Authorizable> findAuthorizables(String relPath, String value, int searchType) throws RepositoryException;

    /**
     * Return {@link Authorizable}s that match a specific {@link Query}.
     *
     * @param query  A query
     * @return  Iterator of authorizables witch match the <code>query</code>.
     * @throws RepositoryException  If an error occurs.
     */
    Iterator<Authorizable> findAuthorizables(Query query) throws RepositoryException;

    /**
     * Creates an User for the given userID / password pair; neither of the
     * specified parameters can be <code>null</code>.<br>
     * Same as {@link #createUser(String,String,Principal,String)} where
     * the specified userID is equal to the principal name and the intermediate
     * path is <code>null</code>.
     *
     * @param userID The id of the new user.
     * @param password The initial password of this user.
     * @return The new <code>User</code>.
     * @throws AuthorizableExistsException in case the given userID is already
     * in use or another Authorizable with the same principal name exists.
     * @throws RepositoryException If another error occurs.
     */
    User createUser(String userID, String password) throws AuthorizableExistsException, RepositoryException;

    /**
     * Creates an User for the given parameters. If the implementation is not
     * able to deal with the <code>intermediatePath</code> that parameter should
     * be ignored.
     * Except for the <code>intermediatePath</code>, neither of the specified
     * parameters can be <code>null</code>.
     *
     * @param userID
     * @param password
     * @param principal
     * @param intermediatePath
     * @return The new <code>User</code>.
     * @throws AuthorizableExistsException in case the given userID is already
     * in use or another Authorizable with the same principal name exists.
     * @throws RepositoryException If the current Session is
     * not allowed to create users or some another error occurs.
     */
    User createUser(String userID, String password, Principal principal,
                    String intermediatePath) throws AuthorizableExistsException, RepositoryException;

    /**
     * Creates a Group for the given groupID, which must not be <code>null</code>.
     * <br>
     * Same as {@link #createGroup(String, Principal,String)} where the specified
     * groupID is the name of the <code>Principal</code> the intermediate path
     * is <code>null</code>.
     *
     * @param groupID The id of the new group; must not be <code>null</code>.
     * @return The new <code>Group</code>.
     * @throws AuthorizableExistsException in case the given groupID is already
     * in use or another {@link Authorizable} with the same
     * {@link Authorizable#getID() ID} or principal name already exists.
     * @throws RepositoryException If another error occurs.
     */
    Group createGroup(String groupID) throws AuthorizableExistsException, RepositoryException;

    /**
     * Creates a new <code>Group</code> that is based on the given principal.
     * Note that the group's ID is implementation specific. The implementation
     * may take the principal name as ID hint but must in any case assert that
     * it is unique among the IDs known to this manager.
     *
     * @param principal A non-null <code>Principal</code>
     * @return The new <code>Group</code>.
     * @throws AuthorizableExistsException in case the given principal is
     * already in use with another Authorizable.
     * @throws RepositoryException If another error occurs.
     */
    Group createGroup(Principal principal) throws AuthorizableExistsException, RepositoryException;

    /**
     * Same as {@link #createGroup(String, Principal, String)} where the
     * name of the specified principal is used to create the group's ID. 
     *
     * @param principal
     * @param intermediatePath
     * @return The new <code>Group</code>.
     * @throws AuthorizableExistsException in case the given principal is
     * already in use with another Authorizable.
     * @throws RepositoryException If another error occurs.
     */
    Group createGroup(Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException;

    /**
     * Creates a new <code>Group</code> that is based on the given id, principal
     * and the specified <code>intermediatePath</code> hint. If the implementation
     * is not able to deal with the <code>intermediatePath</code> this parameter
     * should be ignored.
     *
     * @param principal
     * @param intermediatePath
     * @return The new <code>Group</code>.
     * @throws AuthorizableExistsException in case the given principal is already
     * in use with another Authorizable.
     * @throws RepositoryException If another error occurs.
     */
    Group createGroup(String groupID, Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException;

    /**
     * If any write operations executed through the User API are automatically
     * persisted this method returns <code>true</code>. In this case there are
     * no pending transient changes left and there is no need to explicitly call
     * {@link javax.jcr.Session#save()}. If this method returns <code>false</code>
     * any changes must be completed by an extra save call on the
     * <code>Session</code> associated with this <code>UserManager</code>.
     *
     * @return <code>true</code> if changes are automatically persisted;
     * <code>false</code> if changes made through this API (including method
     * calls on  {@link Authorizable} and subclasses are only transient and
     * must be persisted using {@link javax.jcr.Session#save()}.
     * @see #autoSave(boolean)
     */
    boolean isAutoSave();

    /**
     * Changes the auto save behavior of this <code>UserManager</code>.
     * <p/>
     * Note, that this shouldn't be allowed in cases where the associated session
     * is different from the original session accessing the user manager.
     *
     * @param enable If <code>true</code> changes made through this API will
     * be automatically saved; otherwise an explicit call to
     * {@link javax.jcr.Session#save()} is required in order to persist changes.
     * @throws UnsupportedRepositoryOperationException If the implementation
     * does not allow to change the auto save behavior.
     * @throws RepositoryException If some other error occurs.
     */
    void autoSave(boolean enable) throws UnsupportedRepositoryOperationException, RepositoryException;
}
