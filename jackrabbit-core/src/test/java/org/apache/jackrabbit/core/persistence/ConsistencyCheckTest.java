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
package org.apache.jackrabbit.core.persistence;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.version.Version;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.JUnitTest;
import org.apache.jackrabbit.test.config.PersistenceManagerConf;
import org.apache.jackrabbit.test.config.RepositoryConf;
import org.apache.jackrabbit.uuid.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ConsistencyCheckTest</code> tests the PersistenceManager's consistencyCheck feature.
 *
 * For analysis, run this test with log4j enabled (see below).
 *
 */
public class ConsistencyCheckTest extends TestCase {
    
    private static Logger log = LoggerFactory.getLogger(JUnitTest.class);
    
    //-------------------------------------------------------------------< generic utility methods >
    
    public static File createTempDir(String prefix, String suffix) {
        try {
            File dir = File.createTempFile(prefix, suffix);
            dir.delete();
            dir.mkdir();
            
            return dir;
        } catch (IOException e) {
            fail("Cannot create temp directory for test: " + e);
            return null;
        }
    }

    public static void delete(File file) {
        File[] files = file.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            delete(files[i]);
        }
        file.delete();
    }
    
    private static String getUUID(Node node) {
        return ((NodeImpl) node).getNodeId().toString();
    }
    
    private static int indentCount = 0;
    
    private static void displayTree(Node node) throws RepositoryException {
        StringBuffer indent = new StringBuffer();
        for (int i=0; i < indentCount; i++) {
            indent.append("    ");
        }
        log.debug(indent + node.getName() + " [" + getUUID(node) + "]");
        NodeIterator nodes = node.getNodes();
        indentCount++;
        while (nodes.hasNext()) {
            displayTree(nodes.nextNode());
        }
        indentCount--;
    }

    //-------------------------------------------------------------------< repository setup >
    
    private final static File DIRECTORY = createTempDir("jackrabbit", "test");
    
    private final static String PROTOCOL = "jdbc:derby:";
    
    private final static String DB_PATH = "/db/itemState";
    
    private final static String VERSIONING_DB_PATH = "/version/db/itemState";
    
    private static AdminRepositoryImpl repository = createRepository();
    
    private static AdminRepositoryImpl createRepository() {
        log.debug("Start repo at '" + DIRECTORY.getPath() + "'...");
        
        RepositoryConf conf = new RepositoryConf();
        // ensure correct derby config
        PersistenceManagerConf pmc = conf.getWorkspaceConfTemplate().getPersistenceManagerConf();
        pmc.setParameter("url", PROTOCOL + "${wsp.home}" + DB_PATH + ";create=true");
        
        pmc = conf.getVersioningConf().getPersistenceManagerConf();
        pmc.setParameter("url", PROTOCOL + "${rep.home}" + VERSIONING_DB_PATH + ";create=true");
        pmc.setParameter("schemaObjectPrefix", "VERSION_");
        try {
            RepositoryConfig config = conf.createConfig(DIRECTORY.getPath());
            config.init();
            return new AdminRepositoryImpl(config);
        } catch (Exception e) {
            fail("Could not create repository for test: " + e);
            return null;
        }
    }
    
    private Session login(String workspace) throws RepositoryException {
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()), workspace);
    }
    
    private static void deleteRepository() {
        log.debug("Shutdown repo...");
        repository.shutdown();
        delete(DIRECTORY);
    }
    
    /**
     * <code>AdminRepositoryImpl</code> makes <code>checkConsistency()</code> method
     * in <code>RepositoryImpl</code> accessible.
     */
    private static class AdminRepositoryImpl extends RepositoryImpl {
        
        public AdminRepositoryImpl(RepositoryConfig config) throws RepositoryException {
            super(config);
        }

        // make protected method in RepositoryImpl public
        public void checkConsistency(String workspaceName, String[] uuids,
                boolean recursive, boolean fix) throws IllegalStateException,
                NoSuchWorkspaceException, RepositoryException {
            super.checkConsistency(workspaceName, uuids, recursive, fix);
        }
    }
    
    //-------------------------------------------------------------------< helper methods >
    
    private Connection getDerbyConnection(String workspace) throws SQLException {
        Connection conn = null;
        Properties props = new Properties();
        props.put("user", "");
        props.put("password", "");
        
        if (JCR_SYSTEM.equals(workspace)) {
            conn = DriverManager.getConnection(PROTOCOL + DIRECTORY.getPath() + VERSIONING_DB_PATH, props);
        } else {
            String basePath = DIRECTORY.getPath() + File.separatorChar + "workspaces" + File.separatorChar;
            conn = DriverManager.getConnection(PROTOCOL + basePath + workspace + DB_PATH, props);
        }

        conn.setAutoCommit(false);
        return conn;
    }
    
    private void setUUIDParams(PreparedStatement s, int startIndex, UUID uuid) throws SQLException {
        s.setLong(startIndex, uuid.getMostSignificantBits());
        s.setLong(startIndex+1, uuid.getLeastSignificantBits());
    }
    
    private void deleteNodeBundle(Connection conn, String workspace, String uuid) throws SQLException {
        PreparedStatement s = conn.prepareStatement(
                "delete from " + workspace.toUpperCase() + "_BUNDLE where NODE_ID_HI = ? and NODE_ID_LO = ?");
        setUUIDParams(s, 1, new UUID(uuid));
        s.execute();
    }
    
    private byte[] readNodeBundleBlob(Connection conn, String workspace, String uuid) throws SQLException {
        PreparedStatement s = conn.prepareStatement(
                "select BUNDLE_DATA from " + workspace.toUpperCase() + "_BUNDLE where NODE_ID_HI = ? and NODE_ID_LO = ?");
        setUUIDParams(s, 1, new UUID(uuid));
        ResultSet rs = s.executeQuery();
        if (rs.next()) {
            return rs.getBytes(1);
        }
        return null;
    }
    
    private void writeNodeBundleBlob(Connection conn, String workspace, String uuid, byte[] bytes) throws SQLException {
        PreparedStatement s = conn.prepareStatement(
                "update " + workspace.toUpperCase() + "_BUNDLE set BUNDLE_DATA = ? where NODE_ID_HI = ? and NODE_ID_LO = ?");
        s.setBytes(1, bytes);
        setUUIDParams(s, 2, new UUID(uuid));
        s.execute();
    }
    
    //-------------------------------------------------------------------< test data setup >
    
    private final static String WORKSPACE = "default";
    
    private static final String JCR_SYSTEM = "jcr:system";

    private static String testNodeUUID;
    
    private static String missingChildUUID;
    
    private static String missingGrandChildUUID;
    
    private static String missingParentUUID;

    private static String clearedNodeUUID;
    
    private static String brokenNodeUUID;
    
    private void setupWorkspaceTestData() throws Exception {
        Session session = login(WORKSPACE);
        
        // prepare some parent-child relationships
        
        // nested tree where children will get lost
        Node fruits = session.getRootNode().addNode("fruits");
        Node apple = fruits.addNode("apple");
        Node bananas = fruits.addNode("bananas");
        Node melons = fruits.addNode("melons");
        melons.addNode("bittermelon");
        Node watermelon = melons.addNode("watermelon");
        Node honeydew = melons.addNode("honeydew");
        
        // separate tree where the parent will get lost
        Node vegetables = session.getRootNode().addNode("vegetables");
        vegetables.addNode("cucumber");
        
        testNodeUUID = getUUID(fruits);
        missingChildUUID = getUUID(bananas);
        missingGrandChildUUID = getUUID(watermelon);
        missingParentUUID = getUUID(vegetables);
        clearedNodeUUID = getUUID(honeydew);
        brokenNodeUUID = getUUID(apple);
        
        session.save();
        
        Connection conn = getDerbyConnection(WORKSPACE);
        
        // now delete/modify some bundles directly in the database
        log.debug("delete parent      " + missingParentUUID);
        deleteNodeBundle(conn, WORKSPACE, missingParentUUID);
        
        log.debug("delete child       " + missingChildUUID);
        deleteNodeBundle(conn, WORKSPACE, missingChildUUID);
        log.debug("delete grand child " + missingGrandChildUUID);
        deleteNodeBundle(conn, WORKSPACE, missingGrandChildUUID);
        
        log.debug("clear node         " + clearedNodeUUID);
        // makes the bundle invalid, ie. BundleBinding.checkBundle() will throw an exception
        writeNodeBundleBlob(conn, WORKSPACE, clearedNodeUUID, new byte[10]);
        
        log.debug("break node         " + brokenNodeUUID);
        byte[] bytes = readNodeBundleBlob(conn, WORKSPACE, brokenNodeUUID);
        // makes the bundle invalid, ie. BundleBinding.checkBundle() will return false (but won't throw an exception)
        bytes[25] = 1;
        writeNodeBundleBlob(conn, WORKSPACE, brokenNodeUUID, bytes);
        
        conn.commit();
        conn.close();
    }
    
    
    private static String brokenVersion;
    
    private void setupVersioningTestData() throws Exception {
        Session session = login(WORKSPACE);
        // create a new node + first version
        Node node = session.getRootNode().addNode("food");
        node.addMixin("mix:versionable");
        node.addNode("values").setProperty("kcal", 300);
        session.save();
        node.checkin();
        
        // create a second version
        node.checkout();
        node.getNode("values").setProperty("kcal", 1234);
        session.save();
        node.checkin();

        // create a third version
        node.checkout();
        node.getNode("values").setProperty("kcal", 5000);
        session.save();
        node.checkin();
        
        //displayTree(session.getRootNode().getNode("jcr:system/jcr:versionStorage"));
        
        brokenVersion = "1.1";
        Version v = node.getVersionHistory().getVersion(brokenVersion);
        String missingNodeUUID = getUUID(v.getNode("jcr:frozenNode").getNode("values"));
        
        Connection conn = getDerbyConnection(JCR_SYSTEM);
        
        log.debug("delete node       " + missingNodeUUID);
        deleteNodeBundle(conn, "version", missingNodeUUID);
        
        conn.commit();
        conn.close();
    }
    
    private void assertRepositoryException(Node rootNode, String path) {
        try {
            rootNode.getNode(path);
        } catch (PathNotFoundException e) {
            fail("JCR-1428: no need for a consistencyFix then...");
        } catch (RepositoryException e) {
            // expected
        }
    }
    
    private void assertPathNotFoundException(Node rootNode, String path) {
        try {
            rootNode.getNode(path);
        } catch (PathNotFoundException e) {
            // expewcted
        } catch (RepositoryException e) {
            log.error("JCR-1428: fix in consistencyCheck did not work", e);
            fail("JCR-1428: fix in consistencyCheck did not work: " + e);
        }
    }
    
    //-------------------------------------------------------------------< tests >
    
    /**
     * To verify the check, one has to manually look at the error log
     * therefore run this test with these log4j settings:
     *
     * for looking at bundle errors only:
     * log4j.rootLogger=ERROR, file
     * log4j.logger.org.apache.jackrabbit.core=ERROR, stdout
     * log4j.logger.org.apache.jackrabbit.test=DEBUG
     *
     * for full details on checked bundles, add this:
     * log4j.logger.org.apache.jackrabbit.core.persistence.bundle.util.BundleBinding=INFO
     *
     * for details on fixing, add this:
     * log4j.logger.org.apache.jackrabbit.core.persistence.bundle.BundleDbPersistenceManager=INFO
     *
     */
    public void testConsistencyCheck() throws Exception {
        // 1. test, init data
        setupWorkspaceTestData();
        
        log.debug("================================================ Checking workspace " + WORKSPACE + "...");
        repository.checkConsistency(WORKSPACE, null, false, false);

        log.debug("================================================ Checking workspace " + WORKSPACE + ", single node...");
        repository.checkConsistency(WORKSPACE, new String[] { testNodeUUID, missingParentUUID }, false, false);

        log.debug("================================================ Checking workspace " + WORKSPACE + ", single node, recursively...");
        repository.checkConsistency(WORKSPACE, new String[] { testNodeUUID }, true, false);
    }
    
    public void testConsistencyFix() throws Exception {
        // ensure all item state caches are empty
        repository.shutdown();
        repository = createRepository();
        
        Session session;

        // tests only the erroneous behaviour when bundles are missing
        session = login(WORKSPACE);
        assertRepositoryException(session.getRootNode(), "fruits/bananas");
        assertRepositoryException(session.getRootNode(), "fruits/melons/watermelon");
        
        log.debug("================================================ Checking and fixing workspace " + WORKSPACE + "...");
        repository.checkConsistency(WORKSPACE, null, false, true);

        // check the log here, it should no longer include any "Fixing bundle..." messages
        log.debug("================================================ Rechecking workspace " + WORKSPACE + "...");
        repository.checkConsistency(WORKSPACE, null, false, false);
        
        repository.shutdown();
        repository = createRepository();
        
        session = login(WORKSPACE);
        assertPathNotFoundException(session.getRootNode(), "fruits/bananas");
        assertPathNotFoundException(session.getRootNode(), "fruits/melons/watermelon");
    }
    
    public void testConsistencyCheckVersioning() throws Exception {
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> versioning storage...");
        // 1. versioning test, setup data
        setupVersioningTestData();
        
        log.debug("================================================ Checking " + JCR_SYSTEM + "...");
        repository.checkConsistency(JCR_SYSTEM, null, false, false);
    }
    
    public void testConsistencyFixVersioning() throws Exception {
        // ensure all item state caches are empty
        repository.shutdown();
        repository = createRepository();
        
        Session session = login(WORKSPACE);
        
        // for debugging
        //displayTree(session.getRootNode().getNode("jcr:system/jcr:versionStorage"));
        
        Node node = session.getRootNode().getNode("food");
        assertRepositoryException(node.getVersionHistory().getVersion(brokenVersion), "jcr:frozenNode/values");
        
        log.debug("================================================ Checking and fixing " + JCR_SYSTEM + "...");
        repository.checkConsistency(JCR_SYSTEM, null, false, true);

        // check the log here, it should no longer include any "Fixing bundle..." messages
        log.debug("================================================ Rechecking " + JCR_SYSTEM + "...");
        repository.checkConsistency(JCR_SYSTEM, null, false, false);
        
        repository.shutdown();
        repository = createRepository();
        
        session = login(WORKSPACE);
        node = session.getRootNode().getNode("food");
        assertPathNotFoundException(node.getVersionHistory().getVersion(brokenVersion), "jcr:frozenNode/values");
    }
    
    public void testRepositoryShutdown() {
        // last test, stop (not necessarily needed, but cleans up file system)
        deleteRepository();
    }
}
