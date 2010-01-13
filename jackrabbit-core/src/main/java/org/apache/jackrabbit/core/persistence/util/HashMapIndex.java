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
package org.apache.jackrabbit.core.persistence.util;

import java.util.HashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;

import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.util.StringIndex;

/**
 * Implements a {@link StringIndex} that is based on a hashmap and persists
 * the names as property file.
 * <p/>
 * Please note that this class is not synchronized and the calls need to ensure
 * thread safeness.
 */
public class HashMapIndex implements StringIndex {

    /**
     * holds the string-to-index lookups.
     */
    private final HashMap<String, Integer> stringToIndex = new HashMap<String, Integer>();

    /**
     * holds the index-to-string lookups.
     */
    private final HashMap<Integer, String> indexToString = new HashMap<Integer, String>();

    /**
     * a copy of the {@link #stringToIndex} as properties class for faster
     * storing.
     */
    private final Properties stringToIndexProps = new Properties();

    /**
     * the filesystem resource that stores the lookup tables.
     */
    private FileSystemResource file;

    /**
     * the time when the resource was last modified.
     */
    private long lastModified = -1;

    /**
     * Creates a new hashmap index and loads the lookup tables from the
     * filesystem resource. If it does not exist yet, it will create a new one.
     *
     * @param file the filesystem resource that stores the lookup tables.
     *
     * @throws IOException if an I/O error occurs.
     * @throws FileSystemException if an I/O error occurs.
     */
    public HashMapIndex(FileSystemResource file)
            throws FileSystemException, IOException {
        this.file = file;
        if (!file.exists()) {
            file.makeParentDirs();
            file.getOutputStream().close();
        }
        load();
    }

    /**
     * Loads the lookup table from the filesystem resource.
     *
     * @throws IOException if an I/O error occurs.
     * @throws FileSystemException if an I/O error occurs.
     */
    private void load() throws IOException, FileSystemException {
        long modTime = file.lastModified();
        if (modTime > lastModified) {
            InputStream in = file.getInputStream();
            stringToIndexProps.clear();
            stringToIndexProps.load(in);
            Iterator<Object> iter = stringToIndexProps.keySet().iterator();
            while (iter.hasNext()) {
                String uri = (String) iter.next();
                String prop = stringToIndexProps.getProperty(uri);
                Integer idx = Integer.valueOf(prop);
                stringToIndex.put(uri, idx);
                indexToString.put(idx, uri);
            }
            in.close();
        }
        lastModified = modTime;
    }

    /**
     * Saves the lookup table to the filesystem resource.
     *
     * @throws IOException if an I/O error occurs.
     * @throws FileSystemException if an I/O error occurs.
     */
    private void save() throws IOException, FileSystemException {
        OutputStream out = file.getOutputStream();
        stringToIndexProps.store(out, "string index");
        out.close();
        lastModified = file.lastModified();
    }

    /**
     * {@inheritDoc}
     *
     * This implementation reloads the table from the resource if a lookup fails
     * and if the resource was modified since.
     */
    public int stringToIndex(String nsUri) {
        Integer idx = stringToIndex.get(nsUri);
        if (idx == null) {
            try {
                load();
            } catch (Exception e) {
                IllegalStateException ise = new IllegalStateException("Unable to load lookup table for uri: " + nsUri);
                ise.initCause(e);
                throw ise;
            }
            idx = stringToIndex.get(nsUri);
        }
        if (idx == null) {
            idx = Integer.valueOf(indexToString.size());
            stringToIndex.put(nsUri, idx);
            indexToString.put(idx, nsUri);
            stringToIndexProps.put(nsUri, idx.toString());
            try {
                save();
            } catch (Exception e) {
                IllegalStateException ise = new IllegalStateException("Unable to store lookup table for uri: "  + nsUri);
                ise.initCause(e);
                throw ise;
            }
        }
        return idx.intValue();
    }

    /**
     * {@inheritDoc}
     *
     * This implementation reloads the table from the resource if a lookup fails
     * and if the resource was modified since.
     */
    public String indexToString(int i) {
        Integer idx = Integer.valueOf(i);
        String s = indexToString.get(idx);
        if (s == null) {
            try {
                load();
            } catch (Exception e) {
                IllegalStateException ise = new IllegalStateException("Unable to load lookup table for index: " + i);
                ise.initCause(e);
                throw ise;
            }
            s = indexToString.get(idx);
        }
        return s;
    }
}
