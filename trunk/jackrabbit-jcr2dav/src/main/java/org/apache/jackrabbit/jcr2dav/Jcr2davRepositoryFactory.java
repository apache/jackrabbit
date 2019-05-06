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
package org.apache.jackrabbit.jcr2dav;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.jcr2spi.Jcr2spiRepositoryFactory;
import org.apache.jackrabbit.jcr2spi.RepositoryImpl;
import org.apache.jackrabbit.spi.RepositoryServiceFactory;
import org.apache.jackrabbit.spi2dav.Spi2davRepositoryServiceFactory;
import org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory;

/**
 * Repository factory for JCR to WebDAV connections. This factory supports
 * three main configuration parameters:
 * <dl>
 *   <dt>{@link JcrUtils#REPOSITORY_URI org.apache.jackrabbit.repository.uri}</dt>
 *   <dd>
 *     If this parameter contains a valid http or https URI, then an spi2davex
 *     connection to that URI is returned.
 *   </dd>
 *   <dt>{@link Spi2davRepositoryServiceFactory#PARAM_REPOSITORY_URI org.apache.jackrabbit.spi2dav.uri}</dt>
 *   <dd>
 *     If this parameter is specified, then an spi2dav connection
 *     to that URI is returned.
 *   </dd>
 *   <dt>{@link Spi2davexRepositoryServiceFactory#PARAM_REPOSITORY_URI org.apache.jackrabbit.spi2davex.uri}</dt>
 *   <dd>
 *     If this parameter is specified, then an spi2davex connection
 *     to that URI is returned.
 *   </dd>
 * </dl>
 *
 * @since Apache Jackrabbit 2.0
 */
@SuppressWarnings("unchecked")
public class Jcr2davRepositoryFactory implements RepositoryFactory {

    private static final String DAV_URI =
        Spi2davRepositoryServiceFactory.PARAM_REPOSITORY_URI;

    private static final String DAVEX_URI =
        Spi2davexRepositoryServiceFactory.PARAM_REPOSITORY_URI;

    public Repository getRepository(Map parameters) throws RepositoryException {
        if (parameters == null) {
            return null;
        } else if (parameters.containsKey(DAV_URI)) {
            return getRepository(
                    new Spi2davRepositoryServiceFactory(), parameters);
        } else if (parameters.containsKey(DAVEX_URI)) {
            return getRepository(
                    new Spi2davexRepositoryServiceFactory(), parameters);
        } else if (parameters.containsKey(JcrUtils.REPOSITORY_URI)) {
            Map copy = new HashMap(parameters);
            Object parameter = copy.remove(JcrUtils.REPOSITORY_URI);
            try {
                URI uri = new URI(parameter.toString().trim());
                String scheme = uri.getScheme();
                // TODO: Check whether this is a valid dav or davex URI
                // TODO: Support tags like <link rel="jackrabbit-spi2dav" ...>
                if ("http".equalsIgnoreCase(scheme)
                        || "https".equalsIgnoreCase(scheme)) {
                    copy.put(DAVEX_URI, parameter);
                    return getRepository(
                            new Spi2davexRepositoryServiceFactory(), copy);
                } else {
                    return null;
                }
            } catch (URISyntaxException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private Repository getRepository(
            RepositoryServiceFactory factory, Map parameters)
            throws RepositoryException {
        try {
            return RepositoryImpl.create(
                    new Jcr2spiRepositoryFactory.RepositoryConfigImpl(
                            factory, parameters));
        } catch (RepositoryException e) {
            // Unable to connect to the specified repository.
            // Most likely the server is either not running or
            // the given URI does not point to a valid davex server.
            return null;
        }
    }

}
