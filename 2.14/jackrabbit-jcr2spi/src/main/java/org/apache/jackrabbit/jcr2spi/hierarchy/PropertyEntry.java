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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.spi.PropertyId;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.InvalidItemStateException;

/**
 * <code>PropertyEntry</code>...
 */
public interface PropertyEntry extends HierarchyEntry {

    /**
     * @return the <code>NodeId</code> of this child node entry.
     */
    public PropertyId getId() throws InvalidItemStateException, RepositoryException;

    /**
     * Returns the ID that must be used for resolving this entry OR loading its
     * children entries from the persistent layer. This is the same as
     * <code>getId()</code> unless any of its ancestors has been transiently
     * moved.
     *
     * @return
     * @see #getId()
     */
    public PropertyId getWorkspaceId() throws InvalidItemStateException, RepositoryException;

    /**
     * @return the referenced <code>PropertyState</code>.
     * @throws ItemNotFoundException if the <code>PropertyState</code> does not
     * exist anymore.
     * @throws RepositoryException if an error occurs while retrieving the
     * <code>PropertyState</code>.
     */
    public PropertyState getPropertyState() throws ItemNotFoundException, RepositoryException;

}