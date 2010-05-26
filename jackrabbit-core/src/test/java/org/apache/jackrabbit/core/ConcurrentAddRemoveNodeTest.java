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

import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * <code>ConcurrentAddRemoveNodeTest</code> checks if concurrent modifications
 * on a node are properly handled. Adding and removing distinct child nodes
 * by separate sessions must succeed.
 */
public class ConcurrentAddRemoveNodeTest extends AbstractConcurrencyTest {

    private final AtomicInteger count = new AtomicInteger();

    public void testAddRemove() throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test)
                    throws RepositoryException {
                String name = "node-" + count.getAndIncrement();
                for (int i = 0; i < 10; i++) {
                    if (test.hasNode(name)) {
                        test.getNode(name).remove();
                    } else {
                        test.addNode(name);
                    }
                    session.save();
                }
            }
        }, 10, testRootNode.getPath());
    }
}
