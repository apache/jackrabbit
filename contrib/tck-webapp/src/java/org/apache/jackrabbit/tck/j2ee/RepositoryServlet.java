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
package org.apache.jackrabbit.tck.j2ee;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.jcr.*;
import java.io.*;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;

/**
 * The RepositoryServlet connects (starts) to a jsr170 repository and
 * puts the reference into the application context
 */
public class RepositoryServlet extends HttpServlet {

    /** the logger */
    private static Logger log;// = Logger.getLogger(RepositoryServlet.class);

    /** repository configuration path */
    public final static String INIT_PARAM_REPOSITORY_CONFIG = "repository-config";

    /** repository home */
    public final static String INIT_PARAM_REPOSITORY_HOME = "repository-home";

    /** repository name */
    public final static String INIT_PARAM_REPOSITORY_NAME = "repository-name";

    /** jaas configuration file path */
    public static final String JAAS_CONFIG_FILE = "jaas-config-file";

    /** user id name */
    public static final String USER_ID = "jcr-userid";

    /** user password name */
    public static final String  USER_PASSWORD = "jcr-password";

    /** log4j config */
    public final static String PARAM_LOG4J_CONFIG = "log4j-config";

    /** the repository to read/write test results and config */
    private static RepositoryImpl repository;

    /** the user id */
    private static String uid;

    /** the password */
    private static String pw;

    /**
     * The init method starts the repository to read/write test results and configuration,
     * sets the jaas config and the user id and the user password
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();

        try {
            // setup log4j
            // todo: do correctly
            String log4jConfig = getServletConfig().getInitParameter(PARAM_LOG4J_CONFIG);
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
            log_info("RepositoryServlet initializing..");

            // setup home directory
            String repHome = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_HOME);
            if (repHome == null) {
                log_info(INIT_PARAM_REPOSITORY_HOME + " missing.");
                throw new ServletException(INIT_PARAM_REPOSITORY_HOME + " missing.");
            }
                File repositoryHome;
            try {
                repositoryHome = new File(repHome).getCanonicalFile();
            } catch (IOException e) {
                log_info(INIT_PARAM_REPOSITORY_HOME + " invalid." + e.toString());
                throw new ServletException(INIT_PARAM_REPOSITORY_HOME + " invalid." + e.toString());
            }
            log_info("  repository-home = " + repositoryHome.getPath());

            // setup repository
            String repConfig = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_CONFIG);
            if (repConfig == null) {
                log_info(INIT_PARAM_REPOSITORY_CONFIG + " missing.");
                throw new ServletException(INIT_PARAM_REPOSITORY_CONFIG + " missing.");
            }
            log_info("  repository-config = " + repConfig);

            in = getServletContext().getResourceAsStream(repConfig);
            if (in == null) {
                try {
                    in = new FileInputStream(new File(repositoryHome, repConfig));
                } catch (FileNotFoundException e) {
                    log_info(INIT_PARAM_REPOSITORY_CONFIG + " invalid." + e.toString());
                    throw new ServletException(INIT_PARAM_REPOSITORY_CONFIG + " invalid." + e.toString());
                }
            }

            // get repository name
            String repositoryName = getServletConfig().getInitParameter(INIT_PARAM_REPOSITORY_NAME);
            if (repositoryName == null) {
                repositoryName = "default";
            }
            log_info("  repository-name = " + repositoryName);

            InputSource is = new InputSource(in);
            RepositoryConfig config = RepositoryConfig.create(is, repositoryHome.getPath());
            repository = RepositoryImpl.create(config);

            log_info("JSR170 RI Repository initialized.");

            // set jaas config file path
            String jaasConfigFile = getServletConfig().getInitParameter(JAAS_CONFIG_FILE);
            if (jaasConfigFile != null && !"".equals(jaasConfigFile)) {
                System.setProperty("java.security.auth.login.config", jaasConfigFile);
                log_info("JAAS config path set by the tck webapp. java.security.auth.login.config = " + jaasConfigFile);
            } else {
                log_info("No JAAS config path set by the tck webapp.");
            }


            // set user id and password to read/write test results and configuration
            uid = getServletConfig().getInitParameter(USER_ID);
            pw = getServletConfig().getInitParameter(USER_PASSWORD);

        } catch (RepositoryException e) {
            log_info("Unable to initialize repository: " + e.toString(), e);
            throw new ServletException("Unable to initialize repository: " + e.toString(), e);
        }

    }

    public void destroy() {
        super.destroy();
        log_info("RepositoryServlet shutting down...");
    }

    private void log_info(String msg) {
        if (log != null) {
            log.info(msg);
        } else {
            log(msg);
        }
    }

    private void log_info(String msg, Throwable t) {
        if (log != null) {
            log.info(msg, t);
        } else {
            log(msg, t);
        }
    }
    /**
     * Returns the JSR170 repository
     * @return a jsr170 repository
     */
    public static Repository getRepository() {
        return repository;
    }

    /**
     * Returns the jcr session
     *
     * @return
     */
    public static Session getSession() {
        try {
            return login();
        } catch (RepositoryException e) {
            log.error("Unable to retrieve session: " + e.toString());
        }
        return null;
    }

    /**
     * Logs in to the repository. The user to login is specified in the servlet config.
     * @throws RepositoryException
     */
    public static Session login()
	    throws RepositoryException {

        // login
        Session repSession = repository.login(new SimpleCredentials(uid, pw.toCharArray()), null);
        return repSession;
    }

}
