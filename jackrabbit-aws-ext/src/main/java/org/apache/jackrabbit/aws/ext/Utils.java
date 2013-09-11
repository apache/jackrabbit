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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Amazon S3 utilities.
 */
public final class Utils {

    public static final String DEFAULT_CONFIG_FILE = "aws.properties";
    
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private static final String DELETE_CONFIG_SUFFIX = ";burn";

    /**
     * private constructor so that class cannot initialized from outside.
     */
    private Utils() {

    }

    /**
     * Create AmazonS3Client from properties.
     * 
     * @param prop properties to configure @link {@link AmazonS3Client}
     * @return {@link AmazonS3Client}
     */
    public static AmazonS3Client openService(final Properties prop) {
        AWSCredentials credentials = new BasicAWSCredentials(
            prop.getProperty(S3Constants.ACCESS_KEY),
            prop.getProperty(S3Constants.SECRET_KEY));
        int connectionTimeOut = Integer.parseInt(prop.getProperty("connectionTimeout"));
        int socketTimeOut = Integer.parseInt(prop.getProperty("socketTimeout"));
        int maxConnections = Integer.parseInt(prop.getProperty("maxConnections"));
        int maxErrorRetry = Integer.parseInt(prop.getProperty("maxErrorRetry"));
        ClientConfiguration cc = new ClientConfiguration();
        cc.setConnectionTimeout(connectionTimeOut);
        cc.setSocketTimeout(socketTimeOut);
        cc.setMaxConnections(maxConnections);
        cc.setMaxErrorRetry(maxErrorRetry);
        return new AmazonS3Client(credentials, cc);
    }

    /**
     * Delete S3 bucket. This method first deletes all objects from bucket and
     * then delete empty bucket.
     * 
     * @param prop properties to configure @link {@link AmazonS3Client} and
     * delete bucket.
     */
    public static void deleteBucket(final Properties prop) throws IOException {
        AmazonS3Client s3service = openService(prop);
        String bucketName = prop.getProperty(S3Constants.S3_BUCKET);
        if (!s3service.doesBucketExist(bucketName)) {
            return;
        }
        ObjectListing prevObjectListing = s3service.listObjects(bucketName);
        while (true) {
            List<DeleteObjectsRequest.KeyVersion> deleteList = new ArrayList<DeleteObjectsRequest.KeyVersion>();
            for (S3ObjectSummary s3ObjSumm : prevObjectListing.getObjectSummaries()) {
                deleteList.add(new DeleteObjectsRequest.KeyVersion(
                    s3ObjSumm.getKey()));
            }
            if (deleteList.size() > 0) {
                DeleteObjectsRequest delObjsReq = new DeleteObjectsRequest(
                    bucketName);
                delObjsReq.setKeys(deleteList);
                DeleteObjectsResult dobjs = s3service.deleteObjects(delObjsReq);
                if (dobjs.getDeletedObjects().size() != deleteList.size()) {
                    throw new IOException(
                        "Incomplete delete object request. only  "
                            + dobjs.getDeletedObjects().size() + " out of "
                            + deleteList.size() + " are deleted");
                }
                LOG.info(deleteList.size()
                        + " records deleted from datastore");
            }
            if (!prevObjectListing.isTruncated()) {
                break;
            }
            prevObjectListing = s3service.listNextBatchOfObjects(prevObjectListing);
        }
        s3service.deleteBucket(bucketName);
        s3service.shutdown();
    }

    /**
     * Read a configuration properties file. If the file name ends with ";burn",
     * the file is deleted after reading.
     * 
     * @param fileName the properties file name
     * @return the properties
     * @throws IOException if the file doesn't exist
     */
    public static Properties readConfig(String fileName) throws IOException {
        boolean delete = false;
        if (fileName.endsWith(DELETE_CONFIG_SUFFIX)) {
            delete = true;
            fileName = fileName.substring(0, fileName.length()
                - DELETE_CONFIG_SUFFIX.length());
        }
        if (!new File(fileName).exists()) {
            throw new IOException("Config file not found: " + fileName);
        }
        Properties prop = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(fileName);
            prop.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
            if (delete) {
                deleteIfPossible(new File(fileName));
            }
        }
        return prop;
    }

    private static void deleteIfPossible(final File file) {
        boolean deleted = file.delete();
        if (!deleted) {
            LOG.warn("Could not delete " + file.getAbsolutePath());
        }
    }

}
