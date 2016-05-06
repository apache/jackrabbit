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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.jackrabbit.core.data.DataStoreException;

/**
 * VFS Utilities.
 */
class VFSUtils {

    static Set<FileType> FILE_ONLY_TYPES = Collections
            .unmodifiableSet(new HashSet<FileType>(Arrays.asList(FileType.FILE)));

    static Set<FileType> FOLDER_ONLY_TYPES = Collections
            .unmodifiableSet(new HashSet<FileType>(Arrays.asList(FileType.FOLDER)));

    static Set<FileType> FILE_OR_FOLDER_TYPES = Collections
            .unmodifiableSet(new HashSet<FileType>(Arrays.asList(FileType.FILE, FileType.FOLDER)));

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

    static List<FileObject> getChildFiles(FileObject folderObject) throws DataStoreException {
        return getChildrenOfTypes(folderObject, FILE_ONLY_TYPES);
    }

    static List<FileObject> getChildFolders(FileObject folderObject) throws DataStoreException {
        return getChildrenOfTypes(folderObject, FOLDER_ONLY_TYPES);
    }

    static List<FileObject> getChildFileOrFolders(FileObject folderObject) throws DataStoreException {
        return getChildrenOfTypes(folderObject, FILE_OR_FOLDER_TYPES);
    }

    static boolean hasAnyChildFileOrFolder(FileObject folderObject) throws DataStoreException {
        return !getChildFileOrFolders(folderObject).isEmpty();
    }

    static List<FileObject> getChildrenOfTypes(FileObject folderObject, Set<FileType> fileTypes) throws DataStoreException {
        try {
            FileObject [] children = folderObject.getChildren();
            List<FileObject> files = new ArrayList<FileObject>(children.length);
            FileType fileType;
            for (int i = 0; i < children.length; i++) {
                fileType = children[i].getType();
                if (fileTypes.contains(fileType)) {
                    files.add(children[i]);
                }
            }
            return files;
        } catch (FileSystemException e) {
            throw new DataStoreException(
                    "Could not find children under " + folderObject.getName().getURI(), e);
        }
    }

    private VFSUtils() {
    }

}
