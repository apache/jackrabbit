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
package org.apache.jackrabbit.core.query.lucene;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.apache.lucene.store.Directory;
import org.apache.jackrabbit.core.query.lucene.directory.IndexInputStream;
import org.apache.jackrabbit.core.query.lucene.directory.IndexOutputStream;

/**
 * Stores a sequence of index names.
 */
class IndexInfos {

    /**
     * For new segment names.
     */
    private int counter = 0;

    /**
     * Flag that indicates if index infos needs to be written to disk.
     */
    private boolean dirty = false;

    /**
     * List of index names
     */
    private List indexes = new ArrayList();

    /**
     * Set of names for quick lookup.
     */
    private Set names = new HashSet();

    /**
     * Name of the file where the infos are stored.
     */
    private final String name;

    /**
     * Creates a new IndexInfos using <code>fileName</code>.
     *
     * @param fileName the name of the file where infos are stored.
     */
    IndexInfos(String fileName) {
        this.name = fileName;
    }

    /**
     * Returns <code>true</code> if this index infos exists in
     * <code>dir</code>.
     *
     * @param dir the directory where to look for the index infos.
     * @return <code>true</code> if it exists; <code>false</code> otherwise.
     * @throws IOException if an error occurs while reading from the directory.
     */
    boolean exists(Directory dir) throws IOException {
        return dir.fileExists(name);
    }

    /**
     * Returns the name of the file where infos are stored.
     *
     * @return the name of the file where infos are stored.
     */
    String getFileName() {
        return name;
    }

    /**
     * Reads the index infos.
     *
     * @param dir the directory from where to read the index infos.
     * @throws IOException if an error occurs.
     */
    void read(Directory dir) throws IOException {
        InputStream in = new IndexInputStream(dir.openInput(name));
        try {
            DataInputStream di = new DataInputStream(in);
            counter = di.readInt();
            for (int i = di.readInt(); i > 0; i--) {
                String indexName = di.readUTF();
                indexes.add(indexName);
                names.add(indexName);
            }
        } finally {
            in.close();
        }
    }

    /**
     * Writes the index infos to disk if they are dirty.
     *
     * @param dir the directory where to write the index infos.
     * @throws IOException if an error occurs.
     */
    void write(Directory dir) throws IOException {
        // do not write if not dirty
        if (!dirty) {
            return;
        }

        OutputStream out = new IndexOutputStream(dir.createOutput(name + ".new"));
        try {
            DataOutputStream dataOut = new DataOutputStream(out);
            dataOut.writeInt(counter);
            dataOut.writeInt(indexes.size());
            for (int i = 0; i < indexes.size(); i++) {
                dataOut.writeUTF(getName(i));
            }
        } finally {
            out.close();
        }
        // delete old
        if (dir.fileExists(name)) {
            dir.deleteFile(name);
        }
        dir.renameFile(name + ".new", name);
        dirty = false;
    }

    /**
     * Returns the index name at position <code>i</code>.
     * @param i the position.
     * @return the index name.
     */
    String getName(int i) {
        return (String) indexes.get(i);
    }

    /**
     * Returns the number of index names.
     * @return the number of index names.
     */
    int size() {
        return indexes.size();
    }

    /**
     * Adds a name to the index infos.
     * @param name the name to add.
     */
    void addName(String name) {
        if (names.contains(name)) {
            throw new IllegalArgumentException("already contains: " + name);
        }
        indexes.add(name);
        names.add(name);
        dirty = true;
    }

    /**
     * Removes the name from the index infos.
     * @param name the name to remove.
     */
    void removeName(String name) {
        indexes.remove(name);
        names.remove(name);
        dirty = true;
    }

    /**
     * Removes the name from the index infos.
     * @param i the position.
     */
    void removeName(int i) {
        Object name = indexes.remove(i);
        names.remove(name);
        dirty = true;
    }

    /**
     * Returns <code>true</code> if <code>name</code> exists in this
     * <code>IndexInfos</code>; <code>false</code> otherwise.
     *
     * @param name the name to test existence.
     * @return <code>true</code> it is exists in this <code>IndexInfos</code>.
     */
    boolean contains(String name) {
        return names.contains(name);
    }

    /**
     * Returns a new unique name for an index folder.
     * @return a new unique name for an index folder.
     */
    String newName() {
        dirty = true;
        return "_" + Integer.toString(counter++, Character.MAX_RADIX);
    }
}
