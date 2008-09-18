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
import org.apache.lucene.index.CorruptIndexException;

import java.io.IOException;

/**
 * Wraps an <code>IndexReader</code> and allows to commit changes without
 * closing the reader.
 */
class CommittableIndexReader extends FilterIndexReader {

    /**
     * A modification count on this index reader. Initialied with
     * {@link IndexReader#getVersion()} and incremented with every call to
     * {@link #doDelete(int)}.
     */
    private volatile long modCount;

    /**
     * Creates a new <code>CommittableIndexReader</code> based on <code>in</code>.
     *
     * @param in the <code>IndexReader</code> to wrap.
     */
    CommittableIndexReader(IndexReader in) {
        super(in);
        modCount = in.getVersion();
    }

    //------------------------< FilterIndexReader >-----------------------------

    /**
     * {@inheritDoc}
     * <p/>
     * Increments the modification count.
     */
    protected void doDelete(int n) throws CorruptIndexException, IOException {
        super.doDelete(n);
        modCount++;
    }

    //------------------------< additional methods >----------------------------

    /**
     * @return the modification count of this index reader.
     */
    long getModificationCount() {
        return modCount;
    }
}
