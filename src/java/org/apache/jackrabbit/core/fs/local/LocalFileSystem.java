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
package org.apache.jackrabbit.core.fs.local;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * A <code>LocalFileSystem</code> ...
 */
public class LocalFileSystem implements FileSystem {

    private static Logger log = Logger.getLogger(LocalFileSystem.class);

    private File root;

    /**
     * Default constructor
     */
    public LocalFileSystem() {
    }

    public String getPath() {
        if (root != null) {
            return root.getPath();
        } else {
            return null;
        }
    }

    /**
     * Sets the path to the root directory of this local filesystem. please note
     * that this method can be called via reflection during initialization and
     * must not be altered.
     *
     * @param rootPath the path to the root directory
     */
    public void setPath(String rootPath) {
        setRoot(new File(osPath(rootPath)));
    }

    public void setRoot(File root) {
        this.root = root;
    }

    private String osPath(String genericPath) {
        if (File.separator.equals(SEPARATOR)) {
            return genericPath;
        }
        return genericPath.replace(SEPARATOR_CHAR, File.separatorChar);
    }

    //-------------------------------------------< java.lang.Object overrides >
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LocalFileSystem) {
            LocalFileSystem other = (LocalFileSystem) obj;
            if (root == null) {
                return other.root == null;
            } else {
                return root.equals(other.root);
            }
        }
        return false;
    }

    //-----------------------------------------------------------< FileSystem >
    /**
     * {@inheritDoc}
     */
    public void init() throws FileSystemException {
        if (root == null) {
            String msg = "root directory not set";
            log.debug(msg);
            throw new FileSystemException(msg);
        }

        if (root.exists()) {
            if (!root.isDirectory()) {
                String msg = "path does not denote a folder";
                log.debug(msg);
                throw new FileSystemException(msg);
            }
        } else {
            if (!root.mkdirs()) {
                String msg = "failed to create root";
                log.debug(msg);
                throw new FileSystemException(msg);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws FileSystemException {
        root = null;
    }

    /**
     * {@inheritDoc}
     */
    public void copy(String srcPath, String destPath) throws FileSystemException {
        File src = new File(root, osPath(srcPath));
        File dest = new File(root, osPath(destPath));
        try {
            FileUtil.copy(src, dest);
        } catch (IOException ioe) {
            String msg = "copying " + src.getPath() + " to " + dest.getPath() + " failed";
            log.debug(msg);
            throw new FileSystemException(msg, ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void createFolder(String folderPath) throws FileSystemException {
        File f = new File(root, osPath(folderPath));
        if (f.exists()) {
            String msg = f.getPath() + " already exists";
            log.debug(msg);
            throw new FileSystemException(msg);
        }
        if (!f.mkdirs()) {
            String msg = "failed to create folder " + f.getPath();
            log.debug(msg);
            throw new FileSystemException(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteFile(String filePath) throws FileSystemException {
        File f = new File(root, osPath(filePath));
        if (!f.isFile()) {
            String msg = f.getPath() + " does not denote an existing file";
            throw new FileSystemException(msg);
        }
        try {
            FileUtil.delete(f);
        } catch (IOException ioe) {
            String msg = "failed to delete " + f.getPath();
            throw new FileSystemException(msg, ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteFolder(String folderPath) throws FileSystemException {
        File f = new File(root, osPath(folderPath));
        if (!f.isDirectory()) {
            String msg = f.getPath() + " does not denote an existing folder";
            log.debug(msg);
            throw new FileSystemException(msg);
        }
        try {
            FileUtil.delete(f);
        } catch (IOException ioe) {
            String msg = "failed to delete " + f.getPath();
            log.debug(msg);
            throw new FileSystemException(msg, ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(String path) throws FileSystemException {
        File f = new File(root, osPath(path));
        return f.exists();
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream(String filePath) throws FileSystemException {
        File f = new File(root, osPath(filePath));
        try {
            return new FileInputStream(f);
        } catch (FileNotFoundException fnfe) {
            String msg = f.getPath() + " does not denote an existing file";
            log.debug(msg);
            throw new FileSystemException(msg, fnfe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public OutputStream getOutputStream(String filePath) throws FileSystemException {
        File f = new File(root, osPath(filePath));
        try {
            return new FileOutputStream(f);
        } catch (FileNotFoundException fnfe) {
            String msg = "failed to get output stream for " + f.getPath();
            log.debug(msg);
            throw new FileSystemException(msg, fnfe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RandomAccessOutputStream getRandomAccessOutputStream(String filePath)
            throws FileSystemException {
        File f = new File(root, osPath(filePath));
        try {
            return new RAFOutputStream(new RandomAccessFile(f, "rw"));
        } catch (IOException e) {
            String msg = "failed to get output stream for " + f.getPath();
            log.debug(msg);
            throw new FileSystemException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasChildren(String path) throws FileSystemException {
        File f = new File(root, osPath(path));
        if (!f.exists()) {
            String msg = f.getPath() + " does not exist";
            log.debug(msg);
            throw new FileSystemException(msg);
        }
        if (f.isFile()) {
            return false;
        }
        return (f.list().length > 0);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFile(String path) throws FileSystemException {
        File f = new File(root, osPath(path));
        return f.isFile();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFolder(String path) throws FileSystemException {
        File f = new File(root, osPath(path));
        return f.isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    public long lastModified(String path) throws FileSystemException {
        File f = new File(root, osPath(path));
        return f.lastModified();
    }

    /**
     * {@inheritDoc}
     */
    public long length(String filePath) throws FileSystemException {
        File f = new File(root, osPath(filePath));
        if (!f.exists()) {
            return -1;
        }
        return f.length();
    }

    /**
     * {@inheritDoc}
     */
    public void touch(String filePath) throws FileSystemException {
        File f = new File(root, osPath(filePath));
        f.setLastModified(System.currentTimeMillis());
    }

    /**
     * {@inheritDoc}
     */
    public String[] list(String folderPath) throws FileSystemException {
        File f = new File(root, osPath(folderPath));
        String[] entries = f.list();
        if (entries == null) {
            String msg = folderPath + " does not denote a folder";
            log.debug(msg);
            throw new FileSystemException(msg);
        }
        return entries;
    }

    /**
     * {@inheritDoc}
     */
    public String[] listFiles(String folderPath) throws FileSystemException {
        File folder = new File(root, osPath(folderPath));
        File[] files = folder.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isFile();
            }
        });
        if (files == null) {
            String msg = folderPath + " does not denote a folder";
            log.debug(msg);
            throw new FileSystemException(msg);
        }
        String[] entries = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            entries[i] = files[i].getName();
        }
        return entries;
    }

    /**
     * {@inheritDoc}
     */
    public String[] listFolders(String folderPath) throws FileSystemException {
        File file = new File(root, osPath(folderPath));
        File[] folders = file.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        if (folders == null) {
            String msg = folderPath + " does not denote a folder";
            log.debug(msg);
            throw new FileSystemException(msg);
        }
        String[] entries = new String[folders.length];
        for (int i = 0; i < folders.length; i++) {
            entries[i] = folders[i].getName();
        }
        return entries;
    }

    /**
     * {@inheritDoc}
     */
    public void move(String srcPath, String destPath) throws FileSystemException {
        File src = new File(root, osPath(srcPath));
        File dest = new File(root, osPath(destPath));

        if (dest.exists()) {
            // we need to move the existing file/folder out of the way first
            try {
                FileUtil.delete(dest);
            } catch (IOException ioe) {
                String msg = "moving " + src.getPath() + " to " + dest.getPath() + " failed";
                log.debug(msg);
                throw new FileSystemException(msg, ioe);
            }
        }
        File destParent = dest.getParentFile();
        if (!destParent.exists()) {
            // create destination parent folder first
            if (!destParent.mkdirs()) {
                String msg = "moving " + src.getPath() + " to " + dest.getPath() + " failed";
                log.debug(msg);
                throw new FileSystemException(msg);
            }
        }

        // now we're ready to move/rename the file/folder
        if (!src.renameTo(dest)) {
            String msg = "moving " + src.getPath() + " to " + dest.getPath() + " failed";
            log.debug(msg);
            throw new FileSystemException(msg);
        }
/*
        try {
            FileUtil.copy(src, dest);
            FileUtil.delete(src);
        } catch (IOException ioe) {
            String msg = "moving " + src.getPath() + " to " + dest.getPath() + " failed";
            log.debug(msg);
            throw new FileSystemException(msg, ioe);
        }
*/
    }
}
