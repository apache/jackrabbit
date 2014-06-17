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
package org.apache.jackrabbit.core.security.principal;

import javax.jcr.RepositoryException;
import java.util.Properties;

/**
 * Registry used to store and retrieve <code>PrincipalProvider</code>s.
 *
 * @see PrincipalProvider
 */
public interface PrincipalProviderRegistry {

    /**
     * Registers a new provider by means of a configuration. The
     * registry expects the properties to contain a
     * {@link org.apache.jackrabbit.core.config.LoginModuleConfig#PARAM_PRINCIPAL_PROVIDER_CLASS}
     * to be able to create a instance of PrincipalProvider.
     * <p>
     * The Properties will be passed to the instantiated Provider via
     * {@link PrincipalProvider#init(Properties)}
     *
     * @param configuration Properties for the Provider
     * @return the newly added Provider or <code>null</code> if the configuration
     * was incomplete.
     * @throws RepositoryException if an error occurs while creating the
     * provider instance.
     */
    PrincipalProvider registerProvider(Properties configuration) throws RepositoryException;

    /**
     * @return the default principal provider
     */
    PrincipalProvider getDefault();

    /**
     * @param className Name of the principal provider class.
     * @return PrincipalProvider or <code>null</code> if no provider with
     * the given class name was registered.
     */
    PrincipalProvider getProvider(String className);

    /**
     * Returns all registered providers.
     *
     * @return all registered providers.
     */
    PrincipalProvider[] getProviders();
}
