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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ReorderSNSTest</code>...
 */
public class ReorderReferenceableSNSTest extends ReorderTest {

    private static Logger log = LoggerFactory.getLogger(ReorderReferenceableSNSTest.class);

    @Override
    protected void createOrderableChildren() throws RepositoryException, NotExecutableException {
        child1 = testRootNode.addNode(nodeName2, testNodeType);
        child2 = testRootNode.addNode(nodeName2, testNodeType);
        child3 = testRootNode.addNode(nodeName2, testNodeType);
        child4 = testRootNode.addNode(nodeName2, testNodeType);
        Node[] children = new Node[] { child1, child2, child3, child4};
        for (int i = 0; i < children.length; i++) {
            if (children[i].canAddMixin(mixReferenceable)) {
                children[i].addMixin(mixReferenceable);
            } else {
                throw new NotExecutableException();
            }
        }
        testRootNode.save();
    }
}
