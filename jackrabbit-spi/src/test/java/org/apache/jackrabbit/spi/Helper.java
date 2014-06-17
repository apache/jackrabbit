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
package org.apache.jackrabbit.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Credentials;
import java.util.HashMap;
import java.util.Map;

/** <code>Helper</code>... */
public class Helper {

    private static Logger log = LoggerFactory.getLogger(Helper.class);

    /**
     * RepositoryService stub reference.
     */
    private RepositoryServiceStub repoServiceStub;

    /**
     * Overlay configuration.
     */
    private Map configuration = new HashMap();

    /**
     * Creates a repository helper with configuration from
     * <code>repositoryStubImpl.properties</code> file.
     */
    public Helper() {
    }

    /**
     * Creates a repository helper with additional configuration parameters.
     *
     * @param config configuration which overlays the values from the property
     *   file.
     */
    public Helper(Map config) {
        configuration.putAll(config);
    }

    /**
     * Returns the repository service instance to test.
     * @return the repository service instance to test.
     * @throws RepositoryException if the repository could not be obtained.
     */
    public RepositoryService getRepositoryService() throws RepositoryException {
        if (repoServiceStub == null) {
            repoServiceStub = RepositoryServiceStub.getInstance(configuration);
        }
        return repoServiceStub.getRepositoryService();
    }

    /**
     * Returns the value of the configuration property with specified
     * <code>name</code>. If the property does not exist <code>null</code> is
     * returned.
     * <p>
     * Configuration properties are defined in the file:
     * <code>repositoryStubImpl.properties</code>.
     *
     * @param name the name of the property to retrieve.
     * @return the value of the property or <code>null</code> if non existent.
     * @throws RepositoryException if the configuration file cannot be found.
     */
    public String getProperty(String name) throws RepositoryException {
        // force assignment of repoStub
        getRepositoryService();
        return repoServiceStub.getProperty(name);
    }

    public Credentials getAdminCredentials() throws RepositoryException {
        // force assignment of repoStub
        getRepositoryService();
        return repoServiceStub.getAdminCredentials();
    }

    public Credentials getReadOnlyCredentials() throws RepositoryException {
        // force assignment of repoStub
        getRepositoryService();
        return repoServiceStub.getReadOnlyCredentials();
    }

    public SessionInfo getAdminSessionInfo() throws RepositoryException {
        // force assignment of repoStub
        getRepositoryService();
        String propName = RepositoryServiceStub.PROP_PREFIX + "." + RepositoryServiceStub.PROP_WORKSPACE;
        String wspName = repoServiceStub.getProperty(propName);
        return getRepositoryService().obtain(getAdminCredentials(), wspName);
    }

    public SessionInfo getReadOnlySessionInfo() throws RepositoryException {
        // force assignment of repoStub
        getRepositoryService();
        String propName = RepositoryServiceStub.PROP_PREFIX + "." + RepositoryServiceStub.PROP_WORKSPACE;
        String wspName = repoServiceStub.getProperty(propName);
        return getRepositoryService().obtain(getReadOnlyCredentials(), wspName);
    }
}