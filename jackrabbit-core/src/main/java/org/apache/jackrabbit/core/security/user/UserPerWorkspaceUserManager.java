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

import org.apache.jackrabbit.core.SessionImpl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.util.Properties;

/**
 * Derived UserManager implementation that allows to switch between autosaving
 * and transient change mode.
 * <p/>
 * NOTE: This requires that the Session passed to the user manager upon creation
 * is identical to the Session passed to
 * {@link org.apache.jackrabbit.core.security.JackrabbitSecurityManager#getUserManager(Session)}.
 *
 * @see UserPerWorkspaceUserManager
 */
public class UserPerWorkspaceUserManager extends UserManagerImpl {

    private boolean autoSave = true;

    /**
     * Same as <code>TransientChangeUserManagerImpl(session, adminID, null)</code>.
     *
     * @param session
     * @param adminId
     * @throws RepositoryException
     */
    public UserPerWorkspaceUserManager(SessionImpl session, String adminId) throws RepositoryException {
        this(session, adminId, null);
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

    //--------------------------------------------------------< UserManager >---
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
}