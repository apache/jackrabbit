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

import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;

import java.io.IOException;

/**
 * Implements a TermDocs with a single document.
 */
class SingleTermDocs implements TermDocs {

    /**
     * Single document number;
     */
    private final int doc;

    /**
     * Flag to return the document number once.
     */
    private boolean next = true;

    /**
     * Creates a <code>SingleTermDocs</code> that returns <code>doc</code> as
     * its single document.
     *
     * @param doc the document number.
     */
    SingleTermDocs(int doc) {
        this.doc = doc;
    }

    /**
     * @throws UnsupportedOperationException always
     */
    public void seek(Term term) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always
     */
    public void seek(TermEnum termEnum) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public int doc() {
        return doc;
    }

    /**
     * {@inheritDoc}
     */
    public int freq() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    public boolean next() throws IOException {
        boolean hasNext = next;
        next = false;
        return hasNext;
    }

    /**
     * {@inheritDoc}
     */
    public int read(int[] docs, int[] freqs) throws IOException {
        if (next && docs.length > 0) {
            docs[0] = doc;
            freqs[0] = 1;
            next = false;
            return 1;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean skipTo(int target) throws IOException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
    }
}
