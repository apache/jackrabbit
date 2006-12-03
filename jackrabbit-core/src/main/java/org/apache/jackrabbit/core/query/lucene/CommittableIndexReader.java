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

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;

/**
 * Wraps an <code>IndexReader</code> and allows to commit changes without
 * closing the reader.
 */
class CommittableIndexReader extends FilterIndexReader {

    /**
     * Creates a new <code>CommittableIndexReader</code> based on <code>in</code>.
     *
     * @param in the <code>IndexReader</code> to wrap.
     */
    CommittableIndexReader(IndexReader in) {
        super(in);
    }

    /**
     * Commits the documents marked as deleted to disc.
     *
     * @throws IOException if an error occurs while writing.
     */
    void commitDeleted() throws IOException {
        commit();
    }
}
