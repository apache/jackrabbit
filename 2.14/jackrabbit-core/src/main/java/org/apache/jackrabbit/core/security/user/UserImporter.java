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
package org.apache.jackrabbit.core.security.user;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Impersonation;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.flat.PropertySequence;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionWriteOperation;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.xml.NodeInfo;
import org.apache.jackrabbit.core.xml.PropInfo;
import org.apache.jackrabbit.core.xml.ProtectedNodeImporter;
import org.apache.jackrabbit.core.xml.ProtectedPropertyImporter;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>UserImporter</code> implements a <code>DefaultProtectedPropertyImporter</code>
 * and <code>DefaultProtectedNodeImporter</code> that is able to deal with
 * user/group content as defined by the default user related node types present
 * with jackrabbit-core.
 * <p>
 * The importer is intended to be used by applications that import user content
 * extracted from another repository instance and immediately persist the
 * imported content using {@link javax.jcr.Session#save()}. Omitting the
 * save call will lead to transient, semi-validated user content and eventually
 * to inconsistencies.
 * <p>
 * Note the following restrictions:
 * <ul>
 * <li>The importer will only be initialized if the user manager is an instance
 * of <code>UserPerWorkspaceUserManager</code>.
 * </li>
 * <li>The importer will only be initialized if the editing session starting
 * this import is the same as the UserManager's Session instance.
 * </li>
 * <li>The jcr:uuid property of user and groups is defined to represent the
 * hashed authorizable id as calculated by the UserManager. This importer
 * is therefore not able to handle imports with
 * {@link ImportUUIDBehavior#IMPORT_UUID_CREATE_NEW}.</li>
 * <li>Importing user/group nodes outside of the hierarchy defined by
 * {@link org.apache.jackrabbit.core.security.user.UserManagerImpl#getUsersPath()}
 * and {@link org.apache.jackrabbit.core.security.user.UserManagerImpl#getGroupsPath()}
 * will fail upon save as the mandatory properties will not be imported. The same may
 * be true in case of {@link ImportUUIDBehavior#IMPORT_UUID_COLLISION_REPLACE_EXISTING}
 * inserting the user/group node at some other place in the node hierarchy.</li>
 * <li>While creating user/groups through the API the <code>UserManagerImpl</code> makes
 * sure that authorizables are never nested and are created below a hierarchy
 * of nt:AuthorizableFolder nodes. This isn't enforced by means of node type
 * constraints but only by the API. This importer currently doesn't perform such
 * a validation check.</li>
 * <li>Any attempt to import conflicting data will cause the import to fail
 * either immediately or upon calling {@link javax.jcr.Session#save()} with the
 * following exceptions:
 * <ul>
 * <li><code>rep:members</code> : Group membership</li>
 * <li><code>rep:impersonators</code> : Impersonators of a User.</li>
 * </ul>
 * The import behavior of these two properties is defined by the {@link #PARAM_IMPORT_BEHAVIOR}
 * configuration parameter, which can be set to
 * <ul>
 * <li>{@link ImportBehavior#NAME_IGNORE ignore}: A warning is logged.</li>
 * <li>{@link ImportBehavior#NAME_BESTEFFORT best effort}: A warning is logged
 * and the importer tries to fix the problem.</li>
 * <li>{@link ImportBehavior#NAME_ABORT abort}: The import is immediately
 * aborted with a ConstraintViolationException. (<strong>default</strong>)</li>
 * </ul>
 * </li>
 * </ul>
 * Known Issue:<br>
 * Importing <code>rep:impersonators</code> property referring to principals
 * that are created during this import AND have principalName different from the
 * ID will no succeed, as the validation in <code>ImpersonationImpl</code> isn't able
 * to find the authorizable with the given principal (reason: query will only
 * find persisted content).
 */
public class UserImporter implements ProtectedPropertyImporter, ProtectedNodeImporter {

    private static final Logger log = LoggerFactory.getLogger(UserImporter.class);

    /**
     * Parameter name for the import behavior configuration option.
     */
    public static final String PARAM_IMPORT_BEHAVIOR = "importBehavior";

    private JackrabbitSession session;

    private NamePathResolver resolver;

    private ReferenceChangeTracker referenceTracker;

    private UserPerWorkspaceUserManager userManager;

    private boolean initialized = false;

    private boolean resetAutoSave = false;

    private int importBehavior = ImportBehavior.IGNORE;

    /**
     * Container used to collect group members stored in protected nodes.
     */
    private Membership currentMembership;

    /**
     * Temporary store for the pw an imported new user to be able to call
     * the creation actions irrespective of the order of protected properties
     */
    private Map<String,String> currentPw = new HashMap<String,String>(1);

    public boolean init(JackrabbitSession session, NamePathResolver resolver,
                        boolean isWorkspaceImport,
                        int uuidBehavior, ReferenceChangeTracker referenceTracker) {

        this.session = session;
        this.resolver = resolver;
        this.referenceTracker = referenceTracker;

        if (initialized) {
            throw new IllegalStateException("Already initialized");
        }
        if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW) {
            log.debug("ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW isn't supported when importing users or groups.");
            return false;
        }
        if (isWorkspaceImport) {
            log.debug("Only Session-Import is supported when importing users or groups.");
            return false;
        }
        try {
            UserManager uMgr = session.getUserManager();
            if (uMgr instanceof UserPerWorkspaceUserManager) {
                // make sure the user managers autosave flag can be changed to false.
                if (uMgr.isAutoSave()) {
                    uMgr.autoSave(false);
                    resetAutoSave = true;
                    log.debug("Changed autosave behavior of UserManager to 'false'.");
                }
                userManager = (UserPerWorkspaceUserManager) uMgr;
                initialized = true;
            } else {
                // either wrong implementation or one that implicitly calls save.
                log.debug("Failed to initialize UserImporter: UserManager isn't instance of UserPerWorkspaceUserManager or does implicit save call.");
            }
        } catch (RepositoryException e) {
            // failed to access user manager or to set the autosave behavior
            // -> return false (not initialized) as importer can't operate.
            log.error("Failed to initialize UserImporter: ", e);
        }
        return initialized;
    }

    // -----------------------------------------< ProtectedPropertyImporter >---
    /**
     * @see ProtectedPropertyImporter#handlePropInfo(org.apache.jackrabbit.core.NodeImpl, org.apache.jackrabbit.core.xml.PropInfo, org.apache.jackrabbit.spi.QPropertyDefinition)
     */
    public boolean handlePropInfo(NodeImpl parent, PropInfo protectedPropInfo, QPropertyDefinition def) throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized");
        }

        /* importer can only handle protected properties below user/group
           nodes that are properly stored underneath the configured users/groups
           hierarchies (see {@link UserManagerImpl#getAuthorizable(NodeImpl)}.
           this prevents from importing user/group nodes somewhere in the
           content hierarchy which isn't possible when creating user/groups
           using the corresponding API calls  {@link UserManager#createUser} or
           {@link UserManager#createGroup} respectively. */
        Authorizable a = userManager.getAuthorizable(parent);
        if (a == null) {
            log.warn("Cannot handle protected PropInfo " + protectedPropInfo + ". Node " + parent + " doesn't represent a valid Authorizable.");
            return false;
        }

        // TODO: check if import should be aborted in case of nested authorizable.

        // assert that user manager is isn't in auto-save mode
        if (userManager.isAutoSave()) {
            userManager.autoSave(false);
        }
        try {
            Name propName = protectedPropInfo.getName();
            if (UserConstants.P_PRINCIPAL_NAME.equals(propName)) {
                // minimal validation that passed definition really matches the
                // protected rep:principalName property defined by rep:Authorizable.
                if (def.isMultiple() || !UserConstants.NT_REP_AUTHORIZABLE.equals(def.getDeclaringNodeType())) {
                    // some other unexpected property definition -> cannot handle
                    log.warn("Unexpected definition for property rep:principalName");
                    return false;
                }

                Value v = protectedPropInfo.getValues(PropertyType.STRING, resolver)[0];
                String princName = v.getString();
                userManager.setPrincipal(parent, new PrincipalImpl(princName));

                /*
                Execute authorizable actions for a NEW group as this is the
                same place in the userManager#createGroup that the actions
                are called.
                In case of a NEW user the actions are executed if the password
                has been imported before.
                */
                if (parent.isNew()) {
                    if (a.isGroup()) {
                        userManager.onCreate((Group) a);
                    } else if (currentPw.containsKey(a.getID())) {
                        userManager.onCreate((User) a, currentPw.remove(a.getID()));
                    }
                }

                return true;
            } else if (UserConstants.P_PASSWORD.equals(propName)) {
                if (a.isGroup()) {
                    log.warn("Expected parent node of type rep:User.");
                    return false;
                }
                // minimal validation of the passed definition
                if (def.isMultiple() || !UserConstants.NT_REP_USER.equals(def.getDeclaringNodeType())) {
                    // some other unexpected property definition -> cannot handle
                    log.warn("Unexpected definition for property rep:password");
                    return false;
                }

                Value v = protectedPropInfo.getValues(PropertyType.STRING, resolver)[0];
                String pw = v.getString();
                userManager.setPassword(parent, pw, false);

                /*
                 Execute authorizable actions for a NEW user at this point after
                 having set the password if the principal name has already been
                 processed, otherwise postpone it.
                 */
                if (parent.isNew()) {
                    if (parent.hasProperty(UserConstants.P_PRINCIPAL_NAME)) {
                        userManager.onCreate((User) a, pw);
                    } else {
                        // principal name not yet available -> remember the pw
                        currentPw.clear();
                        currentPw.put(a.getID(), pw);
                    }
                }
                return true;

            } else if (UserConstants.P_IMPERSONATORS.equals(propName)) {
                if (a.isGroup()) {
                    // unexpected parent type -> cannot handle
                    log.warn("Expected parent node of type rep:User.");
                    return false;
                }

                // minimal validation of the passed definition
                if (!def.isMultiple() || !UserConstants.MIX_REP_IMPERSONATABLE.equals(def.getDeclaringNodeType())) {
                    // some other unexpected property definition -> cannot handle
                    log.warn("Unexpected definition for property rep:impersonators");
                    return false;
                }

                // since impersonators may be imported later on, postpone processing
                // to the end.
                // see -> process References
                Value[] vs = protectedPropInfo.getValues(PropertyType.STRING, resolver);
                referenceTracker.processedReference(new Impersonators(a.getID(), vs));
                return true;

            } else if (UserConstants.P_DISABLED.equals(propName)) {
                if (a.isGroup()) {
                    log.warn("Expected parent node of type rep:User.");
                    return false;
                }
                // minimal validation of the passed definition
                if (def.isMultiple() || !UserConstants.NT_REP_USER.equals(def.getDeclaringNodeType())) {
                    // some other unexpected property definition -> cannot handle
                    log.warn("Unexpected definition for property rep:disabled");
                    return false;
                }

                Value v = protectedPropInfo.getValues(PropertyType.STRING, resolver)[0];
                ((User) a).disable(v.getString());

                return true;

            } else if (UserConstants.P_MEMBERS.equals(propName)) {
                if (!a.isGroup()) {
                    // unexpected parent type -> cannot handle
                    log.warn("Expected parent node of type rep:Group.");
                    return false;
                }

                // minimal validation of the passed definition
                if (!def.isMultiple() || !UserConstants.NT_REP_GROUP.equals(def.getDeclaringNodeType())) {
                    // some other unexpected property definition -> cannot handle
                    log.warn("Unexpected definition for property rep:members");
                    return false;
                }

                // since group-members are references to user/groups that potentially
                // are to be imported later on -> postpone processing to the end.
                // see -> process References
                Membership membership = new Membership(a.getID());
                for (Value v : protectedPropInfo.getValues(PropertyType.WEAKREFERENCE, resolver)) {
                    membership.addMember(new NodeId(v.getString()));
                }
                referenceTracker.processedReference(membership);
                return true;

            } // else: cannot handle -> return false

            return false;
        } finally {
            // reset the autosave mode of the user manager in order to restore
            // the original state.
            if (resetAutoSave) {
                userManager.autoSave(true);
            }
        }
    }

    /**
     * @see ProtectedPropertyImporter#handlePropInfo(org.apache.jackrabbit.core.NodeImpl, org.apache.jackrabbit.core.xml.PropInfo, org.apache.jackrabbit.spi.QPropertyDefinition)
     */
    public boolean handlePropInfo(NodeState parent, PropInfo protectedPropInfo, QPropertyDefinition def) throws RepositoryException {
        return false;
    }

    /**
     * @see org.apache.jackrabbit.core.xml.ProtectedPropertyImporter#processReferences()
     */
    public void processReferences() throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized");
        }

        // assert that user manager is isn't in auto-save mode
        if (userManager.isAutoSave()) {
            userManager.autoSave(false);
        }
        try {
            List<Object> processed = new ArrayList<Object>();
            for (Iterator<Object> it = referenceTracker.getProcessedReferences(); it.hasNext();) {
                Object reference = it.next();
                if (reference instanceof Membership) {
                    Authorizable a = userManager.getAuthorizable(((Membership) reference).groupId);
                    if (a == null || !a.isGroup()) {
                        throw new RepositoryException(((Membership) reference).groupId + " does not represent a valid group.");
                    }

                    final Group gr = (Group) a;
                    // 1. collect members to add and to remove.
                    Map<String, Authorizable> toRemove = new HashMap<String, Authorizable>();
                    for (Iterator<Authorizable> declMembers = gr.getDeclaredMembers(); declMembers.hasNext();) {
                        Authorizable dm = declMembers.next();
                        toRemove.put(dm.getID(), dm);
                    }

                    List<Authorizable> toAdd = new ArrayList<Authorizable>();
                    final List<Membership.Member> nonExisting = new ArrayList<Membership.Member>();

                    for (Membership.Member member : ((Membership) reference).members) {
                        NodeId remapped = referenceTracker.getMappedId(member.id);
                        NodeId id = (remapped == null) ? member.id : remapped;

                        Authorizable authorz = null;
                        try {
                            NodeImpl n = ((SessionImpl) session).getNodeById(id);
                            authorz = userManager.getAuthorizable(n);
                        } catch (RepositoryException e) {
                            // no such node or failed to retrieve authorizable
                            // warning is logged below.
                        }
                        if (authorz != null) {
                            if (toRemove.remove(authorz.getID()) == null) {
                                toAdd.add(authorz);
                            } // else: no need to remove from rep:members
                        } else {
                            handleFailure("New member of " + gr + ": No such authorizable (NodeID = " + id + ")");
                            if (importBehavior == ImportBehavior.BESTEFFORT) {
                                log.info("ImportBehavior.BESTEFFORT: Remember non-existing member for processing.");
                                nonExisting.add(member);
                            }
                        }
                    }

                    // 2. adjust members of the group
                    for (Authorizable m : toRemove.values()) {
                        if (!gr.removeMember(m)) {
                            handleFailure("Failed remove existing member (" + m + ") from " + gr);
                        }
                    }
                    for (Authorizable m : toAdd) {
                        if (!gr.addMember(m)) {
                            handleFailure("Failed add member (" + m + ") to " + gr);
                        }
                    }

                    // handling non-existing members in case of best-effort
                    if (!nonExisting.isEmpty()) {
                        log.info("ImportBehavior.BESTEFFORT: Found " + nonExisting.size() + " entries of rep:members pointing to non-existing authorizables. Adding to rep:members.");
                        final NodeImpl groupNode = ((AuthorizableImpl) gr).getNode();

                        if (userManager.hasMemberSplitSize()) {
                            userManager.performProtectedOperation((SessionImpl) session, new SessionWriteOperation<Object>() {
                                public Boolean perform(SessionContext context) throws RepositoryException {
                                    NodeImpl nMembers = (groupNode.hasNode(UserConstants.N_MEMBERS)
                                            ? groupNode.getNode(UserConstants.N_MEMBERS)
                                            : groupNode.addNode(UserConstants.N_MEMBERS, UserConstants.NT_REP_MEMBERS, null));

                                    // Create N_MEMBERS node structure for holding member references
                                    for (Membership.Member member : nonExisting) {
                                        PropertySequence properties = GroupImpl.getPropertySequence(nMembers, userManager);
                                        String propName = member.name;
                                        if (propName == null) {
                                            log.debug("Ignoring unnamed user with id {}", member.id);
                                            continue;
                                        }
                                        if (properties.hasItem(propName)) {
                                            log.debug("Overwriting authorizable {} which is already member of {}.", propName, gr);
                                            properties.removeProperty(propName);
                                        }
                                        Value newMember = session.getValueFactory().createValue(member.id.toString(), PropertyType.WEAKREFERENCE);
                                        properties.addProperty(propName, newMember);
                                    }
                                    return null;
                                }
                            });
                        } else {
                            // Create P_MEMBERS for holding member references
                            // build list of valid members set before ....
                            List<Value> memberValues = new ArrayList<Value>();
                            if (groupNode.hasProperty(UserConstants.P_MEMBERS)) {
                                Value[] vls = groupNode.getProperty(UserConstants.P_MEMBERS).getValues();
                                memberValues.addAll(Arrays.asList(vls));
                            }

                            // ... and the non-Existing ones.
                            for (Membership.Member member : nonExisting) {
                                memberValues.add(session.getValueFactory().createValue(member.id.toString(), PropertyType.WEAKREFERENCE));
                            }

                            // and use implementation specific method to set the
                            // value of rep:members properties which was not possible
                            // through the API
                            userManager.setProtectedProperty(groupNode,
                                    UserConstants.P_MEMBERS,
                                    memberValues.toArray(new Value[memberValues.size()]),
                                    PropertyType.WEAKREFERENCE);
                        }
                    }

                    processed.add(reference);

                } else if (reference instanceof Impersonators) {
                    Authorizable a = userManager.getAuthorizable(((Impersonators) reference).userId);
                    if (a == null || a.isGroup()) {
                        throw new RepositoryException(((Impersonators) reference).userId + " does not represent a valid user.");
                    }

                    Impersonation imp = ((User) a).getImpersonation();

                    // 1. collect principals to add and to remove.
                    Map<String, Principal> toRemove = new HashMap<String, Principal>();
                    for (PrincipalIterator pit = imp.getImpersonators(); pit.hasNext();) {
                        Principal princ = pit.nextPrincipal();
                        toRemove.put(princ.getName(), princ);
                    }

                    List<Principal> toAdd = new ArrayList<Principal>();
                    Value[] vs = ((Impersonators) reference).values;
                    for (Value v : vs) {
                        String princName = v.getString();
                        if (toRemove.remove(princName) == null) {
                            // add it to the list of new impersonators to be added.
                            toAdd.add(new PrincipalImpl(princName));
                        } // else: no need to revoke impersonation for the given principal.
                    }

                    // 2. adjust set of impersonators
                    for (Principal princ : toRemove.values()) {
                        if (!imp.revokeImpersonation(princ)) {
                            handleFailure("Failed to revoke impersonation for " + princ.getName() + " on " + a);
                        }
                    }
                    for (Principal princ : toAdd) {
                        if (!imp.grantImpersonation(princ)) {
                            handleFailure("Failed to grant impersonation for " + princ.getName() + " on " + a);
                        }
                    }
                    // NOTE: no best effort handling so far. (TODO)

                    processed.add(reference);
                }
            }
            // successfully processed this entry of the reference tracker
            // -> remove from the reference tracker.
            referenceTracker.removeReferences(processed);
        } finally {
            // reset the autosave mode of the user manager in order to restore
            // the original state.
            if (resetAutoSave) {
                userManager.autoSave(true);
            }
        }
    }

    // ---------------------------------------------< ProtectedNodeImporter >---
    /**
     * @see ProtectedNodeImporter#start(org.apache.jackrabbit.core.NodeImpl)
     */
    public boolean start(NodeImpl protectedParent) throws RepositoryException {
        String repMembers = resolver.getJCRName(UserConstants.NT_REP_MEMBERS);
        if (repMembers.equals(protectedParent.getPrimaryNodeType().getName())) {
            NodeImpl groupNode = protectedParent;
            while (groupNode.getDepth() != 0 &&
                    repMembers.equals(groupNode.getPrimaryNodeType().getName())) {

                groupNode = (NodeImpl) groupNode.getParent();
            }
            Authorizable auth = userManager.getAuthorizable(groupNode);
            if (auth == null) {
                log.debug("Cannot handle protected node " + protectedParent + ". It nor one of its parents represent a valid Authorizable.");
                return false;
            } else {
                currentMembership = new Membership(auth.getID());
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * @see ProtectedNodeImporter#start(org.apache.jackrabbit.core.state.NodeState)
     */
    public boolean start(NodeState protectedParent) {
        return false;
    }

    /**
     * @see ProtectedNodeImporter#start(org.apache.jackrabbit.core.NodeImpl)
     */
    public void startChildInfo(NodeInfo childInfo, List<PropInfo> propInfos) throws RepositoryException {
        assert (currentMembership != null);

        if (UserConstants.NT_REP_MEMBERS.equals(childInfo.getNodeTypeName())) {
            for (PropInfo prop : propInfos) {
                for (Value v : prop.getValues(PropertyType.WEAKREFERENCE, resolver)) {
                    String name = resolver.getJCRName(prop.getName());
                    NodeId id = new NodeId(v.getString());
                    currentMembership.addMember(name, id);
                }
            }
        } else {
            log.warn("{} is not of type {}", childInfo.getName(), UserConstants.NT_REP_MEMBERS);
        }
    }

    /**
     * @see org.apache.jackrabbit.core.xml.ProtectedNodeImporter#endChildInfo()
     */
    public void endChildInfo() throws RepositoryException {
    }

    /**
     * @see ProtectedNodeImporter#end(org.apache.jackrabbit.core.NodeImpl)
     */
    public void end(NodeImpl protectedParent) throws RepositoryException {
        referenceTracker.processedReference(currentMembership);
        currentMembership = null;
    }

    /**
     * @see ProtectedNodeImporter#end(org.apache.jackrabbit.core.state.NodeState)
     */
    public void end(NodeState protectedParent) {
    }

    //---------------------------------------------------------< BeanConfig >---
    /**
     * @return human readable representation of the <code>importBehavior</code> value.
     */
    public String getImportBehavior() {
        return ImportBehavior.nameFromValue(importBehavior);
    }

    /**
     *
     * @param importBehaviorStr
     */
    public void setImportBehavior(String importBehaviorStr) {
        this.importBehavior = ImportBehavior.valueFromName(importBehaviorStr);
    }

    //------------------------------------------------------------< private >---
    /**
     * Handling the import behavior
     *
     * @param msg
     * @throws RepositoryException
     */
    private void handleFailure(String msg) throws RepositoryException {
        switch (importBehavior) {
            case ImportBehavior.IGNORE:
            case ImportBehavior.BESTEFFORT:
                log.warn(msg);
                break;
            case ImportBehavior.ABORT:
                throw new ConstraintViolationException(msg);
            default:
                // no other behavior. nothing to do.

        }
    }

    //------------------------------------------------------< inner classes >---
    /**
     * Inner class used to postpone import of group membership to the very end
     * of the import. This allows to import membership of user/groups that
     * are only being created during this import.
     *
     * @see ImportBehavior For additional configuration options.
     */
    private static final class Membership {
        private final String groupId;
        private final List<Member> members = new LinkedList<Member>();

        public Membership(String groupId) {
            this.groupId = groupId;
        }

        public void addMember(String name, NodeId id) {
            members.add(new Member(name, id));
        }

        public void addMember(NodeId id) {
            addMember(null, id);
        }

        // If only Java had tuples...
        public class Member {
            private final String name;
            private final NodeId id;

            public Member(String name, NodeId id) {
                super();
                this.name = name;
                this.id = id;
            }
        }
    }

    /**
     * Inner class used to postpone import of impersonators to the very end
     * of the import. This allows to import impersonation values pointing
     * to user that are only being created during this import.
     *
     * @see ImportBehavior For additional configuration options.
     */
    private static final class Impersonators {

        private final String userId;
        private final Value[] values;

        private Impersonators(String userId, Value[] values) {
            this.userId = userId;
            this.values = values;
        }
    }

    /**
     * Inner class defining the treatment of membership or impersonator
     * values pointing to non-existing authorizables.
     */
    public static final class ImportBehavior {

        /**
         * If a member or impersonator value cannot be set due to constraints
         * enforced by the API implementation, the failure is logged as
         * warning but otherwise ignored.
         */
        public static final int IGNORE = 1;
        /**
         * Same as {@link #IGNORE} but in addition tries to circumvent the
         * problem. This option should only be used with validated and trusted
         * XML passed to the SessionImporter.
         */
        public static final int BESTEFFORT = 2;
        /**
         * Aborts the import as soon as invalid values are detected throwing
         * a <code>ConstraintViolationException</code>.
         */
        public static final int ABORT = 3;

        public static final String NAME_IGNORE = "ignore";
        public static final String NAME_BESTEFFORT = "besteffort";
        public static final String NAME_ABORT = "abort";

        public static int valueFromName(String behaviorString) {
            if (NAME_IGNORE.equalsIgnoreCase(behaviorString)) {
                return IGNORE;
            } else if (NAME_BESTEFFORT.equalsIgnoreCase(behaviorString)) {
                return BESTEFFORT;
            } else if (NAME_ABORT.equalsIgnoreCase(behaviorString)) {
                return ABORT;
            } else {
                log.error("Invalid behavior " + behaviorString + " -> Using default: ABORT.");
                return ABORT;
            }
        }

        public static String nameFromValue(int importBehavior) {
            switch (importBehavior) {
                case ImportBehavior.IGNORE:
                    return NAME_IGNORE;
                case ImportBehavior.ABORT:
                    return NAME_ABORT;
                case ImportBehavior.BESTEFFORT:
                    return NAME_BESTEFFORT;
                default:
                    throw new IllegalArgumentException("Invalid import behavior: " + importBehavior);
            }
        }
    }
}