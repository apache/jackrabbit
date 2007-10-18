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
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.Name;

import java.io.Serializable;

/**
 * <code>ChildInfoImpl</code> implements a serializable <code>ChildInfo</code>.
 */
public class ChildInfoImpl implements ChildInfo, Serializable {

    /**
     * The name of this child info.
     */
    private final Name name;

    /**
     * The unique id for this child info or <code>null</code> if it does not
     * have a unique id.
     */
    private final String uniqueId;

    /**
     * 1-based index of this child info.
     */
    private final int index;

    /**
     * Creates a new serializable <code>ChildInfoImpl</code>.
     *
     * @param name     the name of the child node.
     * @param uniqueId the unique id of the child node or <code>null</code>.
     * @param index    the index of the child node.
     */
    public ChildInfoImpl(Name name, String uniqueId, int index) {
        this.name = name;
        this.uniqueId = uniqueId;
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getUniqueID() {
        return uniqueId;
    }

    /**
     * {@inheritDoc}
     */
    public int getIndex() {
        return index;
    }
}
