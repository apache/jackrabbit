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

package org.apache.jackrabbit.aws.ext;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.data.LazyFileInputStream;
import org.apache.jackrabbit.util.TransientFileFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a LRU cache for input streams.
 */
public class LocalCache {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(LocalCache.class);

    /**
     * The directory where the files are created.
     */
    private final File directory;

    /**
     * The directory where tmp files are created.
     */
    private final File tmp;

    /**
     * The file names of the files that need to be deleted.
     */
    private final Set<String> toBeDeleted = new HashSet<String>();

    /**
     * The size in bytes.
     */
    private long size;

    /**
     * The filename Vs file size LRU cache.
     */
    private LRUCache cache;

    /**
     * If true cache is in purgeMode and not available. All operation would be no-op.
     */
    private volatile boolean purgeMode = false;

    /**
     * Build LRU cache of files located at 'path'. If cache size exceeds configurable size, all further entries are deleted.
     *
     * @param path
     * @param tmpPath
     * @param size
     * @param cachePurgeTrigFactor
     * @param cachePurgeResizeFactor
     * @throws RepositoryException
     */
    public LocalCache(String path, String tmpPath, long size, double cachePurgeTrigFactor, double cachePurgeResizeFactor)
            throws RepositoryException {
        this.size = size;
        directory = new File(path);
        tmp = new File(tmpPath);
        cache = new LRUCache(size, cachePurgeTrigFactor, cachePurgeResizeFactor);
        ArrayList<File> allFiles = new ArrayList<File>();

        @SuppressWarnings("unchecked")
        Iterator it = FileUtils.iterateFiles(directory, null, true);
        while (it.hasNext()) {
            File f = (File) it.next();
            allFiles.add(f);
        }
        Collections.sort(allFiles, new Comparator<File>() {
            public int compare(File o1, File o2) {
                long l1 = o1.lastModified(), l2 = o2.lastModified();
                return l1 < l2 ? -1 : l1 > l2 ? 1 : 0;
            }
        });
        String dataStorePath = directory.getAbsolutePath();
        long time = System.currentTimeMillis();
        int count = 0;
        int deletecount = 0;
        for (File f : allFiles) {
            if (f.exists()) {
                long length = f.length();
                String name = f.getPath();
                if (name.startsWith(dataStorePath)) {
                    name = name.substring(dataStorePath.length());
                }
                // convert to java path format
                name = name.replace("\\", "/");
                if (name.startsWith("/") || name.startsWith("\\")) {
                    name = name.substring(1);
                }
                if ((cache.currentSizeInBytes + length) < cache.maxSizeInBytes) {
                    count++;
                    cache.put(name, length);
                } else {
                    if (tryDelete(name)) {
                        deletecount++;
                    }
                }
                long now = System.currentTimeMillis();
                if (now > time + 5000) {
                    LOG.info("Processed {" + (count + deletecount) + "}/{" + allFiles.size() + "}");
                    time = now;
                }
            }
        }
        LOG.info("Cached {" + count + "}/{" + allFiles.size() + "} , currentSizeInBytes = " + cache.currentSizeInBytes);
        LOG.info("Deleted {" + deletecount + "}/{" + allFiles.size() + "} files .");
    }

    /**
     * Store an item in the cache and return the input stream. If cache is in purgeMode or file doesn't exists, inputstream from a transient
     * file is returned. Otherwise inputStream from cached file is returned.
     *
     * @param key the key
     * @param in the input stream
     * @return the (new) input stream
     */
    public synchronized InputStream store(String fileName, InputStream in) throws IOException {
        fileName = fileName.replace("\\", "/");
        File f = getFile(fileName);
        long length = 0;
        if (!f.exists() || isInPurgeMode()) {
            OutputStream out = null;
            File transFile = null;
            try {
                TransientFileFactory tff = TransientFileFactory.getInstance();
                transFile = tff.createTransientFile("s3-", "tmp", tmp);
                out = new BufferedOutputStream(new FileOutputStream(transFile));
                length = IOUtils.copyLarge(in, out);
            } finally {
                IOUtils.closeQuietly(out);
            }
            // rename the file to local fs cache
            if (canAdmitFile(length) && (f.getParentFile().exists() || f.getParentFile().mkdirs()) && transFile.renameTo(f) && f.exists()) {
                if (transFile.exists() && transFile.delete()) {
                    LOG.info("tmp file = " + transFile.getAbsolutePath() + " not deleted successfully");
                }
                transFile = null;
                toBeDeleted.remove(fileName);
                if (cache.get(fileName) == null) {
                    cache.put(fileName, f.length());
                }
            } else {
                f = transFile;
            }
        } else {
            // f.exists and not in purge mode
            f.setLastModified(System.currentTimeMillis());
            toBeDeleted.remove(fileName);
            if (cache.get(fileName) == null) {
                cache.put(fileName, f.length());
            }
        }
        cache.tryPurge();
        return new LazyFileInputStream(f);
    }

    /**
     * Store an item along with file in cache.
     *
     * @param fileName
     * @param src
     * @throws IOException
     */

    public synchronized void store(String fileName, File src) throws IOException {
        fileName = fileName.replace("\\", "/");
        File dest = getFile(fileName);
        File parent = dest.getParentFile();
        if (src.exists() && !dest.exists() && !src.equals(dest) && canAdmitFile(src.length()) && (parent.exists() || parent.mkdirs())
            && (src.renameTo(dest))) {
            toBeDeleted.remove(fileName);
            if (cache.get(fileName) == null) {
                cache.put(fileName, dest.length());
            }

        } else if (dest.exists()) {
            dest.setLastModified(System.currentTimeMillis());
            toBeDeleted.remove(fileName);
            if (cache.get(fileName) == null) {
                cache.put(fileName, dest.length());
            }
        }
        cache.tryPurge();
    }

    /**
     * Get the stream, or null if not in the cache.
     *
     * @param fileName the file name
     * @return the stream or null
     */
    public InputStream getIfStored(String fileName) throws IOException {

        fileName = fileName.replace("\\", "/");
        File f = getFile(fileName);
        synchronized (this) {
            if (!f.exists() || isInPurgeMode()) {
                log("purgeMode true or file doesn't exists: getIfStored returned");
                return null;
            } else {
                f.setLastModified(System.currentTimeMillis());
                return new LazyFileInputStream(f);
            }
        }
    }

    /**
     * delete an item from cache.
     *
     * @param fileName
     */

    public synchronized void delete(String fileName) {
        if (isInPurgeMode()) {
            log("purgeMode true :delete returned");
        }
        fileName = fileName.replace("\\", "/");
        cache.remove(fileName);
    }

    /**
     * Return length of item if exists or null
     *
     * @param fileName
     * @return
     */
    public Long getFileLength(String fileName) {
        fileName = fileName.replace("\\", "/");
        File f = getFile(fileName);
        synchronized (this) {
            if (!f.exists() || isInPurgeMode()) {
                log("purgeMode true or file doesn't exists: getFileLength returned");
                return null;
            }
            f.setLastModified(System.currentTimeMillis());
            return f.length();
        }
    }

    /**
     * Close the cache. All temporary files are deleted.
     */
    public void close() {
        log("close");
        deleteOldFiles();
    }

    /**
     * check if cache can admit bytes of given length.
     *
     * @param length
     * @return
     */
    private synchronized boolean canAdmitFile(long length) {
        // order is important here
        boolean value = !isInPurgeMode() && (cache.canAdmitFile(length));
        if (!value) {
            log("cannot admit file of length=" + length + " and currentSizeInBytes=" + cache.currentSizeInBytes);
        }
        return value;
    }

    /**
     * Return if cache is in purge mode
     *
     * @return
     */
    private synchronized boolean isInPurgeMode() {
        return purgeMode || size == 0;
    }

    private synchronized void setPurgeMode(boolean purgeMode) {
        this.purgeMode = purgeMode;
    }

    private File getFile(String fileName) {
        return new File(directory, fileName);
    }

    private void deleteOldFiles() {
        int initialSize = toBeDeleted.size();
        int count = 0;
        for (String n : new ArrayList<String>(toBeDeleted)) {
            if (tryDelete(n)) {
                count++;
            }
        }
        LOG.info("deleted [" + count + "]/[" + initialSize + "] files");
    }

    private boolean tryDelete(String fileName) {
        log("cache delete " + fileName);
        File f = getFile(fileName);
        if (f.exists() && f.delete()) {
            log(fileName + "  deleted successfully");
            toBeDeleted.remove(fileName);
            while (true) {
                f = f.getParentFile();
                if (f.equals(directory) || f.list().length > 0) {
                    break;
                }
                // delete empty parent folders (except the main directory)
                f.delete();
            }
            return true;
        } else if (f.exists()) {
            LOG.info("not able to delete file = " + f.getAbsolutePath());
            toBeDeleted.add(fileName);
            return false;
        }
        return true;
    }

    private static int maxSizeElements(long bytes) {
        // after a CQ installation, the average item in
        // the data store is about 52 KB
        int count = (int) (bytes / 65535);
        count = Math.max(1024, count);
        count = Math.min(64 * 1024, count);
        return count;
    }

    private void log(String s) {
        LOG.debug(s);
    }

    /**
     * A LRU map. Key: file name, value: file length
     */
    private class LRUCache extends LinkedHashMap<String, Long> {
        private static final long serialVersionUID = 1L;

        private volatile long currentSizeInBytes;

        private final long maxSizeInBytes;

        private long cachePurgeTrigSize;

        private long cachePurgeResize;

        public LRUCache(long maxSizeInBytes, double cachePurgeTrigFactor, double cachePurgeResizeFactor) {
            super(maxSizeElements(maxSizeInBytes), (float) 0.75, true);
            this.maxSizeInBytes = maxSizeInBytes;
            this.cachePurgeTrigSize = new Double(cachePurgeTrigFactor * maxSizeInBytes).longValue();
            this.cachePurgeResize = new Double(cachePurgeResizeFactor * maxSizeInBytes).longValue();
        }

        @Override
        public synchronized Long remove(Object key) {
            String fileName = (String) key;
            fileName = fileName.replace("\\", "/");
            Long flength = null;
            if (tryDelete(fileName)) {
                flength = super.remove(key);
                if (flength != null) {
                    log("cache entry { " + fileName + "} with size {" + flength + "} removed.");
                    currentSizeInBytes -= flength.longValue();
                }
            } else if (!getFile(fileName).exists()) {
                // second attempt. remove from cache if file doesn't exists
                flength = super.remove(key);
                if (flength != null) {
                    log(" file not exists. cache entry { " + fileName + "} with size {" + flength + "} removed.");
                    currentSizeInBytes -= flength.longValue();
                }
            }
            return flength;
        }

        @Override
        public synchronized Long put(String key, Long value) {
            long flength = value.longValue();
            currentSizeInBytes += flength;
            return super.put(key.replace("\\", "/"), value);
        }

        private synchronized void tryPurge() {
            if (currentSizeInBytes > cachePurgeTrigSize && !isInPurgeMode()) {
                setPurgeMode(true);
                LOG.info("currentSizeInBytes[" + cache.currentSizeInBytes + "] exceeds (cachePurgeTrigSize)[" + cache.cachePurgeTrigSize
                    + "]");
                new Thread(new PurgeJob()).start();
            }
        }

        private synchronized boolean canAdmitFile(long length) {
            return (cache.currentSizeInBytes + length < cache.maxSizeInBytes);
        }
    }

    private class PurgeJob implements Runnable {
        public void run() {
            try {
                synchronized (cache) {
                    LOG.info(" cache purge job started");
                    // first try to delete toBeDeleted files
                    int initialSize = cache.size();
                    for (String fileName : new ArrayList<String>(toBeDeleted)) {
                        cache.remove(fileName);
                    }
                    Iterator<Map.Entry<String, Long>> itr = cache.entrySet().iterator();
                    while (itr.hasNext()) {
                        Map.Entry<String, Long> entry = itr.next();
                        if (entry.getKey() != null) {
                            if (cache.currentSizeInBytes > cache.cachePurgeResize) {
                                itr.remove();

                            } else {
                                break;
                            }
                        }

                    }
                    LOG.info(" cache purge job completed: cleaned [" + (initialSize - cache.size()) + "] files and currentSizeInBytes = [ "
                        + cache.currentSizeInBytes + "]");
                }
            } catch (Exception e) {
                LOG.error("error in purge jobs:", e);
            } finally {
                setPurgeMode(false);
            }
        }
    }
}
