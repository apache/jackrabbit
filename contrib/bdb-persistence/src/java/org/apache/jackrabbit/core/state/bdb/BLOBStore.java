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
package org.apache.jackrabbit.core.state.bdb;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.fs.FileSystemResource;

import java.io.InputStream;

public interface BLOBStore {

    public String put(PropertyId id, int index, InputStream in, long size) throws Exception;
    public FileSystemResource get(String blobId) throws Exception;
    public boolean remove(String blobId) throws Exception;
    
}
