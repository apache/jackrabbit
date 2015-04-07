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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.core.JackrabbitRepositoryStub;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.jcr2spi.AbstractRepositoryConfig;
import org.apache.jackrabbit.jcr2spi.RepositoryImpl;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStubException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.security.Principal;
import java.util.Properties;

/**
 * <code>RepositoryStubImpl</code> implements a repository stub that
 * initializes a Jackrabbit repository and wraps it with a SPI2JCR layer and
 * a JCR2SPI layer.
 */
public class RepositoryStubImpl extends JackrabbitRepositoryStub {

    /**
     * The Jackrabbit repository.
     */
    private Repository repo;

    /**
     * Constructor required by TCK.
     *
     * @param env the environment.
     */
    public RepositoryStubImpl(Properties env) {
        super(env);
    }

    /**
     * @return the repository instance to test.
     * @throws RepositoryStubException if an error occurs while starting up the
     *                                 repository.
     */
    public Repository getRepository() throws RepositoryStubException {
        if (repo == null) {
            try {
                final RepositoryService service = getRepositoryService();
                repo = RepositoryImpl.create(new AbstractRepositoryConfig() {
                    public RepositoryService getRepositoryService() {
                        return service;
                    }
                });
            } catch (RepositoryException e) {
                throw new RepositoryStubException(e);
            }
        }
        return repo;
    }

    @Override
    public Principal getKnownPrincipal(Session session) throws RepositoryException {
        return EveryonePrincipal.getInstance();
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
        brconfig.setDepth(NameConstants.NT_UNSTRUCTURED, BatchReadConfig.DEPTH_INFINITE);

        return new RepositoryServiceImpl(jackrabbitRepo, brconfig);
    }
}
