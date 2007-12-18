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

import org.apache.jackrabbit.core.state.ISMLocking;

import java.util.Properties;

/**
 * ItemStateManager locking configuration. This bean configuration class
 * is used to create configured ISMLocking objects.
 *
 * @see WorkspaceConfig#getISMLockingConfig()
 * @see VersioningConfig#getISMLockingConfig()
 */
public class ISMLockingConfig extends BeanConfig {

    /**
     * The default ism locking implementation class.
     */
    private static final String DEFAULT_ISM_LOCKING_CLASS
            = "org.apache.jackrabbit.core.state.DefaultISMLocking";

    /**
     * @return a ISM locking configuration with default values.
     */
    public static ISMLockingConfig createDefaultConfig() {
        return new ISMLockingConfig(DEFAULT_ISM_LOCKING_CLASS, new Properties());
    }

    /**
     * Creates a new ISM locking configuration.
     *
     * @param className  the class name of the ISM locking implementation.
     * @param parameters configuration parameters.
     */
    public ISMLockingConfig(String className, Properties parameters) {
        super(className, parameters);
    }

    /**
     * @return a new ISMLocking instance based on this configuration.
     * @throws ConfigurationException on bean configuration errors.
     */
    public ISMLocking createISMLocking() throws ConfigurationException {
        return (ISMLocking) newInstance();
    }
}
