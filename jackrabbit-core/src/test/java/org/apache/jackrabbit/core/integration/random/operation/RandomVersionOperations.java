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

import javax.jcr.Session;
import javax.jcr.NodeIterator;

/**
 * <code>RandomVersionOperations</code> executes a number of version operations.
 */
public class RandomVersionOperations extends RandomOperations {

    public RandomVersionOperations(OperationFactory factory,
                 Session s,
                 NodeIterator nodes,
                 int numOperations) {
        super(factory, s, nodes, numOperations);
    }

    public NodeIterator execute() throws Exception {
        return super.execute();
    }

    protected Operation getRandomOperation(String path) throws Exception {
        OperationFactory f = getFactory();
        return f.randomVersionOperation(f.getNode(path));
    }
}
