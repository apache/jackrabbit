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
package org.apache.jackrabbit.jcr.core;

import org.apache.jackrabbit.jcr.fs.FileSystem;

import java.util.HashMap;

/**
 * A <code>WorkspaceDef</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.15 $, $Date: 2004/08/02 16:19:41 $
 */
public abstract class WorkspaceDef {

    private String name;
    private String persistenceManagerClass;
    private HashMap persistenceManagerParams;
    private FileSystem wspStore;
    private FileSystem blobStore;

    /**
     * Package private constructor.
     *
     * @param name                     name of the workspace
     * @param wspStore                 the file system where the workspace stores its state
     * @param blobStore                the file system where the workspace stores BLOB data
     * @param persistenceManagerClass  FQN of class implementing the <code>PersistenceManager</code> interface
     * @param persistenceManagerParams parameters for the <code>PersistenceManager</code>
     */
    WorkspaceDef(String name, FileSystem wspStore, FileSystem blobStore,
		 String persistenceManagerClass, HashMap persistenceManagerParams) {
	this.name = name;
	this.wspStore = wspStore;
	this.blobStore = blobStore;
	this.persistenceManagerClass = persistenceManagerClass;
	this.persistenceManagerParams = persistenceManagerParams;
    }

    /**
     * Returns the name of the workspace
     *
     * @return the name of the workspace
     */
    public String getName() {
	return name;
    }

    /**
     * Returns the FQN of the class implementing the
     * <code>PersistenceManager</code> interface.
     *
     * @return FQN of class
     */
    public String getPersistenceManagerClass() {
	return persistenceManagerClass;
    }

    /**
     * Returns parameters for the <code>PersistenceManager</code>.
     *
     * @return parameters for the <code>PersistenceManager</code>
     */
    public HashMap getPersistenceManagerParams() {
	return persistenceManagerParams;
    }

    /**
     * Returns the file system where the workspace stores its state
     *
     * @return the file system where the workspace stores its state
     */
    public FileSystem getWorkspaceStore() {
	return wspStore;
    }

    /**
     * Returns the file system where the workspace stores BLOB data
     *
     * @return the file system where the workspace stores BLOB data
     */
    public FileSystem getBlobStore() {
	return blobStore;
    }

    /**
     * Returns true if the workspace is dynamic, i.e. transparently based
     * on a stable workspace, otherwise returns false.
     *
     * @return true if the workspace is dynamic, otherwise false
     */
    public abstract boolean isDynamic();
}
