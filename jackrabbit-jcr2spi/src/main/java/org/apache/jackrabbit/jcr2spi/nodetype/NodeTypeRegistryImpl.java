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
package org.apache.jackrabbit.jcr2spi.nodetype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.version.OnParentVersionAction;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>NodeTypeRegistry</code> ...
 */
public class NodeTypeRegistryImpl implements NodeTypeRegistry, EffectiveNodeTypeProvider {

    private static Logger log = LoggerFactory.getLogger(NodeTypeRegistryImpl.class);

    // cache of pre-built aggregations of node types
    private final EffectiveNodeTypeCache entCache;

    // map of node type names and node type definitions
    private final NodeTypeDefinitionMap registeredNTDefs;

    /**
     * Object used to persist new nodetypes and modified nodetype definitions.
     */
    private final NodeTypeStorage storage;

    /**
     * Class used to validate NodeType definitions
     */
    private final DefinitionValidator validator;

    /**
     * Listeners (soft references)
     */
    private final Map<NodeTypeRegistryListener, NodeTypeRegistryListener> listeners = Collections
            .synchronizedMap(new ReferenceMap<>(ReferenceStrength.WEAK, ReferenceStrength.WEAK));

    /**
     * Create a new <code>NodeTypeRegistry</code>
     *
     * @param storage
     * @param nsRegistry
     * @return <code>NodeTypeRegistry</code> object
     */
    public static NodeTypeRegistryImpl create(NodeTypeStorage storage, NamespaceRegistry nsRegistry) {
        NodeTypeRegistryImpl ntRegistry = new NodeTypeRegistryImpl(storage, nsRegistry);
        return ntRegistry;
    }

    /**
     * Clears all caches.
     */
    public synchronized void dispose() {
        entCache.clear();
        registeredNTDefs.clear();
        listeners.clear();
    }

    /**
     * Private constructor
     *
     * @param storage
     * @param nsRegistry
     */
    private NodeTypeRegistryImpl(NodeTypeStorage storage, NamespaceRegistry nsRegistry) {
        this.storage = storage;
        this.validator = new DefinitionValidator(this, nsRegistry);

        entCache = new BitsetENTCacheImpl();
        registeredNTDefs = new NodeTypeDefinitionMap();
    }

    //---------------------------------------------------< NodeTypeRegistry >---
    /**
     * @see NodeTypeRegistry#addListener(NodeTypeRegistryListener)
     */
    public void addListener(NodeTypeRegistryListener listener) {
        if (!listeners.containsKey(listener)) {
            listeners.put(listener, listener);
        }
    }

    /**
     * @see NodeTypeRegistry#removeListener(NodeTypeRegistryListener)
     */
    public void removeListener(NodeTypeRegistryListener listener) {
        listeners.remove(listener);
    }

    /**
     * @see NodeTypeRegistry#getRegisteredNodeTypes()
     */
    public Name[] getRegisteredNodeTypes() throws RepositoryException {
        Set<Name> qNames = registeredNTDefs.keySet();
        return qNames.toArray(new Name[registeredNTDefs.size()]);
    }


    /**
     * @see NodeTypeRegistry#isRegistered(Name)
     */
    public boolean isRegistered(Name nodeTypeName) {
        return registeredNTDefs.containsKey(nodeTypeName);
    }

    /**
     * @see NodeTypeRegistry#registerNodeTypes(Collection, boolean)
     */
    public synchronized void registerNodeTypes(Collection<QNodeTypeDefinition> ntDefs, boolean allowUpdate) throws NodeTypeExistsException, InvalidNodeTypeDefinitionException, RepositoryException {
        List<Name> added = new ArrayList<Name>();
        List<Name> modified = new ArrayList<Name>();
        for (QNodeTypeDefinition def : ntDefs) {
            Name name = def.getName();
            if (isRegistered(name)) {
                modified.add(name);
            } else {
                added.add(name);
            }
        }

        // validate new nodetype definitions
        Map<QNodeTypeDefinition, EffectiveNodeType> defMap = validator.validateNodeTypeDefs(ntDefs, registeredNTDefs);
        storage.registerNodeTypes(ntDefs.toArray(new QNodeTypeDefinition[ntDefs.size()]), allowUpdate);

        // update internal cache:
        // unregister modified node type definition
        internalUnregister(modified);
        // register all new and modified definition
        internalRegister(defMap);

        // notify listeners
        for (Name ntName : added) {
            notifyRegistered(ntName);
        }
        for (Name ntName : modified) {
            notifyReRegistered(ntName);
        }
    }

    /**
     * @see NodeTypeRegistry#unregisterNodeTypes(Collection)
     */
    public synchronized void unregisterNodeTypes(Collection<Name> nodeTypeNames)
            throws NoSuchNodeTypeException, RepositoryException {
        // do some preliminary checks
        for (Name ntName : nodeTypeNames) {
            // Best effort check for node types other than those to be
            // unregistered that depend on the given node types
            Set<Name> dependents = registeredNTDefs.getDependentNodeTypes(ntName);
            dependents.removeAll(nodeTypeNames);
            if (dependents.size() > 0) {
                StringBuffer msg = new StringBuffer();
                msg.append(ntName).append(" can not be removed because the following node types depend on it: ");
                for (Name name : dependents) {
                    msg.append(name);
                    msg.append(" ");
                }
                throw new RepositoryException(msg.toString());
            }
        }

        // persist removal of node type definitions
        // NOTE: conflict with existing content not asserted on client
        storage.unregisterNodeTypes(nodeTypeNames.toArray(new Name[nodeTypeNames.size()]));


        // all preconditions are met, node types can now safely be unregistered
        internalUnregister(nodeTypeNames);

        // notify listeners
        for (Name ntName : nodeTypeNames) {
            notifyUnregistered(ntName);
        }
    }

    /**
     * @see NodeTypeRegistry#getNodeTypeDefinition(Name)
     */
    public QNodeTypeDefinition getNodeTypeDefinition(Name nodeTypeName)
        throws NoSuchNodeTypeException {
        QNodeTypeDefinition def = registeredNTDefs.get(nodeTypeName);
        if (def == null) {
            throw new NoSuchNodeTypeException("Nodetype " + nodeTypeName + " doesn't exist");
        }
        return def;
    }
    //------------------------------------------< EffectiveNodeTypeProvider >---
    /**
     * @see EffectiveNodeTypeProvider#getEffectiveNodeType(Name)
     */
    public synchronized EffectiveNodeType getEffectiveNodeType(Name ntName)
            throws NoSuchNodeTypeException {
        return getEffectiveNodeType(ntName, entCache, registeredNTDefs);
    }

    /**
     * @see EffectiveNodeTypeProvider#getEffectiveNodeType(Name[])
     */
    public synchronized EffectiveNodeType getEffectiveNodeType(Name[] ntNames)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        return getEffectiveNodeType(ntNames, entCache, registeredNTDefs);
    }

    /**
     * @see EffectiveNodeTypeProvider#getEffectiveNodeType(Name[], Map)
     */
    public EffectiveNodeType getEffectiveNodeType(Name[] ntNames, Map<Name, QNodeTypeDefinition> ntdMap)
        throws ConstraintViolationException, NoSuchNodeTypeException {
        return getEffectiveNodeType(ntNames, entCache, ntdMap);
    }

    /**
     * @see EffectiveNodeTypeProvider#getEffectiveNodeType(QNodeTypeDefinition, Map)
     */
    public EffectiveNodeType getEffectiveNodeType(QNodeTypeDefinition ntd, Map<Name, QNodeTypeDefinition> ntdMap)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        TreeSet<Name> mergedNodeTypes = new TreeSet<Name>();
        TreeSet<Name> inheritedNodeTypes = new TreeSet<Name>();
        TreeSet<Name> allNodeTypes = new TreeSet<Name>();
        Map<Name, List<QItemDefinition>> namedItemDefs = new HashMap<Name, List<QItemDefinition>>();
        List<QItemDefinition> unnamedItemDefs = new ArrayList<QItemDefinition>();
        Set<Name> supportedMixins = null;

        Name ntName = ntd.getName();
        // prepare new instance
        mergedNodeTypes.add(ntName);
        allNodeTypes.add(ntName);

        Name[] smixins = ntd.getSupportedMixinTypes();

        if (smixins != null) {
            supportedMixins = new HashSet<Name>();
            supportedMixins.addAll(Arrays.asList(smixins));
        }

        // map of all item definitions (maps id to definition)
        // used to effectively detect ambiguous child definitions where
        // ambiguity is defined in terms of definition identity
        Set<QItemDefinition> itemDefIds = new HashSet<QItemDefinition>();

        for (QNodeDefinition nd : ntd.getChildNodeDefs()) {
            // check if child node definition would be ambiguous within
            // this node type definition
            if (itemDefIds.contains(nd)) {
                // conflict
                String msg;
                if (nd.definesResidual()) {
                    msg = ntName + " contains ambiguous residual child node definitions";
                } else {
                    msg = ntName + " contains ambiguous definitions for child node named "
                            + nd.getName();
                }
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            } else {
                itemDefIds.add(nd);
            }
            if (nd.definesResidual()) {
                // residual node definition
                unnamedItemDefs.add(nd);
            } else {
                // named node definition
                Name name = nd.getName();
                List<QItemDefinition> defs = namedItemDefs.get(name);
                if (defs == null) {
                    defs = new ArrayList<QItemDefinition>();
                    namedItemDefs.put(name, defs);
                }
                if (defs.size() > 0) {
                    /**
                     * there already exists at least one definition with that
                     * name; make sure none of them is auto-create
                     */
                    for (QItemDefinition qDef : defs) {
                        if (nd.isAutoCreated() || qDef.isAutoCreated()) {
                            // conflict
                            String msg = "There are more than one 'auto-create' item definitions for '"
                                    + name + "' in node type '" + ntName + "'";
                            log.debug(msg);
                            throw new ConstraintViolationException(msg);
                        }
                    }
                }
                defs.add(nd);
            }
        }
        for (QPropertyDefinition pd : ntd.getPropertyDefs()) {
            // check if property definition would be ambiguous within
            // this node type definition
            if (itemDefIds.contains(pd)) {
                // conflict
                String msg;
                if (pd.definesResidual()) {
                    msg = ntName + " contains ambiguous residual property definitions";
                } else {
                    msg = ntName + " contains ambiguous definitions for property named "
                            + pd.getName();
                }
                log.debug(msg);
                throw new ConstraintViolationException(msg);
            } else {
                itemDefIds.add(pd);
            }
            if (pd.definesResidual()) {
                // residual property definition
                unnamedItemDefs.add(pd);
            } else {
                // named property definition
                Name name = pd.getName();
                List<QItemDefinition> defs = namedItemDefs.get(name);
                if (defs == null) {
                    defs = new ArrayList<QItemDefinition>();
                    namedItemDefs.put(name, defs);
                }
                if (defs.size() > 0) {
                    /**
                     * there already exists at least one definition with that
                     * name; make sure none of them is auto-create
                     */
                    for (QItemDefinition qDef : defs) {
                        if (pd.isAutoCreated() || qDef.isAutoCreated()) {
                            // conflict
                            String msg = "There are more than one 'auto-create' item definitions for '"
                                    + name + "' in node type '" + ntName + "'";
                            log.debug(msg);
                            throw new ConstraintViolationException(msg);
                        }
                    }
                }
                defs.add(pd);
            }
        }

        // create empty effective node type instance
        EffectiveNodeTypeImpl ent = new EffectiveNodeTypeImpl(mergedNodeTypes,
                inheritedNodeTypes, allNodeTypes, namedItemDefs,
                unnamedItemDefs, supportedMixins);

        // resolve supertypes recursively
        Name[] supertypes = ntd.getSupertypes();
        if (supertypes.length > 0) {
            EffectiveNodeTypeImpl effSuperType = (EffectiveNodeTypeImpl) getEffectiveNodeType(supertypes, ntdMap);
            ent.internalMerge(effSuperType, true);
        }
        return ent;
    }

    /**
     *
     * @param ntName
     * @param entCache
     * @param ntdCache
     * @return
     * @throws NoSuchNodeTypeException
     */
    private EffectiveNodeType getEffectiveNodeType(Name ntName,
                                                   EffectiveNodeTypeCache entCache,
                                                   Map<Name, QNodeTypeDefinition> ntdCache)
        throws NoSuchNodeTypeException {
        // 1. check if effective node type has already been built
        EffectiveNodeTypeCache.Key key = entCache.getKey(new Name[]{ntName});
        EffectiveNodeType ent = entCache.get(key);
        if (ent != null) {
            return ent;
        }

        // 2. make sure we've got the definition of the specified node type
        QNodeTypeDefinition ntd = ntdCache.get(ntName);
        if (ntd == null) {
            throw new NoSuchNodeTypeException(ntName.toString());
        }

        // 3. build effective node type
        synchronized (entCache) {
            try {
                ent = getEffectiveNodeType(ntd, ntdCache);
                // store new effective node type
                entCache.put(ent);
                return ent;
            } catch (ConstraintViolationException e) {
                // should never get here as all known node types should be valid!
                String msg = "Internal error: encountered invalid registered node type " + ntName;
                log.debug(msg);
                throw new NoSuchNodeTypeException(msg, e);
            }
        }
    }

    /**
     * @param ntNames
     * @param entCache
     * @param ntdCache
     * @return
     * @throws ConstraintViolationException
     * @throws NoSuchNodeTypeException
     */
    private EffectiveNodeType getEffectiveNodeType(Name[] ntNames,
                                                   EffectiveNodeTypeCache entCache,
                                                   Map<Name, QNodeTypeDefinition> ntdCache)
        throws ConstraintViolationException, NoSuchNodeTypeException {

        EffectiveNodeTypeCache.Key key = entCache.getKey(ntNames);
        // 1. check if aggregate has already been built
        if (entCache.contains(key)) {
            return entCache.get(key);
        }

        // 2. make sure we've got the definitions of the specified node types
        for (Name ntName : ntNames) {
            if (!ntdCache.containsKey(ntName)) {
                throw new NoSuchNodeTypeException(ntName.toString());
            }
        }

        // 3. build aggregate
        EffectiveNodeTypeCache.Key requested = key;
        EffectiveNodeTypeImpl result = null;
        synchronized (entCache) {
            // build list of 'best' existing sub-aggregates
            while (key.getNames().length > 0) {
                // find the (sub) key that matches the current key the best
                EffectiveNodeTypeCache.Key subKey = entCache.findBest(key);
                if (subKey != null) {
                    EffectiveNodeTypeImpl ent = (EffectiveNodeTypeImpl) entCache.get(subKey);
                    if (result == null) {
                        result = ent;
                    } else {
                        result = result.merge(ent);
                        // store intermediate result
                        entCache.put(result);
                    }
                    // subtract the result from the temporary key
                    key = key.subtract(subKey);
                } else {
                    /**
                     * no matching sub-aggregates found:
                     * build aggregate of remaining node types through iteration
                     */
                    for (Name remainder : key.getNames()) {
                        QNodeTypeDefinition ntd = ntdCache.get(remainder);
                        EffectiveNodeType ent = getEffectiveNodeType(ntd, ntdCache);
                        // store new effective node type
                        entCache.put(ent);
                        if (result == null) {
                            result = (EffectiveNodeTypeImpl) ent;
                        } else {
                            result = result.merge((EffectiveNodeTypeImpl) ent);
                            // store intermediate result (sub-aggregate)
                            entCache.put(result);
                        }
                    }
                    break;
                }
            }
        }
        // also put the requested key, since the merge could have removed some
        // the redundant nodetypes
        if (!entCache.contains(requested)) {
            entCache.put(requested, result);
        }
        // we're done
        return result;
    }

    //------------------------------------------------------------< private >---
    /**
     * Notify the listeners that a node type <code>ntName</code> has been registered.
     */
    private void notifyRegistered(Name ntName) {
        for (NodeTypeRegistryListener ntrl : copyListeners()) {
            if (ntrl != null) {
                ntrl.nodeTypeRegistered(ntName);
            }
        }
    }

    /**
     * Notify the listeners that a node type <code>ntName</code> has been re-registered.
     */
    private void notifyReRegistered(Name ntName) {
        for (NodeTypeRegistryListener ntrl : copyListeners()) {
            if (ntrl != null) {
                ntrl.nodeTypeReRegistered(ntName);
            }
        }
    }

    /**
     * Notify the listeners that a node type <code>ntName</code> has been unregistered.
     */
    private void notifyUnregistered(Name ntName) {
        for (NodeTypeRegistryListener ntrl : copyListeners()) {
            if (ntrl != null) {
                ntrl.nodeTypeUnregistered(ntName);
            }
        }
    }

    private NodeTypeRegistryListener[] copyListeners() {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] lstnrs = new NodeTypeRegistryListener[listeners.size()];
        int cnt = 0;
        for (NodeTypeRegistryListener ntrl : listeners.values()) {
            lstnrs[cnt++] = ntrl;
        }
        return lstnrs;
    }

    private void internalRegister(Map<QNodeTypeDefinition, EffectiveNodeType> defMap) {
        for (Map.Entry<QNodeTypeDefinition, EffectiveNodeType> entry : defMap.entrySet()) {
            QNodeTypeDefinition ntd = entry.getKey();
            internalRegister(ntd, entry.getValue());
        }
    }

    private void internalRegister(QNodeTypeDefinition ntd, EffectiveNodeType ent) {
        // store new effective node type instance if present. otherwise it
        // will be created on demand.
        if (ent != null) {
            entCache.put(ent);
        } else {
            log.debug("Effective node type for " + ntd + " not yet built.");
        }
        // register nt-definition
        registeredNTDefs.put(ntd.getName(), ntd);
    }

    private void internalUnregister(Name name) {
        registeredNTDefs.remove(name);
        entCache.invalidate(name);
    }

    private void internalUnregister(Collection<Name> ntNames) {
        for (Name name : ntNames) {
            internalUnregister(name);
        }
    }

    //-------------------------------------------------------------< Object >---

    /**
     * Returns the the state of this instance in a human readable format.
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NodeTypeRegistry (").append(this).append(")\n");
        builder.append("Known NodeTypes:\n");
        builder.append(registeredNTDefs);
        builder.append("\n");
        builder.append(entCache);
        return builder.toString();
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Inner class representing the map of <code>QNodeTypeDefinition</code>s
     * that have been loaded yet.
     */
    private class NodeTypeDefinitionMap implements Map<Name, QNodeTypeDefinition> {

        // map of node type names and node type definitions
        private Map<Name, QNodeTypeDefinition> nodetypeDefinitions =
            new HashMap<Name, QNodeTypeDefinition>();

        private Collection<QNodeTypeDefinition> getValues() {
            return nodetypeDefinitions.values();
        }

        private Set<Name> getKeySet() {
            return nodetypeDefinitions.keySet();
        }

        /**
         * Returns the names of those registered node types that have
         * dependencies on the given node type.
         * <p>
         * Note, that the returned Set may not be complete with respect
         * to all node types registered within the repository. Instead it
         * will only contain those node type definitions that are known so far.
         *
         * @param nodeTypeName node type name
         * @return a set of node type <code>Name</code>s
         * @throws NoSuchNodeTypeException
         */
        private Set<Name> getDependentNodeTypes(Name nodeTypeName) throws NoSuchNodeTypeException {
            if (!nodetypeDefinitions.containsKey(nodeTypeName)) {
                throw new NoSuchNodeTypeException(nodeTypeName.toString());
            }
            // get names of those node types that have dependencies on the
            // node type with the given nodeTypeName.
            HashSet<Name> names = new HashSet<Name>();
            for (QNodeTypeDefinition ntd : getValues()) {
                if (ntd.getDependencies().contains(nodeTypeName)) {
                    names.add(ntd.getName());
                }
            }
            return names;
        }

        private void updateInternalMap(Iterator<QNodeTypeDefinition> definitions) {
            // since definition were retrieved from the storage, validation
            // can be omitted -> register without building effective-nodetype.
            // TODO: check if correct
            while (definitions.hasNext()) {
                internalRegister(definitions.next(), null);
            }
        }

        //------------------------------------------------------------< Map >---
        public int size() {
            return nodetypeDefinitions.size();
        }

        public void clear() {
            nodetypeDefinitions.clear();
        }

        public boolean isEmpty() {
            return nodetypeDefinitions.isEmpty();
        }

        public boolean containsKey(Object key) {
            if (!(key instanceof Name)) {
                return false;
            }
            return get(key) != null;
        }

        public boolean containsValue(Object value) {
            if (!(value instanceof QNodeTypeDefinition)) {
                return false;
            }
            return get(((QNodeTypeDefinition)value).getName()) != null;
        }

        public Set<Name> keySet() {
            // to be aware of all (recently) registered nodetypes retrieve
            // complete set from the storage again and add missing / replace
            // existing definitions.
            try {
                Iterator<QNodeTypeDefinition> it = storage.getAllDefinitions();
                updateInternalMap(it);
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            return getKeySet();
        }

        public Collection<QNodeTypeDefinition> values() {
            // make sure all node type definitions have been loaded.
            keySet();
            // and retrieve the collection containing all definitions.
            return getValues();
        }

        public QNodeTypeDefinition put(Name key, QNodeTypeDefinition value) {
            return nodetypeDefinitions.put(key, value);
        }

        public void putAll(Map<? extends Name, ? extends QNodeTypeDefinition> t) {
            throw new UnsupportedOperationException("Implementation missing");
        }

        public Set<Map.Entry<Name, QNodeTypeDefinition>> entrySet() {
            // make sure all node type definitions have been loaded.
            keySet();
            return nodetypeDefinitions.entrySet();
        }

        public QNodeTypeDefinition get(Object key) {
            if (!(key instanceof Name)) {
                throw new IllegalArgumentException();
            }
            QNodeTypeDefinition def = nodetypeDefinitions.get(key);
            if (def == null) {
                try {
                    // node type does either not exist or hasn't been loaded yet
                    Iterator<QNodeTypeDefinition> it = storage.getDefinitions(new Name[] {(Name) key});
                    updateInternalMap(it);
                } catch (RepositoryException e) {
                    log.debug(e.getMessage());
                }
            }
            def = nodetypeDefinitions.get(key);
            return def;
        }

        public QNodeTypeDefinition remove(Object key) {
            return nodetypeDefinitions.remove(key);
        }

        //---------------------------------------------------------< Object >---

        /**
         * Returns the the state of this instance in a human readable format.
         */
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (QNodeTypeDefinition ntd : getValues()) {
                builder.append(ntd.getName());
                builder.append("\n\tSupertypes");
                for (Name supertype : ntd.getSupertypes()) {
                    builder.append("\n\t\t").append(supertype);
                }
                builder.append("\n\tMixin\t").append(ntd.isMixin());
                builder.append("\n\tOrderableChildNodes\t").append(ntd.hasOrderableChildNodes());
                builder.append("\n\tPrimaryItemName\t").append(ntd.getPrimaryItemName() == null ? "<null>" : ntd.getPrimaryItemName().toString());
                for (QPropertyDefinition pd : ntd.getPropertyDefs()) {
                    builder.append("\n\tPropertyDefinition");
                    builder.append(" (declared in ").append(pd.getDeclaringNodeType()).append(") ");
                    builder.append("\n\t\tName\t\t").append(pd.definesResidual() ? "*" : pd.getName().toString());
                    String type = "null";
                        if (pd.getRequiredType() != 0) {
                            type = PropertyType.nameFromValue(pd.getRequiredType());
                        }
                    builder.append("\n\t\tRequiredType\t").append(type);
                    builder.append("\n\t\tValueConstraints\t");
                    QValueConstraint[] vca = pd.getValueConstraints();
                    if (vca == null) {
                        builder.append("<null>");
                    } else {
                        for (int n = 0; n < vca.length; n++) {
                            if (n > 0) {
                                builder.append(", ");
                            }
                            builder.append(vca[n].getString());
                        }
                    }
                    QValue[] defVals = pd.getDefaultValues();
                    StringBuffer defaultValues = new StringBuffer();
                    if (defVals == null) {
                        defaultValues.append("<null>");
                    } else {
                        for (QValue defVal : defVals) {
                            if (defaultValues.length() > 0) {
                                defaultValues.append(", ");
                            }
                            try {
                                defaultValues.append(defVal.getString());
                            } catch (RepositoryException e) {
                                defaultValues.append(defVal.toString());
                            }
                        }
                    }
                    builder.append("\n\t\tDefaultValue\t").append(defaultValues.toString());
                    builder.append("\n\t\tAutoCreated\t").append(pd.isAutoCreated());
                    builder.append("\n\t\tMandatory\t").append(pd.isMandatory());
                    builder.append("\n\t\tOnVersion\t").append(OnParentVersionAction.nameFromValue(pd.getOnParentVersion()));
                    builder.append("\n\t\tProtected\t").append(pd.isProtected());
                    builder.append("\n\t\tMultiple\t").append(pd.isMultiple());
                }
                QNodeDefinition[] nd = ntd.getChildNodeDefs();
                for (QNodeDefinition aNd : nd) {
                    builder.append("\n\tNodeDefinition");
                    builder.append(" (declared in ").append(aNd.getDeclaringNodeType()).append(") ");
                    builder.append("\n\t\tName\t\t").append(aNd.definesResidual() ? "*" : aNd.getName().toString());
                    Name[] reqPrimaryTypes = aNd.getRequiredPrimaryTypes();
                    if (reqPrimaryTypes != null && reqPrimaryTypes.length > 0) {
                        for (Name reqPrimaryType : reqPrimaryTypes) {
                            builder.append("\n\t\tRequiredPrimaryType\t").append(reqPrimaryType);
                        }
                    }
                    Name defPrimaryType = aNd.getDefaultPrimaryType();
                    if (defPrimaryType != null) {
                        builder.append("\n\t\tDefaultPrimaryType\t").append(defPrimaryType);
                    }
                    builder.append("\n\t\tAutoCreated\t").append(aNd.isAutoCreated());
                    builder.append("\n\t\tMandatory\t").append(aNd.isMandatory());
                    builder.append("\n\t\tOnVersion\t").append(OnParentVersionAction.nameFromValue(aNd.getOnParentVersion()));
                    builder.append("\n\t\tProtected\t").append(aNd.isProtected());
                    builder.append("\n\t\tAllowsSameNameSiblings\t").append(aNd.allowsSameNameSiblings());
                }
            }
            return builder.toString();
        }

    }

}
