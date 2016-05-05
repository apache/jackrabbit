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
package org.apache.jackrabbit.vfs.ext.ds;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.jackrabbit.core.data.DataStoreException;

/**
 * VFS Utilities.
 */
class VFSUtils {

    static FileObject createChildFolder(FileObject baseFolder, String name) throws DataStoreException {
        FileObject childFolder = null;

        try {
            childFolder = baseFolder.resolveFile(name);

            if (!childFolder.exists()) {
                childFolder.createFolder();
                childFolder = baseFolder.getChild(childFolder.getName().getBaseName());
            }
        } catch (FileSystemException e) {
            throw new DataStoreException(
                    "Could not create a child folder, '" + name + "' under " + baseFolder.getName().getURI(),
                    e);
        }

        return childFolder;
    }

    static FileObject createChildFile(FileObject baseFolder, String name) throws DataStoreException {
        FileObject childFile = null;

        try {
            childFile = baseFolder.resolveFile(name);

            if (!childFile.exists()) {
                childFile.createFile();
                childFile = baseFolder.getChild(childFile.getName().getBaseName());
            }
        } catch (FileSystemException e) {
            throw new DataStoreException(
                    "Could not create a child file, '" + name + "' under " + baseFolder.getName().getURI(),
                    e);
        }

        return childFile;
    }

    private VFSUtils() {
    }

}
