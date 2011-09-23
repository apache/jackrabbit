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

import org.apache.jackrabbit.uuid.UUID;

import java.io.IOException;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a document id which can be based on a Node uuid or a lucene
 * document number.
 */
abstract class DocId {

    static final int[] EMPTY = new int[0];

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
        return new PlainDocId(docNumber);
    }

    /**
     * Creates a <code>DocId</code> based on a node UUID.
     *
     * @param uuid the node uuid.
     * @return a <code>DocId</code> based on a node UUID.
     * @throws IllegalArgumentException if the <code>uuid</code> is malformed.
     */
    static DocId create(String uuid) {
        return create(UUID.fromString(uuid));
    }

    /**
     * Creates a <code>DocId</code> based on a node UUID.
     *
     * @param uuid the node uuid.
     * @return a <code>DocId</code> based on a node UUID.
     */
    static DocId create(UUID uuid) {
        return new UUIDDocId(uuid);
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
     * <code>DocId</code> based on a UUID.
     */
    private static final class UUIDDocId extends DocId {

        /**
         * The logger instance for this class.
         */
        private static final Logger log = LoggerFactory.getLogger(UUIDDocId.class);

        /**
         * The least significant 64 bits of the uuid (bytes 8-15)
         */
        private final long lsb;

        /**
         * The most significant 64 bits of the uuid (bytes 0-7)
         */
        private final long msb;

        /**
         * The previously calculated foreign segment document id.
         */
        private ForeignSegmentDocId doc;

        /**
         * Creates a <code>DocId</code> based on a Node uuid.
         *
         * @param uuid the Node uuid.
         */
        UUIDDocId(UUID uuid) {
            this.lsb = uuid.getLeastSignificantBits();
            this.msb = uuid.getMostSignificantBits();
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
                segDocId = reader.createDocId(new UUID(msb, lsb));
                if (segDocId != null) {
                    realDoc = reader.getDocumentNumber(segDocId);
                    doc = segDocId;
                } else {
                    log.warn("Unknown parent node with id {}", segDocId);
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
            return "UUIDDocId(" + new UUID(msb, lsb) + ")";
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
                docIds[i] = new UUIDDocId(UUID.fromString(uuids[i]));
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
            for (int i = 0; i < docIds.length; i++) {
                sb.append(separator);
                separator = ", ";
                sb.append(new UUID(docIds[i].msb, docIds[i].lsb));
            }
            sb.append(")");
            return sb.toString();
        }
    }
}
