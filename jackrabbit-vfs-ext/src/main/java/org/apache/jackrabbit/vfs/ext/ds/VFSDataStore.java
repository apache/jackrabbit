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

import java.util.Properties;

import org.apache.jackrabbit.core.data.Backend;
import org.apache.jackrabbit.core.data.CachingDataStore;

/**
 * Commons VFS based data store.
 */
public class VFSDataStore extends CachingDataStore {

    private Properties properties;

    @Override
    protected Backend createBackend() {
        VFSBackend backend = new VFSBackend();
        if(properties != null){
            backend.setProperties(properties);
        }
        return backend;
    }

    @Override
    protected String getMarkerFile() {
        return "vfs.init.done";
    }

    /**
     * Properties required to configure the S3Backend
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
