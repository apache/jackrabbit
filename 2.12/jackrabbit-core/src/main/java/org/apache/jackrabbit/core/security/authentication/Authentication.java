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
package org.apache.jackrabbit.core.security.authentication;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

/**
 * The <code>Authentication</code> interface defines methods to validate
 * {@link javax.jcr.Credentials Credentials} upon authentication. The validation
 * dependants on the authentication mechanism used, i.e.
 * <ul>
 * <li>comparison of UserID/password pair retrieved from the given Credentials
 * with Credentials stored for a particular user,</li>
 * <li>bind to a LDAP with a given ID,</li>
 * <li>validation of a SSO ticket.</li>
 * </ul>
 *
 */
public interface Authentication {

    /**
     * An Authentication may only be able to handle certain types of
     * <code>Credentials</code> as the authentication process is tightly coupled
     * to the semantics of the <code>Credentials</code>.
     * E.g.: A ticket based <code>Authentication</code> is dependant on a
     * Credentials implementation which allows access to this ticket.
     *
     * @param credentials in questions
     * @return <code>true</code> if the current Authentication handles the given Credentials
     */
    boolean canHandle(Credentials credentials);

    /**
     * True if the Credentials identify the <code>User</code> related to this
     * Authentication.
     *
     * @param credentials to verify
     * @return <code>true</code> if <code>Credentials</code> identify the
     * <code>User</code>.
     * @throws RepositoryException If an error occurs.
     */
    boolean authenticate(Credentials credentials) throws RepositoryException;

}
