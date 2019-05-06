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
package org.apache.jackrabbit.core.query.lucene.join;

import java.io.IOException;

import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.spi.Name;

/**
 * <code>Condition</code> defines an interface for a join condition.
 */
public interface Condition {

    /**
     * Returns the matching inner score nodes for the given outer score node
     * <code>sn</code>.
     *
     * @param outer the current score nodes of the outer source.
     * @return the matching score nodes in the inner source.
     * @throws IOException if an error occurs while evaluating the condition.
     */
    public ScoreNode[][] getMatchingScoreNodes(ScoreNode outer)
            throws IOException;

    /**
     * @return the selector name of the inner hits.
     */
    public Name[] getInnerSelectorNames();

    /**
     * Closes this condition and frees resources.
     *
     * @throws IOException if an error occurs while closing this condition.
     */
    public void close() throws IOException;
}
