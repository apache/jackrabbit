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
package org.apache.jackrabbit.command.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.commons.lang.ClassUtils;
import org.apache.jackrabbit.command.CommandHelper;
import org.apache.log4j.Logger;

/**
 * <p>
 * Servlet that handler jcr requests with commons-chain.<br>
 * Usage scenario example: JCR Ajax server.
 * </p>
 */
public class JcrCommandServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(JcrCommandServlet.class);

    private static String CATALOG_KEY = "catalog";

    private static String JNDI_PROPERTIES_KEY = "jndi.properties";

    private static String JNDI_ADDRESS_KEY = "jndi.address";

    private static String USER_KEY = "user";

    private static String PASSWORD_KEY = "password";

    public static String COMMAND_KEY = "command";

    public static String OUTPUT_FLAVOR_KEY = "flavor";

    private static String DEFAULT_OUTPUT_FLAVOR = "text/plain";

    private static String PRINT_SUCCESS_KEY = "print.success";

    private static String SUCCESS_MESSAGE_KEY = "success.message.key";

    private static String AUTO_SAVE_KEY = "auto.save";

    private static String SESSION_KEY = "session.key";

    /**
     * key under which the jcr is stored in the http session
     */
    private String sessionKey = CommandHelper.SESSION_KEY;

    /**
     * flag that indicates whether a "success" string should be printed in the
     * response
     */
    private boolean printSuccess;

    /**
     * Sucess message context key
     */
    private String successMessageKey = "success.message";

    /**
     * flag that indicates whether the changes should be saved automatically
     */
    private boolean autoSave;

    /**
     * jcr repository
     */
    private Repository repository;

    /**
     * user name to login
     */
    private String user;

    /**
     * password to login
     */
    private String password;

    /**
     * commons chain catalog
     */
    private Catalog catalog;

    public void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // read parameters
        String cmdName = req.getRequestURI().substring(
                req.getContextPath().length() + req.getServletPath().length());
        String flavor = req.getParameter(OUTPUT_FLAVOR_KEY);
        if (flavor == null) {
            flavor = DEFAULT_OUTPUT_FLAVOR;
        }

        // set content type
        res.setContentType(flavor);
        // try to get the jcr session from the httpsession
        boolean closeSession = false;
        Session session = (Session) req.getSession().getAttribute(
                this.sessionKey);
        try {
            if (session == null) {
                if (this.user == null) {
                    session = this.repository.login(req.getRemoteUser());
                } else {
                    session = this.repository.login(new SimpleCredentials(
                            this.user, this.password.toCharArray()));
                }
                // If a new session was created then close it after running the
                // command
                closeSession = true;
            }

            PrintWriter pw = null;
            try {
                // create context
                JcrServletWebContext ctx = new JcrServletWebContext(session,
                        getServletContext(), req, res);

                // get writer
                pw = CommandHelper.getOutput(ctx);

                // lookup command
                Command cmd = catalog.getCommand(cmdName);
                if (cmd == null) {
                    throw new IllegalArgumentException("command " + cmdName
                            + " not found");
                }

                // execute command
                cmd.execute(ctx);

                if (this.autoSave) {
                    // save changes
                    session.save();
                }

                if (this.printSuccess) {
                    // write success message
                    String msg = (String) ctx.get(successMessageKey);
                    if (msg == null) {
                        pw.write("success");
                    } else {
                        pw.write(msg);
                    }
                }

            } catch (Exception e) {
                pw.write(ClassUtils.getShortClassName(e.getClass()) + "\n"
                        + e.getMessage());
            } finally {
                pw.close();

            }

        } catch (Exception e) {
            throw new ServletException(e);

        } finally {
            if (closeSession && session != null) {
                session.logout();
            }

        }
    }

    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);

        String jndiProperties = cfg.getInitParameter(JNDI_PROPERTIES_KEY);
        String jndiAddress = cfg.getInitParameter(JNDI_ADDRESS_KEY);
        this.user = cfg.getInitParameter(USER_KEY);
        this.password = cfg.getInitParameter(PASSWORD_KEY);
        
        this.successMessageKey = cfg.getInitParameter(SUCCESS_MESSAGE_KEY);

        if (cfg.getInitParameter(PRINT_SUCCESS_KEY) != null) {
            this.printSuccess = Boolean.valueOf(cfg
                    .getInitParameter(PRINT_SUCCESS_KEY)).booleanValue();
        }

        if (cfg.getInitParameter(AUTO_SAVE_KEY) != null) {
            this.autoSave = Boolean
                    .valueOf(cfg.getInitParameter(AUTO_SAVE_KEY)).booleanValue();
        }

        if (cfg.getInitParameter(SESSION_KEY) != null) {
            this.sessionKey = cfg.getInitParameter(SESSION_KEY);
        }

        try {

            // parse catalog
            ConfigParser parser = new ConfigParser();
            parser.parse(JcrCommandServlet.class.getClassLoader().getResource(
                    cfg.getInitParameter(CATALOG_KEY)));
            catalog = CatalogFactoryBase.getInstance().getCatalog();

            // get repository
            InputStream is = new ByteArrayInputStream(jndiProperties
                    .getBytes("UTF-8"));
            Properties props = new Properties();
            props.load(is);
            InitialContext ctx = new InitialContext(props);
            this.repository = (Repository) ctx.lookup(jndiAddress);

        } catch (Exception e) {
            String msg = "unable to get repository through jndi";
            log.error(msg, e);
            throw new ServletException(msg, e);

        }
    }

}
