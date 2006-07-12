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

import org.apache.jackrabbit.spi.NodeId;

import java.util.List;

/**
 * <code>NodeReferences</code>...
 */
public interface NodeReferences {

    // DIFF JR: return NodeId instead of NodeReferenceId
    /**
     * Returns the identifier of this node references object.
     *
     * @return the id of this node references object.
     */
    public NodeId getId();

    /**
     * Returns a flag indicating whether this object holds any references
     *
     * @return <code>true</code> if this object holds references,
     *         <code>false</code> otherwise
     */
    public boolean hasReferences();

    /**
     * @return the list of references
     */
    public List getReferences();
}
