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

import org.apache.jackrabbit.util.Text;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * <code>org.apache.jackrabbit.core.ConcurrentRenameTest</code>...
 */
public class ConcurrentRenameTest extends AbstractConcurrencyTest {

    private static final int NUM_MOVES = 100;
    private static final int NUM_THREADS = 2;

    public void testConcurrentRename() throws Exception {
        runTask(new Task() {

            public void execute(Session session, Node test)
                    throws RepositoryException {
                String name = Thread.currentThread().getName();
                // create node
                Node n = test.addNode(name);
                session.save();
                // do moves
                for (int i = 0; i < NUM_MOVES; i++) {
                    String path = n.getPath();
                    String newName = name + "-" + i;
                    String newPath = Text.getRelativeParent(path, 1) + "/" + newName;
                    session.move(path, newPath);
                    session.save();
                    n = session.getNode(newPath);
                }
            }
        }, NUM_THREADS, testRootNode.getPath());
    }
}