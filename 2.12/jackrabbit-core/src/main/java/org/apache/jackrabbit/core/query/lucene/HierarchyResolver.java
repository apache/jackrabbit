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

/**
 * <code>HierarchyResolver</code> extends an {@link org.apache.lucene.index.IndexReader}
 * with the ability to resolve a JCR hierarchy.
 */
public interface HierarchyResolver {

    /**
     * Returns the document number of the parent of <code>n</code> or an empty
     * array if <code>n</code> does not have a parent (<code>n</code> is the
     * root node).
     *
     * @param n          the document number.
     * @param docNumbers an array for reuse. An implementation should use the
     *                   passed array as a container for the return value,
     *                   unless the length of the returned array is different
     *                   from <code>docNumbers</code>. In which case an
     *                   implementation will create a new array with an
     *                   appropriate size.
     * @return the document number of <code>n</code>'s parent.
     * @throws java.io.IOException if an error occurs while reading from the
     *                             index.
     */
    int[] getParents(int n, int[] docNumbers) throws IOException;
}
