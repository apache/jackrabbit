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

import java.util.Properties;
import java.util.Collections;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.RepositoryServiceStub;
import org.apache.jackrabbit.test.RepositoryStubException;
import org.apache.jackrabbit.test.RepositoryStub;

/** <code>ServiceStubImpl</code>... */
public class ServiceStubImpl extends RepositoryServiceStub {

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

    public RepositoryService getRepositoryService() throws RepositoryException {
        if (service == null) {
            Repository repository;
            try {
                repository = RepositoryStub.getInstance(Collections.EMPTY_MAP).getRepository();
            } catch (RepositoryStubException e) {
                throw new RepositoryException(e);
            }
            service = new RepositoryServiceImpl(repository, new BatchReadConfig());
        }
        return service;
    }

    public Credentials getAdminCredentials() {
        if (adminCredentials == null) {
            adminCredentials = new SimpleCredentials(getProperty(PROP_PREFIX + "." + PROP_ADMIN_NAME),
                    getProperty(PROP_PREFIX + "." + PROP_ADMIN_PWD).toCharArray());
        }
        return adminCredentials;
    }

    public Credentials getReadOnlyCredentials() {
        if (readOnlyCredentials == null) {
            readOnlyCredentials = new SimpleCredentials(getProperty(PROP_PREFIX + "." + PROP_READONLY_NAME),
                getProperty(PROP_PREFIX + "." + PROP_READONLY_PWD).toCharArray());
        }
        return readOnlyCredentials;
    }
}