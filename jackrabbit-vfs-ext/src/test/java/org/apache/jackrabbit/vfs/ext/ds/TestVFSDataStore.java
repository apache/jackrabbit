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
import java.net.URI;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.TestCaseBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test {@link CachingDataStore} with VFSBackend and local cache on. It requires
 * to pass vfs config file via system property. For e.g.
 * -Dconfig=/opt/repository/vfs.properties. Sample vfs properties located at
 * src/test/resources/vfs.properties
 */
public class TestVFSDataStore extends TestCaseBase {

    protected static final Logger LOG = LoggerFactory.getLogger(TestVFSDataStore.class);

    private String vfsUri;

    @Override
    protected CachingDataStore createDataStore() throws RepositoryException {
        VFSDataStore vfsds = new VFSDataStore();
        Properties props = loadProperties("/vfs.properties");

        String uriValue = props.getProperty(VFSBackend.VFS_BACKEND_URI);
        if (uriValue != null && !"".equals(uriValue.trim())) {
            vfsUri = uriValue + "/vfsds" + "-"
                + String.valueOf(randomGen.nextInt(100000)) + "-"
                + String.valueOf(randomGen.nextInt(100000));
        } else {
            vfsUri = new File(new File(dataStoreDir), "vfsds").toURI().toString();
        }
        props.setProperty(VFSBackend.VFS_BACKEND_URI, vfsUri);
        LOG.info("vfsBackendUri [{}] set.", vfsUri);
        vfsds.setProperties(props);
        vfsds.init(dataStoreDir);

        return vfsds;
    }

    @Override
    protected void tearDown() {
        LOG.info("cleaning vfsUri [{}]", vfsUri);
        File f = new File(URI.create(vfsUri));
        try {
            for (int i = 0; i < 4 && f.exists(); i++) {
                FileUtils.deleteQuietly(f);
                Thread.sleep(2000);
            }
        } catch (Exception ignore) {

        }
        super.tearDown();
    }

}
