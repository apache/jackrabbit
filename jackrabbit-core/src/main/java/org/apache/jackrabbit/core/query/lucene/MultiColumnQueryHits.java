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

import org.apache.jackrabbit.spi.Name;

import java.io.IOException;

/**
 * <code>MultiColumnQueryHits</code> defines an interface for reading tuples of
 * {@link ScoreNode}s. The {@link ScoreNode}s within a tuple are identified by
 * selector {@link Name}s.
 */
public interface MultiColumnQueryHits extends CloseableHits {

    /**
     * Returns the next score nodes in this QueryHits or <code>null</code> if
     * there are no more score nodes.
     *
     * @return the next score nodes in this QueryHits.
     * @throws IOException if an error occurs while reading from the index.
     */
    ScoreNode[] nextScoreNodes() throws IOException;

    /**
     * @return the selector names that correspond to the {@link ScoreNode}s
     *         returned by {@link #nextScoreNodes()}.
     */
    Name[] getSelectorNames();
}
