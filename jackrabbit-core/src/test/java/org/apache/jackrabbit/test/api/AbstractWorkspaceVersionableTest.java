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
 * <code>AbstractWorkspaceReferenceableTest</code> is the abstract base class
 * for all copying/moving/cloning related test classes with versionable nodes in
 * workspace.
 */
abstract class AbstractWorkspaceVersionableTest extends AbstractWorkspaceCopyBetweenTest {


    protected void setUp() throws Exception {
        super.setUp();

        // we assume versioning is supported by repository
        NodeTypeManager ntMgr = superuser.getWorkspace().getNodeTypeManager();

        // assert that this repository supports versioning
        try {
            NodeType versionableNt = ntMgr.getNodeType(mixVersionable);
            if (versionableNt == null) {
                throw new NotExecutableException("Repository does not support versioning: mixin nodetype '" + mixVersionable + "' is missing.");
            }
        } catch (NoSuchNodeTypeException e) {
            throw new NotExecutableException("Repository does not support versioning: mixin nodetype '" + mixVersionable + "' is missing.");
        }
    }

    /**
     * add the mix:versionable mixin type to a node.
     */
    protected Node addMixinVersionableToNode(Node parent, Node node) throws RepositoryException {
        NodeType nodetype = node.getPrimaryNodeType();
        if (!nodetype.isNodeType(mixVersionable)) {
            node.addMixin(mixVersionable);
        }
        parent.save();

        return node;
    }
}