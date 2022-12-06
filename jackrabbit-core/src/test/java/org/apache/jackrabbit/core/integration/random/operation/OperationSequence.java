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

import org.apache.commons.collections4.iterators.IteratorChain;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;

import javax.jcr.NodeIterator;
import javax.jcr.Session;
import java.util.List;

/**
 * <code>OperationSequence</code> wraps other operations and executes the in
 * sequence.
 */
public class OperationSequence extends Operation {

    private final Operation[] ops;

    public OperationSequence(Session s, List<Operation> operations) {
        super(s, "/");
        this.ops = operations.toArray(new Operation[operations.size()]);
    }

    @SuppressWarnings("unchecked")
    public NodeIterator execute() throws Exception {
        IteratorChain<NodeIterator> chain = new IteratorChain<>();
        for (int i = 0; i < ops.length; i++) {
            chain.addIterator(ops[i].execute());
        }
        return new NodeIteratorAdapter(chain);
    }
}
