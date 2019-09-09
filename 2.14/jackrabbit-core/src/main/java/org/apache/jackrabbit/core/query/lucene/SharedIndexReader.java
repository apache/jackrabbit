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

import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.Term;

import java.util.BitSet;
import java.io.IOException;

/**
 * Implements an <code>IndexReader</code>, that will close when all connected
 * clients are disconnected AND the <code>SharedIndexReader</code>s
 * <code>close()</code> method itself has been called.
 */
class SharedIndexReader extends RefCountingIndexReader {

    /**
     * Creates a new <code>SharedIndexReader</code> which is based on
     * <code>in</code>.
     * @param in the underlying <code>IndexReader</code>.
     */
    public SharedIndexReader(CachingIndexReader in) {
        super(in);
    }

    /**
     * Returns the tick value when the underlying {@link CachingIndexReader} was
     * created.
     *
     * @return the creation tick for the underlying reader.
     */
    long getCreationTick() {
        return getBase().getCreationTick();
    }

    /**
     * Returns the <code>DocId</code> of the parent of <code>n</code> or
     * {@link DocId#NULL} if <code>n</code> does not have a parent
     * (<code>n</code> is the root node).
     *
     * @param n the document number.
     * @param deleted the documents that should be regarded as deleted.
     * @return the <code>DocId</code> of <code>n</code>'s parent.
     * @throws IOException if an error occurs while reading from the index.
     */
    public DocId getParent(int n, BitSet deleted) throws IOException {
        return getBase().getParent(n, deleted);
    }

    /**
     * Simply passes the call to the wrapped reader as is.<br/>
     * If <code>term</code> is for a {@link FieldNames#UUID} field and this
     * <code>SharedIndexReader</code> does not have such a document,
     * {@link EmptyTermDocs#INSTANCE} is returned.
     *
     * @param term the term to enumerate the docs for.
     * @return TermDocs for <code>term</code>.
     * @throws IOException if an error occurs while reading from the index.
     */
    public TermDocs termDocs(Term term) throws IOException {
        return in.termDocs(term);
    }

    /**
     * Returns the {@link CachingIndexReader} this reader is based on.
     *
     * @return the {@link CachingIndexReader} this reader is based on.
     */
    public CachingIndexReader getBase() {
        return (CachingIndexReader) in;
    }
}
