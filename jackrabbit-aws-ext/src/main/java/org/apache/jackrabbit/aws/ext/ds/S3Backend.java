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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.jackrabbit.aws.ext.S3Constants;
import org.apache.jackrabbit.aws.ext.Utils;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

/**
 * A data store backend that stores data on Amazon S3.
 */
public class S3Backend implements Backend {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(S3Backend.class);

    private AmazonS3 s3service;

    private String bucket;

    private TransferManager tmx;

    private CachingDataStore store;

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

    public void init(CachingDataStore store, String homeDir, String config) throws DataStoreException {
        if (config == null) {
            config = Utils.DEFAULT_CONFIG_FILE;
        }
        try {
            Properties prop = Utils.readConfig(config);
            LOG.debug("init");
            this.store = store;
            s3service = Utils.openService(prop);
            bucket = prop.getProperty(S3Constants.S3_BUCKET);
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
            /*
             * setting endpoint to remove latency of redirection. If endpoint is not set, invocation first goes us standard region, which
             * redirects it to correct location.
             */
            s3service.setEndpoint(endpoint);
            LOG.info("S3 service endpoint: " + endpoint);
            tmx = new TransferManager(s3service, createDefaultExecutorService());
            LOG.debug("  done");
        } catch (Exception e) {
            LOG.debug("  error ", e);
            throw new DataStoreException("Could not initialize S3 from " + config, e);
        }
    }

    public void write(DataIdentifier identifier, File file) throws DataStoreException {
        String key = getKeyName(identifier);
        ObjectMetadata objectMetaData = null;
        long start = System.currentTimeMillis();
        LOG.debug("write {0} length {1}", identifier, file.length());
        try {
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
                    throw new DataStoreException("Collision: " + key + " new length: " + file.length() + " old length: " + l);
                }
                LOG.debug(key + "   exists");
                CopyObjectRequest copReq = new CopyObjectRequest(bucket, key, bucket, key);
                copReq.setNewObjectMetadata(objectMetaData);
                s3service.copyObject(copReq);
                LOG.debug("lastModified of " + identifier.toString() + " updated successfully");
                LOG.debug("   updated");
            }
        } catch (AmazonServiceException e) {
            LOG.debug("   does not exist", e);
            // not found - create it
        }
        if (objectMetaData == null) {
            LOG.debug("   creating");
            try {
                // start multipart parallel upload using amazon sdk
                Upload up = tmx.upload(new PutObjectRequest(bucket, key, file));
                // wait for upload to finish
                UploadResult uploadResult = up.waitForUploadResult();
                LOG.debug("   done");
            } catch (Exception e2) {
                LOG.debug("   could not upload", e2);
                throw new DataStoreException("Could not upload " + key, e2);
            }
        }
        LOG.debug("    ms: {0}", System.currentTimeMillis() - start);

    }

    private String getKeyName(DataIdentifier identifier) {
        return KEY_PREFIX + identifier.toString();
    }

    private String getIdentifierName(String key) {
        if (!key.startsWith(KEY_PREFIX)) {
            return null;
        }
        return key.substring(KEY_PREFIX.length());
    }

    public boolean exists(DataIdentifier identifier) throws DataStoreException {
        String key = getKeyName(identifier);
        try {
            LOG.debug("exists {0}", identifier);
            ObjectMetadata objectMetaData = s3service.getObjectMetadata(bucket, key);
            if (objectMetaData != null) {
                LOG.debug("  true");
                return true;
            }
            return false;
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 404) {
                LOG.info("key [" + identifier.toString() + "] not found.");
                return false;
            }
            throw new DataStoreException("Error occured to getObjectMetadata for key [" + identifier.toString() + "]", e);
        }
    }

    public void touch(DataIdentifier identifier, long minModifiedDate) throws DataStoreException {
        String key = getKeyName(identifier);
        try {
            if (minModifiedDate != 0) {
                ObjectMetadata objectMetaData = s3service.getObjectMetadata(bucket, key);
                if (objectMetaData.getLastModified().getTime() < minModifiedDate) {
                    CopyObjectRequest copReq = new CopyObjectRequest(bucket, key, bucket, key);
                    copReq.setNewObjectMetadata(objectMetaData);
                    s3service.copyObject(copReq);
                    LOG.debug("lastModified of " + identifier.toString() + " updated successfully");
                }
            }
        } catch (Exception e) {
            throw new DataStoreException("An Exception occurred while trying to set the last modified date of record "
                + identifier.toString(), e);
        }
    }

    public InputStream read(DataIdentifier identifier) throws DataStoreException {
        String key = getKeyName(identifier);
        try {
            LOG.debug("read {" + identifier + "}");
            S3Object object = s3service.getObject(bucket, key);
            InputStream in = object.getObjectContent();
            LOG.debug("  return");
            return in;
        } catch (AmazonServiceException e) {
            throw new DataStoreException("Object not found: " + key, e);
        }
    }

    public Iterator<DataIdentifier> getAllIdentifiers() throws DataStoreException {
        try {
            LOG.debug("getAllIdentifiers");
            ArrayList<DataIdentifier> ids = new ArrayList<DataIdentifier>();
            ObjectListing prevObjectListing = s3service.listObjects(bucket, KEY_PREFIX);
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
            LOG.debug("  return");
            return ids.iterator();
        } catch (AmazonServiceException e) {
            throw new DataStoreException("Could not list objects", e);
        }
    }

    public void close() {
        s3service = null;
    }

    public long getLastModified(DataIdentifier identifier) throws DataStoreException {
        String key = getKeyName(identifier);
        try {
            ObjectMetadata object = s3service.getObjectMetadata(bucket, key);
            return object.getLastModified().getTime();
        } catch (AmazonServiceException e) {
            throw new DataStoreException("Could not getLastModified of dataIdentifier " + identifier, e);
        }
    }

    public long getLength(DataIdentifier identifier) throws DataStoreException {
        String key = getKeyName(identifier);
        try {
            ObjectMetadata object = s3service.getObjectMetadata(bucket, key);
            return object.getContentLength();
        } catch (AmazonServiceException e) {
            throw new DataStoreException("Could not length of dataIdentifier " + identifier, e);
        }
    }

    public void deleteRecord(DataIdentifier identifier) throws DataStoreException {
        String key = getKeyName(identifier);
        try {
            s3service.deleteObject(bucket, key);
        } catch (AmazonServiceException e) {
            throw new DataStoreException("Could not getLastModified of dataIdentifier " + identifier, e);
        }
    }

    public List<DataIdentifier> deleteAllOlderThan(long min) throws DataStoreException {
        LOG.info("deleteAllOlderThan " + new Date(min));
        List<DataIdentifier> diDeleteList = new ArrayList<DataIdentifier>(30);
        ObjectListing prevObjectListing = s3service.listObjects(bucket);
        while (true) {
            List<DeleteObjectsRequest.KeyVersion> deleteList = new ArrayList<DeleteObjectsRequest.KeyVersion>();
            for (S3ObjectSummary s3ObjSumm : prevObjectListing.getObjectSummaries()) {
                DataIdentifier identifier = new DataIdentifier(getIdentifierName(s3ObjSumm.getKey()));
                if (!store.inUse.containsKey(identifier) && s3ObjSumm.getLastModified().getTime() < min) {
                    LOG.info("add id :" + s3ObjSumm.getKey() + " to delete lists");
                    deleteList.add(new DeleteObjectsRequest.KeyVersion(s3ObjSumm.getKey()));
                    diDeleteList.add(new DataIdentifier(getIdentifierName(s3ObjSumm.getKey())));
                }
            }
            if (deleteList.size() > 0) {
                DeleteObjectsRequest delObjsReq = new DeleteObjectsRequest(bucket);
                delObjsReq.setKeys(deleteList);
                DeleteObjectsResult dobjs = s3service.deleteObjects(delObjsReq);
                if (dobjs.getDeletedObjects().size() != deleteList.size()) {
                    throw new DataStoreException("Incomplete delete object request. only  " + dobjs.getDeletedObjects().size() + " out of "
                        + deleteList.size() + " are deleted");
                } else {
                    LOG.info(deleteList.size() + " records deleted from datastore");
                }
            }
            if (!prevObjectListing.isTruncated()) break;
            prevObjectListing = s3service.listNextBatchOfObjects(prevObjectListing);
        }
        LOG.info("deleteAllOlderThan  exit");
        return diDeleteList;
    }

    /**
     * Returns a new thread pool configured with the default settings.
     *
     * @return A new thread pool configured with the default settings.
     */
    private ThreadPoolExecutor createDefaultExecutorService() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private int threadCount = 1;

            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setContextClassLoader(getClass().getClassLoader());
                thread.setName("s3-transfer-manager-worker-" + threadCount++);
                return thread;
            }
        };
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(10, threadFactory);
    }
}
