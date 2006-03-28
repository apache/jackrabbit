/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;

/**
 * Utility class for easy handling a test repository. This class contains
 * a static test repository instance for use by test cases. The
 * {@link javax.jcr.Repository#login()} method of the test repository
 * instance should return a session with full read-write access.
 */
public class TestRepository {

    /**
     * Name of the resource containing the test repository configuration.
     * The test repository configuration is located inside the Jackrabbit
     * jar file to enforce a standard test environment.
     */
    private static final String CONF_RESOURCE = "test-repository.xml";

    /**
     * Name of the system property that can be used to override the
     * default test repository location.
     */
    private static final String HOME_PROPERTY =
        "org.apache.jackrabbit.test.repository.home";

    /**
     * Default test repository location.
     */
    private static final String HOME_DEFAULT = "jackrabbit-test-repository";

    /**
     * The test repository instance.
     */
    private static Repository instance = null;

    /**
     * Returns the test repository instance. If a repository instance has
     * not yet been registered using {@link #setInstance(Repository)} as
     * the test repostitory, then a simple {@link TransientRepository}
     * instance is created with the standard test repository configuration
     * and the test repository location (either "jackrabbit-test-repository"
     * or the value of the "org.apache.jackrabbit.test.repository.home"
     * system property).
     *
     * @return test repository instance
     * @throws RepositoryException if a test repository can not be instantiated
     */
    public static synchronized Repository getInstance() throws RepositoryException {
        try {
            if (instance == null) {
                ClassLoader loader = TestRepository.class.getClassLoader();
                InputStream xml = loader.getResourceAsStream(CONF_RESOURCE);
                String home = System.getProperty(HOME_PROPERTY, HOME_DEFAULT);
                RepositoryConfig config = RepositoryConfig.create(xml, home);
                instance = new TransientRepository(config);
            }
            return instance;
        } catch (ConfigurationException e) {
            throw new RepositoryException(
                    "Error in test repository configuration", e);
        } catch (IOException e) {
            throw new RepositoryException(
                    "Error in test repository initialization", e);
        }
    }

    /**
     * Sets the given repository as the test repository instance. This method
     * is designed for use by the main Jackrabbit test suite to facilitate
     * smooth integration of standalone test cases.
     *
     * @param repository test repository
     */
    public static synchronized void setInstance(Repository repository) {
        instance = repository;
    }

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private TestRepository() {
    }

}
