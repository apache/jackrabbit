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
package org.apache.jackrabbit.browser;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.jackrabbit.command.CommandHelper;

/**
 * Filter stores the jcr session in http session. It also synchronizes the
 * access to a jcr session.
 */
public class JcrLoginFilter implements Filter {

	private static Repository repository;

	public void destroy() {

	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws java.io.IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;

		// Create the jcr session if necessary
		try {
			if (httpRequest.getSession()
					.getAttribute(CommandHelper.SESSION_KEY) == null) {
				Session s = getRepository().login(
						new SimpleCredentials(httpRequest.getRemoteUser(), ""
								.toCharArray()));
				httpRequest.getSession().setAttribute(
						CommandHelper.SESSION_KEY, s);
			}
		} catch (Exception e) {
			throw new ServletException(
					"unable to bind jcr session to http session", e);
		}

		// get the session from the http session
		Object session = httpRequest.getSession().getAttribute(
				CommandHelper.SESSION_KEY);

		// Synchronize request
		synchronized (session) {
			chain.doFilter(request, response);
		}

	}

	public void init(FilterConfig cfg) throws ServletException {

	}

	public static Repository getRepository() {
		if (repository == null) {
			try {
				InitialContext ctx = new InitialContext();
				String jndiAddress = (String) ctx
						.lookup("java:comp/env/jcr/jndi/address");
				String jndiProperties = (String) ctx
						.lookup("java:comp/env/jcr/jndi/properties");
				Properties properties = new Properties();
				properties.load(new ByteArrayInputStream(jndiProperties
						.getBytes()));
				InitialContext repoCtx = new InitialContext(properties);
				repository = (Repository) repoCtx.lookup(jndiAddress);
			} catch (Exception e) {
				throw new IllegalStateException(
						"unable to retrieve repository. ", e);
			}
		}
		return repository;
	}

}
