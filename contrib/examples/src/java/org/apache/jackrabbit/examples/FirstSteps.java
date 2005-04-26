/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import org.apache.jackrabbit.core.jndi.RegistryHelper;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.StringValue;
import javax.jcr.Value;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * The First Steps example class.
 */
public class FirstSteps {

    /**
     * Run the First Steps example.
     *
     * @param args command line arguments (ignored)
     */
    public static void main(String[] args) {
        try {
            Repository repository = getRepository();
            SimpleCredentials creds = new SimpleCredentials("username", "password".toCharArray());
            Session session = repository.login(creds);
            Node root = session.getRootNode();

            System.out.println(root.getPrimaryNodeType().getName());

            if (!root.hasNode("testnode")) {
                System.out.println("creating testnode");
                Node node = root.addNode("testnode", "nt:unstructured");
                node.setProperty("testprop", new StringValue("Hello, World."));
                session.save();
            }

            if (!root.hasNode("importxml")) {
                System.out.println("importing xml");
                Node node = root.addNode("importxml", "nt:unstructured");
                InputStream xml = new FileInputStream("repotest/test.xml");
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
     * Creates a Repository instance to be used by the example class.
     *
     * @return repository instance
     * @throws Exception on errors
     */
    private static Repository getRepository() throws Exception {
        String configFile = "repotest/repository.xml";
        String repHomeDir = "repotest";

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
        env.put(Context.PROVIDER_URL, "localhost");
        InitialContext ctx = new InitialContext(env);

        RegistryHelper.registerRepository(ctx, "repo", configFile, repHomeDir, true);
        return (Repository) ctx.lookup("repo");
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
