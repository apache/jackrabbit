/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.nodetype;

import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.log4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.OnParentVersionAction;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

/**
 * A <code>NodeTypeRegistry</code> ...
 */
public class NodeTypeRegistry {
    private static Logger log = Logger.getLogger(NodeTypeRegistry.class);

    // some well known node type names
    // rep:root
    public static final QName REP_ROOT =
            new QName(NamespaceRegistryImpl.NS_REP_URI, "root");
    // rep:system
    public static final QName REP_SYSTEM =
            new QName(NamespaceRegistryImpl.NS_REP_URI, "system");
    // nt:unstructured
    public static final QName NT_UNSTRUCTURED =
            new QName(NamespaceRegistryImpl.NS_NT_URI, "unstructured");
    // nt:base
    public static final QName NT_BASE =
            new QName(NamespaceRegistryImpl.NS_NT_URI, "base");
    // nt:hierarchyNode
    public static final QName NT_HIERARCHYNODE =
            new QName(NamespaceRegistryImpl.NS_NT_URI, "hierarchyNode");
    // nt:resource
    public static final QName NT_RESOURCE =
            new QName(NamespaceRegistryImpl.NS_NT_URI, "resource");
    // nt:query
    public static final QName NT_QUERY =
            new QName(NamespaceRegistryImpl.NS_NT_URI, "query");
    // mix:referenceable
    public static final QName MIX_REFERENCEABLE =
            new QName(NamespaceRegistryImpl.NS_MIX_URI, "referenceable");
    // mix:lockable
    public static final QName MIX_LOCKABLE =
            new QName(NamespaceRegistryImpl.NS_MIX_URI, "lockable");
    // mix:versionable
    public static final QName MIX_VERSIONABLE =
            new QName(NamespaceRegistryImpl.NS_MIX_URI, "versionable");
    // nt:versionHistory
    public static final QName NT_VERSION_HISTORY =
            new QName(NamespaceRegistryImpl.NS_NT_URI, "versionHistory");
    // nt:version
    public static final QName NT_VERSION =
            new QName(NamespaceRegistryImpl.NS_NT_URI, "version");
    // nt:frozenVersionableChild
    public static final QName NT_FROZEN_VERSIONABLE_CHILD =
            new QName(NamespaceRegistryImpl.NS_NT_URI, "frozenVersionableChild");
    // jcr:primaryType
    public static final QName JCR_PRIMARY_TYPE =
            new QName(NamespaceRegistryImpl.NS_JCR_URI, "primaryType");

    private static final String BUILTIN_NODETYPES_RESOURCE_PATH =
            "org/apache/jackrabbit/core/nodetype/builtin_nodetypes.xml";

    private static final String CUSTOM_NODETYPES_RESOURCE_NAME = "custom_nodetypes.xml";

    // file system where node type registrations are persisted
    private final FileSystem ntStore;
    /**
     * resource holding custom node type definitions which are represented as
     * nodes in the repository; it is needed in order to make the registrations
     * persistent.
     */
    private final FileSystemResource customNodeTypesResource;

    // persistent node type definitions of built-in & custom node types
    private final NodeTypeDefStore builtInNTDefs;
    private final NodeTypeDefStore customNTDefs;

    // cache of pre-built aggregations of node types
    private final EffectiveNodeTypeCache entCache;

    // map of node type names and node type definitions
    private final HashMap registeredNTDefs;

    // definition of the root node
    private final ChildNodeDef rootNodeDef;

    // map of id's and property definitions
    private final HashMap propDefs;
    // map of id's and node definitions
    private final HashMap nodeDefs;

    /**
     * namespace registry for resolving prefixes and namespace URI's;
     * used for (de)serializing node type definitions
     */
    private final NamespaceRegistryImpl nsReg;

    /**
     * FIXME
     * flag used to temporarily disable checking that auto-created properties
     * have a default value; this check has to be disabled while validating
     * built-in node types because there are properties defined in built-in
     * node types which are auto-created but don't have a fixed default value
     * that can be exposed in a property definition because it is
     * system-generated (e.g. jcr:primaryType in nt:base).
     */
    private boolean checkAutoCreatePropHasDefault = true;

    /**
     * Listeners (soft references)
     */
    private final Map listeners =
            Collections.synchronizedMap(new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK));

    /**
     * Create a new <code>NodeTypeRegistry</codes>
     *
     * @param nsReg
     * @param ntStore
     * @return <code>NodeTypeRegistry</codes> object
     * @throws RepositoryException
     */
    public static NodeTypeRegistry create(NamespaceRegistryImpl nsReg, FileSystem ntStore)
            throws RepositoryException {
        NodeTypeRegistry ntMgr = new NodeTypeRegistry(nsReg, ntStore);
        return ntMgr;
    }

    /**
     * Private constructor
     *
     * @param nsReg
     * @param ntStore
     * @throws RepositoryException
     */
    private NodeTypeRegistry(NamespaceRegistryImpl nsReg, FileSystem ntStore)
            throws RepositoryException {
        this.nsReg = nsReg;
        this.ntStore = ntStore;
        customNodeTypesResource = new FileSystemResource(this.ntStore, CUSTOM_NODETYPES_RESOURCE_NAME);
        try {
            // make sure path to resource exists
            if (!customNodeTypesResource.exists()) {
                customNodeTypesResource.makeParentDirs();
            }
        } catch (FileSystemException fse) {
            String error = "internal error: invalid resource: " + customNodeTypesResource.getPath();
            log.error(error, fse);
            throw new RepositoryException(error, fse);
        }

        entCache = new EffectiveNodeTypeCache();
        registeredNTDefs = new HashMap();
        propDefs = new HashMap();
        nodeDefs = new HashMap();

        // setup definition of root node
        rootNodeDef = createRootNodeDef();
        nodeDefs.put(new NodeDefId(rootNodeDef), rootNodeDef);

        // load and register pre-defined (i.e. built-in) node types
        /**
         * temporarily disable checking that auto-create properties have
         * default values
         */
        checkAutoCreatePropHasDefault = false;
        builtInNTDefs = new NodeTypeDefStore();
        InputStream in = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream(BUILTIN_NODETYPES_RESOURCE_PATH);
            builtInNTDefs.load(in);
            internalRegister(builtInNTDefs.all());
        } catch (IOException ioe) {
            String error = "internal error: failed to read built-in node type definitions stored in " + BUILTIN_NODETYPES_RESOURCE_PATH;
            log.error(error, ioe);
            throw new RepositoryException(error, ioe);
        } catch (InvalidNodeTypeDefException intde) {
            String error = "internal error: invalid built-in node type definition stored in " + BUILTIN_NODETYPES_RESOURCE_PATH;
            log.error(error, intde);
            throw new RepositoryException(error, intde);
        } finally {
            /**
             * re-enable checking that auto-create properties have default values
             */
            checkAutoCreatePropHasDefault = true;
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }

        // load and register custom node types
        customNTDefs = new NodeTypeDefStore();
        in = null;
        try {
            if (customNodeTypesResource.exists()) {
                in = customNodeTypesResource.getInputStream();
            }
        } catch (FileSystemException fse) {
            String error = "internal error: failed to access custom node type definitions stored in " + customNodeTypesResource.getPath();
            log.error(error, fse);
            throw new RepositoryException(error, fse);
        }
        if (in == null) {
            log.info("no custom node type definitions found");
        } else {
            try {
                customNTDefs.load(in);
                internalRegister(customNTDefs.all());
            } catch (IOException ioe) {
                String error = "internal error: failed to read custom node type definitions stored in " + customNodeTypesResource.getPath();
                log.error(error, ioe);
                throw new RepositoryException(error, ioe);
            } catch (InvalidNodeTypeDefException intde) {
                String error = "internal error: invalid custom node type definition stored in " + customNodeTypesResource.getPath();
                log.error(error, intde);
                throw new RepositoryException(error, intde);
            } finally {
                try {
                    in.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    private static ChildNodeDef createRootNodeDef() {
        ChildNodeDef def = new ChildNodeDef();

        // FIXME need a fake declaring node type
        def.setDeclaringNodeType(new QName(NamespaceRegistryImpl.NS_DEFAULT_URI, ""));
        def.setRequiredPrimaryTypes(new QName[]{REP_ROOT});
        def.setDefaultPrimaryType(REP_ROOT);
        def.setMandatory(true);
        def.setProtected(false);
        def.setOnParentVersion(OnParentVersionAction.VERSION);
        def.setAllowSameNameSibs(false);
        def.setAutoCreate(true);
        return def;
    }

    /**
     * Validates and registers the specified collection of <code>NodeTypeDef</code>
     * objects. An <code>InvalidNodeTypeDefException</code> is thrown if the
     * validation of any of the contained <code>NodeTypeDef</code> objects fails.
     * <p/>
     * Note that in the case an exception is thrown, some node types might have
     * been nevertheless successfully registered.
     *
     * @param ntDefs collection of <code>NodeTypeDef</code> objects
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     * @see #registerNodeType
     */
    private synchronized void internalRegister(Collection ntDefs)
            throws InvalidNodeTypeDefException, RepositoryException {
        ArrayList list = new ArrayList(ntDefs);

        // iterate over definitions until there are no more definitions with
        // unresolved (i.e. unregistered) dependencies or an error occurs;

        int count = -1;  // number of registered nt's per iteration
        while (list.size() > 0 && count != 0) {
            count = 0;
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                NodeTypeDef ntd = (NodeTypeDef) iterator.next();
                // check if definition has unresolved dependencies
                if (registeredNTDefs.keySet().containsAll(ntd.getDependencies())) {
                    // try to register it
                    internalRegister(ntd);
                    // remove it from list
                    iterator.remove();
                    // increase count
                    count++;
                }
            }
        }
        if (list.size() > 0) {
            StringBuffer msg = new StringBuffer();
            msg.append("the following node types could not be registered because of unresolvable dependencies: ");
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                msg.append(((NodeTypeDef) iterator.next()).getName());
                msg.append(" ");
            }
            log.error(msg.toString());
            throw new InvalidNodeTypeDefException(msg.toString());
        }
    }

    private EffectiveNodeType internalRegister(NodeTypeDef ntd)
            throws InvalidNodeTypeDefException, RepositoryException {
        QName name = ntd.getName();
        if (name != null && registeredNTDefs.containsKey(name)) {
            String msg = name + " already exists";
            log.error(msg);
            throw new InvalidNodeTypeDefException(msg);
        }

        EffectiveNodeType ent = validateNodeTypeDef(ntd);

        // store new effective node type instance
        entCache.put(ent);

        // register clone of node type definition
        try {
            ntd = (NodeTypeDef) ntd.clone();
        } catch (CloneNotSupportedException e) {
            // should never get here
            log.fatal("internal error", e);
            throw new InternalError(e.getMessage());
        }
        registeredNTDefs.put(name, ntd);

        // store property & child node definitions of new node type by id
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef def = pda[i];
            PropDefId id = new PropDefId(def);
            propDefs.put(id, def);
        }
        ChildNodeDef[] nda = ntd.getChildNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            ChildNodeDef def = nda[i];
            NodeDefId id = new NodeDefId(def);
            nodeDefs.put(id, def);
        }

        return ent;
    }

    private void internalUnregister(QName name)
            throws NoSuchNodeTypeException, RepositoryException {
        if (!registeredNTDefs.containsKey(name)) {
            throw new NoSuchNodeTypeException(name.toString());
        }
        if (builtInNTDefs.contains(name)) {
            throw new RepositoryException(name.toString() + ": can't unregister built-in node type.");
        }

        NodeTypeDef ntd = (NodeTypeDef) registeredNTDefs.get(name);
        registeredNTDefs.remove(name);
        /**
         * remove all affected effective node types from aggregates cache
         * (collect keys first to prevent ConcurrentModificationException)
         */
        Iterator iter = entCache.keys();
        ArrayList keys = new ArrayList();
        while (iter.hasNext()) {
            keys.add(iter.next());
        }
        iter = keys.iterator();
        while (iter.hasNext()) {
            WeightedKey k = (WeightedKey) iter.next();
            EffectiveNodeType ent = (EffectiveNodeType) entCache.get(k);
            if (ent.includesNodeType(name)) {
                entCache.remove(k);
            }
        }

        // remove property & child node definitions
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDefId id = new PropDefId(pda[i]);
            propDefs.remove(id);
        }
        ChildNodeDef[] nda = ntd.getChildNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            NodeDefId id = new NodeDefId(nda[i]);
            nodeDefs.remove(id);
        }
    }

    private void persistCustomNTDefs() throws RepositoryException {
        OutputStream out = null;
        try {
            out = customNodeTypesResource.getOutputStream();
            customNTDefs.store(out, nsReg);
        } catch (IOException ioe) {
            String error = "internal error: failed to persist custom node type definitions to " + customNodeTypesResource.getPath();
            log.error(error, ioe);
            throw new RepositoryException(error, ioe);
        } catch (FileSystemException fse) {
            String error = "internal error: failed to persist custom node type definitions to " + customNodeTypesResource.getPath();
            log.error(error, fse);
            throw new RepositoryException(error, fse);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    /**
     * Add a <code>NodeTypeRegistryListener</code>
     *
     * @param listener the new listener to be informed on (un)registration
     *                 of node types
     */
    void addListener(NodeTypeRegistryListener listener) {
        if (!listeners.containsKey(listener)) {
            listeners.put(listener, listener);
        }
    }

    /**
     * Remove a <code>NodeTypeRegistryListener</code>
     *
     * @param listener an existing listener
     */
    void removeListener(NodeTypeRegistryListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify the listeners that a node type <code>ntName</code> has been registered.
     */
    private void notifyRegistered(QName ntName) {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] la = new NodeTypeRegistryListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (NodeTypeRegistryListener) iter.next();
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].nodeTypeRegistered(ntName);
            }
        }
    }

    /**
     * Notify the listeners that a node type <code>ntName</code> has been re-registered.
     */
    private void notifyReRegistered(QName ntName) {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] la = new NodeTypeRegistryListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (NodeTypeRegistryListener) iter.next();
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].nodeTypeReRegistered(ntName);
            }
        }
    }

    /**
     * Notify the listeners that a node type <code>ntName</code> has been unregistered.
     */
    private void notifyUnregistered(QName ntName) {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] la = new NodeTypeRegistryListener[listeners.size()];
        Iterator iter = listeners.values().iterator();
        int cnt = 0;
        while (iter.hasNext()) {
            la[cnt++] = (NodeTypeRegistryListener) iter.next();
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].nodeTypeUnregistered(ntName);
            }
        }
    }

    private EffectiveNodeType validateNodeTypeDef(NodeTypeDef ntd)
            throws InvalidNodeTypeDefException, RepositoryException {

        /**
         * the effective (i.e. merged and resolved) node type resulting from
         * the specified node type definition;
         * the effective node type will finally be created after the definition
         * has been verified and checked for conflicts etc.; in some cases it
         * will be created already at an earlier stage during the validation
         * of child node definitions
         */
        EffectiveNodeType ent = null;

        QName name = ntd.getName();
        if (name == null) {
            String msg = "no name specified";
            log.error(msg);
            throw new InvalidNodeTypeDefException(msg);
        }

        // validate supertypes
        QName[] supertypes = ntd.getSupertypes();
        if (supertypes != null && supertypes.length > 0) {
            for (int i = 0; i < supertypes.length; i++) {
                /**
                 * simple check for infinite recursion
                 * (won't trap recursion on a deeper inheritance level)
                 */
                if (name.equals(supertypes[i])) {
                    String msg = "[" + name + "] invalid supertype: "
                            + supertypes[i] + " (infinite recursion))";
                    log.error(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
                if (!registeredNTDefs.containsKey(supertypes[i])) {
                    String msg = "[" + name + "] invalid supertype: " + supertypes[i];
                    log.error(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
            }

            /**
             * check for circularity in inheritance chain
             * ('a' extends 'b' extends 'a')
             */
            Stack inheritanceChain = new Stack();
            inheritanceChain.push(name);
            checkForCircularInheritance(supertypes, inheritanceChain);
        }

        /**
         * note that infinite recursion through inheritance is automatically
         * being checked by the following call to getEffectiveNodeType()
         * as it's impossible to register an node type definition which
         * references a supertype that isn't registered yet...
         */

        /**
         * build effective (i.e. merged and resolved) node type from supertypes
         * and check for conflicts
         */
        if (supertypes != null && supertypes.length > 0) {
            try {
                EffectiveNodeType est = getEffectiveNodeType(supertypes);
                // make sure that all primary types except nt:base extend from nt:base
                if (!ntd.isMixin() && !NT_BASE.equals(ntd.getName()) &&
                        !est.includesNodeType(NT_BASE)) {
                    String msg = "[" + name
                            + "] all primary node types except nt:base itself must be (directly or indirectly) derived from nt:base";
                    log.error(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
            } catch (NodeTypeConflictException ntce) {
                String msg = "[" + name + "] failed to validate supertypes";
                log.error(msg, ntce);
                throw new InvalidNodeTypeDefException(msg, ntce);
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = "[" + name + "] failed to validate supertypes";
                log.error(msg, nsnte);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        } else {
            // no supertypes specified: has to be either a mixin type or nt:base
            if (!ntd.isMixin() && !NT_BASE.equals(ntd.getName())) {
                String msg = "[" + name
                        + "] all primary node types except nt:base itself must be (directly or indirectly) derived from nt:base";
                log.error(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
        }

        // validate property definitions
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            /**
             * sanity check:
             * make sure declaring node type matches name of node type definition
             */
            if (!name.equals(pd.getDeclaringNodeType())) {
                String msg = "[" + name + "#" + pd.getName()
                        + "] invalid declaring node type specified";
                log.error(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // check that auto-created properties specify a name
            if (pd.definesResidual() && pd.isAutoCreate()) {
                String msg = "[" + name + "#" + pd.getName()
                        + "] auto-created properties must specify a name";
                log.error(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            /**
             * check default values:
             * make sure type of value is consistent with required property type
             */
            InternalValue[] defVals = pd.getDefaultValues();
            if (defVals != null && defVals.length != 0) {
                int reqType = pd.getRequiredType();
                for (int j = 0; j < defVals.length; j++) {
                    if (reqType == PropertyType.UNDEFINED) {
                        reqType = defVals[j].getType();
                    } else {
                        if (defVals[j].getType() != reqType) {
                            String msg = "[" + name + "#" + pd.getName()
                                    + "] type of default value(s) is not consistent with required property type";
                            log.error(msg);
                            throw new InvalidNodeTypeDefException(msg);
                        }
                    }
                }
            } else {
                // no default values specified
                if (checkAutoCreatePropHasDefault) {
                    // auto-created properties must have a default value
                    if (pd.isAutoCreate()) {
                        String msg = "[" + name + "#" + pd.getName()
                                + "] auto-created property must have a default value";
                        log.error(msg);
                        throw new InvalidNodeTypeDefException(msg);
                    }
                }
            }

            // check that default values satisfy value constraints
            ValueConstraint[] constraints = pd.getValueConstraints();
            if (constraints != null && constraints.length > 0) {
                if (defVals != null && defVals.length > 0) {
                    // check value constraints on every value
                    for (int j = 0; j < defVals.length; j++) {
                        // constraints are OR-ed together
                        boolean satisfied = false;
                        ConstraintViolationException cve = null;
                        for (int k = 0; k < constraints.length; k++) {
                            try {
                                constraints[k].check(defVals[j]);
                                // at least one constraint is satisfied
                                satisfied = true;
                                break;
                            } catch (ConstraintViolationException e) {
                                cve = e;
                                continue;
                            }
                        }
                        if (!satisfied) {
                            // report last exception we encountered
                            String msg = "[" + name + "#" + pd.getName()
                                    + "] default value does not satisfy value constraint";
                            log.error(msg, cve);
                            throw new InvalidNodeTypeDefException(msg, cve);
                        }
                    }
                }

                /**
                 * ReferenceConstraint:
                 * the specified node type must be registered, with one notable
                 * exception: the node type just being registered
                 */
                if (pd.getRequiredType() == PropertyType.REFERENCE) {
                    for (int j = 0; j < constraints.length; j++) {
                        ReferenceConstraint rc = (ReferenceConstraint) constraints[j];
                        QName ntName = rc.getNodeTypeName();
                        if (!name.equals(ntName) && !registeredNTDefs.containsKey(ntName)) {
                            String msg = "[" + name + "#" + pd.getName()
                                    + "] invalid REFERENCE value constraint '"
                                    + ntName + "' (unknown node type)";
                            log.error(msg);
                            throw new InvalidNodeTypeDefException(msg);
                        }
                    }
                }
            }
        }

        // validate child-node definitions
        ChildNodeDef[] cnda = ntd.getChildNodeDefs();
        for (int i = 0; i < cnda.length; i++) {
            ChildNodeDef cnd = cnda[i];
            /**
             * sanity check:
             * make sure declaring node type matches name of node type definition
             */
            if (!name.equals(cnd.getDeclaringNodeType())) {
                String msg = "[" + name + "#" + cnd.getName()
                        + "] invalid declaring node type specified";
                log.error(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // check that auto-created child-nodes specify a name
            if (cnd.definesResidual() && cnd.isAutoCreate()) {
                String msg = "[" + name + "#" + cnd.getName()
                        + "] auto-created child-nodes must specify a name";
                log.error(msg);
                throw new InvalidNodeTypeDefException(msg);
            }
            // check default primary type
            QName dpt = cnd.getDefaultPrimaryType();
            boolean referenceToSelf = false;
            EffectiveNodeType defaultENT = null;
            if (dpt != null) {
                // check if this node type specifies itself as default primary type
                if (name.equals(dpt)) {
                    referenceToSelf = true;
                }
                /**
                 * the default primary type must be registered, with one notable
                 * exception: the node type just being registered
                 */
                if (!name.equals(dpt) && !registeredNTDefs.containsKey(dpt)) {
                    String msg = "[" + name + "#" + cnd.getName()
                            + "] invalid default primary type '" + dpt + "'";
                    log.error(msg);
                    throw new InvalidNodeTypeDefException(msg);
                }
                /**
                 * build effective (i.e. merged and resolved) node type from
                 * default primary type and check for conflicts
                 */
                try {
                    if (!referenceToSelf) {
                        defaultENT = getEffectiveNodeType(dpt);
                    } else {
                        /**
                         * the default primary type is identical with the node
                         * type just being registered; we have to instantiate it
                         * 'manually'
                         */
                        ent = EffectiveNodeType.create(this, ntd);
                        defaultENT = ent;
                    }
                    if (cnd.isAutoCreate()) {
                        /**
                         * check for circularity through default primary types
                         * of auto-created child nodes (node type 'a' defines
                         * auto-created child node with default primary type 'a')
                         */
                        Stack definingNTs = new Stack();
                        definingNTs.push(name);
                        checkForCircularNodeAutoCreation(defaultENT, definingNTs);
                    }
                } catch (NodeTypeConflictException ntce) {
                    String msg = "[" + name + "#" + cnd.getName()
                            + "] failed to validate default primary type";
                    log.error(msg, ntce);
                    throw new InvalidNodeTypeDefException(msg, ntce);
                } catch (NoSuchNodeTypeException nsnte) {
                    String msg = "[" + name + "#" + cnd.getName()
                            + "] failed to validate default primary type";
                    log.error(msg, nsnte);
                    throw new InvalidNodeTypeDefException(msg, nsnte);
                }
            }

            // check required primary types
            QName[] reqTypes = cnd.getRequiredPrimaryTypes();
            if (reqTypes != null && reqTypes.length > 0) {
                for (int n = 0; n < reqTypes.length; n++) {
                    QName rpt = reqTypes[n];
                    referenceToSelf = false;
                    /**
                     * check if this node type specifies itself as required
                     * primary type
                     */
                    if (name.equals(rpt)) {
                        referenceToSelf = true;
                    }
                    /**
                     * the required primary type must be registered, with one
                     * notable exception: the node type just being registered
                     */
                    if (!name.equals(rpt) && !registeredNTDefs.containsKey(rpt)) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] invalid required primary type: " + rpt;
                        log.error(msg);
                        throw new InvalidNodeTypeDefException(msg);
                    }
                    /**
                     * check if default primary type satisfies the required
                     * primary type constraint
                     */
                    if (defaultENT != null && !defaultENT.includesNodeType(rpt)) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] default primary type does not satisfy required primary type constraint "
                                + rpt;
                        log.error(msg);
                        throw new InvalidNodeTypeDefException(msg);
                    }
                    /**
                     * build effective (i.e. merged and resolved) node type from
                     * required primary type constraint and check for conflicts
                     */
                    try {
                        if (!referenceToSelf) {
                            getEffectiveNodeType(rpt);
                        } else {
                            /**
                             * the required primary type is identical with the
                             * node type just being registered; we have to
                             * instantiate it 'manually'
                             */
                            if (ent == null) {
                                ent = EffectiveNodeType.create(this, ntd);
                            }
                        }
                    } catch (NodeTypeConflictException ntce) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] failed to validate required primary type constraint";
                        log.error(msg, ntce);
                        throw new InvalidNodeTypeDefException(msg, ntce);
                    } catch (NoSuchNodeTypeException nsnte) {
                        String msg = "[" + name + "#" + cnd.getName()
                                + "] failed to validate required primary type constraint";
                        log.error(msg, nsnte);
                        throw new InvalidNodeTypeDefException(msg, nsnte);
                    }
                }
            }
        }

        /**
         * now build effective (i.e. merged and resolved) node type from
         * this node type definition; this will potentially detect more
         * conflicts or problems
         */
        if (ent == null) {
            try {
                ent = EffectiveNodeType.create(this, ntd);
            } catch (NodeTypeConflictException ntce) {
                String msg = "[" + name + "] failed to resolve node type definition";
                log.error(msg, ntce);
                throw new InvalidNodeTypeDefException(msg, ntce);
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = "[" + name + "] failed to resolve node type definition";
                log.error(msg, nsnte);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }
        return ent;
    }

    /**
     * @return
     */
    synchronized QName[] getRegisteredNodeTypes() {
        return (QName[]) registeredNTDefs.keySet().toArray(new QName[registeredNTDefs.size()]);
    }

    /**
     * @return
     */
    public ChildNodeDef getRootNodeDef() {
        return rootNodeDef;
    }

    /**
     *
     * @param ntName
     * @return
     * @throws NoSuchNodeTypeException
     */
    public synchronized EffectiveNodeType getEffectiveNodeType(QName ntName)
            throws NoSuchNodeTypeException {
        // 1. make sure that the specified node type is registered
        if (!registeredNTDefs.containsKey(ntName)) {
            throw new NoSuchNodeTypeException(ntName.toString());
        }

        // 2. check if effective node type has already been built
        WeightedKey key = new WeightedKey(new QName[]{ntName});
        if (entCache.contains(key)) {
            return entCache.get(key);
        }

        // 3. build effective node type
        try {
            EffectiveNodeType ent = EffectiveNodeType.create(this, ntName);
            // store new effective node type
            entCache.put(ent);
            return ent;
        } catch (NodeTypeConflictException ntce) {
            // should never get here as all registered node types have to be valid!
            String msg = "internal error: encountered invalid registered node type " + ntName;
            log.error(msg, ntce);
            throw new NoSuchNodeTypeException(msg, ntce);
        }
    }

    /**
     *
     * @param ntNames
     * @return
     * @throws NodeTypeConflictException
     * @throws NoSuchNodeTypeException
     */
    public synchronized EffectiveNodeType getEffectiveNodeType(QName[] ntNames)
            throws NodeTypeConflictException, NoSuchNodeTypeException {
        // 1. make sure every single node type is registered
        for (int i = 0; i < ntNames.length; i++) {
            if (!registeredNTDefs.containsKey(ntNames[i])) {
                throw new NoSuchNodeTypeException(ntNames[i].toString());
            }
        }

        WeightedKey key = new WeightedKey(ntNames);

        // 2. check if aggregate has already been built
        if (entCache.contains(key)) {
            return entCache.get(key);
        }

        // 3. build aggregate
        EffectiveNodeType result = null;

        // build list of 'best' existing sub-aggregates
        ArrayList tmpResults = new ArrayList();
        while (key.size() > 0) {
            // check if we've already built this aggregate
            if (entCache.contains(key)) {
                tmpResults.add(entCache.get(key));
                // subtract the result from the temporary key
                // (which is 'empty' now)
                key = key.subtract(key);
                break;
            }
            /**
             * walk list of existing aggregates sorted by 'weight' of
             * aggregate (i.e. the cost of building it)
             */
            boolean foundSubResult = false;
            Iterator iter = entCache.keys();
            while (iter.hasNext()) {
                WeightedKey k = (WeightedKey) iter.next();
                /**
                 * check if the existing aggregate is a 'subset' of the one
                 * we're looking for
                 */
                if (key.contains(k)) {
                    tmpResults.add(entCache.get(k));
                    // subtract the result from the temporary key
                    key = key.subtract(k);
                    foundSubResult = true;
                    break;
                }
            }
            if (!foundSubResult) {
                /**
                 * no matching sub-aggregates found:
                 * build aggregate of remaining node types through iteration
                 */
                QName[] remainder = key.toArray();
                for (int i = 0; i < remainder.length; i++) {
                    EffectiveNodeType ent = null;
                    ent = EffectiveNodeType.create(this, remainder[i]);
                    // store new effective node type
                    entCache.put(ent);
                    if (result == null) {
                        result = ent;
                    } else {
                        result = result.merge(ent);
                        // store intermediate result (sub-aggregate)
                        entCache.put(result);
                    }
                }
                // add aggregate of remaining node types to result list
                tmpResults.add(result);
                break;
            }
        }
        // merge the sub-aggregates into new effective node type
        for (int i = 0; i < tmpResults.size(); i++) {
            if (result == null) {
                result = (EffectiveNodeType) tmpResults.get(i);
            } else {
                result = result.merge((EffectiveNodeType) tmpResults.get(i));
                // store intermediate result
                entCache.put(result);
            }
        }
        // we're done
        return result;
    }

    void checkForCircularInheritance(QName[] supertypes, Stack inheritanceChain)
            throws InvalidNodeTypeDefException, RepositoryException {
        for (int i = 0; i < supertypes.length; i++) {
            QName nt = supertypes[i];
            int pos = inheritanceChain.lastIndexOf(nt);
            if (pos >= 0) {
                StringBuffer buf = new StringBuffer();
                for (int j = 0; j < inheritanceChain.size(); j++) {
                    if (j == pos) {
                        buf.append("--> ");
                    }
                    buf.append(inheritanceChain.get(j));
                    buf.append(" extends ");
                }
                buf.append("--> ");
                buf.append(nt);
                throw new InvalidNodeTypeDefException("circular inheritance detected: " + buf.toString());
            }

            try {
                QName[] sta = getNodeTypeDef(nt).getSupertypes();
                if (sta != null && sta.length > 0) {
                    // check recursively
                    inheritanceChain.push(nt);
                    checkForCircularInheritance(sta, inheritanceChain);
                    inheritanceChain.pop();
                }
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = "unknown supertype: " + nt;
                log.error(msg, nsnte);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }
    }

    void checkForCircularNodeAutoCreation(EffectiveNodeType childNodeENT, Stack definingParentNTs)
            throws InvalidNodeTypeDefException {
        // check for circularity through default node types of auto-created child nodes
        // (node type 'a' defines auto-created child node with default node type 'a')
        QName[] childNodeNTs = childNodeENT.getAllNodeTypes();
        for (int i = 0; i < childNodeNTs.length; i++) {
            QName nt = childNodeNTs[i];
            int pos = definingParentNTs.lastIndexOf(nt);
            if (pos >= 0) {
                StringBuffer buf = new StringBuffer();
                for (int j = 0; j < definingParentNTs.size(); j++) {
                    if (j == pos) {
                        buf.append("--> ");
                    }
                    buf.append("node type ");
                    buf.append(definingParentNTs.get(j));
                    buf.append(" defines auto-created child node with default ");
                }
                buf.append("--> ");
                buf.append("node type ");
                buf.append(nt);
                throw new InvalidNodeTypeDefException("circular node auto-creation detected: " + buf.toString());
            }
        }

        ChildNodeDef[] nodeDefs = childNodeENT.getAutoCreateNodeDefs();
        for (int i = 0; i < nodeDefs.length; i++) {
            QName dnt = nodeDefs[i].getDefaultPrimaryType();
            QName definingNT = nodeDefs[i].getDeclaringNodeType();
            try {
                if (dnt != null) {
                    // check recursively
                    definingParentNTs.push(definingNT);
                    checkForCircularNodeAutoCreation(getEffectiveNodeType(dnt), definingParentNTs);
                    definingParentNTs.pop();
                }
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = definingNT + " defines invalid default node type for child node " + nodeDefs[i].getName();
                log.error(msg, nsnte);
                throw new InvalidNodeTypeDefException(msg, nsnte);
            }
        }
    }

    /**
     * Validates the <code>NodeTypeDef</code> and returns
     * a registered <code>EffectiveNodeType</code> instance.
     * <p/>
     * The validation includes the following checks:
     * <ul>
     * <li>Supertypes must exist and be registered</li>
     * <li>Inheritance graph must not be circular</li>
     * <li>Aggregation of supertypes must not result in name conflicts,
     * ambiguities, etc.</li>
     * <li>Definitions of auto-created properties must specify a name</li>
     * <li>Default values in property definitions must satisfy value constraints
     * specified in the same property definition</li>
     * <li>Definitions of auto-created child-nodes must specify a name</li>
     * <li>Default node type in child-node definitions must exist and be
     * registered</li>
     * <li>The aggregation of the default node types in child-node definitions
     * must not result in name conflicts, ambiguities, etc.</li>
     * <li>Definitions of auto-created child-nodes must not specify default
     * node types which would lead to infinite child node creation
     * (e.g. node type 'A' defines auto-created child node with default
     * node type 'A' ...)</li>
     * <li>Node types specified as constraints in child-node definitions
     * must exist and be registered</li>
     * <li>The aggregation of the node types specified as constraints in
     * child-node definitions must not result in name conflicts, ambiguities,
     * etc.</li>
     * <li>Default node types in child-node definitions must satisfy
     * node type constraints specified in the same child-node definition</li>
     * </ul>
     *
     * @param ntd the definition of the new node type
     * @return an <code>EffectiveNodeType</code> instance
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    public synchronized EffectiveNodeType registerNodeType(NodeTypeDef ntd)
            throws InvalidNodeTypeDefException, RepositoryException {
        // validate and register new node type definition
        EffectiveNodeType ent = internalRegister(ntd);

        // persist new node type definition
        customNTDefs.add(ntd);
        persistCustomNTDefs();

        // notify listeners
        notifyRegistered(ntd.getName());

        return ent;
    }

    /**
     * Same as <code>{@link #registerNodeType(NodeTypeDef)}</code> except
     * that a collection of <code>NodeTypeDef</code>s is registered instead of
     * just one.
     * <p/>
     * This method can be used to register a set of node types that have
     * dependencies on each other.
     * <p/>
     * Note that in the case an exception is thrown, some node types might have
     * been nevertheless successfully registered.
     *
     * @param ntDefs a collection of <code>NodeTypeDef<code>s
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    public synchronized void registerNodeTypes(Collection ntDefs)
            throws InvalidNodeTypeDefException, RepositoryException {
        // exceptions that might be thrown by internalRegister(Collection)
        RepositoryException re = null;
        InvalidNodeTypeDefException intde = null;

        // store names of currently registered node types before proceeding
        HashSet oldNTNames = new HashSet(registeredNTDefs.keySet());

        try {
            // validate and register new node type definitions
            internalRegister(ntDefs);
        } catch (RepositoryException e) {
            // store exception so it can be re-thrown later on
            re = e;
        } catch (InvalidNodeTypeDefException e) {
            // store exception so it can be re-thrown later on
            intde = e;
        }

        /**
         * build set of names of actually registered new node types
         * (potentially a subset of those specified in ntDefs if an exception
         * had been thrown)
         */
        HashSet newNTNames = new HashSet(registeredNTDefs.keySet());
        newNTNames.removeAll(oldNTNames);

        if (newNTNames.size() > 0) {
            // persist new node type definitions
            for (Iterator iter = newNTNames.iterator(); iter.hasNext();) {
                QName ntName = (QName) iter.next();
                customNTDefs.add((NodeTypeDef) registeredNTDefs.get(ntName));
            }
            persistCustomNTDefs();

            // notify listeners
            for (Iterator iter = newNTNames.iterator(); iter.hasNext();) {
                QName ntName = (QName) iter.next();
                notifyRegistered(ntName);
            }
        }

        // re-throw exception as necessary
        if (re != null) {
            throw re;
        } else if (intde != null) {
            throw intde;
        }
    }

    /**
     * @param name
     * @throws NoSuchNodeTypeException
     * @throws RepositoryException
     */
    public synchronized void unregisterNodeType(QName name)
            throws NoSuchNodeTypeException, RepositoryException {
        if (!registeredNTDefs.containsKey(name)) {
            throw new NoSuchNodeTypeException(name.toString());
        }
        if (builtInNTDefs.contains(name)) {
            throw new RepositoryException(name.toString() + ": can't unregister built-in node type.");
        }

        /**
         * check if there are node types that have dependencies on the given
         * node type
         */
        Set dependentNTs = getDependentNodeTypes(name);
        if (dependentNTs.size() > 0) {
            StringBuffer msg = new StringBuffer();
            msg.append(name + " could not be removed because the following node types are referencing it: ");
            Iterator iterator = dependentNTs.iterator();
            while (iterator.hasNext()) {
                msg.append((QName) iterator.next());
                msg.append(" ");
            }
            throw new RepositoryException(msg.toString());
        }

        /**
         * todo
         * 1. apply deep locks on root nodes in every workspace or alternatively
         *    put repository in 'single-user' mode
         * 2. check if the given node type is currently referenced by nodes
         *    in the repository.
         * 3. remove the node type if it is not currently referenced, otherwise
         *    throw exception
         *
         * the above checks are absolutely necessary in order to guarantee
         * integrity of repository content.
         *
         * throw exception while this is not implemented properly yet
         */
        boolean isReferenced = true;
        if (isReferenced) {
            throw new RepositoryException("not yet implemented");
        }

        internalUnregister(name);

        // persist removal of node type definition
        customNTDefs.remove(name);
        persistCustomNTDefs();

        // notify listeners
        notifyUnregistered(name);
    }

    /**
     * @param ntd
     * @return
     * @throws NoSuchNodeTypeException
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    public synchronized EffectiveNodeType reregisterNodeType(NodeTypeDef ntd)
            throws NoSuchNodeTypeException, InvalidNodeTypeDefException,
            RepositoryException {
        QName name = ntd.getName();
        if (!registeredNTDefs.containsKey(name)) {
            throw new NoSuchNodeTypeException(name.toString());
        }
        if (builtInNTDefs.contains(name)) {
            throw new RepositoryException(name.toString() + ": can't reregister built-in node type.");
        }

        /**
         * validate new node type definition
         */
        validateNodeTypeDef(ntd);

        /**
         * build diff of current and new definition and determine type of change
         */
        NodeTypeDef ntdOld = (NodeTypeDef) registeredNTDefs.get(name);
        NodeTypeDefDiff diff = NodeTypeDefDiff.create(ntdOld, ntd);
        if (!diff.isModified()) {
            // the definition has not been modified, there's nothing to do here...
            return getEffectiveNodeType(name);
        }
        if (diff.isTrivial()) {
            /**
             * the change is trivial and has no effect on current content
             * (e.g. that would be the case when non-mandatory properties had
             * been added);
             * re-register node type definition and update caches &
             * notify listeners on re-registration
             */
            internalUnregister(name);
            // remove old node type definition from store
            customNTDefs.remove(name);

            EffectiveNodeType entNew = internalRegister(ntd);

            // add new node type definition to store
            customNTDefs.add(ntd);
            // persist node type definitions
            persistCustomNTDefs();

            // notify listeners
            notifyReRegistered(name);
            return entNew;
        }

        /**
         * collect names of node types that have dependencies on the given
         * node type
         */
        Set dependentNTs = getDependentNodeTypes(name);

        /**
         * non-trivial change of node type definition
         * todo
         * 1. apply deep locks on root nodes in every workspace or alternatively
         *    put repository in 'exclusive' or 'single-user' mode
         * 2. check if the given node type (or any node type that has
         *    dependencies on this node type) is currently referenced by nodes
         *    in the repository.
         * 3. check if applying changes to affected nodes would violate
         *    existing node type constraints
         * 4. apply and persist changes to affected nodes (e.g. update
         *    definition id's, etc.)
         *
         * the above checks/actions are absolutely necessary in order to
         * guarantee integrity of repository content.
         *
         * throw exception while this is not implemented properly yet
         */
        boolean conflictingContent = true;
        if (conflictingContent) {
            throw new RepositoryException("not yet implemented");
        }

        // unregister old node type definition
        internalUnregister(name);
        // register new definition
        EffectiveNodeType entNew = internalRegister(ntd);

        // persist modified node type definitions
        customNTDefs.remove(name);
        customNTDefs.add(ntd);
        persistCustomNTDefs();

        // notify listeners
        notifyReRegistered(name);
        return entNew;
    }

    /**
     * Returns the names of those registered node types that have
     * dependencies on the given node type.
     *
     * @param nodeTypeName
     * @return a set of node type <code>QName</code>s
     * @throws NoSuchNodeTypeException
     */
    public synchronized Set getDependentNodeTypes(QName nodeTypeName)
            throws NoSuchNodeTypeException {
        if (!registeredNTDefs.containsKey(nodeTypeName)) {
            throw new NoSuchNodeTypeException(nodeTypeName.toString());
        }

        /**
         * collect names of those node types that have dependencies on the given
         * node type
         */
        HashSet names = new HashSet();
        Iterator iter = registeredNTDefs.values().iterator();
        while (iter.hasNext()) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();
            if (ntd.getDependencies().contains(nodeTypeName)) {
                names.add(ntd.getName());
            }
        }
        return names;
    }

    /**
     * @param nodeTypeName
     * @return
     * @throws NoSuchNodeTypeException
     */
    public synchronized NodeTypeDef getNodeTypeDef(QName nodeTypeName) throws NoSuchNodeTypeException {
        if (!registeredNTDefs.containsKey(nodeTypeName)) {
            throw new NoSuchNodeTypeException(nodeTypeName.toString());
        }
        NodeTypeDef def = (NodeTypeDef) registeredNTDefs.get(nodeTypeName);
        // return clone to make sure nobody messes around with the 'real' definition
        try {
            return (NodeTypeDef) def.clone();
        } catch (CloneNotSupportedException e) {
            // should never get here
            log.fatal("internal error", e);
            throw new InternalError(e.getMessage());
        }
    }

    /**
     * @param nodeTypeName
     * @return
     */
    public synchronized boolean isRegistered(QName nodeTypeName) {
        return registeredNTDefs.containsKey(nodeTypeName);
    }

    /**
     * @param id
     * @return
     */
    public ChildNodeDef getNodeDef(NodeDefId id) {
        ChildNodeDef def = (ChildNodeDef) nodeDefs.get(id);
        if (def == null) {
            return null;
        }
        // return clone to make sure nobody messes around with the 'real' definition
        try {
            return (ChildNodeDef) def.clone();
        } catch (CloneNotSupportedException e) {
            // should never get here
            log.fatal("internal error", e);
            throw new InternalError(e.getMessage());
        }
    }

    /**
     * @param id
     * @return
     */
    public PropDef getPropDef(PropDefId id) {
        PropDef def = (PropDef) propDefs.get(id);
        if (def == null) {
            return null;
        }
        // return clone to make sure nobody messes around with the 'real' definition
        try {
            return (PropDef) def.clone();
        } catch (CloneNotSupportedException e) {
            // should never get here
            log.fatal("internal error", e);
            throw new InternalError(e.getMessage());
        }
    }

    //----------------------------------------------------------< diagnostics >
    /**
     * Dumps the state of this <code>NodeTypeManager</code> instance.
     *
     * @param ps
     * @throws RepositoryException
     */
    void dump(PrintStream ps) throws RepositoryException {
        ps.println("NodeTypeManager (" + this + ")");
        ps.println();
        ps.println("Registered NodeTypes:");
        ps.println();
        Iterator iter = registeredNTDefs.values().iterator();
        while (iter.hasNext()) {
            NodeTypeDef ntd = (NodeTypeDef) iter.next();
            ps.println(ntd.getName());
            QName[] supertypes = ntd.getSupertypes();
            ps.println("\tSupertypes");
            for (int i = 0; i < supertypes.length; i++) {
                ps.println("\t\t" + supertypes[i]);
            }
            ps.println("\tMixin\t" + ntd.isMixin());
            ps.println("\tOrderableChildNodes\t" + ntd.hasOrderableChildNodes());
            ps.println("\tPrimaryItemName\t" + (ntd.getPrimaryItemName() == null ? "<null>" : ntd.getPrimaryItemName().toString()));
            PropDef[] pd = ntd.getPropertyDefs();
            for (int i = 0; i < pd.length; i++) {
                ps.print("\tPropertyDef");
                ps.println(" (declared in " + pd[i].getDeclaringNodeType() + ") id=" + new PropDefId(pd[i]));
                ps.println("\t\tName\t\t" + (pd[i].definesResidual() ? "*" : pd[i].getName().toString()));
                String type = pd[i].getRequiredType() == 0 ? "null" : PropertyType.nameFromValue(pd[i].getRequiredType());
                ps.println("\t\tRequiredType\t" + type);
                ValueConstraint[] vca = pd[i].getValueConstraints();
                StringBuffer constraints = new StringBuffer();
                if (vca == null) {
                    constraints.append("<null>");
                } else {
                    for (int n = 0; n < vca.length; n++) {
                        if (constraints.length() > 0) {
                            constraints.append(", ");
                        }
                        constraints.append(vca[n].getDefinition());
                    }
                }
                ps.println("\t\tValueConstraints\t" + constraints.toString());
                InternalValue[] defVals = pd[i].getDefaultValues();
                StringBuffer defaultValues = new StringBuffer();
                if (defVals == null) {
                    defaultValues.append("<null>");
                } else {
                    for (int n = 0; n < defVals.length; n++) {
                        if (defaultValues.length() > 0) {
                            defaultValues.append(", ");
                        }
                        defaultValues.append(defVals[n].toString());
                    }
                }
                ps.println("\t\tDefaultValue\t" + defaultValues.toString());
                ps.println("\t\tAutoCreate\t" + pd[i].isAutoCreate());
                ps.println("\t\tMandatory\t" + pd[i].isMandatory());
                ps.println("\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(pd[i].getOnParentVersion()));
                ps.println("\t\tProtected\t" + pd[i].isProtected());
                ps.println("\t\tMultiple\t" + pd[i].isMultiple());
            }
            ChildNodeDef[] nd = ntd.getChildNodeDefs();
            for (int i = 0; i < nd.length; i++) {
                ps.print("\tNodeDef");
                ps.println(" (declared in " + nd[i].getDeclaringNodeType() + ") id=" + new NodeDefId(nd[i]));
                ps.println("\t\tName\t\t" + (nd[i].definesResidual() ? "*" : nd[i].getName().toString()));
                QName[] reqPrimaryTypes = nd[i].getRequiredPrimaryTypes();
                if (reqPrimaryTypes != null && reqPrimaryTypes.length > 0) {
                    for (int n = 0; n < reqPrimaryTypes.length; n++) {
                        ps.print("\t\tRequiredPrimaryType\t" + reqPrimaryTypes[n]);
                    }
                }
                QName defPrimaryType = nd[i].getDefaultPrimaryType();
                if (defPrimaryType != null) {
                    ps.print("\n\t\tDefaultPrimaryType\t" + defPrimaryType);
                }
                ps.println("\n\t\tAutoCreate\t" + nd[i].isAutoCreate());
                ps.println("\t\tMandatory\t" + nd[i].isMandatory());
                ps.println("\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(nd[i].getOnParentVersion()));
                ps.println("\t\tProtected\t" + nd[i].isProtected());
                ps.println("\t\tAllowSameNameSibs\t" + nd[i].allowSameNameSibs());
            }
        }
        ps.println();

        entCache.dump(ps);
    }

    //--------------------------------------------------------< inner classes >
    /**
     * A <code>WeightedKey</code> uniquely identifies
     * a combination (i.e. an aggregation) of one or more node types.
     * The weight is an indicator for the cost involved in building such an
     * aggregate (an aggregation multiple complex node types with deep
     * inheritance trees is more costly to build/validate than an agreggation
     * of two very simple node types with just one property definition each).
     * <p/>
     * A very simple (and not very accurate) approximation of the weight would
     * be the number of explicitly aggregated node types (ignoring inheritance
     * and complexity of each involved node type). A better approximation would
     * be the number of <b>all</b>, explicitly and implicitly (note that
     * inheritance is also an aggregation) aggregated node types.
     * <p/>
     * The more accurate the weight definition, the more efficient is the
     * the building of new aggregates.
     * <p/>
     * It is important to note that the weight is not part of the key value,
     * i.e. it is not considered by the <code>hashCode()</code> and
     * <code>equals(Object)</code> methods. It does however affect the order
     * of <code>WeightedKey</code> instances. See
     * <code>{@link #compareTo(Object)}</code> for more information.
     * <p/>
     * Let's assume we have an aggregation of node types named "b", "a" and "c".
     * Its key would be "[a, b, c]" and the weight 3 (using the simple
     * approximation).
     */
    static class WeightedKey implements Comparable {
        /**
         * set of node type names, sorted in ascending order
         */
        private final TreeSet set;
        private final int weight;

        /**
         * @param ntNames
         */
        WeightedKey(QName[] ntNames) {
            this(ntNames, ntNames.length);
        }

        /**
         * @param ntNames
         * @param weight
         */
        WeightedKey(QName[] ntNames, int weight) {
            this.weight = weight;

            set = new TreeSet();
            for (int i = 0; i < ntNames.length; i++) {
                // add name to this sorted set
                set.add(ntNames[i]);
            }
        }

        /**
         * @param ntNames
         */
        WeightedKey(Collection ntNames) {
            this(ntNames, ntNames.size());
        }

        /**
         * @param ntNames
         * @param weight
         */
        WeightedKey(Collection ntNames, int weight) {
            this.weight = weight;
            set = new TreeSet(ntNames);
        }

        /**
         * The key is the string representation of this sorted set
         * (e.g. the key for a set containing entries "c", "b" and "a" would
         * be "[a, b, c]").
         *
         * @return string representation of this sorted set
         * @see AbstractCollection#toString
         */
        String getKey() {
            return set.toString();
        }

        /**
         * @return
         */
        int getWeight() {
            return weight;
        }

        int size() {
            return set.size();
        }

        Iterator iterator() {
            return Collections.unmodifiableSortedSet(set).iterator();
        }

        Set getSet() {
            return Collections.unmodifiableSortedSet(set);
        }

        QName[] toArray() {
            return (QName[]) set.toArray(new QName[set.size()]);
        }

        boolean contains(WeightedKey otherKey) {
            return set.containsAll(otherKey.getSet());
        }

        WeightedKey subtract(WeightedKey otherKey) {
            Set tmp = (Set) set.clone();
            tmp.removeAll(otherKey.getSet());
            return new WeightedKey(tmp);

        }

        /**
         * The resulting sort-order is: 1. descending weight, 2. ascending key
         * (i.e. string representation of this sorted set).
         *
         * @param o
         * @return
         */
        public int compareTo(Object o) {
            WeightedKey other = (WeightedKey) o;
            if (getWeight() > other.getWeight()) {
                return -1;
            } else if (getWeight() < other.getWeight()) {
                return 1;
            }
            return getKey().compareTo(other.getKey());
        }

        public int hashCode() {
            int h = 17;
            // ignore weight
            Iterator i = set.iterator();
            while (i.hasNext()) {
                Object obj = i.next();
                h = 37 * h + (obj != null ? obj.hashCode() : 0);
            }
            return h;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof WeightedKey) {
                WeightedKey other = (WeightedKey) obj;
                // ignore weight
                return set.equals(other.set);
            }
            return false;
        }

        public String toString() {
            return set.toString() + " (" + weight + ")";
        }
    }

    /**
     * <code>EfectiveNodeTypeCache</code> ...
     */
    private class EffectiveNodeTypeCache {
        // ordered set of keys
        final TreeSet sortedKeys;
        // cache of pre-build aggregations of node types
        final HashMap aggregates;

        EffectiveNodeTypeCache() {
            sortedKeys = new TreeSet();
            aggregates = new HashMap();
        }

        void put(EffectiveNodeType ent) {
            // we define the weight as the total number of included node types
            // (through aggregation and inheritance)
            int weight = ent.getAllNodeTypes().length;
            // the effective node type is identified by the list of merged
            // (i.e. aggregated) node types
            WeightedKey k = new WeightedKey(ent.getMergedNodeTypes(), weight);
            aggregates.put(k, ent);
            sortedKeys.add(k);
        }

        boolean contains(QName[] ntNames) {
            return aggregates.containsKey(new WeightedKey(ntNames));
        }

        boolean contains(WeightedKey key) {
            return aggregates.containsKey(key);
        }

        EffectiveNodeType get(QName[] ntNames) {
            return (EffectiveNodeType) aggregates.get(new WeightedKey(ntNames));
        }

        EffectiveNodeType get(WeightedKey key) {
            return (EffectiveNodeType) aggregates.get(key);
        }

        EffectiveNodeType remove(QName[] ntNames) {
            return remove(new WeightedKey(ntNames));
        }

        EffectiveNodeType remove(WeightedKey key) {
            EffectiveNodeType removed = (EffectiveNodeType) aggregates.remove(key);
            if (removed != null) {
                // remove index entry

                // FIXME: can't simply call TreeSet.remove(key) because the entry
                // in sortedKeys might have a different weight and would thus
                // not be found
                Iterator iter = sortedKeys.iterator();
                while (iter.hasNext()) {
                    WeightedKey k = (WeightedKey) iter.next();
                    // WeightedKey.equals(Object) ignores the weight
                    if (key.equals(k)) {
                        sortedKeys.remove(k);
                        break;
                    }
                }
            }
            return removed;
        }

        /**
         * Returns an iterator over the keys. The order of the returned keys is:
         * <ul>
         * <li>1. descending weight</li>
         * <li>2. ascending key (i.e. unique identifier of aggregate)</li>
         * </ul>
         *
         * @see NodeTypeRegistry.WeightedKey#compareTo
         */
        Iterator keys() {
            return sortedKeys.iterator();
        }

        //------------------------------------------------------< diagnostics >
        /**
         * Dumps the state of this <code>EffectiveNodeTypeCache</code> instance.
         *
         * @param ps
         * @throws RepositoryException
         */
        void dump(PrintStream ps) throws RepositoryException {
            ps.println("EffectiveNodeTypeCache (" + this + ")");
            ps.println();
            ps.println("EffectiveNodeTypes in cache:");
            ps.println();
            Iterator iter = sortedKeys.iterator();
            while (iter.hasNext()) {
                WeightedKey k = (WeightedKey) iter.next();
                //EffectiveNodeType ent = (EffectiveNodeType) aggregates.get(k);
                ps.println(k);
            }
        }
    }
}
