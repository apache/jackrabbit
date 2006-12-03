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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.lucene.util.Constants;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.OutputStream;
import org.apache.lucene.store.InputStream;

import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

/**
 * This is a wrapper class to provide lock creation in the index directory.
 * <p/>
 * As of lucene 1.3 lock files are created in the users temp directory, which is
 * not very user friendly when the system crashed. One has to look up the temp
 * directory and find out which lock file belongs to which lucene index.
 * <p/>
 * This wrapper class delegates most operations to the default FSDirectory
 * implementation of lucene but has its own makeLock() implementation.
 */
class FSDirectory extends Directory {

    /**
     * Flag indicating whether locks are disabled
     */
    private static final boolean DISABLE_LOCKS =
            Boolean.getBoolean("disableLuceneLocks") || Constants.JAVA_1_1;

    /**
     * The actual FSDirectory implementation
     */
    private final org.apache.lucene.store.FSDirectory delegatee;

    /**
     * The directory where this index is located
     */
    private final File directory;

    /**
     * internal ref count for cached FSDirectories
     */
    private int refCount = 0;

    /**
     * map where the cached FSDirectories are stored
     */
    private static final Map directories = new HashMap();

    /**
     * Creates a new FSDirectory based on a lucene FSDirectory instance.
     *
     * @param delegatee the lucene FSDirectory instance.
     * @param directory the directory where this index is located.
     */
    private FSDirectory(org.apache.lucene.store.FSDirectory delegatee, File directory) {
        this.delegatee = delegatee;
        this.directory = directory;
    }

    /**
     * Returns the directory instance for the named location.
     * <p/>
     * <p>Directories are cached, so that, for a given canonical path, the same
     * FSDirectory instance will always be returned.  This permits
     * synchronization on directories.
     *
     * @param file   the path to the directory.
     * @param create if true, create, or erase any existing contents.
     * @return the FSDirectory for the named file.
     */
    public static FSDirectory getDirectory(File file, boolean create)
            throws IOException {
        FSDirectory dir;
        synchronized (directories) {
            dir = (FSDirectory) directories.get(file.getCanonicalPath());
            if (dir == null) {
                dir = new FSDirectory(org.apache.lucene.store.FSDirectory.getDirectory(file, create), file);
                directories.put(file.getCanonicalPath(), dir);
            }
        }
        synchronized (dir) {
            dir.refCount++;
        }
        return dir;
    }

    /**
     * Creates a lock file in the current index directory.
     *
     * @param name the name of the lock file.
     * @return a Lock object with the given name.
     */
    public Lock makeLock(String name) {
        final File lockFile = new File(directory, name);
        return new Lock() {
            public boolean obtain() throws IOException {
                if (DISABLE_LOCKS) {
                    return true;
                }
                return lockFile.createNewFile();
            }

            public void release() {
                if (DISABLE_LOCKS) {
                    return;
                }
                lockFile.delete();
            }

            public boolean isLocked() {
                if (DISABLE_LOCKS) {
                    return false;
                }
                return lockFile.exists();
            }

            public String toString() {
                return "Lock@" + lockFile;
            }
        };
    }

    /**
     * @inheritDoc
     */
    public synchronized void close()
            throws IOException {
        delegatee.close();
        if (--refCount <= 0) {
            // really close
            synchronized (directories) {
                directories.remove(directory.getCanonicalPath());
            }
        }
    }

    /**
     * @inheritDoc
     */
    public OutputStream createFile(String name)
            throws IOException {
        return delegatee.createFile(name);
    }

    /**
     * @inheritDoc
     */
    public void deleteFile(String name)
            throws IOException {
        delegatee.deleteFile(name);
    }

    /**
     * @inheritDoc
     */
    public boolean fileExists(String name)
            throws IOException {
        return delegatee.fileExists(name);
    }

    /**
     * @inheritDoc
     */
    public long fileLength(String name)
            throws IOException {
        return delegatee.fileLength(name);
    }

    /**
     * @inheritDoc
     */
    public long fileModified(String name)
            throws IOException {
        return delegatee.fileModified(name);
    }

    /**
     * @inheritDoc
     */
    public String[] list()
            throws IOException {
        return delegatee.list();
    }

    /**
     * @inheritDoc
     */
    public InputStream openFile(String name)
            throws IOException {
        return delegatee.openFile(name);
    }

    /**
     * @inheritDoc
     */
    public void renameFile(String from, String to)
            throws IOException {
        delegatee.renameFile(from, to);
    }

    /**
     * @inheritDoc
     */
    public void touchFile(String name)
            throws IOException {
        delegatee.touchFile(name);
    }
}
