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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractRepositoryConfig</code>...
 */
public abstract class AbstractRepositoryConfig implements RepositoryConfig {

    private static Logger log = LoggerFactory.getLogger(AbstractRepositoryConfig.class);

    private static final int DEFAULT_ITEM_CACHE_SIZE = 5000;
    private static final int DEFAULT_INFO_CACHE_SIZE = 5000;
    private static final int DEFAULT_POLL_TIMEOUT = 3000; // 3 seconds

    public CacheBehaviour getCacheBehaviour() {
        return CacheBehaviour.INVALIDATE;
    }

    public int getItemCacheSize() {
        return DEFAULT_ITEM_CACHE_SIZE;
    }

    public int getPollTimeout() {
        return DEFAULT_POLL_TIMEOUT;
    }

    @Override
    public <T> T getConfiguration(String name, T defaultValue) {
        return null;
    }

    public int getInfoCacheSize() {
        return DEFAULT_INFO_CACHE_SIZE;
    }

}
