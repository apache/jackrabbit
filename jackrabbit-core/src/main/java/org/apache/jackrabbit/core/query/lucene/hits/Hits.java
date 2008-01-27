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
 * Representation of a set of hits
 */
public interface Hits {

    /**
     * Marks the document with doc number <code>doc</code> as a hit.
     * Implementations may throw an exception if you call set() after next() or
     * skipTo() has been called.
     */
    void set(int doc);

    /**
     * Return the doc number of the next hit in the set. Subsequent calls never
     * return the same doc number.
     */
    int next() throws IOException;

    /**
     * Skips to the first match beyond the current whose document number is
     * greater than or equal to the given target. Returns -1 if there is no
     * matching document number greater than target.
     */
    int skipTo(int target) throws IOException;

}
