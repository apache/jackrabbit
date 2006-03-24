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
package org.apache.jackrabbit.server;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import java.util.Properties;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

/**
 * The RepositoryStartupServlet starts a jackrabbit repository and registers it
 * to the JNDI environment.
 */
public class RepositoryStartupServlet extends HttpServlet {

    /**
     * the default logger
     */
    private static Logger log;

    /**
     * initial param name for the repository config location
     */
    public final static String INIT_PARAM_REPOSITORY_CONFIG = "repository-config";

    /**
     * initial param name for the repository home directory
     */
    public final static String INIT_PARAM_REPOSITORY_HOME = "repository-home";

    /**
     * initial param name for the repository name
     */
    public final static String INIT_PARAM_REPOSITORY_NAME = "repository-name";

    /**
     * initial param name for the log4j config properties
     */
    public final static String INIT_PARAM_LOG4J_CONFIG = "log4j-config";

    /**
     * the registered repository
     */
    private RepositoryImpl repository;

    /**
     * the name of the repository as configured
     */
    private String repositoryName;

    /**
     * the jndi context, created base on configuration
     */
    private InitialContext jndiContext;

    /**
     * Initializes the servlet
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();
        initLog4J();
        log.info("RepositoryStartupServlet initializing...");
        initRepository();
        registerJNDI();
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
        unregisterJNDI();
        repository.shutdown();
        log("RepositoryStartupServlet shut down.");
    }

    /**
     * Initializes Log4J
     *
     * @throws ServletException
     */
    private void initLog4J() throws ServletException {
        // setup log4j
        String log4jConfig = getServletConfig().getInitParameter(INIT_PARAM_LOG4J_CONFIG);
        InputStream in = getServletContext().getResourceAsStream(log4jConfig);
        if (in == null) {
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
     *
     * @throws ServletException
     */
    private void initRepository() throws ServletException {
        // setup home directory
        String repHome = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_HOME);
        if (repHome == null) {
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
        if (repConfig == null) {
            log.error(INIT_PARAM_REPOSITORY_CONFIG + " missing.");
            throw new ServletException(INIT_PARAM_REPOSITORY_CONFIG + " missing.");
        }
        log.info("  repository-config = " + repConfig);

        InputStream in = getServletContext().getResourceAsStream(repConfig);
        if (in == null) {
            try {
                in = new FileInputStream(new File(repositoryHome, repConfig));
            } catch (FileNotFoundException e) {
                log.error(INIT_PARAM_REPOSITORY_CONFIG + " invalid." + e.toString());
                throw new ServletException(INIT_PARAM_REPOSITORY_CONFIG + " invalid." + e.toString());
            }
        }

        // get repository name
        repositoryName = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_NAME);
        if (repositoryName == null) {
            repositoryName = "default";
        }
        log.info("  repository-name = " + repositoryName);

        try {
            InputSource is = new InputSource(in);
            RepositoryConfig config = RepositoryConfig.create(is, repositoryHome.getAbsolutePath());
            repository = RepositoryImpl.create(config);
        } catch (RepositoryException e) {
            throw new ServletException("Error while creating repository", e);
        }

        // setup repository
        SimpleCredentials cred = new SimpleCredentials("user", "".toCharArray());
        try {
            RepositorySetup.run(repository.login(cred));
        } catch (RepositoryException e) {
            log("Error while setting up repository", e);
        }
    }

    /**
     * Registers the repository in the JNDI context
     *
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

}
