/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements the repository manager.
 */
public final class JCARepositoryManager {

    /**
     * Instance of manager.
     */
    private final static JCARepositoryManager INSTANCE =
            new JCARepositoryManager();

    /**
     * References.
     */
    private final Map references;

    /**
     * Construct the manager.
     */
    private JCARepositoryManager() {
        this.references = new HashMap();
    }

    /**
     * Create repository.
     */
    public RepositoryImpl createRepository(String homeDir, String configFile)
            throws RepositoryException {
        Reference ref = getReference(homeDir, configFile);
        return ref.create();
    }

    /**
     * Shutdown the repository.
     */
    public void shutdownRepository(String homeDir, String configFile) {
        Reference ref = getReference(homeDir, configFile);
        ref.shutdown();
    }

    /**
     * Return the reference.
     */
    private synchronized Reference getReference(String homeDir, String configFile) {
        Reference ref = new Reference(homeDir, configFile);
        Reference other = (Reference) references.get(ref);

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
         */
        private final String configFile;

        /**
         * Repository instance.
         */
        private RepositoryImpl repository;

        /**
         * Reference count.
         */
        private int count;

        /**
         * Construct the manager.
         */
        private Reference(String homeDir, String configFile) {
            this.homeDir = homeDir;
            this.configFile = configFile;
            this.repository = null;
            this.count = 0;
        }

        /**
         * Return the repository.
         */
        public RepositoryImpl create()
                throws RepositoryException {
            if (repository == null) {
                RepositoryConfig config = RepositoryConfig.create(configFile, homeDir);
                repository = RepositoryImpl.create(config);
            }

            count++;
            return repository;
        }

        /**
         * Shutdown the repository.
         */
        public void shutdown() {
            if (count > 0) {
                count--;

                if (count == 0) {
                    repository.shutdown();
                    repository = null;
                }
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
            return equals(homeDir, o.homeDir) &&
                   equals(configFile, o.configFile);
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
}

