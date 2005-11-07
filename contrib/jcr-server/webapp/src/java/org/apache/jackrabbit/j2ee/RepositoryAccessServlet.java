/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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

    /** default logger */
    private static final Logger log = Logger.getLogger(RepositoryAccessServlet.class);

    // todo: implement correctly
    public final static String INIT_PARAM_LOG4J_CONFIG = "log4j-config";

    /** the 'repository-name' init parameter */
    public final static String INIT_PARAM_REPOSITORY_NAME = "repository-name";

    /** the 'rmi-uri' init parameter */
    public final static String INIT_PARAM_RMI_URI = "rmi-uri";

    /** the 'missing-auth-mapping' init parameter */
    public final static String INIT_PARAM_MISSING_AUTH_MAPPING = "missing-auth-mapping";

    private static final String CTX_ATTR_REPOSITORY = "jcr.repository";

    private String repositoryName;

    /**
     * Initializes this servlet
     *
     * @throws javax.servlet.ServletException
     */
    public void init() throws ServletException {
	log.info("RepositoryAccessServlet initializing...");
        repositoryName = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_NAME);
        if (repositoryName==null) {
            repositoryName="default";
        }
        Repository repository = null;

        // try to retrieve via rmi
        if (repository == null) {
            String rmiURI = getRMIUri();
            if (rmiURI != null) {
                repository = getRepositoryByRMI(rmiURI);
            }
        }
        // try to retrieve via jndi
        if (repository == null) {
            InitialContext context = getInitialContext();
            if (context != null) {
                repository = getRepositoryByJNDI(context);
            }
        }
        // error
        if (repository == null) {
            log.error("Unable to retrieve repository");
            throw new ServletException("Unable to retrieve repository");
        }
        getServletContext().setAttribute(CTX_ATTR_REPOSITORY, repository);
        log.info(repository.getDescriptor(Repository.REP_NAME_DESC) + " v" + repository.getDescriptor(Repository.REP_VERSION_DESC));

	log.info("RepositoryAccessServlet initialized.");
    }

    private InitialContext getInitialContext() throws ServletException {
	// retrieve JNDI Context environment
	try {
	    Properties env = new Properties();
	    Enumeration names = getServletConfig().getInitParameterNames();
	    while (names.hasMoreElements()) {
		String name = (String) names.nextElement();
		if (name.startsWith("java.naming.")) {
		    env.put(name, getServletConfig().getInitParameter(name));
		    log.info("  adding property to JNDI environment: " + name + "=" + env.getProperty(name));
		}
	    }
	    return new InitialContext(env);
	} catch (NamingException e) {
	    log.error("Create initial context: " + e.toString());
	    throw new ServletException(e);
	}
    }

    private String getRMIUri() {
	// setup repository name
	return getServletConfig().getInitParameter(INIT_PARAM_RMI_URI);
    }

    /**
     * tries to retrieve the repository using RMI
     */
    private Repository getRepositoryByJNDI(InitialContext jndiContext) {
        // acquire via JNDI
        try {
            Repository r = (Repository) jndiContext.lookup(repositoryName);
            log.info("Acquired repository via JNDI.");
            return r;
        } catch (NamingException e) {
            log.error("Error while retrieving repository using JNDI (name=" + repositoryName +"): " + e);
            return null;
        }
    }

    /**
     * tries to retrieve the repository using RMI
     */
    private Repository getRepositoryByRMI(String rmiURI) {
        // acquire via RMI
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
     */
    public static Repository getRepository(ServletContext ctx) {
	return (Repository) ctx.getAttribute(CTX_ATTR_REPOSITORY);
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