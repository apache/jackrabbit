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
package org.apache.jackrabbit.vfs.ext.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.util.DelegatingFileSystemOptionsBuilder;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.vfs.ext.ds.LazyFileContentInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VFS (Commons-VFS) based <code>FileSystem</code> implementation.
 */
public class VFSFileSystem implements FileSystem {

    private static Logger log = LoggerFactory.getLogger(VFSFileSystem.class);

    /**
     * Configuration property name for the base VFS folder URI.
     */
    static final String BASE_FOLDER_URI = "baseFolderUri";

    /**
     * Configuration property name for the VFS {@link FileSystemManager} class name.
     */
    static final String FILE_SYSTEM_MANAGER_CLASS_NAME = "fileSystemManagerClassName";

    /**
     * The backend VFS configuration.
     */
    private String config;

    /**
     * Property key prefix for FielSystemOptions.
     */
    private static final String FILE_SYSTEM_OPTIONS_PROP_PREFIX = "fso.";

    /**
     * The class name of the VFS {@link FileSystemManager} instance used in this VFS file system.
     * If this is not set, then {@link StandardFileSystemManager} is used by default.
     */
    private String fileSystemManagerClassName;

    /**
     * The VFS {@link FileSystemManager} instance used in this VFS file system.
     * If {@link #fileSystemManagerClassName} is not set, then a {@link StandardFileSystemManager} instance is created by default.
     */
    private FileSystemManager fileSystemManager;

    /**
     * {@link FileSystemOptions} used when resolving the {@link #baseFolder}.
     */
    private FileSystemOptions fileSystemOptions;

    /**
     * Properties used when building a {@link FileSystemOptions} using {@link DelegatingFileSystemOptionsBuilder}.
     */
    private Properties fileSystemOptionsProperties;

    /**
     * The VFS base folder URI where all the entry files are maintained.
     */
    private String baseFolderUri;

    /**
     * The VFS base folder object where all the entry files are maintained.
     */
    private FileObject baseFolder;

    /**
     * Default constructor.
     */
    public VFSFileSystem() {
    }

    /**
     * Return path of configuration properties.
     * 
     * @return path of configuration properties.
     */
    public String getConfig() {
        return config;
    }

    /**
     * Set the configuration properties path.
     * 
     * @param config
     *            path of configuration properties.
     */
    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * Returns the class name of the VFS {@link FileSystemManager} instance used in this VFS file system.
     * @return
     */
    public String getFileSystemManagerClassName() {
        return fileSystemManagerClassName;
    }

    /**
     * Sets the class name of the VFS {@link FileSystemManager} instance used in this VFS file system.
     * If this is not set, then {@link StandardFileSystemManager} is used by default.
     * @param fileSystemManagerClassName
     */
    public void setFileSystemManagerClassName(String fileSystemManagerClassName) {
        this.fileSystemManagerClassName = fileSystemManagerClassName;
    }

    /**
     * Returns the VFS {@link FileSystemManager} instance used in this VFS file system.
     * @return the VFS {@link FileSystemManager} instance used in this VFS file system
     */
    public FileSystemManager getFileSystemManager() {
        return fileSystemManager;
    }

    /**
     * Returns {@link FileSystemOptions} instance used when resolving the {@link #baseFolder}.
     * This may return null if {@link FileSystemOptions} instance was not injected or
     * a {@link #fileSystemOptionsProperties} instance cannot be injected or created.
     * Therefore, the caller should check whether or not this returns null.
     * When returning null, the caller may not use a {@link FileSystemOptions} instance.
     * @return {@link FileSystemOptions} instance used when resolving the {@link #baseFolder}
     */
    public FileSystemOptions getFileSystemOptions() throws FileSystemException {
        if (fileSystemOptions == null) {
            fileSystemOptions = createFileSystemOptions();
        }

        return fileSystemOptions;
    }

    /**
     * Sets the {@link FileSystemOptions} instance used when resolving the {@link #baseFolder}.
     * @param fileSystemOptions {@link FileSystemOptions} instance used when resolving the {@link #baseFolder}
     */
    public void setFileSystemOptions(FileSystemOptions fileSystemOptions) {
        this.fileSystemOptions = fileSystemOptions;
    }

    /**
     * Sets the properties used when building a {@link FileSystemOptions}, using {@link DelegatingFileSystemOptionsBuilder}.
     * @param fileSystemOptionsProperties properties used when building a {@link FileSystemOptions}
     */
    public void setFileSystemOptionsProperties(Properties fileSystemOptionsProperties) {
        this.fileSystemOptionsProperties = fileSystemOptionsProperties;
    }

    /**
     * Sets the properties in a semi-colon delimited string used when building a {@link FileSystemOptions},
     * using {@link DelegatingFileSystemOptionsBuilder}.
     * @param fileSystemOptionsPropertiesInString properties in String
     */
    public void setFileSystemOptionsPropertiesInString(String fileSystemOptionsPropertiesInString) {
        if (fileSystemOptionsPropertiesInString != null) {
            try {
                StringReader reader = new StringReader(fileSystemOptionsPropertiesInString);
                Properties props = new Properties();
                props.load(reader);
                fileSystemOptionsProperties = props;
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not load file system options properties.", e);
            }
        }
    }

    /**
     * Sets the base VFS folder URI.
     * @param baseFolderUri base VFS folder URI
     */
    public void setBaseFolderUri(String baseFolderUri) {
        this.baseFolderUri = baseFolderUri;
    }

    //-------------------------------------------< java.lang.Object overrides >

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof VFSFileSystem) {
            final VFSFileSystem other = (VFSFileSystem) obj;

            if (baseFolder == null) {
                return other.baseFolder == null;
            } else {
                return baseFolder.equals(other.baseFolder);
            }
        }

        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 0;
    }

    //-----------------------------------------------------------< FileSystem >

    @Override
    public void init() throws FileSystemException {
        overridePropertiesFromConfig();

        if (baseFolderUri == null) {
            throw new FileSystemException("VFS base folder URI must be set.");
        }

        fileSystemManager = createFileSystemManager();

        FileName baseFolderName = null;

        try {
            baseFolderName = fileSystemManager.resolveURI(baseFolderUri);

            final FileSystemOptions fso = getFileSystemOptions();

            if (fso != null) {
                baseFolder = fileSystemManager.resolveFile(baseFolderUri, fso);
            } else {
                baseFolder = fileSystemManager.resolveFile(baseFolderUri);
            }

            baseFolder.createFolder();
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            throw new FileSystemException("Could not initialize the VFS base folder at '"
                    + (baseFolderName == null ? "" : baseFolderName.getFriendlyURI()) + "'.", e);
        }

        log.info("VFSFileSystem initialized at the base path, {}.", baseFolderUri);
    }

    @Override
    public void close() throws FileSystemException {
        if (fileSystemManager instanceof DefaultFileSystemManager) {
            ((DefaultFileSystemManager) fileSystemManager).close();
        }
    }

    @Override
    public void createFolder(String folderPath) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(folderPath);

            if (fo.isFolder()) {
                log.debug("Folder already exists at {}.", fo.getName().getFriendlyURI());
                throw new FileSystemException("Folder already exists at " + folderPath + ".");
            }

            fo.createFolder();
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to create folder at " + folderPath + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public void deleteFile(String filePath) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(filePath);

            if (!fo.isFile()) {
                throw new FileSystemException("File doesn't exist at " + filePath + ".");
            }

            fo.delete();
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to delete file at " + filePath;
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public void deleteFolder(String folderPath) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(folderPath);

            if (!fo.isFolder()) {
                String msg = "Folder doesn't exist at " + folderPath + ".";
                log.debug(msg);
                throw new FileSystemException(msg);
            }

            fo.deleteAll();
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to delete folder at " + folderPath + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public boolean exists(String path) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(path);
            return fo.exists();
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to check if file exists at " + path + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public InputStream getInputStream(String filePath) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(filePath);
            final FileObject folder = fo.getParent();

            if (!folder.exists() || !folder.isFolder()) {
                throw new FileSystemException("Folder doesn't exist for " + filePath + ".");
            }

            return new LazyFileContentInputStream(fo);
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to open an input stream from " + filePath + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public OutputStream getOutputStream(String filePath) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(filePath);
            final FileObject folder = fo.getParent();

            if (!folder.exists() || !folder.isFolder()) {
                throw new FileSystemException("Folder doesn't exist for " + filePath + ".");
            }

            return fo.getContent().getOutputStream();
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to open an output stream to " + filePath + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public boolean hasChildren(String path) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(path);

            if (!fo.exists()) {
                final String msg = "File doesn't exist at " + path + ".";
                log.debug(msg);
                throw new FileSystemException(msg);
            }

            if (fo.isFile()) {
                return false;
            }

            return (fo.getChildren().length > 0);
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to check if there's any child at " + path + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public boolean isFile(String path) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(path);
            return fo.isFile();
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to check if it is a file at " + path + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public boolean isFolder(String path) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(path);
            return fo.isFolder();
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to check if it is a folder at " + path + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public long lastModified(String path) throws FileSystemException {
        try (final FileObject fo = resolveFileObject(path)) {
            return fo.getContent().getLastModifiedTime();
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to get the last modified time of the file at " + path + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public long length(String filePath) throws FileSystemException {
        try (final FileObject fo = resolveFileObject(filePath)) {
            return fo.getContent().getSize();
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to get the length of the file at " + filePath + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public String[] list(String folderPath) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(folderPath);
            final FileObject[] children = fo.getChildren();
            final int size = children.length;
            final String[] entries = new String[size];

            for (int i = 0; i < size; i++) {
                entries[i] = children[i].getName().getBaseName();
            }

            return entries;
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to list children of the folder at " + folderPath + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public String[] listFiles(String folderPath) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(folderPath);
            final List<String> entries = new LinkedList<>();

            for (FileObject child : fo.getChildren()) {
                if (child.isFile()) {
                    entries.add(child.getName().getBaseName());
                }
            }

            return entries.toArray(new String[entries.size()]);
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to list child files of the folder at " + folderPath + ".";
            log.debug(msg, e);
            throw new FileSystemException(msg, e);
        }
    }

    @Override
    public String[] listFolders(String folderPath) throws FileSystemException {
        try {
            final FileObject fo = resolveFileObject(folderPath);
            final List<String> entries = new LinkedList<>();

            for (FileObject child : fo.getChildren()) {
                if (child.isFolder()) {
                    entries.add(child.getName().getBaseName());
                }
            }

            return entries.toArray(new String[entries.size()]);
        } catch (org.apache.commons.vfs2.FileSystemException e) {
            final String msg = "Failed to list child folders of the folder at " + folderPath + ".";
            log.debug(msg);
            throw new FileSystemException(msg, e);
        }
    }

    /**
     * Creates a {@link FileSystemManager} instance.
     * @return a {@link FileSystemManager} instance.
     * @throws FileSystemException if an error occurs creating the manager.
     */
    protected FileSystemManager createFileSystemManager() throws FileSystemException {
        FileSystemManager fileSystemManager = null;

        try {
            if (getFileSystemManagerClassName() == null) {
                fileSystemManager = new StandardFileSystemManager();
            } else {
                final Class<?> mgrClass = Class.forName(getFileSystemManagerClassName());
                fileSystemManager = (FileSystemManager) mgrClass.newInstance();
            }

            if (fileSystemManager instanceof DefaultFileSystemManager) {
                ((DefaultFileSystemManager) fileSystemManager).init();
            }
        } catch (final Exception e) {
            throw new FileSystemException(
                    "Could not create file system manager of class: " + getFileSystemManagerClassName(), e);
        }

        return fileSystemManager;
    }

    /**
     * Builds and returns {@link FileSystemOptions} instance which is used when resolving the {@link #baseFolder}
     * during the initialization.
     * If {@link #fileSystemOptionsProperties} is available, this scans all the property key names starting with {@link #FILE_SYSTEM_OPTIONS_PROP_PREFIX}
     * and uses the rest of the key name after the {@link #FILE_SYSTEM_OPTIONS_PROP_PREFIX} as the combination of scheme and property name
     * when building a {@link FileSystemOptions} using {@link DelegatingFileSystemOptionsBuilder}.
     * @return {@link FileSystemOptions} instance which is used when resolving the {@link #baseFolder} during the initialization
     * @throws FileSystemException if any file system exception occurs
     */
    protected FileSystemOptions createFileSystemOptions() throws FileSystemException {
        FileSystemOptions fso = null;

        if (fileSystemOptionsProperties != null) {
            try {
                fso = new FileSystemOptions();
                DelegatingFileSystemOptionsBuilder delegate = new DelegatingFileSystemOptionsBuilder(
                        getFileSystemManager());

                String key;
                String schemeDotPropName;
                String scheme;
                String propName;
                String value;
                int offset;

                for (Enumeration<?> e = fileSystemOptionsProperties.propertyNames(); e.hasMoreElements();) {
                    key = (String) e.nextElement();

                    if (key.startsWith(FILE_SYSTEM_OPTIONS_PROP_PREFIX)) {
                        value = fileSystemOptionsProperties.getProperty(key);
                        schemeDotPropName = key.substring(FILE_SYSTEM_OPTIONS_PROP_PREFIX.length());
                        offset = schemeDotPropName.indexOf('.');

                        if (offset > 0) {
                            scheme = schemeDotPropName.substring(0, offset);
                            propName = schemeDotPropName.substring(offset + 1);
                            delegate.setConfigString(fso, scheme, propName, value);
                        } else {
                            log.warn("Ignoring an FileSystemOptions property in invalid format. Key: {}, Value: {}",
                                    key, value);
                        }
                    }
                }
            } catch (org.apache.commons.vfs2.FileSystemException e) {
                throw new FileSystemException("Could not create File System Options.", e);
            }
        }

        return fso;
    }

    /**
     * Returns properties used when building a {@link FileSystemOptions} instance by the properties
     * during the initialization.
     * @return properties used when building a {@link FileSystemOptions} instance by the properties during the initialization
     */
    protected Properties getFileSystemOptionsProperties() {
        return fileSystemOptionsProperties;
    }

    private void overridePropertiesFromConfig() throws FileSystemException {
        final String config = getConfig();

        // If config param provided, then override properties from the config file.
        if (config != null && !"".equals(config)) {
            try {
                final Properties props = readConfig(config);

                String propValue = props.getProperty(BASE_FOLDER_URI);
                if (propValue != null && !"".equals(propValue)) {
                    setBaseFolderUri(propValue);
                }

                propValue = props.getProperty(FILE_SYSTEM_MANAGER_CLASS_NAME);
                if (propValue != null && !"".equals(propValue)) {
                    setFileSystemManagerClassName(propValue);
                }

                final Properties fsoProps = new Properties();
                String propName;
                for (Enumeration<?> propNames = props.propertyNames(); propNames.hasMoreElements();) {
                    propName = (String) propNames.nextElement();
                    if (propName.startsWith(FILE_SYSTEM_OPTIONS_PROP_PREFIX)) {
                        fsoProps.setProperty(propName, props.getProperty(propName));
                    }
                }
                if (!fsoProps.isEmpty()) {
                    this.setFileSystemOptionsProperties(fsoProps);
                }
            } catch (IOException e) {
                throw new FileSystemException("Configuration file doesn't exist at '" + config + "'.");
            }
        }
    }

    private Properties readConfig(String fileName) throws IOException {
        if (!new File(fileName).exists()) {
            throw new IOException("Config file not found: " + fileName);
        }

        Properties prop = new Properties();

        try (FileInputStream in = new FileInputStream(fileName)) {
            prop.load(in);
        }

        return prop;
    }

    private FileObject resolveFileObject(final String path) throws FileSystemException {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null.");
        }

        final String normalizedPath = (path.startsWith("/") ? path.substring(1) : path).trim();

        if (normalizedPath.isEmpty()) {
            return baseFolder;
        } else {
            try {
                return baseFolder.resolveFile(normalizedPath, NameScope.DESCENDENT);
            } catch (org.apache.commons.vfs2.FileSystemException e) {
                throw new FileSystemException("Cannot resolve file at " + path + ".", e);
            }
        }
    }
}
