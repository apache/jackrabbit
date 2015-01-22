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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Repository;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.JackrabbitRepositoryStub;
import org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet;
import org.apache.jackrabbit.test.RepositoryStubException;
import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class RepositoryStubImpl extends JackrabbitRepositoryStub {

	private static Repository repository;

	private static SocketConnector connector;

	private static Server server;

	private static Repository client;

	private final String acProviderImplClass;
	
	private final String protectedRemoveImplClass;
	
	private static final String PROP_ACCESSCONTROL_PROVIDER_CLASS = "org.apache.jackrabbit.jcr2spi.AccessControlProvider.class";

	private static final String PROP_PROTECTED_ITEM_REMOVE_CLASS = "javax.jcr.tck.access.control.list.handler";
	
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

		if (connector == null) {
			connector = new SocketConnector();
			connector.setHost("localhost");
			String pvalue = System.getProperty("org.apache.jackrabbit.jcr2dav.RepositoryStubImpl.port", "0");
			int port = pvalue.equals("") ? 0 : Integer.parseInt(pvalue);
			connector.setPort(port);
		}

		if (server == null) {
			server = new Server();
			server.addConnector(connector);

			ServletHolder holder = new ServletHolder(new JcrRemotingServlet() {
				protected Repository getRepository() {
					return repository;
				}
			});
			holder.setInitParameter(JCRWebdavServerServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, "");
			holder.setInitParameter(JCRWebdavServerServlet.INIT_PARAM_MISSING_AUTH_MAPPING, "");
			holder.setInitParameter(JcrRemotingServlet.INIT_PARAM_PROTECTED_HANDLERS_CONFIG, protectedRemoveImplClass);

			Context context = new Context(server, "/");
			context.addServlet(holder, "/*");
			server.addHandler(context);

			try {
				server.start();
			} catch (Exception e) {
				throw new RepositoryStubException(e);
			}
		}

		if (client == null) {
			try {
				Map<String, String> parameters = new HashMap<String, String>();

				String uri = "http://localhost:" + connector.getLocalPort() + "/";

				parameters.put(JcrUtils.REPOSITORY_URI, uri);
				parameters.put(PROP_ACCESSCONTROL_PROVIDER_CLASS, acProviderImplClass);

				client = JcrUtils.getRepository(parameters);
			} catch (Exception e) {
				throw new RepositoryStubException(e);
			}
		}

		return client;
	}

	public static void stopServer() throws Exception {
		if (server != null) {
			server.stop();
		}
	}
}
