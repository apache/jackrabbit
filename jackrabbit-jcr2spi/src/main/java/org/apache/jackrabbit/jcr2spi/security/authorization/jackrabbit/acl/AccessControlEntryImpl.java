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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.authorization.PrivilegeCollection;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.value.QValueValue;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.value.ValueHelper;

class AccessControlEntryImpl implements JackrabbitAccessControlEntry {

    /*
     * The principal this entry has been created for.
     */
    private final Principal principal;
    
    /*
     * The privileges in this entry.
     */
    private final Privilege[] privileges;
        
    /*
     * Whether this entry is allowed/denied
     */
    private final boolean isAllow;
    
    /*
     * Restrictions that may apply with this entry.
     */
    private final Map<Name, QValue> restrictions;
    private final Map<Name, Iterable<QValue>> mvRestrictions;

    private final NamePathResolver resolver;

    private final QValueFactory qvf;

    private int hashCode = -1;
    private int privsHashCode = -1;

    /**
     * 
     * @param principal
     * @param privileges
     * @param isAllow
     * @param restrictions
     * @throws RepositoryException 
     */
    AccessControlEntryImpl(Principal principal, Privilege[] privileges, boolean isAllow,
                           Map<Name, QValue> restrictions, Map<Name, Iterable<QValue>> mvRestrictions,
                           NamePathResolver resolver, QValueFactory qvf) throws RepositoryException {
        if (principal == null || (privileges != null && privileges.length == 0)) {
            throw new AccessControlException("An Entry must not have a NULL principal or empty privileges");
        }
        checkAbstract(privileges);
        
        this.principal = principal;
        this.privileges = privileges;
        this.isAllow = isAllow;
        this.resolver = resolver;
        this.qvf = qvf;

        if (restrictions == null) {
            this.restrictions = Collections.<Name, QValue>emptyMap();
        } else {
            this.restrictions = restrictions;
        }
        if (mvRestrictions == null) {
            this.mvRestrictions = Collections.emptyMap();
        } else {
            this.mvRestrictions = mvRestrictions;
        }
    }
    
    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Privilege[] getPrivileges() {
        return privileges;
    }
    
    @Override
    public boolean isAllow() {
        return isAllow;
    }
    
    @Override
    public String[] getRestrictionNames() throws RepositoryException {
        List<String> restNames = new ArrayList<String>(restrictions.size());
        for (Name restName : restrictions.keySet()) {
            restNames.add(resolver.getJCRName(restName));
        }
        return restNames.toArray(new String[restNames.size()]);
    }

    @Override
    public Value getRestriction(String restrictionName)
            throws ValueFormatException, RepositoryException {
        try {
            Name restName = resolver.getQName(restrictionName);
            if (!restrictions.containsKey(restName)) {
                return null;
            }
            return createJcrValue(restrictions.get(restName));
        } catch (IllegalStateException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    /*
     * As of Jackrabbit 2.8, this extention has been added to the Jackrabbit API.
     * However, Jackrabbit (before) OAK doesn't support mv. restrictions. Thus simply
     * return an array containing the single restriction value.
     */
    @Override
    public Value[] getRestrictions(String restrictionName)
            throws RepositoryException {
        return new Value[] {getRestriction(restrictionName)};
    }

    @Override
    public PrivilegeCollection getPrivilegeCollection() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    //-------------------------------------------------------------< Object >---
    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = buildHashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AccessControlEntryImpl) {
            AccessControlEntryImpl other = (AccessControlEntryImpl) obj;
            return principal.getName().equals(other.principal.getName()) &&
                    isAllow == other.isAllow &&
                    restrictions.equals(other.restrictions) &&
                    mvRestrictions.equals(other.mvRestrictions) &&
                    getPrivilegesHashCode() == other.getPrivilegesHashCode();
        }
        return false;
    }

    //-------------------------------------------------------------< private >---
    private int buildHashCode() {
        int h = 17;
        h = 37 * h + principal.getName().hashCode();
        h = 37 * h + getPrivilegesHashCode();
        h = 37 * h + Boolean.valueOf(isAllow).hashCode();
        h = 37 * h + restrictions.hashCode();
        h = 37 * h + mvRestrictions.hashCode();
        return h;
    }

    private int getPrivilegesHashCode() {
        if (privsHashCode == -1) {
            Set<Privilege> prvs = new HashSet<Privilege>(Arrays.asList(privileges));
            for (Privilege p : privileges) {
                if (p.isAggregate()) {
                    prvs.addAll(Arrays.asList(p.getAggregatePrivileges()));
                }
            }
            privsHashCode = prvs.hashCode();
        }
        return privsHashCode;
    }

    private void checkAbstract(Privilege[] privileges) throws AccessControlException {
        for (Privilege privilege : privileges) {
            if (privilege.isAbstract()) {
                throw new AccessControlException("An Entry cannot contain abstract privileges.");
            }
        }
    }

    /**
     * Creates a jcr Value from the given qvalue using the specified
     * factory.
     * @return         the jcr value representing the qvalue.
     */
    private Value createJcrValue(QValue qValue) throws RepositoryException {
        
        // build ValueFactory
        ValueFactoryQImpl valueFactory = new ValueFactoryQImpl(qvf, resolver);

        // build jcr value
        QValueValue jcrValue = new QValueValue(qValue, resolver);
        
        return ValueHelper.copy(jcrValue, valueFactory);
    }
}
