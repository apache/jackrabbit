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

import org.apache.jackrabbit.core.query.lucene.IOCounters;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * <code>FSDirectoryManager</code> implements a directory manager for
 * {@link FSDirectory} instances.
 */
public class FSDirectoryManager implements DirectoryManager {

    /**
     * The base directory.
     */
    private File baseDir;

    private boolean useSimpleFSDirectory;

    /**
     * {@inheritDoc}
     */
    public void init(SearchIndex handler) throws IOException {
        baseDir = new File(handler.getPath());
        useSimpleFSDirectory = handler.isUseSimpleFSDirectory();
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
        return new FSDir(dir, useSimpleFSDirectory);
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

        public FSDir(File dir, boolean simpleFS) throws IOException {
            if (!dir.mkdirs()) {
                if (!dir.isDirectory()) {
                    throw new IOException("Unable to create directory: '" + dir + "'");
                }
            }
            LockFactory lockFactory = new NativeFSLockFactory(dir);
            if (simpleFS) {
                directory = new SimpleFSDirectory(dir, lockFactory);
            } else {
                directory = FSDirectory.open(dir, lockFactory);
            }
        }

        @Override
        public String[] listAll() throws IOException {
            File[] files = directory.getDirectory().listFiles(FILTER);
            if (files == null) {
                return null;
            }
            String[] names = new String[files.length];
            for (int i = 0; i < names.length; i++) {
                names[i] = files[i].getName();
            }
            return names;
        }

        @Override
        public boolean fileExists(String name) throws IOException {
            return directory.fileExists(name);
        }

        @Override
        public long fileModified(String name) throws IOException {
            return directory.fileModified(name);
        }

        @Override
        public void touchFile(String name) throws IOException {
            directory.touchFile(name);
        }

        @Override
        public void deleteFile(String name) throws IOException {
            directory.deleteFile(name);
        }

        @Override
        public long fileLength(String name) throws IOException {
            return directory.fileLength(name);
        }

        @Override
        public IndexOutput createOutput(String name) throws IOException {
            return directory.createOutput(name);
        }

        @Override
        public IndexInput openInput(String name) throws IOException {
            IndexInput in = directory.openInput(name);
            return new IndexInputLogWrapper(name, in);
        }

        @Override
        public void close() throws IOException {
            directory.close();
        }

        @Override
        public IndexInput openInput(String name, int bufferSize)
                throws IOException {
            IndexInput in = directory.openInput(name, bufferSize);
            return new IndexInputLogWrapper(name, in);
        }

        @Override
        public Lock makeLock(String name) {
            return directory.makeLock(name);
        }

        @Override
        public void clearLock(String name) throws IOException {
            directory.clearLock(name);
        }

        @Override
        public void setLockFactory(LockFactory lockFactory) throws IOException {
            directory.setLockFactory(lockFactory);
        }

        @Override
        public LockFactory getLockFactory() {
            return directory.getLockFactory();
        }

        @Override
        public String getLockID() {
            return directory.getLockID();
        }

        public String toString() {
            return getClass().getName() + '@' + directory;
        }
    }

    /**
     * Implements an index input wrapper that logs the number of time bytes
     * are read from storage.
     */
    private static final class IndexInputLogWrapper extends IndexInput {

        private IndexInput in;

        IndexInputLogWrapper(String name, IndexInput in) {
            super(name);
            this.in = in;
        }

        @Override
        public byte readByte() throws IOException {
            return in.readByte();
        }

        @Override
        public void readBytes(byte[] b, int offset, int len) throws IOException {
            IOCounters.incrRead();
            in.readBytes(b, offset, len);
        }

        @Override
        public void close() throws IOException {
            in.close();
        }

        @Override
        public long getFilePointer() {
            return in.getFilePointer();
        }

        @Override
        public void seek(long pos) throws IOException {
            in.seek(pos);
        }

        @Override
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
