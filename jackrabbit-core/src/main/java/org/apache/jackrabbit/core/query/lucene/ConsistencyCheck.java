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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.jackrabbit.core.id.NodeId;

/**
 * Consistency check implementation for search indexes delegating to {@link DefaultConsistencyCheck} for backward compatibility purposes.
 * @deprecated Use {@link DefaultConsistencyCheck} directly or implement {@link ConsistencyCheckInterface}
 */
public class ConsistencyCheck implements ConsistencyCheckInterface {

    private final ConsistencyCheckInterface delegate;

    /**
     * Creates a new consistency check for the given index.
     *
     * @param index the index to check
     * @param handler the QueryHandler to use
     * @param excludedIds the set of node ids that are not indexed
     * @deprecated Use {@link DefaultConsistencyCheck} directly or implement {@link ConsistencyCheckInterface}
     */
    @Deprecated
    public ConsistencyCheck(MultiIndex index, SearchIndex handler, Set<NodeId> excludedIds) {
        this.delegate = new DefaultConsistencyCheck(index, handler, excludedIds);
    }

    @Override
    public void run() throws IOException {
        delegate.run();
    }

    @Override
    public void repair(boolean ignoreFailure) throws IOException {
        delegate.repair(ignoreFailure);
    }

    @Override
    public List<ConsistencyCheckError> getErrors() {
        return delegate.getErrors();
    }

    @Override
    public void doubleCheckErrors() {
        delegate.doubleCheckErrors();
    }
}
