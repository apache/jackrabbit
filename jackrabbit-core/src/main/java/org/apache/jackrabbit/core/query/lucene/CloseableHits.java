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
 * Defines an interface for query hits that need to be closed when done reading
 * from it. A client will call {@link #close()} to release resources after a
 * query has been executed and the results have been read.
 */
public interface CloseableHits {

    /**
     * Releases resources held by this hits instance.
     *
     * @throws IOException if an error occurs while releasing resources.
     */
    void close() throws IOException;

    /**
     * @return the number of results or <code>-1</code> if the size is unknown.
     */
    int getSize();

    /**
     * Skips a <code>n</code> score nodes.
     *
     * @param n the number of score nodes to skip.
     * @throws IOException if an error occurs while skipping.
     */
    void skip(int n) throws IOException;
}
