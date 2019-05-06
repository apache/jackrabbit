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
package org.apache.jackrabbit.core.security.authorization.acl;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.GlobPattern;
import org.apache.jackrabbit.core.security.authorization.PrivilegeBits;
import org.apache.jackrabbit.core.security.authorization.PrivilegeManagerImpl;
import org.apache.jackrabbit.core.security.principal.GroupPrincipals;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry... TODO
 */
class Entry implements AccessControlConstants {

    private static final Logger log = LoggerFactory.getLogger(Entry.class);

    private final String principalName;
    private final boolean isGroupEntry;
    private final PrivilegeBits privilegeBits;
    private final boolean isAllow;
    private final NodeId id;
    private final GlobPattern pattern;
    private final boolean hasRestrictions;

    private int hashCode;

    private Entry(NodeId id, String principalName, boolean isGroupEntry,
                  PrivilegeBits privilegeBits, boolean allow, String path, Value globValue) throws RepositoryException {

        this.principalName = principalName;
        this.isGroupEntry = isGroupEntry;
        this.privilegeBits = privilegeBits;
        this.isAllow = allow;
        this.id = id;
        this.pattern = calculatePattern(path, globValue);
        this.hasRestrictions = (globValue != null);
    }

    static List<Entry> readEntries(NodeImpl aclNode, String path) throws RepositoryException {
        if (aclNode == null || !NT_REP_ACL.equals(aclNode.getPrimaryNodeTypeName())) {
            throw new IllegalArgumentException("Node must be of type 'rep:ACL'");
        }
        SessionImpl sImpl = (SessionImpl) aclNode.getSession();
        PrincipalManager principalMgr = sImpl.getPrincipalManager();
        PrivilegeManagerImpl privilegeMgr = (PrivilegeManagerImpl) ((JackrabbitWorkspace) sImpl.getWorkspace()).getPrivilegeManager();

        NodeId nodeId = aclNode.getParentId();

        List<Entry> entries = new ArrayList<Entry>();
        // load the entries:
        NodeIterator itr = aclNode.getNodes();
        while (itr.hasNext()) {
            NodeImpl aceNode = (NodeImpl) itr.nextNode();
            try {
                String principalName = aceNode.getProperty(P_PRINCIPAL_NAME).getString();
                boolean isGroupEntry = false;
                Principal princ = principalMgr.getPrincipal(principalName);
                if (princ != null) {
                    isGroupEntry = GroupPrincipals.isGroup(princ);
                }

                InternalValue[] privValues = aceNode.getProperty(P_PRIVILEGES).internalGetValues();
                Name[] privNames = new Name[privValues.length];
                for (int i = 0; i < privValues.length; i++) {
                    privNames[i] = privValues[i].getName();
                }

                Value globValue = null;
                if (aceNode.hasProperty(P_GLOB)) {
                    globValue = aceNode.getProperty(P_GLOB).getValue();
                }

                boolean isAllow = NT_REP_GRANT_ACE.equals(aceNode.getPrimaryNodeTypeName());
                Entry ace = new Entry(nodeId, principalName, isGroupEntry, privilegeMgr.getBits(privNames), isAllow, path, globValue);
                entries.add(ace);
            } catch (RepositoryException e) {
                log.debug("Failed to build ACE from content. {}", e.getMessage());
            }
        }

        return entries;
    }

    private static GlobPattern calculatePattern(String path, Value globValue) throws RepositoryException {
        if (path == null) {
            return null;
        } else {
            if (globValue == null) {
                return GlobPattern.create(path);
            } else {
                return GlobPattern.create(path, globValue.getString());
            }
        }
    }

    /**
     * @param nodeId
     * @return <code>true</code> if this entry is defined on the node
     * at <code>nodeId</code>
     */
    boolean isLocal(NodeId nodeId) {
        return id != null && id.equals(nodeId);
    }

    /**
     *
     * @param jcrPath
     * @return
     */
    boolean matches(String jcrPath) {
        return pattern != null && pattern.matches(jcrPath);
    }

    PrivilegeBits getPrivilegeBits() {
        return privilegeBits;
    }

    boolean isAllow() {
        return isAllow;
    }

    String getPrincipalName() {
        return principalName;
    }

    boolean isGroupEntry() {
        return isGroupEntry;
    }

    boolean hasRestrictions() {
        return hasRestrictions;
    }

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (hashCode == -1) {
            int h = 17;
            h = 37 * h + principalName.hashCode();
            h = 37 * h + privilegeBits.hashCode();
            h = 37 * h + Boolean.valueOf(isAllow).hashCode();
            h = 37 * h + pattern.hashCode();
            hashCode = h;
        }
        return hashCode;
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Entry) {
            Entry other = (Entry) obj;
            return principalName.equals(other.principalName) &&
                   privilegeBits.equals(other.privilegeBits) &&
                   isAllow == other.isAllow &&
                   pattern.equals(other.pattern);
        }
        return false;
    }
}
