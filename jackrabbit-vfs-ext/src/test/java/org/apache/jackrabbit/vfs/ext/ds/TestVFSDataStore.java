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
import java.io.StringReader;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.TestCaseBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import junit.framework.Assert;

/**
 * Test {@link CachingDataStore} with VFSBackend with a VFS file system (local file system) by default.
 * <P>
 * You can provide different properties to use other file system backend by passing vfs config file via system property.
 * For e.g. -Dconfig=/opt/repository/vfs-webdav.properties.
 * </P>
 * <P>
 * Sample VFS properties located at src/test/resources/vfs-*.properties
 * </P>
 */
public class TestVFSDataStore extends TestCaseBase {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TestVFSDataStore.class);

    // Example when http provider or its variants (https or webdav) FSO is used.
    @SuppressWarnings("unused")
    private static final String HTTP_FILE_SYSTEM_OPTIONS_PARAM_XML =
            "<param "
            + "name=\"fileSystemOptionsPropertiesInString\" "
            + "value=\"fso.http.maxTotalConnections = 200&#13;"
            + "        fso.http.maxConnectionsPerHost = 100&#13;"
            + "        fso.http.preemptiveAuth = true\" />";

    private static final String SFTP_FILE_SYSTEM_OPTIONS_PARAM_XML =
            "<param "
            + "name=\"fileSystemOptionsPropertiesInString\" "
            + "value=\"fso.sftp.identities = /home/tester/.ssh/id_rsa&#13;"
            + "        fso.sftp.timeout = 30000\" />";

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
        baseFolderUri = props.getProperty(VFSDataStore.BASE_FOLDER_URI);
        dataStore.setBaseFolderUri(baseFolderUri);
        LOG.info("baseFolderUri [{}] set.", baseFolderUri);
        String value = props.getProperty(VFSDataStore.ASYNC_WRITE_POOL_SIZE);
        if (value != null) {
            dataStore.setAsyncWritePoolSize(Integer.parseInt(value));
            LOG.info("asyncWritePoolSize [{}] set.", dataStore.getAsyncWritePoolSize());
        }
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
            while (backend.getAsyncWriteExecutorActiveCount() > 0 && seconds++ < 300) {
                Thread.sleep(1000);
            }

            VFSTestUtils.deleteAllDescendantFiles(vfsBaseFolder);
        } catch (Exception e) {
            LOG.error("Failed to clean base folder at '{}'.", vfsBaseFolder.getName().getFriendlyURI(), e);
        }

        super.tearDown();
    }

    /**
     * Test case to validate {@link VFSDataStore#setFileSystemOptionsPropertiesInString(String)}.
     */
    public void testSetFileSystemOptionsPropertiesInString() throws Exception {
        try {
            long start = System.currentTimeMillis();
            LOG.info("Testcase: " + this.getClass().getName()
                + "#setFileSystemOptionsPropertiesInString, testDir=" + dataStoreDir);
            doSetFileSystemOptionsPropertiesInString();
            LOG.info("Testcase: " + this.getClass().getName()
                + "#setFileSystemOptionsPropertiesInString finished, time taken = ["
                + (System.currentTimeMillis() - start) + "]ms");
        } catch (Exception e) {
            LOG.error("error:", e);
            fail(e.getMessage());
        }
    }

    /**
     * Test {@link VFSDataStore#setFileSystemOptionsPropertiesInString(String)} and validate the internal properties.
     */
    protected void doSetFileSystemOptionsPropertiesInString() throws Exception {
        dataStore = new VFSDataStore();
        Properties props = getConfigProps();
        baseFolderUri = props.getProperty(VFSDataStore.BASE_FOLDER_URI);
        dataStore.setBaseFolderUri(baseFolderUri);
        LOG.info("baseFolderUri [{}] set.", baseFolderUri);
        dataStore.setFileSystemOptionsProperties(props);
        dataStore.setSecret("123456");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(SFTP_FILE_SYSTEM_OPTIONS_PARAM_XML)));
        Element paramElem = document.getDocumentElement();
        String propsInString = paramElem.getAttribute("value");
        dataStore.setFileSystemOptionsPropertiesInString(propsInString);
        final Properties internalProps = dataStore.getFileSystemOptionsProperties();
        Assert.assertEquals("/home/tester/.ssh/id_rsa", internalProps.getProperty("fso.sftp.identities"));
        Assert.assertEquals("30000", internalProps.getProperty("fso.sftp.timeout"));

        dataStore.init(dataStoreDir);

        final FileSystemOptions fso = dataStore.getFileSystemOptions();
        final SftpFileSystemConfigBuilder configBuilder = SftpFileSystemConfigBuilder.getInstance();
        File [] identities = configBuilder.getIdentities(fso);
        Assert.assertNotNull(identities);
        Assert.assertEquals(1, identities.length);
        Assert.assertEquals("/home/tester/.ssh/id_rsa", FilenameUtils.separatorsToUnix(identities[0].getPath()));
        Assert.assertEquals(Integer.valueOf(30000), configBuilder.getTimeout(fso));

        dataStore.close();
    }

    private Properties getConfigProps() {
        if (configProps == null) {
            Properties props = new Properties();
            String baseFolderUri = new File(new File(dataStoreDir), "vfsds").toURI().toString();
            props.setProperty(VFSDataStore.BASE_FOLDER_URI, baseFolderUri);
            // By default (when testing with local file system), disable asynchronous writing to the backend.
            props.setProperty(VFSDataStore.ASYNC_WRITE_POOL_SIZE, "0");
            configProps = props;
        }

        return configProps;
    }
}
