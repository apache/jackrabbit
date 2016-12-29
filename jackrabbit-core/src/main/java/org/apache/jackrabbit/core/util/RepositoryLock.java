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
package org.apache.jackrabbit.core.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exclusive lock on a repository home directory. This class encapsulates
 * collective experience on how to acquire an exclusive lock on a given
 * directory. The lock is expected to be exclusive both across process
 * boundaries and within a single JVM. The lock mechanism must also work
 * consistently on a variety of operating systems and JVM implementations.
 *
 * @see <a href="https://issues.apache.org/jira/browse/JCR-213">JCR-213</a>
 * @see <a href="https://issues.apache.org/jira/browse/JCR-233">JCR-233</a>
 * @see <a href="https://issues.apache.org/jira/browse/JCR-254">JCR-254</a>
 * @see <a href="https://issues.apache.org/jira/browse/JCR-912">JCR-912</a>
 * @see <a href="https://issues.apache.org/jira/browse/JCR-933">JCR-933</a>
 */
public class RepositoryLock implements RepositoryLockMechanism {

    /**
     * Name of the lock file within a directory.
     */
    private static final String LOCK = ".lock";

    /**
     * Logger instance.
     */
    private static final Logger LOG =
        LoggerFactory.getLogger(RepositoryLock.class);

    /**
     * The locked directory.
     */
    private File directory;

    /**
     * The lock file within the given directory.
     */
    private File file;

    /**
     * The random access file.
     */
    private RandomAccessFile randomAccessFile;

    /**
     * Unique identifier (canonical path name) of the locked directory.
     * Used to ensure exclusive locking within a single JVM.
     *
     * @see https://issues.apache.org/jira/browse/JCR-933
     */
    private String identifier;

    /**
     * The file lock. Used to ensure exclusive locking across process boundaries.
     *
     * @see https://issues.apache.org/jira/browse/JCR-233
     */
    private FileLock lock;

    public RepositoryLock() {
        // used by the factory
    }
    
    /**
     * Create a new RepositoryLock object and initialize it.
     * @deprecated 
     * This constructor is deprecated; use the default constructor
     * and {@link #init(String)} instead.
     * 
     * @param path directory path
     * @throws RepositoryException if the canonical path of the directory
     *                             can not be determined
     */
    public RepositoryLock(String path) throws RepositoryException {
        init(path);
    }
    
    /**
     * Initialize the instance for the given directory path. The lock still needs to be 
     * explicitly acquired using the {@link #acquire()} method.
     *
     * @param path directory path
     * @throws RepositoryException if the canonical path of the directory
     *                             can not be determined
     */
    public void init(String path) throws RepositoryException {
        try {
            directory = new File(path).getCanonicalFile();
            file = new File(directory, LOCK);
            identifier =
                (RepositoryLock.class.getName() + ":" + directory.getPath())
                .intern();
            lock = null;
        } catch (IOException e) {
            throw new RepositoryException(
                    "Unable to determine canonical path of " + path, e);
        }
    }

    /**
     * Lock the repository home.
     *
     * @throws RepositoryException if the repository lock can not be acquired
     */
    public void acquire() throws RepositoryException {
        if (file.exists()) {
            LOG.warn("Existing lock file " + file + " detected."
                    + " Repository was not shut down properly.");
        }
        try {
            tryLock();
        } catch (RepositoryException e) {
            closeRandomAccessFile();
            throw e;
        }
    }

    /**
     * Try to lock the random access file.
     *
     * @throws RepositoryException
     */
    private void tryLock() throws RepositoryException {
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            lock = randomAccessFile.getChannel().tryLock();
        } catch (IOException e) {
            throw new RepositoryException(
                    "Unable to create or lock file " + file, e);
        } catch (OverlappingFileLockException e) {
            // JCR-912: OverlappingFileLockException with JRE 1.6
            throw new RepositoryException(
                    "The repository home " + directory + " appears to be in use"
                    + " since the file named " + file.getName()
                    + " is already locked by the current process.");
        }

        if (lock == null) {
            throw new RepositoryException(
                    "The repository home " + directory + " appears to be in use"
                    + " since the file named " + file.getName()
                    + " is locked by another process.");
        }

        // JCR-933: due to a bug in java 1.4/1.5 on *nix platforms
        // it's possible that java.nio.channels.FileChannel.tryLock()
        // returns a non-null FileLock object although the lock is already
        // held by *this* jvm process
        synchronized (identifier) {
            if (null != System.getProperty(identifier)) {
                // note that the newly acquired (redundant) file lock
                // is deliberately *not* released because this could
                // potentially cause, depending on the implementation,
                // the previously acquired lock(s) to be released
                // as well
                throw new RepositoryException(
                        "The repository home " + directory + " appears to be"
                        + " already locked by the current process.");
            } else {
                try {
                    System.setProperty(identifier, identifier);
                } catch (SecurityException e) {
                    LOG.warn("Unable to set system property: " + identifier, e);
                }
            }
        }
    }

    /**
     * Close the random access file if it is open, and set it to null.
     */
    private void closeRandomAccessFile() {
        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
            } catch (IOException e) {
                LOG.warn("Unable to close the random access file " + file, e);
            }
            randomAccessFile = null;
        }
    }

    /**
     * Releases repository lock.
     */
    public void release() {
        if (lock != null) {
            try {
                FileChannel channel = lock.channel();
                lock.release();
                channel.close();
            } catch (IOException e) {
                // ignore
            }
            lock = null;
            closeRandomAccessFile();
        }

        if (!file.delete()) {
            LOG.warn("Unable to delete repository lock file");
        }

        // JCR-933: see #acquire()
        synchronized (identifier) {
            try {
                System.getProperties().remove(identifier);
            } catch (SecurityException e) {
                LOG.error("Unable to clear system property: " + identifier, e);
            }
        }
    }

}
