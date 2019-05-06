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
package org.apache.jackrabbit.spi.commons.tree;

import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Tree;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

public abstract class AbstractTree implements Tree {

    private final Name nodeName;
    private final Name ntName;
    private final String uniqueId;

    private final NamePathResolver resolver;

    private List<Tree> children;

    protected AbstractTree(Name nodeName, Name ntName, String uniqueId, NamePathResolver resolver) {
        this.nodeName = nodeName;
        this.ntName = ntName;
        this.uniqueId = uniqueId;
        this.children = new ArrayList<Tree>();
        this.resolver = resolver;
    }

    protected NamePathResolver getResolver() {
        return resolver;
    }
    
    protected List<Tree> getChildren() {
        return children;
    }

    protected abstract Tree createChild(Name name, Name primaryTypeName, String uniqueId);

    //---------------------------------------------------------------< Tree >---
    @Override
    public Name getName() {
        return nodeName;
    }

    @Override
    public Name getPrimaryTypeName() {
        return ntName;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public Tree addChild(Name childName, Name primaryTypeName, String uniqueId) {
        Tree child = createChild(childName, primaryTypeName, uniqueId);        
        children.add(child);
        
        return child;
    }
}
