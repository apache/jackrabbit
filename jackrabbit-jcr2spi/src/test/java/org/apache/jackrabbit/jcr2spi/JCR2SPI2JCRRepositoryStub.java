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
import org.apache.jackrabbit.spi2jcr.RepositoryServiceImpl;
import org.apache.jackrabbit.spi2jcr.BatchReadConfig;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.name.NameConstants;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.util.Properties;

/**
 * <code>JCR2SPI2JCRRepositoryStub</code> implements a repository stub that
 * initializes a Jackrabbit repository and wraps it with a SPI2JCR layer and
 * a JCR2SPI layer.
 */
public class JCR2SPI2JCRRepositoryStub extends DefaultRepositoryStub {

    /**
     * The Jackrabbit repository.
     */
    private Repository repo;

    /**
     * Constructor required by TCK.
     *
     * @param env the environment.
     */
    public JCR2SPI2JCRRepositoryStub(Properties env) {
        super(env);
    }

    /**
     * @return the repository instance to test.
     * @throws RepositoryStubException if an error occurs while starting up the
     *                                 repository.
     */
    public Repository getRepository() throws RepositoryStubException {
        if (repo == null) {
            final RepositoryService service = getRepositoryService();
            try {
                repo = RepositoryImpl.create(new AbstractRepositoryConfig() {
                    public RepositoryService getRepositoryService() {
                        return service;
                    }
                });
            } catch (RepositoryException e) {
                RepositoryStubException ex = new RepositoryStubException(e.getMessage());
                ex.initCause(e);
                throw ex;
            }
        }
        return repo;
    }

    /**
     *
     * @return
     * @throws RepositoryStubException
     */
    public RepositoryService getRepositoryService() throws RepositoryStubException {
        Repository jackrabbitRepo = super.getRepository();

        // TODO: make configurable
        BatchReadConfig brconfig = new BatchReadConfig();
        brconfig.setDepth(NameConstants.NT_FILE, BatchReadConfig.DEPTH_INFINITE);
        brconfig.setDepth(NameConstants.NT_RESOURCE, BatchReadConfig.DEPTH_INFINITE);

        return new RepositoryServiceImpl(jackrabbitRepo, brconfig);
    }
}
