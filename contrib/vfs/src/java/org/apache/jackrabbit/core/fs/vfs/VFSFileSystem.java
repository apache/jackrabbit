/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.core.fs.vfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs.AllFileSelector;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.FileUtil;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.cache.SoftRefFilesCache;
import org.apache.commons.vfs.impl.StandardFileSystemManager;
import org.apache.commons.vfs.util.RandomAccessMode;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;

/**
 * FileSystem backed by Commons VFS
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class VFSFileSystem implements FileSystem
{
    /**
     * Logger
     */
    private Log log = LogFactory.getLog(VFSFileSystem.class);

    /**
     * File selector
     */
    public final static FileSelector ALL = new AllFileSelector() ;
    
    /**
     * VFS manager
     */
    StandardFileSystemManager fsManager;

    /**
     * Scheme
     */
    private String prefix;

    /**
     * Path
     */
    private String path;

    /**
     * The config file
     */
    private String config;

    /**
     * 
     */
    public VFSFileSystem()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#init()
     */
    public void init() throws org.apache.jackrabbit.core.fs.FileSystemException
    {

        if (this.path == null)
        {
            String msg = "Path is not set";
            log.error(msg);
            throw new org.apache.jackrabbit.core.fs.FileSystemException(msg);
        }

        if (this.config == null)
        {
            String msg = "Configuration file name is not set (\"config\" parameter ).";
            log.error(msg);
            throw new org.apache.jackrabbit.core.fs.FileSystemException(msg);
        }

        try
        {
            // Init file system
            fsManager = new StandardFileSystemManager();
            
            // Set class loader for resource retrieval
            fsManager.setClassLoader(this.getClass().getClassLoader());
            
            // Configuration file name
            fsManager.setConfiguration(this.getClass().getClassLoader()
                    .getResource(this.config).toExternalForm());
            
            // Set the logger
            fsManager.setLogger(log);
            
            // Cache strategy
            // FIXME: set through configuration
            fsManager.setFilesCache(new SoftRefFilesCache());
            fsManager.init();

            // Set the base folder
            FileObject fo = fsManager
                    .resolveFile(this.prefix + ":" + this.path);
            fsManager.setBaseFile(fo);
            
        } catch (FileSystemException e)
        {
            String msg = "Unable to init VFS FileSystem";
            log.error(msg, e);
            throw new org.apache.jackrabbit.core.fs.FileSystemException(msg, e);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#close()
     */
    public void close()
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        this.fsManager.close() ;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#getInputStream(java.lang.String)
     */
    public InputStream getInputStream(String filePath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            FileObject file = this.getFile(filePath);
            this.validateFile(file);
            return file.getContent().getInputStream();
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#getOutputStream(java.lang.String)
     */
    public OutputStream getOutputStream(String filePath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            FileObject file = this.getFile(filePath);
            if (!file.exists())
            {
                file.createFile();
            }
            this.validateFile(file);
            return file.getContent().getOutputStream();
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#getRandomAccessOutputStream(java.lang.String)
     */
    public RandomAccessOutputStream getRandomAccessOutputStream(String filePath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            FileObject file = this.getFile(filePath);
            this.validateFile(file);
            RandomAccessContent raf = file.getContent().getRandomAccessContent(
                    RandomAccessMode.READWRITE);
            return new VFSRAFOutputStream(raf);
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#createFolder(java.lang.String)
     */
    public void createFolder(String folderPath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            FileObject folder = this.getFile(folderPath);
            folder.createFolder();
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#exists(java.lang.String)
     */
    public boolean exists(String path)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            return this.getFile(path).exists();
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#isFile(java.lang.String)
     */
    public boolean isFile(String path)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            return this.getFile(path).getType().equals(FileType.FILE);
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#isFolder(java.lang.String)
     */
    public boolean isFolder(String path)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            return this.getFile(path).getType().equals(FileType.FOLDER);
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#hasChildren(java.lang.String)
     */
    public boolean hasChildren(String path)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            return this.getFile(path).getChildren().length > 0;
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#length(java.lang.String)
     */
    public long length(String filePath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            FileObject file = this.getFile(filePath);
            this.validateFile(file);
            return file.getContent().getSize();
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#lastModified(java.lang.String)
     */
    public long lastModified(String path)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            return this.getFile(path).getContent().getLastModifiedTime();
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#touch(java.lang.String)
     */
    public void touch(String filePath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            FileObject file = this.getFile(filePath);
            this.validateFile(file);
            file.getContent().setLastModifiedTime(
                    Calendar.getInstance().getTimeInMillis());
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#list(java.lang.String)
     */
    public String[] list(String folderPath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        return this.list(this.getFile(folderPath), null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#listFiles(java.lang.String)
     */
    public String[] listFiles(String folderPath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        return this.list(this.getFile(folderPath), FileType.FILE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#listFolders(java.lang.String)
     */
    public String[] listFolders(String folderPath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        return this.list(this.getFile(folderPath), FileType.FOLDER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#deleteFile(java.lang.String)
     */
    public void deleteFile(String filePath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            FileObject file = this.getFile(filePath);
            this.validateFile(file);
            this.delete(file);
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#deleteFolder(java.lang.String)
     */
    public void deleteFolder(String folderPath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            FileObject folder = this.getFile(folderPath);
            this.validateFolder(folder);
            this.delete(folder);
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#move(java.lang.String,
     *      java.lang.String)
     */
    public void move(String srcPath, String destPath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            FileObject src = this.getFile(srcPath);
            FileObject dest = this.getFile(destPath);
            src.moveTo(dest);
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jackrabbit.core.fs.FileSystem#copy(java.lang.String,
     *      java.lang.String)
     */
    public void copy(String srcPath, String destPath)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            FileObject src = this.getFile(srcPath);
            FileObject dest = this.getFile(destPath);
            FileUtil.copyContent(src, dest);
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        } catch (IOException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /**
     * Gets the FileObject for the given path
     * 
     * @param path
     * @return FileSystem
     * @throws FileSystemException
     */
    private FileObject getFile(String path)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            if (path.startsWith("/")) {
                path = path.substring(1, path.length()) ;
            }
            return fsManager.resolveFile(path);
        } catch (FileSystemException e)
        {
            String msg = "Unable to get file " + path;
            log.error(msg, e);
            throw new org.apache.jackrabbit.core.fs.FileSystemException(msg, e);
        }
    }

    /**
     * Validates Folder Type
     * 
     * @param folder
     * @throws FileSystemException
     */
    private void validateFolder(FileObject folder) throws FileSystemException
    {
        if (!folder.getType().equals(FileType.FOLDER))
        {
            String msg = folder.getName().getPath()
                    + " does not denote a folder";
            log.error(msg);
            throw new FileSystemException(msg);
        }
    }

    /**
     * Validates File Type
     * 
     * @param folder
     * @throws FileSystemException
     */
    private void validateFile(FileObject file) throws FileSystemException
    {
        if (!file.getType().equals(FileType.FILE))
        {
            String msg = file.getName().getPath() + " does not denote a file";
            log.error(msg);
            throw new FileSystemException(msg);
        }
    }

    /**
     * List the children for the given Type.
     * 
     * @param path
     * @param type
     * @return
     * @throws org.apache.jackrabbit.core.fs.FileSystemException
     */
    private String[] list(FileObject folder, FileType type)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            this.validateFolder(folder);
            FileObject[] fo = folder.getChildren();
            Collection c = new ArrayList();
            for (int i = 0; i < fo.length; i++)
            {
                if (type == null)
                {
                    c.add(fo[i].getName().getBaseName());
                } else
                {
                    if (fo[i].getType().equals(type))
                    {
                        c.add(fo[i].getName().getBaseName());
                    }
                }
            }
            return (String[]) c.toArray(new String[c.size()]);
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    /**
     * Deletes the given File
     */
    private void delete(FileObject file)
            throws org.apache.jackrabbit.core.fs.FileSystemException
    {
        try
        {
            file.delete(ALL);
        } catch (FileSystemException e)
        {
            throw new org.apache.jackrabbit.core.fs.FileSystemException(e);
        }
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    /**
     * Makes a file canonical
     */
    public static File getCanonicalFile(final File file)
    {
        try
        {
            return file.getCanonicalFile();
        } catch (IOException e)
        {
            return file.getAbsoluteFile();
        }
    }

    public String getConfig()
    {
        return config;
    }

    public void setConfig(String config)
    {
        this.config = config;
    }

    /**
     * @return Returns the scheme.
     */
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * @param scheme
     *            The scheme to set.
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }
}