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
package org.apache.jackrabbit.commons.servlet;

import javax.jcr.Repository;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.jackrabbit.commons.repository.ProxyRepository;
import org.apache.jackrabbit.commons.repository.RepositoryFactory;

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
 */
public class ContextRepositoryServlet extends AbstractRepositoryServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 6222606878557491477L;

    /**
     * Creates and returns a repository proxy for accessing a repository
     * in the configured servlet context attribute.
     *
     * @return repository proxy
     */
    protected Repository getRepository() throws ServletException {
        String path = getInitParameter("path");
        String name = getInitParameter("name", Repository.class.getName());

        ServletContext context = getServletContext();
        if (path != null && context.equals(context.getContext(path))) {
            path = null;
        }

        if (path == null && name.equals(getAttributeName())) {
            throw new ServletException(
                    "Invalid configuration: Can not duplicate attribute "
                    + name + " of servlet " + getServletName());
        }

        RepositoryFactory factory;
        if (context != null) {
            factory = new CrossContextRepositoryFactory(context, path, name);
        } else {
            factory = new ContextRepositoryFactory(context, name);
        }
        return new ProxyRepository(factory);
    }

}
