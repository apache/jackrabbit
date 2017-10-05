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
package org.apache.jackrabbit.client;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

/**
 * This {@link RepositoryFactory} implementations is capable of creating any
 * repository which is covered by the Apache Jackrabbit project. It does so by
 * delegating back to secondary RepositoryFactory implementations. The
 * parameters passed to the {@link #getRepository(Map)} method determine which
 * secondary RepositoryFactory this factory delegates to.
 */
public class RepositoryFactoryImpl implements RepositoryFactory {

    /**
     * When this key parameter is present, this factory delegates to
     * {@code org.apache.jackrabbit.jcr2spi.Jcr2spiRepositoryFactory}
     */
    public static final String PARAM_REPOSITORY_SERVICE_FACTORY = "org.apache.jackrabbit.spi.RepositoryServiceFactory";

    /**
     * When this key parameter is present, this factory delegates to
     * {@code org.apache.jackrabbit.jcr2spi.Jcr2spiRepositoryFactory}
     */
    public static final String PARAM_REPOSITORY_CONFIG = "org.apache.jackrabbit.jcr2spi.RepositoryConfig";

    /**
     * Creates a JCR repository from the given <code>parameters</code>.
     * If either {@link #PARAM_REPOSITORY_SERVICE_FACTORY} or
     * {@link #PARAM_REPOSITORY_CONFIG} is present, this factory delegates
     * to {@code org.apache.jackrabbit.jcr2spi.Jcr2spiRepositoryFactory}.
     * Otherwise it delegates to
     * {@code org.apache.jackrabbit.core.RepositoryFactoryImpl}.
     *
     * @see RepositoryFactory#getRepository(java.util.Map)
     */
    public Repository getRepository(@SuppressWarnings("rawtypes") Map parameters) throws RepositoryException {
        String repositoryFactoryName = parameters != null && (
                                       parameters.containsKey(PARAM_REPOSITORY_SERVICE_FACTORY) ||
                                       parameters.containsKey(PARAM_REPOSITORY_CONFIG))
                ? "org.apache.jackrabbit.jcr2spi.Jcr2spiRepositoryFactory"
                : "org.apache.jackrabbit.core.RepositoryFactoryImpl";

        Object repositoryFactory;
        try {
            Class<?> repositoryFactoryClass = Class.forName(repositoryFactoryName, true,
                    Thread.currentThread().getContextClassLoader());

            repositoryFactory = repositoryFactoryClass.newInstance();
        }
        catch (Exception e) {
            throw new RepositoryException(e);
        }

        if (repositoryFactory instanceof RepositoryFactory) {
            return ((RepositoryFactory) repositoryFactory).getRepository(parameters);
        }
        else {
            throw new RepositoryException(repositoryFactory + " is not a RepositoryFactory");
        }
    }
}