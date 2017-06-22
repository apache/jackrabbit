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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.security.authorization.jackrabbit.AccessControlConstants;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AccessControlListImpl implements JackrabbitAccessControlList, AccessControlConstants {

    private static final Logger log = LoggerFactory.getLogger(AccessControlListImpl.class);

    private final String jcrPath;

    private final List<AccessControlEntry> entries;
    private final QValueFactory qValueFactory;
    private final NamePathResolver resolver;

    /**
     * Namespace sensitive name of the REP_GLOB property in standard JCR form.
     */
    private final String jcrRepGlob;

    public AccessControlListImpl(String jcrPath, NamePathResolver resolver, QValueFactory qValueFactory) throws RepositoryException {
        this.jcrPath = jcrPath;
        this.entries = new ArrayList<AccessControlEntry>();
        this.qValueFactory = qValueFactory;
        this.resolver = resolver;

        try {
            jcrRepGlob = resolver.getJCRName(P_GLOB);
        } catch (NamespaceException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    AccessControlListImpl(NodeState aclNode, String aclPath, NamePathResolver resolver, QValueFactory factory, AccessControlManager acm) throws RepositoryException {
        this(aclPath, resolver, factory);

        NodeEntry entry = (NodeEntry) aclNode.getHierarchyEntry();
        Iterator<NodeEntry> it = entry.getNodeEntries();

        while(it.hasNext()) {
            NodeState aceNode = it.next().getNodeState();
            try {

                PropertyState ps = aceNode.getPropertyState(N_REP_PRINCIPAL_NAME);

                // rep:principal property
                String principalName = ps.getValue().getString();
                Principal principal = createPrincipal(principalName);

                // rep:privileges property
                ps = aceNode.getPropertyState(N_REP_PRIVILEGES);

                QValue[] values = ps.getValues();
                Privilege[] privileges = new Privilege[values.length];
                for (int i = 0; i < values.length; i++) {
                    privileges[i] = acm.privilegeFromName(values[i].getString());
                }

                // rep:glob property -> restrictions
                Map<Name, QValue> restrictions = null;
                if (aceNode.hasPropertyName(P_GLOB)) {
                    ps = aceNode.getPropertyState(P_GLOB);
                    restrictions = Collections.singletonMap(ps.getName(), ps.getValue());
                }

                // the isAllow flag
                boolean isAllow = NT_REP_GRANT_ACE.equals(aceNode.getNodeTypeName());
                // build the entry
                AccessControlEntry ace = new AccessControlEntryImpl(principal, privileges, isAllow, restrictions, Collections.EMPTY_MAP, resolver, qValueFactory);
                entries.add(ace);
            } catch (RepositoryException e) {
                log.debug("Fail to create Entry for "+ aceNode.getName().toString());
            }
        }
    }

    //--------------------------------------------------< AccessControlList >---
    @Override
    public AccessControlEntry[] getAccessControlEntries()
            throws RepositoryException {
        return entries.toArray(new AccessControlEntry[entries.size()]);
    }

    @Override
    public boolean addAccessControlEntry(Principal principal, Privilege[] privileges) throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, true, Collections.<String, Value>emptyMap());
    }

    @Override
    public void removeAccessControlEntry(AccessControlEntry ace)
            throws AccessControlException, RepositoryException {
        if (entries.contains(ace)) {
            entries.remove(ace);
        } else {
            throw new AccessControlException("Entry not present in this list");
        }
    }

    //----------------------------------------< JackrabbitAccessControlList >---
    @Override
    public String getPath() {
        return jcrPath;
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean addEntry(Principal principal, Privilege[] privileges,
                            boolean isAllow) throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, isAllow, Collections.<String, Value>emptyMap());
    }

    @Override
    public boolean addEntry(Principal principal, Privilege[] privileges,
                            boolean isAllow, Map<String, Value> restrictions,
                            Map<String, Value[]> mvRestrictions) throws AccessControlException,
            RepositoryException {


        // create entry to be added
        Map<Name, QValue> rs = createRestrictions(restrictions);
        Map<Name, Iterable<QValue>> mvRs = createMvRestrictions(mvRestrictions);
        AccessControlEntry entry = createEntry(principal, privileges, isAllow, rs, mvRs);

        return entries.add(entry);

    }

    @Override
    public boolean addEntry(Principal principal, Privilege[] privileges,
                            boolean isAllow, Map<String, Value> restrictions)
            throws AccessControlException, RepositoryException {
        return addEntry(principal, privileges, isAllow, restrictions, Collections.EMPTY_MAP);
    }

    @Override
    public String[] getRestrictionNames() throws RepositoryException {
        return new String[] {jcrRepGlob};
    }

    @Override
    public int getRestrictionType(String restrictionName) throws RepositoryException {
        if (!jcrRepGlob.equals(restrictionName)) {
            // JR2 feature
            return PropertyType.UNDEFINED;
        }
        return PropertyType.STRING;
    }

    @Override
    public boolean isMultiValueRestriction(String restrictionName) throws RepositoryException {
        return false;
    }

    @Override
    public void orderBefore(AccessControlEntry srcEntry,
                            AccessControlEntry destEntry) throws AccessControlException,
            UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedRepositoryOperationException("not yet implemented");
    }

    //-------------------------------------------------------------< Object >---
    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * Returns true if the path and the entries are equal; false otherwise.
     *
     * @param obj Object to be tested.
     * @return true if the path and the entries are equal; false otherwise.
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof AccessControlListImpl) {
            AccessControlListImpl acl = (AccessControlListImpl) obj;
            return jcrPath.equals(acl.jcrPath) && entries.equals(acl.entries);
        }
        return false;
    }

    //------------------------------------------------------------< private >---
    private AccessControlEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow,
                                           Map<Name, QValue> restrictions, Map<Name, Iterable<QValue>> mvRestrictions) throws RepositoryException {
        return new AccessControlEntryImpl(principal, privileges, isAllow, restrictions, mvRestrictions, resolver, qValueFactory);
    }

    private Map<Name, QValue> createRestrictions(Map<String, Value> restrictions) throws RepositoryException {
        Map<Name, QValue> rs = new HashMap<Name, QValue>(restrictions.size());
        for (String restName : restrictions.keySet()) {
            Value v = restrictions.get(restName);
            rs.put(resolver.getQName(restName), ValueFormat.getQValue(v, resolver, qValueFactory));
        }
        return rs;
    }

    private Map<Name, Iterable<QValue>> createMvRestrictions(Map<String, Value[]> restrictions) throws RepositoryException {
            Map<Name, Iterable<QValue>> rs = new HashMap<Name, Iterable<QValue>>(restrictions.size());
            for (String restName : restrictions.keySet()) {
                QValue[] qvs = ValueFormat.getQValues(restrictions.get(restName), resolver, qValueFactory);
                rs.put(resolver.getQName(restName), Arrays.asList(qvs));
            }
            return rs;
        }

    private static Principal createPrincipal(final String name) {
        return new Principal() {
            @Override
            public String getName() {
                return name;
            }
        };
    }
}
