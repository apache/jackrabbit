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
package org.apache.jackrabbit.core.config;

import javax.security.auth.spi.LoginModule;

/**
 * LoginModule configuration. This bean configuration class is used to
 * create login module objects.
 * <p>
 * Login module is an optional configuration that allows to use JackRabbit
 * in a non-JAAS environment.
 *
 * @see RepositoryConfig#getLoginModuleConfig()
 */
public class LoginModuleConfig extends BeanConfig {

    /**
     * UserId of the anonymous user. Optional parameter in the LoginModule
     * configuration.
     */
    public static final String PARAM_ANONYMOUS_ID = "anonymousId";
    
    /**
     * UserId of the administrator. Optional parameter in the LoginModule
     * configuration.
     */
    public static final String PARAM_ADMIN_ID = "adminId";

    /**
     * Property-Key for the fully qualified class name of the implementation of
     * {@link org.apache.jackrabbit.core.security.principal.PrincipalProvider}
     * to be used with the LoginModule.
     */
    public static final String PARAM_PRINCIPAL_PROVIDER_CLASS = "principalProvider";

    /**
     * Same as {@link LoginModuleConfig#PARAM_PRINCIPAL_PROVIDER_CLASS}.
     * Introduced for compatibility reasons.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2629">JCR-2629</a>
     */
    public static final String COMPAT_PRINCIPAL_PROVIDER_CLASS = "principal_provider.class";

    /**
     * Property-Key if the <code>PrincipalProvider</code> configured with
     * {@link LoginModuleConfig#PARAM_PRINCIPAL_PROVIDER_CLASS} be registered using the
     * specified name instead of the class name which is used by default if the
     * name parameter is missing.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2629">JCR-2629</a>
     */
    public static final String COMPAT_PRINCIPAL_PROVIDER_NAME = "principal_provider.name";

    /**
     * Creates an access manager configuration object from the
     * given bean configuration.
     *
     * @param config bean configuration
     */
    public LoginModuleConfig(BeanConfig config) {
        super(config);
        setValidate(false); // JCR-1920
    }

    public LoginModule getLoginModule() throws ConfigurationException {
        return newInstance(LoginModule.class);
    }
}
