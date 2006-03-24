/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.j2ee;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.log4j.Logger;

import javax.jcr.Repository;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This Class implements a servlet that is used as unified mechanism to retrieve
 * a jcr repository either through JNID, RMI or JCRWebdavServer.
 */
public class RepositoryAccessServlet extends HttpServlet {

    /**
     * default logger
     */
    private static final Logger log = Logger.getLogger(RepositoryAccessServlet.class);

    /**
     * the 'repository-name' init parameter
     */
    public final static String INIT_PARAM_REPOSITORY_NAME = "repository-name";

    /**
     * the 'rmi-uri' init parameter
     */
    public final static String INIT_PARAM_RMI_URI = "rmi-uri";

    /**
     * the 'missing-auth-mapping' init parameter
     */
    //public final static String INIT_PARAM_MISSING_AUTH_MAPPING = "missing-auth-mapping";

    private static final String CTX_ATTR_REPOSITORY = "jcr.repository";

    private static final String CTX_ATTR_REPOSITORY_NAME = "jcr.repository.name";

    private static final String CTX_ATTR_REPOSITORY_RMI_URI = "jcr.repository.rmiURI";

    private static final String CTX_ATTR_REPOSITORY_JNDI_CONTEXT = "jcr.repository.jndiContext";

    /**
     * Initializes this servlet
     *
     * @throws javax.servlet.ServletException
     */
    public void init() throws ServletException {
	log.info("RepositoryAccessServlet initializing...");
        // fetching the name
        String repositoryName = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_NAME);
        if (repositoryName == null) {
            repositoryName = "default";
        }
        getServletContext().setAttribute(CTX_ATTR_REPOSITORY_NAME, repositoryName);

        // fetching the rmiuri
        getServletContext().setAttribute(CTX_ATTR_REPOSITORY_RMI_URI, getRMIUri());

        // setup initial context
        getServletContext().setAttribute(CTX_ATTR_REPOSITORY_JNDI_CONTEXT, getInitialContext());

	log.info("RepositoryAccessServlet initialized.");
    }

    private InitialContext getInitialContext() {
	// retrieve JNDI Context environment
	try {
	    Properties env = new Properties();
	    Enumeration names = getServletConfig().getInitParameterNames();
	    while (names.hasMoreElements()) {
		String name = (String) names.nextElement();
		if (name.startsWith("java.naming.")) {
                    String initParam = getServletConfig().getInitParameter(name);
                    if (initParam.equals("")) {
                        log.info("  ignoring empty JNDI init param: " + name);
                    } else {
                        env.put(name, initParam);
                        log.info("  adding property to JNDI environment: " + name + "=" + initParam);
                    }
		}
	    }
	    return new InitialContext(env);
	} catch (NamingException e) {
	    log.error("Create initial context: " + e.toString());
	    return null;
	}
    }

    private String getRMIUri() {
	// setup repository name
	return getServletConfig().getInitParameter(INIT_PARAM_RMI_URI);
    }

    /**
     * tries to retrieve the repository using RMI
     */
    private static Repository getRepositoryByJNDI(ServletContext ctx) {
        // acquire via JNDI
        String repositoryName = (String) ctx.getAttribute(CTX_ATTR_REPOSITORY_NAME);
        InitialContext jndiContext = (InitialContext) ctx.getAttribute(CTX_ATTR_REPOSITORY_JNDI_CONTEXT);
        if (jndiContext == null) {
            return null;
        }
        try {
            Repository r = (Repository) jndiContext.lookup(repositoryName);
            log.info("Acquired repository via JNDI.");
            return r;
        } catch (NamingException e) {
            log.error("Error while retrieving repository using JNDI (name=" + repositoryName + "): " + e);
            return null;
        }
    }

    /**
     * tries to retrieve the repository using RMI
     */
    private static Repository getRepositoryByRMI(ServletContext ctx) {
        // acquire via RMI
        String rmiURI = (String) ctx.getAttribute(CTX_ATTR_REPOSITORY_RMI_URI);
        if (rmiURI == null) {
            return null;
        }
        log.info("  trying to retrieve repository using rmi. uri=" + rmiURI);
        ClientFactoryDelegater cfd;
        try {
            Class clazz = Class.forName("org.apache.jackrabbit.j2ee.RMIClientFactoryDelegater");
            cfd = (ClientFactoryDelegater) clazz.newInstance();
        } catch (NoClassDefFoundError e) {
            log.error("Unable to locate RMI ClientRepositoryFactory. jcr-rmi.jar missing? " + e.toString());
            return null;
        } catch (Exception e) {
            log.error("Unable to locate RMI ClientRepositoryFactory. jcr-rmi.jar missing?" + e.toString());
            return null;
        }

        try {
            Repository r = cfd.getRepository(rmiURI);
            log.info("Acquired repository via RMI.");
            return r;
        } catch (Exception e) {
            log.error("Error while retrieving repository using RMI: " + e);
            return null;
        }
    }

    /**
     * Returns the JSR170 repository
     *
     * @return a jsr170 repository
     * @throws IllegalStateException if the repository is not available in the context.
     */
    public static Repository getRepository(ServletContext ctx) {
        Repository repository = (Repository) ctx.getAttribute(CTX_ATTR_REPOSITORY);
        if (repository != null) {
            return repository;
        } else {
            repository = getRepositoryByRMI(ctx);
        }
        // try to retrieve via jndi
        if (repository == null) {
            repository = getRepositoryByJNDI(ctx);
        }
        // error
        if (repository == null) {
            log.fatal("The repository is not available. Check config of 'RepositoryAccessServlet'.");
            throw new IllegalStateException("The repository is not available.");
        } else {
            ctx.setAttribute(CTX_ATTR_REPOSITORY, repository);
            log.info(repository.getDescriptor(Repository.REP_NAME_DESC) + " v" + repository.getDescriptor(Repository.REP_VERSION_DESC));
            return repository;
        }
    }
}

/**
 * optional class for RMI, will only be used, if RMI client is present
 */
abstract class ClientFactoryDelegater {

    public abstract Repository getRepository(String uri)
	    throws RemoteException, MalformedURLException, NotBoundException;
}

/**
 * optional class for RMI, will only be used, if RMI server is present
 */
class RMIClientFactoryDelegater extends ClientFactoryDelegater {

    // only used to enforce linking upon Class.forName()
    static String FactoryClassName = ClientRepositoryFactory.class.getName();

    public Repository getRepository(String uri)
	    throws MalformedURLException, NotBoundException, RemoteException {
	System.setProperty("java.rmi.server.useCodebaseOnly", "true");
	return new ClientRepositoryFactory().getRepository(uri);
   }
}