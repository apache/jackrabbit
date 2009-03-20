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

/**
 * <code>EmptyTermDocs</code> implements a TermDocs, which is empty.
 */
class EmptyTermDocs implements TermDocs {

    /**
     * Single instance of this class.
     */
    public static final TermDocs INSTANCE = new EmptyTermDocs();

    private EmptyTermDocs() {
    }

    public void seek(Term term) {
    }

    public void seek(TermEnum termEnum) {
    }

    public int doc() {
        return -1;
    }

    public int freq() {
        return -1;
    }

    public boolean next() {
        return false;
    }

    public int read(int[] docs, int[] freqs) {
        return 0;
    }

    public boolean skipTo(int target) {
        return false;
    }

    public void close() {
    }
}
