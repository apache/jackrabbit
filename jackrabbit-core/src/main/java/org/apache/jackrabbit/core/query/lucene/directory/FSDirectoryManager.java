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

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;

import java.io.IOException;
import java.io.File;
import java.io.FileFilter;

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
        return FSDirectory.getDirectory(dir, new NativeFSLockFactory(dir));
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
            for (int i = 0; i < files.length; i++) {
                if (!files[i].delete()) {
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
}
