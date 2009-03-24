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
package org.apache.jackrabbit.spi2davex;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.RepositoryServiceStub;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import java.util.Properties;

/** <code>ServiceStubImpl</code>... */
public class ServiceStubImpl extends RepositoryServiceStub {

    private static Logger log = LoggerFactory.getLogger(ServiceStubImpl.class);

    public static final String PROP_REPOSITORY_URI = "org.apache.jackrabbit.spi.spi2davex.uri";
    public static final String PROP_DEFAULT_DEPTH = "org.apache.jackrabbit.spi.spi2davex.defaultDepth";
    public static final String PROP_WSP_NAME = "org.apache.jackrabbit.spi.spi2davex.workspacename";

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

    /**
     * Workaround for SPI issue JCR-1851
     * 
     * @param propName Configuration property name.
     * @return Configured property value.
     */
    public String getProperty(String propName) {
        String prop = super.getProperty(propName);
        if (prop == null && propName.equals("workspacename")) {
            prop = super.getProperty(PROP_WSP_NAME);
        }
        return prop;
    }

    public RepositoryService getRepositoryService() throws RepositoryException {
        if (service == null) {
            String uri = getProperty(PROP_REPOSITORY_URI);
            service = new RepositoryServiceImpl(uri, new BatchReadConfig() {
                public int getDepth(Path path, PathResolver resolver) {
                    String depthStr = getProperty(PROP_DEFAULT_DEPTH);
                    return Integer.parseInt(depthStr);
                }
            });
        }
        return service;
    }

    public Credentials getAdminCredentials() {
        if (adminCredentials == null) {
            adminCredentials = new SimpleCredentials(getProperty(RepositoryServiceStub.PROP_PREFIX + "." + RepositoryServiceStub.PROP_ADMIN_NAME),
                    getProperty(RepositoryServiceStub.PROP_PREFIX + "." + RepositoryServiceStub.PROP_ADMIN_PWD).toCharArray());
        }
        return adminCredentials;
    }

    public Credentials getReadOnlyCredentials() {
        if (readOnlyCredentials == null) {
            readOnlyCredentials = new SimpleCredentials(getProperty(RepositoryServiceStub.PROP_PREFIX + "." + RepositoryServiceStub.PROP_READONLY_NAME),
                getProperty(RepositoryServiceStub.PROP_PREFIX + "." + RepositoryServiceStub.PROP_READONLY_PWD).toCharArray());
        }
        return readOnlyCredentials;
    }
}