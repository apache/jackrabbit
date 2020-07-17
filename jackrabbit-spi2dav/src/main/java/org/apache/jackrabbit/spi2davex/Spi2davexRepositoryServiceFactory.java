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
package org.apache.jackrabbit.spi2davex;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.RepositoryServiceFactory;
import org.apache.jackrabbit.spi.commons.ItemInfoCacheImpl;
import org.apache.jackrabbit.spi2dav.ConnectionOptions;

/**
 * This {@link RepositoryServiceFactory} implementation is responsible
 * for creating {@link RepositoryServiceImpl} instances which communicate via <a href="https://jackrabbit.apache.org/archive/wiki/JCR/RemoteAccess_115513494.html#RemoteAccess-DavEx">DavEx</a>.
 * All parameter keys defined in this class and in addition the ones from {@link ConnectionOptions} 
 * are supported as arguments for {@link #createRepositoryService(Map)}.
 */
public class Spi2davexRepositoryServiceFactory implements RepositoryServiceFactory {

    /**
     * Mandatory configuration parameter: It's value is expected to specify the
     * URI of the JCR server implementation. {@link #DEFAULT_REPOSITORY_URI} is used as
     * fallback if no parameters or uri has been specified and the uri could not
     * been retrieved from system props either.
     */
    public static final String PARAM_REPOSITORY_URI = "org.apache.jackrabbit.spi2davex.uri";

    /**
     * Default URI for the {@link #PARAM_REPOSITORY_URI} configuration
     * parameter.
     */
    public static final String DEFAULT_REPOSITORY_URI = "http://localhost:8080/jackrabbit/server";

    /**
     * Optional batch read configuration parameter: If present it's value is
     * expected to be an instance of {@link BatchReadConfig}
     */
    public static final String PARAM_BATCHREAD_CONFIG = "org.apache.jackrabbit.spi2davex.BatchReadConfig";

    /**
     * Optional configuration parameter: It's value determines the size of the
     * {@link ItemInfoCache} cache. Defaults to {@link ItemInfoCacheImpl#DEFAULT_CACHE_SIZE}.
     */
    public static final String PARAM_ITEMINFO_CACHE_SIZE = "org.apache.jackrabbit.spi2davex.ItemInfoCacheSize";

    /**
     * Optional configuration parameter: It's value defines the
     * maximumConnectionsPerHost value on the HttpClient configuration and 
     * must be an int greater than zero.
     * Rather use {@link ConnectionOptions#PARAM_MAX_CONNECTIONS} instead.
     */
    public static final String PARAM_MAX_CONNECTIONS = "org.apache.jackrabbit.spi2davex.MaxConnections";

    /** 
     * For connecting to JCR servers older than version 1.5, the default workspace needs to be passed 
     * (if not explicitly given in each {@link Repository#login()} call)
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-4120">JCR-4120</a>
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1842">JCR-1842</a>
     */
    public static final String PARAM_WORKSPACE_NAME_DEFAULT =  "org.apache.jackrabbit.spi2davex.WorkspaceNameDefault";

    public RepositoryService createRepositoryService(Map<?, ?> parameters) throws RepositoryException {
        // retrieve the repository uri
        String uri;
        if (parameters == null) {
            uri = System.getProperty(PARAM_REPOSITORY_URI);
        } else {
            Object repoUri = parameters.get(PARAM_REPOSITORY_URI);
            uri = (repoUri == null) ? null : repoUri.toString();
        }
        if (uri == null) {
            uri = DEFAULT_REPOSITORY_URI;
        }

        // load other optional configuration parameters
        BatchReadConfig brc = null;
        int itemInfoCacheSize = ItemInfoCacheImpl.DEFAULT_CACHE_SIZE;

        // since JCR-4120 the default workspace name is no longer set to 'default'
        // note: if running with JCR Server < 1.5 a default workspace name must therefore be configured
        String workspaceNameDefault = null;

        if (parameters != null) {
            // batchRead config
            Object param = parameters.get(PARAM_BATCHREAD_CONFIG);
            if (param != null && param instanceof BatchReadConfig) {
                brc = (BatchReadConfig) param;
            }

            // itemCache size config
            param = parameters.get(PARAM_ITEMINFO_CACHE_SIZE);
            if (param != null) {
                try {
                    itemInfoCacheSize = Integer.parseInt(param.toString());
                } catch (NumberFormatException e) {
                    // ignore, use default
                }
            }

            param = parameters.get(PARAM_WORKSPACE_NAME_DEFAULT);
            if (param != null) {
                workspaceNameDefault = param.toString();
            }
        }

        return new RepositoryServiceImpl(uri, workspaceNameDefault, brc, itemInfoCacheSize, ConnectionOptions.fromServiceFactoryParameters(parameters));
    }

}
