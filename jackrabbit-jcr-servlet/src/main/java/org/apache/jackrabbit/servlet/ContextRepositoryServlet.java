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
package org.apache.jackrabbit.servlet;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;

/**
 * Servlet that makes a repository from one servlet context attribute
 * available as another servlet context attribute. The source context
 * can be different from the context of this servlet.
 * <p>
 * The supported initialization parameters of this servlet are:
 * <dl>
 *   <dt>javax.jcr.Repository</dt>
 *   <dd>
 *     Name of the target servlet context attribute.
 *     The default value is "<code>javax.jcr.Repository</code>".
 *   </dd>
 *   <dt>path</dt>
 *   <dd>
 *     Context path of the source servlet context. The source context
 *     defaults to the context of this servlet if this parameter is not set.
 *   </dd>
 *   <dt>name</dt>
 *   <dd>
 *     Name of the source servlet context attribute. The default value
 *     is "<code>javax.jcr.Repository</code>". The name of the source attribute
 *     can be the same as the name of target attribute only if the source
 *     context is different from the context of this servlet.
 *   </dd>
 * </dl>
 * <p>
 * This servlet can also be mapped to the URL space. See
 * {@link AbstractRepositoryServlet} for the details.
 *
 * @since 1.4
 */
public class ContextRepositoryServlet extends AbstractRepositoryServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 6222606878557491477L;

    /**
     * Creates and returns the repository in the configured servlet
     * context attribute.
     *
     * @return repository
     */
    protected Repository getRepository() throws RepositoryException {
        String path = getInitParameter("path");
        String name = getInitParameter("name", Repository.class.getName());

        ServletContext context = getServletContext();
        if (path != null && context.equals(context.getContext(path))) {
            path = null;
        }

        if (path == null && name.equals(getAttributeName())) {
            throw new RepositoryException(
                    "Invalid configuration: Can not duplicate attribute "
                    + name + " of servlet " + getServletName());
        }

        ServletContext otherContext = context.getContext(path);
        if (otherContext == null) {
            throw new RepositoryException(
                    "Repository not found: Servlet context " + path
                    + " does not exist or is not accessible from context "
                    + context.getServletContextName());
        }

        Object repository = otherContext.getAttribute(name);
        if (repository instanceof Repository) {
            return (Repository) repository;
        } else if (repository != null) {
            throw new RepositoryException(
                    "Invalid repository: Attribute " + name
                    + " in servlet context " + otherContext.getServletContextName()
                    + " is an instance of " + repository.getClass().getName());
        } else {
            throw new RepositoryException(
                    "Repository not found: Attribute " + name
                    + " does not exist in servlet context "
                    + otherContext.getServletContextName());
        }
    }

}
