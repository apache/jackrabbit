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
package org.apache.jackrabbit.commons.iterator;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.jackrabbit.commons.JcrUtils;

/**
 * Adapter class that adapts a {@link NodeIterator} instance to an
 * {@link Iterable} instance that always returns the same underlying
 * iterator.
 *
 * @since Apache Jackrabbit 2.0
 * @deprecated - Use {@link JcrUtils#in(NodeIterator)} instead
 */
@Deprecated
public class NodeIterable implements Iterable<Node> {

    /**
     * The node iterator being adapted.
     */
    private final NodeIterator iterator;

    /**
     * Creates an iterable adapter for the given node iterator.
     *
     * @param iterator the node iterator to be adapted
     */
    public NodeIterable(NodeIterator iterator) {
        this.iterator = iterator;
    }

    /**
     * Returns the node iterator.
     *
     * @return node iterator
     */
    @SuppressWarnings("unchecked")
    public Iterator<Node> iterator() {
        return iterator;
    }

}
