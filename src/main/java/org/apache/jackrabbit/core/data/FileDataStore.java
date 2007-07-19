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
package org.apache.jackrabbit.core.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Simple file-based data store. Data records are stored as normal files
 * named using a message digest of the contained binary stream.
 * <p>
 * A three level directory structure is used to avoid placing too many
 * files in a single directory. The chosen structure is designed to scale
 * up to billions of distinct records.
 * <p>
 * This implementation relies on the underlying file system to support
 * atomic O(1) move operations with {@link File#renameTo(File)}.
 */
public class FileDataStore implements DataStore {

    /**
     * The digest algorithm used to uniquely identify records.
     */
    private static final String DIGEST = "SHA-1";

    /**
     * Name of the directory used for temporary files.
     */
    private static final String TMP = "tmp";

    /**
     * Temporary file counter used to guarantee that concurrent threads
     * in this JVM do not accidentally use the same temporary file names.
     * <p>
     * This variable is static to allow multiple separate data store
     * instances in the same JVM to access the same data store directory
     * on disk. The counter is initialized to a random number based on the
     * time when this class was first loaded to minimize the chance of two
     * separate JVM processes (or class loaders within the same JVM) using
     * the same temporary file names. 
     */
    private static long counter = new Random().nextLong();
    
    private long minModifiedDate;

    /**
     * Returns the next value of the internal temporary file counter.
     *
     * @return next counter value
     */
    private static synchronized long nextCount() {
        return counter++;
    }

    /**
     * The directory that contains all the data record files. The structure
     * of content within this directory is controlled by this class.
     */
    private final File directory;

    /**
     * Creates a data store based on the given directory.
     *
     * @param directory data store directory
     */
    public FileDataStore(File directory) {
        this.directory = directory;
    }

    /**
     * Returns the record with the given identifier. Note that this method
     * performs no sanity checks on the given identifier. It is up to the
     * caller to ensure that only identifiers of previously created data
     * records are used.
     *
     * @param identifier data identifier
     * @return identified data record
     */
    public DataRecord getRecord(DataIdentifier identifier) {
        File file = getFile(identifier);
        if(minModifiedDate != 0 && file.exists() && file.canWrite()) {
            if(file.lastModified() < minModifiedDate) {
                file.setLastModified(System.currentTimeMillis());
            }
        }
        return new FileDataRecord(identifier, file);
    }

    /**
     * Creates a new record based on the given input stream. The stream
     * is first consumed and the contents are saved in a temporary file
     * and the SHA-1 message digest of the stream is calculated. If a
     * record with the same SHA-1 digest (and length) is found then it is
     * returned. Otherwise the temporary file is moved in place to become
     * the new data record that gets returned.
     *
     * @param input binary stream
     * @return data record that contains the given stream
     * @throws IOException if the record could not be created
     */
    public DataRecord addRecord(InputStream input) throws IOException {
        File temporary = newTemporaryFile();
        try {
            // Copy the stream to the temporary file and calculate the
            // stream length and the message digest of the stream
            long length = 0;
            MessageDigest digest = MessageDigest.getInstance(DIGEST);
            OutputStream output = new FileOutputStream(temporary);
            try {
                byte[] b = new byte[4096];
                for (int n = input.read(b); n != -1; n = input.read(b)) {
                    output.write(b, 0, n);
                    digest.update(b, 0, n);
                    length += n;
                }
            } finally {
                output.close();
            }

            // Check if the same record already exists, or
            // move the temporary file in place if needed
            DataIdentifier identifier = new DataIdentifier(digest.digest());
            File file = getFile(identifier);
            File parent = file.getParentFile();
            if (!parent.isDirectory()) {
                parent.mkdirs();
            }
            if (!file.exists()) {
                temporary.renameTo(file);
            } else {
                long now = System.currentTimeMillis();
                if(file.lastModified() < now) {
                    file.setLastModified(now);
                }
            }

            // Sanity checks on the record file. These should never fail,
            // but better safe than sorry...
            if (!file.isFile()) {
                throw new IOException("Not a file: " + file);
            }
            if (file.length() != length) {
                throw new IOException(DIGEST + " collision: " + file);
            }

            return new FileDataRecord(identifier, file);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(DIGEST + " not available: " + e.getMessage());
        } finally {
            temporary.delete();
        }
    }

    /**
     * Returns the identified file. This method implements the pattern
     * used to avoid problems with too many files in a single directory.
     * <p>
     * No sanity checks are performed on the given identifier.
     *
     * @param identifier data identifier
     * @return identified file
     */
    private File getFile(DataIdentifier identifier) {
        String string = identifier.toString();
        File file = directory;
        file = new File(file, string.substring(0, 2));
        file = new File(file, string.substring(2, 4));
        file = new File(file, string.substring(4, 6));
        return new File(file, string);
    }

    /**
     * Returns a unique temporary file to be used for creating a new
     * data record. A synchronized counter value and the current time are
     * used to construct the name of the temporary file in a way that
     * minimizes the chance of collisions across concurrent threads or
     * processes.
     *
     * @return temporary file
     */
    private File newTemporaryFile() {
        File temporary = new File(directory, TMP);

        if (!temporary.isDirectory()) {
            temporary.mkdirs();
        }
        String name = TMP + "-" + nextCount() + "-" + System.currentTimeMillis();
        return new File(temporary, name);
    }

}
