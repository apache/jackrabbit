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
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Simple file-based data store. Data records are stored as normal files
 * named using a message digest of the contained binary stream.
 * 
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="className" value="org.apache.jackrabbit.core.data.FileDataStore"/>
 * <li>&lt;param name="{@link #setPath(String) path}" value="/data/datastore"/>
 * <li>&lt;param name="{@link #setMinRecordLength(int) minRecordLength}" value="1024"/>
 * </ul>
 * 
 * <p>
 * If the directory is not set, the directory &lt;repository home&gt;/repository/datastore is used.
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
     * The default value for the minimum object size.
     */
    private static final int DEFAULT_MIN_RECORD_LENGTH = 100;

    /**
     * Name of the directory used for temporary files.
     * Must be at least 3 characters.
     */
    private static final String TMP = "tmp";

    /**
     * The minimum modified date. If a file is accessed (read or write) with a modified date 
     * older than this value, the modified date is updated to the current time.
     */
    private long minModifiedDate;

    /**
     * The directory that contains all the data record files. The structure
     * of content within this directory is controlled by this class.
     */
    private File directory;
    
    /**
     * The name of the directory that contains all the data record files. The structure
     * of content within this directory is controlled by this class.
     */
    private String path;
    
    /**
     * The minimum size of an object that should be stored in this data store.
     */
    private int minRecordLength = DEFAULT_MIN_RECORD_LENGTH;
    
    /**
     * All data identifiers that are currently in use are in this set until they are garbage collected.
     */
    private WeakHashMap inUse = new WeakHashMap();

    /**
     * Creates a uninitialized data store.
     *
     */
    public FileDataStore() {
    }
    
    /**
     * Initialized the data store.
     * If the path is not set, &lt;repository home&gt;/repository/datastore is used.
     * This directory is automatically created if it does not yet exist.
     * 
     * @param config the repository configuration
     */
    public void init(String homeDir) {
        if (path == null) {
            path = homeDir + "/repository/datastore";
        }
        directory = new File(path);
        directory.mkdirs();
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
        if (minModifiedDate != 0 && file.exists() && file.canWrite()) {
            if (file.lastModified() < minModifiedDate) {
                file.setLastModified(System.currentTimeMillis());
            }
        }
        usesIdentifier(identifier);
        return new FileDataRecord(identifier, file);
    }
    
    private void usesIdentifier(DataIdentifier identifier) {
        inUse.put(identifier, new WeakReference(identifier));
    }

    /**
     * Creates a new data record. 
     * The stream is first consumed and the contents are saved in a temporary file
     * and the SHA-1 message digest of the stream is calculated. If a
     * record with the same SHA-1 digest (and length) is found then it is
     * returned. Otherwise the temporary file is moved in place to become
     * the new data record that gets returned.
     *
     * @param input binary stream
     * @return data record that contains the given stream
     * @throws DataStoreException if the record could not be created
     */
    public DataRecord addRecord(InputStream input) throws DataStoreException {
        File temporary = null;
        try {
            temporary = newTemporaryFile();
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
            usesIdentifier(identifier);
            File file = getFile(identifier);
            File parent = file.getParentFile();
            if (!parent.isDirectory()) {
                parent.mkdirs();
            }
            if (!file.exists()) {
                temporary.renameTo(file);
                if (!file.exists()) {
                    throw new IOException("Can not rename " + temporary.getAbsolutePath() + " to " + file.getAbsolutePath() + " (media read only?)");
                }
            } else {
                long now = System.currentTimeMillis();
                if (file.lastModified() < now) {
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
            throw new DataStoreException(DIGEST + " not available", e);
        } catch (IOException e) {
            throw new DataStoreException("Could not add record", e);
        } finally {
            if (temporary != null) {
                temporary.delete();
            }
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
        usesIdentifier(identifier);
        String string = identifier.toString();
        File file = directory;
        file = new File(file, string.substring(0, 2));
        file = new File(file, string.substring(2, 4));
        file = new File(file, string.substring(4, 6));
        return new File(file, string);
    }

    /**
     * Returns a unique temporary file to be used for creating a new
     * data record. 
     *
     * @return temporary file
     * @throws IOException 
     */
    private File newTemporaryFile() throws IOException {
        if (!directory.isDirectory()) {
            directory.mkdirs();
        }
        return File.createTempFile(TMP, null, directory);
    }

    /**
     * {@inheritDoc}
     */
    public void updateModifiedDateOnAccess(long before) {
        minModifiedDate = before;
    }
    
    /**
     * {@inheritDoc}
     */    
    public int deleteAllOlderThan(long min) {
        return deleteOlderRecursive(directory, min);
    }

    private int deleteOlderRecursive(File file, long min) {
        int count = 0;
        if (file.isFile() && file.exists() && file.canWrite()) {
            if (file.lastModified() < min) {
                DataIdentifier id = new DataIdentifier(file.getName());
                if (!inUse.containsKey(id)) {
                    file.delete();
                    count++;
                }
            }
        } else if (file.isDirectory()) {
            File[] list = file.listFiles();
            for (int i = 0; i < list.length; i++) {
                count += deleteOlderRecursive(list[i], min);
            }
        }
        return count;
    }
    
    private void listRecursive(List list, File file) {
        File[] l = file.listFiles();
        for (int i = 0; l != null && i < l.length; i++) {
            File f = l[i];
            if (f.isDirectory()) {
                listRecursive(list, f);
            } else {
                list.add(f);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Iterator getAllIdentifiers() {
        ArrayList files = new ArrayList();
        listRecursive(files, directory);
        ArrayList identifiers = new ArrayList();
        for (int i = 0; i < files.size(); i++) {
            File f = (File) files.get(i);
            String name = f.getName();
            if (!name.startsWith(TMP)) {
                DataIdentifier id = new DataIdentifier(name);
                identifiers.add(id);
            }
        }
        return identifiers.iterator();
    }
    
    /**
     * {@inheritDoc}
     */
    public void clearInUse() {
        inUse.clear();
    }

    /**
     * Get the name of the directory where this data store keeps the files.
     * 
     * @return the full path name
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the name of the directory where this data store keeps the files.
     * 
     * @param path the full path name
     */
    public void setPath(String directoryName) {
        this.path = directoryName;
    }

    /**
     * {@inheritDoc}
     */
    public int getMinRecordLength() {
        return minRecordLength;
    }

    /**
     * Set the minimum object length.
     * 
     * @param minRecordLength the length
     */
    public void setMinRecordLength(int minRecordLength) {
        this.minRecordLength = minRecordLength;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
    }

}
