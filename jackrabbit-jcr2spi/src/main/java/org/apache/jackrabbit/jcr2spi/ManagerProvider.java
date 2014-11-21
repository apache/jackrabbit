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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.jcr2spi.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.security.AccessManager;
import org.apache.jackrabbit.jcr2spi.lock.LockStateManager;
import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeTypeProvider;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeDefinitionProvider;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;

import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.RepositoryException;

/**
 * <code>ManagerProvider</code>...
 */
public interface ManagerProvider {

    public org.apache.jackrabbit.spi.commons.conversion.NamePathResolver getNamePathResolver();

    public NameResolver getNameResolver();

    public org.apache.jackrabbit.spi.commons.conversion.PathResolver getPathResolver();

    public NamespaceResolver getNamespaceResolver();

    public HierarchyManager getHierarchyManager();

    public AccessManager getAccessManager();

    /**
     * Returns the <code>LockStateManager</code> associated with this
     * <code>ManagerProvider</code>.
     *
     * @return the <code>LockStateManager</code> associated with this
     * <code>ManagerProvider</code>
     */
    public LockStateManager getLockStateManager();

    /**
     * Returns the <code>VersionManager</code> associated with this
     * <code>ManagerProvider</code>.
     *
     * @return the <code>VersionManager</code> associated with this
     * <code>ManagerProvider</code>
     */
    public VersionManager getVersionStateManager();

    public ItemDefinitionProvider getItemDefinitionProvider();

    public NodeTypeDefinitionProvider getNodeTypeDefinitionProvider();

    public EffectiveNodeTypeProvider getEffectiveNodeTypeProvider();

    /**
     * Same as {@link Session#getValueFactory()} but omits the check, if this repository
     * is really level 2 compliant. Therefore, this method may be used for
     * internal functionality only, that require creation and conversion of
     * JCR values.
     *
     * @return
     * @throws RepositoryException
     */
    public ValueFactory getJcrValueFactory() throws RepositoryException;

    public QValueFactory getQValueFactory() throws RepositoryException;

    public AccessControlProvider getAccessControlProvider() throws RepositoryException;
}