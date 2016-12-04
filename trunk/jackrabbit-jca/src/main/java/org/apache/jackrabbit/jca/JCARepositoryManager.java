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
package org.apache.jackrabbit.jca;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements the repository manager.
 */
public final class JCARepositoryManager {

    /** The config file prefix that signifies the file is to be loaded from the classpath. */
    public static final String CLASSPATH_CONFIG_PREFIX = "classpath:";

    /**
     * Instance of manager.
     */
    private static final JCARepositoryManager INSTANCE =
            new JCARepositoryManager();

    /**
     * References.
     */
    private final Map<Map<String, String>, Repository> repositories =
        new HashMap<Map<String, String>, Repository>();

    /**
     * Flag indicating that the life cycle
     * of the resource is not managed by the
     * application server
     */
    private boolean autoShutdown = true;

    /**
     * Construct the manager.
     */
    private JCARepositoryManager() {
    }

    /**
     * Create repository.
     *
     * @param parameters repository parameters
     * @return repository instance
     */
    public synchronized Repository createRepository(
            Map<String, String> parameters) throws RepositoryException {
        Repository repository = repositories.get(parameters);
        if (repository == null) {
        	if (parameters.containsKey(JcrUtils.REPOSITORY_URI)) {
	            repository = JcrUtils.getRepository(parameters);
        	} else {
	            repository = createNonTransientRepository(parameters);
        	}
            repositories.put(parameters, repository);
        }
        return repository;
    }

    /**
     * Creates a non transient Repository
     * 
     * @param parameters
     * @return Repository
     * @throws RepositoryException
     */
    private Repository createNonTransientRepository(
			Map<String, String> parameters) throws RepositoryException {
        RepositoryConfig config = null;

        String configFile = parameters.get(JCAManagedConnectionFactory.CONFIGFILE_KEY);
        String homeDir = parameters.get(JCAManagedConnectionFactory.HOMEDIR_KEY);
        
        if (configFile != null && configFile.startsWith(CLASSPATH_CONFIG_PREFIX)) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = this.getClass().getClassLoader();
            }

        	InputStream configInputStream = cl.getResourceAsStream(
        			configFile.substring(CLASSPATH_CONFIG_PREFIX.length()));
            try {
            	config = RepositoryConfig.create(configInputStream, homeDir);
            } finally {
                if (configInputStream != null) {
                    try {
                        configInputStream.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }            	
        } else if (configFile != null) {
            config = RepositoryConfig.create(configFile, homeDir);
        } else {
            config = RepositoryConfig.create(new File(homeDir));
        }
        return RepositoryImpl.create(config);
	}

	/**
     * Shutdown all the repositories.
     */
    public synchronized void shutdown() {
        for (Repository repository : repositories.values()) {
            if (repository instanceof JackrabbitRepository) {
                ((JackrabbitRepository) repository).shutdown();
            }
        }
        repositories.clear();
    }

    /**
     * Return the instance.
     */
    public static JCARepositoryManager getInstance() {
        return INSTANCE;
    }

    public boolean isAutoShutdown() {
        return autoShutdown;
    }

    public void setAutoShutdown(boolean autoShutdown) {
        this.autoShutdown = autoShutdown;
    }

    /**
     * Try to shutdown the repository only if
     * {@link JCARepositoryManager#autoShutdown} is true.
     */
    public synchronized void autoShutdownRepository(
            Map<String, String> parameters) {
        if (this.isAutoShutdown()) {
            Repository repository = repositories.get(parameters);
            if (repository instanceof JackrabbitRepository) {
                ((JackrabbitRepository) repository).shutdown();
            }
        }
    }

}
