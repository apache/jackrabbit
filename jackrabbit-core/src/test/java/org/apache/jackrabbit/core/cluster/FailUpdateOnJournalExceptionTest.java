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
package org.apache.jackrabbit.core.cluster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.JUnitTest;

/**
 * <code>FailUpdateOnJournalExceptionTest</code> checks if
 * UpdateEventChannel.updateCreated(Update) throws a ClusterException
 * when locking the Journal fails. See JCR-3417
 */
public class FailUpdateOnJournalExceptionTest extends JUnitTest {

    private RepositoryImpl repo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAll();
        copyInputStreamToFile(
                getClass().getResourceAsStream("repository-with-test-journal.xml"),
                new File(getTestDir(), "repository.xml"));
        repo = RepositoryImpl.create(RepositoryConfig.create(getTestDir()));
    }

    @Override
    protected void tearDown() throws Exception {
        if (repo != null) {
            repo.shutdown();
        }
        deleteAll();
        super.tearDown();
    }

    public void testUpdate() throws Exception {
        Session s = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
        Node root = s.getRootNode();
        root.addNode("foo");
        s.save();
        root.addNode("bar");
        TestJournal.refuseLock = true;
        try {
            s.save();
            fail("Session.save() must fail with RepositoryException when Journal cannot be locked.");
        } catch (RepositoryException e) {
            // expected
        } finally {
            TestJournal.refuseLock = false;
        }
    }

    private static void deleteAll() throws IOException {
        FileUtils.deleteDirectory(getTestDir());
    }

    private static File getTestDir() throws IOException {
        return new File("target",
                FailUpdateOnJournalExceptionTest.class.getSimpleName());
    }
    
    public static void copyInputStreamToFile(InputStream source, File destination) throws IOException {
        try {
            FileOutputStream output = openOutputStream(destination, false);
            try {
                IOUtils.copy(source, output);
                output.close(); // don't swallow close Exception if copy completes normally
            } finally {
                IOUtils.closeQuietly(output);
            }
        } finally {
            IOUtils.closeQuietly(source);
        }
    }

    public static FileOutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }

}
