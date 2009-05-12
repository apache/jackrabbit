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
package org.apache.jackrabbit.spi.commons.nodetype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.commons.iterator.NodeTypeIteratorAdapter;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;

/**
 * <code>AbstractNodeType</code>...
 */
/**
 * <code>AbstractNodeType</code>...
 */
public abstract class AbstractNodeType implements NodeType {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(AbstractNodeType.class);

    private final AbstractNodeTypeManager ntMgr;

    /**
     * Create a new <code>AbstractNodeType</code>.
     *
     * @param ntMgr
     */
    public AbstractNodeType(AbstractNodeTypeManager ntMgr) {
        this.ntMgr = ntMgr;
    }

    //-----------------------------------------------------------< NodeType >---
    /**
     * @see javax.jcr.nodetype.NodeType#getDeclaredSubtypes()
     */
    public NodeTypeIterator getDeclaredSubtypes() {
        return getSubtypes(true);
    }

    /**
     * @see javax.jcr.nodetype.NodeType#getSubtypes()
     */
    public NodeTypeIterator getSubtypes() {
        return getSubtypes(false);
    }        

    //--------------------------------------------------------------------------
    /**
     * Returns the node types derived from this node type.
     *
     * @param directOnly if <code>true</code> only direct subtypes will be considered
     *
     * @return an <code>NodeTypeIterator</code>.
     * @see NodeType#getSubtypes
     * @see NodeType#getDeclaredSubtypes
     */
    public NodeTypeIterator getSubtypes(boolean directOnly) {
        NodeTypeIterator iter;
        try {
            iter = ntMgr.getAllNodeTypes();
        } catch (RepositoryException e) {
            // should never get here
            log.error("failed to retrieve registered node types", e);
            return NodeTypeIteratorAdapter.EMPTY;
        }

        ArrayList<NodeType> result = new ArrayList<NodeType>();
        String thisName = getName();
        while (iter.hasNext()) {
            NodeType nt = iter.nextNodeType();
            if (!nt.getName().equals(thisName)) {
                if (directOnly) {
                    // direct subtypes only
                    String[] names = nt.getDeclaredSupertypeNames();
                    for (int i = 0; i < names.length; i++) {
                        if (names[i].equals(thisName)) {
                            result.add(nt);
                            break;
                        }
                    }
                } else {
                    // direct and indirect subtypes
                    if (nt.isNodeType(thisName)) {
                        result.add(nt);
                    }
                }
            }
        }
        return new NodeTypeIteratorAdapter(result);
    }
}