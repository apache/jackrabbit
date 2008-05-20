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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The <code>PrivilegeSet</code> represents a set of {@link Privilege}s.
 */
public final class PrivilegeRegistry {

    private static final Map REGISTERED_PRIVILEGES = new HashMap(10);
    private static final Map BITS_TO_PRIVILEGES = new HashMap();

    public static final int NO_PRIVILEGE = 0;
    public static final int READ = 1;
    public static final int MODIFY_PROPERTIES = 2;
    public static final int ADD_CHILD_NODES = 4;
    public static final int REMOVE_CHILD_NODES = 8;
    public static final int READ_AC = 16;
    public static final int MODIFY_AC =32;
    public static final int WRITE = 14;
    public static final int ALL = 63;

    public static final Privilege READ_PRIVILEGE = registerPrivilege(Privilege.READ, READ);
    public static final Privilege MODIFY_PROPERTIES_PRIVILEGE = registerPrivilege(Privilege.MODIFY_PROPERTIES, MODIFY_PROPERTIES);
    public static final Privilege ADD_CHILD_NODES_PRIVILEGE = registerPrivilege(Privilege.ADD_CHILD_NODES, ADD_CHILD_NODES);
    public static final Privilege REMOVE_CHILD_NODES_PRIVILEGE = registerPrivilege(Privilege.REMOVE_CHILD_NODES, REMOVE_CHILD_NODES);
    public static final Privilege READ_AC_PRIVILEGE = registerPrivilege(Privilege.READ_ACCESS_CONTROL, READ_AC);
    public static final Privilege MODIFY_AC_PRIVILEGE = registerPrivilege(Privilege.MODIFY_ACCESS_CONTROL, MODIFY_AC);
    public static final Privilege WRITE_PRIVILEGE = registerPrivilege(Privilege.WRITE, new PrivilegeImpl[] {
            (PrivilegeImpl) MODIFY_PROPERTIES_PRIVILEGE,
            (PrivilegeImpl) ADD_CHILD_NODES_PRIVILEGE,
            (PrivilegeImpl) REMOVE_CHILD_NODES_PRIVILEGE });
    public static final Privilege ALL_PRIVILEGE = registerPrivilege(new AllPrivilege());

    public static Privilege[] getRegisteredPrivileges() {
        return (Privilege[]) REGISTERED_PRIVILEGES.values().toArray(new Privilege[REGISTERED_PRIVILEGES.size()]);
    }

    public static Privilege[] getPrivileges(String[] privilegeNames) throws AccessControlException {
        if (privilegeNames == null || privilegeNames.length == 0) {
            throw new AccessControlException();
        }
        PrivilegeImpl[] privileges = new PrivilegeImpl[privilegeNames.length];
        for (int i = 0; i < privilegeNames.length; i++) {
            String name = privilegeNames[i];
            if (REGISTERED_PRIVILEGES.containsKey(name)) {
                privileges[i] = (PrivilegeImpl) REGISTERED_PRIVILEGES.get(name);
            } else {
                throw new AccessControlException("Unknown privilege " + name);
            }
        }
        return privileges;
    }

    /**
     *
     * @param privilegeNames
     * @return
     * @throws AccessControlException if the specified <code>privilegeNames</code>
     * are <code>null</code>, an empty array or if any of the names is not known
     * to this registry.
     */
    public static int getBits(String[] privilegeNames) throws AccessControlException {
        if (privilegeNames == null || privilegeNames.length == 0) {
            throw new AccessControlException();
        }
        PrivilegeImpl[] privileges = new PrivilegeImpl[privilegeNames.length];
        for (int i = 0; i < privilegeNames.length; i++) {
            String name = privilegeNames[i];
            if (REGISTERED_PRIVILEGES.containsKey(name)) {
                privileges[i] = (PrivilegeImpl) REGISTERED_PRIVILEGES.get(name);
            } else {
                throw new AccessControlException("Unknown privilege " + name);
            }
        }
        return PrivilegeImpl.getBits(privileges, false);
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
        return PrivilegeImpl.getBits(privileges, false);
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
    public static Privilege[] getPrivileges(int bits) {
        Privilege[] privs = new Privilege[0];
        if (bits > NO_PRIVILEGE) {
            PrivilegeImpl privilege = getPrivilege(bits);
            if (REGISTERED_PRIVILEGES.containsKey(privilege.getName())) {
                privs = new Privilege[] {privilege};
            } else {
                privs = privilege.getDeclaredAggregatePrivileges();
            }
        }
        return privs;
    }

    /**
     * Returns those bits from <code>bits</code> that are not present in
     * the <code>otherBits</code>, i.e. subtracts the other privileges from
     * this one.<br>
     * If the specified <code>otherBits</code> do not intersect with
     * <code>bits</code>,  <code>bits</code> are returned.<br>
     * If <code>bits</code> is included <code>otherBits</code>,
     * {@link #NO_PRIVILEGE} is returned.
     *
     * @param bits
     * @param otherBits
     * @return the differences of the 2 privileges or <code>{@link #NO_PRIVILEGE}</code>.
     */
    public static int diff(int bits, int otherBits) {
        return bits & ~otherBits;
    }

    /**
     *
     * @param name
     * @param description
     * @param privileges
     * @return new aggregate from the specified privileges.
     */
    private static PrivilegeImpl getPrivilege(String name, String description, PrivilegeImpl[] privileges) {
        return new AggregatePrivilege(name, description, privileges);
    }

    /**
     *
     * @param bits
     * @return PrivilegeImpl that corresponds to the given bits.
     */
    private static PrivilegeImpl getPrivilege(int bits) {
        PrivilegeImpl priv = null;
        Object key = new Integer(bits);
        if (BITS_TO_PRIVILEGES.containsKey(key)) {
            return (PrivilegeImpl) BITS_TO_PRIVILEGES.get(key);
        } else {
            // shortcut
            if ((bits & ALL) == ALL) {
                return (PrivilegeImpl) ALL_PRIVILEGE;
            }

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
            if ((bits & READ_AC) == READ_AC) {
                privileges.add(READ_AC_PRIVILEGE);
            }
            if ((bits & MODIFY_AC) == MODIFY_AC) {
                privileges.add(MODIFY_AC_PRIVILEGE);
            }

            if (!privileges.isEmpty()) {
                if (privileges.size() == 1) {
                    priv = (PrivilegeImpl) privileges.get(0);
                } else {
                    String name = "AggregatePrivilege" + bits;
                    priv = getPrivilege(name, null, (PrivilegeImpl[]) privileges.toArray(new PrivilegeImpl[privileges.size()]));
                }
                BITS_TO_PRIVILEGES.put(key, priv);
            }
            return priv;
        }
    }

    private static Privilege registerPrivilege(String name, int bits) {
        PrivilegeImpl priv = new SimplePrivilege(name, null, bits);
        return registerPrivilege(priv);
    }

    private static Privilege registerPrivilege(String name, PrivilegeImpl[] privileges) {
        PrivilegeImpl priv = getPrivilege(name, null, privileges);
        return registerPrivilege(priv);
    }

    private static Privilege registerPrivilege(PrivilegeImpl privilege) {
        REGISTERED_PRIVILEGES.put(privilege.getName(), privilege);
        BITS_TO_PRIVILEGES.put(new Integer(privilege.getBits()), privilege);
        return privilege;
    }

    //--------------------------------------------------------------------------
    /**
     * Avoid instanciation of the <code>PrivilegeRegistry</code>.
     */
    private PrivilegeRegistry() {}

    //--------------------------------------------------------------------------
    /**
     *
     */
    private static abstract class PrivilegeImpl implements Privilege {

        static final Privilege[] EMPTY_ARRAY = new Privilege[0];

        private final String name;
        private final String description;

        private PrivilegeImpl(String name, String description) {
            if (name == null) {
                throw new IllegalArgumentException("A privilege must have a name.");
            }
            this.name = name;
            this.description = description;
        }

        abstract int getBits();

        //------------------------------------------------------< Privilege >---
        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isAbstract() {
            return false;
        }

        /**
         * @param  privileges to get the bit representation for
         * @param ignoreUnkown
         * @return bit representing the Action
         * @throws AccessControlException If <code>ignoreUnkown</code> is false
         * and any of the specified privileges not a registered privilege.
         */
        private static int getBits(Privilege[] privileges, boolean ignoreUnkown) throws AccessControlException {
            int bits = NO_PRIVILEGE;
            for (int i = 0; i < privileges.length; i++) {
                Privilege priv = privileges[i];
                if (priv instanceof PrivilegeImpl) {
                    bits |= ((PrivilegeImpl) priv).getBits();
                } else if (priv.isAggregate()) {
                    // try to resolve the aggregates
                    bits |= getBits(priv.getDeclaredAggregatePrivileges(), ignoreUnkown);
                } else {
                    PrivilegeImpl p = (PrivilegeImpl) REGISTERED_PRIVILEGES.get(priv.getName());
                    if (p != null) {
                        bits |= p.getBits();
                    } else if (!ignoreUnkown) {
                        throw new AccessControlException("Unknown privilege '" + priv.getName() + "'.");
                    }
                }
            }
            return bits;
        }

        //---------------------------------------------------------< Object >---
        public int hashCode() {
            return getBits();
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof PrivilegeImpl) {
                return getBits() == ((PrivilegeImpl) obj).getBits();
            }
            return false;
        }
    }

    /**
     * Simple (non-aggregate) privilege.
     */
    private static class SimplePrivilege extends PrivilegeImpl {

        private final int bits;

        private SimplePrivilege(String name, String description, int bits) {
            super(name, description);
            this.bits = bits;
        }

        int getBits() {
            return bits;
        }

        //------------------------------------------------------< Privilege >---

        public boolean isAggregate() {
            return false;
        }

        public Privilege[] getDeclaredAggregatePrivileges() {
            return EMPTY_ARRAY;
        }

        public Privilege[] getAggregatePrivileges() {
            return EMPTY_ARRAY;
        }
    }

    /**
     * Aggregate privilege
     */
    private static class AggregatePrivilege extends PrivilegeImpl {

        private final Privilege[] declaredAggregates;
        private final Set aggregates;
        private final int bits;

        private AggregatePrivilege(String name, String description, PrivilegeImpl[] declaredAggregates) {
            super(name, description);
            this.declaredAggregates = declaredAggregates;
            Set aggrgt = new HashSet();
            int bts = 0;
            for (int i = 0; i < declaredAggregates.length; i++) {
                PrivilegeImpl priv = declaredAggregates[i];
                bts |= priv.getBits();
                if (priv.isAggregate()) {
                    aggrgt.addAll(Arrays.asList(priv.getAggregatePrivileges()));
                } else {
                    aggrgt.add(priv);
                }
            }
            aggregates = Collections.unmodifiableSet(aggrgt);
            bits = bts;
        }

        int getBits() {
            return bits;
        }

        //------------------------------------------------------< Privilege >---
        public boolean isAggregate() {
            return true;
        }

        public Privilege[] getDeclaredAggregatePrivileges() {
            return declaredAggregates;
        }

        public Privilege[] getAggregatePrivileges() {
            return (Privilege[]) aggregates.toArray(new Privilege[aggregates.size()]);
        }
    }

    /**
     * The ALL privilege
     */
    private static class AllPrivilege extends PrivilegeImpl {

        private AllPrivilege() {
            super(Privilege.ALL, null);
        }

        int getBits() {
            return PrivilegeRegistry.ALL;
        }

        //------------------------------------------------------< Privilege >---
        public boolean isAggregate() {
            return true;
        }

        public Privilege[] getDeclaredAggregatePrivileges() {
            return getAggregatePrivileges();
        }

        public Privilege[] getAggregatePrivileges() {
            Set all = new HashSet(REGISTERED_PRIVILEGES.values());
            for (Iterator it = all.iterator(); it.hasNext();) {
                Privilege priv = (Privilege) it.next();
                if (priv.isAggregate()) {
                    it.remove();
                }
            }
            return (Privilege[]) all.toArray(new Privilege[all.size()]);
        }
    }
}
