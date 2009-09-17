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

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.RepositoryServiceFactory;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;

/**
 * This {@link RepositoryServiceFactory} implementation is responsible
 * for creating {@link RepositoryServiceImpl} instances.
 */
public class Spi2davexRepositoryServiceFactory implements RepositoryServiceFactory {

    /**
     * Mandatory configuration parameter: It's value is expected to specify the
     * URI of the JCR server implementation. {@link #DEFAULT_REPOSITORY_URI} is used as
     * fallback if no parameters or uri has been specified and the uri could not
     * been retrieved from system props either.
     *
     * @see org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet
     */
    public static final String PARAM_REPOSITORY_URI = "org.apache.jackrabbit.spi2davex.uri";

    /**
     * Default URI for the {@link #PARAM_REPOSITORY_URI} configuration
     * parameter.
     */
    public static String DEFAULT_REPOSITORY_URI = "http://localhost:8080/jackrabbit/server";

    /**
     * Optional batch read configuration parameter: If present it's value is
     * expected to be an instance of {@link BatchReadConfig}
     */
    public static final String PARAM_BATCHREAD_CONFIG = "org.apache.jackrabbit.spi2dav.BatchReadConfig";

    public RepositoryService createRepositoryService(Map<?, ?> parameters) throws RepositoryException {
        String uri;

        if (parameters == null) {
            uri = System.getProperty(PARAM_REPOSITORY_URI);
        }
        else {
            uri = parameters.get(PARAM_REPOSITORY_URI) == null
                    ? null
                    : parameters.get(PARAM_REPOSITORY_URI).toString();
        }
        if (uri == null) {
            uri = DEFAULT_REPOSITORY_URI;
        }

        BatchReadConfig brc = null;
        if (parameters != null) {
            Object param = parameters.get(PARAM_BATCHREAD_CONFIG);
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

}
