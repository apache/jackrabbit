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
package org.apache.jackrabbit.core.xml;

import static org.apache.jackrabbit.core.security.authorization.AccessControlConstants.NT_REP_ACCESS_CONTROL;
import static org.apache.jackrabbit.core.security.authorization.AccessControlConstants.NT_REP_PRINCIPAL_ACCESS_CONTROL;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.jcr.AccessDeniedException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.principal.UnknownPrincipal;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AccessControlImporter</code> implements a
 * <code>ProtectedNodeImporter</code> that is able to deal with access control
 * content as defined by the default ac related node types present with
 * jackrabbit-core.
 */
public class AccessControlImporter extends DefaultProtectedNodeImporter {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(AccessControlImporter.class);

    private static final int STATUS_UNDEFINED = 0;
    private static final int STATUS_AC_FOLDER = 1;
    private static final int STATUS_PRINCIPAL_AC = 2;
    private static final int STATUS_ACL = 3;
    private static final int STATUS_ACE = 4;

    private static final Set<Name> ACE_NODETYPES = new HashSet<Name>(2);
    static {
        ACE_NODETYPES.add(AccessControlConstants.NT_REP_DENY_ACE);
        ACE_NODETYPES.add(AccessControlConstants.NT_REP_GRANT_ACE);
    }

    private final Stack<Integer> prevStatus = new Stack<Integer>();
    
    private AccessControlManager acMgr;
    private int status = STATUS_UNDEFINED;
    private NodeImpl parent = null;

    private boolean principalbased = false;

    private boolean initialized = false;

    // keep best-effort for backward compatibility reasons
    private ImportBehavior importBehavior = ImportBehavior.BEST_EFFORT;

    /**
     * the ACL for non-principal based
     */
    private JackrabbitAccessControlList acl = null;

    @Override
    public boolean init(JackrabbitSession session, NamePathResolver resolver,
                        boolean isWorkspaceImport, int uuidBehavior,
                        ReferenceChangeTracker referenceTracker) {
        if (super.init(session, resolver, isWorkspaceImport, uuidBehavior, referenceTracker)) {
            if (initialized) {
                throw new IllegalStateException("Already initialized");
            }
            try {
                acMgr = session.getAccessControlManager();
                initialized = true;
            } catch (RepositoryException e) {
                // initialization failed. ac-import not possible
            }
        }
        return initialized;
    }

    @Override
    public boolean start(NodeImpl protectedParent) throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized");
        }
        if (isStarted()) {
            // only ok if same parent
            if (!protectedParent.isSame(parent)) {
                throw new IllegalStateException();
            }
            return true;
        }
        if (isWorkspaceImport) {
            log.debug("AccessControlImporter may not be used with the WorkspaceImporter");
            return false;
        }
        if (!protectedParent.getDefinition().isProtected()) {
            log.debug("AccessControlImporter may not be started with a non-protected parent.");
            return false;
        }

        if (isPolicyNode(protectedParent)) {
            String parentPath = protectedParent.getParent().getPath();
            acl = getACL(parentPath);
            if (acl == null) {
                log.warn("AccessControlImporter cannot be started: no ACL for {}.", parentPath);
                return false;
            }
            status = STATUS_ACL;
        } else if (isRepoPolicyNode(protectedParent)) {
            acl = getACL(null);
            if (acl == null) {
                log.warn("AccessControlImporter cannot be started: no Repo ACL.");
                return false;
            }
            status = STATUS_ACL;
        } else if (protectedParent.isNodeType(AccessControlConstants.NT_REP_ACCESS_CONTROL)) {
            status = STATUS_AC_FOLDER;
            principalbased = true;
            acl = null;
        } // else: nothing this importer can deal with.

        if (isStarted()) {
            parent = protectedParent;
            return true;
        } else {
            return false;
        }
    }

    private JackrabbitAccessControlList getACL(String path) throws RepositoryException, AccessDeniedException {
        JackrabbitAccessControlList acl = null;
        for (AccessControlPolicy p: acMgr.getPolicies(path)) {
            if (p instanceof JackrabbitAccessControlList) {
                acl = (JackrabbitAccessControlList) p;
                break;
            }
        }
        if (acl != null) {
            // clear all existing entries
            for (AccessControlEntry ace: acl.getAccessControlEntries()) {
                acl.removeAccessControlEntry(ace);
            }
        }
        return acl;
    }

    @Override
    public boolean start(NodeState protectedParent) throws IllegalStateException, RepositoryException {
        if (isStarted()) {
            throw new IllegalStateException();
        }
        if (isWorkspaceImport) {
            log.debug("AccessControlImporter may not be used with the WorkspaceImporter");
            return false;
        }
        return false;
    }

    @Override
    public void end(NodeImpl protectedParent) throws RepositoryException {
        if (!isStarted()) {
            return;
        }

        if (!principalbased) {
            checkStatus(STATUS_ACL, "STATUS_ACL expected.");
            acMgr.setPolicy(acl.getPath(), acl);
        } else {
            checkStatus(STATUS_AC_FOLDER, "STATUS_AC_FOLDER expected.");
            if (!prevStatus.isEmpty()) {
                throw new ConstraintViolationException("Incomplete protected item tree: "+ prevStatus.size()+ " calls to 'endChildInfo' missing.");
            }
        }
        reset();
    }

    @Override
    public void end(NodeState protectedParent) throws IllegalStateException, ConstraintViolationException, RepositoryException {
        // nothing to do. will never get here.
    }

    @Override
    public void startChildInfo(NodeInfo childInfo, List<PropInfo> propInfos) throws RepositoryException {
        if (!isStarted()) {
            return;
        }
        
        Name ntName = childInfo.getNodeTypeName();
        int previousStatus = status;

        if (!principalbased) {
            checkStatus(STATUS_ACL, "Cannot handle childInfo " + childInfo + "; rep:ACL may only contain a single level of child nodes representing the ACEs");
            addACE(childInfo, propInfos);
            status = STATUS_ACE;
        } else {
            switch (status) {
                case STATUS_AC_FOLDER:
                    if (NT_REP_ACCESS_CONTROL.equals(ntName)) {
                        // yet another intermediate node -> keep status
                        status = STATUS_AC_FOLDER;
                    } else if (NT_REP_PRINCIPAL_ACCESS_CONTROL.equals(ntName)) {
                        // the start of a principal-based acl
                        status = STATUS_PRINCIPAL_AC;
                    } else {
                        // illegal node type -> throw exception
                        throw new ConstraintViolationException("Unexpected node type " + ntName + ". Should be rep:AccessControl or rep:PrincipalAccessControl.");
                    }
                    checkIdMixins(childInfo);
                    break;
                case STATUS_PRINCIPAL_AC:
                    if (NT_REP_ACCESS_CONTROL.equals(ntName)) {
                        // some intermediate path between principal paths.
                        status = STATUS_AC_FOLDER;
                    } else if (NT_REP_PRINCIPAL_ACCESS_CONTROL.equals(ntName)) {
                        // principal-based ac node underneath another one -> keep status
                        status = STATUS_PRINCIPAL_AC;
                    } else {
                        // the start the acl definition itself
                        checkDefinition(childInfo, AccessControlConstants.N_POLICY, AccessControlConstants.NT_REP_ACL);
                        status = STATUS_ACL;
                    }
                    checkIdMixins(childInfo);
                    break;
                case STATUS_ACL:
                    // nodeinfo must define an ACE
                    addACE(childInfo, propInfos);
                    status = STATUS_ACE;
                    break;
                default:
                    throw new ConstraintViolationException("Cannot handle childInfo " + childInfo + "; unexpected status " + status + " .");

            }
        }
        prevStatus.push(previousStatus);        
    }

    @Override
    public void endChildInfo() throws RepositoryException {
        if (!isStarted()) {
            return;
        }

        // if the protected imported is started at an existing protected node
        // SessionImporter does not remember it on the stack of parents node.
        if (!principalbased) {
            // childInfo + props have already been handled
            // -> assert valid status
            // -> no further actions required.
            checkStatus(STATUS_ACE, "Upon completion of a NodeInfo the status must be STATUS_ACE.");
        }

        // reset the status
        status = prevStatus.pop();
    }

    private boolean isStarted() {
        return status > STATUS_UNDEFINED;
    }
    
    private void reset() {
        status = STATUS_UNDEFINED;
        parent = null;
        acl = null;
    }

    private void checkStatus(int expectedStatus, String message) throws ConstraintViolationException {
        if (status != expectedStatus) {
            throw new ConstraintViolationException(message);
        }
    }

    private static boolean isPolicyNode(NodeImpl node) throws RepositoryException {
        Name nodeName = node.getQName();
        return AccessControlConstants.N_POLICY.equals(nodeName) && node.isNodeType(AccessControlConstants.NT_REP_ACL);
    }

    /**
     * @param node The node to be tested.
     * @return <code>true</code> if the specified node is the 'rep:repoPolicy'
     * acl node underneath the root node; <code>false</code> otherwise.
     * @throws RepositoryException If an error occurs.
     */
    private static boolean isRepoPolicyNode(NodeImpl node) throws RepositoryException {
        Name nodeName = node.getQName();
        return AccessControlConstants.N_REPO_POLICY.equals(nodeName) &&
                node.isNodeType(AccessControlConstants.NT_REP_ACL) &&
                node.getDepth() == 1;
    }

    private static void checkDefinition(NodeInfo nInfo, Name expName, Name expNodeTypeName) throws ConstraintViolationException {
        if (expName != null && !expName.equals(nInfo.getName())) {
            // illegal name
            throw new ConstraintViolationException("Unexpected Node name "+ nInfo.getName() +". Node name should be " + expName + ".");
        }
        if (expNodeTypeName != null && !expNodeTypeName.equals(nInfo.getNodeTypeName())) {
            // illegal name
            throw new ConstraintViolationException("Unexpected node type " + nInfo.getNodeTypeName() + ". Node type should be " + expNodeTypeName + ".");
        }
    }

    private static void checkIdMixins(NodeInfo nInfo) throws ConstraintViolationException {
        // neither explicit id NOR mixin types may be present.
        Name[] mixins = nInfo.getMixinNames();
        NodeId id = nInfo.getId();
        if (id != null || mixins != null) {
            throw new ConstraintViolationException("The node represented by NodeInfo " + nInfo + " may neither be referenceable nor have mixin types.");
        }
    }

    private void addACE(NodeInfo childInfo, List<PropInfo> propInfos) throws RepositoryException, UnsupportedRepositoryOperationException {

        // node type may only be rep:GrantACE or rep:DenyACE
        Name ntName = childInfo.getNodeTypeName();
        if (!ACE_NODETYPES.contains(ntName)) {
            throw new ConstraintViolationException("Cannot handle childInfo " + childInfo + "; expected a valid, applicable rep:ACE node definition.");
        }

        checkIdMixins(childInfo);

        boolean isAllow = AccessControlConstants.NT_REP_GRANT_ACE.equals(ntName);
        Principal principal = null;
        Privilege[] privileges = null;
        Map<String, TextValue> restrictions = new HashMap<String, TextValue>();

        for (PropInfo pInfo : propInfos) {
            Name name = pInfo.getName();
            if (AccessControlConstants.P_PRINCIPAL_NAME.equals(name)) {
                Value[] values = pInfo.getValues(PropertyType.STRING, resolver);
                if (values == null || values.length != 1) {
                    throw new ConstraintViolationException("");
                }
                String pName = values[0].getString();
                principal = session.getPrincipalManager().getPrincipal(pName);
                if (principal == null) {
                    if (importBehavior == ImportBehavior.BEST_EFFORT) {
                        // create "fake" principal that is always accepted in ACLTemplate.checkValidEntry()
                        principal = new UnknownPrincipal(pName);
                    } else {
                        // create "fake" principal. this is checked again in ACLTemplate.checkValidEntry()
                        principal = new PrincipalImpl(pName);
                    }
                }
            } else if (AccessControlConstants.P_PRIVILEGES.equals(name)) {
                Value[] values = pInfo.getValues(PropertyType.NAME, resolver);
                privileges = new Privilege[values.length];
                for (int i = 0; i < values.length; i++) {
                    privileges[i] = acMgr.privilegeFromName(values[i].getString());
                }
            } else {
                TextValue[] txtVls = pInfo.getTextValues();
                for (TextValue txtV : txtVls) {
                    restrictions.put(resolver.getJCRName(name), txtV);
                }
            }
        }

        if (principalbased) {
            // try to access policies
            List<AccessControlPolicy> policies = new ArrayList<AccessControlPolicy>();
            if (acMgr instanceof JackrabbitAccessControlManager) {
                JackrabbitAccessControlManager jacMgr = (JackrabbitAccessControlManager) acMgr;
                policies.addAll(Arrays.asList(jacMgr.getPolicies(principal)));
                policies.addAll(Arrays.asList(jacMgr.getApplicablePolicies(principal)));
            }
            for (AccessControlPolicy policy : policies) {
                if (policy instanceof JackrabbitAccessControlList) {
                    JackrabbitAccessControlList acl = (JackrabbitAccessControlList) policy;
                    Map<String, Value> restr = new HashMap<String, Value>();
                    for (String restName : acl.getRestrictionNames()) {
                        TextValue txtVal = restrictions.remove(restName);
                        if (txtVal != null) {
                            restr.put(restName, txtVal.getValue(acl.getRestrictionType(restName), resolver));
                        }
                    }
                    if (!restrictions.isEmpty()) {
                        throw new ConstraintViolationException("ACE childInfo contained restrictions that could not be applied.");
                    }
                    acl.addEntry(principal, privileges, isAllow, restr);
                    acMgr.setPolicy(acl.getPath(), acl);
                    return;
                }
            }
        } else {
            Map<String, Value> restr = new HashMap<String, Value>();
            for (String restName : acl.getRestrictionNames()) {
                TextValue txtVal = restrictions.remove(restName);
                if (txtVal != null) {
                    restr.put(restName, txtVal.getValue(acl.getRestrictionType(restName), resolver));
                }
            }
            if (!restrictions.isEmpty()) {
                throw new ConstraintViolationException("ACE childInfo contained restrictions that could not be applied.");
            }
            acl.addEntry(principal, privileges, isAllow, restr);
            return;
        }


        // could not apply the ACE. No suitable ACL found.
        throw new ConstraintViolationException("Cannot handle childInfo " + childInfo + "; No policy found to apply the ACE.");        
    }

    //---------------------------------------------------------< BeanConfig >---
    /**
     * @return human readable representation of the <code>importBehavior</code> value.
     */
    public String getImportBehavior() {
        return importBehavior.getString();
    }

    /**
     *
     * @param importBehaviorStr
     */
    public void setImportBehavior(String importBehaviorStr) {
        this.importBehavior = ImportBehavior.fromString(importBehaviorStr);
    }


    public static enum ImportBehavior {

        /**
         * Default behavior that does not try to prevent errors or incompatibilities between the content
         * and the ACL manager (eg. does not try to fix missing principals)
         */
        DEFAULT("default"),

        /**
         * Tries to minimize errors by adapting the content and bypassing validation checks (e.g. allows adding
         * ACEs with missing principals, even if ACL manager would not allow this).
         */
        BEST_EFFORT("bestEffort");

        private final String value;

        ImportBehavior(String value) {
            this.value = value;
        }

        public static ImportBehavior fromString(String str) {
            if (str.equals("bestEffort")) {
                return BEST_EFFORT;
            } else {
                return ImportBehavior.valueOf(str.toUpperCase());
            }
        }

        public String getString() {
            return value;
        }
    }
}
