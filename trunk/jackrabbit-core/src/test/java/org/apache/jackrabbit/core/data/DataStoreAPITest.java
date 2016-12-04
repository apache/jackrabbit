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

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.api.JackrabbitRepositoryFactory;
import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.api.management.RepositoryManager;
import org.apache.jackrabbit.core.RepositoryFactoryImpl;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Test data store garbage collection as described in the wiki at
 * http://wiki.apache.org/jackrabbit/DataStore
 */
public class DataStoreAPITest extends TestCase {

    private static final String TEST_DIR = "target/repository-datastore-test";

    /**
     * Test data store garbage collection.
     */
    public void testDataStoreGarbageCollection() throws RepositoryException {
        JackrabbitRepositoryFactory rf = new RepositoryFactoryImpl();
        Properties prop = new Properties();
        prop.setProperty("org.apache.jackrabbit.repository.home", TEST_DIR);
        prop.setProperty("org.apache.jackrabbit.repository.conf", TEST_DIR + "/repository.xml");
        JackrabbitRepository rep = (JackrabbitRepository) rf.getRepository(prop);
        RepositoryManager rm = rf.getRepositoryManager(rep);

        // need to login to start the repository
        Session session = rep.login();

        DataStoreGarbageCollector gc = rm.createDataStoreGarbageCollector();
        try {
            gc.mark();
            gc.sweep();
        } finally {
            gc.close();
        }

        session.logout();
        rm.stop();
    }

    public void tearDown() throws IOException {
        setUp();
    }

    public void setUp() throws IOException {
        FileUtils.deleteDirectory(new File(TEST_DIR));
    }

}
