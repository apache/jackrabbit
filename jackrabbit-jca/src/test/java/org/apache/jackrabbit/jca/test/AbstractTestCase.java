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
package org.apache.jackrabbit.jca.test;

import junit.framework.TestCase;
import org.apache.jackrabbit.jca.JCAManagedConnectionFactory;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import java.io.File;

/**
 * This implements the abstract test case.
 */
public abstract class AbstractTestCase
        extends TestCase {

    /**
     * Repository home directory.
     */
    public static final String JCR_HOME_DIR = "target/repository";

    /**
     * Repository configuration file.
     */
    public static final String JCR_CONFIG_FILE =
        "classpath:org/apache/jackrabbit/core/repository.xml";

    /**
     * Default credentials.
     */
    public static final Credentials JCR_SUPERUSER =
            new SimpleCredentials("admin", "admin".toCharArray());

    /**
     * Anonymous credentials.
     */
    public static final Credentials JCR_ANONUSER =
            new SimpleCredentials("anonymous", new char[0]);

    /**
     * Repository workspace.
     */
    public static final String JCR_WORKSPACE = "default";

    /**
     * Managed connection factory.
     */
    protected JCAManagedConnectionFactory mcf;

    /**
     * Setup the test.
     */
    protected void setUp() throws Exception {
        File home = new File(JCR_HOME_DIR);
        if (!home.exists()) {
            home.mkdirs();
        }

        // Construct the managed connection factory
        this.mcf = new JCAManagedConnectionFactory();
        this.mcf.setHomeDir(JCR_HOME_DIR);
        this.mcf.setConfigFile(JCR_CONFIG_FILE);
    }

}
