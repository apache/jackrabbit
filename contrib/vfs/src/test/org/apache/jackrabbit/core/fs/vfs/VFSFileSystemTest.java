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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;
import org.apache.jackrabbit.test.JUnitTest;

/**
 * <p>
 * FileSystem backed by Commons VFS Tests
 * </p>
 * 
 * <p>
 * In order to run the following VM arguments must be set:
 * <ul>
 * <li>fs.prefix = [provider prefix]</li>
 * <li>fs.path = [root path]</li>
 * <li>fs.config = [providers config file]</li>
 * </ul>
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class VFSFileSystemTest extends JUnitTest
{
    private static final String PREFIX = "fs.prefix";

    private static final String PATH = "fs.path";

    private static final String CONFIG = "fs.config";

    private static final String TEST_FOLDER = "testFolder1";

    private static final String TEST_FILE = "testFile1.txt";

    private static final String TEST_FOLDER2 = TEST_FOLDER + "/testFolder2";

    private static final String TEST_FILE2 = TEST_FOLDER2 + "/testFile2.txt";

    private static final String TEST_FILE3 = TEST_FOLDER2 + "/testFile3.txt";

    /**
     * VFS fs
     */
    private VFSFileSystem fs;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        fs = new VFSFileSystem();
        fs.setPrefix(System.getProperty(PREFIX));
        fs.setPath(System.getProperty(PATH));
        fs.setConfig(System.getProperty(CONFIG));
        fs.init();
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        fs.close();
        fs = null;
    }

    public void testFileSystem() throws Exception
    {
        /*
         * Create folder
         */
        // depth = 0
        fs.createFolder(TEST_FOLDER);
        assertTrue(fs.exists(TEST_FOLDER));
        assertTrue(fs.isFolder(TEST_FOLDER));
        // depth = 1
        fs.createFolder(TEST_FOLDER2);
        assertTrue(fs.exists(TEST_FOLDER2));
        assertTrue(fs.isFolder(TEST_FOLDER2));
        // Children
        assertTrue(fs.hasChildren(TEST_FOLDER));
        assertTrue(fs.list(TEST_FOLDER)[0].equals(getName(TEST_FOLDER2)));
        assertTrue(fs.listFiles(TEST_FOLDER).length == 0);
        assertTrue(fs.listFolders(TEST_FOLDER)[0].equals(getName(TEST_FOLDER2)));

        /*
         * Create file
         */
        // depth = 1
        byte[] write = "hello world".getBytes();
        byte[] read = new byte[write.length];

        OutputStream out = fs.getOutputStream(TEST_FILE);
        out.write(write);
        out.flush();
        out.close();
        assertTrue(fs.exists(TEST_FILE));
        fs.getInputStream(TEST_FILE).read(read);
        assertTrue(Arrays.equals(write, read));
        // depth = 2
        out = fs.getOutputStream(TEST_FILE2);
        out.write(write);
        out.flush();
        out.close();
        assertTrue(fs.exists(TEST_FILE2));
        InputStream in = fs.getInputStream(TEST_FILE2);
        in.read(read);
        in.close();
        assertTrue(Arrays.equals(write, read));

        /*
         * Delete file
         */
        fs.deleteFile(TEST_FILE2);
        assertFalse(fs.exists(TEST_FILE2));

        /*
         * Delete folder
         */
        fs.deleteFolder(TEST_FOLDER2);
        assertFalse(fs.exists(TEST_FOLDER2));

        /*
         * Copy file
         */
        fs.copy(TEST_FILE, TEST_FILE2);
        assertTrue(fs.exists(TEST_FILE2));
        assertTrue(fs.isFile(TEST_FILE2));

        /*
         * Move file
         */
        fs.move(TEST_FILE2, TEST_FILE3);
        assertFalse(fs.exists(TEST_FILE2));
        assertTrue(fs.exists(TEST_FILE3));
        assertTrue(fs.isFile(TEST_FILE3));

        /* Radom access content */
        RandomAccessOutputStream rout = fs
                .getRandomAccessOutputStream(TEST_FILE);
        rout.seek(100);
        rout.write(10);
        rout.flush();
        rout.close();

        in = fs.getInputStream(TEST_FILE);
        in.skip(100);
        assertTrue(in.read() == 10);
        in.close();

    }

    /**
     * Get the name
     * 
     * @param path
     * @return
     */
    private String getName(String path)
    {
        return path.substring(path.lastIndexOf("/") + 1, path.length());
    }

}