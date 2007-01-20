/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.decorator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

/**
 * Node iterator that decorates all iterated nodes. This utility class is
 * used by the decorator layer to manage the decoration of all the nodes
 * returned by an underlying node iterator. This class delegates
 * all method calls to the underlying node iterator and uses the given
 * decorator factory to decorate all the returned node instances.
 */
public class DecoratingNodeIterator extends DecoratingRangeIterator
        implements NodeIterator {

    /**
     * Creates a decorating node iterator.
     *
     * @param factory decorator factory
     * @param session decorated session
     * @param iterator underlying node iterator
     */
    public DecoratingNodeIterator(
            DecoratorFactory factory, Session session, NodeIterator iterator) {
        super(factory, session, iterator);
    }

    /**
     * Decorates and returns the next node from the underlying node iterator.
     *
     * @return next node (decorated)
     * @see NodeIterator#nextNode()
     */
    public Node nextNode() {
        return (Node) next();
    }

}
