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
package org.apache.jackrabbit.lite.nodetype;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.base.nodetype.BaseNodeTypeManager;
import org.apache.jackrabbit.iterator.ArrayNodeTypeIterator;

public class LiteNodeTypeManager extends BaseNodeTypeManager
        implements NodeTypeManager {

    private final Set types = new HashSet();

    protected void addNodeType(NodeType type) {
        types.add(type);
    }

    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        return new ArrayNodeTypeIterator(types);
    }

}
