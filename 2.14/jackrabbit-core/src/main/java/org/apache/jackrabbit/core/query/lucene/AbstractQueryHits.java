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

/**
 * <code>AbstractQueryHits</code> serves as a base class for {@link QueryHits}
 * implementations.
 */
public abstract class AbstractQueryHits implements QueryHits {

    /**
     * This default implementation does nothing.
     */
    public void close() throws IOException {
    }

    /**
     * Provides a default implementation:
     * <pre>
     * while (n-- &gt; 0) {
     *     if (nextScoreNode() == null) {
     *         return;
     *     }
     * }
     * </pre>
     * Sub classes may overwrite this method and implement are more efficient
     * way to skip hits.
     *
     * @param n the number of hits to skip.
     * @throws IOException if an error occurs while skipping.
     */
    public void skip(int n) throws IOException {
        while (n-- > 0) {
            if (nextScoreNode() == null) {
                return;
            }
        }
    }

    /**
     * This default implementation returns <code>-1</code>.
     *
     * @return <code>-1</code>.
     */
    public int getSize() {
        return -1;
    }
}
