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
     * @param id
     * @return Authorizable or <code>null</code>, if not present.
     * @throws RepositoryException
     * @see Authorizable#getID()
     */
    Authorizable getAuthorizable(String id) throws RepositoryException;

    /**
     * Get the Authorizable by its main Principal.
     *
     * @param principal
     * @return Authorizable or <code>null</code>, if not present.
     * @throws RepositoryException
     */
    Authorizable getAuthorizable(Principal principal) throws RepositoryException;

    /**
     * Returns all <code>Authorizable</code>s that have
     * {@link Authorizable#getProperty(String) property} with the given name and
     * that Property equals the given value.
     *
     * @param propertyName
     * @param value
     * @return All <code>Authorizable</code>s that have a property with the given
     * name exactly matching the given value.
     * @throws RepositoryException
     * @see Authorizable#getProperty(String)
     */
    Iterator findAuthorizables(String propertyName, String value) throws RepositoryException;

    /**
     * Returns all <code>Authorizable</code>s that have
     * {@link Authorizable#getProperty(String) property} with the given name and
     * that Property equals the given value. In contrast to
     * {@link #findAuthorizables(String, String)} the type of authorizable is
     * respected while executing the search.
     *
     * @param propertyName
     * @param value
     * @param searchType Any of the following constants:
     * <ul>
     * <li>{@link #SEARCH_TYPE_AUTHORIZABLE}</li>
     * <li>{@link #SEARCH_TYPE_GROUP}</li>
     * <li>{@link #SEARCH_TYPE_USER}</li>
     * </ul>
     * @return
     * @throws RepositoryException
     */
    Iterator findAuthorizables(String propertyName, String value, int searchType) throws RepositoryException;

    /**
     * Creates an User for the given userID / password pair; neither of the
     * specified parameters can be <code>null</code>.<br>
     * Same as {@link #createUser(String,String,Principal,String)} where
     * the specified userID is equal to the principal name and the intermediate
     * path is <code>null</code>.
     *
     * @param userID
     * @param password The initial password of this user.
     * @return The new <code>User</code>.
     * @throws AuthorizableExistsException in case the given userID is already
     * in use or another Authorizable with the same principal name exists.
     * @throws RepositoryException If another error occurs.
     */
    User createUser(String userID, String password) throws AuthorizableExistsException, RepositoryException;

    /**
     * Creates an User for the given userID that authenitcates with the given
     * {@link javax.jcr.Credentials Credentials} and returns the specified
     * Principal upon {@link User#getPrincipal()}. If the implementation is not
     * able to deal with the <code>itermediatePath</code> that parameter should
     * be ignored.
     * Except for the <code>itermediatePath</code>, neither of the specified
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
     * Creates a new <code>Group</code> that is based on the given principal.
     *
     * @param principal A non-null <code>Principal</code>
     * @return The new <code>Group</code>.
     * @throws AuthorizableExistsException in case the given groupID is already
     * in use or another Authorizable with the same principal name exists.
     * @throws RepositoryException If another error occurs.
     */
    Group createGroup(Principal principal) throws AuthorizableExistsException, RepositoryException;

    /**
     * Creates a new <code>Group</code> that is based on the given principal
     * and the specified <code>itermediatePath</code> hint. If the implementation is not
     * able to deal with the <code>itermediatePath</code> that parameter should
     * be ignored.
     *
     * @param principal
     * @param intermediatePath
     * @return The new <code>Group</code>.
     * @throws AuthorizableExistsException in case the given groupID is already
     * in use or another Authorizable with the same principal name exists.
     * @throws RepositoryException If another error occurs.
     */
    Group createGroup(Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException;
}
