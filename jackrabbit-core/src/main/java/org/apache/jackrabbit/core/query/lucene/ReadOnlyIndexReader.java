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
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;

import java.util.BitSet;
import java.io.IOException;

/**
 * Overwrites the methods that would modify the index and throws an
 * {@link UnsupportedOperationException} in each of those methods. A
 * <code>ReadOnlyIndexReader</code> will always show all documents that have
 * not been deleted at the time when the index reader is created.
 */
class ReadOnlyIndexReader extends FilterIndexReader {

    /**
     * The underlying shared reader.
     */
    private final SharedIndexReader reader;

    /**
     * The deleted documents as initially read from the IndexReader passed
     * in the constructor of this class.
     */
    private final BitSet deleted;

    /**
     * Creates a new index reader based on <code>reader</code> at
     * <code>modificationTick</code>.
     * @param reader the underlying <code>IndexReader</code>.
     * @param deleted the documents that are deleted in <code>reader</code>.
     */
    public ReadOnlyIndexReader(SharedIndexReader reader,
                               BitSet deleted) {
        super(reader);
        this.reader = reader;
        this.deleted = deleted;
        // register this
        reader.addClient(this);
    }

    /**
     * Returns the <code>DocId</code> of the parent of <code>n</code> or
     * {@link DocId#NULL} if <code>n</code> does not have a parent
     * (<code>n</code> is the root node).
     *
     * @param n the document number.
     * @return the <code>DocId</code> of <code>n</code>'s parent.
     * @throws IOException if an error occurs while reading from the index.
     */
    public DocId getParent(int n) throws IOException {
        return getBase().getParent(n, deleted);
    }

    /**
     * Returns the {@link SharedIndexReader} this reader is based on.
     *
     * @return the {@link SharedIndexReader} this reader is based on.
     */
    public SharedIndexReader getBase() {
        return (SharedIndexReader) in;
    }

    //---------------------< IndexReader overwrites >---------------------------

    /**
     * Returns true if document <code>n</code> has been deleted
     * @param n the document number
     * @return true if document <code>n</code> has been deleted
     */
    public boolean isDeleted(int n) {
        return deleted.get(n);
    }

    /**
     * Returns <code>true</code> if any documents have been deleted.
     *
     * @return <code>true</code> if any documents have been deleted.
     */
    public boolean hasDeletions() {
        return !deleted.isEmpty();
    }

    /**
     * Returns the number of documents in this index reader.
     *
     * @return the number of documents in this index reader.
     */
    public int numDocs() {
        return maxDoc() - deleted.cardinality();
    }

    /**
     * @exception UnsupportedOperationException always
     */
    protected final void doDelete(int docNum) {
        throw new UnsupportedOperationException("IndexReader is read-only");
    }

    /**
     * @exception UnsupportedOperationException always
     */
    protected final void doUndeleteAll() {
        throw new UnsupportedOperationException("IndexReader is read-only");
    }

    /**
     * @exception UnsupportedOperationException always
     */
    protected final void doCommit() {
        throw new UnsupportedOperationException("IndexReader is read-only");
    }

    /**
     * Unregisters this reader from the shared index reader. Specifically, this
     * method does <b>not</b> close the underlying index reader, because it is
     * shared by multiple <code>ReadOnlyIndexReader</code>s.
     * @throws IOException if an error occurs while closing the reader.
     */
    protected void doClose() throws IOException {
        reader.removeClient(this);
    }

    /**
     * Wraps the underlying <code>TermDocs</code> and filters out documents
     * marked as deleted.<br/>
     * If <code>term</code> is for a {@link FieldNames#UUID} field and this
     * <code>ReadOnlyIndexReader</code> does not have such a document,
     * {@link CachingIndexReader#EMPTY} is returned.
     *
     * @param term the term to enumerate the docs for.
     * @return TermDocs for <code>term</code>.
     * @throws IOException if an error occurs while reading from the index.
     */
    public TermDocs termDocs(Term term) throws IOException {
        // do not wrap for empty TermDocs
        TermDocs td = reader.termDocs(term);
        if (td != CachingIndexReader.EMPTY) {
            td = new FilteredTermDocs(td);
        }
        return td;
    }

    /**
     * Wraps the underlying <code>TermDocs</code> and filters out documents
     * marked as deleted.
     *
     * @return TermDocs over the whole index.
     * @throws IOException if an error occurs while reading from the index.
     */
    public TermDocs termDocs() throws IOException {
        return new FilteredTermDocs(super.termDocs());
    }

    /**
     * Wraps the underlying <code>TermPositions</code> and filters out documents
     * marked as deleted.
     *
     * @return TermPositions over the whole index.
     * @throws IOException if an error occurs while reading from the index.
     */
    public TermPositions termPositions() throws IOException {
        return new FilteredTermPositions(super.termPositions());
    }

    //----------------------< FilteredTermDocs >--------------------------------

    /**
     * Filters a wrapped TermDocs by omitting documents marked as deleted.
     */
    private class FilteredTermDocs extends FilterTermDocs {

        /**
         * Creates a new filtered TermDocs based on <code>in</code>.
         *
         * @param in the TermDocs to filter.
         */
        public FilteredTermDocs(TermDocs in) {
            super(in);
        }

        /**
         * @inheritDoc
         */
        public boolean next() throws IOException {
            boolean hasNext = super.next();
            while (hasNext && deleted.get(super.doc())) {
                hasNext = super.next();
            }
            return hasNext;
        }

        /**
         * @inheritDoc
         */
        public int read(int[] docs, int[] freqs) throws IOException {
            int count;
            for (count = 0; count < docs.length && next(); count++) {
                docs[count] = doc();
                freqs[count] = freq();
            }
            return count;
        }

        /**
         * @inheritDoc
         */
        public boolean skipTo(int i) throws IOException {
            boolean exists = super.skipTo(i);
            while (exists && deleted.get(doc())) {
                exists = next();
            }
            return exists;
        }
    }

    //---------------------< FilteredTermPositions >----------------------------

    /**
     * Filters a wrapped TermPositions by omitting documents marked as deleted.
     */
    private final class FilteredTermPositions extends FilteredTermDocs
            implements TermPositions {

        /**
         * Creates a new filtered TermPositions based on <code>in</code>.
         *
         * @param in the TermPositions to filter.
         */
        public FilteredTermPositions(TermPositions in) {
            super(in);
        }

        /**
         * @inheritDoc
         */
        public int nextPosition() throws IOException {
            return ((TermPositions) this.in).nextPosition();
        }
    }
}
