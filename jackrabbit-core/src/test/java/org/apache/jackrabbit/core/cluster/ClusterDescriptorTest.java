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
package org.apache.jackrabbit.core.cluster;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.JUnitTest;

/**
 * Tests clustering with a database.
 */
public class ClusterDescriptorTest extends JUnitTest {

    private RepositoryImpl rep1, rep2;

    public void setUp() throws Exception {
        deleteAll();
        FileUtils.copyFile(
                new File("./src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml"),
                new File("./target/descriptorClusterTest/node1/repository.xml"));
        FileUtils.copyFile(
                new File("./src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml"),
                new File("./target/descriptorClusterTest/node2/repository.xml"));

        rep1 = RepositoryImpl.create(RepositoryConfig.create(
                new File("./target/descriptorClusterTest/node1")));
        rep2 = RepositoryImpl.create(RepositoryConfig.create(
                new File("./target/descriptorClusterTest/node2")));
    }

    public void tearDown() throws Exception {
    		rep1.shutdown();
	    rep2.shutdown();
        deleteAll();
    }

    private static void deleteAll() throws IOException {
        FileUtils.deleteDirectory(new File("./target/descriptorClusterTest"));
    }

    public void testRepositoryDescriptor() {
        String clusterId1 =  rep1.getDescriptor(RepositoryImpl.JACKRABBIT_CLUSTER_ID);
        String clusterId2 =  rep2.getDescriptor(RepositoryImpl.JACKRABBIT_CLUSTER_ID);

        assertNotNull("Cluster descriptor not set for cluster node 1", clusterId1);
        assertNotNull("Cluster descriptor not set for cluster node 2", clusterId2);

        assertFalse("Cluster ids should be unique", clusterId1.equals(clusterId2));
    }

}
