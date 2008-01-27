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
package org.apache.jackrabbit.core.query.lucene.hits;

import java.util.Arrays;

/**
 * Uses an integer array to store the hit set. This implementation uses less
 * memory than {@link BitSetHits} if the total number of documents is high and
 * and the number of hits is low or if your hits doc numbers are mostly in the
 * upper part of your doc number range.
 * If you don't know about your hit distribution in advance use
 * {@link AdaptingHits} instead.
 */
public class ArrayHits implements Hits {

    private static final int INITIAL_SIZE = 100;
    private int[] hits;
    private int index;
    private boolean initialized;

    public ArrayHits() {
        this(INITIAL_SIZE);
    }

    public ArrayHits(int initialSize) {
        hits = new int[initialSize];
        index = 0;
        initialized = false;
    }

    private void initialize() {
        if (!initialized) {
            Arrays.sort(hits);
            index = hits.length - index;
            initialized = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void set(int doc) {
        if (initialized) {
            throw new IllegalStateException(
                    "You must not call set() after next() or skipTo()");
        }
        if (index >= hits.length) {
            int[] resizedHits = new int[hits.length * 2];
            System.arraycopy(hits, 0, resizedHits, 0, hits.length);
            hits = resizedHits;
        }
        hits[index++] = doc;
    }

    /**
     * {@inheritDoc}
     */
    public int next() {
        initialize();
        if (index >= hits.length) {
            return -1;
        } else {
            return hits[index++];
        }
    }

    /**
     * {@inheritDoc}
     */
    public int skipTo(int target) {
        initialize();
        for (int i = index; i < hits.length; i++) {
            int nextDocValue = hits[i];
            if (nextDocValue >= target) {
                index = i;
                return next();
            }
        }
        return -1;
    }
}
