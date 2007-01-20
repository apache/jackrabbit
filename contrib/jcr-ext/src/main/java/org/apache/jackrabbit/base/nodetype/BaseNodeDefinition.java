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
package org.apache.jackrabbit.base.nodetype;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

/**
 * Node definition base class.
 */
public class BaseNodeDefinition extends BaseItemDefinition implements NodeDefinition {

    /** Always returns an empty node type array. {@inheritDoc} */
    public NodeType[] getRequiredPrimaryTypes() {
        return new NodeType[0];
    }

    /** Not implemented. {@inheritDoc} */
    public NodeType getDefaultPrimaryType() {
        throw new UnsupportedOperationException();
    }

    /** Always returns <code>false</code>. {@inheritDoc} */
    public boolean allowsSameNameSiblings() {
        return false;
    }

}
