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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A <code>FileSystemResource</code> represents a resource (i.e. file) in a
 * <code>FileSystem</code>.
 */
public class FileSystemResource {

    protected final FileSystem fs;

    protected final String path;

    /**
     * Creates a new <code>FileSystemResource</code>
     *
     * @param fs   the <code>FileSystem</code> where the resource is located
     * @param path the path of the resource in the <code>FileSystem</code>
     */
    public FileSystemResource(FileSystem fs, String path) {
        if (fs == null) {
            throw new IllegalArgumentException("invalid file system argument");
        }
        this.fs = fs;

        if (path == null) {
            throw new IllegalArgumentException("invalid path argument");
        }
        this.path = path;
    }

    /**
     * Returns the <code>FileSystem</code> where this resource is located.
     *
     * @return the <code>FileSystem</code> where this resource is located.
     */
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Returns the path of this resource.
     *
     * @return the path of this resource.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the parent directory of this resource.
     *
     * @return the parent directory.
     */
    public String getParentDir() {
        return FileSystemPathUtil.getParentDir(path);
    }

    /**
     * Returns the name of this resource.
     *
     * @return the name.
     */
    public String getName() {
        return FileSystemPathUtil.getName(path);
    }

    /**
     * Creates the parent directory of this resource, including any necessary
     * but nonexistent parent directories.
     *
     * @throws FileSystemException
     */
    public void makeParentDirs() throws FileSystemException {
        String parentDir = getParentDir();
        if (!fs.exists(parentDir)) {
            fs.createFolder(parentDir);
        }
    }

    /**
     * @see FileSystem#deleteFile
     */
    public void delete() throws FileSystemException {
        fs.deleteFile(path);
    }

    /**
     * @see FileSystem#exists
     */
    public boolean exists() throws FileSystemException {
        return fs.exists(path);
    }

    /**
     * @see FileSystem#getInputStream
     */
    public InputStream getInputStream() throws FileSystemException {
        return fs.getInputStream(path);
    }

    /**
     * Spools this resource to the given output stream.
     *
     * @param out output stream where to spool the resource
     * @throws FileSystemException if the input stream for this resource could
     *                             not be obtained
     * @throws IOException         if an error occurs while while spooling
     * @see FileSystem#getInputStream
     */
    public void spool(OutputStream out) throws FileSystemException, IOException {
        InputStream in = fs.getInputStream(path);
        try {
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * @see FileSystem#getOutputStream
     */
    public OutputStream getOutputStream() throws FileSystemException {
        return fs.getOutputStream(path);
    }

    /**
     * @see FileSystem#getRandomAccessOutputStream
     */
    public RandomAccessOutputStream getRandomAccessOutputStream()
            throws FileSystemException {
        return fs.getRandomAccessOutputStream(path);
    }

    /**
     * @see FileSystem#lastModified
     */
    public long lastModified() throws FileSystemException {
        return fs.lastModified(path);
    }

    /**
     * @see FileSystem#length
     */
    public long length() throws FileSystemException {
        return fs.length(path);
    }

    /**
     * @see FileSystem#touch
     */
    public void touch() throws FileSystemException {
        fs.touch(path);
    }

    /**
     * @see FileSystem#move
     */
    public void move(String destPath) throws FileSystemException {
        fs.move(path, destPath);
    }

    //-------------------------------------------< java.lang.Object overrides >
    /**
     * Returns the path string of this resource. This is just the
     * string returned by the <code>{@link #getPath}</code> method.
     *
     * @return The path string of this resource
     */
    public String toString() {
        return getPath();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FileSystemResource) {
            FileSystemResource other = (FileSystemResource) obj;
            return (path == null ? other.path == null : path.equals(other.path))
                    && (fs == null ? other.fs == null : fs.equals(other.fs));
        }
        return false;
    }
}
