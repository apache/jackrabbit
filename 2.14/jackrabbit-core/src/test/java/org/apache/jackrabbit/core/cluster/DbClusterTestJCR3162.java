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
import org.apache.jackrabbit.test.JUnitTest;

/**
 * Test for JCR3162
 */
public class DbClusterTestJCR3162 extends JUnitTest {

    private static final SimpleCredentials ADMIN = new SimpleCredentials(
            "admin", "admin".toCharArray());

    private RepositoryImpl rep1;
    private RepositoryImpl rep2;

    private String clusterId1 = UUID.randomUUID().toString();
    private String clusterId2 = UUID.randomUUID().toString();

    private String prevClusterId;

    public void setUp() throws Exception {
        deleteAll();
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

        prevClusterId = System.setProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID, clusterId1);
        rep1 = RepositoryImpl.create(RepositoryConfig.create(new File(
                "./target/dbClusterTest/node1")));

        System.setProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID, clusterId2);

    }

    public void tearDown() throws Exception {
        // revert change to system property
        if (prevClusterId == null) {
            System.clearProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID);
        }
        else {
            System.setProperty(ClusterNode.SYSTEM_PROPERTY_NODE_ID, prevClusterId);
        }

        try {
            rep1.shutdown();
            if (rep2 != null) {
                rep2.shutdown();
            }
        } finally {
            deleteAll();
        }
    }

    private static void deleteAll() throws IOException {
        FileUtils.deleteDirectory(new File("./target/dbClusterTest"));
    }

    public void test() throws RepositoryException {
        int count = 5;

        // 1. create
        Session s1 = rep1.login(ADMIN);
        Node n = s1.getRootNode().addNode(
                "test-cluster-" + System.currentTimeMillis(),
                JcrConstants.NT_UNSTRUCTURED);
        n.addMixin(JcrConstants.MIX_VERSIONABLE);
        for (int i = 0; i < count; i++) {
            Node c = n.addNode("child_" + i);
            c.addMixin(JcrConstants.MIX_VERSIONABLE);
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
        checkConsistency(s2, "/", s2.getRootNode().getNodes().getSize());
    }

    private void resetJournalRev() {
        Connection con = null;
        try {
            con = DriverManager.getConnection(
                    "jdbc:h2:./target/dbClusterTest/db", "sa", "sa");
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

    private void checkConsistency(Session s, String path, long nodes)
            throws RepositoryException {

        s.refresh(true);
        Node n = s.getNode(path);

        RowIterator result = s
                .getWorkspace()
                .getQueryManager()
                .createQuery(
                        "SELECT * FROM [" + JcrConstants.NT_BASE
                                + "] as NODE WHERE ischildnode(NODE, ['"
                                + n.getPath() + "'])", Query.JCR_SQL2)
                .execute().getRows();

        int foundViaQuery = 0;
        while (result.hasNext()) {
            result.next();
            foundViaQuery++;
        }

        StringBuilder err = new StringBuilder("Path " + n.getPath() + ": ");
        for (Node c : JcrUtils.getChildNodes(n)) {
            err.append("(");
            err.append(c.getPath());
            err.append("|");
            err.append(c.getPrimaryNodeType().getName());
            err.append("),");
        }
        Assert.assertEquals(err.toString(), nodes, foundViaQuery);

        for (Node c : JcrUtils.getChildNodes(n)) {
            checkConsistency(s, c.getPath(), c.getNodes().getSize());
        }
    }
}
