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
package org.apache.jackrabbit.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.test.RepositoryStubException;

/**
 * RepositoryStub implementation for Apache Jackrabbit.
 *
 * @since Apache Jackrabbit 1.6
 */
public class JackrabbitRepositoryStub extends RepositoryStub {

    /**
     * Property for the repository configuration file. Defaults to
     * &lt;repository home&gt;/repository.xml if not specified.
     */
    public static final String PROP_REPOSITORY_CONFIG =
        "org.apache.jackrabbit.repository.config";

    /**
     * Property for the repository home directory. Defaults to
     * target/repository for convenience in Maven builds.
     */
    public static final String PROP_REPOSITORY_HOME =
        "org.apache.jackrabbit.repository.home";

    /**
     * The encoding of the test resources.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Repository settings.
     */
    private final Properties settings;

    /**
     * The repository instance.
     */
    private Repository repository;

    private static Properties getStaticProperties() {
        Properties properties = new Properties();
        try {
            InputStream stream =
                getResource("JackrabbitRepositoryStub.properties");
            try {
                properties.load(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            // TODO: Log warning
        }
        return properties;
    }

    private static InputStream getResource(String name) {
        return JackrabbitRepositoryStub.class.getResourceAsStream(name);
    }

    /**
     * Constructor as required by the JCR TCK.
     *
     * @param settings repository settings
     */
    public JackrabbitRepositoryStub(Properties settings) {
        super(getStaticProperties());
        // set some attributes on the sessions
        superuser.setAttribute("jackrabbit", "jackrabbit");
        readwrite.setAttribute("jackrabbit", "jackrabbit");
        readonly.setAttribute("jackrabbit", "jackrabbit");

        // Repository settings
        this.settings = settings;
    }

    /**
     * Returns the configured repository instance.
     *
     * @return the configured repository instance.
     * @throws RepositoryStubException if an error occurs while
     *                                 obtaining the repository instance.
     */
    public synchronized Repository getRepository()
            throws RepositoryStubException {
        if (repository == null) {
            try {
                String dir = settings.getProperty(PROP_REPOSITORY_HOME);
                if (dir == null) {
                    dir = new File("target", "repository").getPath();
                }

                new File(dir).mkdirs();

                String xml = settings.getProperty(PROP_REPOSITORY_CONFIG);
                if (xml == null) {
                    xml = new File(dir, "repository.xml").getPath();
                }

                if (!new File(xml).exists()) {
                    InputStream input = getResource("repository.xml");
                    try {
                        OutputStream output = new FileOutputStream(xml);
                        try {
                            IOUtils.copy(input, output);
                        } finally {
                            output.close();
                        }
                    } finally {
                        input.close();
                    }
                }

                RepositoryConfig config = RepositoryConfig.create(xml, dir);
                repository = RepositoryImpl.create(config);

                Session session = repository.login(superuser);
                try {
                    prepareTestContent(session);
                } finally {
                    session.logout();
                }
            } catch (Exception e) {
                RepositoryStubException exception =
                    new RepositoryStubException("Failed to start repository");
                exception.initCause(e);
                throw exception;
            }
        }
        return repository;
    }

    private void prepareTestContent(Session session)
            throws RepositoryException, IOException {
        JackrabbitWorkspace workspace =
            (JackrabbitWorkspace) session.getWorkspace();
        Set workspaces = new HashSet(
                Arrays.asList(workspace.getAccessibleWorkspaceNames()));
        if (!workspaces.contains("test")) {
            workspace.createWorkspace("test");
        }

        JackrabbitNodeTypeManager manager =
            (JackrabbitNodeTypeManager) workspace.getNodeTypeManager();
        if (!manager.hasNodeType("test:versionable")) {
            InputStream xml = getResource("test-nodetypes.xml");
            try {
                manager.registerNodeTypes(xml, JackrabbitNodeTypeManager.TEXT_XML);
            } finally {
                xml.close();
            }
        }

        Node data = getOrAddNode(session.getRootNode(), "testdata");
        addPropertyTestData(getOrAddNode(data, "property"));
        addQueryTestData(getOrAddNode(data, "query"));
        addNodeTestData(getOrAddNode(data, "node"));
        addExportTestData(getOrAddNode(data, "docViewTest"));
        session.save();
    }

    private Node getOrAddNode(Node node, String name)
            throws RepositoryException {
        try {
            return node.getNode(name);
        } catch (PathNotFoundException e) {
            return node.addNode(name);
        }
    }

    /**
     * Creates a boolean, double, long, calendar and a path property at the
     * given node.
     */
    private void addPropertyTestData(Node node) throws RepositoryException {
        node.setProperty("boolean", true);
        node.setProperty("double", Math.PI);
        node.setProperty("long", 90834953485278298l);
        Calendar c = Calendar.getInstance();
        c.set(2005, 6, 18, 17, 30);
        node.setProperty("calendar", c);
        ValueFactory factory = node.getSession().getValueFactory();
        node.setProperty("path", factory.createValue("/", PropertyType.PATH));
        node.setProperty("multi", new String[] { "one", "two", "three" });
    }

    /**
     * Creates four nodes under the given node. Each node has a String
     * property named "prop1" with some content set.
     */
    private void addQueryTestData(Node node) throws RepositoryException {
        while (node.hasNode("node1")) {
            node.getNode("node1").remove();
        }
        getOrAddNode(node, "node1").setProperty(
                "prop1", "You can have it good, cheap, or fast. Any two.");
        getOrAddNode(node, "node1").setProperty("prop1", "foo bar");
        getOrAddNode(node, "node1").setProperty("prop1", "Hello world!");
        getOrAddNode(node, "node2").setProperty("prop1", "Apache Jackrabbit");
    }


    /**
     * Creates three nodes under the given node: one of type nt:resource
     * and the other nodes referencing it.
     */
    private void addNodeTestData(Node node) throws RepositoryException, IOException {
        if (node.hasNode("multiReference")) {
            node.getNode("multiReference").remove();
        }
        if (node.hasNode("resReference")) {
            node.getNode("resReference").remove();
        }
        if (node.hasNode("myResource")) {
            node.getNode("myResource").remove();
        }

        Node resource = node.addNode("myResource", "nt:resource");
        resource.setProperty("jcr:encoding", ENCODING);
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty(
                "jcr:data",
                new ByteArrayInputStream("Hello w\u00F6rld.".getBytes(ENCODING)));
        resource.setProperty("jcr:lastModified", Calendar.getInstance());

        Node resReference = getOrAddNode(node, "reference");
        resReference.setProperty("ref", resource);
        // make this node itself referenceable
        resReference.addMixin("mix:referenceable");

        Node multiReference = node.addNode("multiReference");
        ValueFactory factory = node.getSession().getValueFactory();
        multiReference.setProperty("ref", new Value[] {
                factory.createValue(resource),
                factory.createValue(resReference)
            });
    }

    private void addExportTestData(Node node) throws RepositoryException, IOException {
        getOrAddNode(node, "invalidXmlName").setProperty("propName", "some text");

        // three nodes which should be serialized as xml text in docView export
        // separated with spaces
        getOrAddNode(node, "jcr:xmltext").setProperty(
                "jcr:xmlcharacters", "A text without any special character.");
        getOrAddNode(node, "some-element");
        getOrAddNode(node, "jcr:xmltext").setProperty(
                "jcr:xmlcharacters",
                " The entity reference characters: <, ', ,&, >,  \" should"
                + " be escaped in xml export. ");
        getOrAddNode(node, "some-element");
        getOrAddNode(node, "jcr:xmltext").setProperty(
                "jcr:xmlcharacters", "A text without any special character.");

        Node big = getOrAddNode(node, "bigNode");
        big.setProperty(
                "propName0",
                "SGVsbG8gd8O2cmxkLg==;SGVsbG8gd8O2cmxkLg==".split(";"),
                PropertyType.BINARY);
        big.setProperty("propName1", "text 1");
        big.setProperty(
                "propName2",
                "multival text 1;multival text 2;multival text 3".split(";"));
        big.setProperty("propName3", "text 1");

        addExportValues(node, "propName");
        addExportValues(node, "Prop<>prop");
    }

    /**
     * create nodes with following properties
     * binary & single
     * binary & multival
     * notbinary & single
     * notbinary & multival
     */
    private void addExportValues(Node node, String name)
            throws RepositoryException, IOException {
        String prefix = "valid";
        if (name.indexOf('<') != -1) {
            prefix = "invalid";
        }
        node = getOrAddNode(node, prefix + "Names");

        String[] texts = new String[] {
                "multival text 1", "multival text 2", "multival text 3" };
        getOrAddNode(node, prefix + "MultiNoBin").setProperty(name, texts);

        Node resource = getOrAddNode(node, prefix + "MultiBin");
        resource.setProperty("jcr:encoding", ENCODING);
        resource.setProperty("jcr:mimeType", "text/plain");
        String[] values =
            new String[] { "SGVsbG8gd8O2cmxkLg==", "SGVsbG8gd8O2cmxkLg==" };
        resource.setProperty(name, values, PropertyType.BINARY);
        resource.setProperty("jcr:lastModified", Calendar.getInstance());

        getOrAddNode(node, prefix + "NoBin").setProperty(name,  "text 1");

        resource = getOrAddNode(node, "invalidBin");
        resource.setProperty("jcr:encoding", ENCODING);
        resource.setProperty("jcr:mimeType", "text/plain");
        byte[] bytes = "Hello w\u00F6rld.".getBytes(ENCODING);
        resource.setProperty(name, new ByteArrayInputStream(bytes));
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
    }

}
