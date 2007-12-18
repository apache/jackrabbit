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

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.security.auth.Subject;
import java.io.File;

/**
 * An <code>AMContext</code> is used to provide context information for an
 * <code>AccessManager</code>.
 *
 * @see AccessManager#init(AMContext)
 */
public class AMContext {

    /**
     * the physcial home dir
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

    /**
     * hierarchy manager for resolving ItemId-to-Path mapping
     */
    private final HierarchyManager hierMgr;

    /**
     * namespace resolver for resolving namespaces in qualified paths
     */
    private final NamespaceResolver nsResolver;

    /**
     * name of the workspace
     */
    private final String workspaceName;

    /**
     * Creates a new <code>AMContext</code>.
     *
     * @param physicalHomeDir the physical home directory
     * @param fs              the virtual jackrabbit filesystem
     * @param subject         subject whose access rights should be reflected
     * @param hierMgr         hierarchy manager
     * @param nsResolver      namespace resolver
     * @param workspaceName   workspace name
     */
    public AMContext(File physicalHomeDir,
                     FileSystem fs,
                     Subject subject,
                     HierarchyManager hierMgr,
                     NamespaceResolver nsResolver,
                     String workspaceName) {
        this.physicalHomeDir = physicalHomeDir;
        this.fs = fs;
        this.subject = subject;
        this.hierMgr = hierMgr;
        this.nsResolver = nsResolver;
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
     * Returns the namespace resolver
     *
     * @return the namespace resolver
     */
    public NamespaceResolver getNamespaceResolver() {
        return nsResolver;
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
