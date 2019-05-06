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
package org.apache.jackrabbit.core.query.lucene.directory;

import org.apache.lucene.store.Directory;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;

import java.io.IOException;

/**
 * <code>DirectoryManager</code> defines an interface for managing directory
 * instances used by the search index.
 */
public interface DirectoryManager {

    /**
     * Initializes the directory manager with a reference to the search index.
     *
     * @param handler the query handler implementation.
     * @throws IOException if an error occurs while initializing the directory
     *          manager.
     */
    void init(SearchIndex handler) throws IOException;

    /**
     * Checks if there exists a directory with the given <code>name</code>.
     *
     * @param name the name of a directory.
     * @return <code>true</code> if the directory exists; <code>false</code>
     *          otherwise.
     * @throws IOException if an error occurs while looking up directories.
     */
    boolean hasDirectory(String name) throws IOException;

    /**
     * Gets the directory with the given <code>name</code>. If the directory
     * does not yet exist then it will be created.
     *
     * @param name the name of a directory.
     * @return the directory.
     * @throws IOException if an error occurs while getting or creating the
     *          directory.
     */
    Directory getDirectory(String name) throws IOException;

    /**
     * Returns the names of the currently available directories.
     *
     * @return names of the currently available directories.
     * @throws IOException if an error occurs while retrieving the directory
     *          names.
     */
    String[] getDirectoryNames() throws IOException;

    /**
     * Deletes the directory with the given name.
     *
     * @param name the name of the directory to delete.
     * @return <code>true</code> if the directory could be deleted successfully,
     *          <code>false</code> otherwise. This method also returns
     *          <code>false</code> when the directory with the given
     *          <code>name</code> does not exist.
     */
    boolean delete(String name);

    /**
     * Renames a directory.
     *
     * @param from the name of the directory to rename.
     * @param to the new name for the directory.
     * @return <code>true</code> if the directory was successfully renamed.
     *          Returns <code>false</code> if there is no directory with name
     *          <code>from</code> or there already exists a directory with name
     *          <code>to</code> or an error occurs while renaming the directory.
     */
    boolean rename(String from, String to);

    /**
     * Frees resources associated with this directory manager.
     */
    void dispose();
}
