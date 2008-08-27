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

import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The <code>PrivilegeRegistry</code> defines the set of <code>Privilege</code>s
 * known to the repository.
 */
public final class PrivilegeRegistry {

    private static final Set REGISTERED_PRIVILEGES = new HashSet(10);
    private static final Map BITS_TO_PRIVILEGES = new HashMap();
    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    private static final Privilege[] EMPTY_ARRAY = new Privilege[0];

    public static final int NO_PRIVILEGE = 0;
    public static final int READ = 1;
    public static final int MODIFY_PROPERTIES = 2;
    public static final int ADD_CHILD_NODES = 4;
    public static final int REMOVE_CHILD_NODES = 8;
    public static final int REMOVE_NODE = 16;
    public static final int READ_AC = 32;
    public static final int MODIFY_AC = 64;
    public static final int WRITE = 14;
    public static final int ALL = 127;

    private static final InternalPrivilege READ_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_READ, READ));
    private static final InternalPrivilege MODIFY_PROPERTIES_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_MODIFY_PROPERTIES, MODIFY_PROPERTIES));
    private static final InternalPrivilege ADD_CHILD_NODES_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_ADD_CHILD_NODES, ADD_CHILD_NODES));
    private static final InternalPrivilege REMOVE_CHILD_NODES_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_REMOVE_CHILD_NODES, REMOVE_CHILD_NODES));
    private static final InternalPrivilege REMOVE_NODE_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_REMOVE_NODE, REMOVE_NODE));
    private static final InternalPrivilege READ_AC_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_READ_ACCESS_CONTROL, READ_AC));
    private static final InternalPrivilege MODIFY_AC_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL, MODIFY_AC));
    private static final InternalPrivilege WRITE_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_WRITE, new InternalPrivilege[] {
            MODIFY_PROPERTIES_PRIVILEGE,
            ADD_CHILD_NODES_PRIVILEGE,
            REMOVE_CHILD_NODES_PRIVILEGE }));
    private static final InternalPrivilege ALL_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_ALL, new InternalPrivilege[] {
            READ_PRIVILEGE,
            WRITE_PRIVILEGE,
            REMOVE_NODE_PRIVILEGE,
            READ_AC_PRIVILEGE,
            MODIFY_AC_PRIVILEGE}));

    /**
     * The name resolver used to determine the correct privilege
     * {@link Privilege#getName() name} depending on the sessions namespace
     * mappings.
     */
    private final NameResolver resolver;

    /**
     * Per instance map containing the instance specific representation of
     * the registered privileges.
     */
    private final Map localCache;

    /**
     * Create a new <code>PrivilegeRegistry</code> instance.
     *
     * @param resolver NameResolver used to calculate the JCR name of the
     * privileges.
     */
    public PrivilegeRegistry(NameResolver resolver) {
        this.resolver = resolver;
        localCache = new HashMap(REGISTERED_PRIVILEGES.size());
        for (Iterator it = REGISTERED_PRIVILEGES.iterator(); it.hasNext();) {
            InternalPrivilege ip = (InternalPrivilege) it.next();
            Privilege priv = new PrivilegeImpl(ip, resolver);
            localCache.put(ip.name, priv);
        }
    }

    /**
     * Returns all registered privileges.
     *
     * @return all registered privileges.
     */
    public Privilege[] getRegisteredPrivileges() {
        return (Privilege[]) localCache.values().toArray(new Privilege[localCache.size()]);
    }

    /**
     * Returns the privilege with the specified <code>privilegeName</code>.
     *
     * @param privilegeName
     * @return the privilege with the specified <code>privilegeName</code>.
     * @throws AccessControlException If no privilege with the given name exists.
     * @throws RepositoryException If another error occurs.
     */
    public Privilege getPrivilege(String privilegeName) throws AccessControlException, RepositoryException {
        Name name = resolver.getQName(privilegeName);
        if (localCache.containsKey(name)) {
            return (Privilege) localCache.get(name);
        } else {
            throw new AccessControlException("Unknown privilege " + privilegeName);
        }
    }

    /**
     * Returns an array of registered <code>Privilege</code>s. If the specified
     * <code>bits</code> represent a registered privilege the returned array
     * contains a single element. Otherwise the returned array contains the
     * individual registered privileges that are combined in the givent
     * <code>bits</code>. If <code>bits</code> is {@link #NO_PRIVILEGE 0} or
     * does not match to any registered privilege an empty array will be returned.
     *
     * @param bits
     * @return Array of <code>Privilege</code>s that are presented by the given it
     * or an empty array if <code>bits</code> is lower than {@link #READ} or
     * cannot be resolved to registered <code>Privilege</code>s.
     */
    public Privilege[] getPrivileges(int bits) {
        Privilege[] privs;
        if (bits > NO_PRIVILEGE) {
            InternalPrivilege[] internalPrivs = getInteralPrivileges(bits);
            privs = new Privilege[internalPrivs.length];
            for (int i = 0; i < internalPrivs.length; i++) {
                privs[i] = (Privilege) localCache.get(internalPrivs[i].name);
            }
        } else {
            privs = new Privilege[0];
        }
        return privs;
    }

    /**
     * @param privileges
     * @return
     * @throws AccessControlException If the specified array is null
     * or if it contains an unregistered privilege.
     */
    public static int getBits(Privilege[] privileges) throws AccessControlException {
        if (privileges == null || privileges.length == 0) {
            throw new AccessControlException();
        }
        int bits = NO_PRIVILEGE;
        for (int i = 0; i < privileges.length; i++) {
            Privilege priv = privileges[i];
            if (priv instanceof PrivilegeImpl) {
                bits |= ((PrivilegeImpl) priv).internalPrivilege.getBits();
            } else {
                throw new AccessControlException("Unknown privilege '" + priv.getName() + "'.");
            }
        }
        return bits;
    }

    /**
     *
     * @param bits
     * @return InternalPrivilege that corresponds to the given bits.
     */
    private static InternalPrivilege[] getInteralPrivileges(int bits) {
        Object key = new Integer(bits);
        if (BITS_TO_PRIVILEGES.containsKey(key)) {
            return (InternalPrivilege[]) BITS_TO_PRIVILEGES.get(key);
        } else {
            List privileges = new ArrayList();
            if ((bits & READ) == READ) {
                privileges.add(READ_PRIVILEGE);
            }
            if ((bits & WRITE) == WRITE) {
                privileges.add(WRITE_PRIVILEGE);
            } else {
                if ((bits & MODIFY_PROPERTIES) == MODIFY_PROPERTIES) {
                    privileges.add(MODIFY_PROPERTIES_PRIVILEGE);
                }
                if ((bits & ADD_CHILD_NODES) == ADD_CHILD_NODES) {
                    privileges.add(ADD_CHILD_NODES_PRIVILEGE);
                }
                if ((bits & REMOVE_CHILD_NODES) == REMOVE_CHILD_NODES) {
                    privileges.add(REMOVE_CHILD_NODES_PRIVILEGE);
                }
            }
            if ((bits & REMOVE_NODE) == REMOVE_NODE) {
                privileges.add(REMOVE_NODE_PRIVILEGE);
            }
            if ((bits & READ_AC) == READ_AC) {
                privileges.add(READ_AC_PRIVILEGE);
            }
            if ((bits & MODIFY_AC) == MODIFY_AC) {
                privileges.add(MODIFY_AC_PRIVILEGE);
            }

            InternalPrivilege[] privs;
            if (!privileges.isEmpty()) {
                privs = (InternalPrivilege[]) privileges.toArray(new InternalPrivilege[privileges.size()]);
                BITS_TO_PRIVILEGES.put(key, privs);
            } else {
                privs = new InternalPrivilege[0];
            }
            return privs;
        }
    }

    private static InternalPrivilege registerPrivilege(InternalPrivilege privilege) {
        REGISTERED_PRIVILEGES.add(privilege);
        BITS_TO_PRIVILEGES.put(new Integer(privilege.getBits()), new InternalPrivilege[] {privilege});
        return privilege;
    }

    //--------------------------------------------------------------------------
    /**
     * Internal representation of the registered privileges (without JCR
     * name).
     */
    private static class InternalPrivilege {

        private final Name name;
        private final boolean isAbstract = false;
        private final boolean isAggregate;
        private final InternalPrivilege[] declaredAggregates;
        private final Set aggregates;

        private final int bits;

        /**
         * Create a simple (non-aggregate) internal privilege.
         */
        private InternalPrivilege(String name, int bits) {
            if (name == null) {
                throw new IllegalArgumentException("A privilege must have a name.");
            }
            this.name = NAME_FACTORY.create(name);
            this.bits = bits;

            declaredAggregates = null;
            aggregates = null;
            isAggregate = false;
        }

        /**
         * Create an aggregate internal privilege
         */
        private InternalPrivilege(String name, InternalPrivilege[] declaredAggregates) {
            if (name == null) {
                throw new IllegalArgumentException("A privilege must have a name.");
            }
            this.name = NAME_FACTORY.create(name);
            this.declaredAggregates = declaredAggregates;

            Set aggrgt = new HashSet();
            int bts = 0;
            for (int i = 0; i < declaredAggregates.length; i++) {
                InternalPrivilege priv = declaredAggregates[i];
                bts |= priv.getBits();
                if (priv.isAggregate) {
                    aggrgt.addAll(priv.aggregates);
                } else {
                    aggrgt.add(priv);
                }
            }
            aggregates = Collections.unmodifiableSet(aggrgt);
            bits = bts;
            isAggregate = true;
        }

        int getBits() {
            return bits;
        }

        //---------------------------------------------------------< Object >---
        public int hashCode() {
            return bits;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof InternalPrivilege) {
                return bits == ((InternalPrivilege) obj).bits;
            }
            return false;
        }
    }

    /**
     * Simple wrapper used to provide an public representation of the
     * registered internal privileges properly exposing the JCR name.
     */
    private class PrivilegeImpl implements Privilege {

        private final InternalPrivilege internalPrivilege;
        private final NameResolver resolver;

        private PrivilegeImpl(InternalPrivilege internalPrivilege,
                              NameResolver resolver) {
            this.internalPrivilege = internalPrivilege;
            this.resolver = resolver;
        }

        public String getName() {
            try {
                return resolver.getJCRName(internalPrivilege.name);
            } catch (NamespaceException e) {
                // should not occur -> return internal name representation.
                return internalPrivilege.name.toString();
            }
        }

        public boolean isAbstract() {
            return internalPrivilege.isAbstract;
        }

        public boolean isAggregate() {
            return internalPrivilege.isAggregate;
        }

        public Privilege[] getDeclaredAggregatePrivileges() {
            if (internalPrivilege.isAggregate) {
                int len = internalPrivilege.declaredAggregates.length;
                Privilege[] privs = new Privilege[len];
                for (int i = 0; i < len; i++) {
                    InternalPrivilege ip = internalPrivilege.declaredAggregates[i];
                    privs[i] = (Privilege) localCache.get(ip.name);
                }
                return privs;
            } else {
                return EMPTY_ARRAY;
            }
        }

        public Privilege[] getAggregatePrivileges() {
            if (internalPrivilege.isAggregate) {
                Privilege[] privs = new Privilege[internalPrivilege.aggregates.size()];
                int i = 0;
                for (Iterator it = internalPrivilege.aggregates.iterator(); it.hasNext();) {
                    InternalPrivilege ip = (InternalPrivilege) it.next();
                    privs[i++] = (Privilege) localCache.get(ip.name);
                }
                return privs;
            } else {
                return EMPTY_ARRAY;
            }
        }

        public int hashCode() {
            return internalPrivilege.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof PrivilegeImpl) {
                PrivilegeImpl other = (PrivilegeImpl) obj;
                return internalPrivilege.equals(other.internalPrivilege);
            }
            return false;
        }
    }
}
