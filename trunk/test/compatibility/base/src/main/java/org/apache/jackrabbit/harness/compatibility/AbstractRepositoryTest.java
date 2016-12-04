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
package org.apache.jackrabbit.harness.compatibility;

import static org.testng.AssertJUnit.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

public class AbstractRepositoryTest {

    protected void doCreateRepositories(String name) throws Exception {
        // Create a repository using the Jackrabbit default configuration
        doCreateRepository(
                name,
                RepositoryImpl.class.getResourceAsStream("repository.xml"));

        // Create repositories for any special configurations included
        File directory = new File(new File("src", "test"), "resources");
        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                String xml = file.getName();
                if (file.isFile() && xml.endsWith(".xml")) {
                    doCreateRepository(
                            name + "-" + xml.substring(0, xml.length() - 4),
                            FileUtils.openInputStream(file));
                }
            }
        }
    }

    /**
     * Creates a named test repository with the given configuration file.
     *
     * @param name name of the repository
     * @param xml input stream for reading the repository configuration
     * @throws Exception if the repository could not be created
     */
    protected void doCreateRepository(String name, InputStream xml)
            throws Exception {
        File directory = new File(new File("target", "repository"), name);
        File configuration = new File(directory, "repository.xml");

        // Copy the configuration file into the repository directory
        try {
            OutputStream output = FileUtils.openOutputStream(configuration);
            try {
                IOUtils.copy(xml, output);
            } finally {
                output.close();
            }
        } finally {
            xml.close();
        }

        // Create the repository
        try {
            RepositoryConfig config = RepositoryConfig.create(
                    configuration.getPath(), directory.getPath());
            RepositoryImpl repository = RepositoryImpl.create(config);
            try {
                Session session = repository.login(
                        new SimpleCredentials("admin", "admin".toCharArray()));
                try {
                    createTestData(session);
                } finally {
                    session.logout();
                }
            } finally {
                repository.shutdown();
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
            fail("Create repository " + name);
        }
    }

    protected void createTestData(Session session) throws Exception {
        JackrabbitNodeTypeManager manager = (JackrabbitNodeTypeManager)
            session.getWorkspace().getNodeTypeManager();
        String cnd =
            "<nt='http://www.jcp.org/jcr/nt/1.0'>\n"
            + "<mix='http://www.jcp.org/jcr/mix/1.0'>\n" 
            + "[nt:myversionable] > nt:unstructured, mix:versionable\n";
        manager.registerNodeTypes(
                new ByteArrayInputStream(cnd.getBytes("UTF-8")),
                JackrabbitNodeTypeManager.TEXT_X_JCR_CND);

        Node root = session.getRootNode();
        Node test = root.addNode("test", "nt:unstructured");
        root.save();

        Node versionable = createVersionable(test);
        createProperties(test, versionable);
        createLock(test);
        createUsers(session);
    }

    protected Node createVersionable(Node parent) throws RepositoryException {
        Node versionable = parent.addNode("versionable", "nt:myversionable");
        versionable.setProperty("foo", "A");
        parent.save();

        VersionHistory history = versionable.getVersionHistory();
        Version versionA = versionable.checkin();
        history.addVersionLabel(versionA.getName(), "labelA", false);
        versionable.checkout();
        versionable.setProperty("foo", "B");
        parent.save();
        Version versionB = versionable.checkin();
        history.addVersionLabel(versionB.getName(), "labelB", false);
        return versionable;
    }

    protected void createProperties(Node parent, Node reference)
            throws RepositoryException {
        Node properties = parent.addNode("properties", "nt:unstructured");
        properties.setProperty("boolean", true);
        properties.setProperty("double", 0.123456789);
        properties.setProperty("long", 1234567890);
        properties.setProperty("reference", reference);
        properties.setProperty("string", "test");

        properties.setProperty("multiple", new String[] { "a", "b", "c" });

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(1234567890);
        properties.setProperty("date", calendar);

        byte[] binary = new byte[100 * 1000];
        new Random(1234567890).nextBytes(binary);
        properties.setProperty("binary", new ByteArrayInputStream(binary));

        parent.save();
    }

    protected void createLock(Node parent) throws RepositoryException {
        Node lock = parent.addNode("lock", "nt:unstructured");
        lock.addMixin("mix:lockable");
        parent.save();

        lock.lock(true, false);
    }

    protected void createUsers(Session session) throws RepositoryException {
    }

}
