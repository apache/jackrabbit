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
package org.apache.jackrabbit.test.api;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>AbstractWorkspaceReferenceableTest</code> is the abstract base class for all
 * copying/moving/cloning related test classes with referenceable nodes in workspace.
 */
abstract class AbstractWorkspaceReferenceableTest extends AbstractWorkspaceCopyBetweenTest {


    protected void setUp() throws Exception {
        super.setUp();

        // we assume referencing is supported by repository
        NodeTypeManager ntMgr = superuser.getWorkspace().getNodeTypeManager();

        // assert that this repository supports references
        try {
            NodeType referenceableNt = ntMgr.getNodeType(mixReferenceable);
            if (referenceableNt == null) {
                throw new NotExecutableException("Repository does not support Referencing: mixin nodetype '" + mixReferenceable + "' is missing.");
            }
        } catch (NoSuchNodeTypeException e) {
            throw new NotExecutableException("Repository does not support Referencing: mixin nodetype '" + mixReferenceable + "' is missing.");
        }
    }

    /**
     * add the mix:referenceable mixin type to a node.
     *
     * @param node
     * @return referenceable node.
     */
    protected Node addMixinReferenceableToNode(Node node) throws RepositoryException {
        if (!node.isNodeType(mixReferenceable)) {
            node.addMixin(mixReferenceable);
            node.save();
        }
        return node;
    }
}