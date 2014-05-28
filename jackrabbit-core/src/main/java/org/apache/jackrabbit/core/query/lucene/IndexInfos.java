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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.jackrabbit.core.query.lucene.directory.IndexInputStream;
import org.apache.jackrabbit.core.query.lucene.directory.IndexOutputStream;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores a sequence of index names and their current generation.
 */
class IndexInfos implements Cloneable {

    /**
     * Logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(IndexInfos.class);

    /**
     * IndexInfos version for Jackrabbit 1.0 to 1.5.x
     */
    private static final int NAMES_ONLY = 0;

    /**
     * IndexInfos version for Jackrabbit 2.0
     */
    private static final int WITH_GENERATION = 1;

    /**
     * For new segment names.
     */
    private int counter = 0;

    /**
     * Map of {@link IndexInfo}s. Key=name
     */
    private LinkedHashMap<String, IndexInfo> indexes = new LinkedHashMap<String, IndexInfo>();

    /**
     * The directory where the index infos are stored.
     */
    private final Directory directory;

    /**
     * Base name of the file where the infos are stored.
     */
    private final String name;

    /**
     * The generation for this index infos.
     */
    private long generation = 0;

    /**
     * When this index infos were last modified.
     */
    private long lastModified;

    /**
     * Creates a new IndexInfos using <code>baseName</code> and reads the
     * current generation.
     *
     * @param dir the directory where the index infos are stored.
     * @param baseName the name of the file where infos are stored.
     * @throws IOException if an error occurs while reading the index infos
     * file.
     */
    IndexInfos(Directory dir, String baseName) throws IOException {
        this.directory = dir;
        this.name = baseName;
        long gens[] = getGenerations(getFileNames(dir, baseName), baseName);
        if (gens.length == 0) {
            // write initial infos
            write();
        } else {
            // read most recent generation
            for (int i = gens.length - 1; i >= 0; i--) {
                try {
                    this.generation = gens[i];
                    read();
                    break;
                } catch (EOFException e) {
                    String fileName = getFileName(gens[i]);
                    log.warn("deleting invalid index infos file: " + fileName);
                    dir.deleteFile(fileName);
                    // reset generation
                    this.generation = 0;
                }
            }
        }
    }

    /**
     * Creates a new IndexInfos using <code>fileName</code> and reads the given
     * <code>generation</code> of the index infos.
     *
     * @param dir the directory where the index infos are stored.
     * @param baseName the name of the file where infos are stored.
     * @param generation the generation to read.
     * @throws IOException if an error occurs while reading the index infos
     * file.
     */
    IndexInfos(Directory dir, String baseName, long generation) throws IOException {
        if (generation < 0) {
            throw new IllegalArgumentException();
        }
        this.directory = dir;
        this.name = baseName;
        this.generation = generation;
        read();
    }

    /**
     * Returns the name of the file with the most current version where infos
     * are stored.
     *
     * @return the name of the file where infos are stored.
     */
    String getFileName() {
        return getFileName(generation);
    }

    /**
     * Writes the index infos to disk.
     *
     * @throws IOException if an error occurs.
     */
    void write() throws IOException {
        // increment generation
        generation++;
        String newName = getFileName();
        boolean success = false;
        try {
            OutputStream out = new BufferedOutputStream(new IndexOutputStream(
                    directory.createOutput(newName)));
            try {
                log.debug("Writing IndexInfos {}", newName);
                DataOutputStream dataOut = new DataOutputStream(out);
                dataOut.writeInt(WITH_GENERATION);
                dataOut.writeInt(counter);
                dataOut.writeInt(indexes.size());
                for (Iterator<IndexInfo> it = iterator(); it.hasNext(); ) {
                    IndexInfo info = it.next();
                    dataOut.writeUTF(info.getName());
                    dataOut.writeLong(info.getGeneration());
                    log.debug("  + {}:{}", info.getName(), info.getGeneration());
                }
            } finally {
                out.close();
            }
            directory.sync(Collections.singleton(newName));
            lastModified = System.currentTimeMillis();
            success = true;
        } finally {
            if (!success) {
                // try to delete the file and decrement generation
                try {
                    directory.deleteFile(newName);
                } catch (IOException e) {
                    log.warn("Unable to delete file: " + directory + "/" + newName);
                }
                generation--;
            }
        }
    }

    /**
     * @return an iterator over the {@link IndexInfo}s contained in this index
     *          infos.
     */
    Iterator<IndexInfo> iterator() {
        return indexes.values().iterator();
    }


    /**
     * Returns the number of index names.
     * @return the number of index names.
     */
    int size() {
        return indexes.size();
    }

    /**
     * @return the time when this index infos where last modified.
     */
    long getLastModified() {
        return lastModified;
    }

    /**
     * Adds a name to the index infos.
     *
     * @param name the name to add.
     * @param generation the current generation of the index.
     */
    void addName(String name, long generation) {
        if (indexes.containsKey(name)) {
            throw new IllegalArgumentException("already contains: " + name);
        }
        indexes.put(name, new IndexInfo(name, generation));
    }

    void updateGeneration(String name, long generation) {
        IndexInfo info = indexes.get(name);
        if (info == null) {
            throw new NoSuchElementException(name);
        }
        if (info.getGeneration() != generation) {
            info.setGeneration(generation);
        }
    }

    /**
     * Removes the name from the index infos.
     * @param name the name to remove.
     */
    void removeName(String name) {
        indexes.remove(name);
    }

    /**
     * Returns <code>true</code> if <code>name</code> exists in this
     * <code>IndexInfos</code>; <code>false</code> otherwise.
     *
     * @param name the name to test existence.
     * @return <code>true</code> it is exists in this <code>IndexInfos</code>.
     */
    boolean contains(String name) {
        return indexes.containsKey(name);
    }

    /**
     * @return the generation of this index infos.
     */
    long getGeneration() {
        return generation;
    }

    /**
     * Returns a new unique name for an index folder.
     * @return a new unique name for an index folder.
     */
    String newName() {
        return "_" + Integer.toString(counter++, Character.MAX_RADIX);
    }

    /**
     * Clones this index infos.
     *
     * @return a clone of this index infos.
     */
    @SuppressWarnings("unchecked")
    public IndexInfos clone() {
        try {
            IndexInfos clone = (IndexInfos) super.clone();
            clone.indexes = (LinkedHashMap<String, IndexInfo>) indexes.clone();
            for (Map.Entry<String, IndexInfo> entry : clone.indexes.entrySet()) {
                entry.setValue(entry.getValue().clone());
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            // never happens, this class is cloneable
            throw new RuntimeException();
        }
    }

    //----------------------------------< internal >----------------------------

    /**
     * Reads the index infos with the currently set {@link #generation}.
     *
     * @throws IOException if an error occurs.
     */
    private void read() throws IOException {
        String fileName = getFileName(generation);
        InputStream in = new BufferedInputStream(new IndexInputStream(
                directory.openInput(fileName)));
        try {
            LinkedHashMap<String, IndexInfo> indexes = new LinkedHashMap<String, IndexInfo>();
            DataInputStream di = new DataInputStream(in);
            int version;
            if (generation == 0) {
                version = NAMES_ONLY;
            } else {
                version = di.readInt();
            }
            int counter = di.readInt();
            for (int i = di.readInt(); i > 0; i--) {
                String indexName = di.readUTF();
                long gen = 0;
                if (version >= WITH_GENERATION) {
                    gen = di.readLong();
                }
                indexes.put(indexName, new IndexInfo(indexName, gen));
            }
            // when successfully read set values
            this.lastModified = directory.fileModified(fileName);
            this.indexes = indexes;
            this.counter = counter;
        } finally {
            in.close();
        }
    }

    /**
     * Returns the name of the file with the given generation where infos
     * are stored.
     *
     * @param gen the generation of the file.
     * @return the name of the file where infos are stored.
     */
    private String getFileName(long gen) {
        if (gen == 0) {
            return name;
        } else {
            return name + "_" + Long.toString(gen, Character.MAX_RADIX);
        }
    }

    /**
     * Returns all generations of this index infos.
     *
     * @param directory the directory where the index infos are stored.
     * @param base the base name for the index infos.
     * @return names of all generation files of this index infos.
     */
    private static String[] getFileNames(Directory directory, final String base) {
        String[] names = null;
        try {
            names = directory.listAll();
        } catch (IOException e) {
            // TODO: log warning? or throw?
        }
        if (names == null) {
            return new String[0];
        }
        List<String> nameList = new ArrayList<String>(names.length);
        for (String n : names) {
            if (n.startsWith(base)) {
                nameList.add(n);
            }
        }
        return nameList.toArray(new String[nameList.size()]);
    }

    /**
     * Parse the generation off the file name and return it.
     *
     * @param fileName the generation file that contains index infos.
     * @param base the base name.
     * @return the generation of the given file.
     */
    private static long generationFromFileName(String fileName, String base) {
        if (fileName.equals(base)) {
            return 0;
        } else {
            return Long.parseLong(fileName.substring(base.length() + 1),
                    Character.MAX_RADIX);
        }
    }

    /**
     * Returns the generations fo the given files in ascending order.
     *
     * @param fileNames the file names from where to obtain the generations.
     * @param base the base name.
     * @return the generations in ascending order.
     */
    private static long[] getGenerations(String[] fileNames, String base) {
        long[] gens = new long[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            gens[i] = generationFromFileName(fileNames[i], base);
        }
        Arrays.sort(gens);
        return gens;
    }
}
