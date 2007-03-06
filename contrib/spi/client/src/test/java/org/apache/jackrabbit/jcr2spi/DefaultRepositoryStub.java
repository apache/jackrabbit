/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.test.RepositoryStub;
import org.apache.jackrabbit.test.RepositoryStubException;
import org.apache.jackrabbit.core.jndi.RegistryHelper;

import javax.jcr.Repository;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.Hashtable;

/**
 * Implements the <code>RepositoryStub</code> for the JCR Reference Implementation.
 * TODO: copied from jackrabbit/core test classes
 */
public class DefaultRepositoryStub extends RepositoryStub {

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
    public DefaultRepositoryStub(Properties env) {
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
                String repName = environment.getProperty(DefaultRepositoryStub.PROP_REPOSITORY_NAME, "repo");
                String repConfig = environment.getProperty(DefaultRepositoryStub.PROP_REPOSITORY_CONFIG);
                String repHome = environment.getProperty(DefaultRepositoryStub.PROP_REPOSITORY_HOME);
                String jaasConfig = environment.getProperty(DefaultRepositoryStub.PROP_JAAS_CONFIG);

                // set jaas config from stub properties if system property is
                // not set.
                if (System.getProperty(DefaultRepositoryStub.SYS_JAAS_CONFIG) == null && jaasConfig != null) {
                    System.setProperty(DefaultRepositoryStub.SYS_JAAS_CONFIG, jaasConfig);
                }

                // register repository instance
                Hashtable env = new Hashtable();
                env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
                env.put(Context.PROVIDER_URL, "localhost");
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
