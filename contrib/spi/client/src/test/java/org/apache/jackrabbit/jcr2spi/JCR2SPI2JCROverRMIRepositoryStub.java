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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.test.RepositoryStubException;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.rmi.server.ServerRepositoryService;
import org.apache.jackrabbit.spi.rmi.remote.RemoteRepositoryService;
import org.apache.jackrabbit.spi.rmi.client.ClientRepositoryService;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.util.Properties;
import java.rmi.RemoteException;

/**
 * <code>JCR2SPI2JCROverRMIRepositoryStub</code> implements a repository stub that
 * initializes a Jackrabbit repository and wraps it with a SPI2JCR layer,
 * a SPI-RMI layer and a JCR2SPI layer.
 */
public class JCR2SPI2JCROverRMIRepositoryStub extends JCR2SPI2JCRRepositoryStub {

    /**
     * The Jackrabbit repository.
     */
    private Repository repo;

    /**
     * Constructor required by TCK.
     *
     * @param env the environment.
     */
    public JCR2SPI2JCROverRMIRepositoryStub(Properties env) {
        super(env);
    }

    /**
     * @return the repository instance to test.
     * @throws RepositoryStubException if an error occurs while starting up the
     *                                 repository.
     */
    public Repository getRepository() throws RepositoryStubException {
        if (repo == null) {
            RepositoryService spi2jcrRepoService = super.getRepositoryService();
            try {
                RemoteRepositoryService remoteRepoService = new ServerRepositoryService(spi2jcrRepoService);
                final RepositoryService localRepoService = new ClientRepositoryService(remoteRepoService);

                repo = RepositoryImpl.create(new AbstractRepositoryConfig() {
                    public RepositoryService getRepositoryService() {
                        return localRepoService;
                    }
                });
            } catch (RepositoryException e) {
                RepositoryStubException ex = new RepositoryStubException(e.getMessage());
                ex.initCause(e);
                throw ex;
            } catch (RemoteException e) {
                RepositoryStubException ex = new RepositoryStubException(e.getMessage());
                ex.initCause(e);
                throw ex;
            }
        }
        return repo;
    }
}
