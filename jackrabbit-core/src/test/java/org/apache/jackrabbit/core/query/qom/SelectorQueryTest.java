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
package org.apache.jackrabbit.core.query.qom;

import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;

/**
 * <code>SelectorQueryTest</code>...
 */
public class SelectorQueryTest extends AbstractQOMTest {

    public void testSelector() throws RepositoryException {
        // make sure there's at least one node with this node type
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        Query q = qomFactory.createQuery(
                qomFactory.selector(testNodeType), null, null, null);
        NodeIterator it = q.execute().getNodes();
        while (it.hasNext()) {
            assertTrue("Wrong node type", it.nextNode().isNodeType(testNodeType));
        }
    }
}
