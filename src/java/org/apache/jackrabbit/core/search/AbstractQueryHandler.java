/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.search;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.state.ItemStateManager;

import java.io.IOException;

/**
 * Implements default behaviour for some methods of {@link QueryHandler}.
 */
public abstract class AbstractQueryHandler implements QueryHandler {

    /**
     * A <code>FileSystem</code> to store the search index
     */
    private FileSystem fs;

    /**
     * The persistent <code>ItemStateManager</code>
     */
    private ItemStateManager stateProvider;

    /**
     * PropertyType registry to look up the type of a property with a given name.
     */ 
    private PropertyTypeRegistry propRegistry;

    /**
     * The UUID of the root node.
     */
    private String rootUUID;

    /**
     * Initializes this query handler by setting all properties in this class
     * with appropriate parameter values.
     *
     * @param fs            a {@link FileSystem} this
     *                      <code>QueryHandler</code> may use to store its index.
     * @param stateProvider provides persistent item states.
     * @param rootUUID the uuid of the root node.
     * @param registry the property type registry.
     */
    public final void init(FileSystem fs, 
                           ItemStateManager stateProvider, 
                           String rootUUID,
                           PropertyTypeRegistry registry)
            throws IOException {
        this.fs = fs;
        this.stateProvider = stateProvider;
        this.rootUUID = rootUUID;
        this.propRegistry = registry;
        doInit();
    }

    /**
     * This method must be implemented by concrete sub classes and will be
     * called from {@link #init}.
     */
    protected abstract void doInit() throws IOException;

    /**
     * Returns the persistent {@link ItemStateManager}
     * of the workspace this <code>QueryHandler</code> is based on.
     *
     * @return the persistent <code>ItemStateProvider</code> of the current
     *         workspace.
     */
    protected ItemStateManager getItemStateProvider() {
        return stateProvider;
    }

    /**
     * Returns the {@link org.apache.jackrabbit.core.fs.FileSystem} instance
     * this <code>QueryHandler</code> may use to store its index.
     *
     * @return the <code>FileSystem</code> instance for this
     *         <code>QueryHandler</code>.
     */
    protected FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Returns the UUID of the root node.
     * @return the UUID of the root node.
     */
    protected String getRootUUID() {
        return rootUUID;
    }

    /**
     * Returns the PropertyTypeRegistry for this repository.
     * @return the PropertyTypeRegistry for this repository.
     */
    protected PropertyTypeRegistry getPropertyTypeRegistry() {
        return propRegistry;
    }
}
