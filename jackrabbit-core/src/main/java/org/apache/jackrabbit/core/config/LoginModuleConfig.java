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
     * Creates an access manager configuration object from the
     * given bean configuration.
     *
     * @param config bean configuration
     */
    public LoginModuleConfig(BeanConfig config) {
        super(config);
    }

    public LoginModule getLoginModule() throws ConfigurationException {
        Object result = newInstance();
        if (result instanceof LoginModule) {
            return (LoginModule) result;
        } else {
            throw new ConfigurationException("Invalid login module implementation class "
                    + getClassName() + ".");
        }
    }
}
