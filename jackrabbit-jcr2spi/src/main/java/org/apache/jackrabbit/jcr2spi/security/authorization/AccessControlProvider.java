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
package org.apache.jackrabbit.jcr2spi.security.authorization;

import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * AccessControlProvider... TODO
 */
public interface AccessControlProvider {

    void init(RepositoryConfig config) throws RepositoryException;

    /**
     * The privileges corresponding to the specified name.
     *
     * @param sessionInfo
     * @param resolver
     * @return
     * @see javax.jcr.security.AccessControlManager#privilegeFromName(String)
     */
    Privilege privilegeFromName(SessionInfo sessionInfo, NamePathResolver resolver, String privilegeName) throws RepositoryException;

    /**
     * Obtain the privileges supported at the specified path.
     *
     * @param sessionInfo
     * @param nodeId The id of an existing node or {@code null} to obtain privileges
     *               that are supported for repository level access.
     * @param npResolver
     * @return
     * @throws RepositoryException
     * @see javax.jcr.security.AccessControlManager#getSupportedPrivileges(String)
     */
    Map<String, Privilege> getSupportedPrivileges(SessionInfo sessionInfo, NodeId nodeId, NamePathResolver npResolver) throws RepositoryException;

    Set<Privilege> getPrivileges(SessionInfo sessionInfo, NodeId id, NamePathResolver npResolver) throws RepositoryException;

    AccessControlManager createAccessControlManager(SessionInfo sessionInfo,
                                                    UpdatableItemStateManager itemStateManager,
                                                    ItemManager itemManager,
                                                    ItemDefinitionProvider definitionProvider,
                                                    HierarchyManager hierarchyManager,
                                                    NamePathResolver npResolver) throws RepositoryException;
}
