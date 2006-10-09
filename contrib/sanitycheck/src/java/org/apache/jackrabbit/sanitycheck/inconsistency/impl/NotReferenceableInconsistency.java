/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.sanitycheck.inconsistency.impl;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.util.uuid.UUID;

/**
 * Reference to a node which is not referenceable
 */
public class NotReferenceableInconsistency extends
        AbstractValueInconsistency 
{
    private NodeState referencedNode;

    /**
     * @inheritDoc
     */
    public String getDescription()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("The node ");
        sb.append(getProperty().getParentUUID());
        sb.append(" contains a property of type REFERENCE that points to non referenceable node (");
        UUID uuid = (UUID) getProperty().getValues()[getIndex()].internalValue();
        sb.append(uuid.toString());
        sb.append("): ");
        sb.append(getProperty().getName().toString());
        sb.append("[");
        sb.append(getIndex());
        sb.append("].");
        return sb.toString();
    }

    public NodeState getReferencedNode()
    {
        return referencedNode;
    }

    public void setReferencedNode(NodeState referencedNode)
    {
        this.referencedNode = referencedNode;
    }

}
