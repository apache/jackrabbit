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
package org.apache.jackrabbit.jcr2dav;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.JackrabbitRepositoryStub;
import org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStubException;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class RepositoryStubImpl extends JackrabbitRepositoryStub {

    private static final String PROP_ACCESSCONTROL_PROVIDER_CLASS = "org.apache.jackrabbit.jcr2spi.AccessControlProvider.class";

    private static final String PROP_PROTECTED_ITEM_REMOVE_CLASS = "org.apache.jackrabbit.server.ProtectedItemRemoveHandler.class";

    private static Repository repository;

    private static ServerConnector connector;

    private static Server server;

    private static Repository client;

    private final String acProviderImplClass;

    private final String protectedRemoveImplClass;

    private static final String WEBDAV_SERVLET_CONTEXT_PATH = System.getProperty("WebDAVServletContext", "");

    private static final String WEBDAV_SERVLET_PATH_PREFIX = System.getProperty("WebDAVServletPrefix", "");

    private static final String WEBDAV_SERVLET_PATH_MAPPING = WEBDAV_SERVLET_PATH_PREFIX + "/*";

    public RepositoryStubImpl(Properties env) {
        super(env);
        acProviderImplClass = env.getProperty(PROP_ACCESSCONTROL_PROVIDER_CLASS);
        protectedRemoveImplClass = env.getProperty(PROP_PROTECTED_ITEM_REMOVE_CLASS);
    }

    @Override
    public Repository getRepository() throws RepositoryStubException {
        if (repository == null) {
            repository = super.getRepository();
        }

        if (server == null) {
            server = new Server();

            ServletHolder holder = new ServletHolder(new JcrRemotingServlet() {
                protected Repository getRepository() {
                    return repository;
                }
            });
            String pathPrefix = WEBDAV_SERVLET_PATH_PREFIX;
            if (pathPrefix.endsWith("/")) {
                pathPrefix = pathPrefix.substring(0, pathPrefix.length() - 1);
            }
            holder.setInitParameter(JCRWebdavServerServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, pathPrefix);
            holder.setInitParameter(JCRWebdavServerServlet.INIT_PARAM_MISSING_AUTH_MAPPING, "");
            holder.setInitParameter(JcrRemotingServlet.INIT_PARAM_PROTECTED_HANDLERS_CONFIG, protectedRemoveImplClass);

            ServletContextHandler schandler = new ServletContextHandler(server, WEBDAV_SERVLET_CONTEXT_PATH);
            schandler.addServlet(holder, WEBDAV_SERVLET_PATH_MAPPING);
        }

        if (connector == null) {
            connector = new ServerConnector(server);
            connector.setHost("localhost");
            String pvalue = System.getProperty("org.apache.jackrabbit.jcr2dav.RepositoryStubImpl.port", "0");
            int port = pvalue.equals("") ? 0 : Integer.parseInt(pvalue);
            connector.setPort(port);
            server.addConnector(connector);

            try {
                server.start();
            } catch (Exception e) {
                throw new RepositoryStubException(e);
            }
        }

        if (client == null) {
            try {
                Map<String, String> parameters = new HashMap<String, String>();

                String uri = "http://localhost:" + connector.getLocalPort() + WEBDAV_SERVLET_CONTEXT_PATH + WEBDAV_SERVLET_PATH_PREFIX;

                String parmName = System.getProperty(this.getClass().getName() + ".REPURIPARM", JcrUtils.REPOSITORY_URI);
                parameters.put(parmName, uri);
                parameters.put(PROP_ACCESSCONTROL_PROVIDER_CLASS, acProviderImplClass);

                client = JcrUtils.getRepository(parameters);
            } catch (Exception e) {
                throw new RepositoryStubException(e);
            }
        }

        return client;
    }

    @Override
    public Principal getKnownPrincipal(Session session) throws RepositoryException {
        // TODO
        return new Principal() {
            @Override
            public String getName() {
                return "everyone";
            }
        };
    }

    @Override
    public Principal getUnknownPrincipal(Session session) throws RepositoryException, NotExecutableException {
        // TODO
        return new Principal() {
            @Override
            public String getName() {
                return "unknownPrincipal";
            }
        };
    }

    public static void stopServer() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}
