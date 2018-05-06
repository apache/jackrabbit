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
package org.apache.jackrabbit.core.persistence.util;

import org.apache.jackrabbit.core.id.PropertyId;

import java.io.InputStream;

/**
 * <code>BLOBStore</code> represents an abstract store for binary property
 * values (BLOBs).
 * <p>
 * Note that The DataStore should nowadays be used instead of the BLOBStore.
 * This interface and the implementing classes are kept mostly for backwards
 * compatibility.
 *
 * @see ResourceBasedBLOBStore
 */
public interface BLOBStore {
    /**
     * Creates a unique identifier for the BLOB data associated with the given
     * property id and value subscript.
     *
     * @param id    id of the property associated with the BLOB data
     * @param index subscript of the value holding the BLOB data
     * @return a string identifying the BLOB data
     */
    String createId(PropertyId id, int index);

    /**
     * Stores the BLOB data and returns a unique identifier.
     *
     * @param blobId identifier of the BLOB data as returned by
     *               {@link #createId(PropertyId, int)}
     * @param in     stream containing the BLOB data
     * @param size   size of the BLOB data
     * @throws Exception if an error occured
     */
    void put(String blobId, InputStream in, long size) throws Exception;

    /**
     * Retrieves the BLOB data with the specified id as a binary stream.
     *
     * @param blobId identifier of the BLOB data as returned by
     *               {@link #createId(PropertyId, int)}
     * @return an input stream that delivers the BLOB data
     * @throws Exception if an error occured
     */
    InputStream get(String blobId) throws Exception;

    /**
     * Removes the BLOB data with the specified id.
     *
     * @param blobId identifier of the BLOB data as returned by
     *               {@link #createId(PropertyId, int)}
     * @return <code>true</code> if BLOB data with the given id exists and has
     *         been successfully removed, <code>false</code> if there's no BLOB
     *         data with the given id.
     * @throws Exception if an error occured
     */
    boolean remove(String blobId) throws Exception;
}
