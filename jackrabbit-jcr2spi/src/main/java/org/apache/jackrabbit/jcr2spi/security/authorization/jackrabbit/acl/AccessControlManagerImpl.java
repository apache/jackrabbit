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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.commons.iterator.AccessControlPolicyIteratorAdapter;
import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.AccessControlConstants;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * Jackrabbit-core specific implementation of the {@code AccessControlManager}.
 */
class AccessControlManagerImpl implements AccessControlManager, AccessControlConstants {

    private final SessionInfo sessionInfo;
    private final HierarchyManager hierarchyManager;
    private final NamePathResolver npResolver;
    private final QValueFactory qvf;
    private final AccessControlProvider acProvider;

    AccessControlManagerImpl(SessionInfo sessionInfo,
                             UpdatableItemStateManager itemStateMgr,
                             ItemManager itemManager,
                             ItemDefinitionProvider definitionProvider,
                             HierarchyManager hierarchyManager,
                             NamePathResolver npResolver,
                             QValueFactory qvf,
                             AccessControlProvider acProvider) {
        this.sessionInfo = sessionInfo;
        this.hierarchyManager = hierarchyManager;
        this.npResolver = npResolver;
        this.qvf = qvf;
        this.acProvider = acProvider;
    }

    public Privilege[] getSupportedPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        NodeState state = getNodeState(getQPath(absPath));
        Map<String, Privilege> privileges = acProvider.getSupportedPrivileges(sessionInfo, state.getNodeId(), npResolver);
        return privileges.values().toArray(new Privilege[privileges.size()]);
    }

    public Privilege privilegeFromName(String privilegeName) throws AccessControlException, RepositoryException {
        return acProvider.privilegeFromName(sessionInfo, npResolver, privilegeName);
    }

    public boolean hasPrivileges(String absPath, Privilege[] privileges) throws PathNotFoundException, RepositoryException {
        Set<Privilege> privs = acProvider.getPrivileges(sessionInfo, getNodeState(getQPath(absPath)).getNodeId());
        return privs.containsAll(Arrays.asList(privileges));
    }

    public Privilege[] getPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        Set<Privilege> privs = acProvider.getPrivileges(sessionInfo, getNodeState(getQPath(absPath)).getNodeId());
        return privs.toArray(new Privilege[privs.size()]);
    }

    public AccessControlPolicy[] getEffectivePolicies(String absPath) throws RepositoryException {
        // TODO
        throw new UnsupportedRepositoryOperationException("not yet implemented");
    }

    public AccessControlPolicyIterator getApplicablePolicies(String absPath) throws RepositoryException {
        checkValidNodePath(absPath);

        AccessControlPolicy[] applicable = getApplicable(absPath);
        if (applicable != null) {
            return new AccessControlPolicyIteratorAdapter(Arrays.asList(applicable));
        } else {
            return AccessControlPolicyIteratorAdapter.EMPTY;
        }
    }

    public AccessControlPolicy[] getPolicies(String absPath) throws RepositoryException {
        List<AccessControlList> policies = new ArrayList<AccessControlList>();
        NodeState aclNode = getAclNode(absPath);
        final AccessControlList acl;
        
        if (aclNode != null) {
            acl = createPolicy(aclNode, absPath);
            policies.add(acl);
        }
        return policies.toArray(new AccessControlList[policies.size()]);
    }
     
    public void setPolicy(String absPath, AccessControlPolicy policy) throws RepositoryException {
        // TODO
        throw new UnsupportedRepositoryOperationException("not yet implemented");
    }
        
    public void removePolicy(String absPath, AccessControlPolicy policy) throws RepositoryException {
        // TODO
        throw new UnsupportedRepositoryOperationException("not yet implemented");
    }

    //--------------------------------------------------< private >---
    private AccessControlPolicy[] getApplicable(String absPath) throws RepositoryException {
        NodeState controlledState;
        if (absPath == null) {
            controlledState = getRootNodeState();
        } else {
            controlledState = getNodeState(absPath);
        }
        
        AccessControlPolicy acl = null;
        NodeState aclNode = getAclNode(controlledState, absPath);
        if (aclNode == null) {
            acl = new AccessControlListImpl(absPath, npResolver, qvf);
        }
        
        return (acl == null) ? new AccessControlPolicy[0] : new AccessControlPolicy[] {acl};
    }

    /**
     * Creates an AccessControlList policy for the given acl node.
     * @param aclNode the node for which the policy is to be created for.
     * @param aclPath the policy path.
     * @return
     */
    private AccessControlListImpl createPolicy(NodeState aclNode, String aclPath) throws RepositoryException {
        return new AccessControlListImpl(aclNode, aclPath, npResolver, qvf, this);
    }

    private NodeState getAclNode(String controlledNodePath) throws RepositoryException {
        NodeState controlledNode;
        if (controlledNodePath == null) {
            controlledNode = getRootNodeState();
        } else {
            controlledNode = getNodeState(controlledNodePath);
        }
        return getAclNode(controlledNode, controlledNodePath);
    }
    
    private NodeState getAclNode(NodeState aclNode, String controlledNodePath) throws RepositoryException {
        NodeState acl = null;
        if (controlledNodePath == null) {
            if (isRepoAccessControlled(aclNode)) {
                acl = aclNode.getChildNodeState(N_REPO_POLICY, 1);
            }
        } else {
            if (isAccessControlled(aclNode)) {
                acl = aclNode.getChildNodeState(N_POLICY, 1);
            }
        }
        return acl;
    }

    private Path getQPath(String name) throws RepositoryException {
        try {
            return npResolver.getQPath(name);
        } catch(NamespaceException e) {
            throw new RepositoryException(e.getMessage());
        }
    }
        
    /**
     * Test if the given node state is of node type
     * {@link AccessControlConstants#NT_REP_REPO_ACCESS_CONTROLLABLE}
     * and if it has a child node named
     * {@link AccessControlConstants#N_REPO_POLICY}.
     *
     * @param      nodeState the node state to be tested
     * @return     <code>true</code> if the node is access controlled and has a
     *             rep:policy child; <code>false</code> otherwise.
     * @throws     RepositoryException if an error occurs
     */
    private boolean isRepoAccessControlled(NodeState nodeState) throws RepositoryException {
        return isNodeType(nodeState, NT_REP_REPO_ACCESS_CONTROLLABLE) &&
               nodeState.hasChildNodeEntry(N_REPO_POLICY, 1);
    }
 
    private boolean isAccessControlled(NodeState nodeState) throws RepositoryException {
        return isNodeType(nodeState, NT_REP_ACCESS_CONTROLLABLE) &&
               nodeState.hasChildNodeEntry(N_POLICY, 1);
    }
    
    /**
     * Checks if the given node state has the specified mixin.
     * NOTE: we take the transiently added mixins
     *       into consideration e.g if added during
     *       a setPolicies call and the changes are yet to be saved.
     * @param nodeState
     * @param mixinName
     */
    private boolean isNodeType(NodeState nodeState, Name mixinName) throws RepositoryException {
        List<Name> lst = Arrays.asList(nodeState.getAllNodeTypeNames());
        return (lst == null) ? false : lst.contains(mixinName);
    }

    private NodeState getNodeState(String nodePath) throws RepositoryException {
        return getNodeState(getQPath(nodePath));
    }
    
    private NodeState getRootNodeState() throws RepositoryException {
        try {
            return getHierarchyManager().getRootEntry().getNodeState();
        } catch (ItemNotFoundException e) {
            throw new RepositoryException(e.getMessage());
        }
    }
    
    private NodeState getNodeState(Path qPath) throws RepositoryException {
        return getHierarchyManager().getNodeState(qPath);
    }
    
    private void checkValidNodePath(String absPath) throws PathNotFoundException, RepositoryException {
        if (absPath != null) {
            Path qPath = getQPath(absPath);
            if (!qPath.isAbsolute()) {
                throw new RepositoryException("Absolute path expected. Found: " + absPath);
            }

            if (getHierarchyManager().getNodeEntry(qPath).getNodeState() == null) {
                throw new PathNotFoundException(absPath);
            }
        }
    }

    private HierarchyManager getHierarchyManager() {
        return hierarchyManager;
    }
}
