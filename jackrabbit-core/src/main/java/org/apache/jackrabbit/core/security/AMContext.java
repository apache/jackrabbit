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

import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.Session;
import javax.security.auth.Subject;
import java.io.File;

/**
 * An <code>AMContext</code> is used to provide <code>Session</code> specific
 * context information for an <code>AccessManager</code>.
 *
 * @see AccessManager#init(AMContext)
 * @see AccessManager#init(AMContext, org.apache.jackrabbit.core.security.authorization.AccessControlProvider, org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager)
 */
public class AMContext {

    /**
     * the physical home dir
     */
    private final File physicalHomeDir;

    /**
     * the virtual jackrabbit filesystem
     */
    private final FileSystem fs;

    /**
     * Subject whose access rights the access manager should reflect
     */
    private final Subject subject;

    private final Session session;

    /**
     * hierarchy manager for resolving ItemId-to-Path mapping
     */
    private final HierarchyManager hierMgr;

    /**
     * The privilege manager
     */
    private final PrivilegeManager privilegeMgr;

    /**
     * name and path resolver for resolving JCR name/path strings to internal
     * Name/Path objects (and vice versa).
     */
    private final NamePathResolver resolver;

    /**
     * name of the workspace
     */
    private final String workspaceName;

    /**
     * Creates a new <code>AMContext</code>.
     *
     * @param physicalHomeDir the physical home directory
     * @param fs              the virtual jackrabbit filesystem
     * @param session         the session.
     * @param subject         subject whose access rights should be reflected
     * @param hierMgr         hierarchy manager
     * @param privilegeMgr    privilege manager
     * @param resolver        name and path resolver
     * @param workspaceName   workspace name
     */
    public AMContext(File physicalHomeDir,
                     FileSystem fs,
                     Session session,
                     Subject subject,
                     HierarchyManager hierMgr,
                     PrivilegeManager privilegeMgr,
                     NamePathResolver resolver,
                     String workspaceName) {
        this.physicalHomeDir = physicalHomeDir;
        this.fs = fs;
        this.session = session;
        this.subject = subject;
        this.hierMgr = hierMgr;
        this.privilegeMgr = privilegeMgr;
        this.resolver = resolver;
        this.workspaceName = workspaceName;
    }


    /**
     * Returns the physical home directory
     *
     * @return the physical home directory
     */
    public File getHomeDir() {
        return physicalHomeDir;
    }

    /**
     * Returns the virtual filesystem
     *
     * @return the virtual filesystem
     */
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Returns the session
     *
     * @return the session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Returns the subject
     *
     * @return the subject
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * Returns the hierarchy manager
     *
     * @return the hierarchy manager
     */
    public HierarchyManager getHierarchyManager() {
        return hierMgr;
    }

    /**
     * Returns the privilege manager
     *
     * @return the privilege manager
     */
    public PrivilegeManager getPrivilegeManager() {
        return privilegeMgr;
    }

    /**
     * Returns the namespace resolver
     *
     * @return the namespace resolver
     */
    public NamePathResolver getNamePathResolver() {
        return resolver;
    }

    /**
     * Returns the name of the workspace.
     *
     * @return the name of the workspace
     */
    public String getWorkspaceName() {
        return workspaceName;
    }
}
