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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implementation of Hits which starts with marking hits in an
 * ArrayHits instance and switches to a BitSetHits instance if at least the
 * threshold of 8kb for the ArrayHits is reached and a BitSetHits instance
 * would consume less memory.
 */
public class AdaptingHits implements Hits {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(AdaptingHits.class);

    /**
     * The lower threshold before a conversion is tried
     */
    private static final int DEFAULT_THRESHOLD = 2048;

    /**
     * Internal hits instance
     */
    private Hits hits;

    /**
     * The maximum doc number in hits. Used to calculate the expected
     * BitSetHits memory footprint.
     */
    private int maxDoc;

    /**
     * The total number of hits. Used to calculate the memory footprint of the
     * initial ArrayHits instance.
     */
    private int docCount;

    private int threshold;

    public AdaptingHits() {
        this(DEFAULT_THRESHOLD);
    }

    public AdaptingHits(int threshold) {
        this.threshold = threshold;
        hits = new ArrayHits();
        maxDoc = 0;
    }

    /**
     * {@inheritDoc}
     */
    public int next() throws IOException {
        // delegate to the internal Hits instance
        return hits.next();
    }

    /**
     * {@inheritDoc}
     */
    public void set(int doc) {
        hits.set(doc);
        docCount++;
        if (doc > maxDoc) {
            maxDoc = doc;
        }

        if (docCount > threshold && (hits instanceof ArrayHits)) {
            int intArraySize = docCount * 4;
            int bitSetSize = maxDoc / 8;
            if (bitSetSize < intArraySize) {
                log.debug("BitSet is smaller than int[]: "
                        + bitSetSize + " vs " + intArraySize);
                BitSetHits bitSetHits = new BitSetHits();
                int i = 0;
                while (i > -1) {
                    try {
                        i = hits.next();
                        if (i > -1) {
                            bitSetHits.set(i);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                hits = bitSetHits;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public int skipTo(int target) throws IOException {
        // delegate to the internal Hits instance
        return hits.skipTo(target);
    }

    Hits getInternalHits() {
        return hits;
    }

}
