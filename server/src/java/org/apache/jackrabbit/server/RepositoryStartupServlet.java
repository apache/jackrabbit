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
package org.apache.jackrabbit.server;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.jcr.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.*;
import java.util.Properties;
import java.util.Enumeration;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.net.MalformedURLException;

/**
 * The RepositoryStartupServlet starts a jackrabbit repository and registers it
 * to the JNDI environment and optional to the RMI registry.
 */
public class RepositoryStartupServlet extends HttpServlet {

    /** the default logger */
    private static Logger log;

    /** initial param name for the repository config location */
    public final static String INIT_PARAM_REPOSITORY_CONFIG = "repository-config";

    /** initial param name for the repository home directory */
    public final static String INIT_PARAM_REPOSITORY_HOME = "repository-home";

    /** initial param name for the repository name */
    public final static String INIT_PARAM_REPOSITORY_NAME = "repository-name";

    /** initial param name for the rmi port */
    public final static String INIT_PARAM_RMI_PORT = "rmi-port";

    /** initial param name for the log4j config properties */
    public final static String INIT_PARAM_LOG4J_CONFIG = "log4j-config";

    /** the registered repository */
    private static RepositoryImpl repository;

    /** the name of the repository as configured */
    private static String repositoryName;

    /** the jndi context, created base on configuration */
    private static InitialContext jndiContext;

    /** the rmi uri, in the form of  '//:${rmi-port}/${repository-name}' */
    private static String rmiURI;

    /**
     * Initializes the servlet
     * @throws ServletException
     */
    public void init() throws ServletException {
	super.init();
	initLog4J();
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
	unregisterRMI();
	unregisterJNDI();
	log("RepositoryStartupServlet shut down.");
    }

    /**
     * Initializes Log4J
     * @throws ServletException
     */
    private void initLog4J() throws ServletException {
	// setup log4j
	String log4jConfig = getServletConfig().getInitParameter(INIT_PARAM_LOG4J_CONFIG);
	InputStream in =getServletContext().getResourceAsStream(log4jConfig);
	if (in==null) {
	    // try normal init
	    PropertyConfigurator.configure(log4jConfig);
	} else {
	    try {
		Properties log4jProperties = new Properties();
		log4jProperties.load(in);
		in.close();
		PropertyConfigurator.configure(log4jProperties);
	    } catch (IOException e) {
		throw new ServletException("Unable to load log4jProperties: " + e.toString());
	    }
	}
	log = Logger.getLogger(RepositoryStartupServlet.class);
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
	    InputSource is = new InputSource(in);
	    RepositoryConfig config = RepositoryConfig.create(is, repositoryHome.getAbsolutePath());
	    repository = RepositoryImpl.create(config);
	} catch (RepositoryException e) {
	    throw new ServletException("Error while creating repository", e);
	}
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
	if (rmiPortStr != null) {
	    int rmiPort = 0;
	    try {
		rmiPort = Integer.parseInt(rmiPortStr);
	    } catch (NumberFormatException e) {
		log.warn("Invalid port in rmi-port param: " + e);
	    }
	    if (rmiPort == 0) {
		rmiPort = Registry.REGISTRY_PORT;
	    }

	    // try to create remote repository
	    Remote remote = null;
	    try {
		Class clazz = Class.forName("org.apache.jackrabbit.server.RMIRemoteFactoryDelegater");
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
		rmiURI = "//:" + rmiPort + "/" + repositoryName;
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