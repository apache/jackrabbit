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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.commons.iterator.AccessControlPolicyIteratorAdapter;
import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.operation.AddNode;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.Remove.RemovePolicy;
import org.apache.jackrabbit.jcr2spi.operation.SetMixin;
import org.apache.jackrabbit.jcr2spi.operation.SetPolicy;
import org.apache.jackrabbit.jcr2spi.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.AccessControlConstants;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jackrabbit-core specific implementation of the {@code AccessControlManager}.
 */
class AccessControlManagerImpl implements AccessControlManager, AccessControlConstants {

    private static final Logger log = LoggerFactory.getLogger(AccessControlManagerImpl.class);
    
    private final SessionInfo sessionInfo;
    private final HierarchyManager hierarchyManager;
    private final NamePathResolver npResolver;
    private final QValueFactory qvf;
    private final AccessControlProvider acProvider;
    private final UpdatableItemStateManager itemStateMgr;
    private final ItemDefinitionProvider definitionProvider;
    
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
        this.itemStateMgr = itemStateMgr;
        this.npResolver = npResolver;
        this.qvf = qvf;
        this.acProvider = acProvider;
        this.definitionProvider = definitionProvider;
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
        Set<Privilege> privs = acProvider.getPrivileges(sessionInfo, getNodeState(getQPath(absPath)).getNodeId(), npResolver);
        return privs.containsAll(Arrays.asList(privileges));
    }

    public Privilege[] getPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        Set<Privilege> privs = acProvider.getPrivileges(sessionInfo, getNodeState(getQPath(absPath)).getNodeId(), npResolver);
        return privs.toArray(new Privilege[privs.size()]);
    }

    public AccessControlPolicy[] getEffectivePolicies(String absPath) throws RepositoryException {
        // TODO
        throw new UnsupportedRepositoryOperationException("not yet implemented");
    }

    public AccessControlPolicyIterator getApplicablePolicies(String absPath) throws RepositoryException {
        checkValidNodePath(absPath);

        AccessControlPolicy[] applicable = getApplicable(absPath);
        if (applicable != null && applicable.length > 0) {
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
        checkValidNodePath(absPath);
        checkValidPolicy(policy);
        checkAcccessControlItem(absPath);
        
        NodeState acl = createResourceBaseACL(absPath, (AccessControlList) policy);   
        Operation sp = SetPolicy.create(acl);
        execute(sp);
    }
        
    public void removePolicy(String absPath, AccessControlPolicy policy) throws RepositoryException {
        checkValidNodePath(absPath);
        checkValidPolicy(policy);
        
        NodeState aclNode = getAclNode(absPath);        
        if (aclNode != null) {
            removeNode(aclNode);
        } else {
            throw new AccessControlException("No policy exist at "+absPath);
        }
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
        } catch (NamespaceException e) {
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

    /**
     * Checks whether if the given nodePath points to an access
     * control policy or entry node.
     * @param nodePath
     * @throws AccessControlException
     * @throws RepositoryException
     */
    private void checkAcccessControlItem(String nodePath) throws AccessControlException, RepositoryException {
        NodeState controlledState = getNodeState(nodePath);
        Name ntName = controlledState.getNodeTypeName();
        boolean isAcItem =  ntName.equals(NT_REP_ACL) ||
                            ntName.equals(NT_REP_GRANT_ACE) ||
                            ntName.equals(NT_REP_DENY_ACE);
        if (isAcItem) {
            throw new AccessControlException("The path: "+nodePath+" points to an access control content node");
        }

    }
    
    private NodeState createResourceBaseACL(String absPath, AccessControlList policy) throws AccessControlException, RepositoryException {
        NodeState aclNode = getAclNode(absPath);
        if (aclNode == null) {
            // policy node doesn't exist at absPath -> create one.
            Name ntName = (absPath == null) ? N_REPO_POLICY : N_POLICY;
            aclNode = createAclNode(absPath, ntName, NT_REP_ACL);
       } else {
            Iterator<NodeEntry> it = getNodeEntry(aclNode).getNodeEntries();
            while(it.hasNext()) {
                it.next().transientRemove();
            }
        }

        // create the entry nodes
        for (AccessControlEntry entry : policy.getAccessControlEntries()) {
            createAceNode(aclNode, entry);
        }
       return aclNode;
    }
    
    private NodeState createAclNode(String nodePath, Name nodeName, Name nodeTypeName) throws RepositoryException {
        NodeState parent = null;
        Name mixinType = null;
        if (nodePath == null) {
            parent = getRootNodeState();
            mixinType = NT_REP_REPO_ACCESS_CONTROLLABLE;
        } else {
            parent = getNodeState(nodePath);
            mixinType = NT_REP_ACCESS_CONTROLLABLE;
        }

        setMixin(parent, mixinType);

        String uuid = null;
        Name[] allNodeTypeNames = parent.getAllNodeTypeNames();
        QNodeDefinition def = getQNodeDefinition(allNodeTypeNames, nodeName, nodeTypeName);

        NodeState aclNode = addNode(parent, nodeName, uuid, nodeTypeName, def);
        return aclNode;
    }
    
    private void createAceNode(NodeState parentState, AccessControlEntry entry) throws RepositoryException {
        AccessControlEntryImpl ace = (AccessControlEntryImpl) entry;
        
        String uuid = null;
        boolean isAllow = ace.isAllow();
        QItemDefinition definition = null;
        Name nodeName = getUniqueNodeName(parentState, (isAllow) ? "allow" : "deny");
        Name nodeTypeName = (isAllow) ? NT_REP_GRANT_ACE : NT_REP_DENY_ACE;
        
        definition = getQNodeDefinition(parentState.getAllNodeTypeNames(), nodeName, nodeTypeName);
        
        NodeState aceNode = addNode(parentState, nodeName, uuid, nodeTypeName, definition);
               
        // add rep:principalName property
        String valueStr = ace.getPrincipal().getName();
        QValue value = qvf.create(valueStr, PropertyType.STRING);
        definition = getQPropertyDefinition(aceNode.getAllNodeTypeNames(), N_REP_PRINCIPAL_NAME, PropertyType.STRING);  
        addProperty(aceNode, N_REP_PRINCIPAL_NAME, definition, new QValue[] {value}, PropertyType.STRING);

        // add rep:privileges MvProperty
        Privilege[] privs = ace.getPrivileges();
        QValue[] vls = new QValue[privs.length];
        Name privilegeName = null;
        try {
            for (int i = 0; i < privs.length; i++) {
                privilegeName = getQName(privs[i].getName());
                vls[i] = qvf.create(privilegeName.toString(), PropertyType.NAME);
            }            
        } catch (ValueFormatException e) {
            throw new RepositoryException(e.getMessage());
        }

        definition = getQPropertyDefinition(aceNode.getAllNodeTypeNames(), N_REP_PRIVILEGES, PropertyType.NAME);
        addProperty(aceNode, N_REP_PRIVILEGES, definition, vls, PropertyType.NAME);

        // TODO: add rep:glob property            
    }
    
    private NodeState getNodeState(String nodePath) throws RepositoryException {
        return getNodeState(getQPath(nodePath));
    }
    
    private Name getQName(String name) throws RepositoryException {
        try {
            return npResolver.getQName(name);
        } catch(NamespaceException e) {
            throw new RepositoryException(e.getMessage());
        }
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
    
    private NodeEntry getNodeEntry(NodeState nodeState) throws RepositoryException {
        return getHierarchyManager().getNodeEntry(nodeState.getPath());
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

    private void checkValidPolicy(AccessControlPolicy policy) throws AccessControlException {
        if (policy == null || !(policy instanceof AccessControlListImpl)) {
            throw new AccessControlException("Policy is not applicable ");
        }
    }
    
    private NodeState addNode(NodeState parent, Name nodeName, String uuid, Name nodeTypeName, QItemDefinition def) throws RepositoryException {
        Operation sp = SetPolicy.createAddNode(parent, nodeName, nodeTypeName, uuid);
        execute(sp);
        return (NodeState) ((AddNode) sp).getAddedStates().get(0);
    }
    
    private void addProperty(NodeState parent, Name propName, QItemDefinition def, QValue[] values, int propType) throws RepositoryException {
        Operation ap = SetPolicy.createAddProperty(parent, propName, propType, values, (QPropertyDefinition) def);
        execute(ap);
    }
    
    private void removeNode(NodeState aclNode) throws RepositoryException {        
        Operation removePolicy = RemovePolicy.create(aclNode);
        execute(removePolicy);
    }
    
    private void setMixin(NodeState parent, Name mixinName) throws RepositoryException {
        if (!isNodeType(parent, mixinName)){
            Operation sm = SetMixin.create(parent, new Name[]{mixinName});
            execute(sm);
         } else {
             log.debug(mixinName.toString()+" is already present on the given node state "+parent.getName().toString());
         }
    }
    
    private void execute(Operation op) throws RepositoryException {
        getItemStateManager().execute(op);
    }
    
    // copied from jackrabbit-core ACLEditor
    /**
     * Create a unique valid name for the Permission nodes to be save.
     *
     * @param node a name for the child is resolved
     * @param name if missing the {@link #DEFAULT_ACE_NAME} is taken
     * @return the name
     * @throws RepositoryException if an error occurs
     */
    private Name getUniqueNodeName(NodeState node, String name) throws RepositoryException {
            
        try {        
            NameParser.checkFormat(name);
        } catch (NameException e) {                        
            log.debug("Invalid path name for Permission: " + name + ".");       
        }

        int i = 0;
        String check = name;
        while (node.hasChildNodeEntry(getQName(check), 1)) {
            check = name + i;
            i++;
        }
        return getQName(check);
    }
    
    private UpdatableItemStateManager getItemStateManager() {
        return itemStateMgr;
    }
    
    private HierarchyManager getHierarchyManager() {
        return hierarchyManager;
    }
    
    private ItemDefinitionProvider getDefinitionProvider() {
        return definitionProvider;
    }
    
    private QNodeDefinition getQNodeDefinition(Name[] nodeTypeNames, Name nodeName, Name nodeTypeName) throws RepositoryException {
        return getDefinitionProvider().getQNodeDefinition(nodeTypeNames, nodeName, nodeTypeName);
    }
    
    private QPropertyDefinition getQPropertyDefinition(Name[] nodeTypes, Name propName, int propType) throws RepositoryException {
        return getDefinitionProvider().getQPropertyDefinition(nodeTypes, propName, propType);
    }
}
