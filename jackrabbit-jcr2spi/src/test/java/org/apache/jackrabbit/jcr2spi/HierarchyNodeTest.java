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
package org.apache.jackrabbit.jcr2spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.Session;
import java.util.Calendar;
import java.util.Set;
import java.util.HashSet;

/**
 * <code>HierarchyNodeTest</code>...
 */
public class HierarchyNodeTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(HierarchyNodeTest.class);

    private Set hierarchyNodeProps = new HashSet();
    private Set resourceProps = new HashSet();

    private String ntFolder;
    private String ntFile;
    private String ntResource;

    private Node fileNode;

    protected void setUp() throws Exception {
        super.setUp();
        Session s = testRootNode.getSession();
        String jcrPrefix = s.getNamespacePrefix(NS_JCR_URI);
        String ntPrefix = s.getNamespacePrefix(NS_NT_URI);

        ntFolder = ntPrefix + ":folder";
        ntFile = ntPrefix + ":file";
        ntResource = ntPrefix + ":resource";

        hierarchyNodeProps.add(jcrPrefix+":primaryType");
        hierarchyNodeProps.add(jcrPrefix+":created");

        resourceProps.add(jcrPrefix+":primaryType");
        resourceProps.add(jcrPrefix+":lastModified");
        resourceProps.add(jcrPrefix+":mimeType");
        resourceProps.add(jcrPrefix+":data");
        resourceProps.add(jcrPrefix+":uuid");

        try {
            Node folder = testRootNode.addNode("folder", ntFolder);
            fileNode = folder.addNode("file", ntFile);

            Node content = fileNode.addNode(jcrPrefix + ":content", ntResource);
            content.setProperty(jcrPrefix + ":lastModified", Calendar.getInstance());
            content.setProperty(jcrPrefix + ":mimeType", "text/plain");
            content.setProperty(jcrPrefix + ":data", "some plain text");

            testRootNode.save();
        } catch (RepositoryException e) {
            throw new NotExecutableException("Cannot create hierarchy nodes.");
        }
    }

    protected void tearDown() throws Exception {
        fileNode = null;
        super.tearDown();
    }

    public void testGetProperties() throws RepositoryException {
        Session readSession = helper.getReadOnlySession();
        try {
            dump((Node) readSession.getItem(fileNode.getPath()));
        } finally {
            readSession.logout();
        }
    }

    /** Recursively outputs the contents of the given node. */
    private void dump(Node node) throws RepositoryException {

        // Then output the properties
        PropertyIterator properties = node.getProperties();
        Set set = new HashSet();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            set.add(property.getName());
        }

        if (node.getPrimaryNodeType().getName().equals(ntFolder)) {
            assertTrue(hierarchyNodeProps.size() == set.size() && hierarchyNodeProps.containsAll(set));
        } else if (node.getPrimaryNodeType().getName().equals(ntFile)) {
            assertTrue(hierarchyNodeProps.size() == set.size() && hierarchyNodeProps.containsAll(set));
        } else if (node.getPrimaryNodeType().getName().equals(ntResource)) {
            assertTrue(resourceProps.size() == set.size() && resourceProps.containsAll(set));
        }

        // Finally output all the child nodes recursively
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            dump(nodes.nextNode());
        }
    }
}