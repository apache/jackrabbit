/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.test.RepositoryStubException;
import org.apache.jackrabbit.spi2dav.RepositoryServiceImpl;
import org.apache.jackrabbit.identifier.IdFactoryImpl;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.value.ValueFactoryImplEx;
import org.apache.log4j.PropertyConfigurator;

import javax.jcr.Repository;
import javax.jcr.ValueFactory;
import java.util.Properties;

/**
 * <code>JCR2SPIRepositoryStub</code> implements a repository stub that returns
 * a JCR2SPI Client that works on a SPI2DAV implementation which in turn
 * connects to an JCR WebDAV Server.
 */
public class JCR2SPIRepositoryStub extends RepositoryStub {

    /**
     * Property for the repository url
     */
    public static final String PROP_REPOSITORY_URL = "org.apache.jackrabbit.jcr2spi.repository.url";
    /**
     * Property for the default workspace name
     */
    public static final String PROP_WORKSPACE_NAME = "org.apache.jackrabbit.jcr2spi.workspace.name";

    static {
        PropertyConfigurator.configure(JCR2SPIRepositoryStub.class.getClassLoader().getResource("log4j.properties"));
    }

    /**
     * The repository instance
     */
    private Repository repository;

    /**
     * Overwritten constructor from base class.
     */
    public JCR2SPIRepositoryStub(Properties env) {
        super(env);
    }

    public synchronized Repository getRepository() throws RepositoryStubException {
        if (repository == null) {
            try {
                String url = environment.getProperty(PROP_REPOSITORY_URL);

                final IdFactory idFactory = IdFactoryImpl.getInstance();
                final ValueFactory vFactory = ValueFactoryImplEx.getInstance();
                final RepositoryServiceImpl webdavRepoService = new RepositoryServiceImpl(url, idFactory, vFactory);

                RepositoryConfig config = new AbstractRepositoryConfig() {
                    public RepositoryService getRepositoryService() {
                        return webdavRepoService;
                    }

                    public ValueFactory getValueFactory() {
                        return vFactory;
                    }

                    public String getDefaultWorkspaceName() {
                        String name = environment.getProperty(PROP_WORKSPACE_NAME);
                        return name;
                    }
                };

                repository = RepositoryImpl.create(config);
            } catch (Exception e) {
                throw new RepositoryStubException(e.toString());
            }
        }
        return repository;
    }
}
