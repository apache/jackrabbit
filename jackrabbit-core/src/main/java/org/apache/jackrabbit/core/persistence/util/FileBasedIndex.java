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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.util.StringIndex;

/**
 * Implements a {@link StringIndex} that is based on a hashmap and persists
 * the names as property file.
 */
public class FileBasedIndex extends HashMapIndex {

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
    public FileBasedIndex(FileSystemResource file)
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
     */
    protected void load() {
        try {
            long modTime = file.lastModified();
            if (modTime != lastModified) {
                lastModified = modTime;

                InputStream in = file.getInputStream();
                try {
                    Properties properties = new Properties();
                    properties.load(in);
                    for (Object name
                            : Collections.list(properties.propertyNames())) {
                        String string = name.toString();
                        Integer index =
                            Integer.valueOf(properties.getProperty(string));
                        stringToIndex.put(string, index);
                        indexToString.put(index, string);
                    }
                } finally {
                    in.close();
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load lookup table", e);
        }
    }

    /**
     * Saves the lookup table to the filesystem resource.
     */
    protected void save() {
        try {
            OutputStream out = file.getOutputStream();
            try {
                Properties properties = new Properties();
                for (Map.Entry<String, Integer> entry
                        : stringToIndex.entrySet()) {
                    properties.setProperty(
                            entry.getKey(), entry.getValue().toString());
                }
                properties.store(out, "string index");
            } finally {
                out.close();
            }
            lastModified = file.lastModified();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to store lookup table", e);
        }
    }

}
