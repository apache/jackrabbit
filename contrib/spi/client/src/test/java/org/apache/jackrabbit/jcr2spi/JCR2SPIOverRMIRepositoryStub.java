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
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.rmi.client.ClientRepositoryService;
import org.apache.jackrabbit.spi.rmi.remote.RemoteRepositoryService;
import org.apache.jackrabbit.spi.rmi.common.ValueFactoryImpl;
import org.apache.log4j.PropertyConfigurator;

import javax.jcr.Repository;
import javax.jcr.ValueFactory;
import java.util.Properties;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * <code>JCR2SPIRepositoryStub</code> implements a repository stub that returns
 * a JCR2SPI Client that works on a SPI-RMI transport.
 */
public class JCR2SPIOverRMIRepositoryStub extends RepositoryStub {

    /**
     * Property for the repository url
     */
    public static final String PROP_REPOSITORY_NAME = "org.apache.jackrabbit.spi.rmi.repository.name";

    static {
        PropertyConfigurator.configure(JCR2SPIOverRMIRepositoryStub.class.getClassLoader().getResource("log4j.properties"));
    }

    /**
     * The repository instance
     */
    private Repository repository;

    /**
     * Overwritten constructor from base class.
     */
    public JCR2SPIOverRMIRepositoryStub(Properties env) {
        super(env);
    }

    public synchronized Repository getRepository() throws RepositoryStubException {
        if (repository == null) {
            try {
                String repName = environment.getProperty(PROP_REPOSITORY_NAME);

                Registry reg = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
                RemoteRepositoryService remoteService = (RemoteRepositoryService) reg.lookup(repName);
                final RepositoryService rmiService = new ClientRepositoryService(remoteService);

                RepositoryConfig config = new RepositoryConfig() {
                    public RepositoryService getRepositoryService() {
                        return rmiService;
                    }

                    public ValueFactory getValueFactory() {
                        return ValueFactoryImpl.getInstance();
                    }

                    public String getDefaultWorkspaceName() {
                        return null;
                    }

                    public CacheBehaviour getCacheBehaviour() {
                        return CacheBehaviour.INVALIDATE;
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
