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
package org.apache.jackrabbit.jcr2spi.nodetype;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.RepositoryException;
import java.util.Iterator;

/**
 * <code>NodeTypeStorage</code>...
 */
public interface NodeTypeStorage {

    /**
     * Returns an Iterator over all node type definitions registered.
     *
     * @return
     * @throws RepositoryException
     */
    public Iterator getAllDefinitions() throws RepositoryException;

    /**
     * Returns the <code>QNodeTypeDefinition</code>s for the given node type
     * names. The implementation is free to return additional definitions e.g.
     * dependencies.
     *
     * @param nodeTypeNames
     * @return
     * @throws NoSuchNodeTypeException
     * @throws RepositoryException
     */
    public Iterator getDefinitions(Name[] nodeTypeNames) throws NoSuchNodeTypeException, RepositoryException;

    public void registerNodeTypes(QNodeTypeDefinition[] nodeTypeDefs) throws NoSuchNodeTypeException, RepositoryException;

    public void reregisterNodeTypes(QNodeTypeDefinition[] nodeTypeDefs) throws NoSuchNodeTypeException, RepositoryException;

    public void unregisterNodeTypes(Name[] nodeTypeNames) throws NoSuchNodeTypeException, RepositoryException;
}