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
package org.apache.jackrabbit.api.security;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import java.security.Principal;

/**
 * <code>JackrabbitAccessControlManager</code> provides extensions to the
 * <code>AccessControlManager</code> interface.
 */
public interface JackrabbitAccessControlManager extends AccessControlManager {

    /**
     * Returns the applicable policies for the specified <code>principal</code>
     * or an empty array if no additional policies can be applied.
     *
     * @param principal A principal known to the editing session.
     * @return array of policies for the specified <code>principal</code>. Note
     * that the policy object returned must reveal the path of the node where
     * they can be applied later on using {@link AccessControlManager#setPolicy(String, javax.jcr.security.AccessControlPolicy)}.
     * @throws AccessDeniedException if the session lacks
     * <code>MODIFY_ACCESS_CONTROL</code> privilege.
     * @throws AccessControlException if the specified principal does not exist
     * or if another access control related exception occurs.
     * @throws UnsupportedRepositoryOperationException if editing access control
     * policies by principal is not supported.
     * @throws RepositoryException if another error occurs.
     * @see JackrabbitAccessControlPolicy#getPath()
     */
    JackrabbitAccessControlPolicy[] getApplicablePolicies(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns the <code>AccessControlPolicy</code> objects that have been set
     * for the given <code>principal</code> or an empty array if no policy has
     * been set. This method reflects the binding state, including transient
     * policy modifications.
     *
     * @param principal
     * @return
     * @throws AccessDeniedException
     * @throws AccessControlException
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    JackrabbitAccessControlPolicy[] getPolicies(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException;
}