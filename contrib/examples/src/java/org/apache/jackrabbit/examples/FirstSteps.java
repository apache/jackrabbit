/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.examples;

import java.io.InputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

/**
 * The First Steps example class.
 */
public class FirstSteps {

    private final Repository repository;

    public FirstSteps(Repository repository) {
        this.repository = repository;
    }

    /**
     * Run the First Steps example.
     */
    public void run() {
        try {
            SimpleCredentials creds =
                new SimpleCredentials("username", "password".toCharArray());
            Session session = repository.login(creds);
            Node root = session.getRootNode();

            System.out.println(root.getPrimaryNodeType().getName());

            if (!root.hasNode("testnode")) {
                System.out.println("creating testnode");
                Node node = root.addNode("testnode", "nt:unstructured");
                node.setProperty("testprop", session.getValueFactory().createValue("Hello, World."));
                session.save();
            }

            if (!root.hasNode("importxml")) {
                System.out.println("importing xml");
                Node node = root.addNode("importxml", "nt:unstructured");
                InputStream xml =
                    getClass().getClassLoader().getResourceAsStream(
                            "org/apache/jackrabbit/examples/firststeps.xml");
                session.importXML(
                        "/importxml", xml, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                session.save();
            }

            dump(root);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Dumps the contents of the given node to standard output.
     *
     * @param node the node to be dumped
     * @throws RepositoryException on repository errors
     */
    public static void dump(Node node) throws RepositoryException {
        System.out.println(node.getPath());

        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            System.out.print(property.getPath() + "=");
            if (property.getDefinition().isMultiple()) {
                Value[] values = property.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        System.out.println(",");
                    }
                    System.out.println(values[i].getString());
                }
            } else {
                System.out.print(property.getString());
            }
            System.out.println();
        }

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node child = nodes.nextNode();
            dump(child);
        }
    }

}
