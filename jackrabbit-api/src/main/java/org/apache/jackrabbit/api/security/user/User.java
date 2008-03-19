/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.api.security.user;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Impersonation;

import javax.jcr.RepositoryException;
import java.util.Iterator;

/**
 * User is a special {@link Authorizable} that can be authenticated and
 * impersonated.
 *
 * @see #getCredentials()
 * @see #getImpersonation()
 */
public interface User extends Authorizable {

    /**
     * @return true if the current Authorizable is has all Privileges
     */
    boolean isAdmin();

    /**
     * Returns an iterator of <code>Credentials</code> object that belong
     * to this user. The iterator's size must be greater than zero.
     *
     * @return an iterator over <code>Credentials</code> that contains
     * at least a single <code>Credentials</code> object.
     */
    Iterator getCredentials() throws RepositoryException;

    /**
     * @return <code>Impersonation</code> for this <code>User</code>.
     */
    Impersonation getImpersonation() throws RepositoryException;

}
