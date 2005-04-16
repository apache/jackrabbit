/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.lite.nodetype;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

import org.apache.jackrabbit.base.nodetype.BaseNodeTypeManager;
import org.apache.jackrabbit.iterator.ArrayNodeTypeIterator;

/**
 * TODO
 */
public class LiteNodeTypeManager extends BaseNodeTypeManager {

    private final Set types;

    protected LiteNodeTypeManager(Session session) {
        this.types = new HashSet();
    }

    protected void addNodeType(NodeType type) {
        types.add(type);
    }

    /** {@inheritDoc} */
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        return new ArrayNodeTypeIterator(
                (NodeType[]) types.toArray(new NodeType[0]));
    }

}
