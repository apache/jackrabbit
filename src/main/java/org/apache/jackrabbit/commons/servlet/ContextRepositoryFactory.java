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
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.commons.repository.RepositoryFactory;

/**
 * Factory that returns a repository from a servlet context attribute.
 */
public class ContextRepositoryFactory implements RepositoryFactory {

    /**
     * Servlet context.
     */
    private final ServletContext context;

    /**
     * Name of the servlet context attribute.
     */
    private final String name;

    /**
     * Creates a factory that looks up a repository form the configured
     * servlet context attribute.
     *
     * @param context servlet context
     * @param name name of the servlet context attribute
     */
    public ContextRepositoryFactory(
            ServletContext context, String name) {
        this.context = context;
        this.name = name;
    }

    /**
     * Returns the repository from the configured servlet context attribute.
     *
     * @return repository instance
     * @throws RepositoryException if the servlet context attribute does not
     *                             exist, or if it does not contain a
     *                             repository instance
     */
    public Repository getRepository() throws RepositoryException {
        Object repository = context.getAttribute(name);
        if (repository instanceof Repository) {
            return (Repository) repository;
        } else if (repository != null) {
            throw new RepositoryException(
                    "Invalid repository: Attribute " + name
                    + " in servet context " + context.getServletContextName()
                    + " is an instance of " + repository.getClass().getName());
        } else {
            throw new RepositoryException(
                    "Repository not found: Attribute " + name
                    + " does not exist in servet context "
                    + context.getServletContextName());
        }
    }

}
