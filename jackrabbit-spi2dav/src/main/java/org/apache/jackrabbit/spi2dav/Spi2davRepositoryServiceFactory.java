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
package org.apache.jackrabbit.spi2dav;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.RepositoryServiceFactory;
import org.apache.jackrabbit.spi.commons.ItemInfoCacheImpl;
import org.apache.jackrabbit.spi.commons.identifier.IdFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;

/**
 * This {@link RepositoryServiceFactory} implementation is responsible
 * for creating {@link RepositoryServiceImpl} instances which communicate via WebDAV.
 * All parameter keys defined in this class and in addition the ones from {@link ConnectionOptions} 
 * are supported as arguments for {@link #createRepositoryService(Map)}.
 */
public class Spi2davRepositoryServiceFactory implements RepositoryServiceFactory {

    /**
     * Mandatory configuration parameter: It's value is expected to specify the
     * URI of the JCR server implementation.
     */
    public static final String PARAM_REPOSITORY_URI = "org.apache.jackrabbit.spi2dav.uri";

    /**
     * Optional configuration parameter: It's value is expected to be an instance
     * of {@link IdFactory}. If missing {@link IdFactoryImpl} is used.
     */
    public static final String PARAM_ID_FACTORY = "org.apache.jackrabbit.spi2dav.IdFactory";

    /**
     * Optional configuration parameter: It's value is expected to be an instance
     * of {@link NameFactory}. If missing {@link NameFactoryImpl} is used.
     */
    public static final String PARAM_NAME_FACTORY = "org.apache.jackrabbit.spi2dav.NameFactory";

    /**
     * Optional configuration parameter: It's value is expected to be an instance
     * of {@link PathFactory}. If missing {@link PathFactoryImpl} is used.
     */
    public static final String PARAM_PATH_FACTORY = "org.apache.jackrabbit.spi2dav.PathFactory";

    /**
     * Optional configuration parameter: It's value is expected to be an instance
     * of {@link QValueFactory}. If missing {@link QValueFactoryImpl} is used.
     */
    public static final String PARAM_QVALUE_FACTORY = "org.apache.jackrabbit.spi2dav.QValueFactory";

    /**
     * Optional configuration parameter: It's value determines the size of the
     * {@link ItemInfoCache} cache. Defaults to {@link ItemInfoCacheImpl#DEFAULT_CACHE_SIZE}.
     */
    public static final String PARAM_ITEMINFO_CACHE_SIZE = "org.apache.jackrabbit.spi2dav.ItemInfoCacheSize";

    /**
     * Optional configuration parameter: It's value defines the
     * maximumConnectionsPerHost value on the HttpClient configuration and
     * must be an int greater than zero.
     * Rather use {@link ConnectionOptions#PARAM_MAX_CONNECTIONS} instead.
     */
    public static final String PARAM_MAX_CONNECTIONS = "org.apache.jackrabbit.spi2dav.MaxConnections";

    public RepositoryService createRepositoryService(Map<?, ?> parameters) throws RepositoryException {
        if (parameters == null) {
            throw new RepositoryException("Parameter " + PARAM_REPOSITORY_URI + " missing");
        }

        String uri;
        if (parameters.get(PARAM_REPOSITORY_URI) == null) {
            throw new RepositoryException("Parameter " + PARAM_REPOSITORY_URI + " missing");
        }
        else {
            uri = parameters.get(PARAM_REPOSITORY_URI).toString();
        }

        IdFactory idFactory;
        Object param = parameters.get(PARAM_ID_FACTORY);
        if (param != null && param instanceof IdFactory) {
            idFactory = (IdFactory) param;
        } else {
            idFactory = IdFactoryImpl.getInstance();
        }

        NameFactory nameFactory;
        param = parameters.get(PARAM_NAME_FACTORY);
        if (param != null && param instanceof NameFactory) {
            nameFactory = (NameFactory) param;
        } else {
            nameFactory = NameFactoryImpl.getInstance();
        }

        PathFactory pathFactory;
        param = parameters.get(PARAM_PATH_FACTORY);
        if (param != null && param instanceof PathFactory) {
            pathFactory = (PathFactory) param;
        } else {
            pathFactory = PathFactoryImpl.getInstance();
        }

        QValueFactory vFactory;
        param = parameters.get(PARAM_QVALUE_FACTORY);
        if (param != null && param instanceof QValueFactory) {
            vFactory = (QValueFactory) param;
        } else {
            vFactory = QValueFactoryImpl.getInstance();
        }

        int itemInfoCacheSize = ItemInfoCacheImpl.DEFAULT_CACHE_SIZE;
        param = parameters.get(PARAM_ITEMINFO_CACHE_SIZE);
        if (param != null) {
            try {
                itemInfoCacheSize = Integer.parseInt(param.toString());
            } catch (NumberFormatException e) {
                // ignore, use default
            }
        }
        return new RepositoryServiceImpl(uri, idFactory, nameFactory, pathFactory, vFactory, itemInfoCacheSize, ConnectionOptions.fromServiceFactoryParameters(parameters));
    }
}
