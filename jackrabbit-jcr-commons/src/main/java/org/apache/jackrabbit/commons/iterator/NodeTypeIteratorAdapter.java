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

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

/**
 * Adapter class for turning {@link RangeIterator}s or {@link Iterator}s
 * into {@link NodeTypeIterator}s.
 */
public class NodeTypeIteratorAdapter extends RangeIteratorDecorator
        implements NodeTypeIterator {

    /**
     * Static instance of an empty {@link NodeTypeIterator}.
     */
    public static final NodeTypeIterator EMPTY =
        new NodeTypeIteratorAdapter(RangeIteratorAdapter.EMPTY);

    /**
     * Creates an adapter for the given {@link RangeIterator}.
     *
     * @param iterator iterator of {@link NodeType}s
     */
    public NodeTypeIteratorAdapter(RangeIterator iterator) {
        super(iterator);
    }

    /**
     * Creates an adapter for the given {@link Iterator}.
     *
     * @param iterator iterator of {@link NodeType}s
     */
    public NodeTypeIteratorAdapter(Iterator iterator) {
        super(new RangeIteratorAdapter(iterator));
    }

    /**
     * Creates an iterator for the given collection.
     *
     * @param collection collection of {@link NodeType}s
     */
    public NodeTypeIteratorAdapter(Collection<NodeType> collection) {
        super(new RangeIteratorAdapter(collection));
    }

    //----------------------------------------------------< NodeTypeIterator >

    /**
     * Returns the next node type.
     *
     * @return next node type
     * @throws NoSuchElementException if there is no next node type
     */
    public NodeType nextNodeType() throws NoSuchElementException {
        return (NodeType) next();
    }

}
