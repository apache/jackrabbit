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
package org.apache.jackrabbit.servlet.login;

import java.io.IOException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.servlet.ServletRepository;

/**
 *
 * @since Apache Jackrabbit 1.6
 */
public abstract class AbstractLoginFilter implements Filter {

    private Repository repository;

    private String workspace;

    private String sessionAttribute;

    private String nodeAttribute;

    public void init(FilterConfig config) {
        repository = new ServletRepository(config);
        workspace = config.getInitParameter("workspace");

        sessionAttribute = config.getInitParameter(Session.class.getName());
        if (sessionAttribute == null) {
            sessionAttribute = Session.class.getName();
        }

        nodeAttribute = config.getInitParameter(Node.class.getName());
        if (nodeAttribute == null) {
            nodeAttribute = Node.class.getName();
        }
    }

    public void destroy() {
    }

    public void doFilter(
            ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        try {
            Credentials credentials = getCredentials(httpRequest);
            Session session = repository.login(credentials, workspace);
            try {
                request.setAttribute(sessionAttribute, session);
                request.setAttribute(nodeAttribute, session.getRootNode());
                chain.doFilter(request, response);
                if (session.hasPendingChanges()) {
                    session.save();
                }
            } finally {
                session.logout();
            }
        } catch (ServletException e) {
            Throwable cause = e.getRootCause();
            if (cause instanceof AccessDeniedException) {
                httpResponse.sendError(
                        HttpServletResponse.SC_FORBIDDEN, cause.getMessage());
            } else {
                throw e;
            }
        } catch (LoginException e) {
            httpResponse.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (NoSuchWorkspaceException e) {
            throw new ServletException(
                    "Workspace " + workspace
                    + " not found in the content repository", e);
        } catch (RepositoryException e) {
            throw new ServletException(
                    "Unable to access the content repository", e);
        }
    }

    protected abstract Credentials getCredentials(HttpServletRequest request);

}
