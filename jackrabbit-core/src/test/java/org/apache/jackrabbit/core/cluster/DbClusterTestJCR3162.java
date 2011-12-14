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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.RowIterator;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for JCR3162
 */
public class DbClusterTestJCR3162 {

    private static final SimpleCredentials ADMIN = new SimpleCredentials(
            "admin", "admin".toCharArray());

    private Server server1;
    private Server server2;

    private RepositoryImpl rep1;
    private RepositoryImpl rep2;

    private String clusterId1 = UUID.randomUUID().toString();
    private String clusterId2 = UUID.randomUUID().toString();

    @Before
    public void setUp() throws Exception {
        deleteAll();
        server1 = Server.createTcpServer("-tcpPort", "9001", "-baseDir",
                "./target/dbClusterTest/db1", "-tcpAllowOthers").start();
        server2 = Server.createTcpServer("-tcpPort", "9002", "-baseDir",
                "./target/dbClusterTest/db2", "-tcpAllowOthers").start();
        FileUtils
                .copyFile(
                        new File(
                                "./src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml"),
                        new File("./target/dbClusterTest/node1/repository.xml"));
        FileUtils
                .copyFile(
                        new File(
                                "./src/test/resources/org/apache/jackrabbit/core/cluster/repository-h2.xml"),
                        new File("./target/dbClusterTest/node2/repository.xml"));

        System.setProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID, clusterId1);
        rep1 = RepositoryImpl.create(RepositoryConfig.create(new File(
                "./target/dbClusterTest/node1")));

        System.setProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID, clusterId2);

    }

    @After
    public void tearDown() throws Exception {
        try {
            rep1.shutdown();
            if (rep2 != null) {
                rep2.shutdown();
            }
        } finally {
            server1.stop();
            server2.stop();
            deleteAll();
        }
    }

    private void deleteAll() throws IOException {
        FileUtils.deleteDirectory(new File("./target/dbClusterTest"));
    }

    @Test
    public void test() throws RepositoryException {
        int count = 5;

        // 1. create
        Session s1 = rep1.login(ADMIN);
        Node n = s1.getRootNode().addNode(
                "test-cluster-" + System.currentTimeMillis(),
                JcrConstants.NT_UNSTRUCTURED);
        for (int i = 0; i < count; i++) {
            n.addNode("child_" + i);
        }
        s1.save();

        // 2. rollback journal revision
        resetJournalRev();

        // 3. sync & verify
        // rep1.shutdown();

        // start #2 with an empty search index
        rep2 = RepositoryImpl.create(RepositoryConfig.create(new File(
                "./target/dbClusterTest/node2")));

        // verify
        Session s2 = rep2.login(ADMIN);
        checkConsistency(s2, n.getPath(), count);
    }

    private void resetJournalRev() {
        Connection con = null;
        try {
            con = DriverManager.getConnection(
                    "jdbc:h2:tcp://localhost:9001,localhost:9002/db", "sa",
                    "sa");
            PreparedStatement prep = con
                    .prepareStatement("update JOURNAL_LOCAL_REVISIONS set REVISION_ID=0 where JOURNAL_ID=?");
            prep.setString(1, clusterId2);
            prep.executeUpdate();
            prep.close();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Unable to reset revision to 0. " + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
        }
    }

    private void checkConsistency(Session s, String path, int nodes)
            throws RepositoryException {

        s.refresh(true);
        Node n = s.getNode(path);
        Assert.assertNotNull(n);

        int found = 0;
        for (Node c : JcrUtils.getChildNodes(n)) {
            found++;
        }
        Assert.assertEquals(nodes, found);

        RowIterator result = s
                .getWorkspace()
                .getQueryManager()
                .createQuery(
                        "SELECT * FROM [" + JcrConstants.NT_UNSTRUCTURED
                                + "] as NODE WHERE ischildnode(NODE, [" + path
                                + "])", Query.JCR_SQL2).execute().getRows();

        int foundViaQuery = 0;
        while (result.hasNext()) {
            result.next();
            foundViaQuery++;
        }
        Assert.assertEquals(nodes, foundViaQuery);
    }
}
