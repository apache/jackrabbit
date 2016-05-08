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

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TestVFSDataStore.class);

    /**
     * VFS base folder URI configuration property key name.
     */
    private static final String BASE_FOLDER_URI = "baseFolderUri";

    private String baseFolderUri;

    private VFSDataStore dataStore;

    private Properties configProps;

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

            configProps = props;
        }
    }

    @Override
    protected CachingDataStore createDataStore() throws RepositoryException {
        dataStore = new VFSDataStore();
        Properties props = getConfigProps();
        baseFolderUri = props.getProperty(BASE_FOLDER_URI);
        dataStore.setBaseFolderUri(baseFolderUri);
        LOG.info("baseFolderUri [{}] set.", baseFolderUri);
        dataStore.setFileSystemOptionsProperties(props);
        dataStore.setSecret("123456");
        dataStore.init(dataStoreDir);
        return dataStore;
    }

    @Override
    protected void tearDown() {
        LOG.info("cleaning vfsBaseFolderUri [{}]", baseFolderUri);
        VFSBackend backend = (VFSBackend) dataStore.getBackend();
        FileObject vfsBaseFolder = backend.getBaseFolderObject();

        try {
            // Let's wait for 5 minutes at max if there are still execution jobs in the async writing executor's queue.
            int seconds = 0;
            while (backend.getAsyncWriteExecuter().getActiveCount() > 0 && seconds++ < 300) {
                Thread.sleep(1000);
            }

            VFSTestUtils.deleteAllDescendantFiles(vfsBaseFolder);
        } catch (Exception e) {
            LOG.error("Failed to clean base folder at '{}'.", vfsBaseFolder.getName().getFriendlyURI(), e);
        }

        super.tearDown();
    }

    private Properties getConfigProps() {
        if (configProps == null) {
            Properties props = new Properties();
            String baseFolderUri = new File(new File(dataStoreDir), "vfsds").toURI().toString();
            props.setProperty(BASE_FOLDER_URI, baseFolderUri);
            configProps = props;
        }

        return configProps;
    }
}
