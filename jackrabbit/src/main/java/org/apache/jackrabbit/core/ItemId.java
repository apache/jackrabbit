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
package org.apache.jackrabbit.core;

import java.io.Serializable;

/**
 * <code>ItemId</code> serves as the base class for the concrete classes
 * <code>PropertyId</code> and <code>NodeId</code> who uniquely identify
 * nodes and properties in a workspace.
 */
public abstract class ItemId implements Serializable {

    /** Serialization UID of this class. */
    static final long serialVersionUID = -5138008726453328226L;

    /**
     * Returns <code>true</code> if this id denotes a <code>Node</code>.
     *
     * @return <code>true</code> if this id denotes a <code>Node</code>,
     *         <code>false</code> if it denotes a <code>Property</code>
     * @see PropertyId
     * @see NodeId
     */
    public abstract boolean denotesNode();

}

