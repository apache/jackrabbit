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

import java.util.Collection;
import java.util.Comparator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.Predicate;

/**
 * Traverser implementations are responsible of collecting nodes from the root
 * node base on custom strategies.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public interface Traverser
{

    /**
     * Parameter that optionally affect the Traverser behaviour. <br>
     * 
     * @param expression
     */
    public void setParameter(Object parameter);

    /**
     * Set a node filter
     * 
     * @param node
     */
    public void setFilter(Predicate predicate);

    /**
     * Set the comparator to order the nodes
     * 
     * @param node
     */
    public void setOrder(Comparator comparator);

    /**
     * Set the node to traverse from
     * 
     * @param node
     */
    public void setNode(Node node);

    /**
     * Set the depth
     * 
     * @param depth
     */
    public void setDepth(int depth);

    /**
     * Perform traverse
     */
    public void traverse() throws RepositoryException;

    /**
     * Get the nodes
     * 
     * @return
     */
    public Collection getNodes();

}