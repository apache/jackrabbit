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

import java.io.IOException;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.commons.repository.ProxyRepository;

/**
 * Abstract base class for servlets that make a repository available in
 * the servlet context. This class handles the initialization and cleanup
 * tasks of setting up and clearing the configured repository attribute,
 * while a subclass only needs to implement the abstract
 * {@link #getRepository()} method that returns the actual content repository.
 * <p>
 * The {@link Repository} instance bound to the servlet context is actually
 * a {@link ProxyRepository} for late binding of the underlying content repository.
 * <p>
 * The default name of the repository attribute is
 * "<code>javax.jcr.Repository</code>", but it can be changed by specifying
 * an init parameter with the same name:
 * <pre>
 * &lt;servlet&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;javax.jcr.Repository&lt;/param-name&gt;
 *     &lt;param-value&gt;my.repository.attribute&lt;/param-value&gt;
 *     &lt;description&gt;
 *       This init parameter causes the repository to be looked up from
 *       the "my.repository.attribute" attribute instead of the default
 *       "javax.jcr.Repository".
 *     &lt;/description&gt;
 *   &lt;/init-param&gt;
 * &lt;/servlet&gt;
 * </pre>
 * <p>
 * A repository servlet can also be mapped to the URL space. See the
 * {@link #doGet(HttpServletRequest, HttpServletResponse)} method for
 * the details of the default behavior.
 *
 * @since 1.4
 */
public abstract class AbstractRepositoryServlet extends HttpServlet {

    /**
     * Binds a {@link ProxyRepository} with the repository returned by
     * {@link #getRepository()} in the configured servlet context attribute.
     *
     * @throws ServletException
     */
    public void init() throws ServletException {
        getServletContext().setAttribute(
                getAttributeName(),
                new ProxyRepository() {
                    @Override
                    protected Repository getRepository()
                            throws RepositoryException {
                        return AbstractRepositoryServlet.this.getRepository();
                    }
                });
    }

    /**
     * Removes the repository attribute from the servlet context.
     */
    public void destroy() {
        getServletContext().removeAttribute(getAttributeName());
    }

    /**
     * Returns the repository that will be used by the
     * {@link ProxyRepository} bound to the servlet context.
     *
     * @return repository
     * @throws RepositoryException if the repository could not be created
     */
    protected abstract Repository getRepository()
        throws RepositoryException;

    /**
     * Returns the name of the repository attribute. The default
     * implementation returns "<code>javax.jcr.Repository</code>" or
     * the value of the "<code>javax.jcr.Repository</code>" init parameter.
     * <p>
     * A subclass can override this method to customize the attribute name,
     * but for consistency it is generally better not to do that.
     *
     * @return name of the repository attribute
     */
    protected String getAttributeName() {
        String name = Repository.class.getName();
        return getInitParameter(name, name);
    }

    /**
     * Utility method that returns the named init parameter or the given
     * default value if the parameter does not exist.
     *
     * @param name name of the init parameter
     * @param def default value
     * @return value of the init parameter, or the default value
     */
    protected String getInitParameter(String name, String def) {
        String value = getInitParameter(name);
        if (value == null) {
            value = def;
        }
        return value;
    }

    /**
     * Outputs the repository descriptors either as a collection of properties
     * (see {@link Properties#store(java.io.OutputStream, String)} or
     * individually addressable text/plain resources based on the request URI.
     * <p>
     * A typical mapping for a repository servlet would be:
     * <pre>
     * &lt;servlet-mapping&gt;
     *   &lt;servlet-name&gt;Repository&lt;/servlet-name&gt;
     *   &lt;url-pattern&gt;/repository/*&lt;/url-pattern&gt;
     * &lt;/servlet-mapping&gt;
     * </pre>
     * <p>
     * This mapping would allow clients to retrieve all repository descriptors
     * from <code>http://server/context/repository/</code> and to address
     * individual descriptors by key with URIs like
     * <code>http://server/context/repository/<i>key</i></code>.
     * For example, the name of the repository vendor could be retrieved from
     * <code>http://server/context/repository/jcr.repository.vendor</code>.
     * Likewise, a 404 (not found) response from
     * <code>http://server/context/repository/level.2.supported</code> would
     * indicate that the repository does not support Level 2 features.
     * <p>
     * Note that mapping a repository servlet to the URL space is optional,
     * as the main purpose of the servlet is to make a repository available
     * in the servlet context, not to expose repository information to web
     * clients.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws IOException on IO errors
     * @throws ServletException on servlet errors
     */
    protected void doGet(
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        Repository repository = new ServletRepository(this);

        String info = request.getPathInfo();
        if (info == null || info.equals("/")) {
            Properties descriptors = new Properties();
            String[] keys = repository.getDescriptorKeys();
            for (int i = 0; i < keys.length; i++) {
                descriptors.setProperty(
                        keys[i], repository.getDescriptor(keys[i]));
            }
            // TODO: Using UTF-8 instead of ISO-8859-1 would be better, but
            // would require re-implementing the Properties.store() method
            response.setContentType("text/plain; charset=ISO-8859-1");
            descriptors.store(response.getOutputStream(), getAttributeName());
        } else {
            String key = info.substring(1); // skip the leading "/"
            String descriptor = repository.getDescriptor(key);
            if (descriptor != null) {
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write(descriptor);
            } else {
                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                        "Repository descriptor " + key + " not found");
            }
        }
    }

}
