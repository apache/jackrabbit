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

import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.ReaderUtil;

import java.io.IOException;

/**
 * <code>RefCountingIndexReader</code>...
 */
public class RefCountingIndexReader
        extends FilterIndexReader
        implements ReleaseableIndexReader {

    /**
     * A reference counter. When constructed the refCount is one.
     */
    private int refCount = 1;

    public RefCountingIndexReader(IndexReader in) {
        super(in);
    }

    /**
     * Increments the reference count on this index reader. The reference count
     * is decremented on {@link #release()}.
     */
    synchronized final void acquire() {
        refCount++;
    }

    /**
     * @return the current reference count value.
     */
    public synchronized int getRefCountJr() {
        return refCount;
    }

    //-----------------------< ReleaseableIndexReader >--------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized final void release() throws IOException {
        if (--refCount == 0) {
            close();
        }
    }

    //-----------------------< FilterIndexReader >--------------------------

    @Override
    public IndexReader[] getSequentialSubReaders() {
        return null;
    }

    @Override
    public FieldInfos getFieldInfos() {
        return ReaderUtil.getMergedFieldInfos(in);
    }

    protected void doClose() throws IOException {
        Util.closeOrRelease(in);
    }
}
