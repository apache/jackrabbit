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

import java.io.PrintWriter;

import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.chain.web.servlet.ServletWebContext;
import org.apache.jackrabbit.command.CommandHelper;

/**
 * Commons chain context that wraps an httpservletrequest and exposes attributes
 * from servlet scopes (request, session, application) as context variables.
 * 
 */
public class JcrServletWebContext extends ServletWebContext {

	private static final long serialVersionUID = 1L;

	private Session session;

	private PrintWriter out;

	public JcrServletWebContext(Session session, ServletContext ctx,
			HttpServletRequest req, HttpServletResponse res) {
		super(ctx, req, res);
		this.session = session;
		try {
			this.out = res.getWriter();
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"the given response fails to provide an outputstream");
		}
	}

	public Object get(Object key) {

		// known context variables
		try {

			if (key.equals(CommandHelper.REPOSITORY_KEY)) {
				return session.getRepository();
			}

			if (key.equals(CommandHelper.SESSION_KEY)) {
				return session;
			}

			if (key.equals(CommandHelper.CURRENT_NODE_KEY)) {
				return session.getRootNode();
			}

			if (key.equals(CommandHelper.OUTPUT_KEY)) {
				return this.out;
			}

		} catch (Exception e) {
			return null;
		}

		// lookup in the underlying map
		if (super.get(key)!=null) {
			return super.get(key) ;
		}

		// lookup in request parameters
		if (key instanceof String
				&& this.getRequest().getParameter((String) key) != null) {
			return this.getRequest().getParameter((String) key);
		}

		// lookup in request attributes
		if (key instanceof String && this.getRequestScope().get(key) != null) {
			return this.getRequestScope().get(key);
		}

		// lookup in session attributes
		if (key instanceof String && this.getSessionScope().get(key) != null) {
			return this.getSessionScope().get(key);
		}

		// lookup in application attributes
		if (key instanceof String
				&& this.getApplicationScope().get(key) != null) {
			return this.getSessionScope().get(key);
		}
		
		return null ;

	}
}
