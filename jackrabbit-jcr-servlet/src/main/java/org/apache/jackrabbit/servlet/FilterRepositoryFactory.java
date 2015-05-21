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
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.commons.repository.RepositoryFactory;

/**
 * Factory that looks up a repository from the context of a given filter.
 * <p>
 * The default name of the repository attribute is
 * "<code>javax.jcr.Repository</code>", but it can be changed by specifying
 * an init parameter with the same name:
 * <pre>
 * &lt;filter&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;javax.jcr.Repository&lt;/param-name&gt;
 *     &lt;param-value&gt;my.repository.attribute&lt;/param-value&gt;
 *     &lt;description&gt;
 *       This init parameter causes the repository to be looked up from
 *       the "my.repository.attribute" attribute instead of the default
 *       "javax.jcr.Repository".
 *     &lt;/description&gt;
 *   &lt;/init-param&gt;
 * &lt;/filter&gt;
 * </pre>
 *
 * @since Apache Jackrabbit 1.6
 */
public class FilterRepositoryFactory implements RepositoryFactory {

    /**
     * Configuration of the filter whose context contains the repository.
     */
    private final FilterConfig config;

    /**
     * Creates a factory for looking up a repository from the context
     * associated with the given filter configuration.
     *
     * @param config filter configuration
     */
    public FilterRepositoryFactory(FilterConfig config) {
        this.config = config;
    }

    /**
     * Looks up and returns a repository bound in the servlet context of
     * the given filter.
     *
     * @return repository from servlet context
     * @throws RepositoryException if the repository is not available
     */
    public Repository getRepository() throws RepositoryException {
        String name = config.getInitParameter(Repository.class.getName());
        if (name == null) {
            name = Repository.class.getName();
        }

        ServletContext context = config.getServletContext();
        Object repository = context.getAttribute(name);
        if (repository instanceof Repository) {
            return (Repository) repository;
        } else if (repository != null) {
            throw new RepositoryException(
                    "Invalid repository: Attribute " + name
                    + " in servlet context " + context.getServletContextName()
                    + " is an instance of " + repository.getClass().getName());
        } else {
            throw new RepositoryException(
                    "Repository not found: Attribute " + name
                    + " does not exist in servlet context "
                    + context.getServletContextName());
        }
    }

}
