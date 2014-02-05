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
import java.util.Properties;

import org.apache.jackrabbit.aws.ext.Utils;
import org.apache.jackrabbit.core.data.CachingDataStore;

/**
 * Test {@link CachingDataStore} with S3Backend and local cache on. It requires
 * to pass aws config file via system property. For e.g.
 * -Dconfig=/opt/cq/aws.properties. Sample aws properties located at
 * src/test/resources/aws.properties
 */
public class TestS3Ds extends TestCaseBase {

    public TestS3Ds() {
        config = System.getProperty(CONFIG);
        memoryBackend = false;
        noCache = false;
    }


    @Override
    protected void tearDown() throws IOException {
        super.tearDown();
        Properties props = Utils.readConfig(config);
        Utils.deleteBucket(props);
    }

}
