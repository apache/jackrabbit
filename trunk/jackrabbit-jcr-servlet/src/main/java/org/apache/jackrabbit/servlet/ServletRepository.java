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

import javax.servlet.FilterConfig;
import javax.servlet.GenericServlet;

import org.apache.jackrabbit.commons.repository.ProxyRepository;

/**
 * Proxy for a repository bound in servlet context. The configured repository
 * attribute is looked up from the servlet context during each method call.
 * Thus the repository does not need to exist when this class is instantiated.
 * The repository can also be replaced with another repository during the
 * lifetime of an instance of this class.
 * <p>
 * A typical way to use this class would be:
 * <pre>
 * public class MyServlet extends HttpServlet {
 *
 *     private final Repository repository = new ServletRepository(this);
 *
 *     protected void doGet(
 *             HttpServletRequest request, HttpServletResponse response)
 *             throws ServletException, IOException {
 *          try {
 *              Session session = repository.login();
 *              try {
 *                  ...;
 *              } finally {
 *                  session.logout();
 *              }
 *          } catch (RepositoryException e) {
 *              throw new ServletException(e);
 *          }
 *      }
 *
 * }
 * </pre>
 * <p>
 * Starting with version 1.6 this class can also be used by a servlet filter:
 * <pre>
 * public class MyFilter implements Filter {
 *
 *     private Repository repository;
 *
 *     public void init(FilterConfig config) {
 *         repository = new ServletRepository(config);
 *     }
 *
 *     // ...
 *
 * }
 * </pre>
 
 *
 * @since 1.4
 * @see ServletRepositoryFactory
 * @see FilterRepositoryFactory
 */
public class ServletRepository extends ProxyRepository {

    /**
     * Creates a proxy for a repository found in the context of the given
     * servlet.
     *
     * @param servlet servlet
     */
    public ServletRepository(GenericServlet servlet) {
        super(new ServletRepositoryFactory(servlet));
    }

    /**
     * Creates a proxy for a repository found in the servlet context
     * associated with the given filter configuration.
     *
     * @since Apache Jackrabbit 1.6
     * @param config filter configuration
     */
    public ServletRepository(FilterConfig config) {
        super(new FilterRepositoryFactory(config));
    }

}
