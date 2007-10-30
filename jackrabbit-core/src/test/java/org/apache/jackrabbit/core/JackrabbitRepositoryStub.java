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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.jndi.RegistryHelper;
import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.test.RepositoryStubException;

import javax.jcr.Repository;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Implements the <code>RepositoryStub</code> for the JCR Reference Implementation.
 */
public class JackrabbitRepositoryStub extends RepositoryStub {

    /**
     * Property for the repository name (used for jndi lookup)
     */
    public static final String PROP_REPOSITORY_NAME = "org.apache.jackrabbit.repository.name";

    /**
     * Property for the repository configuration file (used for repository instantiation)
     */
    public static final String PROP_REPOSITORY_CONFIG = "org.apache.jackrabbit.repository.config";

    /**
     * Property for the repository home directory (used for repository instantiation)
     */
    public static final String PROP_REPOSITORY_HOME = "org.apache.jackrabbit.repository.home";

    /**
     * Property for the jaas config path. If the system property
     * <code>java.security.auth.login.config</code> is not set this repository
     * stub will try to read this property from the environment and use the
     * value retrieved as the value for the system property.
     */
    public static final String PROP_JAAS_CONFIG = "org.apache.jackrabbit.repository.jaas.config";

    /**
     * The name of the jaas config system property.
     */
    private static final String SYS_JAAS_CONFIG = "java.security.auth.login.config";

    /**
     * The repository instance
     */
    private Repository repository;

    /**
     * Constructor as required by the JCR TCK.
     *
     * @param env environment properties.
     */
    public JackrabbitRepositoryStub(Properties env) {
        super(env);
        // set some attributes on the sessions
        superuser.setAttribute("jackrabbit", "jackrabbit");
        readwrite.setAttribute("jackrabbit", "jackrabbit");
        readonly.setAttribute("jackrabbit", "jackrabbit");
    }

    /**
     * Returns the configured <code>Repository</code> instance.
     * <br>
     * The default repository name is 'repo'.
     *
     * @return the configured <code>Repository</code> instance.
     * @throws RepositoryStubException if an error occurs while
     *                                 obtaining the Repository instance.
     */
    public synchronized Repository getRepository() throws RepositoryStubException {
        if (repository == null) {
            try {
                String repName = environment.getProperty(PROP_REPOSITORY_NAME, "repo");
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
                e.printStackTrace();
                throw new RepositoryStubException(e.toString());
            }
        }
        return repository;
    }
}
