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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Collections;
import java.util.Random;

/**
 * <code>Operation</code> is the common base class for all operations.
 */
public abstract class Operation {

    private final Random rand = new Random();

    private boolean useTransaction = false;

    private int maxRandomWait = 0;

    private final Session session;

    private final String path;

    public Operation(Session s, String path) {
        this.session = s;
        this.path = path;
    }

    public abstract NodeIterator execute() throws Exception;

    public boolean isUseTransaction() {
        return useTransaction;
    }

    public void setUseTransaction(boolean useTransaction) {
        this.useTransaction = useTransaction;
    }

    protected Session getSession() {
        return session;
    }

    protected String getPath() {
        return path;
    }

    protected Item getItem() throws RepositoryException {
        return session.getItem(path);
    }

    protected Node getNode() throws RepositoryException {
        return (Node) getItem();
    }

    /**
     * Do a random wait. See also {@link #setMaxRandomWait(int)}.
     */
    protected void randomWait() throws Exception {
        if (maxRandomWait > 0) {
            Thread.sleep(rand.nextInt(maxRandomWait));
        }
    }

    /**
     * @return the maximum number of milliseconds to wait when doing a
     *         randomized wait.
     */
    public int getMaxRandomWait() {
        return maxRandomWait;
    }

    /**
     * @param maxRandomWait the maximum number of milliseconds to wait when
     *                      doing a randomized wait.
     */
    public void setMaxRandomWait(int maxRandomWait) {
        this.maxRandomWait = maxRandomWait;
    }

    /**
     * Wraps a single node with a node iterator.
     *
     * @param node the node to wrap.
     * @return a node iterator over the single <code>node</code>.
     */
    protected static NodeIterator wrapWithIterator(Node node) {
        return new NodeIteratorAdapter(Collections.singletonList(node));
    }

    protected String getRandomText(int numChars) {
        StringBuffer tmp = new StringBuffer(numChars);
        for (int i = 0; i < numChars; i++) {
            char c = (char) (rand.nextInt(('z' + 1) - 'a') + 'a');
            tmp.append(c);
        }
        return tmp.toString();
    }

    protected Random getRandom() {
        return rand;
    }
}
