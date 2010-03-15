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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.JcrUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
    private final Map<Reference, Reference> references;

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
        this.references = new HashMap<Reference, Reference>();
    }

    /**
     * Create repository.
     *
     * @param homeDir   The location of the repository.
     * @param configFile The path to the repository configuration file. If the file is located on
     *                   the classpath, the path should be prepended with
     *                   JCARepositoryManager.CLASSPATH_CONFIG_PREFIX.
     * @return repository instance
     */
    public Repository createRepository(String homeDir, String configFile)
            throws RepositoryException {
        Reference ref = getReference(homeDir, configFile);
        return ref.create();
    }

    /**
     * Shutdown all the repositories.
     */
    public void shutdown() {
        Collection<Reference> references = this.references.values();
        Iterator<Reference> iter = references.iterator();
        while (iter.hasNext()) {
            Reference ref = iter.next();
            ref.shutdown();
        }
        this.references.clear();
    }

    /**
     * Return the reference.
     *
     * @param homeDir   The location of the repository.
     * @param configFile The path to the repository configuration file.
     */
    private synchronized Reference getReference(String homeDir, String configFile) {
        Reference ref = new Reference(homeDir, configFile);
        Reference other = references.get(ref);

        if (other == null) {
            references.put(ref, ref);
            return ref;
        } else {
            return other;
        }
    }

    /**
     * Return the instance.
     */
    public static JCARepositoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * Repository reference implementation.
     */
    private final class Reference {
        /**
         * Home directory.
         */
        private final String homeDir;

        /**
         * Configuration file.
         *
         * Configuration files located on the classpath begin with
         * JCARepositoryManager.CLASSPATH_CONFIG_PREFIX.
         */
        private String configFile;

        /**
         * Repository instance.
         */
        private Repository repository;

        /**
         * Construct the manager.
         */
        private Reference(String homeDir, String configFile) {
            this.homeDir = homeDir;
            this.configFile = configFile;
            this.repository = null;
        }

        /**
         * Return the repository.
         */
        public Repository create() throws RepositoryException {
            if (repository == null) {
                File dir = new File(homeDir);
                dir.mkdirs();

                File xml;
                if (configFile.startsWith(CLASSPATH_CONFIG_PREFIX)) {
                    String source =
                        configFile.substring(CLASSPATH_CONFIG_PREFIX.length());
                    xml = new File(homeDir, "repository.xml");
                    copyConfigFile(source, xml);
                } else {
                    xml = new File(configFile);
                }

                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put(
                        "org.apache.jackrabbit.repository.home",
                        dir.getPath());
                parameters.put(
                        "org.apache.jackrabbit.repository.conf",
                        xml.getPath());
                repository = JcrUtils.getRepository(parameters);
            }

            return repository;
        }

        private void copyConfigFile(String source, File target)
                throws RepositoryException {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = this.getClass().getClassLoader();
            }

            InputStream input = cl.getResourceAsStream(source);
            if (input != null) {
                try {
                    try {
                        OutputStream output = new FileOutputStream(target);
                        try {
                            IOUtils.copy(input, output);
                        } finally {
                            output.close();
                        }
                    } finally {
                        input.close();
                    }
                } catch (IOException e) {
                    throw new RepositoryException(
                            "Failed to copy configuration to " + target, e);
                }
            } else {
                throw new RepositoryException(
                        "Repository configuration not found: " + source);
            }
        }

        /**
         * Shutdown the repository.
         */
        public void shutdown() {
            if (repository instanceof JackrabbitRepository) {
                ((JackrabbitRepository) repository).shutdown();
            }
        }

        /**
         * Return the hash code.
         */
        public int hashCode() {
            int result = homeDir != null ? homeDir.hashCode() : 0;
            result = 37 * result + (configFile != null ? configFile.hashCode() : 0);
            return result;
        }

        /**
         * Return true if equals.
         */
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof Reference) {
                return equals((Reference) o);
            } else {
                return false;
            }
        }

        /**
         * Return true if equals.
         */
        private boolean equals(Reference o) {
            return equals(homeDir, o.homeDir)
                && equals(configFile, o.configFile);
        }

        /**
         * Return true if equals.
         */
        private boolean equals(String s1, String s2) {
            if (s1 == s2) {
                return true;
            } else if ((s1 == null) || (s2 == null)) {
                return false;
            } else {
                return s1.equals(s2);
            }
        }
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
     *
     * @param homeDir   The location of the repository.
     * @param configFile The path to the repository configuration file.
     */
    public void autoShutdownRepository(String homeDir, String configFile) {
        if (this.isAutoShutdown()) {
            Reference ref = getReference(homeDir, configFile);
            ref.shutdown();
        }
    }

}
