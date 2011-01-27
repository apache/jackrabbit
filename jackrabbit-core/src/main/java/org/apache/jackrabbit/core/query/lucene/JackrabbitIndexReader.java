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
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;

import java.io.IOException;

/**
 * <code>JackrabbitIndexReader</code> wraps an index reader and
 * {@link ReleaseableIndexReader#release() releases} the underlying reader
 * when a client calls {@link #close()} on this reader. This allows reusing
 * of the underlying index reader instance.
 */
public final class JackrabbitIndexReader
        extends FilterIndexReader
        implements HierarchyResolver, MultiIndexReader {

    /**
     * The hierarchy resolver.
     */
    private final HierarchyResolver resolver;

    /**
     * The underlying index reader exposed as a {@link MultiIndexReader}.
     */
    private final MultiIndexReader reader;

    /**
     * Creates a new <code>JackrabbitIndexReader</code>. The passed index reader
     * must also implement the interfaces {@link HierarchyResolver} and
     * {@link MultiIndexReader}.
     *
     * @param in the underlying index reader.
     * @throws IllegalArgumentException if <code>in</code> does not implement
     *                                  {@link HierarchyResolver} and
     *                                  {@link MultiIndexReader}.
     */
    public JackrabbitIndexReader(IndexReader in) {
        super(in);
        if (!(in instanceof MultiIndexReader)) {
            throw new IllegalArgumentException("IndexReader must also implement MultiIndexReader");
        }
        if (!(in instanceof HierarchyResolver)) {
            throw new IllegalArgumentException("IndexReader must also implement HierarchyResolver");
        }
        this.resolver = (HierarchyResolver) in;
        this.reader = (MultiIndexReader) in;
    }

    /**
     * Overwrite <code>termDocs(Term)</code> and forward the call to the
     * wrapped reader.
     */
    @Override
    public TermDocs termDocs(Term term) throws IOException {
        return in.termDocs(term);
    }

    //--------------------------< FilterIndexReader >---------------------------

    /**
     * Calls release on the underlying {@link MultiIndexReader} instead of
     * closing it.
     *
     * @throws IOException if an error occurs while releaseing the underlying
     *                     index reader.
     */
    protected void doClose() throws IOException {
        reader.release();
    }

    //------------------------< HierarchyResolver >-----------------------------

    /**
     * {@inheritDoc}
     */
    public int[] getParents(int n, int[] docNumbers) throws IOException {
        return resolver.getParents(n, docNumbers);
    }

    //-------------------------< MultiIndexReader >-----------------------------

    /**
     * {@inheritDoc}
     */
    public IndexReader[] getIndexReaders() {
        return reader.getIndexReaders();
    }

    public IndexReader[] getSequentialSubReaders() {
      // No sequential sub-readers
      return null;
    }

    /**
     * {@inheritDoc}
     */
    public ForeignSegmentDocId createDocId(NodeId id) throws IOException {
        return reader.createDocId(id);
    }

    /**
     * {@inheritDoc}
     */
    public int getDocumentNumber(ForeignSegmentDocId docId) throws IOException {
        return reader.getDocumentNumber(docId);
    }

    /**
     * {@inheritDoc}
     */
    public void release() throws IOException {
        reader.release();
    }
}
