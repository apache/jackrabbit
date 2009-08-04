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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PersistenceCopier;
import org.apache.jackrabbit.core.version.VersionManagerImpl;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool for backing up or migrating the entire contents (workspaces,
 * version histories, namespaces, node types, etc.) of a repository to
 * a new repository. The target repository (if it exists) is overwritten.
 *
 * @since Apache Jackrabbit 1.6
 */
public class RepositoryCopier {

    /**
     * Logger instance
     */
    private static final Logger logger =
        LoggerFactory.getLogger(RepositoryCopier.class);

    /**
     * Source repository configuration
     */
    private final RepositoryConfig sourceConfig;

    /**
     * Target repository configuration
     */
    private final RepositoryConfig targetConfig;

    /**
     * Creates a tool for copying the full contents of the source repository.
     * The given source repository directory is expected to contain the
     * repository configuration as a <code>repository.xml</code> file.
     * The target repository directory should not already exist. It will be
     * automatically created with default repository configuration.
     *
     * @param source source repository directory
     * @param target target repository directory
     * @throws RepositoryException if the repositories can not be accessed
     * @throws IOException if the target repository can not be initialized
     */
    public RepositoryCopier(File source, File target)
            throws RepositoryException, IOException {
        this(RepositoryConfig.create(source), RepositoryConfig.install(target));
    }

    /**
     * Creates a tool for copying the full contents of the source repository
     * to the given target repository. Any existing content in the target
     * repository will be overwritten.
     *
     * @param source source repository configuration
     * @param target target repository configuration
     * @throws RepositoryException if the repositories can not be accessed
     */
    public RepositoryCopier(RepositoryConfig source, RepositoryConfig target)
            throws RepositoryException {
        sourceConfig = source;
        targetConfig = target;
    }

    /**
     * Copies the full content from the source to the target repository.
     * Note that both the source and the target repository must be closed
     * during the copy operation as this method requires exclusive access
     * to the repositories.
     *
     * @throws RepositoryException if the copy operation fails
     */
    public void copy() throws RepositoryException {
        logger.info(
                "Copying repository content from {} to {}",
                sourceConfig.getHomeDir(), targetConfig.getHomeDir());

        RepositoryImpl source = RepositoryImpl.create(sourceConfig);
        try {
            RepositoryImpl target = RepositoryImpl.create(targetConfig);
            try {
                copyNamespaces(
                        source.getNamespaceRegistry(),
                        target.getNamespaceRegistry());
                copyNodeTypes(
                        source.getNodeTypeRegistry(),
                        target.getNodeTypeRegistry());
                copyVersionStore(
                        source.getVersionManagerImpl(),
                        target.getVersionManagerImpl(),
                        target.getDataStore());
                copyWorkspaces(source, target);
            } catch (Exception e) {
                throw new RepositoryException("Failed to copy content", e);
            } finally {
                target.shutdown();
            }

            // Remove index directories to force re-indexing on next startup
            // TODO: There should be a cleaner way to do this
            File targetDir = new File(targetConfig.getHomeDir());
            File repoDir = new File(targetDir, "repository");
            FileUtils.deleteQuietly(new File(repoDir, "index"));
            File[] workspaces = new File(targetDir, "workspaces").listFiles();
            for (int i = 0; workspaces != null && i < workspaces.length; i++) {
                FileUtils.deleteQuietly(new File(workspaces[i], "index"));
            }
        } finally {
            source.shutdown();
        }
    }

    private void copyNamespaces(
            NamespaceRegistry source, NamespaceRegistry target)
            throws RepositoryException {
        logger.info("Copying registered namespaces");

        Collection existing = Arrays.asList(target.getURIs());
        String[] uris = source.getURIs();
        for (int i = 0; i < uris.length; i++) {
            if (!existing.contains(uris[i])) {
                // TODO: what if the prefix is already taken?
                target.registerNamespace(source.getPrefix(uris[i]), uris[i]);
            }
        }
    }

    private void copyNodeTypes(NodeTypeRegistry source, NodeTypeRegistry target)
            throws RepositoryException, InvalidNodeTypeDefException {
        logger.info("Copying registered node types");

        Collection existing = Arrays.asList(target.getRegisteredNodeTypes());
        Collection register = new ArrayList();
        Name[] names = source.getRegisteredNodeTypes();
        for (int i = 0; i < names.length; i++) {
            // TODO: what about modified node types?
            if (!existing.contains(names[i])) {
                register.add(source.getNodeTypeDef(names[i]));
            }
        }
        target.registerNodeTypes(register);
    }

    private void copyVersionStore(
            VersionManagerImpl source, VersionManagerImpl target,
            DataStore store)
            throws Exception {
        logger.info("Copying version histories");

        PersistenceCopier copier = new PersistenceCopier(
                source.getPersistenceManager(),
                target.getPersistenceManager(), store);
        copier.copy(RepositoryImpl.VERSION_STORAGE_NODE_ID);
    }

    private void copyWorkspaces(RepositoryImpl source, RepositoryImpl target)
            throws Exception {
        Collection existing = Arrays.asList(target.getWorkspaceNames());
        String[] names = source.getWorkspaceNames();
        for (int i = 0; i < names.length; i++) {
            logger.info("Copying workspace {}" , names[i]);

            if (!existing.contains(names[i])) {
                target.createWorkspace(names[i]);
            }

            PersistenceCopier copier = new PersistenceCopier(
                    source.getWorkspaceInfo(names[i]).getPersistenceManager(),
                    target.getWorkspaceInfo(names[i]).getPersistenceManager(),
                    target.getDataStore());
            copier.excludeNode(RepositoryImpl.SYSTEM_ROOT_NODE_ID);
            copier.copy(RepositoryImpl.ROOT_NODE_ID);
        }
    }

}
