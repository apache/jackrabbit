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

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;

/**
 * <code>TransientItemStateFactory</code> extends the item state factory and
 * adds new methods for creating node states and property states that are new.
 */
public interface TransientItemStateFactory extends ItemStateFactory {

    /**
     * Creates a transient child <code>NodeState</code> with the given
     * <code>name</code>.
     *
     * @param name the name of the <code>NodeState</code> to create.
     * @param uniqueID the unique ID of the <code>NodeState</code> to create or
     * <code>null</code> if the created <code>NodeState</code> cannot be
     * identified by a unique ID.
     * @param parent the parent of the <code>NodeState</code> to create.
     * @param nodeTypeName name of the primary nodetype
     * @param definition the definition for this new NodeState
     * @return the created <code>NodeState</code>.
     */
    public NodeState createNewNodeState(QName name, String uniqueID,
                                        NodeState parent, QName nodeTypeName,
                                        QNodeDefinition definition);

    /**
     * Creates a transient <code>PropertyState</code> with the given
     * <code>name</code>.
     *
     * @param name   the name of the <code>PropertyState</code> to create.
     * @param parent the parent of the <code>PropertyState</code> to create.
     * @param definition definition for this new property state.
     * @return the created <code>PropertyState</code>.
     */
    public PropertyState createNewPropertyState(QName name,
                                                NodeState parent,
                                                QPropertyDefinition definition);

    /**
     * Set the listener that gets informed about NEW states.
     *
     * @param listener
     */
    public void setListener(ItemStateCreationListener listener);
}
