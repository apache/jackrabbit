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

    public void testEverything() throws Exception {
        String[] list;

        // At beginning the file system should contain only the root folder
        assertTrue(fs.exists("/"));
        assertTrue(fs.isFolder("/"));
        assertFalse(fs.isFile("/"));
        assertEquals(0, fs.list("/").length);

        // Create a folder
        fs.createFolder("/folder");
        assertTrue(fs.exists("/folder"));
        assertTrue(fs.isFolder("/folder"));
        assertFalse(fs.isFile("/folder"));
        assertEquals(0, fs.list("/folder").length);
        list = fs.list("/");
        assertEquals(1, list.length);
        assertEquals("folder", list[0]);

        // Create a file inside the folder
        createFile("/folder/file", sampleBytes);
        assertTrue(fs.exists("/folder/file"));
        assertFalse(fs.isFolder("/folder/file"));
        assertTrue(fs.isFile("/folder/file"));
        list = fs.list("/folder");
        assertEquals(1, list.length);
        assertEquals("file", list[0]);
        assertEquals(3, fs.length("/folder/file"));
        verifyStreamInput(fs.getInputStream("/folder/file"), sampleBytes);

        // Copy the file
        fs.copy("/folder/file", "/folder/file2");
        assertTrue(fs.exists("/folder/file"));
        assertFalse(fs.isFolder("/folder/file"));
        assertTrue(fs.isFile("/folder/file"));
        assertTrue(fs.exists("/folder/file2"));
        assertFalse(fs.isFolder("/folder/file2"));
        assertTrue(fs.isFile("/folder/file2"));
        list = fs.list("/folder");
        Arrays.sort(list);
        assertEquals(2, list.length);
        assertEquals("file", list[0]);
        assertEquals("file2", list[1]);
        assertEquals(3, fs.length("/folder/file2"));
        verifyStreamInput(fs.getInputStream("/folder/file2"), sampleBytes);

        // Delete the copy
        fs.deleteFile("/folder/file2");
        assertFalse(fs.exists("/folder/file2"));
        assertFalse(fs.isFolder("/folder/file2"));
        assertFalse(fs.isFile("/folder/file2"));
        list = fs.list("/folder");
        assertEquals(1, list.length);
        assertEquals("file", list[0]);

        // Move the file
        fs.move("/folder/file", "/folder/file2");
        assertFalse(fs.exists("/folder/file"));
        assertFalse(fs.isFolder("/folder/file"));
        assertFalse(fs.isFile("/folder/file"));
        assertTrue(fs.exists("/folder/file2"));
        assertFalse(fs.isFolder("/folder/file2"));
        assertTrue(fs.isFile("/folder/file2"));
        list = fs.list("/folder");
        assertEquals(1, list.length);
        assertEquals("file2", list[0]);
        assertEquals(3, fs.length("/folder/file2"));
        verifyStreamInput(fs.getInputStream("/folder/file2"), sampleBytes);

        // Move the folder
        fs.move("/folder", "/folder2");
        assertFalse(fs.exists("/folder"));
        assertFalse(fs.isFolder("/folder"));
        assertFalse(fs.isFile("/folder"));
        assertTrue(fs.exists("/folder2"));
        assertTrue(fs.isFolder("/folder2"));
        assertFalse(fs.isFile("/folder2"));
        assertFalse(fs.exists("/folder/file2"));
        assertFalse(fs.isFolder("/folder/file2"));
        assertFalse(fs.isFile("/folder/file2"));
        assertTrue(fs.exists("/folder2/file2"));
        assertFalse(fs.isFolder("/folder2/file2"));
        assertTrue(fs.isFile("/folder2/file2"));
        list = fs.list("/");
        assertEquals(1, list.length);
        assertEquals("folder2", list[0]);
        list = fs.list("/folder2");
        assertEquals(1, list.length);
        assertEquals("file2", list[0]);
        assertEquals(3, fs.length("/folder2/file2"));
        verifyStreamInput(fs.getInputStream("/folder2/file2"), sampleBytes);

        // Create a subfolder
        fs.createFolder("/folder2/subfolder");
        assertTrue(fs.exists("/folder2/subfolder"));
        assertTrue(fs.isFolder("/folder2/subfolder"));
        assertFalse(fs.isFile("/folder2/subfolder"));
        assertEquals(0, fs.list("/folder2/subfolder").length);
        list = fs.list("/folder2");
        Arrays.sort(list);
        assertEquals(2, list.length);
        assertEquals("file2", list[0]);
        assertEquals("subfolder", list[1]);
        list = fs.listFiles("/folder2");
        assertEquals(1, list.length);
        assertEquals("file2", list[0]);
        list = fs.listFolders("/folder2");
        assertEquals(1, list.length);
        assertEquals("subfolder", list[0]);

        // Try to create a file coliding with an exisiting folder
        try {
            createFile("/folder2/subfolder", sampleBytes);
            fail("FileSystemException expected");
        } catch (FileSystemException e) {
            // ok
        }

        // Delete the subfolder
        fs.deleteFolder("/folder2/subfolder");
        assertFalse(fs.exists("/folder2/subfolder"));
        assertFalse(fs.isFolder("/folder2/subfolder"));
        assertFalse(fs.isFile("/folder2/subfolder"));
        list = fs.list("/folder2");
        assertEquals(1, list.length);
        assertEquals("file2", list[0]);
        list = fs.listFiles("/folder2");
        assertEquals(1, list.length);
        assertEquals("file2", list[0]);
        assertEquals(0, fs.listFolders("/folder2").length);

        // Delete the folder
        fs.deleteFolder("/folder2");
        assertFalse(fs.exists("/folder2"));
        assertFalse(fs.isFolder("/folder2"));
        assertFalse(fs.isFile("/folder2"));
        assertFalse(fs.exists("/folder2/file2"));
        assertFalse(fs.isFolder("/folder2/file2"));
        assertFalse(fs.isFile("/folder2/file2"));
        assertEquals(0, fs.list("/").length);

        // Test last modified time stamps
        createFile("/file1", sampleBytes);
        Thread.sleep(100);
        createFile("/file2", sampleBytes);
        assertTrue(fs.lastModified("/file1") <= fs.lastModified("/file2"));
        Thread.sleep(100);
        fs.touch("/file1");
        assertTrue(fs.lastModified("/file1") >= fs.lastModified("/file2"));

        // Try to create a file inside a nonexistent folder
        try {
            createFile("/missing/file", sampleBytes);
            fail("FileSystemException expected");
        } catch (FileSystemException e) {
            // ok
        }

        // Try to create a folder inside a nonexistent folder
        fs.createFolder("/missing/subfolder");
        assertTrue(fs.exists("/missing"));
        assertTrue(fs.isFolder("/missing"));
        assertFalse(fs.isFile("/missing"));
        assertTrue(fs.exists("/missing/subfolder"));
        assertTrue(fs.isFolder("/missing/subfolder"));
        assertFalse(fs.isFile("/missing/subfolder"));
        assertEquals(0, fs.list("/missing/subfolder").length);
        list = fs.list("/missing");
        assertEquals(1, list.length);
        assertEquals("subfolder", list[0]);
    }

    public void testMoveFile_destFolderDoesNotExist() throws Exception {
        // Create a folder with a file
        fs.createFolder("/folder");
        createFile("/folder/file", sampleBytes);

        // Check that the destination folder and file do not exist
        assertFalse(fs.exists("/folder2"));
        assertFalse(fs.exists("/folder2/file"));

        // Move the file into a non-existent directory
        fs.move("/folder/file", "/folder2/file");

        // Check that the file has been moved
        assertFalse(fs.exists("/folder/file"));
        assertFalse(fs.isFolder("/folder/file"));
        assertFalse(fs.isFile("/folder/file"));
        assertTrue(fs.exists("/folder2/file"));
        assertFalse(fs.isFolder("/folder2/file"));
        assertTrue(fs.isFile("/folder2/file"));

        // Check that folder2 has been silently created
        assertTrue(fs.exists("/folder2"));
        assertTrue(fs.isFolder("/folder2"));
        assertFalse(fs.isFile("/folder2"));
    }
    
    public void testMoveFolder_destFolderDoesNotExist() throws Exception {
        // Create a folder with a file
        fs.createFolder("/folder");
        createFile("/folder/file", sampleBytes);
        // Check that the destination folder and file do not exist
        assertFalse(fs.exists("/folder2"));
        assertFalse(fs.exists("/folder2/folder3"));
        assertFalse(fs.exists("/folder2/folder3/file"));

        // Move the folder into a non-existent directory
        fs.move("/folder", "/folder2/folder3");

        // Assert
        assertFalse(fs.exists("/folder"));
        assertFalse(fs.exists("/folder/file"));
        
        assertTrue(fs.exists("/folder2"));
        assertTrue(fs.isFolder("/folder2"));
        assertFalse(fs.isFile("/folder2"));

        assertTrue(fs.exists("/folder2/folder3"));
        assertTrue(fs.isFolder("/folder2/folder3"));
        assertFalse(fs.isFile("/folder2/folder3"));

        assertTrue(fs.exists("/folder2/folder3/file"));
        assertFalse(fs.isFolder("/folder2/folder3/file"));
        assertTrue(fs.isFile("/folder2/folder3/file"));
    }

    private void verifyStreamInput(
            InputStream inputStream, byte[] expectedBytes) throws IOException {
        byte[] resultBytes = new byte[3];
        assertEquals(3, inputStream.read(resultBytes));
        inputStream.close();

        assertEquals(expectedBytes[0], resultBytes[0]);
        assertEquals(expectedBytes[1], resultBytes[1]);
        assertEquals(expectedBytes[2], resultBytes[2]);
    }

    private void createFile(String fileName, byte[] bytes)
            throws IOException, FileSystemException {
        OutputStream outputStream = fs.getOutputStream(fileName);
        outputStream.write(bytes);
        outputStream.close();
    }

}
