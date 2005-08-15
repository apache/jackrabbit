/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
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
     * Returns the name of the file where infos are stored.
     * 
     * @return the name of the file where infos are stored.
     */
    String getFileName() {
        return name;
    }

    /**
     * Reads the index infos.
     * @param fs the base file system
     * @throws FileSystemException if an error occurs.
     * @throws IOException if an error occurs.
     */
    void read(FileSystem fs) throws FileSystemException, IOException {
        DataInputStream input = new DataInputStream(fs.getInputStream(name));
        try {
            counter = input.readInt();
            for (int i = input.readInt(); i > 0; i--) {
                indexes.add(input.readUTF());
            }
        } finally {
            input.close();
        }
    }

    /**
     * Writes the index infos to disk if they are dirty.
     * @param fs the base file system
     * @throws FileSystemException if an error occurs.
     * @throws IOException if an error occurs.
     */
    void write(FileSystem fs) throws FileSystemException, IOException {
        // do not write if not dirty
        if (!dirty) {
            return;
        }

        DataOutputStream output = new DataOutputStream(fs.getOutputStream(name + ".new"));
        try {
            output.writeInt(counter);
            output.writeInt(indexes.size());
            for (int i = 0; i < indexes.size(); i++) {
                output.writeUTF(getName(i));
            }
        } finally {
            output.close();
        }
        fs.move(name + ".new", name);
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
        indexes.add(name);
        dirty = true;
    }

    /**
     * Removes the name from the index infos.
     * @param name the name to remove.
     */
    void removeName(String name) {
        indexes.remove(name);
        dirty = true;
    }

    /**
     * Removes the name from the index infos.
     * @param i the position.
     */
    void removeName(int i) {
        indexes.remove(i);
        dirty = true;
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
