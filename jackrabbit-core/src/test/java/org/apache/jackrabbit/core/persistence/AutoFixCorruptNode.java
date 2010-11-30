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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.TransientRepository;

/**
 * Tests that a corrupt node is automatically fixed.
 */
public class AutoFixCorruptNode extends TestCase {

    private final String TEST_DIR = "target/temp/" + getClass().getSimpleName();

    public void setUp() throws Exception {
        FileUtils.deleteDirectory(new File(TEST_DIR));
    }

    public void tearDown() throws Exception {
        setUp();
    }

    public void testAutoFix() throws Exception {

        // new repository
        TransientRepository rep = new TransientRepository(new File(TEST_DIR));
        Session s = openSession(rep, false);
        Node root = s.getRootNode();

        // add nodes /test and /test/missing
        Node test = root.addNode("test");
        Node missing = test.addNode("missing");
        missing.addMixin("mix:referenceable");
        UUID id = UUID.fromString(missing.getIdentifier());
        s.save();
        s.logout();

        // remove the bundle for /test/missing directly in the database
        Connection conn = DriverManager.getConnection(
                "jdbc:derby:"+TEST_DIR+"/workspaces/default/db");
        PreparedStatement prep = conn.prepareStatement(
                "delete from DEFAULT_BUNDLE  where NODE_ID_HI=? and NODE_ID_LO=?");
        prep.setLong(1, id.getMostSignificantBits());
        prep.setLong(2, id.getLeastSignificantBits());
        prep.executeUpdate();
        conn.close();

        // login and try the operation
        s = openSession(rep, false);
        test = s.getRootNode().getNode("test");

        // try to add a node with the same name
        try {
            test.addNode("missing");
            s.save();
        } catch (RepositoryException e) {
            // expected
        }

        s.logout();

        s = openSession(rep, true);
        test = s.getRootNode().getNode("test");
        // iterate over all child nodes fixes the corruption
        NodeIterator it = test.getNodes();
        while (it.hasNext()) {
            it.nextNode();
        }

        // try to add a node with the same name
        test.addNode("missing");
        s.save();

        // try to delete the parent node
        test.remove();
        s.save();

        s.logout();
        rep.shutdown();

        FileUtils.deleteDirectory(new File("repository"));

    }

    private Session openSession(Repository rep, boolean autoFix) throws RepositoryException {
        SimpleCredentials cred = new SimpleCredentials("admin", "admin".toCharArray());
        if (autoFix) {
            cred.setAttribute("org.apache.jackrabbit.autoFixCorruptions", "true");
        }
        return rep.login(cred);
    }

}
