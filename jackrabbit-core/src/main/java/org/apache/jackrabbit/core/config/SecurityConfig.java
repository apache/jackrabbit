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

/**
 * Security configuration. This encapsulates the security related sub
 * configurations {@link AccessManagerConfig} and {@link LoginModuleConfig}.
 */
public class SecurityConfig {

    /**
     * Repository name for a JAAS app-entry configuration.
     */
    private final String name;

    /**
     * Repository security manager configuration;
     */
    private final SecurityManagerConfig smc;

    /**
     * Repository access manager configuration;
     */
    private final AccessManagerConfig amc;

    /**
     * Repository login module configuration. Optional, can be <code>null</code>
     */
    private final LoginModuleConfig lmc;

    /**
     * Creates a new security configuration.
     *
     * @param name repository name for a JAAS app-entry configuration
     * @param smc security manager configuration
     * @param amc access manager configuration
     * @param lmc login module configuration (can be <code>null</code>)
     */
    public SecurityConfig(
            String name,
            SecurityManagerConfig smc,
            AccessManagerConfig amc, LoginModuleConfig lmc) {
        this.name = name;
        this.smc = smc;
        this.amc = amc;
        this.lmc = lmc;
    }

    /**
     * Returns the repository name. The repository name can be used for
     * JAAS app-entry configuration.
     *
     * @return repository name
     */
    public String getAppName() {
        return name;
    }

    /**
     * Returns the repository security manager configuration.
     *
     * @return access manager configuration
     */
    public SecurityManagerConfig getSecurityManagerConfig() {
        return smc;
    }

    /**
     * Returns the repository access manager configuration.
     *
     * @return access manager configuration
     */
    public AccessManagerConfig getAccessManagerConfig() {
        return amc;
    }

    /**
     * Returns the repository login module configuration.
     *
     * @return login module configuration, or <code>null</code> if standard
     *         JAAS mechanism should be used.
     */
    public LoginModuleConfig getLoginModuleConfig() {
        return lmc;
    }

}
