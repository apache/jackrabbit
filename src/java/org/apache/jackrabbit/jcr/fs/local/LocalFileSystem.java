/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.fs.local;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.jcr.fs.FileSystem;
import org.apache.jackrabbit.jcr.fs.FileSystemException;

import java.io.*;

/**
 * A <code>LocalFileSystem</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.17 $, $Date: 2004/08/25 16:44:53 $
 */
public class LocalFileSystem implements FileSystem {

    private static Logger log = Logger.getLogger(LocalFileSystem.class);

    private String rootPath;
    private File root;

    /**
     * Default constructor
     */
    public LocalFileSystem() {
    }

    public String getPath() {
	return rootPath;
    }

    public void setPath(String path) {
	rootPath = osPath(path);
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
	    return (root == null ? other.root == null : root.equals(other.root)) &&
		    (rootPath == null ? other.rootPath == null : rootPath.equals(other.rootPath));
	}
	return false;
    }

    //-----------------------------------------------------------< FileSystem >
    /**
     * @see FileSystem#init()
     */
    public void init() throws FileSystemException {
	if (rootPath == null) {
	    String msg = "path not set";
	    log.error(msg);
	    throw new FileSystemException(msg);
	}
	root = new File(rootPath);

	if (root.exists()) {
	    if (!root.isDirectory()) {
		String msg = "path does not denote a folder";
		log.error(msg);
		throw new FileSystemException(msg);
	    }
	} else {
	    if (!root.mkdirs()) {
		String msg = "failed to create root";
		log.error(msg);
		throw new FileSystemException(msg);
	    }
	}
    }

    /**
     * @see FileSystem#close
     */
    public void close() throws FileSystemException {
	root = null;
    }

    /**
     * @see FileSystem#copy(String, String)
     */
    public void copy(String srcPath, String destPath) throws FileSystemException {
	File src = new File(root, osPath(srcPath));
	File dest = new File(root, osPath(destPath));
	try {
	    FileUtil.copy(src, dest);
	} catch (IOException ioe) {
	    String msg = "copying " + src.getPath() + " to " + dest.getPath() + " failed";
	    log.error(msg, ioe);
	    throw new FileSystemException(msg, ioe);
	}
    }

    /**
     * @see FileSystem#createFolder(String)
     */
    public void createFolder(String folderPath) throws FileSystemException {
	File f = new File(root, osPath(folderPath));
	if (f.exists()) {
	    String msg = f.getPath() + " already exists";
	    log.error(msg);
	    throw new FileSystemException(msg);
	}
	if (!f.mkdirs()) {
	    String msg = "failed to create folder " + f.getPath();
	    log.error(msg);
	    throw new FileSystemException(msg);
	}
    }

    /**
     * @see FileSystem#deleteFile(String)
     */
    public void deleteFile(String filePath) throws FileSystemException {
	File f = new File(root, osPath(filePath));
	if (!f.isFile()) {
	    String msg = f.getPath() + " does not denote an existing file";
	    log.error(msg);
	    throw new FileSystemException(msg);
	}
	try {
	    FileUtil.delete(f);
	} catch (IOException ioe) {
	    String msg = "failed to delete " + f.getPath();
	    log.error(msg, ioe);
	    throw new FileSystemException(msg, ioe);
	}
    }

    /**
     * @see FileSystem#deleteFolder(String)
     */
    public void deleteFolder(String folderPath) throws FileSystemException {
	File f = new File(root, osPath(folderPath));
	if (!f.isDirectory()) {
	    String msg = f.getPath() + " does not denote an existing folder";
	    log.error(msg);
	    throw new FileSystemException(msg);
	}
	try {
	    FileUtil.delete(f);
	} catch (IOException ioe) {
	    String msg = "failed to delete " + f.getPath();
	    log.error(msg, ioe);
	    throw new FileSystemException(msg, ioe);
	}
    }

    /**
     * @see FileSystem#exists(String)
     */
    public boolean exists(String path) throws FileSystemException {
	File f = new File(root, osPath(path));
	return f.exists();
    }

    /**
     * @see FileSystem#getInputStream(String)
     */
    public InputStream getInputStream(String filePath) throws FileSystemException {
	File f = new File(root, osPath(filePath));
	try {
	    return new FileInputStream(f);
	} catch (FileNotFoundException fnfe) {
	    String msg = f.getPath() + " does not denote an existing file";
	    log.error(msg, fnfe);
	    throw new FileSystemException(msg, fnfe);
	}
    }

    /**
     * @see FileSystem#getOutputStream(String)
     */
    public OutputStream getOutputStream(String filePath) throws FileSystemException {
	File f = new File(root, osPath(filePath));
	try {
	    return new FileOutputStream(f);
	} catch (FileNotFoundException fnfe) {
	    String msg = "failed to get output stream for " + f.getPath();
	    log.error(msg, fnfe);
	    throw new FileSystemException(msg, fnfe);
	}
    }

    /**
     * @see FileSystem#hasChildren(String)
     */
    public boolean hasChildren(String path) throws FileSystemException {
	File f = new File(root, osPath(path));
	if (!f.exists()) {
	    String msg = f.getPath() + " does not exist";
	    log.error(msg);
	    throw new FileSystemException(msg);
	}
	if (f.isFile()) {
	    return false;
	}
	return (f.list().length > 0);
    }

    /**
     * @see FileSystem#isFile(String)
     */
    public boolean isFile(String path) throws FileSystemException {
	File f = new File(root, osPath(path));
	return f.isFile();
    }

    /**
     * @see FileSystem#isFolder(String)
     */
    public boolean isFolder(String path) throws FileSystemException {
	File f = new File(root, osPath(path));
	return f.isDirectory();
    }

    /**
     * @see FileSystem#lastModified(String)
     */
    public long lastModified(String path) throws FileSystemException {
	File f = new File(root, osPath(path));
	return f.lastModified();
    }

    /**
     * @see FileSystem#length(String)
     */
    public long length(String filePath) throws FileSystemException {
	File f = new File(root, osPath(filePath));
	if (!f.exists()) {
	    return -1;
	}
	return f.length();
    }

    /**
     * @see FileSystem#list(String)
     */
    public String[] list(String folderPath) throws FileSystemException {
	File f = new File(root, osPath(folderPath));
	String[] entries = f.list();
	if (entries == null) {
	    String msg = folderPath + " does not denote a folder";
	    log.error(msg);
	    throw new FileSystemException(msg);
	}
	return entries;
    }

    /**
     * @see FileSystem#listFiles(String)
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
	    log.error(msg);
	    throw new FileSystemException(msg);
	}
	String entries[] = new String[files.length];
	for (int i = 0; i < files.length; i++) {
	    entries[i] = files[i].getName();
	}
	return entries;
    }

    /**
     * @see FileSystem#listFolders(String)
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
	    log.error(msg);
	    throw new FileSystemException(msg);
	}
	String entries[] = new String[folders.length];
	for (int i = 0; i < folders.length; i++) {
	    entries[i] = folders[i].getName();
	}
	return entries;
    }

    /**
     * @see FileSystem#move(String, String)
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
		log.error(msg, ioe);
		throw new FileSystemException(msg, ioe);
	    }
	}
	File destParent = dest.getParentFile();
	if (!destParent.exists()) {
	    // create destination parent folder first
	    if (!destParent.mkdirs()) {
		String msg = "moving " + src.getPath() + " to " + dest.getPath() + " failed";
		log.error(msg);
		throw new FileSystemException(msg);
	    }
	}

	// now we're ready to move/rename the file/folder
	if (!src.renameTo(dest)) {
	    String msg = "moving " + src.getPath() + " to " + dest.getPath() + " failed";
	    log.error(msg);
	    throw new FileSystemException(msg);
	}
/*
        try {
            FileUtil.copy(src, dest);
            FileUtil.delete(src);
        } catch (IOException ioe) {
	    String msg = "moving " + src.getPath() + " to " + dest.getPath() + " failed";
	    log.error(msg, ioe);
	    throw new FileSystemException(msg, ioe);
        }
*/
    }
}
