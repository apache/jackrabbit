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

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.RepositoryServiceFactory;
import org.apache.jackrabbit.spi.commons.logging.LogWriterProvider;
import org.apache.jackrabbit.spi.commons.logging.SpiLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of {@link RepositoryFactory} is capable of returning the various
 * SPI implementations of the Apache Jackrabbit project:
 * <ul>
 * <li>SPI2DAVex (see jackrabbit-spi2dav module)</li>
 * <li>SPI2DAV (see jackrabbit-spi2dav module)</li>
 * <li>SPI2JCR (see jackrabbit-spi2jcr module)</li>
 * </ul>
 */
public class Jcr2spiRepositoryFactory implements RepositoryFactory {
    static final Logger log = LoggerFactory.getLogger(Jcr2spiRepositoryFactory.class);

    /**
     * This parameter determines the {@link RepositoryServiceFactory} to create the
     * {@link RepositoryService}. This is either an instance of <code>RepositoryServiceFactory
     * </code> or a fully qualified class name of a <code>RepositoryServiceFactory</code>
     * having a no argument constructor.
     */
    public static final String PARAM_REPOSITORY_SERVICE_FACTORY = "org.apache.jackrabbit.spi.RepositoryServiceFactory";

    /**
     * This parameter contains the {@link RepositoryConfig} instance.
     */
    public static final String PARAM_REPOSITORY_CONFIG = "org.apache.jackrabbit.jcr2spi.RepositoryConfig";

    /**
     * Optional configuration parameter for {@link RepositoryConfig#getCacheBehaviour()}. This
     * must be either {@link CacheBehaviour#INVALIDATE} or {@link CacheBehaviour#OBSERVATION}
     * or one of the strings "invalidate" or "observation".
     */
    public static final String PARAM_CACHE_BEHAVIOR = "org.apache.jackrabbit.jcr2spi.CacheBehaviour";

    /**
     * Default value for {@link #PARAM_CACHE_BEHAVIOR}
     */
    public static final CacheBehaviour DEFAULT_CACHE_BEHAVIOR = CacheBehaviour.INVALIDATE;

    /**
     * Optional configuration parameter for the {@link RepositoryConfig#getItemCacheSize()}. This
     * must be either an <code>Integer</code> or a String which parses into an integer.
     */
    public static final String PARAM_ITEM_CACHE_SIZE = "org.apache.jackrabbit.jcr2spi.ItemCacheSize";

    /**
     * Default value for {@link #PARAM_ITEM_CACHE_SIZE}
     */
    public static final int DEFAULT_ITEM_CACHE_SIZE = 5000;

    /**
     * Optional configuration parameter for the {@link RepositoryConfig#getPollTimeout()}. This
     * must be either an <code>Integer</code> or a String which parses into an integer.
     */
    public static final String PARAM_POLL_TIME_OUT = "org.apache.jackrabbit.jcr2spi.PollTimeOut";

    /**
     * Default value for {@link #PARAM_POLL_TIME_OUT}
     */
    public static final int DEFAULT_POLL_TIME_OUT = 3000; // milli seconds

    /**
     * LogWriterProvider configuration parameter: If the parameter is present the
     * <code>RepositoryService</code> defined by the specified
     * <code>RepositoryConfig</code> will be wrapped by calling
     * {@link SpiLoggerFactory#create(org.apache.jackrabbit.spi.RepositoryService, org.apache.jackrabbit.spi.commons.logging.LogWriterProvider) }
     * if the parameter value is an instance of <code>LogWriterProvider</code> or
     * {@link SpiLoggerFactory#create(org.apache.jackrabbit.spi.RepositoryService)}
     * otherwise.
     *
     * @see SpiLoggerFactory#create(org.apache.jackrabbit.spi.RepositoryService)
     * @see SpiLoggerFactory#create(org.apache.jackrabbit.spi.RepositoryService, org.apache.jackrabbit.spi.commons.logging.LogWriterProvider)
     */
    public static final String PARAM_LOG_WRITER_PROVIDER = "org.apache.jackrabbit.spi.commons.logging.LogWriterProvider";

    /**
     * <p>Creates a SPI based <code>Repository</code> instance based on the
     * <code>parameters</code> passed.</p>
     *
     * <p>If the {@link #PARAM_REPOSITORY_SERVICE_FACTORY} parameter is set,
     * the specified {@link RepositoryServiceFactory} is used to create the
     * {@link RepositoryService} instance. All parameters are passed to
     * {@link RepositoryServiceFactory#createRepositoryService(Map)}.</p>
     *
     * <p>If the {@link #PARAM_REPOSITORY_CONFIG} parameter is set, the
     * specified {@link RepositoryConfig} instance is used to create the
     * repository.</p>
     *
     * <p>If both parameters are set, the latter takes precedence and the
     * former is ignores.</p>
     *
     * <p>The known SPI implementations and its <code>RepositoryServiceFactory</code>s are:
     * <ul>
     * <li>SPI2DAVex (see jackrabbit-spi2dav module): <code>Spi2davRepositoryServiceFactory</code></li>
     * <li>SPI2DAV (see jackrabbit-spi2dav module): <code>Spi2davexRepositoryServiceFactory</code></li>
     * <li>SPI2JCR (see jackrabbit-spi2jcr module) <code>Spi2jcrRepositoryServiceFactory</code></li>
     * </ul>
     * <p>
     * NOTE: If the <code>parameters</code> map contains an
     * {@link #PARAM_LOG_WRITER_PROVIDER} entry the
     * {@link org.apache.jackrabbit.spi.RepositoryService RepositoryService} obtained
     * from the configuration is wrapped by a SPI logger. See the
     * {@link org.apache.jackrabbit.spi.commons.logging.SpiLoggerFactory SpiLoggerFactory}
     * for details.
     *
     * @see RepositoryFactory#getRepository(java.util.Map)
     */
    public Repository getRepository(@SuppressWarnings("unchecked") Map parameters) throws RepositoryException {
        RepositoryServiceFactory serviceFactory = getServiceFactory(parameters);
        Object configParam = parameters.get(PARAM_REPOSITORY_CONFIG);

        if (serviceFactory == null && configParam == null) {
            return null;
        }

        RepositoryConfig config;
        if (configParam instanceof RepositoryConfig) {
            config = (RepositoryConfig) configParam;
            if (serviceFactory != null) {
                log.warn("Ignoring {} since {} was specified", PARAM_REPOSITORY_SERVICE_FACTORY,
                        PARAM_REPOSITORY_CONFIG);
            }
        } else {
            if (serviceFactory == null) {
                return null;
            } else {
                config = new RepositoryConfigImpl(serviceFactory, parameters);
            }
        }

        config = SpiLoggerConfig.wrap(config, parameters);
        return RepositoryImpl.create(config);
    }

    // -----------------------------------------------------< private >---

    private static RepositoryServiceFactory getServiceFactory(Map<?, ?> parameters)
            throws RepositoryException {

        Object serviceFactoryParam = parameters.get(PARAM_REPOSITORY_SERVICE_FACTORY);
        if (serviceFactoryParam == null) {
            return null;
        }

        log.debug("Acquiring RepositoryServiceFactory from {}", PARAM_REPOSITORY_SERVICE_FACTORY);

        if (serviceFactoryParam instanceof RepositoryServiceFactory) {
            log.debug("Found RepositoryServiceFactory {}", serviceFactoryParam);
            return (RepositoryServiceFactory) serviceFactoryParam;
        } else if (serviceFactoryParam instanceof String) {
            String serviceFactoryName = (String) serviceFactoryParam;
            log.debug("Found RepositoryServiceFactory class name {}", serviceFactoryName);
            try {
                Class<?> serviceFactoryClass;
                try {
                    serviceFactoryClass = Class.forName(serviceFactoryName, true,
                            Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    // Backup for OSGi
                    serviceFactoryClass = Class.forName(serviceFactoryName);
                }

                Object serviceFactory = serviceFactoryClass.newInstance();

                if (serviceFactory instanceof RepositoryServiceFactory) {
                    log.debug("Found RepositoryServiceFactory {}", serviceFactory);
                    return (RepositoryServiceFactory) serviceFactory;
                } else {
                    String msg = "Error acquiring RepositoryServiceFactory " + serviceFactoryParam;
                    log.error(msg);
                    throw new RepositoryException(msg);
                }
            } catch (Exception e) {
                String msg = "Error acquiring RepositoryServiceFactory";
                log.error(msg, e);
                throw new RepositoryException(msg, e);
            }
        } else {
            String msg = "Error acquiring RepositoryServiceFactory from " + serviceFactoryParam;
            log.error(msg);
            throw new RepositoryException(msg);
        }
    }

    public static class RepositoryConfigImpl implements RepositoryConfig {
        private final RepositoryServiceFactory serviceFactory;
        private final CacheBehaviour cacheBehaviour;
        private final int itemCacheSize;
        private final int pollTimeOut;
        private final Map<?, ?> parameters;
        private RepositoryService repositoryService;

        public RepositoryConfigImpl(RepositoryServiceFactory serviceFactory, Map<?, ?> parameters)
                throws RepositoryException {

            super();
            this.serviceFactory = serviceFactory;
            this.cacheBehaviour = getCacheBehaviour(parameters);
            this.itemCacheSize = getItemCacheSize(parameters);
            this.pollTimeOut = getPollTimeout(parameters);
            this.parameters = parameters;
        }

        public CacheBehaviour getCacheBehaviour() {
            return cacheBehaviour;
        }

        public int getItemCacheSize() {
            return itemCacheSize;
        }

        public int getPollTimeout() {
            return pollTimeOut;
        }

        @Override
        public <T> T getConfiguration(String name, T defaultValue) {
            if (parameters.containsKey(name)) {
                Object value = parameters.get(name);
                Class clazz = (defaultValue == null)
                        ? value.getClass()
                        : defaultValue.getClass();
                if (clazz.isAssignableFrom(value.getClass())) {
                    return (T) value;
                }
            }
            return defaultValue;
        }

        public RepositoryService getRepositoryService() throws RepositoryException {
            if (repositoryService == null) {
                repositoryService = serviceFactory.createRepositoryService(parameters);
            }
            return repositoryService;
        }

        // -----------------------------------------------------< private >---

        private static CacheBehaviour getCacheBehaviour(Map<?, ?> parameters) throws RepositoryException {
            Object paramCacheBehaviour = parameters.get(PARAM_CACHE_BEHAVIOR);
            log.debug("Setting CacheBehaviour from {}", PARAM_CACHE_BEHAVIOR);

            if (paramCacheBehaviour == null) {
                log.debug("{} not set, defaulting to {}", PARAM_CACHE_BEHAVIOR, DEFAULT_CACHE_BEHAVIOR);
                return DEFAULT_CACHE_BEHAVIOR;
            } else if (paramCacheBehaviour instanceof CacheBehaviour) {
                log.debug("Setting CacheBehaviour to {}", paramCacheBehaviour);
                return (CacheBehaviour) paramCacheBehaviour;
            } else if (paramCacheBehaviour instanceof String) {
                String cacheBehaviour = (String) paramCacheBehaviour;
                if ("invalidate".equals(cacheBehaviour)) {
                    log.debug("Setting CacheBehaviour to {}", CacheBehaviour.INVALIDATE);
                    return CacheBehaviour.INVALIDATE;
                } else if ("observation".equals(cacheBehaviour)) {
                    log.debug("Setting CacheBehaviour to {}", CacheBehaviour.OBSERVATION);
                    return CacheBehaviour.OBSERVATION;
                } else {
                    log.error("Invalid valid for CacheBehaviour: {} {}", PARAM_CACHE_BEHAVIOR, cacheBehaviour);
                    throw new RepositoryException("Invalid value for CacheBehaviour: " + cacheBehaviour);
                }
            } else {
                String msg = "Invalid value for CacheBehaviour: " + paramCacheBehaviour;
                log.error(msg);
                throw new RepositoryException(msg);
            }
        }

        private static int getItemCacheSize(Map<?, ?> parameters) throws RepositoryException {
            Object paramItemCacheSize = parameters.get(PARAM_ITEM_CACHE_SIZE);
            log.debug("Setting ItemCacheSize from {}", PARAM_ITEM_CACHE_SIZE);

            if (paramItemCacheSize == null) {
                log.debug("{} not set, defaulting to {}", PARAM_ITEM_CACHE_SIZE, DEFAULT_ITEM_CACHE_SIZE);
                return DEFAULT_ITEM_CACHE_SIZE;
            } else if (paramItemCacheSize instanceof Integer) {
                log.debug("Setting ItemCacheSize to {}", paramItemCacheSize);
                return (Integer) paramItemCacheSize;
            } else if (paramItemCacheSize instanceof String) {
                try {
                    log.debug("Setting ItemCacheSize to {}", paramItemCacheSize);
                    return Integer.parseInt((String) paramItemCacheSize);
                } catch (NumberFormatException e) {
                    String msg = "Invalid value for ItemCacheSize: " + paramItemCacheSize;
                    log.error(msg);
                    throw new RepositoryException(msg, e);
                }
            } else {
                String msg = "Invalid value for ItemCacheSize: " + paramItemCacheSize;
                log.error(msg);
                throw new RepositoryException(msg);
            }
        }

        private static int getPollTimeout(Map<?, ?> parameters) throws RepositoryException {
            Object paramPollTimeOut = parameters.get(PARAM_POLL_TIME_OUT);
            log.debug("Setting PollTimeout from {}", PARAM_POLL_TIME_OUT);

            if (paramPollTimeOut == null) {
                log.debug("{} not set, defaulting to {}", PARAM_POLL_TIME_OUT, DEFAULT_POLL_TIME_OUT);
                return DEFAULT_POLL_TIME_OUT;
            } else if (paramPollTimeOut instanceof Integer) {
                log.debug("Setting PollTimeout to {}", paramPollTimeOut);
                return (Integer) paramPollTimeOut;
            } else if (paramPollTimeOut instanceof String) {
                try {
                    log.debug("Setting PollTimeout to {}", paramPollTimeOut);
                    return Integer.parseInt((String) paramPollTimeOut);
                } catch (NumberFormatException e) {
                    String msg = "Invalid value for PollTimeout: " + paramPollTimeOut;
                    log.error(msg);
                    throw new RepositoryException(msg, e);
                }
            } else {
                String msg = "Invalid value for PollTimeout: " + paramPollTimeOut;
                log.error(msg);
                throw new RepositoryException(msg);
            }
        }

    }

    private static class SpiLoggerConfig implements RepositoryConfig {
        private final RepositoryConfig config;
        private final RepositoryService service;

        private SpiLoggerConfig(RepositoryConfig config, Map<?, ?> parameters) throws RepositoryException {
            super();
            this.config = config;

            Object lwProvider = parameters.get(PARAM_LOG_WRITER_PROVIDER);
            if (lwProvider instanceof LogWriterProvider) {
                service = SpiLoggerFactory.create(config.getRepositoryService(), (LogWriterProvider) lwProvider);
            } else {
                service = SpiLoggerFactory.create(config.getRepositoryService());
            }
        }

        public static RepositoryConfig wrap(RepositoryConfig config, Map<?, ?> parameters)
                throws RepositoryException {

            if (config == null || parameters == null || !parameters.containsKey(PARAM_LOG_WRITER_PROVIDER)) {
                return config;
            } else {
                return new SpiLoggerConfig(config, parameters);
            }
        }

        public CacheBehaviour getCacheBehaviour() {
            return config.getCacheBehaviour();
        }

        public int getItemCacheSize() {
            return config.getItemCacheSize();
        }

        public int getPollTimeout() {
            return config.getPollTimeout();
        }

        @Override
        public <T> T getConfiguration(String name, T defaultValue) {
            return config.getConfiguration(name, defaultValue);
        }

        public RepositoryService getRepositoryService() throws RepositoryException {
            return service;
        }

    }

}
