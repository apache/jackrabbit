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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;

import javax.jcr.RepositoryException;

/**
 * <code>TransientItemStateFactory</code> extends the item state factory and
 * adds new methods for creating node states and property states that are new.
 */
public interface TransientItemStateFactory extends ItemStateFactory {

    /**
     * Creates a transient child <code>NodeState</code> with the given
     * <code>name</code>.
     *
     * @param entry
     * @param nodeTypeName
     * @param definition
     * @return the created <code>NodeState</code>
     */
    public NodeState createNewNodeState(NodeEntry entry,
                                        Name nodeTypeName,
                                        QNodeDefinition definition);

    /**
     * Creates a transient <code>PropertyState</code>.
     *
     * @param entry
     * @param definition
     * @param values
     * @param propertyType
     * @return the created <code>PropertyState</code>.
     */
    public PropertyState createNewPropertyState(PropertyEntry entry,
                                                QPropertyDefinition definition,
                                                QValue[] values, int propertyType)
            throws RepositoryException;
}
