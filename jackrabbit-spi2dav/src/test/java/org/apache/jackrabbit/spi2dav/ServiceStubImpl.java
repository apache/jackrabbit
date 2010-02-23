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

import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.RepositoryServiceStub;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import java.util.Properties;

/** <code>ServiceStubImpl</code>... */
public class ServiceStubImpl extends RepositoryServiceStub {

    private static Logger log = LoggerFactory.getLogger(ServiceStubImpl.class);

    public static final String PROP_REPOSITORY_URI = "org.apache.jackrabbit.spi.uri";

    private RepositoryService service;
    private Credentials adminCredentials;
    private Credentials readOnlyCredentials;

    /**
     * Implementations of this class must overwrite this constructor.
     *
     * @param env the environment variables. This parameter must not be null.
     */
    public ServiceStubImpl(Properties env) {
        super(env);
    }

    @Override
    public RepositoryService getRepositoryService() throws RepositoryException {
        if (service == null) {
            String uri = getProperty(PROP_REPOSITORY_URI);
            IdFactory idFactory = IdFactoryImpl.getInstance();
            NameFactory nFactory = NameFactoryImpl.getInstance();
            PathFactory pFactory = PathFactoryImpl.getInstance();
            QValueFactory vFactory = QValueFactoryImpl.getInstance();
            service = new RepositoryServiceImpl(uri, idFactory, nFactory, pFactory, vFactory);
        }
        return service;
    }

    @Override
    public Credentials getAdminCredentials() {
        if (adminCredentials == null) {
            adminCredentials = new SimpleCredentials(getProperty(RepositoryServiceStub.PROP_PREFIX + "." + RepositoryServiceStub.PROP_ADMIN_NAME),
                    getProperty(RepositoryServiceStub.PROP_PREFIX + "." + RepositoryServiceStub.PROP_ADMIN_PWD).toCharArray());
        }
        return adminCredentials;
    }

    @Override
    public Credentials getReadOnlyCredentials() {
        if (readOnlyCredentials == null) {
            readOnlyCredentials = new SimpleCredentials(getProperty(RepositoryServiceStub.PROP_PREFIX + "." + RepositoryServiceStub.PROP_READONLY_NAME),
                getProperty(RepositoryServiceStub.PROP_PREFIX + "." + RepositoryServiceStub.PROP_READONLY_PWD).toCharArray());
        }
        return readOnlyCredentials;
    }
}
