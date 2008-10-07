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
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.config.BeanConfig;
import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.core.config.JournalConfig;
import org.apache.jackrabbit.core.journal.MemoryJournal;
import org.apache.jackrabbit.test.JUnitTest;

/**
 * Test cases for cluster mode.
 */
public class ClusterTest extends JUnitTest {

    /**
     * Repository home value.
     */
    private static final String REPOSITORY_HOME = "target/repository_for_test";

    /**
     * Repository home.
     */
    private File repositoryHome;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        repositoryHome = new File(REPOSITORY_HOME);
        repositoryHome.mkdirs();
        FileUtils.cleanDirectory(repositoryHome);

        super.setUp();
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        if (repositoryHome != null) {
            FileUtils.deleteDirectory(repositoryHome);
        }
        super.tearDown();
    }

    /**
     * Create a cluster with no configured id. Verify that the generated id
     * will be persisted.
     *
     * @throws Exception
     */
    public void testClusterIdGeneration() throws Exception {
        BeanConfig bc = new BeanConfig(MemoryJournal.class.getName(), new Properties());
        JournalConfig jc = new JournalConfig(bc);
        ClusterConfig cc = new ClusterConfig(null, 0, jc);
        SimpleClusterContext context = new SimpleClusterContext(cc, repositoryHome);

        ClusterNode clusterNode = new ClusterNode();
        clusterNode.init(context);

        String id = ((MemoryJournal) clusterNode.getJournal()).getId();

        clusterNode = new ClusterNode();
        clusterNode.init(context);

        assertEquals(id, ((MemoryJournal) clusterNode.getJournal()).getId());
    }
}
