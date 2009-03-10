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
package org.apache.jackrabbit.core.security;

import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlPolicy;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.security.Principal;

/**
 * <code>JackrabbitAccessControlManager</code>...
 */
public interface JackrabbitAccessControlManager extends AccessControlManager {

    /**
     * Returns the editable policies for the specified <code>principal</code>.
     *
     * @param principal A principal known to the editing session.
     * @return array of policies for the specified <code>principal</code>. Note
     * that the policy object returned must reveal the path of the node where
     * they can be applied later on using {@link AccessControlManager#setPolicy(String, org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy)}.
     * @throws AccessDeniedException if the session lacks
     * <code>MODIFY_ACCESS_CONTROL</code> privilege.
     * @throws AccessControlException if the specified principal does not exist
     * or if another access control related exception occurs.
     * @throws UnsupportedRepositoryOperationException if editing access control
     * policies is not supported.
     * @throws RepositoryException if another error occurs.
     * @see org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlPolicy#getPath()
     */
    JackrabbitAccessControlPolicy[] getApplicablePolicies(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException;
}