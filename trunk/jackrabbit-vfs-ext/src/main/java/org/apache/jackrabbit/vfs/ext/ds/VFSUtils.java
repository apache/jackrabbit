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

    /**
     * A set to filter out other files other than {@link FileType#FILE}.
     */
    static Set<FileType> FILE_ONLY_TYPES = Collections
            .unmodifiableSet(new HashSet<FileType>(Arrays.asList(FileType.FILE)));

    /**
     * A set to filter out other files other than {@link FileType#FOLDER}.
     */
    static Set<FileType> FOLDER_ONLY_TYPES = Collections
            .unmodifiableSet(new HashSet<FileType>(Arrays.asList(FileType.FOLDER)));

    /**
     * A set to filter out other files other than {@link FileType#FILE} or {@link FileType#FOLDER}.
     */
    static Set<FileType> FILE_OR_FOLDER_TYPES = Collections
            .unmodifiableSet(new HashSet<FileType>(Arrays.asList(FileType.FILE, FileType.FOLDER)));

    /**
     * Creates a child folder by the {@code name} under the {@code baseFolder} and retrieves the created folder.
     * @param baseFolder base folder object
     * @param name child folder name
     * @return a child folder by the {@code name} under the {@code baseFolder} and retrieves the created folder
     * @throws DataStoreException if any file system exception occurs
     */
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
                    "Could not create a child folder, '" + name + "' under " + baseFolder.getName().getFriendlyURI(),
                    e);
        }

        return childFolder;
    }

    /**
     * Creates a child file by the {@code name} under the {@code baseFolder} and retrieves the created file.
     * @param baseFolder base folder object
     * @param name child file name
     * @return a child file by the {@code name} under the {@code baseFolder} and retrieves the created file
     * @throws DataStoreException if any file system exception occurs
     */
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
                    "Could not create a child file, '" + name + "' under " + baseFolder.getName().getFriendlyURI(),
                    e);
        }

        return childFile;
    }

    /**
     * Returns child files under {@code folderObject} after filtering out files other than {@link FileType#FILE}.
     * @param folderObject folder object
     * @return child files under {@code folderObject} after filtering out files other than {@link FileType#FILE}
     * @throws DataStoreException if any file system exception occurs
     */
    static List<FileObject> getChildFiles(FileObject folderObject) throws DataStoreException {
        return getChildrenOfTypes(folderObject, FILE_ONLY_TYPES);
    }

    /**
     * Returns child folders under {@code folderObject} after filtering out files other than {@link FileType#FOLDER}.
     * @param folderObject folder object
     * @return child folders under {@code folderObject} after filtering out files other than {@link FileType#FOLDER}
     * @throws DataStoreException if any file system exception occurs
     */
    static List<FileObject> getChildFolders(FileObject folderObject) throws DataStoreException {
        return getChildrenOfTypes(folderObject, FOLDER_ONLY_TYPES);
    }

    /**
     * Returns child file objects under {@code folderObject} after filtering out files other than {@link FileType#FILE} or {@link FileType#FOLDER}.
     * @param folderObject folder object
     * @return child file objects under {@code folderObject} after filtering out files other than {@link FileType#FILE} or {@link FileType#FOLDER}
     * @throws DataStoreException if any file system exception occurs
     */
    static List<FileObject> getChildFileOrFolders(FileObject folderObject) throws DataStoreException {
        return getChildrenOfTypes(folderObject, FILE_OR_FOLDER_TYPES);
    }

    /**
     * Returns true if {@code folderObject} has any file or folder child objects.
     * @param folderObject folder object
     * @return true if {@code folderObject} has any file or folder child objects
     * @throws DataStoreException if any file system exception occurs
     */
    static boolean hasAnyChildFileOrFolder(FileObject folderObject) throws DataStoreException {
        return !getChildFileOrFolders(folderObject).isEmpty();
    }

    private static List<FileObject> getChildrenOfTypes(FileObject folderObject, Set<FileType> fileTypes) throws DataStoreException {
        try {
            String folderBaseName = folderObject.getName().getBaseName();
            FileObject [] children = folderObject.getChildren();
            List<FileObject> files = new ArrayList<FileObject>(children.length);
            String childBaseName;

            for (int i = 0; i < children.length; i++) {
                childBaseName = children[i].getName().getBaseName();
                FileType fileType = null;

                try {
                    fileType = children[i].getType();
                } catch (FileSystemException notDetermineTypeEx) {
                    if (folderBaseName.equals(childBaseName)) {
                        // Ignore this case.
                        // Some WebDAV server or VFS seems to include the folder itself as child as imaginary file type,
                        // and throw FileSystemException saying "Could not determine the type of file" in this case.
                    } else {
                        throw notDetermineTypeEx;
                    }
                }

                if (fileType != null && fileTypes.contains(fileType)) {
                    files.add(children[i]);
                }
            }

            return files;
        } catch (FileSystemException e) {
            throw new DataStoreException(
                    "Could not find children under " + folderObject.getName().getFriendlyURI(), e);
        }
    }

    private VFSUtils() {
    }

}
