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
package org.apache.jackrabbit.core;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import javax.jcr.Session;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.config.RepositoryConfig;

/**
 * Tests the creation of a repository using Oracle persistence and different table and index tablespaces.
 *
 * @author Edouard Hue <edouard.hue@gmail.com>
 */
public class OracleRepositoryTest extends TestCase {
    private File dir;

    private RepositoryConfig config;

    protected void setUp() throws Exception {
        final Properties sysProps = System.getProperties();
        if (!sysProps.containsKey("tests.oracle.url")
            || !sysProps.containsKey("tests.oracle.user")
            || !sysProps.containsKey("tests.oracle.password")
            || !sysProps.containsKey("tests.oracle.tablespace")
            || !sysProps.containsKey("tests.oracle.indexTablespace")) {
            throw new IllegalStateException("Missing system property for test");
        }
        dir = File.createTempFile("jackrabbit_", null, new File("target"));
        dir.delete();
        dir.mkdir();
        final InputStream in = getClass().getResourceAsStream(
                "/org/apache/jackrabbit/core/repository-oracle.xml");
        config = RepositoryConfig.create(in, dir.getPath());
    }
    
    /**
     * Attempt to start a {@link TransientRepository} using {@link #config}, open
     * a new session with default credentials and workspace, then shutdown the repo.
     */
    public void testConfiguration() throws Exception {
        final TransientRepository repo = new TransientRepository(config);
        try {
            final Session session = repo.login();
            session.logout();
        } finally {
            repo.shutdown();
        }
    }

    protected void tearDown() throws Exception {
        if (dir != null) {
            FileUtils.deleteQuietly(dir);
        }
    }
}
