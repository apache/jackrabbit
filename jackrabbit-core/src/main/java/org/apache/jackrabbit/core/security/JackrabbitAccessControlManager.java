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
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.security.Principal;

/**
 * <code>JackrabbitAccessControlManager</code>...
 */
public interface JackrabbitAccessControlManager extends AccessControlManager {

    /**
     * Returns a policy template for the existing node at <code>absPath</code>.
     * 
     * @return policy template for the node at <code>absPath</code>.
     * @throws PathNotFoundException if no node exists for the given
     * <code>nodePath</code>.
     * @throws AccessDeniedException if the session lacks
     * <code>MODIFY_ACCESS_CONTROL</code> privilege for the <code>absPath</code>
     * node.
     * @throws AccessControlException if this implementation does not allow to
     * edit the policy at <code>absPath</code> of if same other access
     * control related exception occurs.
     * @throws UnsupportedRepositoryOperationException if editing the policy
     * is not supported.
     * @throws RepositoryException if another error occurs.
     */
    PolicyTemplate editPolicy(String absPath) throws PathNotFoundException, AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns a policy template for the specified <code>principal.</code>
     *
     * @return policy template for the specified <code>principal</code>.
     * @throws AccessDeniedException if the session lacks
     * <code>MODIFY_ACCESS_CONTROL</code> privilege.
     * @throws AccessControlException if the specified principal does not exist,
     * if this implementation does provide policy tempates for principals or
     * if same other access control related exception occurs.
     * @throws UnsupportedRepositoryOperationException if editing the policy
     * is not supported.
     * @throws RepositoryException if another error occurs.
     */
    PolicyTemplate editPolicy(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException;

}