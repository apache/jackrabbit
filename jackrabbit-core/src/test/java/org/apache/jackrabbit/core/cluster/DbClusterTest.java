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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.JUnitTest;

/**
 * Tests clustering with a database.
 */
public class DbClusterTest extends JUnitTest {

    public void setUp() throws Exception {
        deleteAll();

        FileUtils.copyFile(
                new File("./src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml"),
                new File("./target/dbClusterTest/node1/repository.xml"));
        FileUtils.copyFile(
                new File("./src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml"),
                new File("./target/dbClusterTest/node2/repository.xml"));
    }

    public void tearDown() throws Exception {
        deleteAll();
    }

    private static void deleteAll() throws IOException {
        FileUtils.deleteDirectory(new File("./target/dbClusterTest"));
    }

    public void test() throws RepositoryException {
        RepositoryImpl rep1 = RepositoryImpl.create(RepositoryConfig.create(
                new File("./target/dbClusterTest/node1")));
        RepositoryImpl rep2 = RepositoryImpl.create(RepositoryConfig.create(
                new File("./target/dbClusterTest/node2")));
        Session s1 = rep1.login(new SimpleCredentials("admin", "admin".toCharArray()));
        Session s2 = rep2.login(new SimpleCredentials("admin", "admin".toCharArray()));

        s1.getRootNode().addNode("test1");
        s2.getRootNode().addNode("test2");
        s1.save();
        s2.save();
        s1.refresh(true);
        s2.refresh(true);

        s1.getRootNode().getNode("test2");
        s2.getRootNode().getNode("test1");
        rep1.shutdown();
        rep2.shutdown();
    }

}
