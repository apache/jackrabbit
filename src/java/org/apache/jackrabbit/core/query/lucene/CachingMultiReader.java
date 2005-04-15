/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;

import java.io.IOException;

/**
 * Extends a <code>MultiReader</code> with support for cached <code>TermDocs</code>
 * on {@link FieldNames#UUID} field.
 */
class CachingMultiReader extends MultiReader {

    /**
     * The sub readers.
     */
    private IndexReader[] subReaders;

    /**
     * Doc number starts for each sub reader
     */
    private int[] starts;

    /**
     * Creates a new <code>CachingMultiReader</code> based on sub readers.
     * <p/>
     * This <code>CachingMultiReader</code> poses type requirements on the
     * <code>subReaders</code>: all but one sub readers must be a
     * {@link CachingIndexReader}. The single allowed sub reader not of type
     * {@link CachingIndexReader} must be the last reader in
     * <code>subReaders</code>! Otherwise this constructor will throw an
     * {@link IllegalArgumentException}.
     *
     * @param subReaders the sub readers.
     * @throws IOException if an error occurs while reading from the indexes.
     * @exception IllegalArgumentException if <code>subReaders</code> does
     * not comply to the above type requirements.
     */
    public CachingMultiReader(IndexReader[] subReaders) throws IOException {
        super(subReaders);
        // check readers, all but last must be a CachingIndexReader
        for (int i = 0; i < subReaders.length - 1; i++) {
            if (!(subReaders[i] instanceof CachingIndexReader)) {
                throw new IllegalArgumentException("subReader " + i + " must be of type CachingIndexReader");
            }
        }
        this.subReaders = subReaders;
        starts = new int[subReaders.length + 1];
        int maxDoc = 0;
        for (int i = 0; i < subReaders.length; i++) {
            starts[i] = maxDoc;
            maxDoc += subReaders[i].maxDoc();
        }
        starts[subReaders.length] = maxDoc;
    }

    /**
     * {@inheritDoc}
     */
    public TermDocs termDocs(Term term) throws IOException {
        if (term.field() == FieldNames.UUID) {
            for (int i = 0; i < subReaders.length; i++) {
                TermDocs docs = subReaders[i].termDocs(term);
                if (docs != CachingIndexReader.EMPTY) {
                    // apply offset
                    return new OffsetTermDocs(docs, starts[i]);
                }
            }
        }
        return super.termDocs(term);
    }

    /**
     * Partial <code>TermDocs</code> implementation that applies an offset
     * to a base <code>TermDocs</code> instance.
     */
    private static final class OffsetTermDocs implements TermDocs {

        /**
         * The base <code>TermDocs</code> instance.
         */
        private final TermDocs base;

        /**
         * The offset to apply
         */
        private final int offset;

        /**
         * Creates a new <code>OffsetTermDocs</code> instance.
         * @param base the base <code>TermDocs</code>.
         * @param offset the offset to apply.
         */
        OffsetTermDocs(TermDocs base, int offset) {
            this.base = base;
            this.offset = offset;
        }

        /**
         * @throws UnsupportedOperationException always
         */
        public void seek(Term term) {
            throw new UnsupportedOperationException();
        }

        /**
         * @throws UnsupportedOperationException always
         */
        public void seek(TermEnum termEnum) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public int doc() {
            return base.doc() + offset;
        }

        /**
         * {@inheritDoc}
         */
        public int freq() {
            return base.freq();
        }

        /**
         * @throws UnsupportedOperationException always
         */
        public boolean next() throws IOException {
            return base.next();
        }

        /**
         * @throws UnsupportedOperationException always
         */
        public int read(int[] docs, int[] freqs) {
            throw new UnsupportedOperationException();
        }

        /**
         * @throws UnsupportedOperationException always
         */
        public boolean skipTo(int target) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            base.close();
        }
    }
}
