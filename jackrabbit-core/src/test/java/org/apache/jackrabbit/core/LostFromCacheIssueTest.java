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

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

public class LostFromCacheIssueTest extends AbstractJCRTest {

    private static final String NAMESPACE_PREFIX = "LostFromCacheIssueTestNamespacePrefix";
    private static final String NAMESPACE_URI = "http://www.onehippo.org/test/1.0";

    private static final String TESTNODE_PATH = "/LostFromCacheIssueTest/node";

    private static final String NODETYPE_1 = NAMESPACE_PREFIX + ":mixin";
    private static final String NODETYPE_2 = NAMESPACE_PREFIX + ":mxn";

    public Property mixinTypes;

    public void setUp() throws Exception {

        super.setUp();

        Workspace workspace = superuser.getWorkspace();
        NamespaceRegistry namespaceRegistry = workspace.getNamespaceRegistry();
        NodeTypeManager ntmgr = workspace.getNodeTypeManager();
        NodeTypeRegistry nodetypeRegistry = ((NodeTypeManagerImpl)ntmgr).getNodeTypeRegistry();
        try {
            namespaceRegistry.registerNamespace(NAMESPACE_PREFIX, NAMESPACE_URI);
        } catch (NamespaceException ignore) {
            //already exists
        }
        QNodeTypeDefinition nodeTypeDefinition = new QNodeTypeDefinitionImpl(
                ((SessionImpl)superuser).getQName(NODETYPE_1),
                Name.EMPTY_ARRAY,
                Name.EMPTY_ARRAY,
                true,
                false,
                true,
                false,
                null,
                QPropertyDefinition.EMPTY_ARRAY,
                QNodeDefinition.EMPTY_ARRAY
                );
        try {
            nodetypeRegistry.registerNodeType(nodeTypeDefinition);
        } catch (InvalidNodeTypeDefException ignore) {
            //already exists
        }
        nodeTypeDefinition = new QNodeTypeDefinitionImpl(
                ((SessionImpl)superuser).getQName(NODETYPE_2),
                Name.EMPTY_ARRAY,
                Name.EMPTY_ARRAY,
                true,
                false,
                true,
                false,
                null,
                QPropertyDefinition.EMPTY_ARRAY,
                QNodeDefinition.EMPTY_ARRAY
                );
        try {
            nodetypeRegistry.registerNodeType(nodeTypeDefinition);
        } catch (InvalidNodeTypeDefException ignore) {
            //already exists
        }

        getOrCreate(superuser.getRootNode(), TESTNODE_PATH);
        superuser.save();
    }

    private static Node getOrCreate(Node parent, String path) throws RepositoryException {
        if (parent == null || path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing `parent` or `path`");
        }

        String p = path;
        if (path.startsWith("/")) {
            p = path.substring(1);
        }

        Node node = null;
        try {
            node = parent.getNode(p);
        } catch (PathNotFoundException e) {
            // swallowing exception
        }

        if (node == null) {
            // if null is not there and therefore creating it
            for (String n : p.split("/")) {
                if (node == null) {
                    node = parent.addNode(n);
                } else {
                    node = node.addNode(n);
                }
            }
        }

        return node;
    }

    public void testIssue() throws Exception {
        Node node = superuser.getRootNode().getNode(TESTNODE_PATH.substring(1));
        node.addMixin(NODETYPE_2);
        mixinTypes = node.getProperty("jcr:mixinTypes");
        superuser.save();
        node.addMixin(NODETYPE_1);
        superuser.save();
        node.removeMixin(NODETYPE_2);
        node.removeMixin(NODETYPE_1);
        superuser.save();
        node.addMixin(NODETYPE_1);
        superuser.save();
    }
}
