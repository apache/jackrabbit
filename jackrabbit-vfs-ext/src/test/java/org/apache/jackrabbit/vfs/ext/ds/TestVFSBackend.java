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

import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.http.HttpFileSystemConfigBuilder;
import org.apache.jackrabbit.vfs.ext.VFSConstants;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test VFSBackend.
 */
public class TestVFSBackend extends TestCase {

    private VFSBackend backend;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        backend = new VFSBackend();
    }

    public void testBuildFileSystemOptions_withHttpDefaults() throws Exception {
        testBuildFileSystemOptions_withHttpStyleBackendDefaults("http://localhost:8080/content");
    }

    public void testBuildFileSystemOptions_withHttpsDefaults() throws Exception {
        testBuildFileSystemOptions_withHttpStyleBackendDefaults("https://localhost:8080/content");
    }

    public void testBuildFileSystemOptions_withWebDAVDefaults() throws Exception {
        testBuildFileSystemOptions_withHttpStyleBackendDefaults("webdav://localhost:8080/content");
    }

    public void testBuildFileSystemOptions_withWebDAVProps() throws Exception {
        Properties props = new Properties();
        props.setProperty(VFSConstants.VFS_BASE_FOLDER_URI, "webdav://localhost:8080/content");
        props.setProperty(VFSBackend.PROP_MAX_TOTAL_CONNECTIONS, "40");
        props.setProperty(VFSBackend.PROP_MAX_CONNECTIONS_PER_HOST, "20");
        FileSystemOptions opts = new FileSystemOptions();
        backend.buildFileSystemOptions(opts, props);
        HttpFileSystemConfigBuilder builder = HttpFileSystemConfigBuilder.getInstance();
        Assert.assertEquals(40, builder.getMaxTotalConnections(opts));
        Assert.assertEquals(20, builder.getMaxConnectionsPerHost(opts));
    }

    private void testBuildFileSystemOptions_withHttpStyleBackendDefaults(String baseFolderURI) throws Exception {
        Properties props = new Properties();
        props.setProperty(VFSConstants.VFS_BASE_FOLDER_URI, baseFolderURI);
        FileSystemOptions opts = new FileSystemOptions();
        backend.buildFileSystemOptions(opts, props);
        HttpFileSystemConfigBuilder builder = HttpFileSystemConfigBuilder.getInstance();
        Assert.assertEquals(VFSBackend.DEFAULT_MAX_CONNECTION, builder.getMaxTotalConnections(opts));
        Assert.assertEquals(VFSBackend.DEFAULT_MAX_CONNECTION, builder.getMaxConnectionsPerHost(opts));
    }
}
