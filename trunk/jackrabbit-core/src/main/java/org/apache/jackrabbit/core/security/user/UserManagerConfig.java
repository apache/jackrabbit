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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.core.security.user.action.AuthorizableAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Utility to retrieve configuration parameters for UserManagerImpl
 */
class UserManagerConfig {

    private static final Logger log = LoggerFactory.getLogger(UserManagerConfig.class);

    private final Properties config;
    private final String adminId;
    /**
     * Authorizable actions that will all be executed upon creation and removal
     * of authorizables in the order they are contained in the array.
     * <p>
     * Note, that if {@link #isAutoSave() autosave} is turned on, the configured
     * actions are executed before persisting the creation or removal.
     */
    private AuthorizableAction[] actions;

    UserManagerConfig(Properties config, String adminId, AuthorizableAction[] actions) {
        this.config = config;
        this.adminId = adminId;
        this.actions = (actions == null) ? new AuthorizableAction[0] : actions;
    }

    public <T> T getConfigValue(String key, T defaultValue) {
        if (config != null && config.containsKey(key)) {
            return convert(config.get(key), defaultValue);
        } else {
            return defaultValue;
        }
    }

    public String getAdminId() {
        return adminId;
    }

    public AuthorizableAction[] getAuthorizableActions() {
        return actions;
    }

    public void setAuthorizableActions(AuthorizableAction[] actions) {
        if (actions != null) {
            this.actions = actions;
        }
    }

    //--------------------------------------------------------< private >---
    private <T> T convert(Object v, T defaultValue) {
        if (v == null) {
            return null;
        }

        T value;
        String str = v.toString();
        Class targetClass = (defaultValue == null) ? String.class : defaultValue.getClass();
        try {
            if (targetClass == String.class) {
                value = (T) str;
            } else if (targetClass == Integer.class) {
                value = (T) Integer.valueOf(str);
            } else if (targetClass == Long.class) {
                value = (T) Long.valueOf(str);
            } else if (targetClass == Double.class) {
                value = (T) Double.valueOf(str);
            } else if (targetClass == Boolean.class) {
                value = (T) Boolean.valueOf(str);
            } else {
                // unsupported target type
                log.warn("Unsupported target type {} for value {}", targetClass.getName(), v);
                throw new IllegalArgumentException("Cannot convert config entry " + v + " to " + targetClass.getName());
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid value {}; cannot be parsed into {}", v, targetClass.getName());
            value = defaultValue;
        }

        return value;
    }
}
