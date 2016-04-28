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
package org.apache.jackrabbit.vfs.ext.ds;

import org.apache.jackrabbit.core.data.Backend;
import org.apache.jackrabbit.core.data.CachingDataStore;

/**
 * Commons VFS based data store.
 */
public class VfsDataStore extends CachingDataStore {

    /**
     * The name of the folder that contains all the data record files. The structure
     * of content within this directory is controlled by this class.
     */
    private String basePath;

    /**
     * This thread pool count for asynchronous uploads. By default it is 10.
     */
    private int asyncUploadPoolSize = 10;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public int getAsyncUploadPoolSize() {
        return asyncUploadPoolSize;
    }

    public void setAsyncUploadPoolSize(int asyncUploadPoolSize) {
        this.asyncUploadPoolSize = asyncUploadPoolSize;
    }

    @Override
    protected Backend createBackend() {
        VfsBackend backend = new VfsBackend(getBasePath());
        backend.setAsyncUploadPoolSize(getAsyncUploadPoolSize());
        return backend;
    }

    @Override
    protected String getMarkerFile() {
        return "vfs.init.done";
    }

}
