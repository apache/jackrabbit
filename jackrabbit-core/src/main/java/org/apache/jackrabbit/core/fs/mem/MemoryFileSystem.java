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
package org.apache.jackrabbit.core.fs.mem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;

public class MemoryFileSystem implements FileSystem {

    private Map entries = new HashMap();

    public void close() {
    }

    public void copy(String srcPath, String destPath)
            throws FileSystemException {
        assertExistence(srcPath);
        MemoryFile srcFile = getFile(srcPath);
        OutputStream destinationOutputStream = getOutputStream(destPath);
        try {
            destinationOutputStream.write(srcFile.getData());
        } catch (IOException e) {
            throw new FileSystemException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(destinationOutputStream);
        }
    }

    private MemoryFile getFile(String filePath) throws FileSystemException {
        MemoryFileSystemEntry entry = getEntry(filePath);
        assertIsFile(filePath);
        return (MemoryFile) entry;
    }

    public void createFolder(String folderPath) throws FileSystemException {
        if (exists(folderPath)) {
            throw new FileSystemException("Folder or file " + folderPath
                    + " already exists");
        }
        if (!exists(FileSystem.SEPARATOR)) {
            createFolderInternal("/");
        }
        String relativePath = folderPath.substring(1);
        String[] pathElements = relativePath.split(FileSystem.SEPARATOR);
        String currentFolderPath = "";
        for (int i = 0; i < pathElements.length; i++) {
            String pathElement = pathElements[i];
            currentFolderPath += "/" + pathElement;
            createFolderInternal(currentFolderPath);
        }
    }

    private void createFolderInternal(String folderPath) {
        MemoryFolder folder = new MemoryFolder();
        entries.put(folderPath, folder);
    }

    public void deleteFile(String filePath) throws FileSystemException {
        assertExistence(filePath);
        entries.remove(filePath);
    }

    public void deleteFolder(String folderPath) throws FileSystemException {
        assertIsFolder(folderPath);
        Set allNames = entries.keySet();
        Set selectedNames = new HashSet();
        for (Iterator iter = allNames.iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            if (name.startsWith(folderPath)) {
                selectedNames.add(name);
            }
        }
        for (Iterator iter = selectedNames.iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            entries.remove(name);
        }
    }

    public boolean exists(String path) throws FileSystemException {
        return entries.containsKey(path);
    }

    public InputStream getInputStream(String filePath)
            throws FileSystemException {
        assertExistence(filePath);
        assertIsFile(filePath);

        MemoryFile file = getFile(filePath);
        return new ByteArrayInputStream(file.getData());
    }

    private void assertIsFolder(String folderPath) throws FileSystemException {
        assertExistence(folderPath);
        if (!getEntry(folderPath).isFolder()) {
            throw new FileSystemException("Folder " + folderPath
                    + " does not exist");
        }
    }

    private void assertIsFile(String filePath) throws FileSystemException {
        if (!isFile(filePath)) {
            throw new FileSystemException(filePath + " is a folder");
        }
    }

    public OutputStream getOutputStream(String filePath)
            throws FileSystemException {
        if (isFolder(filePath)) {
            throw new FileSystemException("path denotes folder: " + filePath);
        }

        String folderPath = filePath;
        if (filePath.lastIndexOf(FileSystem.SEPARATOR) > 0) {
            folderPath = filePath.substring(0, filePath.lastIndexOf("/"));
        } else {
            folderPath = "/";
        }
        assertIsFolder(folderPath);

        final MemoryFile file = new MemoryFile();
        entries.put(filePath, file);
        return new FilterOutputStream(new ByteArrayOutputStream()) {
            public void write(byte[] bytes, int off, int len) throws IOException {
                out.write(bytes, off, len);
            }

            public void close() throws IOException {
                out.close();
                file.setData(((ByteArrayOutputStream) out).toByteArray());
            }
        };
    }

    public RandomAccessOutputStream getRandomAccessOutputStream(String filePath)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "Random access is not implemented for the memory file system");
    }

    public boolean hasChildren(String path) throws FileSystemException {
        assertIsFolder(path);
        return list(path).length > 0;
    }

    public void init() {
        createFolderInternal("/");
    }

    public boolean isFile(String path) throws FileSystemException {
        return exists(path) && !getEntry(path).isFolder();
    }

    private MemoryFileSystemEntry getEntry(String path) {
        return ((MemoryFileSystemEntry) entries.get(path));
    }

    private void assertExistence(String path) throws FileSystemException {
        if (!exists(path)) {
            throw new FileSystemException("no such file " + path);
        }
    }

    public boolean isFolder(String path) throws FileSystemException {
        if (path.equals("/")) {
            return true;
        } else {
            return exists(path) && getEntry(path).isFolder();
        }
    }

    public long lastModified(String path) throws FileSystemException {
        assertExistence(path);
        return getEntry(path).getLastModified();
    }

    public long length(String filePath) throws FileSystemException {
        assertIsFile(filePath);
        return getFile(filePath).getData().length;
    }

    public String[] list(String folderPath) {
        if (folderPath.equals("/")) {
            folderPath = "";
        }
        Set allNames = entries.keySet();
        Set selectedNames = new HashSet();
        for (Iterator iter = allNames.iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            if (name.matches(folderPath + "/[^/]*") && !name.equals("/")) {
                selectedNames.add(name.substring(folderPath.length() + 1));
            }
        }
        return (String[]) selectedNames.toArray(new String[selectedNames.size()]);
    }

    public String[] listFiles(String folderPath) {
        return listInternal(folderPath, false);
    }

    public String[] listFolders(String folderPath) {
        return listInternal(folderPath, true);
    }

    private String[] listInternal(String folderPath, boolean isFolder) {
        String[] names = list(folderPath);
        if (folderPath.equals("/")) {
            folderPath = "";
        }
        Set result = new HashSet();
        for (int i = 0; i < names.length; i++) {
            if (getEntry(folderPath + "/" + names[i]).isFolder() == isFolder) {
                result.add(names[i]);
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    public void move(String srcPath, String destPath)
            throws FileSystemException {
        assertExistence(srcPath);
        if (exists(destPath)) {
            throw new FileSystemException("Destination exists: " + destPath);
        }

        // Create destination folder if it does not yet exist
        String[] path = destPath.split(SEPARATOR);
        String folder = "";
        for (int i = 1; i < path.length; i++) {
            folder += SEPARATOR + path[i];
            if (!exists(folder)) {
                createFolder(folder);
            }
        }
        
        Map moves = new HashMap();
        moves.put(srcPath, destPath);
        if (getEntry(srcPath).isFolder()) {
            srcPath = srcPath + "/";
            Iterator iterator = entries.keySet().iterator();
            while (iterator.hasNext()) {
                String name = (String) iterator.next();
                if (name.startsWith(srcPath)) {
                    moves.put(
                            name,
                            destPath + "/" + name.substring(srcPath.length()));
                }
            }
        }

        Iterator iterator = moves.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            entries.put(entry.getValue(), entries.remove(entry.getKey()));
        }
    }

    public void touch(String filePath) throws FileSystemException {
        assertIsFile(filePath);
        getEntry(filePath).touch();
    }

}
