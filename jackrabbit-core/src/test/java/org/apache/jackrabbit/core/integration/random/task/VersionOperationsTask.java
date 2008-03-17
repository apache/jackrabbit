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

import org.apache.jackrabbit.core.integration.random.operation.Operation;
import org.apache.jackrabbit.core.integration.random.operation.OperationFactory;

import javax.jcr.NodeIterator;

/**
 * <code>VersionOperationsTask</code> performs random version operations.
 */
public class VersionOperationsTask extends RandomOperationsTask {

    public VersionOperationsTask(int numLevels, int nodesPerLevel,
                                 int saveInterval, long end) {
        super(new String[]{"mix:versionable"}, numLevels, nodesPerLevel,
                saveInterval, end);
    }

    protected Operation getRandomOperations(OperationFactory f,
                                            NodeIterator randomNodes)
            throws Exception {
        return f.randomVersionOperations(randomNodes, 3, 10);
    }
}
