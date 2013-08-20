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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.jackrabbit.aws.ext.S3Constants;
import org.apache.jackrabbit.aws.ext.Utils;
import org.apache.jackrabbit.aws.ext.ds.CachingDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * Test {@link CachingDataStore} with S3Backend and local cache on. It requires to pass aws config file via system property. For e.g.
 * -Dconfig=/opt/cq/aws.properties. Sample aws properties located at src/test/resources/aws.properties
 */
public class TestS3Ds extends TestCaseBase {

    protected static final Logger LOG = LoggerFactory.getLogger(TestS3Ds.class);

    /**
     * @inheritDoc
     */
    protected String config = System.getProperty(CONFIG);

    /**
     * @inheritDoc
     */
    protected boolean memoryBackend = false;

    /**
     * @inheritDoc
     */
    protected boolean noCache = false;

    protected void setUp() {
        super.setUp();
    }

    protected void tearDown() throws IOException {
        super.tearDown();
        deleteBucket();
    }

    /**
     * Cleaning of bucket after test run.
     */
    public void deleteBucket() throws IOException {

        String config = System.getProperty(CONFIG);
        Properties props = Utils.readConfig(config);
        AmazonS3Client s3service = Utils.openService(props);
        String bucket = props.getProperty(S3Constants.S3_BUCKET);
        if (s3service.doesBucketExist(bucket)) {
            ObjectListing prevObjectListing = s3service.listObjects(bucket);
            while (true) {
                List<DeleteObjectsRequest.KeyVersion> deleteList = new ArrayList<DeleteObjectsRequest.KeyVersion>();
                for (S3ObjectSummary s3ObjSumm : prevObjectListing.getObjectSummaries()) {
                    LOG.info("add id :" + s3ObjSumm.getKey() + " to delete lists");
                    deleteList.add(new DeleteObjectsRequest.KeyVersion(s3ObjSumm.getKey()));
                }
                if (deleteList.size() > 0) {
                    DeleteObjectsRequest delObjsReq = new DeleteObjectsRequest(bucket);
                    delObjsReq.setKeys(deleteList);
                    DeleteObjectsResult dobjs = s3service.deleteObjects(delObjsReq);
                }
                if (!prevObjectListing.isTruncated()) break;
                prevObjectListing = s3service.listNextBatchOfObjects(prevObjectListing);
            }
            s3service.deleteBucket(bucket);
        }
        s3service.shutdown();

    }

}
