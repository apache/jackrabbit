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
package org.apache.jackrabbit.core.integration.random.operation;

import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.commons.packaging.FilterContentPackage;
import org.apache.jackrabbit.commons.predicate.IsNodePredicate;

import javax.jcr.NodeIterator;
import javax.jcr.Session;

/**
 * <code>TraverseNodes</code> traverses a node hierarchy.
 */
public class TraverseNodes extends Operation {

    public TraverseNodes(Session s, String path) {
        super(s, path);
    }

    public NodeIterator execute() throws Exception {
        FilterContentPackage pack = new FilterContentPackage();
        pack.addContent(getPath(), new IsNodePredicate());
        return new NodeIteratorAdapter(pack.getItems(getSession()));
    }
}
