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
package org.apache.jackrabbit.ocm.repository;

import java.util.Hashtable;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.jndi.RegistryHelper;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.exception.RepositoryException;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;

/**
* Utility class for managing JCR repositories.
* <b>Note</b>: most of the utility methods in this class can be used only with Jackrabbit.
*
* @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
* @version $Id: Exp $
*/
public class RepositoryUtil
{

    /** namespace prefix constant */
    public static final String OCM_NAMESPACE_PREFIX   = "ocm";

    /** namespace constant */
    public static final String OCM_NAMESPACE          = "http://jackrabbit.apache.org/ocm";

    /** Item path separator */
    public static final String PATH_SEPARATOR = "/";

    private final static Log log = LogFactory.getLog(RepositoryUtil.class);

    /**
     * Register a new repository
     *
     * @param repositoryName The repository unique name
     * @param configFile The JCR config file
     * @param homeDir The directory containing the complete repository settings (workspace, node types, ...)
     *
     * @throws RepositoryException when it is not possible to register the repository
     */
    public static void registerRepository(String repositoryName, String configFile, String homeDir) throws RepositoryException
    {
        try
        {
            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
            env.put(Context.PROVIDER_URL, "localhost");
            InitialContext ctx = new InitialContext(env);

            RegistryHelper.registerRepository(ctx, repositoryName, configFile, homeDir, true);
        }
        catch (Exception e)
        {
            throw new RepositoryException("Impossible to register the respository : " +
                                           repositoryName + " - config file : " + configFile, e);
        }

    }


    /**
     * Unregister a repository
     *
     * @param repositoryName The repository unique name
     *
     * @throws RepositoryException when it is not possible to unregister the repository
     */
    public static void unRegisterRepository(String repositoryName) throws RepositoryException
    {
        try
        {
        	Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
            env.put(Context.PROVIDER_URL, "localhost");
            InitialContext ctx = new InitialContext(env);

            RegistryHelper.unregisterRepository(ctx, repositoryName);
        }
        catch (Exception e)
        {
            throw new RepositoryException("Impossible to unregister the respository : " +
                                           repositoryName , e);
        }

    }

    /**
     * Get a repository
     *
     * @param repositoryName The repository name
     * @return a JCR repository reference
     *
     * @throws RepositoryException when it is not possible to get the repository.
     *         Before calling this method, the repository has to be registered (@see RepositoryUtil#registerRepository(String, String, String)
     */
    public static Repository getRepository(String repositoryName) throws RepositoryException
    {
        try
        {
            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory");
            env.put(Context.PROVIDER_URL, "localhost");
            InitialContext ctx = new InitialContext(env);

            Repository repository = (Repository) ctx.lookup(repositoryName);
            return repository;
        }
        catch (Exception e)
        {
            throw new RepositoryException("Impossible to get the repository : " + repositoryName, e);
        }
    }

    /**
     * Connect to a JCR repository
     *
     * @param repository The JCR repository
     * @param user The user name
     * @param password The password
     * @return a valid JCR session
     *
     * @throws RepositoryException when it is not possible to connect to the JCR repository
     */
    public static Session login(Repository repository, String user, String password) throws RepositoryException
    {
        try
        {
            Session session = repository.login(new SimpleCredentials(user, password.toCharArray()), null);


            return session;
        }
        catch (Exception e)
        {
            throw new RepositoryException("Impossible to login ", e);
        }
    }





    /**
     * Setup the session.
     * Until now, we check only if the namespace prefix exist in the repository
     *
     */
    public static void setupSession(Session session) throws RepositoryException
    {
         try
         {
        	log.info("Setup Jcr session setup ...");
        	
            String[] jcrNamespaces = session.getWorkspace().getNamespaceRegistry().getPrefixes();
            boolean createNamespace = true;
            for (int i = 0; i < jcrNamespaces.length; i++)
            {
                if (jcrNamespaces[i].equals(OCM_NAMESPACE_PREFIX))
                {
                    createNamespace = false;
                    log.debug("Jackrabbit OCM namespace exists.");
                }
            }

            if (createNamespace)
            {
                session.getWorkspace().getNamespaceRegistry().registerNamespace(OCM_NAMESPACE_PREFIX, OCM_NAMESPACE);
                log.info("Successfully created Jackrabbit OCM namespace.");
            }

            if (session.getRootNode() != null)
            {
                log.info("Jcr session setup successfull.");
            }


        }
        catch (Exception e)
        {
            log.error("Error while setting up the jcr session.", e);
            throw new RepositoryException(e.getMessage());
        }
    }

    /**
     * Encode a path
     * @TODO : drop Jackrabbit dependency
     *
     * @param path the path to encode
     * @return the encoded path
     *
     */
    public static String encodePath(String path)
    {
    	String[] pathElements = Text.explode(path, '/');
    	for (int i=0;i<pathElements.length;i++)
    	{
    		pathElements[i] = ISO9075.encode(pathElements[i]);
    	}
    	return "/" + Text.implode(pathElements, "/");
    }
}
