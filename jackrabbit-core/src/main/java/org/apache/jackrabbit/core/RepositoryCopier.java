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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PersistenceCopier;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.version.VersionManagerImpl;
import org.apache.jackrabbit.spi.Name;

public class RepositoryCopier {

    private final RepositoryImpl source;

    private final RepositoryImpl target;

    public RepositoryCopier(File source, File target)
            throws RepositoryException, IOException {
        if (!source.isDirectory()) {
            throw new RepositoryException("Not a directory: " + source);
        }

        File sx = new File(source, "repository.xml");
        if (!sx.isFile()) {
            throw new RepositoryException(
                    "Not a repository directory: " + source);
        }

        if (target.exists()) {
            throw new RepositoryException("Target directory exists: " + target);
        }
        target.mkdirs();

        File tx = new File(target, "repository.xml");
        OutputStream output = new FileOutputStream(tx);
        try {
            InputStream input =
                RepositoryImpl.class.getResourceAsStream("repository.xml");
            try {
                IOUtils.copy(input, output);
            } finally {
                input.close();
            }
        } finally {
            output.close();
        }

        this.source = RepositoryImpl.create(
                RepositoryConfig.create(sx.getPath(), source.getPath()));
        this.target = RepositoryImpl.create(
                RepositoryConfig.create(tx.getPath(), target.getPath()));
    }

    public RepositoryCopier(RepositoryConfig source, RepositoryConfig target)
            throws RepositoryException {
        this.source = RepositoryImpl.create(source);
        this.target = RepositoryImpl.create(target);
    }

    public void copy() throws Exception {
        System.out.println(
                "Copying repository " + source.getConfig().getHomeDir());
        copyNamespaces();
        copyNodeTypes();
        copyVersionStore();
        copyWorkspaces();

        target.shutdown();
        source.shutdown();

        System.out.println("  Done.");
    }

    private void copyNamespaces() throws RepositoryException {
        NamespaceRegistry sourceRegistry = source.getNamespaceRegistry();
        NamespaceRegistry targetRegistry = target.getNamespaceRegistry();
        Set<String> existing = new HashSet<String>(Arrays.asList(
                targetRegistry.getURIs()));
        for (String uri : sourceRegistry.getURIs()) {
            if (!existing.contains(uri)) {
                // TODO: what if the prefix is already taken?
                targetRegistry.registerNamespace(
                        sourceRegistry.getPrefix(uri), uri);
            }
        }
    }

    private void copyNodeTypes()
            throws RepositoryException, InvalidNodeTypeDefException {
        NodeTypeRegistry sourceRegistry = source.getNodeTypeRegistry();
        NodeTypeRegistry targetRegistry = target.getNodeTypeRegistry();
        Set<Name> existing = new HashSet<Name>(Arrays.asList(
                targetRegistry.getRegisteredNodeTypes()));
        Collection<NodeTypeDef> register = new ArrayList<NodeTypeDef>();
        for (Name name : sourceRegistry.getRegisteredNodeTypes()) {
            // TODO: what about modified node types?
            if (!existing.contains(name)) {
                register.add(sourceRegistry.getNodeTypeDef(name));
            }
        }
        targetRegistry.registerNodeTypes(register);
    }

    private void copyVersionStore()
            throws RepositoryException, ItemStateException {
        System.out.println("  Copying version histories...");
        VersionManagerImpl sourceManager = source.getVersionManagerImpl();
        VersionManagerImpl targetManager = target.getVersionManagerImpl();
        PersistenceCopier copier = new PersistenceCopier(
                sourceManager.getPersistenceManager(),
                targetManager.getPersistenceManager());
        copier.copy(RepositoryImpl.VERSION_STORAGE_NODE_ID);
    }

    private void copyWorkspaces()
            throws RepositoryException, ItemStateException {
        Set<String> existing = new HashSet<String>(Arrays.asList(
                target.getWorkspaceNames()));
        for (String name : source.getWorkspaceNames()) {
            System.out.println("  Copying workspace " + name + "...");

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
