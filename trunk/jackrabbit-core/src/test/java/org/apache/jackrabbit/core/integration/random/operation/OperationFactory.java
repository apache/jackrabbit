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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;

/**
 * <code>OperationFactory</code> hides operation instantiation.
 */
public class OperationFactory {

    private final Random rand = new Random();

    private final Session session;

    private int nodeCount = 0;

    public OperationFactory(Session s) {
        this.session = s;
    }

    /**
     * Creates a series of random content operations using <code>nodes</code> as
     * a source of nodes to operate on.
     */
    public Operation randomContentOperations(NodeIterator nodes,
                                             int minOperations,
                                             int maxOperations)
            throws Exception {
        return new RandomContentOperations(this, session, nodes,
                minOperations + rand.nextInt(maxOperations - minOperations));
    }

    /**
     * Creates random operations for all the nodes <code>op</code> returns on
     * {@link Operation#execute()}.
     */
    public Operation randomContentOperation(Operation op) throws Exception {
        NodeIterator it = op.execute();
        List ops = new ArrayList();
        while (it.hasNext()) {
            Node n = it.nextNode();
            String path = n.getPath();
            switch (rand.nextInt(3)) { // TODO keep in sync with case list
                case 0:
                    ops.add(new AddNode(session, path, "payload" + nodeCount++));
                    break;
                case 1:
                    // limit to 10 distinct properties
                    ops.add(new SetProperty(session, path, "prop" + rand.nextInt(10)));
                    break;
                case 2:
                    ops.add(new Remove(session, path, "payload"));
                    break;
            }
        }
        if (ops.size() == 1) {
            return (Operation) ops.get(0);
        } else {
            OperationSequence sequence = new OperationSequence(session, ops);
            return sequence;
        }
    }

    /**
     * Creates a series of random version operations using <code>nodes</code> as
     * a source of nodes to operate on.
     */
    public Operation randomVersionOperations(NodeIterator nodes,
                                             int minOperations,
                                             int maxOperations)
            throws Exception {
        return new RandomVersionOperations(this, session, nodes,
                minOperations + rand.nextInt(maxOperations - minOperations));
    }

    /**
     * Creates random operations for all the nodes <code>op</code> returns on
     * {@link Operation#execute()}.
     */
    public Operation randomVersionOperation(Operation op) throws Exception {
        NodeIterator it = op.execute();
        List ops = new ArrayList();
        while (it.hasNext()) {
            Node n = it.nextNode();
            String path = n.getPath();
            switch (rand.nextInt(6)) { // TODO keep in sync with case list
                case 0:
                    ops.add(new Checkin(session, path));
                    break;
                case 1:
                    ops.add(new Checkout(session, path));
                    break;
                case 2:
                    //ops.add(new Restore(session, path));
                    break;
                case 3:
                    ops.add(new AddVersionLabel(session, path));
                    break;
                case 4:
                    ops.add(new RemoveVersionLabel(session, path));
                    break;
                case 5:
                    ops.add(new RemoveVersion(session, path));
                    break;
            }
        }
        if (ops.size() == 1) {
            return (Operation) ops.get(0);
        } else {
            OperationSequence sequence = new OperationSequence(session, ops);
            return sequence;
        }
    }

    /**
     * Wraps an XA transaction operation around <code>op</code>.
     */
    public Operation runInTransaction(Operation op) {
        return new XATransaction(session, op);
    }

    /**
     * Creates a new operation that contains the passed <code>ops</code>.
     */
    public Operation runInSequence(Operation[] ops) {
        return new OperationSequence(session, Arrays.asList(ops));
    }

    /**
     * Creates a save operation on the node with the given path.
     */
    public Operation save(String path) {
        return new Save(session, path);
    }

    /**
     * Creates an operation the return the node with the given <code>path</code>.
     */
    public Operation getNode(String path) {
         return new GetNode(session, path);
    }

    /**
     * Creates an operation that returns all nodes that are visited by traversing
     * the node hierarchy starting at the node with the given <code>path</code>.
     */
    public Operation traverseNodes(String path) {
         return new TraverseNodes(session, path);
    }

    /**
     * Creates a node hierarchy.
     */
    public Operation createNodes(String path,
                                 int numLevels,
                                 int nodesPerLevel,
                                 String[] mixins,
                                 int saveInterval) {
        return new CreateNodes(session, path, numLevels,
                nodesPerLevel, mixins, saveInterval);
    }

    /**
     * Creates an iterator that randomly returns nodes produced by <code>op</code>.
     * Please note that the returned iterator always returns <code>true</code>
     * for {@link NodeIterator#hasNext()}!!!
     */
    public NodeIterator getRandomNodes(Operation op) throws Exception {
        return new GetRandomNodes(session, op).execute();
    }
}
