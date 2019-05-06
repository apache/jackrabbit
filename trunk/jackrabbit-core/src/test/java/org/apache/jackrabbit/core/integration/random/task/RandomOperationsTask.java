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
package org.apache.jackrabbit.core.integration.random.task;

import org.apache.jackrabbit.core.AbstractConcurrencyTest;
import org.apache.jackrabbit.core.integration.random.operation.Operation;
import org.apache.jackrabbit.core.integration.random.operation.OperationFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * <code>ContentOperationsTask</code> base class for random operation tasks.
 */
public abstract class RandomOperationsTask implements AbstractConcurrencyTest.Task {

    private final String[] mixins;

    private final int numLevels;

    private final int nodesPerLevel;

    private final int saveInterval;

    private final long end;

    private boolean useXA = false;

    public RandomOperationsTask(String mixins[], int numLevels,
                                int nodesPerLevel, int saveInterval, long end) {
        this.mixins = mixins;
        this.numLevels = numLevels;
        this.nodesPerLevel = nodesPerLevel;
        this.saveInterval = saveInterval;
        this.end = end;
    }

    public void execute(Session session, Node test) throws RepositoryException {
        try {
            OperationFactory f = new OperationFactory(session);
            // create nodes
            f.createNodes(test.getPath(), numLevels, nodesPerLevel,
                    mixins, saveInterval).execute();
            // save nodes
            f.save(test.getPath()).execute();

            NodeIterator nodes = f.getRandomNodes(f.traverseNodes(test.getPath()));
            while (end > System.currentTimeMillis()) {
                Operation op = f.runInSequence(new Operation[]{
                    getRandomOperations(f, nodes),
                    f.save("/")
                });
                if (isUseXA()) {
                    op = f.runInTransaction(op);
                }
                op.execute();
            }
            test.remove();
            session.save();
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    protected abstract Operation getRandomOperations(OperationFactory f,
                                                     NodeIterator randomNodes)
            throws Exception;

    public boolean isUseXA() {
        return useXA;
    }

    public void setUseXA(boolean useXA) {
        this.useXA = useXA;
    }
}
