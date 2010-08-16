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
package org.apache.jackrabbit.performance;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * A {@link ConcurrentReadTest} with a single writer thread that continuously
 * updates the nodes being accessed by the concurrent readers.
 */
public class ConcurrentReadWriteTest extends ConcurrentReadTest {

    private volatile boolean running;

    private Thread writer;

    public void beforeSuite() throws Exception {
        super.beforeSuite();

        running = true;
        writer = new Writer();
        writer.start();
    }

    private class Writer extends Thread implements ItemVisitor {

        private long count = 0;

        @Override
        public void run() {
            try {
                while (running) {
                    root.accept(this);
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        public void visit(Node node) throws RepositoryException {
            if (running) {
                node.setProperty("count", count++);
                node.getSession().save();

                NodeIterator iterator = node.getNodes();
                while (iterator.hasNext()) {
                    iterator.nextNode().accept(this);
                }
            }
        }

        public void visit(Property property) {
        }

    }

    public void afterSuite() throws Exception {
        running = false;
        writer.join();

        super.afterSuite();
    }

}
