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

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.core.cluster.PrivilegeEventChannel;
import org.apache.jackrabbit.core.cluster.PrivilegeEventListener;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.commons.privilege.ParseException;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionReader;
import org.apache.jackrabbit.spi.commons.privilege.PrivilegeDefinitionWriter;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The <code>PrivilegeRegistry</code> defines the set of <code>Privilege</code>s
 * known to the repository.
 */
public final class PrivilegeRegistry implements PrivilegeEventListener {

    private static final Logger log = LoggerFactory.getLogger(PrivilegeRegistry.class);

    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    /**
     * Jackrabbit specific write privilege that combines {@link Privilege#JCR_WRITE}
     * and {@link Privilege#JCR_NODE_TYPE_MANAGEMENT}.
     */
    public static final String REP_WRITE = "{" + Name.NS_REP_URI + "}write";
    public static final Name REP_WRITE_NAME = NAME_FACTORY.create(REP_WRITE);

    /**
     * Jackrabbit specific privilege for privilege management.
     */
    public static final String REP_PRIVILEGE_MANAGEMENT = "{" + Name.NS_REP_URI + "}privilegeManagement";
    public static final Name REP_PRIVILEGE_MANAGEMENT_NAME = NAME_FACTORY.create(REP_PRIVILEGE_MANAGEMENT);

    /**
     * No privileges
     */
    public static final int NO_PRIVILEGE = 0;

    private static final int READ = 1;
    private static final int MODIFY_PROPERTIES = READ << 1;
    private static final int ADD_CHILD_NODES = MODIFY_PROPERTIES << 1;
    private static final int REMOVE_CHILD_NODES = ADD_CHILD_NODES << 1;
    private static final int REMOVE_NODE = REMOVE_CHILD_NODES << 1;
    private static final int READ_AC = REMOVE_NODE << 1;
    private static final int MODIFY_AC = READ_AC << 1;
    private static final int NODE_TYPE_MNGMT = MODIFY_AC << 1;
    private static final int VERSION_MNGMT = NODE_TYPE_MNGMT << 1;
    private static final int LOCK_MNGMT = VERSION_MNGMT << 1;
    private static final int LIFECYCLE_MNGMT = LOCK_MNGMT << 1;
    private static final int RETENTION_MNGMT = LIFECYCLE_MNGMT << 1;
    private static final int WORKSPACE_MNGMT = RETENTION_MNGMT << 1;
    private static final int NODE_TYPE_DEF_MNGMT = WORKSPACE_MNGMT << 1;
    private static final int NAMESPACE_MNGMT = NODE_TYPE_DEF_MNGMT << 1;
    private static final int PRIVILEGE_MNGMT = NAMESPACE_MNGMT << 1;
    
    private static final Map<Name, Integer> PRIVILEGE_NAMES = new HashMap<Name, Integer>();
    static {
        PRIVILEGE_NAMES.put(NameConstants.JCR_READ, READ);
        PRIVILEGE_NAMES.put(NameConstants.JCR_MODIFY_PROPERTIES, MODIFY_PROPERTIES);
        PRIVILEGE_NAMES.put(NameConstants.JCR_ADD_CHILD_NODES, ADD_CHILD_NODES);
        PRIVILEGE_NAMES.put(NameConstants.JCR_REMOVE_CHILD_NODES, REMOVE_CHILD_NODES);
        PRIVILEGE_NAMES.put(NameConstants.JCR_REMOVE_NODE, REMOVE_NODE);
        PRIVILEGE_NAMES.put(NameConstants.JCR_READ_ACCESS_CONTROL, READ_AC);
        PRIVILEGE_NAMES.put(NameConstants.JCR_MODIFY_ACCESS_CONTROL, MODIFY_AC);
        PRIVILEGE_NAMES.put(NameConstants.JCR_NODE_TYPE_MANAGEMENT, NODE_TYPE_MNGMT);
        PRIVILEGE_NAMES.put(NameConstants.JCR_VERSION_MANAGEMENT, VERSION_MNGMT);
        PRIVILEGE_NAMES.put(NameConstants.JCR_LOCK_MANAGEMENT, LOCK_MNGMT);
        PRIVILEGE_NAMES.put(NameConstants.JCR_LIFECYCLE_MANAGEMENT, LIFECYCLE_MNGMT);
        PRIVILEGE_NAMES.put(NameConstants.JCR_RETENTION_MANAGEMENT, RETENTION_MNGMT);
        PRIVILEGE_NAMES.put(NameConstants.JCR_WORKSPACE_MANAGEMENT, WORKSPACE_MNGMT);
        PRIVILEGE_NAMES.put(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT, NODE_TYPE_DEF_MNGMT);
        PRIVILEGE_NAMES.put(NameConstants.JCR_NAMESPACE_MANAGEMENT, NAMESPACE_MNGMT);
        PRIVILEGE_NAMES.put(REP_PRIVILEGE_MANAGEMENT_NAME, PRIVILEGE_MNGMT);
    }

    /**
     * Path to the file system resource used to persist custom privileges
     * registered with this repository.
     */
    private static final String CUSTOM_PRIVILEGES_RESOURCE_NAME = "/privileges/custom_privileges.xml";

    private final Map<Name, Definition> registeredPrivileges = new HashMap<Name, Definition>();
    private final Map<PrivilegeBits, Set<Name>> bitsToNames = new HashMap<PrivilegeBits, Set<Name>>();

    private final Map<Listener, Listener> listeners = Collections.synchronizedMap(new ReferenceMap<>(ReferenceStrength.WEAK, ReferenceStrength.WEAK));

    private final NamespaceRegistry namespaceRegistry;
    private final CustomPrivilegeStore customPrivilegesStore;

    private final NameResolver resolver;

    private PrivilegeBits nextBits = PrivilegeBits.getInstance(PRIVILEGE_MNGMT).nextBits();

    /**
     * Privilege event channel for clustering support.
     */
    private PrivilegeEventChannel eventChannel;

    /**
     * Create a new <code>PrivilegeRegistry</code> instance.
     *
     * @param namespaceRegistry
     * @param fs
     * @throws RepositoryException
     */
    public PrivilegeRegistry(NamespaceRegistry namespaceRegistry, FileSystem fs)
            throws RepositoryException {

        this.namespaceRegistry = namespaceRegistry;
        this.customPrivilegesStore = new CustomPrivilegeStore(new FileSystemResource(fs, CUSTOM_PRIVILEGES_RESOURCE_NAME));
        cacheDefinitions(createBuiltInPrivilegeDefinitions());

        try {
            Map<Name, PrivilegeDefinition> customDefs = customPrivilegesStore.load();
            Map<Name, Definition> definitions = createCustomDefinitions(customDefs);
            cacheDefinitions(definitions);
        } catch (IOException e) {
            throw new RepositoryException("Failed to load custom privileges", e);
        } catch (FileSystemException e) {
            throw new RepositoryException("Failed to load custom privileges", e);
        } catch (ParseException e) {
            throw new RepositoryException("Failed to load custom privileges", e);
        }

        this.resolver = new DefaultNamePathResolver(namespaceRegistry);
    }

    /**
     * Create a new <code>PrivilegeRegistry</code> instance defining only
     * built-in privileges.
     *
     * @param resolver
     * @deprecated Use {@link org.apache.jackrabbit.api.security.authorization.PrivilegeManager} instead.
     * @see org.apache.jackrabbit.api.JackrabbitWorkspace#getPrivilegeManager()
     */
    public PrivilegeRegistry(NameResolver resolver) {
        cacheDefinitions(createBuiltInPrivilegeDefinitions());

        namespaceRegistry = null;
        customPrivilegesStore = null;

        this.resolver = resolver;
    }


    //---------------------------------------------< PrivilegeEventListener >---
    /**
     * {@inheritDoc}
     * @see PrivilegeEventListener#externalRegisteredPrivileges(java.util.Collection)
     */
    public void externalRegisteredPrivileges(Collection<PrivilegeDefinition> definitions) throws RepositoryException {
        Map<Name, PrivilegeDefinition> defs = new HashMap<Name, PrivilegeDefinition>(definitions.size());
        for (PrivilegeDefinition d : definitions) {
            defs.put(d.getName(), d);
        }
        registerCustomDefinitions(defs);
    }

    //----------------------------------------< public methods : clustering >---

    /**
     * Set a clustering event channel to inform about changes.
     *
     * @param eventChannel event channel
     */
    public void setEventChannel(PrivilegeEventChannel eventChannel) {
        this.eventChannel = eventChannel;
        eventChannel.setListener(this);
    }

    //--------------------------------< public methods : privilege registry >---
    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @return all registered privileges.
     * @deprecated Use {@link org.apache.jackrabbit.api.security.authorization.PrivilegeManager#getRegisteredPrivileges()} instead.
     */
    public Privilege[] getRegisteredPrivileges() {
        try {
            return new PrivilegeManagerImpl(this, resolver).getRegisteredPrivileges();
        } catch (RepositoryException e) {
            throw new UnsupportedOperationException("No supported any more. Use PrivilegeManager#getRegisteredPrivileges() instead.");
        }
    }

    /**
     * Creates a new <code>PrivilegeManager</code> from the specified resolver
     * and calls {@link PrivilegeManagerImpl#getRegisteredPrivileges()}.
     *
     * @param privilegeName Name of the privilege.
     * @return the privilege with the specified <code>privilegeName</code>.
     * @throws AccessControlException If no privilege with the given name exists.
     * @throws RepositoryException If another error occurs.
     * @deprecated Use {@link org.apache.jackrabbit.api.security.authorization.PrivilegeManager#getPrivilege(String)} instead.
     */
    public Privilege getPrivilege(String privilegeName) throws AccessControlException, RepositoryException {
        return new PrivilegeManagerImpl(this, resolver).getPrivilege(privilegeName);
    }

    /**
     * Creates a new <code>PrivilegeManager</code> from the specified resolver
     * and calls {@link PrivilegeManagerImpl#getPrivileges(PrivilegeBits)}.
     *
     * @param bits Privilege bits as obtained from {@link #getBits(Privilege[])}.
     * @return Array of <code>Privilege</code>s that are presented by the given it
     * or an empty array if <code>bits</code> is lower than {@link #READ} or
     * cannot be resolved to registered <code>Privilege</code>s.
     * @see #getBits(Privilege[])
     * @deprecated Use {@link PrivilegeManagerImpl#getPrivileges(PrivilegeBits)} instead.
     */
    public Privilege[] getPrivileges(int bits) {
        Set<Privilege> prvs = new PrivilegeManagerImpl(this, resolver).getPrivileges(PrivilegeBits.getInstance(bits));
        return prvs.toArray(new Privilege[prvs.size()]);
    }

    /**
     * Best effort approach to calculate bits for built-in privileges. Throws
     * <code>UnsupportedOperationException</code> if the workaround fails.
     * 
     * @param privileges An array of privileges.
     * @return The privilege bits.
     * @throws AccessControlException If the specified array is null
     * or if it contains an unregistered privilege.
     * @see #getPrivileges(int)
     * @deprecated Use {@link PrivilegeManagerImpl#getBits(javax.jcr.security.Privilege...)} instead.
     */
    public static int getBits(Privilege[] privileges) throws AccessControlException {
        if (privileges == null || privileges.length == 0) {
            throw new AccessControlException("Privilege array is empty or null.");
        }

        Map<String, String> lookup = new HashMap<String,String>(2);
        lookup.put(Name.NS_REP_PREFIX, Name.NS_REP_URI);
        lookup.put(Name.NS_JCR_PREFIX, Name.NS_JCR_URI);

        int bits = NO_PRIVILEGE;
        for (Privilege priv : privileges) {
            String prefix = Text.getNamespacePrefix(priv.getName());
            if (lookup.containsKey(prefix)) {
                Name n = NAME_FACTORY.create(lookup.get(prefix), Text.getLocalName(priv.getName()));
                if (PRIVILEGE_NAMES.containsKey(n)) {
                    bits |= PRIVILEGE_NAMES.get(n);
                } else if (NameConstants.JCR_WRITE.equals(n)) {
                    bits |= createJcrWriteDefinition().bits.longValue();
                } else if (REP_WRITE_NAME.equals(n)) {
                    Definition jcrWrite = createJcrWriteDefinition();
                    bits |= createRepWriteDefinition(jcrWrite).bits.longValue();
                } else if (NameConstants.JCR_ALL.equals(n)) {
                    for (Name pn : PRIVILEGE_NAMES.keySet()) {
                        bits |= PRIVILEGE_NAMES.get(pn);
                    }
                } else {
                    throw new AccessControlException("Unknown privilege '" + priv.getName() + "'.");
                }
            } else {
                throw new AccessControlException("Unknown privilege '" + priv.getName() + "'.");
            }
        }
        return bits;
    }

    /**
     * Build the permissions granted by evaluating the given privileges. Note,
     * that only built-in privileges can be mapped to permissions. Any other
     * privileges will be ignored.
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
    public static int calculatePermissions(PrivilegeBits privs, PrivilegeBits parentPrivs, boolean isAllow, boolean protectsPolicy) {
        return calculatePermissions(privs.longValue(), parentPrivs.longValue(), isAllow, protectsPolicy);
    }

    /**
     * Build the permissions granted by evaluating the given privileges. Note,
     * that only built-in privileges can be mapped to permissions. Any other
     * privileges will be ignored.
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
     * @deprecated Use {@link #calculatePermissions(PrivilegeBits, PrivilegeBits, boolean, boolean)} instead.
     */
    public static int calculatePermissions(int privs, int parentPrivs, boolean isAllow, boolean protectsPolicy) {
        return calculatePermissions((long) privs, (long) parentPrivs, isAllow, protectsPolicy);
    }

    private static int calculatePermissions(long privs, long parentPrivs, boolean isAllow, boolean protectsPolicy) {
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

        // modify_child_node_collection permission is granted through
        // privileges on the parent
        if ((parentPrivs & ADD_CHILD_NODES) == ADD_CHILD_NODES &&
                (parentPrivs & REMOVE_CHILD_NODES) == REMOVE_CHILD_NODES) {
            perm |= Permission.MODIFY_CHILD_NODE_COLLECTION;
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
        if ((privs & WORKSPACE_MNGMT) == WORKSPACE_MNGMT) {
            perm |= Permission.WORKSPACE_MNGMT;
        }
        if ((privs & NODE_TYPE_DEF_MNGMT) == NODE_TYPE_DEF_MNGMT) {
            perm |= Permission.NODE_TYPE_DEF_MNGMT;
        }
        if ((privs & NAMESPACE_MNGMT) == NAMESPACE_MNGMT) {
            perm |= Permission.NAMESPACE_MNGMT;
        }
        if ((privs & PRIVILEGE_MNGMT) == PRIVILEGE_MNGMT) {
            perm |= Permission.PRIVILEGE_MNGMT;
        }
        return perm;
    }

    //-----------------------------------< methods used by PrivilegeManager >---
    /**
     * Validates and registers a new custom privilege definition with the
     * specified characteristics. Upon successful registration the new custom
     * definition is persisted in the corresponding file system resource.
     * <p>
     * The validation includes the following steps:
     * <ul>
     * <li>assert uniqueness of the specified privilegeName</li>
     * <li>make sure the name doesn't use a reserved namespace</li>
     * <li>assert that all names referenced in the specified name set refer
     * to existing privilege definitions.</li>
     * </ul>
     *
     * @param privilegeName
     * @param isAbstract
     * @param declaredAggregateNames
     * @throws RepositoryException If the privilege could not be registered due
     * to constraint violations or if persisting the custom privilege fails.
     */
    void registerDefinition(Name privilegeName, boolean isAbstract, Set<Name> declaredAggregateNames) throws RepositoryException {
        PrivilegeDefinition def = new PrivilegeDefinitionImpl(privilegeName, isAbstract, declaredAggregateNames);
        Map<Name, PrivilegeDefinition> stubs = Collections.singletonMap(privilegeName, def);
        registerCustomDefinitions(stubs);

        // inform clustering about the new privilege.
        if (eventChannel != null) {
            eventChannel.registeredPrivileges(stubs.values());
        }
    }
    
    /**
     * Returns all registered internal privileges.
     *
     * @return all registered internal privileges
     */
    PrivilegeDefinition[] getAll() {
        return registeredPrivileges.values().toArray(new Definition[registeredPrivileges.size()]);
    }

    /**
     * Returns the internal privilege with the specified name or <code>null</code>.
     *
     * @param name Name of the internal privilege.
     * @return the internal privilege with the specified name or <code>null</code>
     */
    PrivilegeDefinition get(Name name) {
        return registeredPrivileges.get(name);
    }

    /**
     * Returns the names of the privileges identified by the specified bits.
     * Note, that custom privileges don't have a integer representation as they
     * are not used for permission calculation.
     * 
     * @param privilegeBits The privilege bits.
     * @return Privilege names that corresponds to the given bits.
     */
    Name[] getNames(PrivilegeBits privilegeBits) {
        if (privilegeBits == null || privilegeBits.isEmpty()) {
            return Name.EMPTY_ARRAY;
        } else if (bitsToNames.containsKey(privilegeBits)) {
            // matches all built-in aggregates and single built-in privileges
            Set<Name> ips = bitsToNames.get(privilegeBits);
            return ips.toArray(new Name[ips.size()]);
        } else {
            // bits are a combination of built-in privileges.
            Set<Name> names = new HashSet<Name>();
            long bits = privilegeBits.longValue();
            if ((bits & READ) == READ) {
                names.add(NameConstants.JCR_READ);
            }
            long repWrite = registeredPrivileges.get(REP_WRITE_NAME).bits.longValue();
            long jcrWrite = registeredPrivileges.get(NameConstants.JCR_WRITE).bits.longValue();
            if ((bits & repWrite) == repWrite) {
                names.add(REP_WRITE_NAME);
            } else if ((bits & jcrWrite) == jcrWrite) {
                names.add(NameConstants.JCR_WRITE);
            } else {
                if ((bits & MODIFY_PROPERTIES) == MODIFY_PROPERTIES) {
                    names.add(NameConstants.JCR_MODIFY_PROPERTIES);
                }
                if ((bits & ADD_CHILD_NODES) == ADD_CHILD_NODES) {
                    names.add(NameConstants.JCR_ADD_CHILD_NODES);
                }
                if ((bits & REMOVE_CHILD_NODES) == REMOVE_CHILD_NODES) {
                    names.add(NameConstants.JCR_REMOVE_CHILD_NODES);
                }
                if ((bits & REMOVE_NODE) == REMOVE_NODE) {
                    names.add(NameConstants.JCR_REMOVE_NODE);
                }
                if ((bits & NODE_TYPE_MNGMT) == NODE_TYPE_MNGMT) {
                    names.add(NameConstants.JCR_NODE_TYPE_MANAGEMENT);
                }
            }
            if ((bits & READ_AC) == READ_AC) {
                names.add(NameConstants.JCR_READ_ACCESS_CONTROL);
            }
            if ((bits & MODIFY_AC) == MODIFY_AC) {
                names.add(NameConstants.JCR_MODIFY_ACCESS_CONTROL);
            }
            if ((bits & VERSION_MNGMT) == VERSION_MNGMT) {
                names.add(NameConstants.JCR_VERSION_MANAGEMENT);
            }
            if ((bits & LOCK_MNGMT) == LOCK_MNGMT) {
                names.add(NameConstants.JCR_LOCK_MANAGEMENT);
            }
            if ((bits & LIFECYCLE_MNGMT) == LIFECYCLE_MNGMT) {
                names.add(NameConstants.JCR_LIFECYCLE_MANAGEMENT);
            }
            if ((bits & RETENTION_MNGMT) == RETENTION_MNGMT) {
                names.add(NameConstants.JCR_RETENTION_MANAGEMENT);
            }
            if ((bits & WORKSPACE_MNGMT) == WORKSPACE_MNGMT) {
                names.add(NameConstants.JCR_WORKSPACE_MANAGEMENT);
            }
            if ((bits & NODE_TYPE_DEF_MNGMT) == NODE_TYPE_DEF_MNGMT) {
                names.add(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT);
            }
            if ((bits & NAMESPACE_MNGMT) == NAMESPACE_MNGMT) {
                names.add(NameConstants.JCR_NAMESPACE_MANAGEMENT);
            }
            if ((bits & PRIVILEGE_MNGMT) == PRIVILEGE_MNGMT) {
                names.add(REP_PRIVILEGE_MANAGEMENT_NAME);
            }

            // include matching custom privilege names
            Set<Name> customNames = new HashSet<Name>();
            Set<Definition> aggr = new HashSet<Definition>();
            for (Definition def : registeredPrivileges.values()) {
                if (def.isCustom && privilegeBits.includes(def.bits)) {
                    customNames.add(def.getName());
                    if (!def.getDeclaredAggregateNames().isEmpty()) {
                        aggr.add(def);
                    }
                }
            }
            // avoid redundant entries in case of aggregate privileges.
            for (Definition aggregate : aggr) {
                customNames.removeAll(aggregate.getDeclaredAggregateNames());
            }
            names.addAll(customNames);

            // remember this resolution.
            if (!names.isEmpty()) {
                bitsToNames.put(privilegeBits, names);
            }
            return names.toArray(new Name[names.size()]);
        }
    }

    /**
     * Return the privilege bits for the specified privilege definitions.
     *
     * @param definitions
     * @return privilege bits.
     */
    PrivilegeBits getBits(PrivilegeDefinition... definitions) {
        switch (definitions.length) {
            case 0:
                return PrivilegeBits.EMPTY;

            case 1:
                if (definitions[0] instanceof Definition) {
                    return ((Definition) definitions[0]).bits;
                } else {
                    return PrivilegeBits.EMPTY;
                }

            default:
                PrivilegeBits bts = PrivilegeBits.getInstance();
                for (PrivilegeDefinition d : definitions) {
                    if (d instanceof Definition) {
                        bts.add(((Definition) d).bits);
                    }
                }
                return bts;
        }
    }

    /**
     * Return the privilege bits for the specified privilege names.
     *
     * @param privilegeNames
     * @return privilege bits.
     */
    PrivilegeBits getBits(Name... privilegeNames) {
        switch (privilegeNames.length) {
            case 0:
                return PrivilegeBits.EMPTY;

            case 1:
                return getBits(privilegeNames[0]);

            default:
                PrivilegeBits bts = PrivilegeBits.getInstance();
                for (Name privName : privilegeNames) {
                    bts.add(getBits(privName));
                }
                return bts;
        }
    }

    /**
     * Return the privilege bits for the specified privilege name.
     *
     * @param privilegeName
     * @return privilege bits.
     */
    PrivilegeBits getBits(Name privilegeName) {
        Definition def = registeredPrivileges.get(privilegeName);
        return (def == null) ? PrivilegeBits.EMPTY : def.bits;
    }

    /**
     * Add a privilege registration listener.
     * 
     * @param listener
     */
    void addListener(Listener listener) {
        listeners.put(listener,listener);
    }

    /**
     * Removes a privilege registration listener.
     *
     * @param listener
     */
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    //---------------------------------------------< privilege registration >---
    /**
     * Register the specified custom privilege definitions.
     * 
     * @param stubs
     * @throws RepositoryException If an error occurs.
     */
    private void registerCustomDefinitions(Map<Name, PrivilegeDefinition> stubs) throws RepositoryException {
        if (customPrivilegesStore == null) {
            throw new UnsupportedOperationException("No privilege store defined.");
        }
        synchronized (registeredPrivileges) {
            Map<Name, Definition> definitions = createCustomDefinitions(stubs);
            try {
                // write the new custom privilege to the store and upon successful
                // update of the file system resource add finally it to the map of
                // registered privileges.
                customPrivilegesStore.append(definitions);
                cacheDefinitions(definitions);

            } catch (IOException e) {
                throw new RepositoryException("Failed to register custom privilegess.", e);
            } catch (FileSystemException e) {
                throw new RepositoryException("Failed to register custom privileges.", e);
            } catch (ParseException e) {
                throw new RepositoryException("Failed to register custom privileges.", e);
            }
        }

        for (Listener l : listeners.keySet()) {
            l.privilegesRegistered(stubs.keySet());
        }
    }

    /**
     * Adds the specified privilege definitions to the internal map(s) and
     * recalculates the jcr:all privilege definition.
     * 
     * @param definitions
     */
    private void cacheDefinitions(Map<Name, Definition> definitions) {
        registeredPrivileges.putAll(definitions);
        for (Definition def : definitions.values()) {
            bitsToNames.put(def.bits, Collections.singleton(def.getName()));
        }

        if (!definitions.containsKey(NameConstants.JCR_ALL)) {
            // redefine the jcr:all privilege definition
            Definition all = registeredPrivileges.get(NameConstants.JCR_ALL);
            bitsToNames.remove(all.bits);
            
            Set<Name> allAggrNames = new HashSet<Name>(all.getDeclaredAggregateNames());
            allAggrNames.addAll(definitions.keySet());

            PrivilegeBits allbits = PrivilegeBits.getInstance(all.bits);
            for (Definition d : definitions.values()) {
                allbits.add(d.bits);
            }

            Definition newAll = new Definition(NameConstants.JCR_ALL, false, allAggrNames, allbits, false);
            registeredPrivileges.put(NameConstants.JCR_ALL, newAll);
            bitsToNames.put(newAll.bits, Collections.singleton(NameConstants.JCR_ALL));
        }
    }
   
    /**
     * Creates <code>PrivilegeDefinition</code>s for all built-in privileges.
     * 
     * @return definitions for all built-in privileges.
     */
    private Map<Name, Definition> createBuiltInPrivilegeDefinitions() {
        Map<Name, Definition> defs = new HashMap<Name, Definition>();

        // all non-aggregate privileges
        int jcrAllBits = NO_PRIVILEGE;
        for (Name privilegeName : PRIVILEGE_NAMES.keySet()) {
            int bits = PRIVILEGE_NAMES.get(privilegeName);
            Definition def = new Definition(privilegeName, false, bits);
            defs.put(privilegeName, def);
            jcrAllBits |= bits;
        }

        // jcr:write
        Definition jcrWrite = createJcrWriteDefinition();
        defs.put(jcrWrite.getName(), jcrWrite);

        // rep:write
        Definition repWrite = createRepWriteDefinition(jcrWrite);
        defs.put(repWrite.getName(), repWrite);

        // jcr:all
        Set<Name> jcrAllAggregates = new HashSet<Name>(10);
        jcrAllAggregates.add(NameConstants.JCR_READ);
        jcrAllAggregates.add(NameConstants.JCR_READ_ACCESS_CONTROL);
        jcrAllAggregates.add(NameConstants.JCR_MODIFY_ACCESS_CONTROL);
        jcrAllAggregates.add(NameConstants.JCR_LOCK_MANAGEMENT);
        jcrAllAggregates.add(NameConstants.JCR_VERSION_MANAGEMENT);
        jcrAllAggregates.add(NameConstants.JCR_NODE_TYPE_MANAGEMENT);
        jcrAllAggregates.add(NameConstants.JCR_RETENTION_MANAGEMENT);
        jcrAllAggregates.add(NameConstants.JCR_LIFECYCLE_MANAGEMENT);
        jcrAllAggregates.add(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT);
        jcrAllAggregates.add(NameConstants.JCR_NAMESPACE_MANAGEMENT);
        jcrAllAggregates.add(NameConstants.JCR_WORKSPACE_MANAGEMENT);
        jcrAllAggregates.add(NameConstants.JCR_WRITE);
        jcrAllAggregates.add(REP_WRITE_NAME);
        jcrAllAggregates.add(REP_PRIVILEGE_MANAGEMENT_NAME);

        Definition jcrAll = new Definition(NameConstants.JCR_ALL, false, jcrAllAggregates, jcrAllBits);
        defs.put(jcrAll.getName(), jcrAll);

        return defs;
    }

    /**
     * Validates the specified <code>DefinitionStub</code>s and creates
     * new custom <code>PrivilegeDefinition</code>s. The validation includes name
     * validation and resolution of declared aggregate names. The latter
     * also includes checks to prevent cyclic aggregation.
     *  
     * @param toRegister
     * @return new privilege definitions.
     * @throws RepositoryException If any of the specified stubs is invalid.
     */
    private Map<Name, Definition> createCustomDefinitions(Map<Name, PrivilegeDefinition> toRegister) throws RepositoryException {
        Map<Name, Definition> definitions = new HashMap<Name, Definition>(toRegister.size());
        Set<PrivilegeDefinition> aggregates = new HashSet<PrivilegeDefinition>();

        for (PrivilegeDefinition stub : toRegister.values()) {
            Name name = stub.getName();
            if (name == null) {
                throw new RepositoryException("Name of custom privilege may not be null.");
            }
            if (registeredPrivileges.containsKey(name)) {
                throw new RepositoryException("Registered privilege with name " + name + " already exists.");
            }

            // namespace validation:
            // - make sure the specified name defines a registered namespace
            namespaceRegistry.getPrefix(name.getNamespaceURI());
            // - and isn't one of the reserved namespaces
            if (isReservedNamespaceURI(name.getNamespaceURI())) {
                throw new RepositoryException("Failed to register custom privilege: Reserved namespace URI: " + name.getNamespaceURI());
            }

            // validate aggregates
            Set<Name> dagn = stub.getDeclaredAggregateNames();
            if (dagn.isEmpty()) {
                // not an aggregate priv definition.
                definitions.put(name, new Definition(stub, nextBits()));
            } else {
                for (Name declaredAggregateName : dagn) {
                    if (name.equals(declaredAggregateName)) {
                        throw new RepositoryException("Declared aggregate name '"+ declaredAggregateName.toString() +"'refers to the same custom privilege.");
                    }
                    if (registeredPrivileges.containsKey(declaredAggregateName)) {
                        log.debug("Declared aggregate name '"+ declaredAggregateName.toString() +"' referring to registered privilege.");
                    } else if (toRegister.containsKey(declaredAggregateName)) {
                        log.debug("Declared aggregate name '"+ declaredAggregateName.toString() +"' referring to un-registered privilege.");
                        // need to check for circular aggregates
                        if (isCircularAggregation(stub, declaredAggregateName, toRegister)) {
                            throw new RepositoryException("Detected circular aggregation within custom privilege caused by " + declaredAggregateName.toString());
                        }
                    } else {
                        throw new RepositoryException("Found unresolvable name of declared aggregate privilege " + declaredAggregateName.toString());
                    }
                }
                // remember for further processing
                aggregates.add(stub);
            }
        }

        // process the aggregate stubs in order to calculate the 'bits'
        while (aggregates.size() > 0) {
            // monitor progress of resolution into proper definitions.
            int cnt = aggregates.size();

            // look for those definitions whose declared aggregates have all been processed.
            for (Iterator<PrivilegeDefinition> itr = aggregates.iterator(); itr.hasNext();) {
                PrivilegeDefinition stub = itr.next();
                PrivilegeBits bts = getAggregateBits(stub.getDeclaredAggregateNames(), definitions);
                if (!bts.isEmpty()) {
                    // make sure the same aggregation is not yet covered by an
                    // already registered privilege
                    if (bitsToNames.containsKey(bts) && bitsToNames.get(bts).size() == 1) {
                        Name existingName = bitsToNames.get(bts).iterator().next();
                        throw new RepositoryException("Custom aggregate privilege '" + stub.getName() + "' is already covered by '" + existingName.toString() + "'");
                    }
                    // ... nor is present within the set of definitions that have
                    // been created before for registration.
                    for (Definition d : definitions.values()) {
                        if (bts.equals(d.bits)) {
                            throw new RepositoryException("Custom aggregate privilege '" + stub.getName() + "' is already defined by '"+ d.getName()+"'");
                        }
                    }

                    // now its save to create the new definition
                    Definition def = new Definition(stub, bts);
                    definitions.put(def.getName(), def);
                    itr.remove();
                } // unresolvable bts -> postpone to next iterator.
            }

            if (cnt == aggregates.size()) {
                // none of the remaining aggregate-definitions could be processed
                throw new RepositoryException("Invalid aggregate privilege definition. Failed to resolve aggregate names.");
            }
        }

        return definitions;
    }

    private boolean isReservedNamespaceURI(String uri) {
        if (namespaceRegistry instanceof NamespaceRegistryImpl) {
            return ((NamespaceRegistryImpl) namespaceRegistry).isReservedURI(uri);
        } else {
            // hardcoded fallback
            return Name.NS_REP_URI.equals(uri)
                    || (uri.startsWith("http://www.w3.org"))
                    || uri.startsWith("http://www.jcp.org");
        }
    }

    /**
     *
     * @return
     */
    private PrivilegeBits nextBits() {
        PrivilegeBits b = nextBits;
        nextBits = nextBits.nextBits();
        return b;
    }

    /**
     *
     * @param declaredAggregateNames
     * @param toRegister
     * @return
     */
    private PrivilegeBits getAggregateBits(Set<Name> declaredAggregateNames, Map<Name, Definition> toRegister) {
        PrivilegeBits bts = PrivilegeBits.getInstance();
        for (Name n : declaredAggregateNames) {
            if (registeredPrivileges.containsKey(n)) {
                bts.add(registeredPrivileges.get(n).bits);
            } else if (toRegister.containsKey(n)) {
                Definition def = toRegister.get(n);
                bts.add(def.bits);
            } else {
                // unknown dependency (should not get here) -> return the empty set.
                return PrivilegeBits.EMPTY;
            }
        }
        return bts.unmodifiable();
    }

    /**
     *
     * @param def
     * @param declaredAggregateName
     * @param toRegister
     * @return
     */
    private boolean isCircularAggregation(PrivilegeDefinition def, Name declaredAggregateName, Map<Name, PrivilegeDefinition> toRegister) {
        PrivilegeDefinition d = toRegister.get(declaredAggregateName);
        if (d.getDeclaredAggregateNames().isEmpty()) {
            return false;
        } else {
            boolean isCircular = false;
            for (Name n : d.getDeclaredAggregateNames()) {
                if (def.getName().equals(n)) {
                    return true;
                }
                if (toRegister.containsKey(n)) {
                    isCircular = isCircularAggregation(def, n, toRegister);
                }
            }
            return isCircular;
        }
    }

    /**
     * @return PrivilegeDefinition for the jcr:write privilege
     */
    private static Definition createJcrWriteDefinition() {
        Set<Name> jcrWriteAggregates = new HashSet<Name>(4);
        jcrWriteAggregates.add(NameConstants.JCR_MODIFY_PROPERTIES);
        jcrWriteAggregates.add(NameConstants.JCR_ADD_CHILD_NODES);
        jcrWriteAggregates.add(NameConstants.JCR_REMOVE_CHILD_NODES);
        jcrWriteAggregates.add(NameConstants.JCR_REMOVE_NODE);

        int jcrWriteBits = NO_PRIVILEGE;
        for (Name privilegeName : jcrWriteAggregates) {
            jcrWriteBits |= PRIVILEGE_NAMES.get(privilegeName);
        }
        return new Definition(NameConstants.JCR_WRITE, false, jcrWriteAggregates, jcrWriteBits);
    }

    private static Definition createRepWriteDefinition(Definition jcrWrite) {
        Set<Name> repWriteAggregates = new HashSet<Name>(2);
        repWriteAggregates.add(NameConstants.JCR_WRITE);
        repWriteAggregates.add(NameConstants.JCR_NODE_TYPE_MANAGEMENT);

        long repWriteBits = jcrWrite.bits.longValue() | PRIVILEGE_NAMES.get(NameConstants.JCR_NODE_TYPE_MANAGEMENT);
        return new Definition(REP_WRITE_NAME, false, repWriteAggregates, repWriteBits);
    }

    //--------------------------------------------------------------------------
    /**
     * Notification about new registered privileges
     */
    interface Listener {
        /**
         * @param privilegeNames
         */
        void privilegesRegistered(Set<Name> privilegeNames);
    }

    /**
     * Internal definition of a JCR privilege extending from the general
     * privilege definition. It defines addition information that ease
     * the evaluation of privileges.
     */
    private final static class Definition extends PrivilegeDefinitionImpl {

        private final PrivilegeBits bits;
        private final boolean isCustom;

        private int hashCode;

        private Definition(PrivilegeDefinition stub, PrivilegeBits bits) {
            this(stub.getName(), stub.isAbstract(), stub.getDeclaredAggregateNames(), bits, true);
        }

        private Definition(Name name, boolean isAbstract, long bits) {
            this(name, isAbstract, Collections.<Name>emptySet(), PrivilegeBits.getInstance(bits), false);
        }

        private Definition(Name name, boolean isAbstract, Set<Name> declaredAggregateNames, long bits) {
            this(name, isAbstract, declaredAggregateNames, PrivilegeBits.getInstance(bits), false);
        }

        private Definition(Name name, boolean isAbstract, Set<Name> declaredAggregateNames, PrivilegeBits bits, boolean isCustom) {
            super(name, isAbstract, declaredAggregateNames);
            if (bits == null || bits.isEmpty()) {
                throw new IllegalArgumentException("Failed to build bit representation of PrivilegeDefinition.");
            } else {
                this.bits = bits.unmodifiable();
            }
            this.isCustom = isCustom;
        }

        //---------------------------------------------------------< Object >---
        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int h = super.hashCode();
                h = 37 * h + bits.hashCode();
                hashCode = h;
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Definition) {
                Definition other = (Definition) obj;
                return bits.equals(other.bits) && super.equals(other);
            }
            return false;
        }
    }

    /**
     * CustomPrivilegeStore used to read and write custom privilege definitions
     * from/to a file system resource.
     */
    private final class CustomPrivilegeStore {

        /**
         * File system resource used to persist custom privileges registered with
         * the repository.
         */
        private final FileSystemResource customPrivilegesResource;

        private CustomPrivilegeStore(FileSystemResource customPrivilegesResource) throws RepositoryException {
            this.customPrivilegesResource = customPrivilegesResource;
            try {
                // make sure path to resource exists
                if (!customPrivilegesResource.exists()) {
                    customPrivilegesResource.makeParentDirs();
                }
            } catch (FileSystemException e) {
                String error = "Internal error: Failed to access/create file system resource for custom privileges at " + customPrivilegesResource.getPath();
                log.debug(error);
                throw new RepositoryException(error, e);
            }
        }

        private Map<Name, PrivilegeDefinition> load() throws FileSystemException, RepositoryException, ParseException, IOException {
            Map<Name, PrivilegeDefinition> stubs = new LinkedHashMap<Name, PrivilegeDefinition>();

            if (customPrivilegesResource.exists()) {
                InputStream in = customPrivilegesResource.getInputStream();
                try {
                    PrivilegeDefinitionReader pr = new PrivilegeDefinitionReader(in, "text/xml");
                    for (PrivilegeDefinition def : pr.getPrivilegeDefinitions()) {
                        Name privName = def.getName();
                        if (stubs.containsKey(privName)) {
                            throw new RepositoryException("Duplicate entry for custom privilege with name " + privName.toString());
                        }
                        stubs.put(privName, def);
                    }
                } finally {
                    in.close();
                }
            }
            return stubs;
        }

        private void append(Map<Name, Definition> newPrivilegeDefinitions) throws IOException, FileSystemException, RepositoryException, ParseException {
            List<PrivilegeDefinition> jcrDefs;
            Map<String, String> nsMapping;

            if (customPrivilegesResource.exists()) {
                InputStream in = customPrivilegesResource.getInputStream();
                try {
                    PrivilegeDefinitionReader pr = new PrivilegeDefinitionReader(in, "text/xml");
                    jcrDefs = new ArrayList<PrivilegeDefinition>(Arrays.asList(pr.getPrivilegeDefinitions()));
                    nsMapping = pr.getNamespaces();
                } finally {
                    in.close();
                }
            } else {
                jcrDefs = new ArrayList<PrivilegeDefinition>();
                nsMapping = new HashMap<String, String>();
            }

            for (Definition d : newPrivilegeDefinitions.values()) {
                String uri = d.getName().getNamespaceURI();
                nsMapping.put(namespaceRegistry.getPrefix(uri), uri);

                for (Name dan : d.getDeclaredAggregateNames()) {
                    uri = dan.getNamespaceURI();
                    nsMapping.put(namespaceRegistry.getPrefix(uri), uri);
                }
                jcrDefs.add(d);
            }

            OutputStream out = customPrivilegesResource.getOutputStream();
            try {
                PrivilegeDefinitionWriter pdw = new PrivilegeDefinitionWriter("text/xml");
                pdw.writeDefinitions(out, jcrDefs.toArray(new PrivilegeDefinition[jcrDefs.size()]), nsMapping);
            } finally {
                out.close();
            }
        }
    }
}
