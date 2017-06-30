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
package org.apache.jackrabbit.test.api.version.simple;

import javax.jcr.RepositoryException;

/**
 * <code>BasicTest</code> checks if simple versioning is correctly set up
 *
 */
public class BasicTest extends AbstractVersionTest {

    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test node is simple versionable
     *
     * @throws RepositoryException
     */
    public void testNodeTypes() throws RepositoryException {
        assertTrue("Node.isNodeType(mix:simpleVersionable) must return true.",
                versionableNode.isNodeType(mixSimpleVersionable));
        assertFalse("Node.isNodeType(mix:versionable) must return false.",
                versionableNode.isNodeType(mixVersionable));
    }

    /**
     * Test if node has a jcr:isCheckedOut property
     *
     * @throws RepositoryException
     */
    public void testICOProperty() throws RepositoryException {
        assertTrue("Versionable node must have a jcr:isCheckedOut property.",
                versionableNode.hasProperty(jcrIsCheckedOut));
    }


}
