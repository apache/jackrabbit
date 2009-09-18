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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

import java.util.Collection;

/**
 * Event channel used to transmit nodetype registry operations.
 */
public interface NodeTypeEventChannel {

    /**
     * Called when one or more node types have been registered.
     *
     * @param ntDefs collection of node type definitions
     */
    void registered(Collection<QNodeTypeDefinition> ntDefs);

    /**
     * Called when a node types has been re-registered.
     *
     * @param ntDef node type definition
     */
    void reregistered(QNodeTypeDefinition ntDef);

    /**
     * Called when one or more node types have been unregistered.
     *
     * @param ntNames collection of node type qnames
     */
    void unregistered(Collection<Name> ntNames);

    /**
     * Set listener that will receive information about incoming, external node type events.
     *
     * @param listener node type event listener
     */
    void setListener(NodeTypeEventListener listener);

}
