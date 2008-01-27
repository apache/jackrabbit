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

import java.io.IOException;

/**
 * Creates the intersection of two hit sets.
 */
public class HitsIntersection implements Hits {

    private final Hits hits1;
    private final Hits hits2;

    private int nextChildrenHit = -1;
    private int nextNameTestHit = -1;

    public HitsIntersection(Hits hits1, Hits hits2) {
        this.hits1 = hits1;
        this.hits2 = hits2;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int doc) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public int next() throws IOException {
        do {
            if (nextChildrenHit == nextNameTestHit) {
                nextNameTestHit = hits2.next();
                nextChildrenHit = hits1.next();
            } else if (nextNameTestHit < nextChildrenHit) {
                nextNameTestHit = hits2.skipTo(nextChildrenHit);
            } else {
                nextChildrenHit = hits1.skipTo(nextNameTestHit);
            }
        } while (nextChildrenHit > -1 && nextNameTestHit > -1
                && nextNameTestHit != nextChildrenHit);

        int nextDoc = -1;
        if (nextChildrenHit == nextNameTestHit) {
            nextDoc = nextChildrenHit;
        }
        return nextDoc;
    }

    /**
     * {@inheritDoc}
     */
    public int skipTo(int target) throws IOException {
        nextChildrenHit = hits1.skipTo(target);
        nextNameTestHit = hits2.skipTo(target);
        if (nextChildrenHit == nextNameTestHit) {
            return nextChildrenHit;
        } else {
            return next();
        }
    }

}
