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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.util.Properties;

/**
 * Derived UserManager implementation that allows to switch between autosaving
 * and transient change mode.
 * <p>
 * NOTE: This requires that the Session passed to the user manager upon creation
 * is identical to the Session passed to
 * {@link org.apache.jackrabbit.core.security.JackrabbitSecurityManager#getUserManager(Session)}.
 *
 * @see UserPerWorkspaceUserManager
 */
public class UserPerWorkspaceUserManager extends UserManagerImpl {

    private boolean autoSave = true;

    /**
     * Same as <code>UserPerWorkspaceUserManager(session, adminID, null, null)</code>.
     *
     * @param session
     * @param adminId
     * @throws RepositoryException
     */
    public UserPerWorkspaceUserManager(SessionImpl session, String adminId) throws RepositoryException {
        super(session, adminId);
    }

    /**
     * Creates a UserManager that doesn't implicitly save changes but requires
     * an explicit call to {@link javax.jcr.Session#save()}.
     *
     * @param session
     * @param adminId
     * @param config
     * @throws javax.jcr.RepositoryException
     */
    public UserPerWorkspaceUserManager(SessionImpl session, String adminId, Properties config) throws RepositoryException {
        super(session, adminId, config);
    }
        
    /**
     * Creates a UserManager that doesn't implicitly save changes but requires
     * an explicit call to {@link javax.jcr.Session#save()}.
     *
     * @param session
     * @param adminId
     * @param config
     * @throws javax.jcr.RepositoryException
     */
    public UserPerWorkspaceUserManager(SessionImpl session, String adminId,
                                       Properties config, MembershipCache mCache) throws RepositoryException {
        super(session, adminId, config, mCache);
    }

    //--------------------------------------------------------< UserManager >---
    /**
     * @see org.apache.jackrabbit.api.security.user.UserManager#getAuthorizableByPath(String)
     */
    @Override
    public Authorizable getAuthorizableByPath(String path) throws UnsupportedRepositoryOperationException, RepositoryException {
        SessionImpl session = getSession();
        if (session.nodeExists(path)) {
            NodeImpl n = (NodeImpl) session.getNode(path);
            return getAuthorizable(n);
        } else {
            return null;
        }
    }

    /**
     * @see org.apache.jackrabbit.api.security.user.UserManager#isAutoSave()
     */
    @Override
    public boolean isAutoSave() {
        return autoSave;
    }

    /**
     * @see org.apache.jackrabbit.api.security.user.UserManager#autoSave(boolean)
     */
    @Override
    public void autoSave(boolean enable) throws UnsupportedRepositoryOperationException, RepositoryException {
        autoSave = enable;
    }

    //--------------------------------------------------------------------------
    /**
     * Returns the path of the specified authorizableNode.
     *
     * @param authorizableNode Node associated with an authorizable.
     * @return The path of the node.
     * @throws RepositoryException If an error occurs while retrieving the path.
     */
    @Override
    String getPath(Node authorizableNode) throws RepositoryException {
        return authorizableNode.getPath();
    }
}