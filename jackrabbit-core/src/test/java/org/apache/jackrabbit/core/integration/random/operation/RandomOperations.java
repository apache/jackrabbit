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

/**
 * <code>RandomOperations</code> is an abstract base class for a set of random
 * operation.
 */
public abstract class RandomOperations extends Operation {

    private final OperationFactory factory;

    private final NodeIterator nodes;

    private final int numOperations;

    public RandomOperations(OperationFactory factory,
                            Session s,
                            NodeIterator nodes,
                            int numOperations) {
        super(s, "/");
        this.factory = factory;
        this.nodes = nodes;
        this.numOperations = numOperations;
    }

    public NodeIterator execute() throws Exception {
        List operations = new ArrayList();
        for (int i = 0; i < numOperations && nodes.hasNext(); i++) {
            Node n = nodes.nextNode();
            operations.add(getRandomOperation(n.getPath()));
        }
        return new OperationSequence(getSession(), operations).execute();
    }

    protected int getNumOperations() {
        return numOperations;
    }

    protected OperationFactory getFactory() {
        return factory;
    }

    protected abstract Operation getRandomOperation(String path) throws Exception;
}
