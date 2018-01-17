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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.test.RepositoryStubException;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.jcr2spi.AbstractRepositoryConfig;
import org.apache.jackrabbit.jcr2spi.RepositoryImpl;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.security.Principal;
import java.util.Properties;

/**
 * <code>RepositoryStubImpl</code>...
 */
public class RepositoryStubImpl extends RepositoryStub {

    /**
     * Property for the repository url
     */
    public static final String PROP_REPOSITORY_URL = "org.apache.jackrabbit.jcr2spi.repository.url";

    /**
     * The repository instance
     */
    private Repository repository;

    /**
     * Overwritten constructor from base class.
     */
    public RepositoryStubImpl(Properties env) {
        super(env);
    }

    @Override
    public synchronized Repository getRepository() throws RepositoryStubException {
        if (repository == null) {
            try {
                String url = environment.getProperty(PROP_REPOSITORY_URL);
                final RepositoryService service = createService(url);
                repository = RepositoryImpl.create(new AbstractRepositoryConfig() {
                    public RepositoryService getRepositoryService() {
                        return service;
                    }
                });
            } catch (Exception e) {
                throw new RepositoryStubException(e);
            }
        }
        return repository;
    }

    protected RepositoryService createService(String uri) throws RepositoryException {
        IdFactory idFactory = IdFactoryImpl.getInstance();
        NameFactory nFactory = NameFactoryImpl.getInstance();
        PathFactory pFactory = PathFactoryImpl.getInstance();
        QValueFactory vFactory = QValueFactoryImpl.getInstance();
        return new RepositoryServiceImpl(uri, idFactory, nFactory, pFactory, vFactory);
    }

    @Override
    public Principal getKnownPrincipal(Session session) throws RepositoryException {
        // TODO Auto-generated method stub
        throw new RepositoryException("TBD");
    }

    @Override
    public Principal getUnknownPrincipal(Session session) throws RepositoryException, NotExecutableException {
        // TODO Auto-generated method stub
        throw new RepositoryException("TBD");
    }
}
