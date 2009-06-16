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
package org.apache.jackrabbit.client.spi2davex;

import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi2davex.RepositoryServiceImpl;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.client.AbstractRepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import java.util.Map;
import java.util.Collections;

/**
 * <code>RepositoryFactoryImpl</code>...
 */
public class RepositoryConfigImpl extends AbstractRepositoryConfig {

    private static Logger log = LoggerFactory.getLogger(org.apache.jackrabbit.client.spilogger.RepositoryConfigImpl.class);

    /**
     * Default URI for the {@link #REPOSITORY_SPI2DAVEX_URI} configuration
     * parameter.
     */
    public static String DEFAULT_URI = "http://localhost:8080/jackrabbit/server";

    /**
     * Mandatory configuration parameter: It's value is expected to specify the
     * URI of the JCR server implementation. {@link #DEFAULT_URI} is used as
     * fallback if no parameters or uri has been specified and the uri could not
     * been retrieved from system props either.
     * 
     * @see org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet
     */
    public static final String REPOSITORY_SPI2DAVEX_URI = "org.apache.jackrabbit.repository.spi2davex.uri";
    /**
     * Optional batch read configuration parameter: If present it's value is
     * expected to be an instance of {@link BatchReadConfig}
     */
    public static final String REPOSITORY_SPI2DAVEX_BATCHREADCONFIG = "org.apache.jackrabbit.repository.spi2dav.batchreadconfig";

    private final RepositoryService service;

    public static RepositoryConfig create(Map parameters) {
        String uri = getURI(parameters);
        if (uri == null) {
            return null;
        }
        try {
            return new RepositoryConfigImpl(uri, parameters);
        } catch (RepositoryException e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    public static RepositoryConfig create(String uri) {
        if (uri == null) {
            uri = getURI(null);
        }
        if (uri == null) {
            return null;
        }
        try {
            return new RepositoryConfigImpl(uri);
        } catch (RepositoryException e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    private static String getURI(Map parameters) {
        String uri;
        if (parameters != null) {
            uri = (parameters.containsKey(REPOSITORY_SPI2DAVEX_URI) ? parameters.get(REPOSITORY_SPI2DAVEX_URI).toString() : null);
        } else {
            uri = System.getProperty(REPOSITORY_SPI2DAVEX_URI);
            if (uri == null) {
                log.debug("Missing repository uri -> using default " + DEFAULT_URI);
                uri = DEFAULT_URI;
            }
        }
        return uri;
    }

    private RepositoryConfigImpl(String uri) throws RepositoryException {
        super();
        service = createService(uri, Collections.EMPTY_MAP);
    }

    private RepositoryConfigImpl(String uri, Map parameters) throws RepositoryException {
        super(parameters);
        service = createService(uri, parameters);
    }

    public RepositoryConfigImpl(String uri, CacheBehaviour cacheBehaviour, int itemCacheSize) throws RepositoryException {
        this(uri, cacheBehaviour, itemCacheSize, DEFAULT_POLL_TIMEOUT);
    }

    public RepositoryConfigImpl(String uri, CacheBehaviour cacheBehaviour, int itemCacheSize, int pollTimeout) throws RepositoryException {
        super(cacheBehaviour, itemCacheSize, pollTimeout);
        service = createService(uri, Collections.EMPTY_MAP);
    }

    private static RepositoryService createService(String uri, Map parameters) throws RepositoryException {
        BatchReadConfig brc = null;
        if (parameters != null) {
            Object param = parameters.get(REPOSITORY_SPI2DAVEX_BATCHREADCONFIG);
            if (param != null && param instanceof BatchReadConfig) {
                brc = (BatchReadConfig) param;
            }
        }
        if (brc == null) {
            brc = new BatchReadConfig() {
                public int getDepth(Path path, PathResolver pathResolver) throws NamespaceException {
                    return 4;
                }
            };
        }
        return new RepositoryServiceImpl(uri, brc);
    }

    public RepositoryService getRepositoryService() throws RepositoryException {
        return service;
    }
}