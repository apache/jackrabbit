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

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.NamespaceRegistry;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <code>RepositorySetup</code> initializes the test candidate with required
 * namespaces, node types and test content.
 */
public class RepositorySetup {

    private static final String NAMESPACES_RESOURCE = "namespaces.properties";

    private static final String NODETYPES_RESOURCE = "custom_nodetypes.xml";

    private static final String TEST_CONTENT_RESOURCE = "testdata.xml";

    private static final String TEST_WORKSPACE_NAME = "test";

    /**
     * The setup tasks to run.
     */
    private final Task[] TASKS = {
        new RegisterNamespaces(),
        new RegisterNodeTypes(),
        new ImportTestContent(),
        new CreateTestWorkspace()
    };

    /**
     * The session to the repository to setup.
     */
    private final SessionImpl session;

    /**
     * Private constructor.
     */
    private RepositorySetup(SessionImpl session) {
        this.session = session;
    }

    /**
     * Executes the repository setup tasks.
     *
     * @throws RepositoryException if an error occurs while running the tasks.
     */
    private void execute() throws RepositoryException {
        for (int i = 0; i < TASKS.length; i++) {
            TASKS[i].execute();
        }
    }

    /**
     * Runs the repository setup.
     *
     * @param session the session of the repository to setup.
     * @throws RepositoryException      if an error occurs while running the
     *                                  tasks.
     * @throws IllegalArgumentException if <code>session</code> is not a jackrabbit
     *                                  session.
     */
    public static void run(Session session) throws RepositoryException {
        if (session instanceof SessionImpl) {
            new RepositorySetup((SessionImpl) session).execute();
        } else {
            throw new IllegalArgumentException("not a Jackrabbit session");
        }
    }

    /**
     * Returns an input stream for reading the specified resource.
     *
     * @param name the resource name.
     * @return An input stream for reading the resource, or <code>null</code> if
     *         the resource could not be found
     */
    private static InputStream getResource(String name) {
        return RepositorySetup.class.getClassLoader().getResourceAsStream(name);
    }

    /**
     * Registers the <code>namespaces</code>.
     *
     * @param namespaces the namespaces to register.
     * @param session the session to register the namespaces.
     */
    private static void registerNamespaces(Properties namespaces,
                                           Session session)
            throws RepositoryException {
        NamespaceRegistry nsReg = session.getWorkspace().getNamespaceRegistry();
        for (Iterator it = namespaces.keySet().iterator(); it.hasNext();) {
            String prefix = (String) it.next();
            String uri = namespaces.getProperty(prefix);
            try {
                nsReg.registerNamespace(prefix, uri);
            } catch (RepositoryException e) {
                // then this namespace is already registered.
            }
        }
    }

    private interface Task {

        /**
         * Executes this task.
         *
         * @throws RepositoryException if an error occurs while running this
         *                             tasks.
         */
        public void execute() throws RepositoryException;
    }

    /**
     * Registers namespaces that are needed to run the test cases.
     */
    private final class RegisterNamespaces implements Task {

        /**
         * @inheritDoc
         */
        public void execute() throws RepositoryException {
            InputStream is = getResource(NAMESPACES_RESOURCE);
            if (is != null) {
                try {
                    Properties namespaces = new Properties();
                    namespaces.load(is);

                    registerNamespaces(namespaces, session);
                } catch (IOException e) {
                    throw new RepositoryException(e.getMessage());
                } finally {
                    try {
                        is.close();
                    } catch (IOException e1) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * Registers node types that are needed to run the test cases.
     */
    private final class RegisterNodeTypes implements Task {

        /**
         * @inheritDoc
         */
        public void execute() throws RepositoryException {
            InputStream is = getResource(NODETYPES_RESOURCE);
            if (is != null) {
                try {
                    NodeTypeReader ntReader = new NodeTypeReader(is);
                    registerNamespaces(ntReader.getNamespaces(), session);

                    NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
                    NodeTypeDef[] nodeTypes = ntReader.getNodeTypeDefs();
                    List unregisteredNTs = new ArrayList();
                    for (int i = 0; i < nodeTypes.length; i++) {
                        try {
                            ntMgr.getNodeType(nodeTypes[i].getName());
                        } catch (NoSuchNodeTypeException e) {
                            // register the node type
                            unregisteredNTs.add(nodeTypes[i]);
                        }
                    }
                    ntMgr.getNodeTypeRegistry().registerNodeTypes(unregisteredNTs);
                } catch (IOException e) {
                    throw new RepositoryException(e.getMessage());
                } catch (InvalidNodeTypeDefException e) {
                    throw new RepositoryException(e.getMessage());
                } finally {
                    try {
                        is.close();
                    } catch (IOException e1) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * Imports test content into the repository if the repository is empty.
     */
    private final class ImportTestContent implements Task {

        /**
         * @inheritDoc
         */
        public void execute() throws RepositoryException {
            InputStream is = getResource(TEST_CONTENT_RESOURCE);
            if (is != null) {
                try {
                    Node rootNode = session.getRootNode();
                    if (!rootNode.hasNode("testdata")) {
                        session.getWorkspace().importXML("/", is,
                                ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING);
                    }
                    if (!rootNode.hasNode("testroot")) {
                        rootNode.addNode("testroot");
                        session.save();
                    }
                } catch (IOException e) {
                    throw new RepositoryException(e.getMessage());
                } finally {
                    try {
                        is.close();
                    } catch (IOException e1) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * Creates a workspace with name 'test' if it is not yet present.
     */
    private final class CreateTestWorkspace implements Task {

        /**
         * @inheritDoc
         */
        public void execute() throws RepositoryException {
            List workspaces = Arrays.asList(session.getWorkspace().getAccessibleWorkspaceNames());
            if (!workspaces.contains(TEST_WORKSPACE_NAME)) {
                ((WorkspaceImpl) session.getWorkspace()).createWorkspace(TEST_WORKSPACE_NAME);
            }
        }
    }
}
