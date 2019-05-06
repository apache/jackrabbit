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
 * <code>ReleaseableIndexReader</code>...
 */
public interface ReleaseableIndexReader {

    /**
     * Releases this index reader and potentially frees resources. In contrast
     * to {@link org.apache.lucene.index.IndexReader#close()} this method
     * does not necessarily close the index reader, but gives the implementation
     * the opportunity to do reference counting.
     *
     * @throws IOException if an error occurs while releasing the index reader.
     */
    public void release() throws IOException;
}
