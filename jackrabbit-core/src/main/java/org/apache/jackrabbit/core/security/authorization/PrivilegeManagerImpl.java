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

import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <code>PrivilegeManager</code>...
 */
public final class PrivilegeManagerImpl implements PrivilegeManager, PrivilegeRegistry.Listener {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(PrivilegeManagerImpl.class);

    private static final Privilege[] EMPTY_ARRAY = new Privilege[0];

    /**
     * The privilege registry
     */
    private final PrivilegeRegistry registry;

    /**
     * The name resolver used to determine the correct privilege
     * {@link javax.jcr.security.Privilege#getName() name} depending on the sessions namespace
     * mappings.
     */
    private final NameResolver resolver;

    /**
     * Per instance map containing the namespace aware representation of
     * the registered privileges.
     */
    private final Map<Name, Privilege> cache;

    public PrivilegeManagerImpl(PrivilegeRegistry registry, NameResolver nameResolver) {
        this.registry = registry;
        this.resolver = nameResolver;
        this.cache = new HashMap<Name, Privilege>();

        // listen to privilege registration (due to weak references no explicit
        // stop-listening call required 
        registry.addListener(this);
    }

    /**
     * Disposes this privilege manager
     */
    public void dispose() {
        registry.removeListener(this);
    }

    //---------------------------------------------------< PrivilegeManager >---
    /**
     * @see PrivilegeManager#getRegisteredPrivileges()
     */
    public Privilege[] getRegisteredPrivileges() throws RepositoryException {
        PrivilegeDefinition[] allDefs = registry.getAll();
        if (allDefs.length != cache.size()) {
            synchronized (cache) {
                for (PrivilegeDefinition def : allDefs) {
                    if (!cache.containsKey(def.getName())) {
                        cache.put(def.getName(), new PrivilegeImpl(def));
                    }
                }
            }
        }
        return cache.values().toArray(new Privilege[allDefs.length]);
    }

    /**
     * @see PrivilegeManager#getPrivilege(String)
     */
    public Privilege getPrivilege(String privilegeName) throws AccessControlException, RepositoryException {
        Name name = resolver.getQName(privilegeName);
        return getPrivilege(name);
    }

    /**
     * Register a new custom privilege with the specified characteristics.
     * <p>
     * The current implementation has the following limitations and constraints:
     *
     * <ul>
     * <li>the name may not be in use by another privilege</li>
     * <li>the namespace URI must be a valid, registered namespace excluding
     * those namespaces marked as being reserved</li>
     * <li>an aggregate custom privilege is valid if all declared aggregate
     * names can be resolved to registered privileges and if there exists
     * no registered privilege with the same aggregated privileges.</li>
     * </ul>
     * <p>
     * <strong>Please note</strong><br>
     * Custom privilege(s) will not be enforced for any kind of repository
     * operations. Those are exclusively covered by the built-in privileges.
     * This also implies that the {@link Permission}s are not affected by
     * custom privileges.
     * <p>
     * Applications making use of the custom privilege(s) are in charge of
     * asserting whether the privileges are granted/denied according to their
     * application specific needs.
     *
     * @param privilegeName The name of the new custom privilege.
     * @param isAbstract Boolean flag indicating if the privilege is abstract.
     * @param declaredAggregateNames An array of privilege names referring to
     * registered privileges being aggregated by this new custom privilege.
     * In case of a non aggregate privilege an empty array should be passed.
     * @return the new privilege.
     * @throws AccessDeniedException If the session this manager has been created
     * lacks rep:privilegeManagement privilege.
     * @throws RepositoryException If the privilege could not be registered due
     * to constraint violations or if persisting the custom privilege fails.
     * @see PrivilegeManager#registerPrivilege(String, boolean, String[])
     */
    public Privilege registerPrivilege(String privilegeName, boolean isAbstract,
                                       String[] declaredAggregateNames)
            throws AccessDeniedException, RepositoryException {
        if (resolver instanceof SessionImpl) {
            SessionImpl sImpl = (SessionImpl) resolver;
            sImpl.getAccessManager().checkRepositoryPermission(Permission.PRIVILEGE_MNGMT);
        } else {
            // cannot evaluate
            throw new AccessDeniedException("Registering privileges is not allowed for the editing session.");
        }

        Name name = resolver.getQName(privilegeName);
        Set<Name> daNames;
        if (declaredAggregateNames == null || declaredAggregateNames.length == 0) {
            daNames = Collections.emptySet();
        } else {
            daNames = new HashSet<Name>(declaredAggregateNames.length);
            for (String declaredAggregateName : declaredAggregateNames) {
                daNames.add(resolver.getQName(declaredAggregateName));
            }
        }
        registry.registerDefinition(name, isAbstract, daNames);

        return getPrivilege(privilegeName);
    }

    //-----------------------------< implementation specific public methods >---       
    /**
     * @param privileges An array of privileges.
     * @return The bits of the privileges contained in the specified
     * array.
     * @throws AccessControlException If the specified array is null, empty
     * or if it contains an unregistered privilege.
     */
    public PrivilegeBits getBits(Privilege... privileges) throws AccessControlException {
        if (privileges == null || privileges.length == 0) {
            throw new AccessControlException("Privilege array is empty or null.");
        }

        PrivilegeDefinition[] defs = new PrivilegeDefinition[privileges.length];
        for (int i = 0; i < privileges.length; i++) {
            Privilege p = privileges[i];
            if (p instanceof PrivilegeImpl) {
                defs[i] = ((PrivilegeImpl) p).definition;
            } else {
                String name = (p == null) ? "null" : p.getName();
                throw new AccessControlException("Unknown privilege '" + name + "'.");
            }
        }
        return registry.getBits(defs);
    }

    /**
     * @param privilegeNames An array of privilege names.
     * @return The bits of the privileges contained in the specified
     * array.
     * @throws AccessControlException If the specified array is null or if it
     * contains the name of an unregistered privilege.
     */
    public PrivilegeBits getBits(Name... privilegeNames) throws RepositoryException {
        if (privilegeNames == null) {
            throw new AccessControlException("Privilege name array is null.");
        }
        return registry.getBits(privilegeNames);
    }

    /**
     * Returns an array of registered <code>Privilege</code>s. If the specified
     * <code>bits</code> represent a single registered privilege the returned array
     * contains a single element. Otherwise the returned array contains the
     * individual registered privileges that are combined in the given
     * <code>bits</code>. If <code>bits</code> does not match to any registered
     * privilege an empty array will be returned.
     *
     * @param bits Privilege bits as obtained from {@link #getBits(Privilege...)}.
     * @return Array of <code>Privilege</code>s that are presented by the given
     * <code>bits</code> or an empty array if <code>bits</code> cannot be
     * resolved to registered <code>Privilege</code>s.
     * @see #getBits(Privilege...)
     */
    public Set<Privilege> getPrivileges(PrivilegeBits bits) {
        Name[] names = registry.getNames(bits);
        if (names.length == 0) {
            return Collections.emptySet();
        } else {
            Set<Privilege> privs = new HashSet<Privilege>(names.length);
            for (Name n : names) {
                try {
                    privs.add(getPrivilege(n));
                } catch (RepositoryException e) {
                    log.error("Internal error: invalid privilege name " + n.toString());
                }
            }
            return privs;
        }
    }

    //------------------------------------------------------------< private >---
    /**
     * @param name
     * @return The privilege with the specified name.
     * @throws AccessControlException
     * @throws RepositoryException
     */
    private Privilege getPrivilege(Name name) throws AccessControlException, RepositoryException {
        Privilege privilege;
        synchronized (cache) {
            if (cache.containsKey(name)) {
                privilege = cache.get(name);
            } else {
                PrivilegeDefinition def = registry.get(name);
                if (def != null) {
                    privilege = new PrivilegeImpl(def);
                    cache.put(name, privilege);
                } else {
                    throw new AccessControlException("Unknown privilege " + resolver.getJCRName(name));
                }
            }
        }
        return privilege;
    }

    //-----------------------------------------< PrivilegeRegistry.Listener >---
    /**
     * @see PrivilegeRegistry.Listener#privilegesRegistered(java.util.Set)
     * @param privilegeNames
     */
    public void privilegesRegistered(Set<Name> privilegeNames) {
        // force recalculation of jcr:all privilege
        synchronized (cache) {
            cache.remove(NameConstants.JCR_ALL);
        }
    }

    //----------------------------------------------------------< Privilege >---
    /**
     * Simple wrapper used to provide an public representation of the
     * registered internal privileges properly exposing the JCR name.
     */
    private class PrivilegeImpl implements Privilege {

        private final PrivilegeDefinition definition;

        private final Privilege[] declaredAggregates;
        private final Privilege[] aggregates;

        private PrivilegeImpl(PrivilegeDefinition definition) throws RepositoryException {
            this.definition = definition;

            Set<Name> set = definition.getDeclaredAggregateNames();
            Name[] declAggrNames = set.toArray(new Name[set.size()]);
            if (declAggrNames.length == 0) {
                declaredAggregates = EMPTY_ARRAY;
                aggregates = EMPTY_ARRAY;
            } else {
                declaredAggregates = new Privilege[declAggrNames.length];
                for (int i = 0; i < declAggrNames.length; i++) {
                    declaredAggregates[i] = getPrivilege(declAggrNames[i]);
                }

                Set<Privilege> aggr = new HashSet<Privilege>();
                for (Privilege decl : declaredAggregates) {
                    aggr.add(decl);
                    if (decl.isAggregate()) {
                        aggr.addAll(Arrays.asList(decl.getAggregatePrivileges()));
                    }
                }
                aggregates = aggr.toArray(new Privilege[aggr.size()]);
            }
        }

        /**
         * @see Privilege#getName()
         */
        public String getName() {
            try {
                return resolver.getJCRName(definition.getName());
            } catch (NamespaceException e) {
                // should not occur -> return internal name representation.
                return definition.getName().toString();
            }
        }

        /**
         * @see Privilege#isAbstract()
         */
        public boolean isAbstract() {
            return definition.isAbstract();
        }

        /**
         * @see Privilege#isAggregate()
         */
        public boolean isAggregate() {
            return declaredAggregates.length > 0;
        }

        /**
         * @see Privilege#getDeclaredAggregatePrivileges()
         */
        public Privilege[] getDeclaredAggregatePrivileges() {
            return declaredAggregates;
        }

        /**
         * @see Privilege#getAggregatePrivileges()
         */
        public Privilege[] getAggregatePrivileges() {
            return aggregates;
        }

        //---------------------------------------------------------< Object >---
        @Override
        public String toString() {
            return getName();
        }

        @Override
        public int hashCode() {
            return definition.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof PrivilegeImpl) {
                PrivilegeImpl other = (PrivilegeImpl) obj;
                return definition.equals(other.definition);
            }
            return false;
        }
    }
}