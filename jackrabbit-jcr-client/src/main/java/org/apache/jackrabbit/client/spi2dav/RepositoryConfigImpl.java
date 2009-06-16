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
package org.apache.jackrabbit.client.spi2dav;

import org.apache.jackrabbit.client.AbstractRepositoryConfig;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.spi2dav.RepositoryServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Map;

/**
 * <code>RepositoryFactoryImpl</code>...
 */
public class RepositoryConfigImpl extends AbstractRepositoryConfig {

    private static Logger log = LoggerFactory.getLogger(org.apache.jackrabbit.client.spilogger.RepositoryConfigImpl.class);

    /**
     * Mandatory configuration parameter: It's value is expected to specify the
     * URI of the JCR server implementation.
     *
     * @see org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet
     */
    public static final String REPOSITORY_SPI2DAV_URI = "org.apache.jackrabbit.repository.spi2dav.uri";
    /**
     * Optional configuration parameter: It's value is expected to be an instance
     * of {@link IdFactory}. If missing {@link IdFactoryImpl} is used.
     */
    public static final String REPOSITORY_SPI2DAV_IDFACTORY = "org.apache.jackrabbit.repository.spi2dav.idfactory";
    /**
     * Optional configuration parameter: It's value is expected to be an instance
     * of {@link NameFactory}. If missing {@link NameFactoryImpl} is used.
     */
    public static final String REPOSITORY_SPI2DAV_NAMEFACTORY = "org.apache.jackrabbit.repository.spi2dav.namefactory";
    /**
     * Optional configuration parameter: It's value is expected to be an instance
     * of {@link PathFactory}. If missing {@link PathFactoryImpl} is used.
     */
    public static final String REPOSITORY_SPI2DAV_PATHFACTORY = "org.apache.jackrabbit.repository.spi2dav.pathfactory";
    /**
     * Optional configuration parameter: It's value is expected to be an instance
     * of {@link QValueFactory}. If missing {@link QValueFactoryImpl} is used.
     */
    public static final String REPOSITORY_SPI2DAV_VALUEFACTORY = "org.apache.jackrabbit.repository.spi2dav.valuefactory";

    private final RepositoryService service;

    public static RepositoryConfig create(Map parameters) {
        if (!parameters.containsKey(REPOSITORY_SPI2DAV_URI)) {
            return null;
        }

        String uri = parameters.get(REPOSITORY_SPI2DAV_URI).toString();
        try {
            return new RepositoryConfigImpl(uri, parameters);
        } catch (RepositoryException e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    public static RepositoryConfig create(String uri) {
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

    private RepositoryConfigImpl(String uri) throws RepositoryException {
        super();
        service = createService(uri, Collections.EMPTY_MAP);
    }

    private RepositoryConfigImpl(String uri, Map parameters) throws RepositoryException {
        super(parameters);
        service = createService(uri, parameters);
    }

    private static RepositoryService createService(String uri, Map parameters) throws RepositoryException {
        IdFactory idFactory;
        Object param = parameters.get(REPOSITORY_SPI2DAV_IDFACTORY);
        if (param != null && param instanceof IdFactory) {
            idFactory = (IdFactory) param;
        } else {
            idFactory = IdFactoryImpl.getInstance();
        }

        NameFactory nameFactory;
        param = parameters.get(REPOSITORY_SPI2DAV_NAMEFACTORY);
        if (param != null && param instanceof NameFactory) {
            nameFactory = (NameFactory) param;
        } else {
            nameFactory = NameFactoryImpl.getInstance();
        }

        PathFactory pathFactory;
        param = parameters.get(REPOSITORY_SPI2DAV_PATHFACTORY);
        if (param != null && param instanceof PathFactory) {
            pathFactory = (PathFactory) param;
        } else {
            pathFactory = PathFactoryImpl.getInstance();
        }

        QValueFactory vFactory;
        param = parameters.get(REPOSITORY_SPI2DAV_VALUEFACTORY);
        if (param != null && param instanceof QValueFactory) {
            vFactory = (QValueFactory) param;
        } else {
            vFactory = QValueFactoryImpl.getInstance();
        }
        return new RepositoryServiceImpl(uri, idFactory, nameFactory, pathFactory, vFactory);
    }

    public RepositoryService getRepositoryService() throws RepositoryException {
        return service;
    }
}
