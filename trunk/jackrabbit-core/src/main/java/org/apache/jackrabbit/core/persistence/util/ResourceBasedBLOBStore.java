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

import org.apache.jackrabbit.core.fs.FileSystemResource;

/**
 * <code>ResourceBasedBLOBStore</code> extends the <code>BLOBStore</code>
 * interface with the method {@link #getResource(String)}
 * <p>
 * Note that The DataStore should nowadays be used instead of the BLOBStore.
 */
public interface ResourceBasedBLOBStore extends BLOBStore {
    /**
     * Retrieves the BLOB data with the specified id as a permanent resource.
     *
     * @param blobId identifier of the BLOB data as returned by
     *               {@link #createId(org.apache.jackrabbit.core.id.PropertyId , int)}
     * @return a resource representing the BLOB data
     * @throws Exception if an error occurred
     */
    FileSystemResource getResource(String blobId) throws Exception;
}
