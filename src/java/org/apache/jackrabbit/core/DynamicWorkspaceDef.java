/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.fs.FileSystem;

import java.util.HashMap;

/**
 * A <code>DynamicWorkspaceDef</code> defines a dynamic workspace, i.e.
 * a workspace that physically only stores those items that differ from
 * the items in the underlying stable workspace.
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.13 $, $Date: 2004/08/02 16:19:40 $
 * @see WorkspaceDef
 * @see StableWorkspaceDef
 */
public class DynamicWorkspaceDef extends WorkspaceDef {

    private String stableWorkspace;

    /**
     * Creates a <code>DynamicWorkspaceDef</code> object, defining a dynamic
     * workspace.
     *
     * @param name                     name of the dynamic workspace
     * @param wspStore                 file system where the dynamic workspace stores its state
     * @param blobStore                file system where the dynamic workspace stores BLOB data
     * @param persistenceManagerClass  FQN of class implementing the <code>PersistenceManager</code> interface
     * @param persistenceManagerParams parameters for the <code>PersistenceManager</code>
     * @param stableWorkspace          name of the stable workspace the dynamic
     *                                 workspace should be based on
     */
    DynamicWorkspaceDef(String name, FileSystem wspStore, FileSystem blobStore,
			String persistenceManagerClass, HashMap persistenceManagerParams,
			String stableWorkspace) {
	super(name, wspStore, blobStore, persistenceManagerClass, persistenceManagerParams);
	this.stableWorkspace = stableWorkspace;
    }

    /**
     * Returns the name of the stable workspace (i.e. the workspace the dynamic
     * workspace should be based on).
     *
     * @return the name of the stable workspace.
     */
    public String getStableWorkspace() {
	return stableWorkspace;
    }

    /**
     * Returns true if the workspace is dynamic, i.e. transparently based
     * on a stable workspace, otherwise returns false.
     *
     * @return true if the workspace is dynamic, otherwise false
     */
    public boolean isDynamic() {
	return true;
    }
}
