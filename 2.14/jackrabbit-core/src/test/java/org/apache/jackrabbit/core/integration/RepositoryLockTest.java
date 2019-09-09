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
package org.apache.jackrabbit.core.integration;

import java.io.File;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;

import junit.framework.TestCase;

/**
 * <code>RepositoryLockTest</code> checks if multiple instatiations of a
 * repository are prevented and the repository lock file is respected (i.e. not
 * deleted). See also JCR-2057.
 */
public class RepositoryLockTest extends TestCase {

    private static final File TARGET = new File("target");

    private static final File REPO_HOME = new File(TARGET, "repository-lock-test");

    private static final File REPO_CONF = new File(new File(TARGET, "repository"), "repository.xml");

    private RepositoryImpl repo;

    /**
     * Makes sure the repository is shutdown.
     */
    protected void tearDown() throws Exception {
        if (repo != null) {
            repo.shutdown();
            repo = null;
        }
        super.tearDown();
    }

    public void testMultipleInstantiation() throws Exception {
        RepositoryConfig config = RepositoryConfig.create(
                REPO_CONF.getAbsolutePath(), REPO_HOME.getAbsolutePath());
        repo = RepositoryImpl.create(config);

        for (int i = 0; i < 3; i++) {
            // try again
            try {
                repo = RepositoryImpl.create(config);
                fail("Multiple instantiation must not be possible");
            } catch (RepositoryException e) {
                // expected
            }
            // check if lock file is still there, see JCR-2057
            assertTrue("repository lock file deleted", new File(REPO_HOME, ".lock").exists());
        }
    }
}


