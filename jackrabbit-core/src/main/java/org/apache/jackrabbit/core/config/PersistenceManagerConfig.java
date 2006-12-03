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
 * Persistence manager configuration. This bean configuration class
 * is used to create configured persistence manager objects.
 * <p>
 * This class is currently only used to assign a static type to
 * more generic bean configuration information.
 *
 * @see WorkspaceConfig#getPersistenceManagerConfig()
 */
public class PersistenceManagerConfig extends BeanConfig {

    /**
     * Creates a persistence manager configuration object from the
     * given bean configuration.
     *
     * @param config bean configuration
     */
    public PersistenceManagerConfig(BeanConfig config) {
        super(config);
    }

}
