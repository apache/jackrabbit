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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.TestCaseBase;
import org.apache.jackrabbit.vfs.ext.VFSConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test {@link CachingDataStore} with VFSBackend with a VFS file system (local file system) by default.
 * <P>
 * You can provide different properties to use other file system backend by passing vfs config file via system property.
 * For e.g. -Dconfig=/opt/repository/vfs.properties.
 * </P>
 * <P>
 * Sample VFS properties located at src/test/resources/vfs*.properties
 * </P>
 */
public class TestVFSDataStore extends TestCaseBase {

    protected static final Logger LOG = LoggerFactory.getLogger(TestVFSDataStore.class);

    private String vfsBaseFolderUri;

    private VFSDataStore vfsDataStore;

    private Properties vfsBackendProps;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String configProp = System.getProperty("config");
        if (configProp != null && !"".equals(configProp)) {
            Properties props = new Properties();
            File configFile = new File(configProp);
            if (configFile.isFile()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(configFile);
                    props.load(fis);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            } else {
                props = loadProperties(configProp);
            }
            vfsBackendProps = props;
        }
    }

    @Override
    protected CachingDataStore createDataStore() throws RepositoryException {
        vfsDataStore = new VFSDataStore();
        Properties props = getBackendProperties();
        vfsBaseFolderUri = props.getProperty(VFSConstants.VFS_BASE_FOLDER_URI);
        LOG.info("vfsBaseFolderUri [{}] set.", vfsBaseFolderUri);
        vfsDataStore.setProperties(props);
        vfsDataStore.setSecret("123456");
        vfsDataStore.init(dataStoreDir);
        return vfsDataStore;
    }

    @Override
    protected void tearDown() {
        LOG.info("cleaning vfsBaseFolderUri [{}]", vfsBaseFolderUri);
        VFSBackend backend = (VFSBackend) vfsDataStore.getBackend();
        FileObject vfsBaseFolder = backend.getBaseFolderObject();
        try {
            while (backend.getAsyncWriteExecuter().getActiveCount() > 0) {
                Thread.sleep(3000);
            }
            VFSTestUtils.deleteAllDescendantFiles(vfsBaseFolder);
        } catch (Exception e) {
            LOG.error("Failed to clean base folder at '{}'.", vfsBaseFolder.getName().getURI(), e);
        }
        super.tearDown();
    }

    private Properties getBackendProperties() {
        if (vfsBackendProps == null) {
            Properties props = loadProperties("/vfs.properties");
            String uriValue = props.getProperty(VFSConstants.VFS_BASE_FOLDER_URI);
            if (uriValue == null || "".equals(uriValue.trim())) {
                String baseFolderUri = new File(new File(dataStoreDir), "vfsds").toURI().toString();
                props.setProperty(VFSConstants.VFS_BASE_FOLDER_URI, baseFolderUri);
            }
            vfsBackendProps = props;
        }
        return vfsBackendProps;
    }
}
