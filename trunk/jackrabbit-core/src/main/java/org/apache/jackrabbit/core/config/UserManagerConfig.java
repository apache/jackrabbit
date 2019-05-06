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

import org.apache.jackrabbit.core.security.user.action.AuthorizableAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.api.security.user.UserManager;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * User manager configuration. This bean configuration class is used to
 * create user manager objects.
 * <p>
 * This configuration is an optional part of the SecurityManager configuration.
 *
 * @see org.apache.jackrabbit.core.config.SecurityManagerConfig#getUserManagerConfig()
 */
public class UserManagerConfig extends BeanConfig {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(UserManagerConfig.class);

    private Constructor<?> constr;

    private final BeanConfig[] actionConfig;

    public UserManagerConfig(BeanConfig config) {
        this(config, null);
    }

    public UserManagerConfig(BeanConfig config, BeanConfig[] actionConfig) {
        super(config);
        setValidate(false); // omit validation of the config properties
        this.actionConfig = actionConfig;
    }

    /**
     * Build a new <code>UserManager</code> instance based on this configuration.
     * Since the initial requirement for the User Management was to allow for
     * implementations that don't store and retrieve user information from
     * repository content, the otherwise used <code>init</code> interface method
     * has intentionally be omitted. This method attempts to retrieve a
     * constructor matching the given <code>parameterTypes</code> and creates
     * an new instance from the <code>initArgs</code> matching the
     * <code>parameterTypes</code>.
     * 
     * @param assignableFrom An UserManager class from which the configured
     * implementation must be assignable.
     * @param parameterTypes Array of classes used to lookup the constructor.
     * @param initArgs The arguments to create the new user manager instance
     * matching the <code>parameterTypes</code>.
     * @return A new instance of <code>UserManager</code> that is assignable from
     * the class passed as <code>assignableFrom</code>.
     * @throws ConfigurationException If the configured user manager implementation
     * is not assignable from the given UserManager class, does not provide
     * a constructor matching <code>parameterTypes</code> or creating the instance
     * fails.
     */
    public UserManager getUserManager(Class<? extends UserManager> assignableFrom, Class<?>[] parameterTypes, Object... initArgs) throws ConfigurationException {
        if (constr == null) {
            String msg = "Invalid UserManager implementation '" + getClassName() + "'.";
            try {
                Class<?> umgrCl = Class.forName(getClassName(), true, getClassLoader());
                if (assignableFrom.isAssignableFrom(umgrCl)) {
                    constr = umgrCl.getConstructor(parameterTypes);
                } else {
                    throw new ConfigurationException("Configured UserManager '" + getClassName() + "' is not assignable from " + assignableFrom);
                }
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException(msg, e);
            } catch (NoSuchMethodException e) {
                throw new ConfigurationException(msg, e);
            }
        }

        try {
            return (UserManager) constr.newInstance(initArgs);
        } catch (Exception e) {
            throw new ConfigurationException("Invalid UserManager implementation '" + getClassName() + "'.", e);
        }
    }

    public AuthorizableAction[] getAuthorizableActions() throws ConfigurationException {
        if (actionConfig == null || actionConfig.length == 0) {
            return new AuthorizableAction[0];
        } else {
            List<AuthorizableAction> actions = new ArrayList<AuthorizableAction>(actionConfig.length);
            for (BeanConfig c : actionConfig) {
                AuthorizableAction action = c.newInstance(AuthorizableAction.class);
                actions.add(action);
            }
            return actions.toArray(new AuthorizableAction[actions.size()]);
        }
    }
}