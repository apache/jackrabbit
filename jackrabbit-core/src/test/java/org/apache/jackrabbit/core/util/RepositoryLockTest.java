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

import javax.jcr.RepositoryException;

import junit.framework.TestCase;

/**
 * Unit tests for the {@link RepositoryLock} class.
 */
public class RepositoryLockTest extends TestCase {

    /**
     * The temporary directory used for testing.
     */
    private File directory;

    /**
     * Sets up the temporary directory used for testing.
     */
    protected void setUp() throws IOException {
        directory = File.createTempFile("RepositoryLock", "Test");
        directory.delete();
        directory.mkdir();
    }

    /**
     * Deletes the temporary directory used for testing.
     */
    protected void tearDown() {
        delete(directory);
    }

    /**
     * Recursively deletes the given file or directory.
     *
     * @param file file or directory to be deleted
     */
    private void delete(File file) {
        File[] files = file.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            delete(files[i]);
        }
        file.delete();
    }

    /**
     * Tests that when an acquired lock is released, the lock file is
     * automatically removed.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testNoFilesLeftBehind() throws RepositoryException {
        RepositoryLock lock = new RepositoryLock(directory.getPath());
        lock.acquire();
        lock.release();
        assertEquals(
                "Some files left behind by a lock",
                0, directory.listFiles().length);
    }

    /**
     * Tests that locking is exclusive within a single JVM.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testTwoLocks() throws RepositoryException {
        RepositoryLock lockA = new RepositoryLock(directory.getPath());
        RepositoryLock lockB = new RepositoryLock(directory.getPath());
        lockA.acquire();
        try {
            lockB.acquire();
            fail("Can acquire an already acquired lock");
        } catch (RepositoryException e) {
        }
        lockA.release();
        try {
            lockB.acquire();
        } catch (RepositoryException e) {
            fail("Can not acquire a released lock");
        }
        lockB.release();
    }

    /**
     * Tests that the canonical path is used for locking.
     *
     * @see https://issues.apache.org/jira/browse/JCR-933
     * @throws RepositoryException
     */
    public void testCanonicalPath() throws RepositoryException {
        RepositoryLock lockA = new RepositoryLock(directory.getPath());
        lockA.acquire();
        try {
            File parent = new File(directory, "..");
            RepositoryLock lockB = new RepositoryLock(
                    new File(parent, directory.getName()).getPath());
            lockB.acquire();
            fail("Can acquire an already acquired lock using a different path");
        } catch (RepositoryException e) {
        }
        lockA.release();
    }

}
