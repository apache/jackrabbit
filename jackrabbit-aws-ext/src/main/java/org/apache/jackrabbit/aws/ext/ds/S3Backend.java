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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.jackrabbit.aws.ext.S3Constants;
import org.apache.jackrabbit.aws.ext.Utils;
import org.apache.jackrabbit.core.data.AsyncUploadCallback;
import org.apache.jackrabbit.core.data.Backend;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

/**
 * A data store backend that stores data on Amazon S3.
 */
public class S3Backend implements Backend {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(S3Backend.class);

    private static final String KEY_PREFIX = "dataStore_";

    /**
     * The default value AWS bucket region.
     */
    private static final String DEFAULT_AWS_BUCKET_REGION = "us-standard";

    /**
     * constants to define endpoint to various AWS region
     */
    private static final String AWSDOTCOM = "amazonaws.com";

    private static final String S3 = "s3";

    private static final String DOT = ".";

    private static final String DASH = "-";
    
    private AmazonS3Client s3service;

    private String bucket;

    private TransferManager tmx;

    private CachingDataStore store;

    private Properties properties;

    private Date startTime;

    /**
     * Initialize S3Backend. It creates AmazonS3Client and TransferManager from
     * aws.properties. It creates S3 bucket if it doesn't pre-exist in S3.
     */
    @Override
    public void init(CachingDataStore store, String homeDir, String config)
            throws DataStoreException {
        Properties initProps = null;
        //Check is configuration is already provided. That takes precedence
        //over config provided via file based config
        if(this.properties != null){
            initProps = this.properties;
        } else {
            if(config == null){
                config = Utils.DEFAULT_CONFIG_FILE;
            }
            try{
                initProps = Utils.readConfig(config);
            }catch(IOException e){
                throw new DataStoreException("Could not initialize S3 from "
                        + config, e);
            }
            this.properties = initProps;
        }
        init(store, homeDir, initProps);
    }

    public void init(CachingDataStore store, String homeDir, Properties prop)
            throws DataStoreException {

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            startTime = new Date();
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            if (LOG.isDebugEnabled()) {
                LOG.debug("init");
            }
            this.store = store;
            s3service = Utils.openService(prop);
            if (bucket == null || "".equals(bucket.trim())) {
                bucket = prop.getProperty(S3Constants.S3_BUCKET);
            }
            String region = prop.getProperty(S3Constants.S3_REGION);
            String endpoint = null;
            if (!s3service.doesBucketExist(bucket)) {

                if (DEFAULT_AWS_BUCKET_REGION.equals(region)) {
                    s3service.createBucket(bucket, Region.US_Standard);
                    endpoint = S3 + DOT + AWSDOTCOM;
                } else if (Region.EU_Ireland.toString().equals(region)) {
                    s3service.createBucket(bucket, Region.EU_Ireland);
                    endpoint = "s3-eu-west-1" + DOT + AWSDOTCOM;
                } else {
                    s3service.createBucket(bucket, region);
                    endpoint = S3 + DASH + region + DOT + AWSDOTCOM;
                }
                LOG.info("Created bucket: " + bucket + " in " + region);
            } else {
                LOG.info("Using bucket: " + bucket);
                if (DEFAULT_AWS_BUCKET_REGION.equals(region)) {
                    endpoint = S3 + DOT + AWSDOTCOM;
                } else if (Region.EU_Ireland.toString().equals(region)) {
                    endpoint = "s3-eu-west-1" + DOT + AWSDOTCOM;
                } else {
                    endpoint = S3 + DASH + region + DOT + AWSDOTCOM;
                }
            }
            String propEndPoint = prop.getProperty(S3Constants.S3_END_POINT);
            if (propEndPoint != null & !"".equals(propEndPoint)) {
                endpoint = propEndPoint;
            }
            /*
             * setting endpoint to remove latency of redirection. If endpoint is
             * not set, invocation first goes us standard region, which
             * redirects it to correct location.
             */
            s3service.setEndpoint(endpoint);
            LOG.info("S3 service endpoint: " + endpoint);

            int writeThreads = 10;
            String writeThreadsStr = prop.getProperty(S3Constants.S3_WRITE_THREADS);
            if (writeThreadsStr != null) {
                writeThreads = Integer.parseInt(writeThreadsStr);
            }
            LOG.info("Using thread pool of [" + writeThreads
                + "] threads in S3 transfer manager");
            tmx = new TransferManager(s3service,
                (ThreadPoolExecutor) Executors.newFixedThreadPool(writeThreads,
                    new NamedThreadFactory("s3-transfer-manager-worker")));
            String renameKeyProp = prop.getProperty(S3Constants.S3_RENAME_KEYS);
            boolean renameKeyBool = (renameKeyProp == null || "".equals(renameKeyProp))
                    ? true
                    : Boolean.parseBoolean(renameKeyProp);
            if (renameKeyBool) {
                renameKeys();
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("S3 Backend initialized in ["
                    + (System.currentTimeMillis() - startTime.getTime())
                    + "] ms");
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("  error ", e);
            }
            throw new DataStoreException("Could not initialize S3 from "
                + prop, e);
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    /**
     * It uploads file to Amazon S3. If file size is greater than 5MB, this
     * method uses parallel concurrent connections to upload.
     */
    @Override
    public void write(DataIdentifier identifier, File file)
            throws DataStoreException {
        this.write(identifier, file, false, null);

    }

    @Override
    public void writeAsync(DataIdentifier identifier, File file,
            AsyncUploadCallback callback) throws DataStoreException {
        if (callback == null) {
            throw new IllegalArgumentException(
                "callback parameter cannot be null in asyncUpload");
        }
        Thread th = new Thread(new AsyncUploadJob(identifier, file, callback));
        th.start();
    }

    /**
     * Check if record identified by identifier exists in Amazon S3.
     */
    @Override
    public boolean exists(DataIdentifier identifier) throws DataStoreException {
        long start = System.currentTimeMillis();
        String key = getKeyName(identifier);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            ObjectMetadata objectMetaData = s3service.getObjectMetadata(bucket,
                key);
            if (objectMetaData != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("exists [" + identifier + "]: [true] took ["
                        + (System.currentTimeMillis() - start) + "] ms");
                }
                return true;
            }
            return false;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 404) {
                LOG.info("exists [" + identifier + "]: [false] took ["
                    + (System.currentTimeMillis() - start) + "] ms");
                return false;
            }
            throw new DataStoreException(
                "Error occured to getObjectMetadata for key ["
                    + identifier.toString() + "]", e);
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    @Override
    public boolean exists(DataIdentifier identifier, boolean touch)
            throws DataStoreException {
        long start = System.currentTimeMillis();
        String key = getKeyName(identifier);
        ObjectMetadata objectMetaData = null;
        boolean retVal = false;
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            objectMetaData = s3service.getObjectMetadata(bucket, key);
            if (objectMetaData != null) {
                retVal = true;
                if (touch) {
                    CopyObjectRequest copReq = new CopyObjectRequest(bucket,
                        key, bucket, key);
                    copReq.setNewObjectMetadata(objectMetaData);
                    s3service.copyObject(copReq);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("[ " + identifier.toString()
                            + "] touched took ["
                            + (System.currentTimeMillis() - start) + "] ms");
                    }
                }
            } else {
                retVal = false;
            }

        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 404) {
                retVal = false;
            } else {
                throw new DataStoreException(
                    "Error occured to find exists for key ["
                        + identifier.toString() + "]", e);
            }
        } catch (Exception e) {
            throw new DataStoreException(
                "Error occured to find exists for key  "
                    + identifier.toString(), e);
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("exists [" + identifier + "]: [" + retVal + "] took ["
                + (System.currentTimeMillis() - start) + "] ms");
        }
        return retVal;
    }

    @Override
    public InputStream read(DataIdentifier identifier)
            throws DataStoreException {
        long start = System.currentTimeMillis();
        String key = getKeyName(identifier);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            S3Object object = s3service.getObject(bucket, key);
            InputStream in = object.getObjectContent();
            if (LOG.isDebugEnabled()) {
                LOG.debug("[ " + identifier.toString() + "] read took ["
                    + (System.currentTimeMillis() - start) + "] ms");
            }
            return in;
        } catch (AmazonServiceException e) {
            throw new DataStoreException("Object not found: " + key, e);
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    @Override
    public Iterator<DataIdentifier> getAllIdentifiers()
            throws DataStoreException {
        long start = System.currentTimeMillis();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            Set<DataIdentifier> ids = new HashSet<DataIdentifier>();
            ObjectListing prevObjectListing = s3service.listObjects(bucket);
            while (true) {
                for (S3ObjectSummary s3ObjSumm : prevObjectListing.getObjectSummaries()) {
                    String id = getIdentifierName(s3ObjSumm.getKey());
                    if (id != null) {
                        ids.add(new DataIdentifier(id));
                    }
                }
                if (!prevObjectListing.isTruncated()) break;
                prevObjectListing = s3service.listNextBatchOfObjects(prevObjectListing);
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("getAllIdentifiers returned size [ " + ids.size()
                    + "] took [" + (System.currentTimeMillis() - start)
                    + "] ms");
            }
            return ids.iterator();
        } catch (AmazonServiceException e) {
            throw new DataStoreException("Could not list objects", e);
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    @Override
    public long getLastModified(DataIdentifier identifier)
            throws DataStoreException {
        long start = System.currentTimeMillis();
        String key = getKeyName(identifier);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            ObjectMetadata object = s3service.getObjectMetadata(bucket, key);
            long lastModified = object.getLastModified().getTime();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Identifier [" + identifier.toString()
                    + "] 's lastModified = [" + lastModified + "] took ["
                    + (System.currentTimeMillis() - start) + "] ms");
            }
            return lastModified;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 404) {
                LOG.info("getLastModified:Identifier [" + identifier.toString()
                    + "] not found. Took ["
                    + (System.currentTimeMillis() - start) + "]ms");
            }
            throw new DataStoreException(e);
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    @Override
    public long getLength(DataIdentifier identifier) throws DataStoreException {
        long start = System.currentTimeMillis();
        String key = getKeyName(identifier);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            ObjectMetadata object = s3service.getObjectMetadata(bucket, key);
            long length = object.getContentLength();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Identifier [" + identifier.toString()
                    + "] 's length = [" + length + "] took ["
                    + (System.currentTimeMillis() - start) + "] ms");
            }
            return length;
        } catch (AmazonServiceException e) {
            throw new DataStoreException("Could not length of dataIdentifier "
                + identifier, e);
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    @Override
    public void deleteRecord(DataIdentifier identifier)
            throws DataStoreException {
        long start = System.currentTimeMillis();
        String key = getKeyName(identifier);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            s3service.deleteObject(bucket, key);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Identifier [" + identifier.toString()
                    + "] 's deleted. It took ["
                    + (System.currentTimeMillis() - start) + "] ms");
            }
        } catch (AmazonServiceException e) {
            throw new DataStoreException(
                "Could not getLastModified of dataIdentifier " + identifier, e);
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    @Override
    public Set<DataIdentifier> deleteAllOlderThan(long min)
            throws DataStoreException {
        long start = System.currentTimeMillis();
        // S3 stores lastModified to lower boundary of timestamp in ms.
        // and hence min is reduced by 1000ms.
        min = min - 1000;
        Set<DataIdentifier> deleteIdSet = new HashSet<DataIdentifier>(30);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            ObjectListing prevObjectListing = s3service.listObjects(bucket);
            while (true) {
                List<DeleteObjectsRequest.KeyVersion> deleteList = new ArrayList<DeleteObjectsRequest.KeyVersion>();
                for (S3ObjectSummary s3ObjSumm : prevObjectListing.getObjectSummaries()) {
                    DataIdentifier identifier = new DataIdentifier(
                        getIdentifierName(s3ObjSumm.getKey()));
                    long lastModified = s3ObjSumm.getLastModified().getTime();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("id [" + identifier + "], lastModified ["
                            + lastModified + "]");
                    }
                    if (!store.isInUse(identifier) && lastModified < min) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("add id :" + s3ObjSumm.getKey()
                                + " to delete lists");
                        }
                        deleteList.add(new DeleteObjectsRequest.KeyVersion(
                            s3ObjSumm.getKey()));
                        deleteIdSet.add(identifier);
                    }
                }
                if (deleteList.size() > 0) {
                    DeleteObjectsRequest delObjsReq = new DeleteObjectsRequest(
                        bucket);
                    delObjsReq.setKeys(deleteList);
                    DeleteObjectsResult dobjs = s3service.deleteObjects(delObjsReq);
                    if (dobjs.getDeletedObjects().size() != deleteList.size()) {
                        throw new DataStoreException(
                            "Incomplete delete object request. only  "
                                + dobjs.getDeletedObjects().size() + " out of "
                                + deleteList.size() + " are deleted");
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(deleteList
                                + " records deleted from datastore");
                        }
                    }
                }
                if (!prevObjectListing.isTruncated()) {
                    break;
                }
                prevObjectListing = s3service.listNextBatchOfObjects(prevObjectListing);
            }
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
        LOG.info("deleteAllOlderThan: min=[" + min + "] exit. Deleted ["
            + deleteIdSet + "] records. Number of records deleted ["
            + deleteIdSet.size() + "] took ["
            + (System.currentTimeMillis() - start) + "] ms");
        return deleteIdSet;
    }

    @Override
    public void close() {
        // backend is closing. abort all mulitpart uploads from start.
        tmx.abortMultipartUploads(bucket, startTime);
        tmx.shutdownNow();
        s3service.shutdown();
        LOG.info("S3Backend closed.");
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * Properties used to configure the backend. If provided explicitly
     * before init is invoked then these take precedence
     *
     * @param properties  to configure S3Backend
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    private void write(DataIdentifier identifier, File file,
            boolean asyncUpload, AsyncUploadCallback callback)
            throws DataStoreException {
        String key = getKeyName(identifier);
        ObjectMetadata objectMetaData = null;
        long start = System.currentTimeMillis();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            // check if the same record already exists
            try {
                objectMetaData = s3service.getObjectMetadata(bucket, key);
            } catch (AmazonServiceException ase) {
                if (ase.getStatusCode() != 404) {
                    throw ase;
                }
            }
            if (objectMetaData != null) {
                long l = objectMetaData.getContentLength();
                if (l != file.length()) {
                    throw new DataStoreException("Collision: " + key
                        + " new length: " + file.length() + " old length: " + l);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(key + "   exists, lastmodified ="
                        + objectMetaData.getLastModified().getTime());
                }
                CopyObjectRequest copReq = new CopyObjectRequest(bucket, key,
                    bucket, key);
                copReq.setNewObjectMetadata(objectMetaData);
                s3service.copyObject(copReq);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("lastModified of " + identifier.toString()
                        + " updated successfully");
                }
                if (callback != null) {
                    callback.call(identifier, file,
                        AsyncUploadCallback.RESULT.SUCCESS);
                }
            }

            if (objectMetaData == null) {
                try {
                    // start multipart parallel upload using amazon sdk
                    Upload up = tmx.upload(new PutObjectRequest(bucket, key,
                        file));
                    // wait for upload to finish
                    if (asyncUpload) {
                        up.addProgressListener(new S3UploadProgressListener(
                            identifier, file, callback));
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("added upload progress listener to identifier ["
                                + identifier + "]");
                        }
                    } else {
                        up.waitForUploadResult();
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("synchronous upload to identifier ["
                                + identifier + "] completed.");
                        }
                        if (callback != null) {
                            callback.call(identifier, file,
                                AsyncUploadCallback.RESULT.SUCCESS);
                        }
                    }
                } catch (Exception e2) {
                    if (!asyncUpload) {
                        callback.call(identifier, file,
                            AsyncUploadCallback.RESULT.ABORTED);
                    }
                    throw new DataStoreException("Could not upload " + key, e2);
                }
            }
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("write [" + identifier + "] length [" + file.length()
                + "], in async mode [" + asyncUpload + "] in ["
                + (System.currentTimeMillis() - start) + "] ms.");
        }
    }

    /**
     * This method rename object keys in S3 concurrently. The number of
     * concurrent threads is defined by 'maxConnections' property in
     * aws.properties. As S3 doesn't have "move" command, this method simulate
     * move as copy object object to new key and then delete older key.
     */
    private void renameKeys() throws DataStoreException {
        long startTime = System.currentTimeMillis();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        long count = 0;
        try {
            Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
            ObjectListing prevObjectListing = s3service.listObjects(bucket,
                KEY_PREFIX);
            List<DeleteObjectsRequest.KeyVersion> deleteList = new ArrayList<DeleteObjectsRequest.KeyVersion>();
            int nThreads = Integer.parseInt(properties.getProperty("maxConnections"));
            ExecutorService executor = Executors.newFixedThreadPool(nThreads,
                new NamedThreadFactory("s3-object-rename-worker"));
            boolean taskAdded = false;
            while (true) {
                for (S3ObjectSummary s3ObjSumm : prevObjectListing.getObjectSummaries()) {
                    executor.execute(new KeyRenameThread(s3ObjSumm.getKey()));
                    taskAdded = true;
                    count++;
                    deleteList.add(new DeleteObjectsRequest.KeyVersion(
                        s3ObjSumm.getKey()));
                }
                if (!prevObjectListing.isTruncated()) break;
                prevObjectListing = s3service.listNextBatchOfObjects(prevObjectListing);
            }
            // This will make the executor accept no new threads
            // and finish all existing threads in the queue
            executor.shutdown();

            try {
                // Wait until all threads are finish
                while (taskAdded
                    && !executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOG.info("Rename S3 keys tasks timedout. Waiting again");
                }
            } catch (InterruptedException ie) {

            }
            LOG.info("Renamed [" + count + "] keys, time taken ["
                + ((System.currentTimeMillis() - startTime) / 1000) + "] sec");
            // Delete older keys.
            if (deleteList.size() > 0) {
                DeleteObjectsRequest delObjsReq = new DeleteObjectsRequest(
                    bucket);
                int batchSize = 500, startIndex = 0, size = deleteList.size();
                int endIndex = batchSize < size ? batchSize : size;
                while (endIndex <= size) {
                    delObjsReq.setKeys(Collections.unmodifiableList(deleteList.subList(
                        startIndex, endIndex)));
                    DeleteObjectsResult dobjs = s3service.deleteObjects(delObjsReq);
                    LOG.info("Records[" + dobjs.getDeletedObjects().size()
                        + "] deleted in datastore from index [" + startIndex
                        + "] to [" + (endIndex - 1) + "]");
                    if (endIndex == size) {
                        break;
                    } else {
                        startIndex = endIndex;
                        endIndex = (startIndex + batchSize) < size
                                ? (startIndex + batchSize)
                                : size;
                    }
                }
            }
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    /**
     * The method convert old key format to new format. For e.g. this method
     * converts old key dataStore_004cb70c8f87d78f04da41e7547cb434094089ea to
     * 004c-b70c8f87d78f04da41e7547cb434094089ea.
     */
    private static String convertKey(String oldKey)
            throws IllegalArgumentException {
        if (!oldKey.startsWith(KEY_PREFIX)) {
            throw new IllegalArgumentException("[" + oldKey
                + "] doesn't start with prefix [" + KEY_PREFIX + "]");
        }
        String key = oldKey.substring(KEY_PREFIX.length());
        return key.substring(0, 4) + DASH + key.substring(4);
    }

    /**
     * Get key from data identifier. Object is stored with key in S3.
     */
    private static String getKeyName(DataIdentifier identifier) {
        String key = identifier.toString();
        return key.substring(0, 4) + DASH + key.substring(4);
    }

    /**
     * Get data identifier from key.
     */
    private static String getIdentifierName(String key) {
        if (!key.contains(DASH)) {
            return null;
        }
        return key.substring(0, 4) + key.substring(5);
    }

    /**
     * The class renames object key in S3 in a thread.
     */
    private class KeyRenameThread implements Runnable {

        private String oldKey;

        public void run() {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(
                    getClass().getClassLoader());
                String newS3Key = convertKey(oldKey);
                CopyObjectRequest copReq = new CopyObjectRequest(bucket,
                    oldKey, bucket, newS3Key);
                s3service.copyObject(copReq);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(oldKey + " renamed to " + newS3Key);
                }
            } finally {
                if (contextClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(
                        contextClassLoader);
                }
            }
        }

        public KeyRenameThread(String oldKey) {
            this.oldKey = oldKey;
        }
    }

    /**
     * Listener which receives callback on status of S3 upload.
     */
    private class S3UploadProgressListener implements ProgressListener {

        private File file;

        private DataIdentifier identifier;

        private AsyncUploadCallback callback;

        public S3UploadProgressListener(DataIdentifier identifier, File file,
                AsyncUploadCallback callback) {
            super();
            this.identifier = identifier;
            this.file = file;
            this.callback = callback;
        }

        public void progressChanged(ProgressEvent progressEvent) {
            switch (progressEvent.getEventCode()) {
                case ProgressEvent.COMPLETED_EVENT_CODE:
                    callback.call(identifier, file,
                        AsyncUploadCallback.RESULT.SUCCESS);
                    break;
                case ProgressEvent.FAILED_EVENT_CODE:
                    callback.call(identifier, file,
                        AsyncUploadCallback.RESULT.FAILED);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * This class implements {@link Runnable} interface to upload {@link File}
     * to S3 asynchronously.
     */
    private class AsyncUploadJob implements Runnable {

        private DataIdentifier identifier;

        private File file;

        private AsyncUploadCallback callback;

        public AsyncUploadJob(DataIdentifier identifier, File file,
                AsyncUploadCallback callback) {
            super();
            this.identifier = identifier;
            this.file = file;
            this.callback = callback;
        }

        public void run() {
            try {
                write(identifier, file, true, callback);
            } catch (DataStoreException e) {
                LOG.error("Could not upload [" + identifier + "], file[" + file
                    + "]", e);
            }

        }
    }
}
