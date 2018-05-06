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
import org.apache.jackrabbit.core.lock.LockManagerImpl;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PersistenceCopier;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool for backing up or migrating the entire contents (workspaces,
 * version histories, namespaces, node types, etc.) of a repository to
 * a new repository. The target repository (if it exists) is overwritten.
 * <p>
 * No cluster journal records are written in the target repository. If the
 * target repository is clustered, it should be the only node in the cluster.
 * <p>
 * The target repository needs to be fully reindexed after the copy operation.
 * The static copy() methods will remove the target search index folders from
 * their default locations to trigger automatic reindexing when the repository
 * is next started.
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
     * Source repository context.
     */
    private final RepositoryContext source;

    /**
     * Target repository context.
     */
    private final RepositoryContext target;

    /**
     * Copies the contents of the repository in the given source directory
     * to a repository in the given target directory.
     *
     * @param source source repository directory
     * @param target target repository directory
     * @throws RepositoryException if the copy operation fails
     * @throws IOException if the target repository can not be initialized
     */
    public static void copy(File source, File target)
            throws RepositoryException, IOException {
        copy(RepositoryConfig.create(source), RepositoryConfig.install(target));
    }

    /**
     * Copies the contents of the repository with the given configuration
     * to a repository in the given target directory.
     *
     * @param source source repository configuration
     * @param target target repository directory
     * @throws RepositoryException if the copy operation fails
     * @throws IOException if the target repository can not be initialized
     */
    public static void copy(RepositoryConfig source, File target)
            throws RepositoryException, IOException {
        copy(source, RepositoryConfig.install(target));
    }

    /**
     * Copies the contents of the source repository with the given
     * configuration to a target repository with the given configuration.
     *
     * @param source source repository configuration
     * @param target target repository directory
     * @throws RepositoryException if the copy operation fails
     */
    public static void copy(RepositoryConfig source, RepositoryConfig target)
            throws RepositoryException {
        RepositoryImpl repository = RepositoryImpl.create(source);
        try {
            copy(repository, target);
        } finally {
            repository.shutdown();
        }
    }

    /**
     * Copies the contents of the given source repository to a repository in
     * the given target directory.
     * <p>
     * The source repository <strong>must not be modified</strong> while
     * the copy operation is running to avoid an inconsistent copy.
     *
     * @param source source repository directory
     * @param target target repository directory
     * @throws RepositoryException if the copy operation fails
     * @throws IOException if the target repository can not be initialized
     */
    public static void copy(RepositoryImpl source, File target)
            throws RepositoryException, IOException {
        copy(source, RepositoryConfig.install(target));
    }

    /**
     * Copies the contents of the given source repository to a target
     * repository with the given configuration.
     * <p>
     * The source repository <strong>must not be modified</strong> while
     * the copy operation is running to avoid an inconsistent copy.
     *
     * @param source source repository directory
     * @param target target repository directory
     * @throws RepositoryException if the copy operation fails
     */
    public static void copy(RepositoryImpl source, RepositoryConfig target)
            throws RepositoryException {
        RepositoryImpl repository = RepositoryImpl.create(target);
        try {
            new RepositoryCopier(source, repository).copy();
        } finally {
            repository.shutdown();
        }

        // Remove index directories to force re-indexing on next startup
        // TODO: There should be a cleaner way to do this
        File targetDir = new File(target.getHomeDir());
        File repoDir = new File(targetDir, "repository");
        FileUtils.deleteQuietly(new File(repoDir, "index"));
        File[] workspaces = new File(targetDir, "workspaces").listFiles();
        if (workspaces != null) {
            for (File workspace : workspaces) {
                FileUtils.deleteQuietly(new File(workspace, "index"));
            }
        }
    }

    /**
     * Creates a tool for copying the full contents of the source repository
     * to the given target repository. Any existing content in the target
     * repository will be overwritten.
     *
     * @param source source repository
     * @param target target repository
     */
    public RepositoryCopier(RepositoryImpl source, RepositoryImpl target) {
        // TODO: It would be better if we were given the RepositoryContext
        // instances directly. Perhaps we should use something like
        // RepositoryImpl.getRepositoryCopier(RepositoryImpl target)
        // instead of this public constructor to achieve that.
        this.source = source.getRepositoryContext();
        this.target = target.getRepositoryContext();
    }

    /**
     * Copies the full content from the source to the target repository.
     * <p>
     * The source repository <strong>must not be modified</strong> while
     * the copy operation is running to avoid an inconsistent copy.
     * <p>
     * This method leaves the search indexes of the target repository in
     * an 
     * Note that both the source and the target repository must be closed
     * during the copy operation as this method requires exclusive access
     * to the repositories.
     *
     * @throws RepositoryException if the copy operation fails
     */
    public void copy() throws RepositoryException {
        logger.info(
                "Copying repository content from {} to {}",
                source.getRepository().repConfig.getHomeDir(),
                target.getRepository().repConfig.getHomeDir());
        try {
            copyNamespaces();
            copyNodeTypes();
            copyVersionStore();
            copyWorkspaces();
        } catch (Exception e) {
            throw new RepositoryException("Failed to copy content", e);
        }
    }

    private void copyNamespaces() throws RepositoryException {
        NamespaceRegistry sourceRegistry = source.getNamespaceRegistry();
        NamespaceRegistry targetRegistry = target.getNamespaceRegistry();

        logger.info("Copying registered namespaces");
        Collection<String> existing = Arrays.asList(targetRegistry.getURIs());
        for (String uri : sourceRegistry.getURIs()) {
            if (!existing.contains(uri)) {
                // TODO: what if the prefix is already taken?
                targetRegistry.registerNamespace(
                        sourceRegistry.getPrefix(uri), uri);
            }
        }
    }

    private void copyNodeTypes() throws RepositoryException {
        NodeTypeRegistry sourceRegistry = source.getNodeTypeRegistry();
        NodeTypeRegistry targetRegistry = target.getNodeTypeRegistry();

        logger.info("Copying registered node types");
        Collection<Name> existing =
            Arrays.asList(targetRegistry.getRegisteredNodeTypes());
        Collection<QNodeTypeDefinition> register = new ArrayList<QNodeTypeDefinition>();
        for (Name name : sourceRegistry.getRegisteredNodeTypes()) {
            // TODO: what about modified node types?
            if (!existing.contains(name)) {
                register.add(sourceRegistry.getNodeTypeDef(name));
            }
        }
        try {
            targetRegistry.registerNodeTypes(register);
        } catch (InvalidNodeTypeDefException e) {
            throw new RepositoryException("Unable to copy node types", e);
        }
    }

    private void copyVersionStore() throws RepositoryException {
        logger.info("Copying version histories");
        PersistenceCopier copier = new PersistenceCopier(
                source.getInternalVersionManager().getPersistenceManager(),
                target.getInternalVersionManager().getPersistenceManager(),
                target.getDataStore());
        copier.copy(RepositoryImpl.VERSION_STORAGE_NODE_ID);
        copier.copy(RepositoryImpl.ACTIVITIES_NODE_ID);
    }

    private void copyWorkspaces() throws RepositoryException {
        Collection<String> existing =
            Arrays.asList(target.getRepository().getWorkspaceNames());
        for (String name : source.getRepository().getWorkspaceNames()) {
            logger.info("Copying workspace {}" , name);

            if (!existing.contains(name)) {
                target.getRepository().createWorkspace(name);
            }

            // Copy all the workspace content
            PersistenceCopier copier = new PersistenceCopier(
                    source.getRepository().getWorkspaceInfo(name).getPersistenceManager(),
                    target.getRepository().getWorkspaceInfo(name).getPersistenceManager(),
                    target.getDataStore());
            copier.excludeNode(RepositoryImpl.SYSTEM_ROOT_NODE_ID);
            copier.copy(RepositoryImpl.ROOT_NODE_ID);

            // Copy all the active open-scoped locks
            LockManagerImpl sourceLockManager =
                source.getRepository().getLockManager(name);
            LockManagerImpl targetLockManager =
                target.getRepository().getLockManager(name);
            targetLockManager.copyOpenScopedLocksFrom(sourceLockManager);
        }
    }

}
