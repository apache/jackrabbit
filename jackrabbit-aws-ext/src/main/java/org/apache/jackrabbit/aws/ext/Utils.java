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
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Amazon S3 utilities.
 */
public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static final String DEFAULT_CONFIG_FILE = "aws.properties";

    private static final String DELETE_CONFIG_SUFFIX = ";burn";

    public static void main(String... args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("-deleteBucket".equals(a)) {
                deleteBucket(args[++i]);
            }
        }
    }


    public static AmazonS3Client openService(Properties prop) throws IOException, AmazonServiceException {
        AWSCredentials credentials = new BasicAWSCredentials(prop.getProperty(S3Constants.ACCESS_KEY),
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
     * Delete an S3 bucket.
     *
     * @param bucketName the bucket name
     */
    public static void deleteBucket(String bucketName) throws Exception {
        Properties prop = readConfig(DEFAULT_CONFIG_FILE);
        AmazonS3 s3service = openService(prop);
        ObjectListing prevObjectListing = s3service.listObjects(bucketName);
        while (true) {
            for (S3ObjectSummary s3ObjSumm : prevObjectListing.getObjectSummaries()) {
                s3service.deleteObject(bucketName, s3ObjSumm.getKey());
            }
            if (!prevObjectListing.isTruncated()) break;
            prevObjectListing = s3service.listNextBatchOfObjects(prevObjectListing);
        }
        s3service.deleteBucket(bucketName);
    }

    /**
     * Read a configuration properties file. If the file name ends with ";burn", the file is deleted after reading.
     *
     * @param fileName the properties file name
     * @return the properties
     * @throw IOException if the file doesn't exist
     */
    public static Properties readConfig(String fileName) throws IOException {
        boolean delete = false;
        if (fileName.endsWith(DELETE_CONFIG_SUFFIX)) {
            delete = true;
            fileName = fileName.substring(0, fileName.length() - DELETE_CONFIG_SUFFIX.length());
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

    static void deleteIfPossible(File file) {
        boolean deleted = file.delete();
        if (!deleted) {
            LOG.warn("Could not delete " + file.getAbsolutePath());
        }
    }

    public static void setLongMeta(S3Object obj, String meta, long x) {
        ObjectMetadata metadata = obj.getObjectMetadata();
        metadata.addUserMetadata(meta, String.valueOf(x));
        obj.setObjectMetadata(metadata);
    }

    public static long getLongMeta(S3Object obj, String meta) {
        return Long.parseLong(obj.getObjectMetadata().getUserMetadata().get(meta).toString());
    }
}
