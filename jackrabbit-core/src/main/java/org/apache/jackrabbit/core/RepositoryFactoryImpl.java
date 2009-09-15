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

import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.api.JackrabbitRepositoryFactory;
import org.apache.jackrabbit.api.management.RepositoryManager;

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
     * Map of repository instances. Key = repository home, value = repository
     * instance.
     */
    private static final Map<String, JackrabbitRepository> REPOSITORY_INSTANCES = new HashMap<String, JackrabbitRepository>();

    /**
     * The repository instances that were created by this factory.
     */
    private final Set<TransientRepository> ownRepositories = new HashSet<TransientRepository>();

    public Repository getRepository(Map parameters) throws RepositoryException {
        JackrabbitRepository repo;
        synchronized (REPOSITORY_INSTANCES) {
            if (parameters == null) {
                repo = getOrCreateRepository(null, null);
            } else if (parameters.containsKey(REPOSITORY_CONF)
                    && parameters.containsKey(REPOSITORY_HOME)) {
                String conf = (String) parameters.get(REPOSITORY_CONF);
                String home = (String) parameters.get(REPOSITORY_HOME);
                repo = getOrCreateRepository(conf, home);
            } else {
                // unknown or insufficient parameters
                repo = null;
            }
        }
        return repo;
    }

    /**
     * Either returns a cached repository or creates a repository instance and
     * puts it into the {@link #REPOSITORY_INSTANCES} cache.
     *
     * @param conf path to the repository configuration file.
     * @param home path to the repository home.
     * @return the repository instance.
     * @throws RepositoryException if an error occurs while creating the
     *          repository instance.
     */
    private JackrabbitRepository getOrCreateRepository(String conf,
                                                       String home)
            throws RepositoryException {
        JackrabbitRepository repo = REPOSITORY_INSTANCES.get(home);
        if (repo == null) {
            TransientRepository tr;
            if (home == null) {
                tr = new TransientRepository();
                // also remember this instance as the default repository
                REPOSITORY_INSTANCES.put(null, tr);
            } else {
                tr = new TransientRepository(conf, home);
            }
            REPOSITORY_INSTANCES.put(tr.getHomeDir(), tr);
            ownRepositories.add(tr);
            repo = tr;
        }
        return repo;
    }

    public RepositoryManager getRepositoryManager(JackrabbitRepository repo) throws RepositoryException {
        if (repo instanceof TransientRepository) {
            throw new RepositoryException("The repository was not created in this factory");
        }
        if (!ownRepositories.contains(repo)) {
            throw new RepositoryException("The repository was not created in this factory");
        }
        return new RepositoryManagerImpl((TransientRepository) repo);
    }
}
