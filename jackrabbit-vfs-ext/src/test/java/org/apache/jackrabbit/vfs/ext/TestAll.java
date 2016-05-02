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

package org.apache.jackrabbit.vfs.ext;

import org.apache.jackrabbit.core.data.TestCaseBase;
import org.apache.jackrabbit.vfs.ext.ds.TestVFSDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite that includes all test cases for the this module.
 */
public class TestAll extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(TestAll.class);

    /**
     * <code>TestAll</code> suite that executes all tests inside this module. To
     * run test cases against Commons VFS, pass VFS configuration properties file as
     * system property -Dconfig=/opt/repository/vfs.properties. Sample VFS properties
     * located at src/test/resources/vfs.properties.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("VFS tests");
        suite.addTestSuite(TestVFSDataStore.class);

        String config = System.getProperty(TestCaseBase.CONFIG);
        LOG.info("config= " + config);
        if (config != null && !"".equals(config.trim())) {
            // TODO
        }

        return suite;
    }
}
