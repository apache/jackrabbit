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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.aws.ext.Utils;
import org.apache.jackrabbit.core.data.Backend;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.TestCaseBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;

/**
 * Test {@link CachingDataStore} with S3Backend and local cache on. It requires
 * to pass aws config file via system property. For e.g.
 * -Dconfig=/opt/cq/aws.properties. Sample aws properties located at
 * src/test/resources/aws.properties
 */
public class TestS3Ds extends TestCaseBase {

    protected static final Logger LOG = LoggerFactory.getLogger(TestS3Ds.class);

    private Date startTime = null;

    public TestS3Ds() {
      config = System.getProperty(CONFIG);
      memoryBackend = false;
      noCache = false;
  }

    protected void setUp() throws Exception {
        startTime = new Date();
        super.setUp();
    }
    protected void tearDown() throws Exception {
        deleteBucket();
        super.tearDown();
    }
    
    protected CachingDataStore createDataStore() throws RepositoryException {
        ds = new S3TestDataStore(String.valueOf(randomGen.nextInt(9999)) + "-test");
        ds.setConfig(config);
        if (noCache) {
            ds.setCacheSize(0);
        }
        ds.init(dataStoreDir);
        return ds;
    }

    /**
     * Cleaning of bucket after test run.
     */
    /**
     * Cleaning of bucket after test run.
     */
    public void deleteBucket() throws Exception {
        Properties props = Utils.readConfig(config);
        AmazonS3Client s3service = Utils.openService(props);
        Backend backend = ds.getBackend();
        String bucket = ((S3Backend)backend).getBucket();
        LOG.info("delete bucket [" + bucket + "]");
        TransferManager tmx = new TransferManager(s3service);
        if (s3service.doesBucketExist(bucket)) {
            for (int i = 0; i < 3; i++) {
                tmx.abortMultipartUploads(bucket, startTime);
                ObjectListing prevObjectListing = s3service.listObjects(bucket);
                while (prevObjectListing != null ) {
                    List<DeleteObjectsRequest.KeyVersion> deleteList = new ArrayList<DeleteObjectsRequest.KeyVersion>();
                    for (S3ObjectSummary s3ObjSumm : prevObjectListing.getObjectSummaries()) {
                        deleteList.add(new DeleteObjectsRequest.KeyVersion(s3ObjSumm.getKey()));
                    }
                    if (deleteList.size() > 0) {
                        DeleteObjectsRequest delObjsReq = new DeleteObjectsRequest(bucket);
                        delObjsReq.setKeys(deleteList);
                        s3service.deleteObjects(delObjsReq);
                    }
                    if (!prevObjectListing.isTruncated()) break;
                    prevObjectListing = s3service.listNextBatchOfObjects(prevObjectListing);
                }
            }
            s3service.deleteBucket(bucket);
            LOG.info("bucket: " + bucket + " deleted");
            tmx.shutdownNow();
            s3service.shutdown();
        }
    }

}
