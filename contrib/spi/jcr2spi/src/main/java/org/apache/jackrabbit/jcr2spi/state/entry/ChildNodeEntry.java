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
package org.apache.jackrabbit.jcr2spi.state.entry;

import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;

/**
 * <code>ChildNodeEntry</code> specifies the name, index (in the case of
 * same-name siblings) and the UUID of a child node entry.
 */
public interface ChildNodeEntry extends ChildItemEntry {

    /**
     * @return the <code>NodeId</code> of this child node entry.
     */
    public NodeId getId();

    /**
     * @return the UUID of the node state which is referenced by this child node
     * entry or <code>null</code> if the node state cannot be identified with a
     * UUID.
     */
    public String getUUID();

    /**
     * @return the index of this child node entry to suppport same-name siblings.
     */
    public int getIndex();

    /**
     * @return the referenced <code>NodeState</code>.
     * @throws NoSuchItemStateException if the <code>NodeState</code> does not
     * exist anymore.
     * @throws ItemStateException If an error occurs while retrieving the
     * <code>NodeState</code>.
     */
    public NodeState getNodeState()
            throws NoSuchItemStateException, ItemStateException;
}
