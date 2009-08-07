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
package org.apache.jackrabbit.api.jsr283;

import org.apache.jackrabbit.api.jsr283.lock.LockManager;
import org.apache.jackrabbit.api.jsr283.version.VersionManager;

import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.AccessDeniedException;

/**
 * The <code>Workspace</code> object represents a "view" of an actual repository workspace
 * entity as seen through the authorization settings of its associated <code>Session</code>.
 * Each <code>Workspace</code> object is associated one-to-one with a <code>Session</code>
 * object. The <code>Workspace</code> object can be acquired by calling
 * <code>{@link Session#getWorkspace()}</code> on the associated <code>Session</code> object.
 */
public interface Workspace extends javax.jcr.Workspace {


    /**
     * A constant for the name of the workspace root node.
     *
     * @since JCR 2.0
     */
    public static final String NAME_WORKSPACE_ROOT = "";

    /**
     * A constant for the absolute path of the workspace root node.
     *
     * @since JCR 2.0
     */
    public static final String PATH_WORKSPACE_ROOT = "/";

    /**
     * A constant for the name of the system node.
     *
     * @since JCR 2.0
     */
    public static final String NAME_SYSTEM_NODE = "{http://www.jcp.org/jcr/1.0}system";

    /**
     * A constant for the absolute path of the system node.
     *
     * @since JCR 2.0
     */
    public static final String PATH_SYSTEM_NODE = "/" + NAME_SYSTEM_NODE;

    /**
     * A constant for the name of the node type definition storage node.
     *
     * @since JCR 2.0
     */
    public static final String NAME_NODE_TYPES_NODE = "{http://www.jcp.org/jcr/1.0}nodeTypes";

    /**
     * A constant for the absolute path of the node type definition storage node.
     *
     * @since JCR 2.0
     */
    public static final String PATH_NODE_TYPES_NODE = PATH_SYSTEM_NODE + "/" + NAME_NODE_TYPES_NODE;

    /**
     * A constant for the name of the version storage node.
     *
     * @since JCR 2.0
     */
    public static final String NAME_VERSION_STORAGE_NODE = "{http://www.jcp.org/jcr/1.0}versionStorage";

    /**
     * A constant for the absolute path of the version storage node.
     *
     * @since JCR 2.0
     */
    public static final String PATH_VERSION_STORAGE_NODE = PATH_SYSTEM_NODE + "/" + NAME_VERSION_STORAGE_NODE;

    /**
     * A constant for the name of the activities node.
     *
     * @since JCR 2.0
     */
    public static final String NAME_ACTIVITIES_NODE = "{http://www.jcp.org/jcr/1.0}activities";

    /**
     * A constant for the absolute path of the activities node.
     *
     * @since JCR 2.0
     */
    public static final String PATH_ACTIVITIES_NODE = PATH_SYSTEM_NODE + "/" + NAME_ACTIVITIES_NODE;

    /**
     * A constant for the name of the configurations node.
     *
     * @since JCR 2.0
     */
    public static final String NAME_CONFIGURATIONS_NODE = "{http://www.jcp.org/jcr/1.0}configurations";

    /**
     * A constant for the absolute path of the configurations node.
     *
     * @since JCR 2.0
     */
    public static final String PATH_CONFIGURATIONS_NODE = PATH_SYSTEM_NODE + "/" + NAME_CONFIGURATIONS_NODE;

    /**
     * A constant for the name of the unfiled storage node.
     *
     * @since JCR 2.0
     */
    public static final String NAME_UNFILED_NODE = "{http://www.jcp.org/jcr/1.0}unfiled";

    /**
     * A constant for the absolute path of the unfiled storage node.
     *
     * @since JCR 2.0
     */
    public static final String PATH_UNFILED_NODE = PATH_SYSTEM_NODE + "/" + NAME_UNFILED_NODE;

    /**
     * A constant for the name of the <code>jcr:xmltext<code> node produced on {@link #importXML}.
     *
     * @since JCR 2.0
     */
    public static final String NAME_JCR_XMLTEXT = "{http://www.jcp.org/jcr/1.0}xmltext";

    /**
     * A constant for the name of the <code>jcr:xmlcharacters<code> property produced on {@link #importXML}.
     *
     * @since JCR 2.0
     */
    public static final String NAME_JCR_XMLCHARACTERS = "{http://www.jcp.org/jcr/1.0}xmlcharacters";

    /**
     * A constant for the relative path from the node representing the imported XML element of
     * the <code>jcr:xmlcharacters<code> property produced on {@link #importXML}.
     *
     * @since JCR 2.0
     */
    public static final String RELPATH_JCR_XMLCHARACTERS = NAME_JCR_XMLTEXT + "/" + NAME_JCR_XMLCHARACTERS ;

    /**
     * Returns the <code>LockManager</code> object, through which locking
     * methods are accessed.
     *
     * @throws UnsupportedRepositoryOperationException if the implementation
     * does not support locking.
     * @throws RepositoryException if an error occurs.
     * @return the <code>LockManager</code> object.
     * @since JCR 2.0
     */
    public LockManager getLockManager() throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns the <code>VersionManager</code> object.
     * <p>
     * If the implementation does not support versioning, an
     * <code>UnsupportedRepositoryOperationException</code> is thrown.
     *
     * @throws UnsupportedRepositoryOperationException if the implementation does
     * not support versioning.
     * @throws RepositoryException if an error occurs.
     *
     * @return an <code>VersionManager</code> object.
     */
    public VersionManager getVersionManager() throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Creates a new <code>Workspace</code> with the specified
     * <code>name</code>. The new workspace is empty, meaning it contains only
     * root node.
     * <p>
     * The new workspace can be accessed through a <code>login</code>
     * specifying its name.
     * <p>
     * Throws an <code>AccessDeniedException</code> if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to create the new workspace.
     * <p>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if the repository does
     * not support the creation of workspaces.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param name A <code>String</code>, the name of the new workspace.
     * @throws AccessDeniedException if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to create the new workspace.
     * @throws UnsupportedRepositoryOperationException if the repository does
     * not support the creation of workspaces.
     * @throws NoSuchWorkspaceException
     * @since JCR 2.0
     */
    public void createWorkspace(String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Creates a new <code>Workspace</code> with the specified <code>name</code>
     * initialized with a <code>clone</code> of the content of the workspace
     * <code>srcWorkspace</code>. Semantically, this method is equivalent to
     * creating a new workspace and manually cloning <code>srcWorkspace</code>
     * to it; however, this method may assist some implementations in optimizing
     * subsequent <code>Node.update</code> and <code>Node.merge</code> calls
     * between the new workspace and its source.
     * <p>
     * The new workspace can be accessed through a <code>login</code>
     * specifying its name.
     * <p>
     * Throws an <code>AccessDeniedException</code> if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to create the new workspace.
     * <p>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if the repository does
     * not support the creation of workspaces.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param name A <code>String</code>, the name of the new workspace.
     * @param srcWorkspace The name of the workspace from which the new workspace is to be cloned.
     * @throws AccessDeniedException if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to create the new workspace.
     * @throws UnsupportedRepositoryOperationException if the repository does
     * not support the creation of workspaces.
     * @throws NoSuchWorkspaceException is <code>srcWorkspace</code> does not exist.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public void createWorkspace(String name, String srcWorkspace)
            throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException;

    /**
     * Deletes the workspace with the specified <code>name</code> from the
     * repository, deleting all content within it.
     * <p>
     * Throws an <code>AccessDeniedException</code> if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to remove the workspace.
     * <p>
     * Throws an <code>UnsupportedRepositoryOperationException</code> if the
     * repository does not support the removal of workspaces.
     *
     * @param name A <code>String</code>, the name of the workspace to be deleted.
     * @throws AccessDeniedException if the session through which
     * this <code>Workspace</code> object was acquired does not have permission
     * to remove the workspace.
     * @throws UnsupportedRepositoryOperationException if the
     * repository does not support the removal of workspaces.
     * @throws NoSuchWorkspaceException is <code>srcWorkspace</code> does not exist.
     * @throws RepositoryException if another error occurs.
     * @since JCR 2.0
     */
    public void deleteWorkspace(String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException;
    


}
