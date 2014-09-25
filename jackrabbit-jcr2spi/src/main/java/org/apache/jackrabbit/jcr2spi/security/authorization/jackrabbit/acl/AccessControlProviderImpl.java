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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import junit.framework.Assert;

import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AccessControlProviderImpl... TODO
 */
class AccessControlProviderImpl implements AccessControlProvider {

    private static final Logger log = LoggerFactory.getLogger(AccessControlProviderImpl.class);

    private RepositoryService service;
    
    public AccessControlProviderImpl(RepositoryService service) {
        this.service = service;
    }

    public Map<String, Privilege> getSupportedPrivileges(SessionInfo sessionInfo, NodeId nodeId, NamePathResolver npResolver) throws RepositoryException {
        PrivilegeDefinition[] pDefs = service.getSupportedPrivileges(sessionInfo, nodeId);
        Map<String, Privilege> privilegeMap = new HashMap<String, Privilege>(pDefs.length);
        for (PrivilegeDefinition def : pDefs) {
            Privilege p = new PrivilegeImpl(def, pDefs, npResolver);
            privilegeMap.put(p.getName(), p);
        }
        return privilegeMap;
    }

    public boolean isGranted(SessionInfo sessionInfo, ItemId itemId, String[] actions) throws RepositoryException {
        return service.isGranted(sessionInfo, itemId, actions);
    }
    
    public QValueFactory getQValueFactory() throws RepositoryException {
        return service.getQValueFactory();
    }
    
    @Override
    public AccessControlManager createAccessControlManager(
            SessionInfo sessionInfo, 
            UpdatableItemStateManager itemStateManager,
            ItemManager itemManager,
            ItemDefinitionProvider definitionProvider,
            HierarchyManager hierarchyManager, NamePathResolver npResolver) throws RepositoryException {
        return new AccessControlManagerImpl(sessionInfo, itemStateManager, itemManager, definitionProvider, hierarchyManager, npResolver, this);
    }
}