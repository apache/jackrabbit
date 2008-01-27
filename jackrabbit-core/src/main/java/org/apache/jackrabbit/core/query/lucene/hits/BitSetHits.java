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

import java.util.BitSet;

/**
 * Uses a BitSet instance to store the hit set. Keep in mind that this BitSet
 * is at least as large as the highest doc number in the hit set. This means it
 * might need of lot of memory for large indexes.
 */
public class BitSetHits implements Hits {
    private BitSet hits;
    private int index;

    public BitSetHits() {
        hits = new BitSet();
        index = 0;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int doc) {
        hits.set(doc);
    }

    /**
     * {@inheritDoc}
     */
    public int next() {
        int result = hits.nextSetBit(index);
        index = result + 1;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int skipTo(int target) {
        index = target;
        return next();
    }
}
