/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.test;

import org.apache.jackrabbit.core.jndi.RegistryHelper;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Context;
import java.util.Properties;
import java.util.Hashtable;

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

                // register repository instance
                Hashtable env = new Hashtable();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
                InitialContext ctx = new InitialContext(env);
                RegistryHelper.registerRepository(ctx, repName, repConfig, repHome, true);

                repository = (Repository) ctx.lookup(repName);
            } catch (Exception e) {
                throw new RepositoryStubException(e.toString());
            }
        }
        return repository;
    }
}
