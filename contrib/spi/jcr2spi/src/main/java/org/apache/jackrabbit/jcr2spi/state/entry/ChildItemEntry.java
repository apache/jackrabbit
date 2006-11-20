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

import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.name.QName;

/**
 * <code>ChildItemEntry</code>...
 */
public interface ChildItemEntry {

    /**
     * True if this ChildItemEntry would resolve to a <code>NodeState</code>.
     * 
     * @return
     */
    public boolean denotesNode();

    /**
     * @return the name of this child entry.
     */
    public QName getName();

    /**
     * Returns <code>true</code> if the referenced <code>NodeState</code> is
     * available. That is, the referenced <code>NodeState</code> is already
     * cached and ready to be returned by {@link #getItemState()}.
     *
     * @return <code>true</code> if the <code>NodeState</code> is available;
     * otherwise <code>false</code>.
     */
    public boolean isAvailable();

    /**
     * @return the referenced <code>ItemState</code>.
     * @throws NoSuchItemStateException if the <code>ItemState</code> does not
     * exist anymore.
     * @throws ItemStateException If an error occurs while retrieving the
     * <code>ItemState</code>.
     */
    public ItemState getItemState() throws NoSuchItemStateException, ItemStateException;
}
