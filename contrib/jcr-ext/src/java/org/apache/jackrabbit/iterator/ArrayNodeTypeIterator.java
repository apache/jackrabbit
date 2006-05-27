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
package org.apache.jackrabbit.iterator;

import java.util.Collection;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

/**
 * Array implementation of the JCR
 * {@link javax.jcr.NodeTypeIterator NodeTypeIterator} interface.
 */
public class ArrayNodeTypeIterator extends ArrayIterator implements
        NodeTypeIterator {

    /**
     * Creates an iterator for the given array of node types.
     *
     * @param types the node types to iterate
     */
    public ArrayNodeTypeIterator(NodeType[] types) {
        super(types);
    }

    /**
     * Creates an iterator for the given collection of node types.
     *
     * @param types the node types to iterate
     */
    public ArrayNodeTypeIterator(Collection types) {
        this((NodeType[]) types.toArray(new NodeType[types.size()]));
    }

    /**
     * Returns the next node type in the array.
     *
     * @return next node type
     * @see NodeTypeIterator#nextNodeType()
     */
    public NodeType nextNodeType() {
        return (NodeType) next();
    }

}
