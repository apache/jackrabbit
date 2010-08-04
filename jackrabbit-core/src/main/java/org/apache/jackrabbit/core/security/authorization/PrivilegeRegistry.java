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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

/**
 * The <code>PrivilegeRegistry</code> defines the set of <code>Privilege</code>s
 * known to the repository.
 */
public final class PrivilegeRegistry {

    /**
     * Jackrabbit specific write privilege that combines {@link Privilege#JCR_WRITE}
     * and {@link Privilege#JCR_NODE_TYPE_MANAGEMENT}.
     */
    public static final String REP_WRITE = "{" + Name.NS_REP_URI + "}write";

    private static final Set<InternalPrivilege> REGISTERED_PRIVILEGES = new HashSet<InternalPrivilege>(20);
    private static final Map<Integer, InternalPrivilege[]> BITS_TO_PRIVILEGES = new HashMap<Integer, InternalPrivilege[]>();
    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    private static final Privilege[] EMPTY_ARRAY = new Privilege[0];

    public static final int NO_PRIVILEGE = 0;

    private static final int READ = 1;
    private static final int MODIFY_PROPERTIES = 2;
    private static final int ADD_CHILD_NODES = 4;
    private static final int REMOVE_CHILD_NODES = 8;
    private static final int REMOVE_NODE = 16;
    
    private static final int READ_AC = 32;
    private static final int MODIFY_AC = 64;

    private static final int NODE_TYPE_MNGMT = 128;
    private static final int VERSION_MNGMT = 256;
    private static final int LOCK_MNGMT = 512;
    private static final int LIFECYCLE_MNGMT = 1024;
    private static final int RETENTION_MNGMT = 2048;

    private static final int WRITE = 30;
    private static final int JACKRABBIT_WRITE = 158;
    private static final int ALL = 4095;

    private static final InternalPrivilege READ_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_READ, READ));
    private static final InternalPrivilege MODIFY_PROPERTIES_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_MODIFY_PROPERTIES, MODIFY_PROPERTIES));
    private static final InternalPrivilege ADD_CHILD_NODES_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_ADD_CHILD_NODES, ADD_CHILD_NODES));
    private static final InternalPrivilege REMOVE_CHILD_NODES_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_REMOVE_CHILD_NODES, REMOVE_CHILD_NODES));
    private static final InternalPrivilege REMOVE_NODE_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_REMOVE_NODE, REMOVE_NODE));

    private static final InternalPrivilege READ_AC_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_READ_ACCESS_CONTROL, READ_AC));
    private static final InternalPrivilege MODIFY_AC_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL, MODIFY_AC));

    private static final InternalPrivilege NODE_TYPE_MANAGEMENT_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT, NODE_TYPE_MNGMT));
    private static final InternalPrivilege VERSION_MANAGEMENT_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_VERSION_MANAGEMENT, VERSION_MNGMT));
    private static final InternalPrivilege LOCK_MANAGEMENT_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_LOCK_MANAGEMENT, LOCK_MNGMT));
    private static final InternalPrivilege LIFECYCLE_MANAGEMENT_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT, LIFECYCLE_MNGMT));
    private static final InternalPrivilege RETENTION_MANAGEMENT_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_RETENTION_MANAGEMENT, RETENTION_MNGMT));

    private static final InternalPrivilege WRITE_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_WRITE, new InternalPrivilege[] {
            MODIFY_PROPERTIES_PRIVILEGE,
            ADD_CHILD_NODES_PRIVILEGE,
            REMOVE_CHILD_NODES_PRIVILEGE,
            REMOVE_NODE_PRIVILEGE,
    }));

    private static final InternalPrivilege JACKRABBIT_WRITE_PRIVILEGE = registerPrivilege(new InternalPrivilege(REP_WRITE, new InternalPrivilege[] {
            WRITE_PRIVILEGE,
            NODE_TYPE_MANAGEMENT_PRIVILEGE
    }));

    private static final InternalPrivilege ALL_PRIVILEGE = registerPrivilege(new InternalPrivilege(Privilege.JCR_ALL, new InternalPrivilege[] {
            READ_PRIVILEGE,
            WRITE_PRIVILEGE,
            JACKRABBIT_WRITE_PRIVILEGE,
            READ_AC_PRIVILEGE,
            MODIFY_AC_PRIVILEGE,
            VERSION_MANAGEMENT_PRIVILEGE,
            LOCK_MANAGEMENT_PRIVILEGE,
            LIFECYCLE_MANAGEMENT_PRIVILEGE,
            RETENTION_MANAGEMENT_PRIVILEGE
    }));

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
    private final Map<Name, Privilege> localCache;

    /**
     * Create a new <code>PrivilegeRegistry</code> instance.
     *
     * @param resolver NameResolver used to calculate the JCR name of the
     * privileges.
     */
    public PrivilegeRegistry(NameResolver resolver) {
        this.resolver = resolver;
        localCache = new HashMap<Name, Privilege>(REGISTERED_PRIVILEGES.size());
        for (InternalPrivilege ip : REGISTERED_PRIVILEGES) {
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
        return localCache.values().toArray(new Privilege[localCache.size()]);
    }

    /**
     * Returns the privilege with the specified <code>privilegeName</code>.
     *
     * @param privilegeName Name of the principal.
     * @return the privilege with the specified <code>privilegeName</code>.
     * @throws AccessControlException If no privilege with the given name exists.
     * @throws RepositoryException If another error occurs.
     */
    public Privilege getPrivilege(String privilegeName) throws AccessControlException, RepositoryException {
        Name name = resolver.getQName(privilegeName);
        if (localCache.containsKey(name)) {
            return localCache.get(name);
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
     * @param bits Privilege bits as obtained from {@link #getBits(Privilege[])}.
     * @return Array of <code>Privilege</code>s that are presented by the given it
     * or an empty array if <code>bits</code> is lower than {@link #READ} or
     * cannot be resolved to registered <code>Privilege</code>s.
     * @see #getBits(Privilege[])
     */
    public Privilege[] getPrivileges(int bits) {
        Privilege[] privs;
        if (bits > NO_PRIVILEGE) {
            InternalPrivilege[] internalPrivs = getInteralPrivileges(bits);
            privs = new Privilege[internalPrivs.length];
            for (int i = 0; i < internalPrivs.length; i++) {
                privs[i] = localCache.get(internalPrivs[i].name);
            }
        } else {
            privs = new Privilege[0];
        }
        return privs;
    }

    /**
     * @param privileges An array of privileges.
     * @return The privilege bits.
     * @throws AccessControlException If the specified array is null
     * or if it contains an unregistered privilege.
     * @see #getPrivileges(int)
     */
    public static int getBits(Privilege[] privileges) throws AccessControlException {
        if (privileges == null || privileges.length == 0) {
            throw new AccessControlException("Privilege array is empty or null.");
        }
        int bits = NO_PRIVILEGE;
        for (Privilege priv : privileges) {
            if (priv instanceof PrivilegeImpl) {
                bits |= ((PrivilegeImpl) priv).internalPrivilege.getBits();
            } else {
                throw new AccessControlException("Unknown privilege '" + priv.getName() + "'.");
            }
        }
        return bits;
    }

    /**
     * Build the permissions granted by evaluating the given privileges.
     *
     * @param privs The privileges granted on the Node itself (for properties
     * the ACL of the direct ancestor).
     * @param parentPrivs The privileges granted on the parent of the Node. Not
     * relevant for properties since it only is used to determine permissions
     * on a Node (add_child_nodes, remove_child_nodes).
     * @param isAllow <code>true</code> if the privileges are granted; <code>false</code>
     * otherwise.
     * @param protectsPolicy If <code>true</code> the affected item itself
     * defines access control related information.
     * @return the permissions granted evaluating the given privileges.
     */
    public static int calculatePermissions(int privs, int parentPrivs, boolean isAllow, boolean protectsPolicy) {
        int perm = Permission.NONE;
        if (protectsPolicy) {
            if ((parentPrivs & READ_AC) == READ_AC) {
                perm |= Permission.READ;
            }
            if ((parentPrivs & MODIFY_AC) == MODIFY_AC) {
                perm |= Permission.ADD_NODE;
                perm |= Permission.SET_PROPERTY;
                perm |= Permission.REMOVE_NODE;
                perm |= Permission.REMOVE_PROPERTY;
                perm |= Permission.NODE_TYPE_MNGMT;
            }
        } else {
            if ((privs & READ) == READ) {
                perm |= Permission.READ;
            }
            if ((privs & MODIFY_PROPERTIES) == MODIFY_PROPERTIES) {
                perm |= Permission.SET_PROPERTY;
                perm |= Permission.REMOVE_PROPERTY;
            }
            // add_node permission is granted through privilege on the parent.
            if ((parentPrivs & ADD_CHILD_NODES) == ADD_CHILD_NODES) {
                perm |= Permission.ADD_NODE;
            }
            /*
             remove_node is
             allowed: only if remove_child_nodes privilege is present on
                      the parent AND remove_node is present on the node itself
             denied : if either remove_child_nodes is denied on the parent
                      OR remove_node is denied on the node itself.
            */
            if (isAllow) {
                if ((parentPrivs & REMOVE_CHILD_NODES) == REMOVE_CHILD_NODES &&
                        (privs & REMOVE_NODE) == REMOVE_NODE) {
                    perm |= Permission.REMOVE_NODE;
                }
            } else {
                if ((parentPrivs & REMOVE_CHILD_NODES) == REMOVE_CHILD_NODES ||
                        (privs & REMOVE_NODE) == REMOVE_NODE) {
                    perm |= Permission.REMOVE_NODE;
                }
            }
        }

        // the remaining (special) permissions are simply defined on the node
        if ((privs & READ_AC) == READ_AC) {
            perm |= Permission.READ_AC;
        }
        if ((privs & MODIFY_AC) == MODIFY_AC) {
            perm |= Permission.MODIFY_AC;
        }
        if ((privs & LIFECYCLE_MNGMT) == LIFECYCLE_MNGMT) {
            perm |= Permission.LIFECYCLE_MNGMT;
        }
        if ((privs & LOCK_MNGMT) == LOCK_MNGMT) {
            perm |= Permission.LOCK_MNGMT;
        }
        if ((privs & NODE_TYPE_MNGMT) == NODE_TYPE_MNGMT) {
            perm |= Permission.NODE_TYPE_MNGMT;
        }
        if ((privs & RETENTION_MNGMT) == RETENTION_MNGMT) {
            perm |= Permission.RETENTION_MNGMT;
        }
        if ((privs & VERSION_MNGMT) == VERSION_MNGMT) {
            perm |= Permission.VERSION_MNGMT;
        }
        return perm;
    }
    
    /**
     * @param bits The privilege bits.
     * @return InternalPrivilege that corresponds to the given bits.
     */
    private static InternalPrivilege[] getInteralPrivileges(int bits) {
        if (BITS_TO_PRIVILEGES.containsKey(bits)) {
            return BITS_TO_PRIVILEGES.get(bits);
        } else {
            List<InternalPrivilege> privileges = new ArrayList<InternalPrivilege>();
            if ((bits & READ) == READ) {
                privileges.add(READ_PRIVILEGE);
            }
            if ((bits & JACKRABBIT_WRITE) == JACKRABBIT_WRITE) {
                privileges.add(JACKRABBIT_WRITE_PRIVILEGE);
            } else if ((bits & WRITE) == WRITE) {
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
                if ((bits & REMOVE_NODE) == REMOVE_NODE) {
                    privileges.add(REMOVE_NODE_PRIVILEGE);
                }
                if ((bits & NODE_TYPE_MNGMT) == NODE_TYPE_MNGMT) {
                    privileges.add(NODE_TYPE_MANAGEMENT_PRIVILEGE);
                }
            }
            if ((bits & READ_AC) == READ_AC) {
                privileges.add(READ_AC_PRIVILEGE);
            }
            if ((bits & MODIFY_AC) == MODIFY_AC) {
                privileges.add(MODIFY_AC_PRIVILEGE);
            }
            if ((bits & VERSION_MNGMT) == VERSION_MNGMT) {
                privileges.add(VERSION_MANAGEMENT_PRIVILEGE);
            }
            if ((bits & LOCK_MNGMT) == LOCK_MNGMT) {
                privileges.add(LOCK_MANAGEMENT_PRIVILEGE);
            }
            if ((bits & LIFECYCLE_MNGMT) == LIFECYCLE_MNGMT) {
                privileges.add(LIFECYCLE_MANAGEMENT_PRIVILEGE);
            }
            if ((bits & RETENTION_MNGMT) == RETENTION_MNGMT) {
                privileges.add(RETENTION_MANAGEMENT_PRIVILEGE);
            }

            InternalPrivilege[] privs;
            if (!privileges.isEmpty()) {
                privs = privileges.toArray(new InternalPrivilege[privileges.size()]);
                BITS_TO_PRIVILEGES.put(bits, privs);
            } else {
                privs = new InternalPrivilege[0];
            }
            return privs;
        }
    }

    private static InternalPrivilege registerPrivilege(InternalPrivilege privilege) {
        REGISTERED_PRIVILEGES.add(privilege);
        BITS_TO_PRIVILEGES.put(privilege.getBits(), new InternalPrivilege[] {privilege});
        return privilege;
    }

    //--------------------------------------------------------------------------
    /**
     * Internal representation of the registered privileges (without JCR
     * name).
     */
    private static class InternalPrivilege {

        private final Name name;
        private final boolean isAbstract;
        private final boolean isAggregate;
        private final InternalPrivilege[] declaredAggregates;
        private final Set<InternalPrivilege> aggregates;

        private final int bits;

        /**
         * Create a simple (non-aggregate) internal privilege.
         * @param name The JCR name of the privilege in the extended form.
         * @param bits The privilege bits.
         */
        private InternalPrivilege(String name, int bits) {
            if (name == null) {
                throw new IllegalArgumentException("A privilege must have a name.");
            }
            this.name = NAME_FACTORY.create(name);
            this.bits = bits;

            isAbstract = false;
            declaredAggregates = null;
            aggregates = null;
            isAggregate = false;
        }

        /**
         * Create an aggregate internal privilege
         * @param name The JCR name of the privilege in its extended form.
         * @param declaredAggregates The declared aggregated privileges.
         */
        private InternalPrivilege(String name, InternalPrivilege[] declaredAggregates) {
            if (name == null) {
                throw new IllegalArgumentException("A privilege must have a name.");
            }
            this.name = NAME_FACTORY.create(name);
            this.isAbstract = false;
            this.declaredAggregates = declaredAggregates;
            Set<InternalPrivilege> aggrgt = new HashSet<InternalPrivilege>();
            int bts = 0;
            for (InternalPrivilege priv : declaredAggregates) {
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
                    privs[i] = localCache.get(ip.name);
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
                for (InternalPrivilege ip : internalPrivilege.aggregates) {
                    privs[i++] = localCache.get(ip.name);
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
