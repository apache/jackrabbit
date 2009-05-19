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
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PersistenceCopier;
import org.apache.jackrabbit.core.state.ItemStateException;
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
                        target.getVersionManagerImpl());
                copyWorkspaces(source, target);
            } catch (InvalidNodeTypeDefException e) {
                throw new RepositoryException("Failed to copy node types", e);
            } catch (ItemStateException e) {
                throw new RepositoryException("Failed to copy item states", e);
            } finally {
                target.shutdown();
            }

            // Remove index directories to force re-indexing on next startup
            // TODO: There should be a cleaner way to do this
            File targetDir = new File(targetConfig.getHomeDir());
            File repoDir = new File(targetDir, "repository");
            FileUtils.deleteQuietly(new File(repoDir, "index"));
            File[] workspaces = new File(targetDir, "workspaces").listFiles();
            if (workspaces != null) {
                for (File workspace : workspaces) {
                    FileUtils.deleteQuietly(new File(workspace, "index"));
                }
            }
        } finally {
            source.shutdown();
        }
    }

    private void copyNamespaces(
            NamespaceRegistry source, NamespaceRegistry target)
            throws RepositoryException {
        logger.info("Copying registered namespaces");

        Collection<String> existing = Arrays.asList(target.getURIs());
        for (String uri : source.getURIs()) {
            if (!existing.contains(uri)) {
                // TODO: what if the prefix is already taken?
                target.registerNamespace(source.getPrefix(uri), uri);
            }
        }
    }

    private void copyNodeTypes(NodeTypeRegistry source, NodeTypeRegistry target)
            throws RepositoryException, InvalidNodeTypeDefException {
        logger.info("Copying registered node types");

        Collection<Name> existing =
            Arrays.asList(target.getRegisteredNodeTypes());
        Collection<NodeTypeDef> register = new ArrayList<NodeTypeDef>();
        for (Name name : source.getRegisteredNodeTypes()) {
            // TODO: what about modified node types?
            if (!existing.contains(name)) {
                register.add(source.getNodeTypeDef(name));
            }
        }
        target.registerNodeTypes(register);
    }

    private void copyVersionStore(
            VersionManagerImpl source, VersionManagerImpl target)
            throws RepositoryException, ItemStateException {
        logger.info("Copying version histories");

        PersistenceCopier copier = new PersistenceCopier(
                source.getPersistenceManager(),
                target.getPersistenceManager());
        copier.copy(RepositoryImpl.VERSION_STORAGE_NODE_ID);
    }

    private void copyWorkspaces(RepositoryImpl source, RepositoryImpl target)
            throws RepositoryException, ItemStateException {
        Collection<String> existing = Arrays.asList(target.getWorkspaceNames());
        for (String name : source.getWorkspaceNames()) {
            logger.info("Copying workspace {}" , name);

            if (!existing.contains(name)) {
                target.createWorkspace(name);
            }

            PersistenceCopier copier = new PersistenceCopier(
                    source.getWorkspaceInfo(name).getPersistenceManager(),
                    target.getWorkspaceInfo(name).getPersistenceManager());
            copier.excludeNode(RepositoryImpl.SYSTEM_ROOT_NODE_ID);
            copier.copy(RepositoryImpl.ROOT_NODE_ID);
        }
    }

}
