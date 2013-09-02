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

package org.apache.jackrabbit.aws.ext.ds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.aws.ext.LocalCache;
import org.apache.jackrabbit.core.data.AbstractDataStore;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.MultiDataStoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A caching data store. The implementing class defines the backend to use.
 */
public abstract class CachingDataStore extends AbstractDataStore implements MultiDataStoreAware {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CachingDataStore.class);

    /**
     * The digest algorithm used to uniquely identify records.
     */
    private static final String DIGEST = "SHA-1";

    private static final String DS_STORE = ".DS_Store";

    /**
     * The minimum size of an object that should be stored in this data store.
     */
    private int minRecordLength = 16 * 1024;

    private String path;

    private File directory;

    private File tmpDir;

    private String secret;

    /**
     * The optional backend configuration.
     */
    private String config;

    /**
     * Name of the directory used for temporary files. Must be at least 3 characters.
     */
    private static final String TMP = "tmp";

    /**
     * The minimum modified date. If a file is accessed (read or write) with a modified date older than this value, the modified date is
     * updated to the current time.
     */
    private long minModifiedDate = 0L;

    private double cachePurgeTrigFactor = 0.95d;

    private double cachePurgeResizeFactor = 0.85d;

    /**
     * All data identifiers that are currently in use are in this set until they are garbage collected.
     */
    protected Map<DataIdentifier, WeakReference<DataIdentifier>> inUse = Collections.synchronizedMap(new WeakHashMap<DataIdentifier, WeakReference<DataIdentifier>>());

    /**
     * The number of bytes in the cache. The default value is 64 GB.
     */
    private long cacheSize = 64L * 1024 * 1024 * 1024;

    protected Backend backend;

    /**
     * The cache.
     */
    private LocalCache cache;

    abstract Backend createBackend();

    abstract String getMarkerFile();

    /**
     * In first initialization it upload all files from local datastore to S3 and local datastore act as a local cache.
     *
     * @see DataStore#init(String)
     */
    public void init(String homeDir) throws RepositoryException {
        if (path == null) {
            path = homeDir + "/repository/datastore";
        }
        directory = new File(path);
        try {
            mkdirs(directory);
        } catch (IOException e) {
            throw new DataStoreException("Could not create directory " + directory.getAbsolutePath(), e);
        }
        tmpDir = new File(homeDir, "/repository/s3tmp");
        try {
            if (!mkdirs(tmpDir)) {
                FileUtils.cleanDirectory(tmpDir);
                LOG.info("tmp = " + tmpDir.getPath() + " cleaned");
            }
        } catch (IOException e) {
            throw new DataStoreException("Could not create directory " + tmpDir.getAbsolutePath(), e);
        }
        LOG.info("cachePurgeTrigFactor = " + cachePurgeTrigFactor + ", cachePurgeResizeFactor = " + cachePurgeResizeFactor);
        backend = createBackend();
        backend.init(this, path, config);
        String markerFileName = getMarkerFile();
        if (markerFileName != null) {
            // create marker file in homeDir to avoid deletion in cache cleanup.
            File markerFile = new File(homeDir, markerFileName);
            if (!markerFile.exists()) {
                LOG.info("load files from local cache");
                loadFilesFromCache();
                try {
                    markerFile.createNewFile();
                } catch (IOException e) {
                    throw new DataStoreException("Could not create marker file " + markerFile.getAbsolutePath(), e);
                }
            } else {
                LOG.info("marker file = " + markerFile.getAbsolutePath() + " exists");
            }
        }
        cache = new LocalCache(path, tmpDir.getAbsolutePath(), cacheSize, cachePurgeTrigFactor, cachePurgeResizeFactor);
    }

    /**
     * @see DataStore#addRecord(InputStream)
     */
    public DataRecord addRecord(InputStream input) throws DataStoreException {
        File temporary = null;
        try {
            temporary = newTemporaryFile();
            DataIdentifier tempId = new DataIdentifier(temporary.getName());
            usesIdentifier(tempId);
            // Copy the stream to the temporary file and calculate the
            // stream length and the message digest of the stream
            long length = 0;
            MessageDigest digest = MessageDigest.getInstance(DIGEST);
            OutputStream output = new DigestOutputStream(new FileOutputStream(temporary), digest);
            try {
                length = IOUtils.copyLarge(input, output);
            } finally {
                output.close();
            }
            DataIdentifier identifier = new DataIdentifier(encodeHexString(digest.digest()));
            synchronized (this) {
                usesIdentifier(identifier);
                backend.write(identifier, temporary);
                String fileName = getFileName(identifier);
                cache.store(fileName, temporary);
            }
            // this will also make sure that
            // tempId is not garbage collected until here
            inUse.remove(tempId);
            return new CachingDataRecord(this, identifier);
        } catch (NoSuchAlgorithmException e) {
            throw new DataStoreException(DIGEST + " not available", e);
        } catch (IOException e) {
            throw new DataStoreException("Could not add record", e);
        } finally {
            if (temporary != null) {
                // try to delete - but it's not a big deal if we can't
                temporary.delete();
            }
        }
    }

    /**
     * @see DataStore#getRecord(DataIdentifier)
     */
    public DataRecord getRecord(DataIdentifier identifier) throws DataStoreException {
        DataRecord record = getRecordIfStored(identifier);
        if (record == null) {
            throw new DataStoreException("Record not found: " + identifier);
        }
        return record;
    }

    /**
     * @see DataStore#getRecordIfStored(DataIdentifier)
     */
    public DataRecord getRecordIfStored(DataIdentifier identifier) throws DataStoreException {
        synchronized (this) {
            usesIdentifier(identifier);
            if (!backend.exists(identifier)) {
                return null;
            }
            backend.touch(identifier, minModifiedDate);
            return new CachingDataRecord(this, identifier);
        }
    }

    /**
     * @see DataStore#updateModifiedDateOnAccess(long)
     */
    public void updateModifiedDateOnAccess(long before) {
        LOG.info("minModifiedDate set to: " + before);
        minModifiedDate = before;
    }

    /**
     * @see DataStore#getAllIdentifiers()
     */
    public Iterator<DataIdentifier> getAllIdentifiers() throws DataStoreException {
        return backend.getAllIdentifiers();
    }

    /**
     * @see MultiDataStoreAware#deleteRecord(DataIdentifier)
     */
    public void deleteRecord(DataIdentifier identifier) throws DataStoreException {
        String fileName = getFileName(identifier);
        synchronized (this) {
            backend.deleteRecord(identifier);
            cache.delete(fileName);
        }
    }

    /**
     * @see DataStore#deleteAllOlderThan(long)
     */
    public synchronized int deleteAllOlderThan(long min) throws DataStoreException {
        List<DataIdentifier> diList = backend.deleteAllOlderThan(min);
        // remove entries from local cache
        for (DataIdentifier identifier : diList) {
            cache.delete(getFileName(identifier));
        }
        return diList.size();
    }

    /**
     * Return inputstream from cache if available or read from backend and store it in cache for further use.
     *
     * @param identifier
     * @return
     * @throws DataStoreException
     */
    InputStream getStream(DataIdentifier identifier) throws DataStoreException {
        InputStream in = null;
        try {
            String fileName = getFileName(identifier);
            InputStream cached = cache.getIfStored(fileName);
            if (cached != null) {
                return cached;
            }
            in = backend.read(identifier);
            return cache.store(fileName, in);
        } catch (IOException e) {
            throw new DataStoreException("IO Exception: " + identifier, e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Return lastModified of record from backend assuming it as a single source of truth.
     *
     * @param identifier
     * @return
     * @throws DataStoreException
     */
    long getLastModified(DataIdentifier identifier) throws DataStoreException {
        LOG.info("accessed lastModified");
        return backend.getLastModified(identifier);
    }

    /**
     * Return the length of record from cache if available otherwise from backend
     *
     * @param identifier
     * @return
     * @throws DataStoreException
     */
    long getLength(DataIdentifier identifier) throws DataStoreException {
        String fileName = getFileName(identifier);
        Long length = cache.getFileLength(fileName);
        if (length != null) {
            return length.longValue();
        }
        return backend.getLength(identifier);
    }

    @Override
    protected byte[] getOrCreateReferenceKey() throws DataStoreException {
        try {
            return secret.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Returns a unique temporary file to be used for creating a new data record.
     *
     * @return temporary file
     * @throws IOException
     */
    private File newTemporaryFile() throws IOException {
        return File.createTempFile(TMP, null, tmpDir);
    }

    private void loadFilesFromCache() throws RepositoryException {
        ArrayList<File> files = new ArrayList<File>();
        listRecursive(files, directory);
        long totalSize = 0;
        for (File f : files) {
            totalSize += f.length();
        }
        long currentSize = 0;
        long time = System.currentTimeMillis();
        for (File f : files) {
            long now = System.currentTimeMillis();
            if (now > time + 5000) {
                LOG.info("Uploaded {" + currentSize + "}/{" + totalSize + "}");
                time = now;
            }
            currentSize += f.length();
            String name = f.getName();
            LOG.debug("upload file = " + name);
            if (!name.startsWith(TMP) && !name.endsWith(DS_STORE) && f.length() > 0) {
                loadFileToBackEnd(f);
            }
        }
        LOG.info("Uploaded {" + currentSize + "}/{" + totalSize + "}");
    }

    private void listRecursive(List<File> list, File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    listRecursive(list, f);
                } else {
                    list.add(f);
                }
            }
        }
    }

    private void loadFileToBackEnd(File f) throws DataStoreException {
        DataIdentifier identifier = new DataIdentifier(f.getName());
        usesIdentifier(identifier);
        backend.write(identifier, f);
        LOG.debug(f.getName() + "uploaded.");

    }

    private String getFileName(DataIdentifier identifier) {
        String name = identifier.toString();
        name = name.substring(0, 2) + "/" + name.substring(2, 4) + "/" + name.substring(4, 6) + "/" + name;
        return name;
    }

    private void usesIdentifier(DataIdentifier identifier) {
        inUse.put(identifier, new WeakReference<DataIdentifier>(identifier));
    }

    private boolean mkdirs(File dir) throws IOException {
        if (dir.exists()) {
            if (dir.isFile()) {
                throw new IOException("Can not create a directory " + "because a file exists with the same name: " + dir.getAbsolutePath());
            }
            return false;
        } else {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IOException("Could not create directory: " + dir.getAbsolutePath());
            }
            return created;
        }
    }

    public void clearInUse() {
        inUse.clear();
    }

    public void close() throws DataStoreException {
        cache.close();
        backend.close();
        cache = null;
    }

    /**
     * Setter for configuration based secret
     *
     * @param secret the secret used to sign reference binaries
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setMinRecordLength(int minRecordLength) {
        this.minRecordLength = minRecordLength;
    }

    public int getMinRecordLength() {
        return minRecordLength;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public double getCachePurgeTrigFactor() {
        return cachePurgeTrigFactor;
    }

    public void setCachePurgeTrigFactor(double cachePurgeTrigFactor) {
        this.cachePurgeTrigFactor = cachePurgeTrigFactor;
    }

    public double getCachePurgeResizeFactor() {
        return cachePurgeResizeFactor;
    }

    public void setCachePurgeResizeFactor(double cachePurgeResizeFactor) {
        this.cachePurgeResizeFactor = cachePurgeResizeFactor;
    }

}
