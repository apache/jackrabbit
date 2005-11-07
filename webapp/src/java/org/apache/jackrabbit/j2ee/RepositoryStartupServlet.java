/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;
import java.util.Properties;

/**
 * The RepositoryStartupServlet starts a jackrabbit repository and registers it
 * to the JNDI environment and optional to the RMI registry.
 */
public class RepositoryStartupServlet extends HttpServlet {

    /** the default logger */
    private static final Logger log = Logger.getLogger(RepositoryStartupServlet.class);

    /** initial param name for the repository config location */
    public final static String INIT_PARAM_REPOSITORY_CONFIG = "repository-config";

    /** initial param name for the repository home directory */
    public final static String INIT_PARAM_REPOSITORY_HOME = "repository-home";

    /** initial param name for the repository name */
    public final static String INIT_PARAM_REPOSITORY_NAME = "repository-name";

    /** initial param name for the rmi port */
    public final static String INIT_PARAM_RMI_PORT = "rmi-port";

    /** initial param name for the rmi host */
    public final static String INIT_PARAM_RMI_HOST = "rmi-host";

    /** initial param name for the rmi uri */
    public final static String INIT_PARAM_RMI_URI = "rmi-uri";

    /** initial param name for the log4j config properties */
    public final static String INIT_PARAM_LOG4J_CONFIG = "log4j-config";

    /** the registered repository */
    private Repository repository;

    /** the name of the repository as configured */
    private String repositoryName;

    /** the jndi context, created base on configuration */
    private InitialContext jndiContext;

    /** the rmi uri, in the form of  '//${rmi-host}:${rmi-port}/${repository-name}' */
    private String rmiURI;

    /**
     * Initializes the servlet
     * @throws ServletException
     */
    public void init() throws ServletException {
	super.init();
	log.info("RepositoryStartupServlet initializing...");
	initRepository();
	registerJNDI();
	registerRMI();
	log.info("RepositoryStartupServlet initialized.");
    }

    /**
     * destroy the servlet
     */
    public void destroy() {
	super.destroy();
	if (log == null) {
	    log("RepositoryStartupServlet shutting down...");
	} else {
	    log.info("RepositoryStartupServlet shutting down...");
	}
        shutdownRepository();
	unregisterRMI();
	unregisterJNDI();
        if (log == null) {
            log("RepositoryStartupServlet shut down.");
        } else {
            log.info("RepositoryStartupServlet shut down.");
        }
    }

    /**
     * Creates a new Repository based on configuration
     * @throws ServletException
     */
    private void initRepository() throws ServletException {
	// setup home directory
	String repHome = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_HOME);
	if (repHome==null) {
	    log.error(INIT_PARAM_REPOSITORY_HOME + " missing.");
	    throw new ServletException(INIT_PARAM_REPOSITORY_HOME + " missing.");
	}
	File repositoryHome;
	try {
	    repositoryHome = new File(repHome).getCanonicalFile();
	} catch (IOException e) {
	    log.error(INIT_PARAM_REPOSITORY_HOME + " invalid." + e.toString());
	    throw new ServletException(INIT_PARAM_REPOSITORY_HOME + " invalid." + e.toString());
	}
	log.info("  repository-home = " + repositoryHome.getPath());

	// get repository config
	String repConfig = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_CONFIG);
	if (repConfig==null) {
	    log.error(INIT_PARAM_REPOSITORY_CONFIG + " missing.");
	    throw new ServletException(INIT_PARAM_REPOSITORY_CONFIG + " missing.");
	}
	log.info("  repository-config = " + repConfig);

	InputStream in = getServletContext().getResourceAsStream(repConfig);
	if (in==null) {
	    try {
		in = new FileInputStream(new File(repositoryHome, repConfig));
	    } catch (FileNotFoundException e) {
		log.error(INIT_PARAM_REPOSITORY_CONFIG + " invalid." + e.toString());
		throw new ServletException(INIT_PARAM_REPOSITORY_CONFIG + " invalid." + e.toString());
	    }
	}

	// get repository name
	repositoryName = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_NAME);
	if (repositoryName==null) {
	    repositoryName="default";
	}
	log.info("  repository-name = " + repositoryName);

	try {
	    repository = createRepository(new InputSource(in), repositoryHome);
	} catch (RepositoryException e) {
	    throw new ServletException("Error while creating repository", e);
	}
    }

    private void shutdownRepository() {
        if (repository instanceof RepositoryImpl) {
            ((RepositoryImpl) repository).shutdown();
            repository = null;
        }
    }
    /**
     * Creates the repository for the given config and homedir.
     *
     * @param is
     * @param homedir
     * @return
     * @throws RepositoryException
     */
    protected Repository createRepository(InputSource is, File homedir)
            throws RepositoryException {
        RepositoryConfig config = RepositoryConfig.create(is, homedir.getAbsolutePath());
        return RepositoryImpl.create(config);
    }

    /**
     * Registers the repository in the JNDI context
     * @throws ServletException
     */
    private void registerJNDI() throws ServletException {
	// registering via jndi
	Properties env = new Properties();
	Enumeration names = getServletConfig().getInitParameterNames();
	while (names.hasMoreElements()) {
	    String name = (String) names.nextElement();
	    if (name.startsWith("java.naming.")) {
		env.put(name, getServletConfig().getInitParameter(name));
		log.info("  adding property to JNDI environment: " + name + "=" + env.getProperty(name));
	    }
	}
	try {
	    jndiContext = new InitialContext(env);
	    jndiContext.bind(repositoryName, repository);
	} catch (NamingException e) {
	    throw new ServletException(e);
	}
	log.info("Repository bound to JNDI with name: " + repositoryName);
    }

    /**
     * Unregisters the repository from the JNDI context
     */
    private void unregisterJNDI() {
	if (jndiContext != null) {
	    try {
		jndiContext.unbind(repositoryName);
	    } catch (NamingException e) {
		log("Error while unbinding repository from JNDI: " + e);
	    }
	}
    }

    /**
     * Registers the repositroy to the RMI registry
     * @throws ServletException
     */
    private void registerRMI() throws ServletException {
	// check registering via RMI
	String rmiPortStr = getServletConfig().getInitParameter(INIT_PARAM_RMI_PORT);
        String rmiHost = getServletConfig().getInitParameter(INIT_PARAM_RMI_HOST);
        rmiURI = getServletConfig().getInitParameter(INIT_PARAM_RMI_URI);
        if (rmiPortStr == null && rmiHost == null && rmiURI == null) {
            return;
        }
        int rmiPort = Registry.REGISTRY_PORT;
	if (rmiPortStr != null) {
	    try {
		rmiPort = Integer.parseInt(rmiPortStr);
	    } catch (NumberFormatException e) {
                log.warn("Invalid port in rmi-port param: " + e + ". using default port.");
            }
	    }
        if (rmiHost == null) {
            rmiHost = "";
	    }

	    // try to create remote repository
	    Remote remote;
	    try {
            Class clazz = Class.forName(getRemoteFactoryDelegaterClass());
		RemoteFactoryDelegater rmf = (RemoteFactoryDelegater) clazz.newInstance();
		remote = rmf.createRemoteRepository(repository);
	    } catch (RemoteException e) {
		throw new ServletException("Unable to create remote repository: " + e.toString(), e);
	    } catch (NoClassDefFoundError e) {
		log.warn("Unable to create RMI repository. jcr-rmi.jar might be missing.: " + e.toString());
		return;
	    } catch (Exception e) {
		log.warn("Unable to create RMI repository. jcr-rmi.jar might be missing.: " + e.toString());
		return;
	    }

	    try {
		System.setProperty("java.rmi.server.useCodebaseOnly", "true");
		try {
		    // start registry
		    LocateRegistry.createRegistry(rmiPort);
		} catch (RemoteException e) {
		    // ignore
		}
            if (rmiURI == null) {
                rmiURI = "//" + rmiHost + ":" + rmiPort + "/" + repositoryName;
            }
		Naming.bind(rmiURI, remote);

		log.info("Repository bound via RMI with name: " + rmiURI);
	    } catch (MalformedURLException e) {
		throw new ServletException("Unable to bind repository via RMI: " + e.toString(), e);
	    } catch (RemoteException e) {
		throw new ServletException("Unable to bind repository via RMI: " + e.toString(), e);
	    } catch (AlreadyBoundException e) {
		throw new ServletException("Unable to bind repository via RMI: " + e.toString(), e);
	    }
	}

    /**
     * Return the fully qualified name of the class providing the remote
     * repository. The class whose name is returned must implement the
     * {@link RemoteFactoryDelegater} interface.
     */
    protected String getRemoteFactoryDelegaterClass() {
        return "org.apache.jackrabbit.j2ee.RMIRemoteFactoryDelegater";
    }

    /**
     * Unregisters the repository from the RMI registry
     */
    private void unregisterRMI() {
	if (rmiURI != null) {
	    try {
		Naming.unbind(rmiURI);
	    } catch (Exception e) {
		log("Error while unbinding repository from JNDI: " + e);
	    }
	}
    }

}

/**
 * optional class for RMI, will only be used, if RMI server is present
 */
abstract class RemoteFactoryDelegater {

    public abstract Remote createRemoteRepository(Repository repository)
	    throws RemoteException;
}
/**
 * optional class for RMI, will only be used, if RMI server is present
 */
class RMIRemoteFactoryDelegater extends RemoteFactoryDelegater {

    // only used to enforce linking upon Class.forName()
    static String FactoryClassName = ServerAdapterFactory.class.getName();

    public Remote createRemoteRepository(Repository repository)
	    throws RemoteException {
	return new ServerAdapterFactory().getRemoteRepository(repository);
    }
}