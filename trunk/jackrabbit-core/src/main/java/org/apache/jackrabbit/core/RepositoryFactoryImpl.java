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
package org.apache.jackrabbit.core;

import static org.apache.jackrabbit.core.config.RepositoryConfigurationParser.REPOSITORY_HOME_VARIABLE;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.api.JackrabbitRepositoryFactory;
import org.apache.jackrabbit.api.management.RepositoryManager;
import org.apache.jackrabbit.commons.JcrUtils;

/**
 * <code>RepositoryFactoryImpl</code> implements a repository factory that
 * creates a {@link TransientRepository} on {@link #getRepository(Map)}.
 */
public class RepositoryFactoryImpl implements JackrabbitRepositoryFactory {

    /**
     * Name of the repository home parameter.
     */
    public static final String REPOSITORY_HOME
            = "org.apache.jackrabbit.repository.home";

    /**
     * Name of the repository configuration parameter.
     */
    public static final String REPOSITORY_CONF
            = "org.apache.jackrabbit.repository.conf";

    /**
     * Map of repository instances.
     * Key = repository parameters, value = repository instance.
     */
    private static final Map<Properties, TransientRepository> REPOSITORIES =
        new HashMap<Properties, TransientRepository>();

    /**
     * The repository instances that were created by this factory.
     */
    private final Set<TransientRepository> ownRepositories =
        new HashSet<TransientRepository>();

    public Repository getRepository(Map parameters) throws RepositoryException {
        if (parameters == null) {
            return getRepository(null, Collections.emptyMap());
        } else if (parameters.containsKey(REPOSITORY_HOME)) {
            String home = parameters.get(REPOSITORY_HOME).toString();
            return getRepository(home, parameters);
        } else if (parameters.containsKey(JcrUtils.REPOSITORY_URI)) {
            Object parameter = parameters.get(JcrUtils.REPOSITORY_URI);
            try {
                URI uri = new URI(parameter.toString().trim());
                String scheme = uri.getScheme();
                if (("file".equalsIgnoreCase(scheme)
                        || "jcr-jackrabbit".equalsIgnoreCase(scheme))
                        && uri.getAuthority() == null) {
                    File file = new File(uri.getPath());
                    if (file.isFile()) {
                        return null; // Not a (possibly missing) directory
                    } else {
                        return getRepository(file.getPath(), parameters);
                    }
                } else {
                    return null; // not a file: or jcr-jackrabbit: URI
                }
            } catch (URISyntaxException e) {
                return null; // not a valid URI
            }
        } else {
            return null; // unknown or insufficient parameters
        }
    }

    private Repository getRepository(String home, Map<?, ?> parameters)
            throws RepositoryException {
        TransientRepository repository =
            getOrCreateRepository(home, parameters);
        ownRepositories.add(repository);
        return repository;
    }

    /**
     * Either returns a cached repository or creates a repository instance and
     * puts it into the {@link #REPOSITORIES} cache.
     *
     * @param home path to the repository home.
     * @return the repository instance.
     * @throws RepositoryException if an error occurs while creating the
     *          repository instance.
     */
    private static synchronized TransientRepository getOrCreateRepository(
            String home, Map<?, ?> parameters) throws RepositoryException {
        // Prepare the repository properties
        Properties properties = new Properties(System.getProperties());
        for (Map.Entry<?, ?> entry : parameters.entrySet()) {
            Object key = entry.getKey();
            if (key != null) {
                Object value = entry.getValue();
                if (value != null) {
                    properties.setProperty(
                            key.toString(), value.toString());
                } else {
                    properties.remove(key.toString());
                }
            }
        }
        if (home != null) {
            properties.put(REPOSITORY_HOME_VARIABLE, home);
        }

        TransientRepository repository = REPOSITORIES.get(properties);
        if (repository == null) {
            try {
                TransientRepository tr;
                if (home == null) {
                    tr = new TransientRepository(properties);
                    // also remember this instance as the default repository
                    REPOSITORIES.put(null, tr);
                } else {
                    tr = new TransientRepository(properties);
                }
                REPOSITORIES.put(properties, tr);
                repository = tr;
            } catch (IOException e) {
                throw new RepositoryException(
                        "Failed to install repository configuration", e);
            }
        }
        return repository;
    }

    public RepositoryManager getRepositoryManager(JackrabbitRepository repo) throws RepositoryException {
        if (!(repo instanceof TransientRepository)) {
            throw new RepositoryException("The repository was not created in this factory");
        }
        if (!ownRepositories.contains(repo)) {
            throw new RepositoryException("The repository was not created in this factory");
        }
        return new RepositoryManagerImpl((TransientRepository) repo);
    }

}
