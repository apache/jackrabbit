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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.jackrabbit.uuid.UUID;

import java.io.IOException;
import java.util.BitSet;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * Implements a document id which can be based on a Node uuid or a lucene
 * document number.
 */
abstract class DocId {

    /**
     * Indicates a null DocId. Will be returned if the root node is asked for
     * its parent.
     */
    static final DocId NULL = new DocId() {

        /**
         * Always returns <code>-1</code>.
         * @param reader the index reader.
         * @return always <code>-1</code>.
         */
        final int getDocumentNumber(IndexReader reader) {
            return -1;
        }

        /**
         * Always returns <code>this</code>.
         * @param offset the offset to apply.
         * @return always <code>this</code>.
         */
        final DocId applyOffset(int offset) {
            return this;
        }

        /**
         * Always returns <code>true</code>.
         * @param deleted the deleted documents.
         * @return always <code>true</code>.
         */
        final boolean isValid(BitSet deleted) {
            return true;
        }
    };

    /**
     * Returns the document number of this <code>DocId</code>. If this id is
     * invalid <code>-1</code> is returned.
     *
     * @param reader the IndexReader to resolve this <code>DocId</code>.
     * @return the document number of this <code>DocId</code> or <code>-1</code>
     *         if it is invalid (e.g. does not exist).
     * @throws IOException if an error occurs while reading from the index.
     */
    abstract int getDocumentNumber(IndexReader reader) throws IOException;

    /**
     * Applies an offset to this <code>DocId</code>. The returned <code>DocId</code>
     * may be the same as <code>this</code> if this <code>DocId</code> does
     * not need to know about an offset.
     *
     * @param offset the offset to apply to.
     * @return <code>DocId</code> with <code>offset</code> applied.
     */
    abstract DocId applyOffset(int offset);

    /**
     * Returns <code>true</code> if this <code>DocId</code> is valid against the
     * set of <code>deleted</code> documents; otherwise <code>false</code>.
     *
     * @param deleted the deleted documents.
     * @return <code>true</code> if this <code>DocId</code> is not delted;
     *         otherwise <code>false</code>.
     */
    abstract boolean isValid(BitSet deleted);

    /**
     * Creates a <code>DocId</code> based on a document number.
     *
     * @param docNumber the document number.
     * @return a <code>DocId</code> based on a document number.
     */
    static DocId create(int docNumber) {
        return new PlainDocId(docNumber);
    }

    /**
     * Creates a <code>DocId</code> based on a node UUID.
     *
     * @param uuid the node uuid.
     * @return a <code>DocId</code> based on a node UUID.
     */
    static DocId create(String uuid) {
        return new UUIDDocId(uuid);
    }

    //--------------------------< internal >------------------------------------

    /**
     * <code>DocId</code> based on a document number.
     */
    private static final class PlainDocId extends DocId {

        /**
         * The document number or <code>-1</code> if not set.
         */
        private final int docNumber;

        /**
         * Creates a <code>DocId</code> based on a document number.
         *
         * @param docNumber the lucene document number.
         */
        PlainDocId(int docNumber) {
            this.docNumber = docNumber;
        }

        /**
         * @inheritDoc
         */
        int getDocumentNumber(IndexReader reader) {
            return docNumber;
        }

        /**
         * @inheritDoc
         */
        DocId applyOffset(int offset) {
            return new PlainDocId(docNumber + offset);
        }

        /**
         * @inheritDoc
         */
        boolean isValid(BitSet deleted) {
            return !deleted.get(docNumber);
        }

        /**
         * Returns a String representation for this <code>DocId</code>.
         *
         * @return a String representation for this <code>DocId</code>.
         */
        public String toString() {
            return "PlainDocId(" + docNumber + ")";
        }
    }

    /**
     * <code>DocId</code> based on a UUID.
     */
    private static final class UUIDDocId extends DocId {

        /**
         * The node uuid.
         */
        private final UUID uuid;

        /**
         * The index reader that was used to calculate the document number.
         * If <code>null</code> then the document number has not yet been
         * calculated.
         */
        private Reference reader;

        /**
         * The previously calculated document number.
         */
        private int docNumber;

        /**
         * Creates a <code>DocId</code> based on a Node uuid.
         *
         * @param uuid the Node uuid.
         * @throws IllegalArgumentException if the <code>uuid</code> is
         *                                  malformed.
         */
        UUIDDocId(String uuid) {
            this.uuid = UUID.fromString(uuid);
        }

        /**
         * @inheritDoc
         */
        int getDocumentNumber(IndexReader reader) throws IOException {
            synchronized (this) {
                if (this.reader != null && reader.equals(this.reader.get())) {
                    return docNumber;
                }
            }
            Term id = new Term(FieldNames.UUID, uuid.toString());
            TermDocs docs = reader.termDocs(id);
            int doc = -1;
            try {
                if (docs.next()) {
                    doc = docs.doc();
                }
            } finally {
                docs.close();
            }
            synchronized (this) {
                docNumber = doc;
                this.reader = new WeakReference(reader);
            }
            return doc;
        }

        /**
         * This implementation will return <code>this</code>. Document number is
         * not known until resolved in {@link #getDocumentNumber(IndexReader)}.
         *
         * @inheritDoc
         */
        DocId applyOffset(int offset) {
            return this;
        }

        /**
         * Always returns <code>true</code>.
         *
         * @param deleted the deleted documents.
         * @return always <code>true</code>.
         */
        boolean isValid(BitSet deleted) {
            return true;
        }

        /**
         * Returns a String representation for this <code>DocId</code>.
         *
         * @return a String representation for this <code>DocId</code>.
         */
        public String toString() {
            return "UUIDDocId(" + uuid + ")";
        }
    }
}
