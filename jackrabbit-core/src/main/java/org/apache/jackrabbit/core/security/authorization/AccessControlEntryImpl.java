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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.authorization.PrivilegeCollection;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.value.ValueHelper;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Simple, immutable implementation of the
 * {@link javax.jcr.security.AccessControlEntry}
 * and the {@link JackrabbitAccessControlEntry} interfaces.
 */
public abstract class AccessControlEntryImpl implements JackrabbitAccessControlEntry {

    /**
     * All privileges contained in this entry.
     */
    private Privilege[] privileges;

    /**
     * PrivilegeBits calculated from built-in privileges
     */
    private final PrivilegeBits privilegeBits;

    /**
     * the Principal of this entry
     */
    private final Principal principal;

    /**
     * Jackrabbit specific extension: if the actions contained are allowed or
     * denied.
     */
    private final boolean allow;

    /**
     * Jackrabbit specific extension: the list of additional restrictions to be
     * included in the evaluation.
     */
    private final Map<Name, Value> restrictions;

    /**
     * Hash code being calculated on demand.
     */
    private int hashCode = -1;

    /**
     * Construct an access control entry for the given principal and privileges.
     *
     * @param principal Principal for this access control entry.
     * @param privileges Privileges for this access control entry.
     * @param isAllow <code>true</code> if this ACE grants the specified
     * privileges to the specified principal; <code>false</code> otherwise.
     * @param restrictions A map of restriction name (String) to restriction
     * (Value). See {@link org.apache.jackrabbit.api.security.JackrabbitAccessControlList#getRestrictionNames()}
     * and {@link org.apache.jackrabbit.api.security.JackrabbitAccessControlList#getRestrictionType(String)}.
     * @throws AccessControlException if either principal or privileges are invalid.
     * @throws RepositoryException if another error occurs.
     */
    protected AccessControlEntryImpl(Principal principal, Privilege[] privileges,
                                     boolean isAllow, Map<String, Value> restrictions)
            throws AccessControlException, RepositoryException {
        if (principal == null || privileges == null) {
            throw new AccessControlException();
        }
        // make sure no abstract privileges are passed.
        for (Privilege privilege : privileges) {
            if (privilege.isAbstract()) {
                throw new AccessControlException("Privilege " + privilege + " is abstract.");
            }
        }
        this.principal = principal;
        this.privileges = privileges;
        this.privilegeBits = getPrivilegeManager().getBits(privileges).unmodifiable();
        this.allow = isAllow;

        if (restrictions == null) {
            this.restrictions = Collections.emptyMap();
        } else {
            this.restrictions = new HashMap<Name, Value>(restrictions.size());
            // validate the passed restrictions and fill the map
            for (String name : restrictions.keySet()) {
                Value value = ValueHelper.copy(restrictions.get(name), getValueFactory());
                this.restrictions.put(getResolver().getQName(name), value);
            }
        }
    }

    /**
     * Construct an access control entry for the given principal and privileges.
     *
     * @param principal Principal for this access control entry.
     * @param privilegesBits Privileges for this access control entry.
     * @param isAllow <code>true</code> if this ACE grants the specified
     * privileges to the specified principal; <code>false</code> otherwise.
     * @param restrictions A map of restriction name (String) to restriction
     * (Value). See {@link org.apache.jackrabbit.api.security.JackrabbitAccessControlList#getRestrictionNames()}
     * and {@link org.apache.jackrabbit.api.security.JackrabbitAccessControlList#getRestrictionType(String)}.
     * @throws RepositoryException if another error occurs.
     */
    protected AccessControlEntryImpl(Principal principal, PrivilegeBits privilegesBits,
                                     boolean isAllow, Map<String, Value> restrictions)
            throws RepositoryException {
        if (principal == null || privilegesBits == null) {
            throw new IllegalArgumentException();
        }
        this.principal = principal;
        this.privilegeBits = privilegesBits.unmodifiable();
        this.allow = isAllow;

        if (restrictions == null) {
            this.restrictions = Collections.emptyMap();
        } else {
            this.restrictions = new HashMap<Name, Value>(restrictions.size());
            // validate the passed restrictions and fill the map
            for (String name : restrictions.keySet()) {
                Value value = ValueHelper.copy(restrictions.get(name), getValueFactory());
                this.restrictions.put(getResolver().getQName(name), value);
            }
        }
    }

    /**
     *
     * @param base
     * @param privilegeBits
     * @param isAllow
     * @throws AccessControlException
     */
    protected AccessControlEntryImpl(AccessControlEntryImpl base, PrivilegeBits privilegeBits, boolean isAllow)
            throws AccessControlException, RepositoryException {
        this(base.principal, privilegeBits, isAllow, (base.restrictions.isEmpty()) ? null : Collections.<String, Value>emptyMap());

        if (!base.restrictions.isEmpty()) {
            // validate the passed restrictions and fill the map
            for (Name name : base.restrictions.keySet()) {
                Value value = ValueHelper.copy(base.restrictions.get(name), getValueFactory());
                this.restrictions.put(name, value);
            }
        }
    }

    /**
     *
     * @param base
     * @param privileges
     * @param isAllow
     * @throws AccessControlException
     */
    protected AccessControlEntryImpl(AccessControlEntryImpl base, Privilege[] privileges, boolean isAllow)
            throws AccessControlException, RepositoryException {
        this(base.principal, privileges, isAllow, (base.restrictions.isEmpty()) ? null : Collections.<String, Value>emptyMap());

        if (!base.restrictions.isEmpty()) {
            // validate the passed restrictions and fill the map
            for (Name name : base.restrictions.keySet()) {
                Value value = ValueHelper.copy(base.restrictions.get(name), getValueFactory());
                this.restrictions.put(name, value);
            }
        }
    }
    
    /**
     * @return the permission bits that correspond to the privileges defined by this entry.
     */
    public PrivilegeBits getPrivilegeBits() {
        return privilegeBits;
    }

    /**
     * Returns <code>true</code> if this ACE defines any restriction.
     *
     * @return <code>true</code> if this ACE defines any restriction;
     * <code>false</code> otherwise.
     */
    public boolean hasRestrictions() {
        return !restrictions.isEmpty();
    }

    /**
     * Returns the restrictions defined for this entry.
     *
     * @return the restrictions defined for this entry.
     */
    public Map<Name,Value> getRestrictions() {
        return Collections.unmodifiableMap(restrictions);
    }

    /**
     * @param restrictionName
     * @return The restriction with the specified name or <code>null</code>.
     */
    public Value getRestriction(Name restrictionName) {
        return ValueHelper.copy(restrictions.get(restrictionName), getValueFactory());
    }

    /**
     * @return Returns the name resolver used to convert JCR names to Name and vice versa.
     */
    protected abstract NameResolver getResolver();

    /**
     * @return The value factory to be used.
     */
    protected abstract ValueFactory getValueFactory();

    /**
     * @return The privilege manager in use.
     */
    protected abstract PrivilegeManagerImpl getPrivilegeManager();

    /**
     * Build the hash code.
     *
     * @return the hash code.
     */
    protected int buildHashCode() {
        int h = 17;
        h = 37 * h + principal.getName().hashCode();
        h = 37 * h + privilegeBits.hashCode();
        h = 37 * h + Boolean.valueOf(allow).hashCode();
        h = 37 * h + restrictions.hashCode();
        return h;
    }

    //-------------------------------------------------< AccessControlEntry >---
    /**
     * @see javax.jcr.security.AccessControlEntry#getPrincipal()
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * @see javax.jcr.security.AccessControlEntry#getPrivileges()
     */
    public Privilege[] getPrivileges() {
        if (privileges == null) {
            Set<Privilege> ps = getPrivilegeManager().getPrivileges(privilegeBits);
            privileges = ps.toArray(new Privilege[ps.size()]);
        }
        return privileges;
    }


    //---------------------------------------< JackrabbitAccessControlEntry >---
    /**
     * @see JackrabbitAccessControlEntry#isAllow()
     */
    public boolean isAllow() {
        return allow;
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry#getRestrictionNames()
     */
    public String[] getRestrictionNames() throws NamespaceException {
        String[] restrNames = new String[restrictions.size()];
        int i = 0;
        for (Name n : restrictions.keySet()) {
            restrNames[i] = getResolver().getJCRName(n);
            i++;
        }
        return restrNames;
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry#getRestriction(String)
     */
    public Value getRestriction(String restrictionName) throws RepositoryException {
        return getRestriction(getResolver().getQName(restrictionName));
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry#getRestrictions(String)
     */
    public Value[] getRestrictions(String restrictionName) throws RepositoryException {
        return new Value[] {getRestriction(restrictionName)};
    }

    @Override
    public PrivilegeCollection getPrivilegeCollection() {
        return new PrivilegeCollection() {
            @Override
            public Privilege[] getPrivileges() {
                return AccessControlEntryImpl.this.getPrivileges();
            }

            @Override
            public boolean includes(String... privilegeNames) throws RepositoryException {
                if (privilegeNames.length == 0) {
                    return true;
                }
                PrivilegeBits pb = AccessControlEntryImpl.this.getPrivilegeBits();
                if (pb.isEmpty()) {
                    return false;
                }
                Set<Privilege> privileges = new HashSet<>(privilegeNames.length);
                PrivilegeManagerImpl privilegeMgr = getPrivilegeManager();
                for (String pName : privilegeNames) {
                    privileges.add(privilegeMgr.getPrivilege(pName));
                }
                PrivilegeBits toTest = privilegeMgr.getBits(privileges.toArray(new Privilege[0]));
                return pb.includes(toTest);
            }
        };
    }

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = buildHashCode();
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
        if (obj instanceof AccessControlEntryImpl) {
            AccessControlEntryImpl other = (AccessControlEntryImpl) obj;
            return principal.getName().equals(other.principal.getName()) &&
                   privilegeBits.equals(other.privilegeBits) &&
                   allow == other.allow &&
                   restrictions.equals(other.restrictions);
        }
        return false;
    }
}
