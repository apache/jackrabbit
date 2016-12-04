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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security manager configuration. This bean configuration class
 * is used to create configured security manager objects.
 * <p>
 * This class is currently only used to assign a static type to
 * more generic bean configuration information.
 *
 * @see SecurityConfig#getSecurityManagerConfig()
 */
public class SecurityManagerConfig extends BeanConfig {

    /**
     * the default logger
     */
    private static final Logger log = LoggerFactory.getLogger(SecurityManagerConfig.class);

    private final String workspaceName;
    private final BeanConfig workspaceAccessConfig;
    private final UserManagerConfig userManagerConfig;

    /**
     * Optional class used to retrieve userID from the subject.
     */
    private final Class uidClass;

    /**
     * Creates an security manager configuration object from the
     * given bean configuration.
     *
     * @param config bean configuration
     * @param workspaceName the security workspace name
     * @param workspaceAccessConfig the configuration for the workspace access.
     */
    public SecurityManagerConfig(BeanConfig config, String workspaceName,
                                 BeanConfig workspaceAccessConfig) {
        this(config, workspaceName, workspaceAccessConfig, null, null);
    }

    /**
     * Creates an security manager configuration object from the
     * given bean configuration.
     *
     * @param config bean configuration
     * @param workspaceName the security workspace name
     * @param workspaceAccessConfig the configuration for the workspace access.
     * @param userManagerConfig Configuration options for the user manager.
     */
    public SecurityManagerConfig(BeanConfig config, String workspaceName,
                                 BeanConfig workspaceAccessConfig,
                                 UserManagerConfig userManagerConfig,
                                 BeanConfig uidClassConfig) {
        super(config);
        this.workspaceName = workspaceName;
        this.workspaceAccessConfig = workspaceAccessConfig;
        this.userManagerConfig = userManagerConfig;
        Class cl = null;
        if (uidClassConfig != null) {
            try {
                cl = Class.forName(uidClassConfig.getClassName(), true, uidClassConfig.getClassLoader());
            } catch (ClassNotFoundException e) {
                log.error("Configured bean implementation class " + uidClassConfig.getClassName() + " was not found -> Ignoring UserIdClass element.", e);
            }
        }
        this.uidClass = cl;
    }

    /**
     * Returns the name of the 'workspaceName' attribute or <code>null</code>
     * if the SecurityManager does not require an extra workspace.
     *
     * @return
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * @return the configuration for the <code>WorkspaceAccessManager</code>.
     * May be <code>null</code> if the configuration entry is missing (i.e.
     * the system default should be used).
     */
    public BeanConfig getWorkspaceAccessConfig() {
        return workspaceAccessConfig;
    }

    /**
     * @return the configuration for the user manager.
     * May be <code>null</code> if the configuration entry is missing (i.e.
     * the system default should be used).
     */
    public UserManagerConfig getUserManagerConfig() {
        return userManagerConfig;
    }
    
    /**
     * @return Class which is used to retrieve the UserID from the Subject.
     * @see org.apache.jackrabbit.core.security.JackrabbitSecurityManager#getUserID(javax.security.auth.Subject, String) 
     * @see javax.security.auth.Subject#getPrincipals(Class)
     */
    public Class getUserIdClass() {
        return uidClass;
    }
}
