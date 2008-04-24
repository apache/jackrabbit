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

import org.apache.jackrabbit.api.security.principal.PrincipalIterator;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.security.Principal;
import java.util.Iterator;

/**
 * The Authorizable is the common base interface for {@link User} and
 * {@link Group}. It provides access to the <code>Principal</code>s associated
 * with an <code>Authorizable</code> (see below) and allow to access and
 * modify additional properties such as e.g. full name, e-mail or address.
 * <p/>
 *
 * Please note the difference between <code>Authorizable</code> and
 * {@link java.security.Principal Principal}:<br>
 * An <code>Authorizable</code> is repository object that is neither associated
 * with nor depending from a particular <code>Session</code> and thus independant
 * of the login mechanisms creating <code>Session</code>s.<br>
 *
 * On the other hand <code>Principal</code>s are representations of user
 * identities. In other words: each <code>Principal</code> within the set
 * associated with the Session's Subject upon login represents an identity for
 * that user. An the set of <code>Principal</code>s may differ between different
 * login mechanisms.<br>
 *
 * Consequently an one-to-many relationship exists between Authorizable
 * and Principal (see also {@link #getPrincipal()} and {@link #getPrincipals()}).
 * <p />
 *
 * The interfaces derived from Authorizable are defined as follows:
 * <ul>
 * <li>{@link User}: defined to be an authorizable that can be authenticated
 * (by using Credentials) and impersonated.</li>
 * <li>{@link Group}: defined to be a collection of other
 * <code>Authorizable</code>s.</li>
 * </ul>
 *
 * @see User
 * @see Group
 */
public interface Authorizable  {

    /**
     * Return the implementation specific identifer for this
     * <code>Authorizable</code>. It could e.g. be a UserID or simply the
     * principal name.
     *
     * @return Name of this <code>Authorizable</code>.
     * @throws RepositoryException if an error occurs.
     */
    String getID() throws RepositoryException;

    /**
	 * @return if the current Authorizable is a {@link Group}
	 */
	boolean isGroup();

    /**
     * @return a representation as Principal.
     * @throws RepositoryException If an error occurs.
     */
    Principal getPrincipal() throws RepositoryException;

    /**
     * Add the given Principal to this Authorizable.
     * Note, that a Principal can only be refered by a single Authorizable in
     * the Repository. If another User or Group already refers to the given
     * Principal a <code>AuthorizableExistsException</code> is thrown.
     *
     * @param principal
     * @return true if added, false if this Authorizable already represents
     * the given Principal.
     * @return AuthorizableExistsException If the given principal is already refered
     * to by another Authorizable.
     * @throws RepositoryException
     */
    boolean addReferee(Principal principal) throws AuthorizableExistsException, RepositoryException;

    /**
     * Remove the specified Principal for the referees of this Authorizable.
     *
     * @param principal
     * @return true if principal has been referee before. False otherwise.
     * @throws RepositoryException
     */
    boolean removeReferee(Principal principal) throws RepositoryException;

    /**
     * @return Iterator of all Principal related to this authentication Object
     * including the main principal, (see {@link #getPrincipal()}).
     * @throws RepositoryException
     */
    PrincipalIterator getPrincipals() throws RepositoryException;

    /**
     * @return all {@link Group}s, this Authorizable is member of
     * @throws RepositoryException
     */
    Iterator memberOf() throws RepositoryException;

    /**
     * Removes this <code>Authorizable</code>, if the session has sufficient
     * permissions. Note, that removing an <code>Authorizable</code> even
     * if it listed as member of a Group or if still has members (this is
     * a Group itself).
     *
     * @throws RepositoryException If an error occured and the
     * <code>Authorizable</code> could not be removed.
     */
    void remove() throws RepositoryException;

    /**
     * Returns the names of properties present with <code>this</code> Authorizable.
     *
     * @return names of properties.
     * @throws RepositoryException If an error occurs.
     * @see #getProperty(String)
     * @see #hasProperty(String)
     */
    Iterator getPropertyNames() throws RepositoryException;

    /**
	 * Tests if a the property with specified name exists.
     *
	 * @param name
	 * @return
	 * @throws RepositoryException
	 * @see #getProperty(String)
	 */
	boolean hasProperty(String name) throws RepositoryException;

    /**
     * Set an arbitrary property to this <code>Authorizable</code>.
     *
     * @param name
     * @param value
     * @throws RepositoryException If the specified property could not be set.
     */
    void setProperty(String name, Value value) throws RepositoryException;

    /**
     * Set an arbitrary property to this <code>Authorizable</code>.
     *
     * @param name
     * @param value multiple values
     * @throws RepositoryException  If the specified property could not be set.
     */
    void setProperty(String name, Value[] value) throws RepositoryException;

	/**
     * Returns the values for the properties with the specified name or
     * <code>null</code>.
     *
     * @param name
     * @return value of the property with the given name or <code>null</code>
     * if no such property exists.
     * @throws RepositoryException If an error occurs.
     */
    Value[] getProperty(String name) throws RepositoryException;

    /**
     * Removes the property with the given name.
     *
     * @param name
     * @return true If the property with the specified name was successfully
     * removed; false if no such property was present.
     * @throws RepositoryException If an error occurs.
     */
    boolean removeProperty(String name) throws RepositoryException;
}
