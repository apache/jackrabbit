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
package org.apache.jackrabbit.core.fs;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A <code>BasedFileSystem</code> represents a 'file system in a file system'.
 */
public class BasedFileSystem implements FileSystem {

    protected final FileSystem fsBase;

    protected final String basePath;

    /**
     * Creates a new <code>BasedFileSystem</code>
     *
     * @param fsBase      the <code>FileSystem</code> the new file system should be based on
     * @param relRootPath the root path relative to <code>fsBase</code>'s root
     */
    public BasedFileSystem(FileSystem fsBase, String relRootPath) {
        if (fsBase == null) {
            throw new IllegalArgumentException("invalid file system argument");
        }
        this.fsBase = fsBase;

        if (relRootPath == null) {
            throw new IllegalArgumentException("invalid null path argument");
        }
        if (relRootPath.equals(SEPARATOR)) {
            throw new IllegalArgumentException("invalid path argument");
        }
        if (!relRootPath.startsWith(SEPARATOR)) {
            relRootPath = SEPARATOR + relRootPath;
        }
        if (relRootPath.endsWith(SEPARATOR)) {
            relRootPath = relRootPath.substring(0, relRootPath.length() - 1);

        }
        this.basePath = relRootPath;
    }

    protected String buildBasePath(String path) {
        if (path.startsWith(SEPARATOR)) {
            return basePath + path;
        } else {
            return basePath + SEPARATOR + path;
        }
    }

    //-----------------------------------------------------------< FileSystem >
    /**
     * @see FileSystem#init
     */
    public void init() throws FileSystemException {
        // check base path
        if (!fsBase.isFolder(basePath)) {
            fsBase.createFolder(basePath);
        }
    }

    /**
     * @see FileSystem#close
     */
    public void close() throws FileSystemException {
        // do nothing; base file system should be closed explicitly
    }

    /**
     * @see FileSystem#copy
     */
    public void copy(String srcPath, String destPath) throws FileSystemException {
        fsBase.copy(buildBasePath(srcPath), buildBasePath(destPath));
    }

    /**
     * @see FileSystem#createFolder
     */
    public void createFolder(String folderPath) throws FileSystemException {
        fsBase.createFolder(buildBasePath(folderPath));
    }

    /**
     * @see FileSystem#deleteFile
     */
    public void deleteFile(String filePath) throws FileSystemException {
        fsBase.deleteFile(buildBasePath(filePath));
    }

    /**
     * @see FileSystem#deleteFolder
     */
    public void deleteFolder(String folderPath) throws FileSystemException {
        fsBase.deleteFolder(buildBasePath(folderPath));
    }

    /**
     * @see FileSystem#exists
     */
    public boolean exists(String path) throws FileSystemException {
        return fsBase.exists(buildBasePath(path));
    }

    /**
     * @see FileSystem#getInputStream
     */
    public InputStream getInputStream(String filePath) throws FileSystemException {
        return fsBase.getInputStream(buildBasePath(filePath));
    }

    /**
     * @see FileSystem#getOutputStream
     */
    public OutputStream getOutputStream(String filePath) throws FileSystemException {
        return fsBase.getOutputStream(buildBasePath(filePath));
    }

    /**
     * @see FileSystem#hasChildren
     */
    public boolean hasChildren(String path) throws FileSystemException {
        return fsBase.hasChildren(buildBasePath(path));
    }

    /**
     * @see FileSystem#isFile
     */
    public boolean isFile(String path) throws FileSystemException {
        return fsBase.isFile(buildBasePath(path));
    }

    /**
     * @see FileSystem#isFolder
     */
    public boolean isFolder(String path) throws FileSystemException {
        return fsBase.isFolder(buildBasePath(path));
    }

    /**
     * @see FileSystem#lastModified
     */
    public long lastModified(String path) throws FileSystemException {
        return fsBase.lastModified(buildBasePath(path));
    }

    /**
     * @see FileSystem#length
     */
    public long length(String filePath) throws FileSystemException {
        return fsBase.length(buildBasePath(filePath));
    }

    /**
     * @see FileSystem#touch
     */
    public void touch(String filePath) throws FileSystemException {
        fsBase.touch(buildBasePath(filePath));
    }

    /**
     * @see FileSystem#list
     */
    public String[] list(String folderPath) throws FileSystemException {
        return fsBase.list(buildBasePath(folderPath));
    }

    /**
     * @see FileSystem#listFiles
     */
    public String[] listFiles(String folderPath) throws FileSystemException {
        return fsBase.listFiles(buildBasePath(folderPath));
    }

    /**
     * @see FileSystem#listFolders
     */
    public String[] listFolders(String folderPath) throws FileSystemException {
        return fsBase.listFolders(buildBasePath(folderPath));
    }

    /**
     * @see FileSystem#move
     */
    public void move(String srcPath, String destPath) throws FileSystemException {
        fsBase.move(buildBasePath(srcPath), buildBasePath(destPath));
    }
}
