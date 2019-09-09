/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.commons.flat;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import java.util.Iterator;

/**
 * Interface for accessing JCR {@link Item}s sequentially through an
 * {@link Iterator} or looking them up through a <code>key</code>.
 *
 * @param <T> extends &lt;Item&gt;
 */
public interface Sequence<T extends Item> extends Iterable<T> {

    /**
     * Iterator for the {@link Item}s in this sequence. The order of the items
     * is implementation specific.
     *
     * @see java.lang.Iterable#iterator()
     */
    Iterator<T> iterator();

    /**
     * Retrieve an {@link Item} from this sequence by its <code>key</code>. If
     * the sequence does not contain the <code>key</code> this method throws an
     * {@link ItemNotFoundException}.
     *
     * @param key The <code>key</code> of the item to retrieve. Must not be
     *            <code>null</code>.
     * @return The item belonging to <code>key</code>.
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    T getItem(String key) throws AccessDeniedException, PathNotFoundException, ItemNotFoundException,
            RepositoryException;

    /**
     * Determine whether this sequence contains a specific <code>key</code>.
     *
     * @param key The <code>key</code> to look up.
     * @return <code>true</code> if this sequence contains <code>key</code>.
     *         <code>False</code> otherwise.
     * @throws RepositoryException
     */
    boolean hasItem(String key) throws RepositoryException;
}