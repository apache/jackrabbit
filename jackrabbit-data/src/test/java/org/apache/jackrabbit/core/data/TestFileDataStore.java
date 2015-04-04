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

package org.apache.jackrabbit.core.data;

import java.io.File;

import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test cases to test {@link FileDataStore}
 */
public class TestFileDataStore extends TestCaseBase {

    protected static final Logger LOG = LoggerFactory.getLogger(TestFileDataStore.class);

    String path;

    @Override
    protected DataStore createDataStore() throws RepositoryException {
        FileDataStore fds = new FileDataStore();
        path = dataStoreDir + "/repository/datastore";
        fds.setPath(path);
        fds.init(dataStoreDir);
        return fds;
    }

    @Override
    protected void tearDown() {
        File f = new File(path);
        try {
            for (int i = 0; i < 4 && f.exists(); i++) {
                FileUtils.deleteQuietly(f);
                Thread.sleep(2000);
            }
        } catch (Exception ignore) {

        }
    }

}
