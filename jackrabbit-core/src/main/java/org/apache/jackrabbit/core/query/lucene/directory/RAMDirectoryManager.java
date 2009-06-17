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
import org.apache.lucene.store.RAMDirectory;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

/**
 * <code>RAMDirectoryManager</code> implements a directory manager for
 * {@link RAMDirectory} instances.
 */
public class RAMDirectoryManager implements DirectoryManager {

    /**
     * Map of directories. Key=String(directory name), Value=Directory.
     */
    private final Map<String, Directory> directories = new HashMap<String, Directory>();

    /**
     * {@inheritDoc}
     */
    public void init(SearchIndex handler) throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasDirectory(String name) throws IOException {
        synchronized (directories) {
            return directories.containsKey(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Directory getDirectory(String name) {
        synchronized (directories) {
            Directory dir = directories.get(name);
            if (dir == null) {
                dir = new RAMDirectory();
                directories.put(name, dir);
            }
            return dir;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDirectoryNames() throws IOException {
        synchronized (directories) {
            return directories.keySet().toArray(
                    new String[directories.size()]);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean delete(String name) {
        synchronized (directories) {
            directories.remove(name);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean rename(String from, String to) {
        synchronized (directories) {
            if (directories.containsKey(to)) {
                return false;
            }
            Directory dir = directories.remove(from);
            if (dir == null) {
                return false;
            }
            directories.put(to, dir);
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
    }
}
