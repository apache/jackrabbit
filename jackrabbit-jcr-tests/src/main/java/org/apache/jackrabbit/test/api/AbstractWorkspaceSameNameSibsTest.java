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

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>AbstractWorkspaceSameNameSibsTest</code> is the abstract base class for
 * all copying/moving/cloning related test classes with samename siblings
 * allowed in workspace.
 */
abstract class AbstractWorkspaceSameNameSibsTest extends AbstractWorkspaceCopyBetweenTest {

    /**
     * Node type with sameNameSibs=true NodeDef
     */
    protected final String PROP_SAME_NAME_SIBS_TRUE_NODE_TYPE = "sameNameSibsTrueNodeType";

    /**
     * Node type with sameNameSibs=false NodeDef
     */
    protected final String PROP_SAME_NAME_SIBS_FALSE_NODE_TYPE = "sameNameSibsFalseNodeType";

    /**
     * A node type where same-name siblings are allowed
     */
    protected NodeType sameNameSibsTrueNodeType;

    /**
     * A node type where NO same-name siblings allowed
     */
    protected NodeType sameNameSibsFalseNodeType;

    protected void setUp() throws Exception {
        super.setUp();

        // we assume sameNameSibs is supported by repository
        NodeTypeManager ntMgr = superuser.getWorkspace().getNodeTypeManager();

        // sameNameSibs ALLOWED
        // make sure 'sameNameSibsTrue' nodetype is properly defined
        try {
            sameNameSibsTrueNodeType = ntMgr.getNodeType(getProperty(PROP_SAME_NAME_SIBS_TRUE_NODE_TYPE));
            NodeDefinition[] childNodeDefs = sameNameSibsTrueNodeType.getDeclaredChildNodeDefinitions();
            boolean isSameNameSibs = false;
            for (int i = 0; i < childNodeDefs.length; i++) {
                if (childNodeDefs[i].allowsSameNameSiblings()) {
                    isSameNameSibs = true;
                    break;
                }
            }
            if (!isSameNameSibs) {
                throw new NotExecutableException("Property 'sameNameSibsTrueNodeType' does not define a nodetype where sameNameSibs are allowed: '" + sameNameSibsTrueNodeType.getName() + "'");
            }
        } catch (NoSuchNodeTypeException e) {
            fail("Property 'sameNameSibsTrueNodeType' does not define an existing nodetype: '" + sameNameSibsTrueNodeType + "'");
        }

        // sameNameSibs NOT ALLOWED
        // make sure 'sameNameSibsFalse' nodetype is properly defined
        try {
            sameNameSibsFalseNodeType = ntMgr.getNodeType(getProperty(PROP_SAME_NAME_SIBS_FALSE_NODE_TYPE));
            NodeDefinition[] childNodeDefs = sameNameSibsFalseNodeType.getDeclaredChildNodeDefinitions();
            boolean isSameNameSibs = true;
            for (int i = 0; i < childNodeDefs.length; i++) {
                if (!childNodeDefs[i].allowsSameNameSiblings()) {
                    isSameNameSibs = false;
                    break;
                }
            }
            if (isSameNameSibs) {
                fail("Property 'sameNameSibsFalseNodeType' does define a nodetype where sameNameSibs are not allowed: '" + sameNameSibsFalseNodeType.getName() + "'");
            }
        } catch (NoSuchNodeTypeException e) {
            fail("Property 'sameNameSibsFalseNodeType' does not define an existing nodetype: '" + sameNameSibsFalseNodeType + "'");
        }

    }

    protected void tearDown() throws Exception {
        sameNameSibsTrueNodeType = null;
        sameNameSibsFalseNodeType = null;
        super.tearDown();
    }
}