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
import org.apache.jackrabbit.util.Base64;
import org.apache.log4j.Logger;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    /** Authorization header name */
    private static final String HEADER_AUTHORIZATION = "Authorization";

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
     * @see #getRepository(ServletContext)
     */
    public static Credentials getCredentialsFromHeader(ServletContext ctx,
                                                       String authHeader)
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
            String missingAuthMapping = ctx.getInitParameter(INIT_PARAM_MISSING_AUTH_MAPPING);
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
     * also {@link #getRepository(ServletContext)}).
     *
     * @param request
     * @return  Session object obtained upon {@link Repository#login(javax.jcr.Credentials)}.
     * @throws ServletException
     * @throws LoginException if credentials are invalid
     * @see #getRepository(ServletContext) in order to be able to login to a specific workspace.
     * @see #getCredentialsFromHeader(ServletContext, String) for a utility method to retrieve
     * credentials from the Authorization header string.
     */
    public static Session login(ServletContext ctx, HttpServletRequest request)
            throws LoginException, ServletException {
        String authHeader = request.getHeader(HEADER_AUTHORIZATION);
	try {
            Repository rep = getRepository(ctx);
	    return rep.login(getCredentialsFromHeader(ctx, authHeader));
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