/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.taglib.traverser;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * Level by level traverse strategy 
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class LevelByLevelTraverser extends AbstractTraverser
{
    private Map levels = new HashMap() ;
    

    /**
     * Traverse the node children tree
     * 
     * @throws RepositoryException
     */
    protected void internalTraverse() throws RepositoryException
    {
        throw new UnsupportedOperationException();
    }
}