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
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A caching data store that consists of {@link LocalCache} and {@link Backend}.
 * {@link Backend} is single source of truth. All methods first try to fetch
 * information from {@link LocalCache}. If record is not available in
 * {@link LocalCache}, then it is fetched from {@link Backend} and saved to
 * {@link LocalCache} for further access. This class is designed to work without
 * {@link LocalCache} and then all information is fetched from {@link Backend}.
 * To disable {@link LocalCache} set {@link #setCacheSize(long)} to 0. *
 * Configuration:
 * 
 * <pre>
 * &lt;DataStore class="org.apache.jackrabbit.aws.ext.ds.CachingDataStore">
 * 
 *     &lt;param name="{@link #setPath(String) path}" value="/data/datastore"/>
 *     &lt;param name="{@link #setConfig(String) config}" value="${rep.home}/backend.properties"/>
 *     &lt;param name="{@link #setCacheSize(long) cacheSize}" value="68719476736"/>
 *     &lt;param name="{@link #setSecret(String) secret}" value="123456"/>
 *     &lt;param name="{@link #setCachePurgeTrigFactor(double)}" value="0.95d"/>
 *     &lt;param name="{@link #setCachePurgeResizeFactor(double) cacheSize}" value="0.85d"/>
 *     &lt;param name="{@link #setMinRecordLength(int) minRecordLength}" value="1024"/>
 * &lt/DataStore>
 */
public abstract class CachingDataStore extends AbstractDataStore implements
        MultiDataStoreAware {

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
     * Name of the directory used for temporary files. Must be at least 3
     * characters.
     */
    private static final String TMP = "tmp";

    /**
     * All data identifiers that are currently in use are in this set until they
     * are garbage collected.
     */
    protected Map<DataIdentifier, WeakReference<DataIdentifier>> inUse =
            Collections.synchronizedMap(new WeakHashMap<DataIdentifier,
                    WeakReference<DataIdentifier>>());

    protected Backend backend;

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
     * The minimum modified date. If a file is accessed (read or write) with a
     * modified date older than this value, the modified date is updated to the
     * current time.
     */
    private long minModifiedDate;

    /**
     * Cache purge trigger factor. Cache will undergo in auto-purge mode if
     * cache current size is greater than cachePurgeTrigFactor * cacheSize
     */
    private double cachePurgeTrigFactor = 0.95d;

    /**
     * Cache resize factor. After auto-purge mode, cache current size would just
     * greater than cachePurgeResizeFactor * cacheSize cacheSize
     */
    private double cachePurgeResizeFactor = 0.85d;

    /**
     * The number of bytes in the cache. The default value is 64 GB.
     */
    private long cacheSize = 64L * 1024 * 1024 * 1024;

    /**
     * The local file system cache.
     */
    private LocalCache cache;

    protected abstract Backend createBackend();

    protected abstract String getMarkerFile();

    /**
     * Initialized the data store. If the path is not set, &lt;repository
     * home&gt;/repository/datastore is used. This directory is automatically
     * created if it does not yet exist. During first initialization, it upload
     * all files from local datastore to backed and local datastore act as a
     * local cache.
     */
    @Override
    public void init(String homeDir) throws RepositoryException {
        if (path == null) {
            path = homeDir + "/repository/datastore";
        }
        directory = new File(path);
        try {
            mkdirs(directory);
        } catch (IOException e) {
            throw new DataStoreException("Could not create directory "
                    + directory.getAbsolutePath(), e);
        }
        tmpDir = new File(homeDir, "/repository/s3tmp");
        try {
            if (!mkdirs(tmpDir)) {
                FileUtils.cleanDirectory(tmpDir);
                LOG.info("tmp = " + tmpDir.getPath() + " cleaned");
            }
        } catch (IOException e) {
            throw new DataStoreException("Could not create directory "
                    + tmpDir.getAbsolutePath(), e);
        }
        LOG.info("cachePurgeTrigFactor = " + cachePurgeTrigFactor
                + ", cachePurgeResizeFactor = " + cachePurgeResizeFactor);
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
                    throw new DataStoreException(
                            "Could not create marker file "
                                    + markerFile.getAbsolutePath(), e);
                }
            } else {
                LOG.info("marker file = " + markerFile.getAbsolutePath()
                        + " exists");
            }
        }
        cache = new LocalCache(path, tmpDir.getAbsolutePath(), cacheSize,
                cachePurgeTrigFactor, cachePurgeResizeFactor);
    }

    /**
     * Creates a new data record in {@link Backend}. The stream is first
     * consumed and the contents are saved in a temporary file and the SHA-1
     * message digest of the stream is calculated. If a record with the same
     * SHA-1 digest (and length) is found then it is returned. Otherwise new
     * record is created in {@link Backend} and the temporary file is moved in
     * place to {@link LocalCache}.
     * 
     * @param input
     *            binary stream
     * @return {@link CachingDataRecord}
     * @throws DataStoreException
     *             if the record could not be created.
     */
    @Override
    public DataRecord addRecord(InputStream input) throws DataStoreException {
        File temporary = null;
        try {
            temporary = newTemporaryFile();
            DataIdentifier tempId = new DataIdentifier(temporary.getName());
            usesIdentifier(tempId);
            // Copy the stream to the temporary file and calculate the
            // stream length and the message digest of the stream
            MessageDigest digest = MessageDigest.getInstance(DIGEST);
            OutputStream output = new DigestOutputStream(new FileOutputStream(
                    temporary), digest);
            try {
                IOUtils.copyLarge(input, output);
            } finally {
                output.close();
            }
            DataIdentifier identifier = new DataIdentifier(
                    encodeHexString(digest.digest()));
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
     * Get a data record for the given identifier or null it data record doesn't
     * exist in {@link Backend}
     * 
     * @param identifier
     *            identifier of record.
     * @return the {@link CachingDataRecord} or null.
     */
    @Override
    public DataRecord getRecordIfStored(DataIdentifier identifier)
            throws DataStoreException {
        synchronized (this) {
            usesIdentifier(identifier);
            if (!backend.exists(identifier)) {
                return null;
            }
            backend.touch(identifier, minModifiedDate);
            return new CachingDataRecord(this, identifier);
        }
    }

    @Override
    public void updateModifiedDateOnAccess(long before) {
        LOG.info("minModifiedDate set to: " + before);
        minModifiedDate = before;
    }

    /**
     * Retrieves all identifiers from {@link Backend}.
     */
    @Override
    public Iterator<DataIdentifier> getAllIdentifiers()
            throws DataStoreException {
        return backend.getAllIdentifiers();
    }

    /**
     * This method deletes record from {@link Backend} and then from
     * {@link LocalCache}
     */
    @Override
    public void deleteRecord(DataIdentifier identifier)
            throws DataStoreException {
        String fileName = getFileName(identifier);
        synchronized (this) {
            backend.deleteRecord(identifier);
            cache.delete(fileName);
        }
    }

    @Override
    public synchronized int deleteAllOlderThan(long min)
            throws DataStoreException {
        List<DataIdentifier> diList = backend.deleteAllOlderThan(min);
        // remove entries from local cache
        for (DataIdentifier identifier : diList) {
            cache.delete(getFileName(identifier));
        }
        return diList.size();
    }

    /**
     * Get stream of record from {@link LocalCache}. If record is not available
     * in {@link LocalCache}, this method fetches record from {@link Backend}
     * and stores it to {@link LocalCache}. Stream is then returned from cached
     * record.
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
     * Return lastModified of record from {@link Backend} assuming
     * {@link Backend} as a single source of truth.
     */
    public long getLastModified(DataIdentifier identifier) throws DataStoreException {
        LOG.info("accessed lastModified");
        return backend.getLastModified(identifier);
    }

    /**
     * Return the length of record from {@link LocalCache} if available,
     * otherwise retrieve it from {@link Backend}.
     */
    public long getLength(DataIdentifier identifier) throws DataStoreException {
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
     * Returns a unique temporary file to be used for creating a new data
     * record.
     */
    private File newTemporaryFile() throws IOException {
        return File.createTempFile(TMP, null, tmpDir);
    }

    /**
     * Load files from {@link LocalCache} to {@link Backend}.
     */
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
            if (!name.startsWith(TMP) && !name.endsWith(DS_STORE)
                    && f.length() > 0) {
                loadFileToBackEnd(f);
            }
        }
        LOG.info("Uploaded {" + currentSize + "}/{" + totalSize + "}");
    }

    /**
     * Traverse recursively and populate list with files.
     */
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

    /**
     * Upload file from {@link LocalCache} to {@link Backend}.
     * 
     * @param f
     *            file to uploaded.
     * @throws DataStoreException
     */
    private void loadFileToBackEnd(File f) throws DataStoreException {
        DataIdentifier identifier = new DataIdentifier(f.getName());
        usesIdentifier(identifier);
        backend.write(identifier, f);
        LOG.debug(f.getName() + "uploaded.");

    }

    /**
     * Derive file name from identifier.
     */
    private static String getFileName(DataIdentifier identifier) {
        String name = identifier.toString();
        name = name.substring(0, 2) + "/" + name.substring(2, 4) + "/"
                + name.substring(4, 6) + "/" + name;
        return name;
    }

    private void usesIdentifier(DataIdentifier identifier) {
        inUse.put(identifier, new WeakReference<DataIdentifier>(identifier));
    }

    private static boolean mkdirs(File dir) throws IOException {
        if (dir.exists()) {
            if (dir.isFile()) {
                throw new IOException("Can not create a directory "
                        + "because a file exists with the same name: "
                        + dir.getAbsolutePath());
            }
            return false;
        }
        boolean created = dir.mkdirs();
        if (!created) {
            throw new IOException("Could not create directory: "
                    + dir.getAbsolutePath());
        }
        return created;
    }

    @Override
    public void clearInUse() {
        inUse.clear();
    }

    public boolean isInUse(DataIdentifier identifier) {
        return inUse.containsKey(identifier);
    }

    @Override
    public void close() throws DataStoreException {
        cache.close();
        backend.close();
        cache = null;
    }

    /**
     * Setter for configuration based secret
     * 
     * @param secret
     *            the secret used to sign reference binaries
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Set the minimum object length.
     * 
     * @param minRecordLength
     *            the length
     */
    public void setMinRecordLength(int minRecordLength) {
        this.minRecordLength = minRecordLength;
    }

    /**
     * Return mininum object length.
     */
    @Override
    public int getMinRecordLength() {
        return minRecordLength;
    }

    /**
     * Return path of configuration properties.
     * 
     * @return path of configuration properties.
     */
    public String getConfig() {
        return config;
    }

    /**
     * Set the configuration properties path.
     * 
     * @param config
     *            path of configuration properties.
     */
    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * @return size of {@link LocalCache}.
     */
    public long getCacheSize() {
        return cacheSize;
    }

    /**
     * Set size of {@link LocalCache}.
     * 
     * @param cacheSize
     *            size of {@link LocalCache}.
     */
    public void setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
    }

    /**
     * 
     * @return path of {@link LocalCache}.
     */
    public String getPath() {
        return path;
    }

    /**
     * Set path of {@link LocalCache}.
     * 
     * @param path
     *            of {@link LocalCache}.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return Purge trigger factor of {@link LocalCache}.
     */
    public double getCachePurgeTrigFactor() {
        return cachePurgeTrigFactor;
    }

    /**
     * Set purge trigger factor of {@link LocalCache}.
     * 
     * @param cachePurgeTrigFactor
     *            purge trigger factor.
     */
    public void setCachePurgeTrigFactor(double cachePurgeTrigFactor) {
        this.cachePurgeTrigFactor = cachePurgeTrigFactor;
    }

    /**
     * @return Purge resize factor of {@link LocalCache}.
     */
    public double getCachePurgeResizeFactor() {
        return cachePurgeResizeFactor;
    }

    /**
     * Set purge resize factor of {@link LocalCache}.
     * 
     * @param cachePurgeResizeFactor
     *            purge resize factor.
     */
    public void setCachePurgeResizeFactor(double cachePurgeResizeFactor) {
        this.cachePurgeResizeFactor = cachePurgeResizeFactor;
    }

}
