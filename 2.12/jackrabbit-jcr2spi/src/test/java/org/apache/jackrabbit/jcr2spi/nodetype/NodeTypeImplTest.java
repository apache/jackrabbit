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
package org.apache.jackrabbit.jcr2spi.nodetype;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>NodeTypeImplTest</code>...
 */
public class NodeTypeImplTest extends AbstractJCRTest {

    private NodeTypeManager ntMgr;
    private NodeTypeImpl nodeType;
    private NameResolver resolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ntMgr = superuser.getWorkspace().getNodeTypeManager();
        NodeType nt = ntMgr.getNodeType(testNodeType);
        if (nt instanceof NodeTypeImpl) {
            nodeType = (NodeTypeImpl) nt;
        } else {
            cleanUp();
            throw new NotExecutableException("NodeTypeImpl expected.");
        }

        if (superuser instanceof NameResolver) {
            resolver = (NameResolver) superuser;
        } else {
            cleanUp();
             throw new NotExecutableException();
        }
    }

    public void testIsNodeType() throws RepositoryException {
        NodeType[] superTypes = nodeType.getSupertypes();

        for (int i = 0; i < superTypes.length; i++) {
            String name = superTypes[i].getName();
            assertTrue(nodeType.isNodeType(resolver.getQName(name)));
        }

        // unknown nt
        String unknownName = "unknown";
        assertFalse(nodeType.isNodeType(unknownName));

        // all non-mixin node types must be derived from nt base.
        if (!nodeType.isMixin()) {
            assertTrue(nodeType.isNodeType("nt:base"));
        }
    }

}