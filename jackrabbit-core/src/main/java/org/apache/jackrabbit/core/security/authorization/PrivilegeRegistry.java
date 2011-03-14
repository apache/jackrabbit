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

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.commons.privilege.ParseException;
import org.apache.jackrabbit.commons.privilege.PrivilegeDefinition;
import org.apache.jackrabbit.commons.privilege.PrivilegeDefinitionReader;
import org.apache.jackrabbit.commons.privilege.PrivilegeDefinitionWriter;
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
public final class PrivilegeRegistry {

    private static final Logger log = LoggerFactory.getLogger(PrivilegeRegistry.class);

    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    /**
     * Jackrabbit specific write privilege that combines {@link Privilege#JCR_WRITE}
     * and {@link Privilege#JCR_NODE_TYPE_MANAGEMENT}.
     */
    public static final String REP_WRITE = "{" + Name.NS_REP_URI + "}write";
    public static final Name REP_WRITE_NAME = NAME_FACTORY.create(REP_WRITE);

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
    }

    /**
     * Path to the file system resource used to persist custom privileges
     * registered with this repository.
     */
    private static final String CUSTOM_PRIVILEGES_RESOURCE_NAME = "/privileges/custom_privileges.xml";

    
    private final Map<Name, Definition> registeredPrivileges = new HashMap<Name, Definition>();
    private final Map<Integer, Set<Name>> bitsToNames = new HashMap<Integer, Set<Name>>();

    @SuppressWarnings("unchecked")    
    private final Map<Listener, Listener> listeners = Collections.synchronizedMap(new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK));

    private final NamespaceRegistry namespaceRegistry;
    private final CustomPrivilegeStore customPrivilegesStore;

    private final NameResolver resolver;

    private int nextBits = RETENTION_MNGMT << 1;

    public PrivilegeRegistry(NamespaceRegistry namespaceRegistry, FileSystem fs)
            throws RepositoryException {

        this.namespaceRegistry = namespaceRegistry;
        this.customPrivilegesStore = new CustomPrivilegeStore(new FileSystemResource(fs, CUSTOM_PRIVILEGES_RESOURCE_NAME));
        registerDefinitions(createBuiltInPrivilegeDefinitions());

        try {
            Map<Name, DefinitionStub> customDefs = customPrivilegesStore.load();
            Map<Name, Definition> definitions = createPrivilegeDefinitions(customDefs);
            registerDefinitions(definitions);
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
        registerDefinitions(createBuiltInPrivilegeDefinitions());

        namespaceRegistry = null;
        customPrivilegesStore = null;

        this.resolver = resolver;
    }

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
     * and calls {@link PrivilegeManagerImpl#getPrivileges(int)}.
     *
     * @param bits Privilege bits as obtained from {@link #getBits(Privilege[])}.
     * @return Array of <code>Privilege</code>s that are presented by the given it
     * or an empty array if <code>bits</code> is lower than {@link #READ} or
     * cannot be resolved to registered <code>Privilege</code>s.
     * @see #getBits(Privilege[])
     * @deprecated Use {@link PrivilegeManagerImpl#getPrivileges(int)} instead.
     */
    public Privilege[] getPrivileges(int bits) {
        return new PrivilegeManagerImpl(this, resolver).getPrivileges(bits);
    }

    /**
     * Best effort approach to calculate bits for built-in privileges. Throws
     * <code>UnsupportedOperationException</code> if the workaround fails.
     * Note, that the bits calculated for jcr:all does not include any
     * registered custom privileges.
     *
     * @param privileges An array of privileges.
     * @return The privilege bits.
     * @throws AccessControlException If the specified array is null
     * or if it contains an unregistered privilege.
     * @see #getPrivileges(int)
     * @deprecated Use {@link PrivilegeManagerImpl#getBits(javax.jcr.security.Privilege[])} instead.
     */
    public static int getBits(Privilege[] privileges) throws AccessControlException {
        if (privileges == null || privileges.length == 0) {
            throw new AccessControlException("Privilege array is empty or null.");
        }

        Map<String, String> lookup = new HashMap<String,String>(2);
        lookup.put(Name.NS_REP_PREFIX, Name.NS_REP_URI);
        lookup.put(Name.NS_JCR_PREFIX, Name.NS_JCR_URI);

        int bits = PrivilegeRegistry.NO_PRIVILEGE;
        for (Privilege priv : privileges) {
            String prefix = Text.getNamespacePrefix(priv.getName());
            if (lookup.containsKey(prefix)) {
                Name n = NAME_FACTORY.create(lookup.get(prefix), Text.getLocalName(priv.getName()));
                if (PRIVILEGE_NAMES.containsKey(n)) {
                    bits |= PRIVILEGE_NAMES.get(n);
                } else if (NameConstants.JCR_WRITE.equals(n)) {
                    bits |= createJcrWriteDefinition().bits;
                } else if (REP_WRITE_NAME.equals(n)) {
                    Definition jcrWrite = createJcrWriteDefinition();
                    bits |= createRepWriteDefinition(jcrWrite.bits).bits;
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

            // modify_child_node_collection permission is granted through
            // privileges on the parent
            if ((parentPrivs & ADD_CHILD_NODES) == ADD_CHILD_NODES &&
                    (parentPrivs & REMOVE_CHILD_NODES) == REMOVE_CHILD_NODES) {
                perm |= Permission.MODIFY_CHILD_NODE_COLLECTION;
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

    //-----------------------------------< methods used by PrivilegeManager >---
    /**
     * Validates and registers a new custom privilege definition with the
     * specified characteristics. Upon successful registration the new custom
     * definition is persisted in the corresponding file system resource.<p/>
     *
     * <p>The validation includes the following steps:</p>
     *
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
        if (customPrivilegesStore == null) {
            throw new UnsupportedOperationException("No privilege store defined.");
        }
        synchronized (registeredPrivileges) {
            Map<Name, DefinitionStub> stubs = Collections.singletonMap(privilegeName, new DefinitionStub(privilegeName, isAbstract, declaredAggregateNames, true));
            Map<Name, Definition> definitions = createPrivilegeDefinitions(stubs);
            try {
                // write the new custom privilege to the store and upon successful
                // update of the file system resource add finally it to the map of
                // registered privileges.
                customPrivilegesStore.append(definitions);
                registerDefinitions(definitions);

            } catch (IOException e) {
                throw new RepositoryException("Failed to register custom privilege " + privilegeName.toString(), e);
            } catch (FileSystemException e) {
                throw new RepositoryException("Failed to register custom privilege " + privilegeName.toString(), e);
            } catch (ParseException e) {
                throw new RepositoryException("Failed to register custom privilege " + privilegeName.toString(), e);
            }
        }

        for (Listener l : listeners.keySet()) {
            l.privilegeRegistered(privilegeName);
        }
    }
    
    /**
     * Returns all registered internal privileges.
     *
     * @return all registered internal privileges
     */
    Definition[] getAll() {
        return registeredPrivileges.values().toArray(new Definition[registeredPrivileges.size()]);
    }

    /**
     * Returns the internal privilege with the specified name or <code>null</code>.
     *
     * @param name Name of the internal privilege.
     * @return the internal privilege with the specified name or <code>null</code>
     */
    Definition get(Name name) {
        return registeredPrivileges.get(name);
    }

    /**
     * @param bits The privilege bits.
     * @return Privilege names that corresponds to the given bits.
     */
    Name[] getNames(int bits) {
        if (bitsToNames.containsKey(bits)) {
            Set<Name> ips = bitsToNames.get(bits);
            return ips.toArray(new Name[ips.size()]);
        } else {
            Set<Name> names = new HashSet<Name>();
            if ((bits & READ) == READ) {
                names.add(NameConstants.JCR_READ);
            }
            int repWrite = registeredPrivileges.get(REP_WRITE_NAME).bits;
            int jcrWrite = registeredPrivileges.get(NameConstants.JCR_WRITE).bits;
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

            // include matching custom privilege names 
            for (Definition def : registeredPrivileges.values()) {
                if (def.isCustom && ((bits & def.bits) == def.bits)) {
                    names.add(def.name);
                }
            }

            if (!names.isEmpty()) {
                bitsToNames.put(bits, names);
            }
            return names.toArray(new Name[names.size()]);
        }
    }

    void addListener(Listener listener) {
        listeners.put(listener,listener);
    }

    //---------------------------------------------< privilege registration >---
    /**
     * Adds the specified privilege definitions to the internal map(s) and
     * recalculates the jcr:all privilege definition.
     * 
     * @param definitions
     */
    private void registerDefinitions(Map<Name, Definition> definitions) {
        registeredPrivileges.putAll(definitions);
        for (Definition def : definitions.values()) {
            bitsToNames.put(def.bits, Collections.singleton(def.name));
        }

        if (!definitions.containsKey(NameConstants.JCR_ALL)) {
            // redefine the jcr:all privilege definition
            Definition all = registeredPrivileges.get(NameConstants.JCR_ALL);

            Set<Name> allAggrNames = all.declaredAggregateNames;
            allAggrNames.addAll(definitions.keySet());

            int allBits = all.bits;
            for (Definition def : definitions.values()) {
                allBits |= def.bits;
            }

            all = new Definition(NameConstants.JCR_ALL, false, allAggrNames, allBits);
            registeredPrivileges.put(NameConstants.JCR_ALL, all);
            bitsToNames.put(all.bits, Collections.singleton(NameConstants.JCR_ALL));
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
        defs.put(jcrWrite.name, jcrWrite);

        // rep:write
        Definition repWrite = createRepWriteDefinition(jcrWrite.bits);
        defs.put(repWrite.name, repWrite);

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
        jcrAllAggregates.add(NameConstants.JCR_WRITE);
        jcrAllAggregates.add(REP_WRITE_NAME);

        Definition jcrAll = new Definition(NameConstants.JCR_ALL, false, jcrAllAggregates, jcrAllBits);
        defs.put(jcrAll.name, jcrAll);

        return defs;
    }

    /**
     * Validates the specified <code>DefinitionStub</code>s and creates
     * new <code>PrivilegeDefinition</code>s. The validation includes name
     * validation and resolution of declared aggregate names. The latter
     * also includes checks to prevent cyclic aggregation.
     *  
     * @param toRegister
     * @return new privilege definitions.
     * @throws RepositoryException If any of the specified stubs is invalid.
     */
    private Map<Name, Definition> createPrivilegeDefinitions(Map<Name, DefinitionStub> toRegister) throws RepositoryException {
        Map<Name, Definition> definitions = new HashMap<Name, Definition>(toRegister.size());
        Set<DefinitionStub> aggregates = new HashSet<DefinitionStub>();

        for (DefinitionStub stub : toRegister.values()) {
            Name name = stub.name;
            if (name == null) {
                throw new RepositoryException("Name of custom privilege may not be null.");
            }
            if (registeredPrivileges.containsKey(name)) {
                throw new RepositoryException("Registered privilege with name " + name + " already exists.");
            }

            // namespace validation:
            // - make sure the specified name defines a registered namespace
            namespaceRegistry.getPrefix(stub.name.getNamespaceURI());
            // - and isn't one of the reserved namespaces
            if (((NamespaceRegistryImpl) namespaceRegistry).isReservedURI(name.getNamespaceURI())) {
                throw new RepositoryException("Failed to register custom privilege: Reserved namespace URI: " + name.getNamespaceURI());
            }

            // validate aggregates
            if (stub.declaredAggregateNames.isEmpty()) {
                // not an aggregate priv definition.
                definitions.put(name, new Definition(stub, nextBits()));
            } else {
                for (Name declaredAggregateName : stub.declaredAggregateNames) {
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
            for (Iterator<DefinitionStub> itr = aggregates.iterator(); itr.hasNext();) {
                DefinitionStub stub = itr.next();
                int bts = getAggregateBits(stub.declaredAggregateNames, definitions);
                if (bitsToNames.containsKey(bts) && bitsToNames.get(bts).size() == 1) {
                    Name existingName = bitsToNames.get(bts).iterator().next();
                    throw new RepositoryException("Custom aggregate privilege '" + stub.name + "' is already covered by '" + existingName.toString() + "'");
                }
                if (bts != NO_PRIVILEGE) {
                    Definition def = new Definition(stub, bts);
                    definitions.put(def.name, def);
                    itr.remove();
                }
            }

            if (cnt == aggregates.size()) {
                // none of the remaining aggregate-definitions could be processed
                throw new RepositoryException("Invalid aggregate privilege definition. Failed to resolve aggregate names.");
            }
        }

        return definitions;
    }

    /**
     *
     * @return
     */
    private int nextBits() {
        int b = nextBits;
        nextBits = nextBits << 1;
        return b;
    }

    /**
     *
     * @param declaredAggregateNames
     * @param toRegister
     * @return
     */
    private int getAggregateBits(Set<Name> declaredAggregateNames, Map<Name, Definition> toRegister) {
        int bts = NO_PRIVILEGE;
        for (Name n : declaredAggregateNames) {
            if (registeredPrivileges.containsKey(n)) {
                bts |= registeredPrivileges.get(n).bits;
            } else if (toRegister.containsKey(n)) {
                Definition def = toRegister.get(n);
                if (def.bits == NO_PRIVILEGE) {
                    // not yet processed dependency -> wait for next iteration.
                    return NO_PRIVILEGE;
                } else {
                    bts |= def.bits;
                }
            } else {
                // unknown dependency (should not get here)
                return NO_PRIVILEGE;
            }
        }
        return bts;
    }

    /**
     *
     * @param def
     * @param declaredAggregateName
     * @param toRegister
     * @return
     */
    private boolean isCircularAggregation(DefinitionStub def, Name declaredAggregateName, Map<Name, DefinitionStub> toRegister) {
        DefinitionStub d = toRegister.get(declaredAggregateName);
        if (d.declaredAggregateNames.isEmpty()) {
            return false;
        } else {
            boolean isCircular = false;
            for (Name n : d.declaredAggregateNames) {
                if (def.name.equals(n)) {
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

    private static Definition createRepWriteDefinition(int jcrWriteBits) {
        Set<Name> repWriteAggregates = new HashSet<Name>(2);
        repWriteAggregates.add(NameConstants.JCR_WRITE);
        repWriteAggregates.add(NameConstants.JCR_NODE_TYPE_MANAGEMENT);

        int repWriteBits = jcrWriteBits | PRIVILEGE_NAMES.get(NameConstants.JCR_NODE_TYPE_MANAGEMENT);
        return new Definition(REP_WRITE_NAME, false, repWriteAggregates, repWriteBits);
    }

    //--------------------------------------------------------------------------
    /**
     * Notification about new registered privileges
     */
    interface Listener {
        /**
         * @param privilegeName The name of the new privilege
         */
        void privilegeRegistered(Name privilegeName);
    }


    /**
     * Raw, non-validated stub of a PrivilegeDefinition
     */
    private static class DefinitionStub {
        
        protected final Name name;
        protected final boolean isAbstract;
        protected final Set<Name> declaredAggregateNames;
        protected final boolean isCustom;
        
        private int hashCode;

        private DefinitionStub(Name name, boolean isAbstract, Set<Name> declaredAggregateNames, boolean isCustom) {
            this.name = name;
            this.isAbstract = isAbstract;
            this.declaredAggregateNames = (declaredAggregateNames == null) ? Collections.<Name>emptySet() : declaredAggregateNames;
            this.isCustom = isCustom;
        }

        //---------------------------------------------------------< Object >---
        @Override
        public String toString() {
            return name.toString();
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int h = 17;
                h = 37 * h + name.hashCode();
                h = 37 * h + Boolean.valueOf(isAbstract).hashCode();
                h = 37 * h + declaredAggregateNames.hashCode();
                hashCode = h;
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof DefinitionStub) {
                DefinitionStub other = (DefinitionStub) obj;
                return name.equals(other.name)
                        && isAbstract==other.isAbstract
                        && declaredAggregateNames.equals(other.declaredAggregateNames);
            }
            return false;
        }
    }

    /**
     * Internal definition of a jcr level privilege.
     */
    static class Definition extends DefinitionStub {

        private final int bits;

        private Definition(DefinitionStub stub, int bits) {
            this(stub.name, stub.isAbstract, stub.declaredAggregateNames, bits, stub.isCustom);
        }

        private Definition(Name name, boolean isAbstract, int bits) {
            this(name, isAbstract, Collections.<Name>emptySet(), bits, false);
        }

        private Definition(Name name, boolean isAbstract, Set<Name> declaredAggregateNames, int bits) {
            this(name, isAbstract, declaredAggregateNames, bits, false);
        }

        private Definition(Name name, boolean isAbstract, Set<Name> declaredAggregateNames, int bits, boolean isCustom) {
            super(name, isAbstract, declaredAggregateNames, isCustom);
            if (bits == NO_PRIVILEGE) {
                throw new IllegalArgumentException("Failed to build int representation of PrivilegeDefinition.");
            } else {
                this.bits = bits;
            }
        }

        int getBits() {
            return bits;
        }

        Name getName() {
            return name;
        }

        boolean isAbstract() {
            return isAbstract;
        }

        Name[] getDeclaredAggregateNames() {
            if (declaredAggregateNames.isEmpty()) {
                return Name.EMPTY_ARRAY;
            } else {
                return declaredAggregateNames.toArray(new Name[declaredAggregateNames.size()]);
            }
        }
    }

    /**
     * CustomPrivilegeStore used to read and write custom privilege definitions
     * from/to a file system resource.
     */
    private class CustomPrivilegeStore {

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

        private Map<Name, DefinitionStub> load() throws FileSystemException, RepositoryException, ParseException, IOException {
            Map<Name, DefinitionStub> stubs = new LinkedHashMap<Name, DefinitionStub>();

            if (customPrivilegesResource.exists()) {
                InputStream in = customPrivilegesResource.getInputStream();
                try {
                    PrivilegeDefinitionReader pr = new PrivilegeDefinitionReader(in, "text/xml");
                    for (PrivilegeDefinition def : pr.getPrivilegeDefinitions()) {

                        Name privName = getName(def.getName());
                        boolean isAbstract = def.isAbstract();
                        Set<Name> declaredAggrNames = new HashSet<Name>();
                        for (String dan : def.getDeclaredAggregateNames()) {
                            declaredAggrNames.add(getName(dan));
                        }

                        if (stubs.containsKey(privName)) {
                            throw new RepositoryException("Duplicate entry for custom privilege with name " + privName.toString());
                        }
                        stubs.put(privName, new DefinitionStub(privName, isAbstract, declaredAggrNames, true));
                    }
                } finally {
                    in.close();
                }
            }
            return stubs;
        }

        private Name getName(String jcrName) throws RepositoryException {
            String uri = namespaceRegistry.getURI(Text.getNamespacePrefix(jcrName));
            return NAME_FACTORY.create(uri, Text.getLocalName(jcrName));
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
                String name = resolver.getJCRName(d.name);
                String uri = d.name.getNamespaceURI();
                nsMapping.put(namespaceRegistry.getPrefix(uri), uri);

                String[] aggrNames = new String[d.declaredAggregateNames.size()];
                int i = 0;
                for (Name dan : d.declaredAggregateNames) {
                    aggrNames[i++] = resolver.getJCRName(dan);
                    uri = d.name.getNamespaceURI();
                    nsMapping.put(namespaceRegistry.getPrefix(uri), uri);
                }
                PrivilegeDefinition pd = new PrivilegeDefinition(name, d.isAbstract, aggrNames);
                jcrDefs.add(pd);
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
