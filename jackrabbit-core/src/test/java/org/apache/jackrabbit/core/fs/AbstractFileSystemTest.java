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
package org.apache.jackrabbit.core.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;

public abstract class AbstractFileSystemTest extends TestCase {

    private FileSystem fs;
    private byte[] sampleBytes = new byte[]{(byte)0x12, (byte)0x0F, (byte)0xF0};

    protected abstract FileSystem getFileSystem();

    protected void setUp() throws Exception {
        fs = getFileSystem();
        fs.init();
    }

    protected void tearDown() throws Exception {
        fs.close();
    }

    public void testIsFolder() throws Exception {
        assertTrue(fs.isFolder("/"));
    }

    public void testCreateFile() throws Exception {
        fs.createFolder("/folder");
        createFile("/folder/file", sampleBytes);
        assertTrue(fs.isFile("/folder/file"));
    }

    public void testCreateFileInNonExistentFolder() throws IOException {
        try {
            createFile("/folder/file", sampleBytes);
            fail("FileSystemException expected");
        } catch (FileSystemException e) {
            // ok
        }
    }

    public void testGetInputStream() throws Exception {
        createFile("/test", sampleBytes);
        InputStream inputStream = fs.getInputStream("/test");
        verifyStreamInput(inputStream, sampleBytes);
    }

    private void verifyStreamInput(InputStream inputStream, byte[] expectedBytes) throws IOException {
        byte[] resultBytes = new byte[3];
        inputStream.read(resultBytes);
        inputStream.close();

        assertEquals(expectedBytes[0], resultBytes[0]);
        assertEquals(expectedBytes[1], resultBytes[1]);
        assertEquals(expectedBytes[2], resultBytes[2]);
    }

    public void testCopy() throws Exception {
        createFile("/test", sampleBytes);
        fs.copy("/test", "/test2");
        assertTrue(fs.exists("/test2"));
        verifyStreamInput(fs.getInputStream("/test2"), sampleBytes);
    }

    private void createFile(String fileName, byte[] bytes) throws IOException, FileSystemException {
        OutputStream outputStream = fs.getOutputStream(fileName);
        outputStream.write(bytes);
        outputStream.close();
    }

    public void testDeleteFile() throws Exception {
        createFile("/test", sampleBytes);
        assertTrue(fs.exists("/test"));
        fs.deleteFile("/test");
        assertFalse(fs.exists("/test"));
    }

    public void testLength() throws Exception {
        createFile("/test", sampleBytes);
        assertEquals(3, fs.length("/test"));
    }

    public void testMove() throws Exception {
        createFile("/test", sampleBytes);
        fs.move("/test", "/test2");
        assertFalse(fs.exists("/test"));
        assertTrue(fs.exists("/test2"));
        verifyStreamInput(fs.getInputStream("/test2"), sampleBytes);
    }

    public void testLastModified() throws Exception {
        createFile("/test", sampleBytes);
        long millis1 = fs.lastModified("/test");
        // ensure time gap
        Thread.sleep(100);
        createFile("/test", sampleBytes);
        long millis2 = fs.lastModified("/test");
        assertTrue(millis1 < millis2);
    }

    public void testTouch() throws Exception {
        createFile("/test", sampleBytes);
        long millis1 = fs.lastModified("/test");
        // ensure time gap
        Thread.sleep(100);
        fs.touch("/test");
        long millis2 = fs.lastModified("/test");
        assertTrue(millis1 < millis2);
    }

    public void testCreateAndDeleteFolder() throws Exception {
        fs.createFolder("/folder");
        assertTrue(fs.isFolder("/folder"));
        fs.deleteFolder("/folder");
        assertFalse(fs.exists("/folder"));
    }

    public void testDeleteNonEmptyFolder() throws Exception {
        fs.createFolder("/folder/subfolder");
        try {
            fs.deleteFolder("/folder");
            fail("FileSystemException expected");
        } catch (FileSystemException e) {
            // ok
        }
    }

    public void testCreateSubFolderWithInNonExistentFolder() throws Exception {
        fs.createFolder("/folder/subfolder");
        assertTrue(fs.isFolder("/folder"));
        assertTrue(fs.isFolder("/folder/subfolder"));
    }

    public void testList() throws Exception {
        fs.createFolder("/folder/subfolder");
        fs.getOutputStream("/folder/file").close();
        fs.getOutputStream("/file").close();

        String[] entries = fs.list("/");
        assertEquals(2, entries.length);
        Arrays.sort(entries);
        assertEquals(entries[0], "/file");
        assertEquals(entries[1], "/folder");

        entries = fs.list("/folder");
        assertEquals(2, entries.length);
        Arrays.sort(entries);
        assertEquals(entries[0], "/folder/file");
        assertEquals(entries[1], "/folder/subfolder");

        entries = fs.listFiles("/folder");
        assertEquals(1, entries.length);
        assertEquals(entries[0], "/folder/file");
    }

}
