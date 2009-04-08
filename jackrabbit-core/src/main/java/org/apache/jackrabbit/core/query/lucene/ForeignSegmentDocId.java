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

/**
 * A <code>DocId</code> that contains a document number and the creation tick
 * of the index segment.
 */
final class ForeignSegmentDocId extends DocId {

    /**
     * Empty array of {@link ForeignSegmentDocId}s.
     */
    static final ForeignSegmentDocId[] EMPTY_ARRAY = new ForeignSegmentDocId[0];

    /**
     * The document number.
     */
    private final int docNumber;

    /**
     * The creation tick of the index segment.
     */
    private final long creationTick;

    /**
     * Creates a <code>DocId</code> based on a document number in the index
     * segment with the given <code>creationTick</code>.
     *
     * @param docNumber    the lucene document number.
     * @param creationTick the creation tick of the index segment.
     */
    ForeignSegmentDocId(int docNumber, long creationTick) {
        this.docNumber = docNumber;
        this.creationTick = creationTick;
    }

    /**
     * @return the document number in the foreign index segment.
     */
    int getDocNumber() {
        return docNumber;
    }

    /**
     * @return the creation tick of the foreign index segment.
     */
    long getCreationTick() {
        return creationTick;
    }

    /**
     * @inheritDoc
     */
    int[] getDocumentNumbers(MultiIndexReader reader, int[] docNumbers) throws IOException {
        int doc = reader.getDocumentNumber(this);
        if (doc == -1) {
            return EMPTY;
        } else {
            if (docNumbers.length == 1) {
                docNumbers[0] = doc;
                return docNumbers;
            } else {
                return new int[]{doc};
            }
        }
    }

    /**
     * This implementation will return <code>this</code>. Document number is
     * not known until resolved in {@link DocId#getDocumentNumbers(MultiIndexReader,int[])}.
     *
     * {@inheritDoc}
     */
    DocId applyOffset(int offset) {
        return this;
    }

    /**
     * Always returns <code>true</code> because this calls is in context of the
     * index segment where this DocId lives. Within this segment this DocId is
     * always valid. Whether the target of this DocId is valid can only be
     * checked in the method {@link DocId#getDocumentNumbers(MultiIndexReader,int[])}.
     *
     * @param deleted the deleted documents in the segment where this DocId
     *                lives.
     * @return always <code>true</code>.
     */
    boolean isValid(BitSet deleted) {
        return true;
    }
}
