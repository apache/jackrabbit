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
package org.apache.jackrabbit.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;

import java.util.Map;

/** <code>AbstractRepositoryConfig</code>... */
public abstract class AbstractRepositoryConfig implements RepositoryConfig {

    private static Logger log = LoggerFactory.getLogger(AbstractRepositoryConfig.class);

    public static final String REPOSITORY_ITEM_CACHE_SIZE = "org.apache.jackrabbit.repository.itemCacheSize";
    public static final String REPOSITORY_CACHEBEHAVIOUR = "org.apache.jackrabbit.repository.cacheBehaviour";
    public static final String REPOSITORY_POLL_TIMEOUT = "org.apache.jackrabbit.repository.pollTimeout";

    public static final int DEFAULT_ITEM_CACHE_SIZE = 5000;
    public static final int DEFAULT_POLL_TIMEOUT = 1000; // ms

    private final CacheBehaviour cacheBehaviour;
    private final int itemCacheSize;
    private final int pollTimeout;

    /**
     * Create a new <code>AbstractRepositoryConfig</code>.
     *
     * @param parameters Map of parameters used to create the RepositoryConfiguration.
     * The following parameters are respected:
     * <ul>
     * <li>{@link #REPOSITORY_CACHEBEHAVIOUR}</li>
     * <li>{@link #REPOSITORY_ITEM_CACHE_SIZE}</li>
     * <li>{@link #REPOSITORY_POLL_TIMEOUT}</li>
     * </ul>
     * Any of the parameters can be omitted in which case the default values
     * are used:
     * <ul>
     * <li>CacheBehaviour: {@link CacheBehaviour#INVALIDATE}</li>
     * <li>item cache size: {@link #DEFAULT_ITEM_CACHE_SIZE}</li>
     * <li>poll time out: {@link #DEFAULT_POLL_TIMEOUT}.</li>
     * </ul>
     */
    protected AbstractRepositoryConfig(Map parameters) {
        if (parameters == null) {
            cacheBehaviour = CacheBehaviour.INVALIDATE;
            itemCacheSize = DEFAULT_ITEM_CACHE_SIZE;
            pollTimeout = DEFAULT_POLL_TIMEOUT;
        } else {
            int cacheSize = AbstractRepositoryConfig.DEFAULT_ITEM_CACHE_SIZE;
            Object param = parameters.get(REPOSITORY_ITEM_CACHE_SIZE);
            if (param != null) {
                if (param instanceof Integer) {
                    cacheSize = ((Integer) param).intValue();
                } else {
                    try {
                        cacheSize = Integer.parseInt(param.toString());
                    } catch (NumberFormatException e) {
                        // ignore.
                    }
                }
            }

            CacheBehaviour cacheBehaviour = CacheBehaviour.INVALIDATE;
            param = parameters.get(REPOSITORY_CACHEBEHAVIOUR);
            if (param != null && param instanceof CacheBehaviour) {
                cacheBehaviour = (CacheBehaviour) param;
            }

            int pollTimeout = AbstractRepositoryConfig.DEFAULT_POLL_TIMEOUT;
            param = parameters.get(REPOSITORY_POLL_TIMEOUT);
            if (param != null) {
                if (param instanceof Integer) {
                    pollTimeout = ((Integer) param).intValue();
                } else {
                    try {
                        pollTimeout = Integer.parseInt(param.toString());
                    } catch (NumberFormatException e) {
                        // ignore.
                    }
                }
            }
            this.cacheBehaviour = cacheBehaviour;
            this.itemCacheSize = cacheSize;
            this.pollTimeout = pollTimeout;
        }
    }

    /**
     * Create a new <code>AbstractRepositoryConfig</code>.
     *
     * @param cacheBehaviour The desired cache behaviour. Either
     * {@link CacheBehaviour#INVALIDATE} or {@link CacheBehaviour#OBSERVATION}.
     * @param itemCacheSize Integer defining the size of the item cache.
     * @param pollTimeout Integer defining the poll timeout.
     */
    protected AbstractRepositoryConfig(CacheBehaviour cacheBehaviour, int itemCacheSize, int pollTimeout) {
        this.cacheBehaviour = cacheBehaviour;
        this.itemCacheSize = itemCacheSize;
        this.pollTimeout = pollTimeout;
    }

    /**
     * Same as {@link #AbstractRepositoryConfig(CacheBehaviour, int, int)} where
     * <pre>
     * CacheBehaviour is {@link CacheBehaviour#INVALIDATE},
     * item cache size is {@link #DEFAULT_ITEM_CACHE_SIZE} and
     * poll time out is {@link #DEFAULT_POLL_TIMEOUT}.
     * </pre>
     */
    protected AbstractRepositoryConfig() {
        this(CacheBehaviour.INVALIDATE, DEFAULT_ITEM_CACHE_SIZE, DEFAULT_POLL_TIMEOUT);
    }

    //---------------------------------------------------< RepositoryConfig >---
    /**
     * @see RepositoryConfig#getCacheBehaviour()
     */
    public CacheBehaviour getCacheBehaviour() {
        return cacheBehaviour;
    }

    /**
     * @see RepositoryConfig#getItemCacheSize()
     */
    public int getItemCacheSize() {
        return itemCacheSize;
    }

    /**
     * @see RepositoryConfig#getPollTimeout()
     */
    public int getPollTimeout() {
        return pollTimeout;
    }
}
