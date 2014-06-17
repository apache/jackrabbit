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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;

/**
 * Wraps an <code>IndexReader</code> and allows to commit changes without
 * closing the reader.
 */
class CommittableIndexReader extends FilterIndexReader {

    /**
     * The maximum size of the delete history.
     */
    private static final int DELETE_HISTORY_SIZE = 1000;

    /**
     * A modification count on this index reader. Initialized with
     * {@link IndexReader#getVersion()} and incremented with every call to
     * {@link #doDelete(int)}.
     */
    private volatile long modCount;

    /**
     * The history of the most recent deletes.
     */
    private final List<Integer> deleteHistory = new LinkedList<Integer>();

    /**
    * The deleted docs for this index reader.
    */
    private final BitSet deletedDocs = new BitSet();

    /**
     * Creates a new <code>CommittableIndexReader</code> based on <code>in</code>.
     *
     * @param in the <code>IndexReader</code> to wrap.
     */
    CommittableIndexReader(IndexReader in) {
        super(in);
        modCount = in.getVersion();
        int maxDocs = in.maxDoc();
        for (int i = 0; i < maxDocs; i++) {
            if (in.isDeleted(i)) {
                deletedDocs.set(i);
            }
        }
    }

    //------------------------< FilterIndexReader >-----------------------------

    /**
     * {@inheritDoc}
     * <p>
     * Increments the modification count.
     */
    protected void doDelete(int n) throws CorruptIndexException, IOException {
        super.doDelete(n);
        modCount++;
        if (deleteHistory.size() >= DELETE_HISTORY_SIZE) {
            deleteHistory.remove(0);
        }
        deleteHistory.add(n);
        deletedDocs.set(n);
    }

    //------------------------< additional methods >----------------------------

    /**
     * @return the modification count of this index reader.
     */
    long getModificationCount() {
        return modCount;
    }

    /**
     * Returns the document numbers of deleted nodes since the given
     * <code>modCount</code>.
     *
     * @param modCount a modification count.
     * @return document numbers of deleted nodes or <code>null</code> if this
     *         index reader cannot provide those document number. e.g. modCount
     *         is too far back in the past.
     * @throws IllegalArgumentException if <code>modCount</code> is larger than
     *                                  {@link #getModificationCount()}.
     */
    Collection<Integer> getDeletedSince(long modCount)
            throws IllegalArgumentException {
        if (modCount > this.modCount) {
            throw new IllegalArgumentException("modCount: "
                    + modCount + " > " + this.modCount);
        }
        if (modCount == this.modCount) {
            return Collections.emptyList();
        }
        long num = this.modCount - modCount;
        if (num > deleteHistory.size()) {
            return null;
        }
        List<Integer> deletes = new ArrayList<Integer>((int) num);
        for (Integer d : deleteHistory.subList((int) (deleteHistory.size() - num),
                deleteHistory.size())) {
            deletes.add(d);
        }
        return deletes;
    }

    /**
     * Returns a copy of the deleted documents BitSet.
     * @return the deleted documents of this index reader.
     */
    BitSet getDeletedDocs() {
        return (BitSet) deletedDocs.clone();
    }

    @Override
    public String toString() {
      final StringBuilder buffer = new StringBuilder("CommittableIndexReader(");
      buffer.append(in);
      buffer.append(',');
      buffer.append(modCount);
      buffer.append(')');
      return buffer.toString();
    }
}
