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
package org.apache.jackrabbit.core.integration.random.operation;

import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <code>GetRandomNodes</code> randomly returns nodes that were returned by
 * the parent operation.
 */
public class GetRandomNodes extends Operation {

    private final Operation op;

    public GetRandomNodes(Session s, Operation op) {
        super(s, "/");
        this.op = op;
    }

    /**
     * Randomly returns node from the parent operation. Please note that the
     * returned iterator runs forever!!!
     * {@link NodeIterator#hasNext()} will always return true.
     */
    public NodeIterator execute() throws Exception {
        final List paths = new ArrayList();
        for (NodeIterator it = op.execute(); it.hasNext(); ) {
            paths.add(it.nextNode().getPath());
        }
        return new NodeIteratorAdapter(new Iterator() {

            public boolean hasNext() {
                // runs forever!!!
                return true;
            }

            public Object next() {
                String path = (String) paths.get(getRandom().nextInt(paths.size()));
                try {
                    return getSession().getItem(path);
                } catch (RepositoryException e) {
                    throw new NoSuchElementException();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        });
    }
}
