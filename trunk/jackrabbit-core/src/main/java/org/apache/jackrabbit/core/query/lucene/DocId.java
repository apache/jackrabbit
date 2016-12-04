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
import java.util.BitSet;

import org.apache.jackrabbit.core.id.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a document id which can be based on a Node uuid or a lucene
 * document number.
 */
abstract class DocId {

    static final int[] EMPTY = new int[0];

    /**
     * All DocIds with a value smaller than {@link Short#MAX_VALUE}.
     */
    private static final PlainDocId[] LOW_DOC_IDS = new PlainDocId[Short.MAX_VALUE];

    static {
        for (int i = 0; i < LOW_DOC_IDS.length; i++) {
            LOW_DOC_IDS[i] = new PlainDocId(i);
        }
    }

    /**
     * Indicates a null DocId. Will be returned if the root node is asked for
     * its parent.
     */
    static final DocId NULL = new DocId() {

        /**
         * Always returns an empty array.
         * @param reader the index reader.
         * @param docNumbers a int array for reuse as return value.
         * @return always an empty array.
         */
        final int[] getDocumentNumbers(MultiIndexReader reader,
                                       int[] docNumbers) {
            return EMPTY;
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
     * Returns the document numbers of this <code>DocId</code>. An empty array
     * is returned if this id is invalid.
     *
     * @param reader     the IndexReader to resolve this <code>DocId</code>.
     * @param docNumbers an array for reuse. An implementation should use the
     *                   passed array as a container for the return value,
     *                   unless the length of the returned array is different
     *                   from <code>docNumbers</code>. In which case an
     *                   implementation will create a new array with an
     *                   appropriate size.
     * @return the document numbers of this <code>DocId</code> or
     *         empty if it is invalid (e.g. does not exist).
     * @throws IOException if an error occurs while reading from the index.
     */
    abstract int[] getDocumentNumbers(MultiIndexReader reader, int[] docNumbers)
            throws IOException;

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
        if (docNumber < Short.MAX_VALUE) {
            // use cached values for docNumbers up to 32k
            return LOW_DOC_IDS[docNumber];
        } else {
            return new PlainDocId(docNumber);
        }
    }

    /**
     * Creates a <code>DocId</code> based on a UUID.
     *
     * @param uuid the UUID
     * @return a <code>DocId</code> based on the UUID.
     * @throws IllegalArgumentException if the <code>uuid</code> is malformed.
     */
    static DocId create(String uuid) {
        return create(new NodeId(uuid));
    }

    /**
     * Creates a <code>DocId</code> based on a node id.
     *
     * @param id the node id
     * @return a <code>DocId</code> based on the node id
     */
    static DocId create(NodeId id) {
        return new UUIDDocId(id);
    }

    /**
     * Creates a <code>DocId</code> that references multiple UUIDs.
     *
     * @param uuids the UUIDs of the referenced nodes.
     * @return a <code>DocId</code> based on multiple node UUIDs.
     */
    static DocId create(String[] uuids)  {
        return new MultiUUIDDocId(uuids);
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
        int[] getDocumentNumbers(MultiIndexReader reader, int[] docNumbers) {
            if (docNumbers.length == 1) {
                docNumbers[0] = docNumber;
                return docNumbers;
            } else {
                return new int[]{docNumber};
            }
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
     * <code>DocId</code> based on a node id.
     */
    private static final class UUIDDocId extends DocId {

        /**
         * The logger instance for this class.
         */
        private static final Logger log = LoggerFactory.getLogger(UUIDDocId.class);

        /**
         * The node identifier.
         */
        private final NodeId id;

        /**
         * The previously calculated foreign segment document id.
         */
        private ForeignSegmentDocId doc;

        /**
         * Creates a <code>DocId</code> based on a node id.
         *
         * @param id the node id.
         */
        UUIDDocId(NodeId id) {
            this.id = id;
        }

        /**
         * @inheritDoc
         */
        int[] getDocumentNumbers(MultiIndexReader reader, int[] docNumbers)
                throws IOException {
            int realDoc = -1;
            ForeignSegmentDocId segDocId = doc;
            if (segDocId != null) {
                realDoc = reader.getDocumentNumber(segDocId);
            }
            if (realDoc == -1) {
                // Cached doc was invalid => create new one
                segDocId = reader.createDocId(id);
                if (segDocId != null) {
                    realDoc = reader.getDocumentNumber(segDocId);
                    doc = segDocId;
                } else {
                    log.warn("Unknown parent node with id {}", id);
                    return EMPTY;
                }
            }

            if (docNumbers.length == 1) {
                docNumbers[0] = realDoc;
                return docNumbers;
            } else {
                return new int[]{realDoc};
            }
        }

        /**
         * This implementation will return <code>this</code>. Document number is
         * not known until resolved in {@link #getDocumentNumbers(MultiIndexReader,int[])}.
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
            return "UUIDDocId(" + id + ")";
        }
    }

    /**
     * A DocId based on multiple UUIDDocIds.
     */
    private static final class MultiUUIDDocId extends DocId {

        /**
         * The internal uuid based doc ids.
         */
        private final UUIDDocId[] docIds;

        /**
         * @param uuids the uuids of the referenced nodes.
         * @throws IllegalArgumentException if one of the uuids is malformed.
         */
        MultiUUIDDocId(String[] uuids) {
            this.docIds = new UUIDDocId[uuids.length];
            for (int i = 0; i < uuids.length; i++) {
                docIds[i] = new UUIDDocId(new NodeId(uuids[i]));
            }
        }

        /**
         * @inheritDoc
         */
        int[] getDocumentNumbers(MultiIndexReader reader, int[] docNumbers)
                throws IOException {
            int[] tmp = new int[1];
            docNumbers = new int[docIds.length];
            for (int i = 0; i < docNumbers.length; i++) {
                docNumbers[i] = docIds[i].getDocumentNumbers(reader, tmp)[0];
            }
            return docNumbers;
        }

        /**
         * This implementation will return <code>this</code>. Document number is
         * not known until resolved in {@link #getDocumentNumbers(MultiIndexReader,int[])}.
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
            StringBuffer sb = new StringBuffer("MultiUUIDDocId(");
            String separator = "";
            for (UUIDDocId docId : docIds) {
                sb.append(separator);
                separator = ", ";
                sb.append(docId.id);
            }
            sb.append(")");
            return sb.toString();
        }
    }
}
