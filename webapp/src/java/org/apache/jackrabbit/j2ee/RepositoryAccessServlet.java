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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.jackrabbit.util.Base64;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletException;
import javax.jcr.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import java.util.Properties;
import java.util.Enumeration;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;

/**
 * This Class implements a servlet that is used as unified mechanism to retrieve
 * a jcr repository either through JNID, RMI or JCRWebdavServer.
 */
public class RepositoryAccessServlet extends HttpServlet {

    /** default logger */
    private static Logger log;

    // todo: implement correctly
    public final static String INIT_PARAM_LOG4J_CONFIG = "log4j-config";

    /** the 'repository-name' init parameter */
    public final static String INIT_PARAM_REPOSITORY_NAME = "repository-name";

    /** the 'rmi-uri' init parameter */
    public final static String INIT_PARAM_RMI_URI = "rmi-uri";

    /** the 'missing-auth-mapping' init parameter */
    public final static String INIT_PARAM_MISSING_AUTH_MAPPING = "missing-auth-mapping";

    /** Authorization header name */
    private static final String HEADER_AUTHORIZATION = "Authorization";

    /** the configured repository name */
    private static String repositoryName;

    private static String rmiURI;

    private static InitialContext jndiContext;

    private static Repository repository;

    private static String missingAuthMapping;

    /**
     * Initializes this servlet
     *
     * @throws javax.servlet.ServletException
     */
    public void init() throws ServletException {
	initLog4J();
	log.info("RepositoryAccessServlet initializing...");
	initJNDI();
	initRMI();
	initRepository();
        missingAuthMapping = getServletConfig().getInitParameter(INIT_PARAM_MISSING_AUTH_MAPPING);
        log.info("  " + INIT_PARAM_MISSING_AUTH_MAPPING + " = " + missingAuthMapping);

	log.info("RepositoryAccessServlet initialized.");
    }

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
	log = Logger.getLogger(RepositoryAccessServlet.class);
    }

    private void initJNDI() throws ServletException {
	// setup repository name
	repositoryName = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_NAME);
	if (repositoryName==null) {
	    repositoryName="default";
	}
	log.info("  repository-name = " + repositoryName);

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
	    jndiContext = new InitialContext(env);
	} catch (NamingException e) {
	    log.error("Create initial context: " + e.toString());
	    throw new ServletException(e);
	}
    }

    private void initRMI() {
	// setup repository name
	rmiURI = getServletConfig().getInitParameter(INIT_PARAM_RMI_URI);
	if (rmiURI != null) {
	    log.info("  rmi-uri = " + rmiURI);
	}
    }

    /**
     * tries to retrieve the repository
     */
    private void initRepository() throws ServletException {
	getRepositoryByRMI();
	if (repository == null) {
	    getRepositoryByJNDI();
	}
	if (repository == null) {
	    log.error("Unable to retrieve repository");
	    throw new ServletException("Unable to retrieve repository");
	}
	log.info(repository.getDescriptor(Repository.REP_NAME_DESC) + " v" + repository.getDescriptor(Repository.REP_VERSION_DESC));
    }

    /**
     * tries to retrieve the repository using RMI
     */
    private void getRepositoryByJNDI() {
	if (jndiContext != null) {
	    // acquire via JNDI
	    try {
		repository = (Repository) jndiContext.lookup(repositoryName);
	    } catch (NamingException e) {
		log.error("Error while retrieving repository using JNDI: " + e);
		return;
	    }
	    log.info("Acquired repository via JNDI.");
	}
    }

    /**
     * tries to retrieve the repository using RMI
     */
    private void getRepositoryByRMI() {
	if (rmiURI != null) {
	    // acquire via RMI
	    ClientFactoryDelegater cfd = null;
	    try {
		Class clazz = Class.forName("org.apache.jackrabbit.j2ee.RMIClientFactoryDelegater");
		cfd = (ClientFactoryDelegater) clazz.newInstance();
	    } catch (NoClassDefFoundError e) {
		log.error("Unable to locate RMI ClientRepositoryFactory. jcr-rmi.jar missing? " + e.toString());
		return;
	    } catch (Exception e) {
		log.error("Unable to locate RMI ClientRepositoryFactory. jcr-rmi.jar missing?" + e.toString());
		return;
	    }

	    try {
		repository = cfd.getRepository(rmiURI);
	    } catch (Exception e) {
		log.error("Error while retrieving repository using RMI: " + e);
		return;
	    }
	    log.info("Acquired repository via RMI.");
	}
    }

    /**
     * Returns the JSR170 repository
     *
     * @return a jsr170 repository
     */
    public static Repository getRepository() {
	return repository;
    }

    /**
     * Build a {@link Credentials} object for the given authorization header.
     * The creds may be used to login to the repository. If the specified header
     * string is <code>null</code> or not of the required format the behaviour
     * depends on the {@link #INIT_PARAM_MISSING_AUTH_MAPPING} param:<br>
     * <ul>
     * <li> if this init-param is missing, a LoginException is thrown.
     *      This is suiteable for clients (eg. webdav clients) for with
     *      sending a proper authorization header is not possible, if the
     *      server never send a 401.
     * <li> if this init-param is present, but with an empty value,
     *      null-credentials are returned, thus forcing an null login
     *      on the repository
     * <li> if this init-param has a 'user:password' value, the respective
     *      simple credentials are generated.
     * </ul>
     *
     * @param authHeader Authorization header as present in the Http request
     * @return credentials or <code>null</code>.
     * @throws ServletException If an IOException occured while decoding the
     * Authorization header.
     * @throws LoginException if no suitable auth header and missing-auth-mapping
     * is not present
     * @see #getRepository()
     * @see #login(HttpServletRequest)
     */
    public static Credentials getCredentialsFromHeader(String authHeader)
	    throws ServletException, LoginException {
	try {
	    if (authHeader != null) {
		String[] authStr = authHeader.split(" ");
		if (authStr.length >= 2 && authStr[0].equalsIgnoreCase(HttpServletRequest.BASIC_AUTH)) {
		    ByteArrayOutputStream out = new ByteArrayOutputStream();
		    Base64.decode(authStr[1].toCharArray(), out);
		    String decAuthStr = out.toString("ISO-8859-1");
		    int pos = decAuthStr.indexOf(':');
		    String userid = decAuthStr.substring(0, pos);
		    String passwd = decAuthStr.substring(pos + 1);
		    return new SimpleCredentials(userid, passwd.toCharArray());
		}
	    }
            // check special handling
            if (missingAuthMapping == null) {
                throw new LoginException();
            } else if (missingAuthMapping.equals("")) {
                return null;
            } else {
                int pos = missingAuthMapping.indexOf(':');
                if (pos<0) {
                    return new SimpleCredentials(missingAuthMapping, null);
                } else {
                    return new SimpleCredentials(
                            missingAuthMapping.substring(0, pos),
                            missingAuthMapping.substring(pos+1).toCharArray()
                    );
                }
            }
	} catch (IOException e) {
	    throw new ServletException("Unable to decode authorization: " + e.toString());
	}
    }

    /**
     * Simple login to the {@link Repository} accessed by this servlet using the
     * Authorization header present in the given request.<p/>
     * Please note, that no workspace information is provided to the repository
     * login ({@link Repository#login(javax.jcr.Credentials)}), thus the default
     * workspace will be selected. In order to provide a specific workspace name,
     * manual {@link Repository#login(Credentials, String) login} is required (see
     * also {@link #getRepository()}).
     *
     * @param request
     * @return  Session object obtained upon {@link Repository#login(javax.jcr.Credentials)}.
     * @throws ServletException
     * @throws LoginException if credentials are invalid
     * @see #getRepository() in order to be able to login to a specific workspace.
     * @see #getCredentialsFromHeader(String) for a utility method to retrieve
     * credentials from the Authorization header string.
     */
    public static Session login(HttpServletRequest request)
            throws LoginException, ServletException {
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
	try {
	    return repository.login(getCredentialsFromHeader(authHeader));
        } catch (LoginException e) {
            throw e;
	} catch (RepositoryException e) {
	    throw new ServletException("Failed to login to the repository: " + e.toString());
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