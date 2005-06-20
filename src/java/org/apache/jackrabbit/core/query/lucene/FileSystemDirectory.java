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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.log4j.Logger;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.InputStream;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.OutputStream;

import java.io.IOException;
import java.util.Map;

/**
 * Implements a lucene Directory based on a
 * {@link org.apache.jackrabbit.core.fs.FileSystem}.
 */
class FileSystemDirectory extends Directory {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = Logger.getLogger(FileSystemDirectory.class);

    /**
     * Map where the cached CQFSDirectories are stored.<br/>
     * Key: base path<br/>
     * Value: FileSystemDirectory
     */
    private static final Map directories =
            new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);

    /**
     * The underlying {@link org.apache.jackrabbit.core.fs.FileSystem} that
     * we use for storing index files.
     */
    private final FileSystem fs;

    /**
     * Returns a lucene <code>Directory</code> which is based on the
     * {@link org.apache.jackrabbit.core.fs.FileSystem} <code>fs</code>.
     * <p/>
     * <code>FileSystemDirectory</code> instances are cached. That is,
     * subsequent calls to <code>getDirectory()</code> with the same
     * <code>FileSystem</code> will return the previously returned
     * <code>FileSystemDirectory</code> instance.
     *
     * @param fs     the <code>FileSystem</code> where this <code>Directory</code>
     *               is based on.
     * @param create if <code>true</code>, an existing index in this
     *               <code>Directory</code> is deleted.
     * @return the <code>FileSystemDirectory</code> instance for the
     *         <code>FileSystem</code> <code>fs</code>.
     * @throws IOException if the <code>FileSystemDirectory</code> cannot
     *                     be created.
     */
    static FileSystemDirectory getDirectory(FileSystem fs,
                                            boolean create)
            throws IOException {

        synchronized (directories) {
            FileSystemDirectory dir = (FileSystemDirectory) directories.get(fs);
            if (dir == null) {
                dir = new FileSystemDirectory(fs, create);
                directories.put(fs, dir);
            }
            return dir;
        }
    }

    /**
     * Creates a new <code>FileSystemDirectory</code> based on
     * <code>FileSystem</code> <code>fs</code>.
     *
     * @param fs     the <code>FileSystem</code> where this <code>Directory</code>
     *               is based on.
     * @param create if <code>true</code>, an existing index in this
     *               <code>Directory</code> is deleted.
     * @throws IOException if the <code>FileSystemDirectory</code> cannot
     *                     be created.
     */
    private FileSystemDirectory(FileSystem fs,
                                boolean create) throws IOException {
        this.fs = fs;
        if (create) {
            try {
                // erase if existing
                String[] files = fs.listFiles("/");
                for (int i = 0; i < files.length; i++) {
                    if (log.isDebugEnabled()) {
                        log.debug("deleting " + files[i]);
                    }
                    fs.deleteFile(files[i]);
                }
            } catch (FileSystemException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] list() throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("list");
        }
        try {
            return fs.listFiles("/");
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean fileExists(String name) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("fileExists: " + name);
        }
        try {
            return fs.exists(name);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public long fileModified(String name) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("fileModified: " + name);
        }
        try {
            return fs.lastModified(name);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void touchFile(String name) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("touchFile: " + name);
        }
        try {
            fs.touch(name);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteFile(String name) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("deleteFile: " + name);
        }
        try {
            fs.deleteFile(name);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void renameFile(String from, String to) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("renameFile: from=" + from + " to=" + to);
        }
        try {
            fs.move(from, to);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public long fileLength(String name) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("fileLength: " + name);
        }
        try {
            return fs.length(name);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public OutputStream createFile(String name) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("createFile: " + name);
        }
        return new FileSystemOutputStream(new FileSystemResource(fs, name));
    }

    /**
     * {@inheritDoc}
     */
    public InputStream openFile(String name) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("openFile: " + name);
        }
        return new FileSystemInputStream(new FileSystemResource(fs, name));
    }

    /**
     * {@inheritDoc}
     */
    public Lock makeLock(String name) {
        final FileSystemResource lock = new FileSystemResource(fs, name);
        return new Lock() {
            public boolean obtain() throws IOException {
                // FIXME: this is not atomic on the filesystem
                // find better way.
                synchronized (FileSystemDirectory.this) {
                    try {
                        if (lock.exists()) {
                            return false;
                        } else {
                            // touch the file
                            lock.getOutputStream().close();
                            return true;
                        }
                    } catch (FileSystemException e) {
                        throw new IOException(e.getMessage());
                    }
                }
            }

            public void release() {
                try {
                    lock.delete();
                } catch (FileSystemException e) {
                    log.error("Unable to release lock file " + this + ": " + e);
                }
            }

            public boolean isLocked() {
                try {
                    boolean locked = lock.exists();
                    return locked;
                } catch (FileSystemException e) {
                    log.error("Unable to determine lock status for file "
                            + this + ": " + e);
                    // we're pessimistic
                    return true;
                }
            }

            public String toString() {
                return "Lock@" + lock.getName();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        // there is nothing to close here.
    }
}
