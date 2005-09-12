/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state.obj;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.fs.FileSystemResource;

import java.io.InputStream;

/**
 * <code>BLOBStore</code> ...
 */
public interface BLOBStore {
    /**
     *
     * @param id id of the property associated with the blob data
     * @param index subscript of the value holding the blob data
     * @param in
     * @param size
     * @return a string identifying the blob data
     * @throws Exception
     */
    String put(PropertyId id, int index, InputStream in, long size) throws Exception;

    /**
     *
     * @param blobId
     * @return
     * @throws Exception
     */
    FileSystemResource get(String blobId) throws Exception;

    /**
     *
     * @param blobId
     * @return
     * @throws Exception
     */
    boolean remove(String blobId) throws Exception;
}
