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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.spi.RepositoryServiceStub;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.core.jndi.RegistryHelper;

import javax.jcr.RepositoryException;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;
import javax.jcr.Credentials;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.Hashtable;

/** <code>ServiceStubImpl</code>... */
public class ServiceStubImpl extends RepositoryServiceStub {

    private static Logger log = LoggerFactory.getLogger(ServiceStubImpl.class);

    /**
     * Property for the repository name (used for jndi lookup)
     */
    public static final String PROP_REPOSITORY_NAME = "org.apache.jackrabbit.spi2jcr.name";

    /**
     * Property for the repository configuration file (used for repository instantiation)
     */
    public static final String PROP_REPOSITORY_CONFIG = "org.apache.jackrabbit.spi2jcr.config";

    /**
     * Property for the repository home directory (used for repository instantiation)
     */
    public static final String PROP_REPOSITORY_HOME = "org.apache.jackrabbit.spi2jcr.home";

    /**
     * Property for the jaas config path. If the system property
     * <code>java.security.auth.login.config</code> is not set this repository
     * stub will try to read this property from the environment and use the
     * value retrieved as the value for the system property.
     */
    public static final String PROP_JAAS_CONFIG = "org.apache.jackrabbit.spi2jcr.jaas.config";

    /**
     * The name of the jaas config system property.
     */
    private static final String SYS_JAAS_CONFIG = "java.security.auth.login.config";


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
                String repName = environment.getProperty(PROP_REPOSITORY_NAME);
                String repConfig = environment.getProperty(PROP_REPOSITORY_CONFIG);
                String repHome = environment.getProperty(PROP_REPOSITORY_HOME);
                String jaasConfig = environment.getProperty(PROP_JAAS_CONFIG);

                // set jaas config from stub properties if system property is
                // not set.
                if (System.getProperty(SYS_JAAS_CONFIG) == null && jaasConfig != null) {
                    System.setProperty(SYS_JAAS_CONFIG, jaasConfig);
                }

                // register repository instance
                Hashtable env = new Hashtable();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
                env.put(Context.PROVIDER_URL, "localhost");
                InitialContext ctx = new InitialContext(env);
                RegistryHelper.registerRepository(ctx, repName, repConfig, repHome, true);

                repository = (Repository) ctx.lookup(repName);
            } catch (Exception e) {
                throw new RepositoryException(e.toString());
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