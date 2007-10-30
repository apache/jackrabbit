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

import org.apache.jackrabbit.spi.Name;

/**
 * <code>NodeDef</code> is the internal representation of
 * a node definition. It refers to <code>Name</code>s only
 * and is thus isolated from session-specific namespace mappings.
 *
 * @see javax.jcr.nodetype.NodeDefinition
 */
public interface NodeDef extends ItemDef {

    NodeDef[] EMPTY_ARRAY = new NodeDef[0];

    /**
     * Returns an identifier for this node definition.
     *
     * @return an identifier for this node definition.
     */
    NodeDefId getId();

    /**
     * Returns the name of the default primary type.
     *
     * @return the name of the default primary type.
     */
    Name getDefaultPrimaryType();

    /**
     * Returns the array of names of the required primary types.
     *
     * @return the array of names of the required primary types.
     */
    Name[] getRequiredPrimaryTypes();

    /**
     * Reports whether this node can have same-name siblings.
     *
     * @return the 'allowsSameNameSiblings' flag.
     */
    boolean allowsSameNameSiblings();
}
