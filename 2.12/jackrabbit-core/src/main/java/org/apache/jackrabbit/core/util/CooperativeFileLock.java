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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.UUID;

import javax.jcr.RepositoryException;

/**
 * The file lock is used to ensure a resource is only open once at any time.
 * It uses a cooperative locking protocol.
 */
public class CooperativeFileLock implements RepositoryLockMechanism {

    /**
     * Logger instance.
     */
    private static final Logger LOG =
        LoggerFactory.getLogger(CooperativeFileLock.class);

    private static final String MAGIC = "CooperativeFileLock";
    private static final String FILE_NAME = "lock.properties";
    private static final int MAX_FILE_RETRY = 16;
    private static final int SLEEP_GAP = 25;
    private static final int TIME_GRANULARITY = 2000;
    private static final int LOCK_SLEEP = 1000;

    private String fileName;
    private long lastWrite;
    private Properties properties;
    private boolean locked;
    private volatile boolean stop;

    private Thread watchdog;

    /**
     * Create a new file locking object using the given file name.
     *
     * @param path basic path to append {@link #FILE_NAME} to.
     */
    public void init(String path) {
        this.fileName = path + File.separatorChar + FILE_NAME;
    }

    /**
     * Lock the directory if possible.
     * This method will also start a background watchdog thread.
     * A file may only be locked once.
     *
     * @throws RepositoryException if locking was not successful
     */
    public synchronized void acquire() throws RepositoryException {
        if (locked) {
            throw new RepositoryException("Already locked " + fileName);
        }
        stop = false;
        lockFile();
        locked = true;
    }

    /**
     * Unlock the directory.
     * The watchdog thread is stopped.
     * This method does nothing if the file is already unlocked.
     */
    public synchronized void release() {
        if (!locked) {
            return;
        }
        try {
            stop = true;
            if (fileName != null) {
                if (load().equals(properties)) {
                    delete(fileName);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error unlocking " + fileName, e);
        } finally {
            locked = false;
        }
        try {
            if (watchdog != null) {
                watchdog.interrupt();
            }
        } catch (Exception e) {
            LOG.debug("Error stopping watchdog " + fileName, e);
        }
    }

    /**
     * Save the properties file.
     */
    private void save() throws RepositoryException {
        try {
            OutputStream out = new FileOutputStream(fileName);
            try {
                properties.store(out, MAGIC);
            } finally {
                out.close();
            }
            lastWrite = new File(fileName).lastModified();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Save " + properties);
            }
        } catch (IOException e) {
            throw getException(e);
        }
    }

    /**
     * Load the properties file.
     *
     * @return the properties object
     */
    private Properties load() throws RepositoryException {
        try {
            Properties p2 = new Properties();
            InputStream in = new FileInputStream(fileName);
            try {
                p2.load(in);
            } finally {
                in.close();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Load " + p2);
            }
            return p2;
        } catch (IOException e) {
            throw getException(e);
        }
    }

    /**
     * Wait until the file is old (not modified for a certain time).
     */
    private void waitUntilOld() throws RepositoryException {
        for (int i = 0; i < TIME_GRANULARITY / SLEEP_GAP; i++) {
            File f = new File(fileName);
            long last = f.lastModified();
            long dist = System.currentTimeMillis() - last;
            if (dist < -TIME_GRANULARITY) {
                // lock file modified in the future -
                // wait for a bit longer than usual
                try {
                    Thread.sleep(2 * LOCK_SLEEP);
                } catch (Exception e) {
                    LOG.debug("Sleep", e);
                }
                return;
            } else if (dist > TIME_GRANULARITY) {
                return;
            }
            try {
                Thread.sleep(SLEEP_GAP);
            } catch (Exception e) {
                    LOG.debug("Sleep", e);
            }
        }
        throw error("Lock file recently modified");
    }

    /**
     * Lock the file.
     */
    private void lockFile() throws RepositoryException {
        properties = new Properties();
        UUID uuid = UUID.randomUUID();
        properties.setProperty("id", uuid.toString());
        if (!createNewFile(fileName)) {
            waitUntilOld();
            save();
            // wait twice the watchdog sleep time
            for (int i = 0; i < 8; i++) {
                sleep(LOCK_SLEEP / 4);
                if (!load().equals(properties)) {
                    throw error("Locked by another process");
                }
            }
            delete(fileName);
            if (!createNewFile(fileName)) {
                throw error("Another process was faster");
            }
        }
        save();
        sleep(SLEEP_GAP);
        if (!load().equals(properties)) {
            stop = true;
            throw error("Concurrent update");
        }
        watchdog = new Thread(new Runnable() {
            public void run() {
                try {
                    while (!stop) {
                        // debug("Watchdog check");
                        try {
                            File f = new File(fileName);
                            if (!f.exists() || f.lastModified() != lastWrite) {
                                save();
                            }
                            Thread.sleep(LOCK_SLEEP);
                        } catch (OutOfMemoryError e) {
                            // ignore
                        } catch (InterruptedException e) {
                            // ignore
                        } catch (NullPointerException e) {
                            // ignore
                        } catch (Exception e) {
                            LOG.debug("Watchdog", e);
                        }
                    }
                } catch (Exception e) {
                    LOG.debug("Watchdog", e);
                }
                LOG.debug("Watchdog end");
            }
        });
        watchdog.setName(MAGIC + " Watchdog " + fileName);
        watchdog.setDaemon(true);
        watchdog.setPriority(Thread.MAX_PRIORITY - 1);
        watchdog.start();
    }

    private RepositoryException getException(Throwable t) {
        return new RepositoryException("Internal error in file lock " + fileName, t);
    }

    private RepositoryException error(String reason) {
        return new RepositoryException("Error locking " + fileName + ", reason: " + reason);
    }

    private void sleep(int time) throws RepositoryException {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw getException(e);
        }
    }

    /**
     * Create a new file, and retry if this doesn't work.
     * If it still doesn't work after some time, this method returns false.
     *
     * @param fileName the name of the file to create
     * @return if the file was created
     */
    private static boolean createNewFile(String fileName) {
        File file = new File(fileName);
        for (int i = 0; i < MAX_FILE_RETRY; i++) {
            try {
                return file.createNewFile();
            } catch (IOException e) {
                // 'access denied' is really a concurrent access problem
                wait(i);
            }
        }
        return false;
    }

    /**
     * Delete a file, and retry if this doesn't work.
     * If it still doesn't work after some time, an exception is thrown.
     *
     * @param fileName the name of the file to delete
     * @throws RepositoryException if the file could not be deleted
     */
    private static void delete(String fileName) throws RepositoryException {
        File file = new File(fileName);
        if (file.exists()) {
            for (int i = 0; i < MAX_FILE_RETRY; i++) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Deleting " + fileName);
                }
                boolean ok = file.delete();
                if (ok) {
                    return;
                }
                wait(i);
            }
            throw new RepositoryException("Could not delete file " + fileName);
        }
    }

    private static void wait(int i) {
        if (i > 8) {
            System.gc();
        }
        try {
            // sleep at most 256 ms
            long sleep = Math.min(256, i * i);
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            // ignore
        }
    }

}
