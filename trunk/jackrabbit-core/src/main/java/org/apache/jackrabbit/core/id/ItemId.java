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
package org.apache.jackrabbit.core.id;

import java.io.Serializable;

/**
 * Node or property identifier. All content items in a Jackrabbit repository
 * have an identifier that uniquely identifies the item in a workspace.
 * <p>
 * This interface is implemented by both the concrete node and property
 * identifier classes in order to allow client code to determine whether
 * an identifier refers to a node or a property.
 */
public interface ItemId extends Serializable {

    /**
     * Checks whether this identifier denotes a node item.
     *
     * @return <code>true</code> if this identifier denotes a node,
     *         <code>false</code> if a property
     * @see PropertyId
     * @see NodeId
     */
    boolean denotesNode();

}
