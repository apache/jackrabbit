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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.util.Enumeration;
import java.util.Properties;

/**
 * The RepositoryStartupServlet starts a jackrabbit repository and registers it
 * to the JNDI environment and optional to the RMI registry.
 * <p id="registerAlgo">
 * <b>Registration with RMI</b>
 * <p>
 * Upon successfull creation of the repository in the {@link #init()} method,
 * the repository is registered with an RMI registry if the web application is
 * so configured. To register with RMI, the following web application
 * <code>init-params</code> are considered: <code>rmi-port</code> designating
 * the port on which the RMI registry is listening, <code>rmi-host</code>
 * designating the interface on the local host on which the RMI registry is
 * active, <code>repository-name</code> designating the name to which the
 * repository is to be bound in the registry, and <code>rmi-uri</code>
 * designating an RMI URI complete with host, optional port and name to which
 * the object is bound.
 * <p>
 * If the <code>rmi-uri</code> parameter is configured with a non-empty value,
 * the <code>rmi-port</code> and <code>rmi-host</code> parameters are ignored.
 * The <code>repository-name</code> parameter is only considered if a non-empty
 * <code>rmi-uri</code> parameter is configured if the latter does not contain
 * a name to which to bind the repository.
 * <p>
 * This is the algorithm used to find out the host, port and name for RMI
 * registration:
 * <ol>
 * <li>If neither a <code>rmi-uri</code> nor a <code>rmi-host</code> nor a
 *      <code>rmi-port</code> parameter is configured, the repository is not
 *      registered with any RMI registry.
 * <li>If a non-empty <code>rmi-uri</code> parameter is configured extract the
 *      host name (or IP address), port number and name to bind to from the
 *      URI. If the URI is not valid, host defaults to <code>0.0.0.0</code>
 *      meaning all interfaces on the local host, port defaults to the RMI
 *      default port (<code>1099</code>) and the name defaults to the value
 *      of the <code>repository-name</code> parameter.
 * <li>If a non-empty <code>rmi-uri</code> is not configured, the host is taken
 *      from the <code>rmi-host</code> parameter, the port from the
 *      <code>rmi-port</code> parameter and the name to bind the repository to
 *      from the <code>repository-name</code> parameter. If the
 *      <code>rmi-host</code> parameter is empty or not configured, the host
 *      defaults to <code>0.0.0.0</code> meaning all interfaces on the local
 *      host. If the <code>rmi-port</code> parameter is empty, not configured,
 *      zero or a negative value, the default port for the RMI registry
 *      (<code>1099</code>) is used.
 * </ol>
 * <p>
 * After finding the host and port of the registry, the RMI registry itself
 * is acquired. It is assumed, that host and port primarily designate an RMI
 * registry, which should be active on the local host but has not been started
 * yet. In this case, the <code>LocateRegistry.createRegistry</code> method is
 * called to create a registry on the local host listening on the host and port
 * configured. If creation fails, the <code>LocateRegistry.getRegistry</code>
 * method is called to get a remote instance of the registry. Note, that
 * <code>getRegistry</code> does not create an actual registry on the given
 * host/port nor does it check, whether an RMI registry is active.
 * <p>
 * When the registry has been retrieved, either by creation or by just creating
 * a remote instance, the repository is bound to the configured name in the
 * registry.
 * <p>
 * Possible causes for registration failures include:
 * <ul>
 * <li>The web application is not configured to register with an RMI registry at
 *      all.
 * <li>The registry is expected to be running on a remote host but does not.
 * <li>The registry is expected to be running on the local host but cannot be
 *      accessed. Reasons include another application which does not act as an
 *      RMI registry is running on the configured port and thus blocks creation
 *      of a new RMI registry.
 * <li>An object may already be bound to the same name as is configured to be
 *      used for the repository.
 * </ul>
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

    /**
     * The rmi uri, in the form of  '//${rmi-host}:${rmi-port}/${repository-name}'
     * This field is only set to a non-<code>null</code> value, if registration
     * of the repository to an RMI registry succeeded in the
     * {@link #registerRMI()} method.
     *
     * @see #registerRMI()
     * @see #unregisterRMI()
     */
    private String rmiURI;

    /**
     * Initializes the servlet
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();
        log.info("RepositoryStartupServlet initializing...");
        initRepository();
        try {
            registerRMI();
            registerJNDI();
        } catch (ServletException e) {
            // shutdown repository
            shutdownRepository();
            log.error("RepositoryStartupServlet initializing failed: "+ e, e);
            throw e;
        }
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

    /**
     * Shuts down the repository
     */
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
     */
    private void registerJNDI() throws ServletException {
        // registering via jndi
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
        try {
            jndiContext = new InitialContext(env);
            jndiContext.bind(repositoryName, repository);
            log.info("Repository bound to JNDI with name: " + repositoryName);
        } catch (NamingException e) {
            throw new ServletException("Unable to bind repository using JNDI.", e);
        }
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
     * Registers the repository to an RMI registry configured in the web
     * application. See <a href="#registerAlgo">Registration with RMI</a> in the
     * class documentation for a description of the algorithms used to register
     * the repository with an RMI registry.
     */
    private void registerRMI() throws ServletException {
        // check registering via RMI
        String rmiPortStr = getServletConfig().getInitParameter(INIT_PARAM_RMI_PORT);
        String rmiHost = getServletConfig().getInitParameter(INIT_PARAM_RMI_HOST);
        String rmiURI = getServletConfig().getInitParameter(INIT_PARAM_RMI_URI);

        // no registration if neither port nor host nor URI is configured
        if (rmiPortStr == null && rmiHost == null && rmiURI == null) {
            return;
        }

        // URI takes precedences, so check whether the configuration has to
        // be set from the URI
        int rmiPort = -1;
        String rmiName = null;
        if (rmiURI != null && rmiURI.length() > 0) {
            URI uri = null;
            try {
                uri = new URI(rmiURI);

                // extract values from the URI, check later
                rmiHost = uri.getHost();
                rmiPort = uri.getPort();
                rmiName = uri.getPath();

            } catch (URISyntaxException use) {
                log.warn("Cannot parse RMI URI '" + rmiURI + "'.", use);
                rmiURI = null; // clear RMI URI use another one
                rmiHost = null; // use default host, ignore rmi-host param
            }

            // cut of leading slash from name if defined at all
            if (rmiName != null && rmiName.startsWith("/")) {
                rmiName = rmiName.substring(1);
            }
        } else {
            // convert RMI port configuration
            if (rmiPortStr != null) {
                try {
                    rmiPort = Integer.parseInt(rmiPortStr);
                } catch (NumberFormatException e) {
                    log.warn("Invalid port in rmi-port param: " + e + ". using default port.");
                    rmiPort = Registry.REGISTRY_PORT;
                }
            }
        }

        // check RMI port
        if (rmiPort == -1 || rmiPort == 0) {
            // accept -1 or 0 as a hint to use the default
            rmiPort = Registry.REGISTRY_PORT;
        } else if (rmiPort < -1 || rmiPort > 0xFFFF) {
            // emit a warning if out of range, use defualt in this case
            log.warn("Invalid port in rmi-port param " + rmiPort + ". using default port.");
            rmiPort = Registry.REGISTRY_PORT;
        }

        // check host - use an empty name if null (i.e. not configured)
        if (rmiHost == null) {
            rmiHost = "";
        }

        // check name - use repositoryName if empty or null
        if (rmiName == null || rmiName.length() ==0) {
            rmiName = repositoryName;
        }

        // reconstruct the rmiURI now because values might have been changed
        rmiURI = "//" + rmiHost + ":" + rmiPort + "/" + rmiName;

        // try to create remote repository
        Remote remote;
        try {
            Class clazz = Class.forName(getRemoteFactoryDelegaterClass());
            RemoteFactoryDelegater rmf = (RemoteFactoryDelegater) clazz.newInstance();
            remote = rmf.createRemoteRepository(repository);
        } catch (RemoteException e) {
            throw new ServletException("Unable to create remote repository.", e);
        } catch (NoClassDefFoundError e) {
            throw new ServletException("Unable to create RMI repository. jcr-rmi.jar might be missing.", e);
        } catch (Exception e) {
            throw new ServletException("Unable to create RMI repository. jcr-rmi.jar might be missing.", e);
        }

        try {
            System.setProperty("java.rmi.server.useCodebaseOnly", "true");
            Registry reg = null;

            // first try to create the registry, which will fail if another
            // application is already running on the configured host/port
            // or if the rmiHost is not local
            try {
                // find the server socket factory: use the default if the
                // rmiHost is not configured
                RMIServerSocketFactory sf;
                if (rmiHost.length() > 0) {
                    log.debug("Creating RMIServerSocketFactory for host " + rmiHost);
                    InetAddress hostAddress = InetAddress.getByName(rmiHost);
                    sf = getRMIServerSocketFactory(hostAddress);
                } else {
                    // have the RMI implementation decide which factory is the
                    // default actually
                    log.debug("Using default RMIServerSocketFactory");
                    sf = null;
                }

                // create a registry using the default client socket factory
                // and the server socket factory retrieved above. This also
                // binds to the server socket to the rmiHost:rmiPort.
                reg = LocateRegistry.createRegistry(rmiPort, null, sf);

            } catch (UnknownHostException uhe) {
                // thrown if the rmiHost cannot be resolved into an IP-Address
                // by getRMIServerSocketFactory
                log.info("Cannot create Registry", uhe);
            } catch (RemoteException e) {
                // thrown by createRegistry if binding to the rmiHost:rmiPort
                // fails, for example due to rmiHost being remote or another
                // application already being bound to the port
                log.info("Cannot create Registry", e);
            }

            // if creation of the registry failed, we try to access an
            // potentially active registry. We do not check yet, whether the
            // registry is actually accessible.
            if (reg == null) {
                log.debug("Trying to access existing registry at " + rmiHost
                    + ":"+ rmiPort);
                try {
                    reg = LocateRegistry.getRegistry(rmiHost, rmiPort);
                } catch (RemoteException re) {
                    log.error("Cannot create the reference to the registry at "
                        + rmiHost + ":" + rmiPort, re);
                }
            }

            // if we finally have a registry, register the repository with the
            // rmiName
            if (reg != null) {
                log.debug("Registering repository as " + rmiName
                    + " to registry " + reg);
                reg.bind(rmiName, remote);
                this.rmiURI = rmiURI;
                log.info("Repository bound via RMI with name: " + rmiURI);
            } else {
                log.info("RMI registry missing, cannot bind repository via RMI");
            }

        } catch (RemoteException e) {
            throw new ServletException("Unable to bind repository via RMI.", e);
        } catch (AlreadyBoundException e) {
            throw new ServletException("Unable to bind repository via RMI.", e);
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
     * Returns an <code>RMIServerSocketFactory</code> used to create the server
     * socket for a locally created RMI registry.
     * <p>
     * This implementation returns a new instance of a simple
     * <code>RMIServerSocketFactory</code> which just creates instances of
     * the <code>java.net.ServerSocket</code> class bound to the given
     * <code>hostAddress</code>. Implementations may overwrite this method to
     * provide factory instances, which provide more elaborate server socket
     * creation, such as SSL server sockets.
     *
     * @param hostAddress The <code>InetAddress</code> instance representing the
     *      the interface on the local host to which the server sockets are
     *      bound.
     *
     * @return A new instance of a simple <code>RMIServerSocketFactory</code>
     *      creating <code>java.net.ServerSocket</code> instances bound to
     *      the <code>rmiHost</code>.
     */
    protected RMIServerSocketFactory getRMIServerSocketFactory(
            final InetAddress hostAddress) {
        return new RMIServerSocketFactory() {
            public ServerSocket createServerSocket(int port) throws IOException {
                return new ServerSocket(port, -1, hostAddress);
            }
        };
    }

    /**
     * Unregisters the repository from the RMI registry, if it has previously
     * been registered.
     */
    private void unregisterRMI() {
        if (rmiURI != null) {
            try {
                Naming.unbind(rmiURI);
            } catch (Exception e) {
                log("Error while unbinding repository from JNDI: " + e);
            } finally {
                // do not try again to unregister
                rmiURI = null;
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