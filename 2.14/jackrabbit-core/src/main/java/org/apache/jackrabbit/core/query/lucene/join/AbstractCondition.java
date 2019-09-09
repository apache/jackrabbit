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
package org.apache.jackrabbit.core.query.lucene.join;

import java.util.Arrays;
import java.io.IOException;

import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.spi.Name;

/**
 * <code>AbstractCondition</code> is a base class for join conditions.
 */
public abstract class AbstractCondition implements Condition {

    /**
     * The inner query hits.
     */
    protected final MultiColumnQueryHits inner;

    /**
     * Creates a new join condition with the given <code>inner</code> query
     * hits.
     *
     * @param inner the inner query hits.
     */
    public AbstractCondition(MultiColumnQueryHits inner) {
        this.inner = inner;
    }

    /**
     * @return selector names of the inner query hits.
     */
    public Name[] getInnerSelectorNames() {
        return inner.getSelectorNames();
    }

    /**
     * Closes this join condition and frees resources. Namely closes the inner
     * query hits.
     *
     * @throws IOException if an error occurs while closing the inner query
     *                     hits.
     */
    public void close() throws IOException {
        inner.close();
    }

    /**
     * Returns the index of the selector with the given <code>selectorName</code>
     * within the given <code>source</code>.
     *
     * @param source       a source.
     * @param selectorName a selector name.
     * @return the index within the source or <code>-1</code> if the name does
     *         not exist in <code>source</code>.
     */
    protected static int getIndex(MultiColumnQueryHits source, Name selectorName) {
        return Arrays.asList(source.getSelectorNames()).indexOf(selectorName);
    }
}
