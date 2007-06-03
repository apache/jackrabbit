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
package org.apache.jackrabbit.commons.repository;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Factory that looks up a repository from JNDI.
 *
 * @since 1.4
 */
public class JNDIRepositoryFactory implements RepositoryFactory {

    /**
     * JNDI context from which to look up the repository.
     */
    private final Context context;

    /**
     * JNDI name of the repository.
     */
    private final String name;

    /**
     * Creates a factory for looking up a repository from JNDI.
     *
     * @param context JNDI context
     * @param name JNDI name of the repository
     */
    public JNDIRepositoryFactory(Context context, String name) {
        this.context = context;
        this.name = name;
    }

    /**
     * Looks up and returns the configured repository.
     *
     * @return repository instance
     * @throws RepositoryException if the repository can not be found
     */
    public Repository getRepository() throws RepositoryException {
        try {
            Object repository = context.lookup(name);
            if (repository instanceof Repository) {
                return (Repository) repository;
            } else if (repository == null) {
                throw new RepositoryException(
                        "Repository not found: The JNDI entry "
                        + name + " is null");
            } else {
                throw new RepositoryException(
                        "Invalid repository: The JNDI entry "
                        + name + " is an instance of "
                        + repository.getClass().getName());
            }
        } catch (NamingException e) {
            throw new RepositoryException(
                    "Repository not found: The JNDI entry " + name
                    + " could not be looked up", e);
        }
    }

}
