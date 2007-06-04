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

import org.apache.jackrabbit.commons.repository.RepositoryFactory;

/**
 * Factory that returns a repository from an attribute in an identified
 * servlet context.
 *
 * @since 1.4
 */
public class CrossContextRepositoryFactory implements RepositoryFactory {

    /**
     * Base servlet context that is used to look up the other context.
     */
    private final ServletContext context;

    /**
     * Context path of the other servlet context that contains the
     * repository attribute.
     */
    private final String path;

    /**
     * Name of the repository attribute in the identified servlet context.
     */
    private final String name;

    /**
     * Creates a factory that looks up a repository from the configured
     * attribute of the configured servlet context.
     *
     * @param context base servlet context
     * @param path context path of the servlet context that contains the
     *             repository attribute
     * @param name name of the repository attribute
     */
    public CrossContextRepositoryFactory(
            ServletContext context, String path, String name) {
        this.context = context;
        this.path = path;
        this.name = name;
    }

    /**
     * Returns the repository from the configured attribute of the configured
     * servlet context.
     *
     * @return repository instance
     * @throws RepositoryException if the servlet context or the repository
     *                             attribute does not exist, or if the
     *                             attribute does not contain a repository
     *                             instance
     */
    public Repository getRepository() throws RepositoryException {
        ServletContext context = this.context.getContext(path);
        if (context == null) {
            throw new RepositoryException(
                    "Repository not found: Servlet context " + path
                    + " does not exist or is not accessible from context "
                    + this.context.getServletContextName());
        }
        return new ContextRepositoryFactory(context, name).getRepository();
    }

}
