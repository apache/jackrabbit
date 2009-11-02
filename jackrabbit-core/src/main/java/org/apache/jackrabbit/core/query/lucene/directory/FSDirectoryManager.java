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
package org.apache.jackrabbit.core.query.lucene.directory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.jackrabbit.core.query.lucene.IOCounters;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;

/**
 * <code>FSDirectoryManager</code> implements a directory manager for
 * {@link FSDirectory} instances.
 */
public class FSDirectoryManager implements DirectoryManager {

    /**
     * The base directory.
     */
    private File baseDir;

    /**
     * {@inheritDoc}
     */
    public void init(SearchIndex handler) throws IOException {
        baseDir = new File(handler.getPath());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasDirectory(String name) throws IOException {
        return new File(baseDir, name).exists();
    }

    /**
     * {@inheritDoc}
     */
    public Directory getDirectory(String name)
            throws IOException {
        File dir;
        if (name.equals(".")) {
            dir = baseDir;
        } else {
            dir = new File(baseDir, name);
        }
        return new FSDir(dir);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDirectoryNames() throws IOException {
        File[] dirs = baseDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        if (dirs != null) {
            String[] names = new String[dirs.length];
            for (int i = 0; i < dirs.length; i++) {
                names[i] = dirs[i].getName();
            }
            return names;
        } else {
            throw new IOException("listFiles for " + baseDir.getPath() + " returned null");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean delete(String name) {
        File directory = new File(baseDir, name);
        // trivial if it does not exist anymore
        if (!directory.exists()) {
            return true;
        }
        // delete files first
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    return false;
                }
            }
        } else {
            return false;
        }
        // now delete directory itself
        return directory.delete();
    }

    /**
     * {@inheritDoc}
     */
    public boolean rename(String from, String to) {
        File src = new File(baseDir, from);
        File dest = new File(baseDir, to);
        return src.renameTo(dest);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
    }

    //-----------------------< internal >---------------------------------------

    private static final class FSDir extends Directory {

        private static final FileFilter FILTER = new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        };

        private final FSDirectory directory;

        public FSDir(File dir) throws IOException {
            directory = FSDirectory.getDirectory(dir,
                    new NativeFSLockFactory(dir));
        }

        public String[] list() throws IOException {
            File[] files = directory.getFile().listFiles(FILTER);
            if (files == null) {
                return null;
            }
            String[] names = new String[files.length];
            for (int i = 0; i < names.length; i++) {
                names[i] = files[i].getName();
            }
            return names;
        }

        public boolean fileExists(String name) throws IOException {
            return directory.fileExists(name);
        }

        public long fileModified(String name) throws IOException {
            return directory.fileModified(name);
        }

        public void touchFile(String name) throws IOException {
            directory.touchFile(name);
        }

        public void deleteFile(String name) throws IOException {
            directory.deleteFile(name);
        }

        public void renameFile(String from, String to) throws IOException {
            directory.renameFile(from, to);
        }

        public long fileLength(String name) throws IOException {
            return directory.fileLength(name);
        }

        public IndexOutput createOutput(String name) throws IOException {
            return directory.createOutput(name);
        }

        public IndexInput openInput(String name) throws IOException {
            IndexInput in = directory.openInput(name);
            return new IndexInputLogWrapper(in);
        }

        public void close() throws IOException {
            directory.close();
        }

        public IndexInput openInput(String name, int bufferSize)
                throws IOException {
            IndexInput in = directory.openInput(name, bufferSize);
            return new IndexInputLogWrapper(in);
        }

        public Lock makeLock(String name) {
            return directory.makeLock(name);
        }

        public void clearLock(String name) throws IOException {
            directory.clearLock(name);
        }

        public void setLockFactory(LockFactory lockFactory) {
            directory.setLockFactory(lockFactory);
        }

        public LockFactory getLockFactory() {
            return directory.getLockFactory();
        }

        public String getLockID() {
            return directory.getLockID();
        }

        public String toString() {
            return this.getClass().getName() + "@" + directory;
        }
    }

    /**
     * Implements an index input wrapper that logs the number of time bytes
     * are read from storage.
     */
    private static final class IndexInputLogWrapper extends IndexInput {

        private IndexInput in;

        IndexInputLogWrapper(IndexInput in) {
            this.in = in;
        }

        public byte readByte() throws IOException {
            return in.readByte();
        }

        public void readBytes(byte[] b, int offset, int len) throws IOException {
            IOCounters.incrRead();
            in.readBytes(b, offset, len);
        }

        public void close() throws IOException {
            in.close();
        }

        public long getFilePointer() {
            return in.getFilePointer();
        }

        public void seek(long pos) throws IOException {
            in.seek(pos);
        }

        public long length() {
            return in.length();
        }

        @Override
        public Object clone() {
            IndexInputLogWrapper clone = (IndexInputLogWrapper) super.clone();
            clone.in = (IndexInput) in.clone();
            return clone;
        }
    }
}
