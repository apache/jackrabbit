/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.rmi.iterator;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

/**
 * Array implementation of the JCR
 * {@link javax.jcr.NodeTypeIterator NodeTypeIterator} interface.
 * This class is used by the JCR-RMI client adapters to convert
 * node type arrays to iterators.
 * 
 * @author Jukka Zitting
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

    /** {@inheritDoc} */
    public NodeType nextNodeType() {
        return (NodeType) next();
    }

}
