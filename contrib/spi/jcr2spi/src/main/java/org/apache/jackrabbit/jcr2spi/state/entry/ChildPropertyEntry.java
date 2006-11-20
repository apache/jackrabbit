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

import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;

/**
 * <code>ChildPropertyEntry</code>...
 */
public interface ChildPropertyEntry extends ChildItemEntry {

    /**
     * @return the <code>NodeId</code> of this child node entry.
     */
    public PropertyId getId();

    /**
     * @return the referenced <code>PropertyState</code>.
     * @throws NoSuchItemStateException if the <code>PropertyState</code> does not
     * exist anymore.
     * @throws ItemStateException if an error occurs while retrieving the
     * <code>PropertyState</code>.
     */
    public PropertyState getPropertyState() throws NoSuchItemStateException, ItemStateException;
}