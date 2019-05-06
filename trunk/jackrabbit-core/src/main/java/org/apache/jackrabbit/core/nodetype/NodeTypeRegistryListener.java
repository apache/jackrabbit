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
package org.apache.jackrabbit.core.nodetype;

import java.util.Collection;

import org.apache.jackrabbit.spi.Name;

/**
 * The <code>NodeTypeRegistryListener</code> interface allows an implementing
 * object to be informed about node type (un)registration.
 *
 * @see NodeTypeRegistry#addListener(NodeTypeRegistryListener)
 * @see NodeTypeRegistry#removeListener(NodeTypeRegistryListener)
 */
public interface NodeTypeRegistryListener {

    /**
     * Called when a node type has been registered.
     *
     * @param ntName name of the node type that has been registered
     */
    void nodeTypeRegistered(Name ntName);

    /**
     * Called when a node type has been re-registered.
     *
     * @param ntName name of the node type that has been registered
     */
    void nodeTypeReRegistered(Name ntName);

    /**
     * Called when a set of node types has been unregistered.
     *
     * @param names names of the types that have been unregistered
     */
    void nodeTypesUnregistered(Collection<Name> names);

}
