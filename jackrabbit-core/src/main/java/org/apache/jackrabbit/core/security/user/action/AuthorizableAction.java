/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.apache.jackrabbit.core.security.user.action;

import org.apache.jackrabbit.api.security.user.Authorizable;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * <code>AuthorizableAction</code>...
 */
public interface AuthorizableAction {

    /**
     * Allows to add application specific modifications associated with the
     * creation of a new authorizable. Note, that this method is called
     * <strong>before</strong> any <code>Session.save</code> call.
     *
     * @param authorizable The new authorizable that has not yet been persisted;
     * e.g. the associated node is still 'NEW'.
     * @param session The editing session associated with the user manager.
     * @throws RepositoryException If an error occurs.
     */
    void onCreate(Authorizable authorizable, Session session) throws RepositoryException;

    /**
     * Allows to add application specific behavior associated with the removal
     * of an authorizable. Note, that this method is called <strong>before</strong>
     * {@link Authorizable#remove} is executed (and persisted); thus the
     * target authorizable still exists.
     *
     * @param authorizable The authorizable to be removed.
     * @param session The editing session associated with the user manager.
     * @throws RepositoryException If an error occurs.
     */
    void onRemove(Authorizable authorizable, Session session) throws RepositoryException;
}