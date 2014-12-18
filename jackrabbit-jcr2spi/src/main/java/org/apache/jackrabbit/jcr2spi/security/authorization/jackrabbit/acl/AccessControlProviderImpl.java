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
package org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.acl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.jcr2spi.security.authorization.PrivilegeImpl;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

public class AccessControlProviderImpl implements AccessControlProvider {

    private RepositoryService service;

    private Map<String, Privilege> privileges = new HashMap<String, Privilege>();

    @Override
    public void init(RepositoryConfig config, RepositoryService service) {
        this.service = service;
    }

    @Override
    public Privilege privilegeFromName(SessionInfo sessionInfo, NamePathResolver resolver, String privilegeName) throws RepositoryException {
        Privilege priv = privileges.get(privilegeName);

        if (priv == null) {
            readPrivilegesFromService(sessionInfo, resolver);
            if (privileges.containsKey(privilegeName)) {
                priv = privileges.get(privilegeName);
            } else {
                throw new AccessControlException("Unknown privilege " + privilegeName);
            }
        }
        return priv;
    }

    @Override
    public Map<String, Privilege> getSupportedPrivileges(SessionInfo sessionInfo, NodeId nodeId, NamePathResolver npResolver) throws RepositoryException {
        PrivilegeDefinition[] pDefs = service.getSupportedPrivileges(sessionInfo, nodeId);
        Map<String, Privilege> privilegeMap = new HashMap<String, Privilege>(pDefs.length);
        for (PrivilegeDefinition def : pDefs) {
            Privilege p = new PrivilegeImpl(def, pDefs, npResolver);
            privilegeMap.put(p.getName(), p);
        }
        return privilegeMap;
    }

    @Override
    public Set<Privilege> getPrivileges(SessionInfo sessionInfo, NodeId id, NamePathResolver npResolver) throws RepositoryException {
        PrivilegeDefinition[] defs = service.getPrivileges(sessionInfo, id);
        Set<Privilege> privileges = new HashSet<Privilege>(defs.length);
        for (PrivilegeDefinition def : defs) {
            Privilege p = new PrivilegeImpl(def, defs, npResolver);
            privileges.add(p);
        }
        return privileges;        
    }

    @Override
    public AccessControlManager createAccessControlManager(
            SessionInfo sessionInfo, 
            UpdatableItemStateManager itemStateManager,
            ItemManager itemManager,
            ItemDefinitionProvider definitionProvider,
            HierarchyManager hierarchyManager, NamePathResolver npResolver) throws RepositoryException {
        return new AccessControlManagerImpl(sessionInfo, itemStateManager, definitionProvider, hierarchyManager, npResolver, service.getQValueFactory(), this);
    }

    //--------------------------------------------------------------------------
    private void readPrivilegesFromService(SessionInfo sessionInfo, NamePathResolver resolver) throws RepositoryException {
        PrivilegeDefinition[] defs = service.getPrivilegeDefinitions(sessionInfo);
        for (PrivilegeDefinition d : defs) {
            privileges.put(resolver.getJCRName(d.getName()), new PrivilegeImpl(d, defs, resolver));
        }
    }
}
