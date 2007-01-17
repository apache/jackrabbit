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

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.jcr2spi.util.Dumpable;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.OnParentVersionAction;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

/**
 * A <code>NodeTypeRegistry</code> ...
 */
public class NodeTypeRegistryImpl implements Dumpable, NodeTypeRegistry {

    private static Logger log = LoggerFactory.getLogger(NodeTypeRegistryImpl.class);

    // cache of pre-built aggregations of node types
    private final EffectiveNodeTypeCache entCache;

    // map of node type names and node type definitions
    private final ConcurrentReaderHashMap registeredNTDefs;

    // definition of the root node
    private final QNodeDefinition rootNodeDef;

    // set of property definitions
    private final Set propDefs;
    // set of node definitions
    private final Set nodeDefs;

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
    private final Map listeners = Collections.synchronizedMap(new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK));

    /**
     * Create a new <code>NodeTypeRegistry</codes>
     *
     * @param nodeTypeDefs
     * @param nsRegistry
     * @return <code>NodeTypeRegistry</codes> object
     * @throws RepositoryException
     */
    public static NodeTypeRegistryImpl create(Collection nodeTypeDefs, NodeTypeStorage storage, QNodeDefinition rootNodeDef, NamespaceRegistry nsRegistry)
            throws RepositoryException {
        NodeTypeRegistryImpl ntRegistry = new NodeTypeRegistryImpl(nodeTypeDefs, storage, rootNodeDef, nsRegistry);
        return ntRegistry;
    }

    /**
     * Private constructor
     *
     * @param nodeTypeDefs
     * @param nsRegistry
     * @throws RepositoryException
     */
    private NodeTypeRegistryImpl(Collection nodeTypeDefs, NodeTypeStorage storage, QNodeDefinition rootNodeDef, NamespaceRegistry nsRegistry)
            throws RepositoryException {
        this.storage = storage;
        this.validator = new DefinitionValidator(this, nsRegistry);

        entCache = new EffectiveNodeTypeCache();
        registeredNTDefs = new ConcurrentReaderHashMap();

        propDefs = new HashSet();
        nodeDefs = new HashSet();

        // setup definition of root node
        this.rootNodeDef = rootNodeDef;
        synchronized (nodeDefs) {
            nodeDefs.add(rootNodeDef);
        }

        try {
            // validate & register the definitions
            /* Note: since the client reads all nodetypes from the server, it is
             * not able to distinguish between built-in and custom-defined
             * nodetypes (compared to Jackrabbit-core) */
            Map defMap = validator.validateNodeTypeDefs(nodeTypeDefs, new HashMap(registeredNTDefs));
            internalRegister(defMap);
        } catch (InvalidNodeTypeDefException intde) {
            String error = "Unexpected error: Found invalid node type definition.";
            log.debug(error);
            throw new RepositoryException(error, intde);
        }
    }



    /**
     * @inheritDoc
     */
    public void addListener(NodeTypeRegistryListener listener) {
        if (!listeners.containsKey(listener)) {
            listeners.put(listener, listener);
        }
    }

    /**
     * @inheritDoc
     */
    public void removeListener(NodeTypeRegistryListener listener) {
        listeners.remove(listener);
    }

    /**
     * @inheritDoc
     */
    public QName[] getRegisteredNodeTypes() {
        return (QName[]) registeredNTDefs.keySet().toArray(new QName[registeredNTDefs.size()]);
    }


    /**
     * @inheritDoc
     */
    public boolean isRegistered(QName nodeTypeName) {
        return registeredNTDefs.containsKey(nodeTypeName);
    }

    /**
     * @inheritDoc
     */
    public QNodeDefinition getRootNodeDef() {
        return rootNodeDef;
    }

    /**
     * @inheritDoc
     */
    public synchronized EffectiveNodeType getEffectiveNodeType(QName ntName)
            throws NoSuchNodeTypeException {
        return getEffectiveNodeType(ntName, entCache, registeredNTDefs);
    }

    /**
     * @inheritDoc
     */
    public synchronized EffectiveNodeType getEffectiveNodeType(QName[] ntNames)
            throws NodeTypeConflictException, NoSuchNodeTypeException {
        return getEffectiveNodeType(ntNames, entCache, registeredNTDefs);
    }

    /**
     * @inheritDoc
     */
    public EffectiveNodeType getEffectiveNodeType(QName[] ntNames, Map ntdMap)
        throws NodeTypeConflictException, NoSuchNodeTypeException {
        return getEffectiveNodeType(ntNames, entCache, ntdMap);
    }

    /**
     *
     * @param ntName
     * @param entCache
     * @param ntdCache
     * @return
     * @throws NoSuchNodeTypeException
     */
    private EffectiveNodeType getEffectiveNodeType(QName ntName,
                                                   EffectiveNodeTypeCache entCache,
                                                   Map ntdCache)
        throws NoSuchNodeTypeException {
        // 1. check if effective node type has already been built
        EffectiveNodeType ent = entCache.get(new QName[]{ntName});
        if (ent != null) {
            return ent;
        }

        // 2. make sure we've got the definition of the specified node type
        QNodeTypeDefinition ntd = (QNodeTypeDefinition) ntdCache.get(ntName);
        if (ntd == null) {
            throw new NoSuchNodeTypeException(ntName.toString());
        }

        // 3. build effective node type
        synchronized (entCache) {
            try {
                ent = EffectiveNodeTypeImpl.create(this, ntd, ntdCache);
                // store new effective node type
                entCache.put(ent);
                return ent;
            } catch (NodeTypeConflictException ntce) {
                // should never get here as all known node types should be valid!
                String msg = "internal error: encountered invalid registered node type " + ntName;
                log.debug(msg);
                throw new NoSuchNodeTypeException(msg, ntce);
            }
        }
    }

    /**
     * @param ntNames
     * @param entCache
     * @param ntdCache
     * @return
     * @throws NodeTypeConflictException
     * @throws NoSuchNodeTypeException
     */
    private EffectiveNodeType getEffectiveNodeType(QName[] ntNames,
                                                   EffectiveNodeTypeCache entCache,
                                                   Map ntdCache)
        throws NodeTypeConflictException, NoSuchNodeTypeException {

        EffectiveNodeTypeCache.WeightedKey key = new EffectiveNodeTypeCache.WeightedKey(ntNames);

        // 1. check if aggregate has already been built
        if (entCache.contains(key)) {
            return entCache.get(key);
        }

        // 2. make sure we've got the definitions of the specified node types
        for (int i = 0; i < ntNames.length; i++) {
            if (!ntdCache.containsKey(ntNames[i])) {
                throw new NoSuchNodeTypeException(ntNames[i].toString());
            }
        }

        // 3. build aggregate
        EffectiveNodeTypeImpl result = null;
        synchronized (entCache) {
            // build list of 'best' existing sub-aggregates
            ArrayList tmpResults = new ArrayList();
            while (key.getNames().length > 0) {
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
                Iterator iter = entCache.keyIterator();
                while (iter.hasNext()) {
                    EffectiveNodeTypeCache.WeightedKey k =
                            (EffectiveNodeTypeCache.WeightedKey) iter.next();
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
                    QName[] remainder = key.getNames();
                    for (int i = 0; i < remainder.length; i++) {
                        QNodeTypeDefinition ntd = (QNodeTypeDefinition) ntdCache.get(remainder[i]);
                        EffectiveNodeTypeImpl ent = EffectiveNodeTypeImpl.create(this, ntd, ntdCache);
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
                    result = (EffectiveNodeTypeImpl) tmpResults.get(i);
                } else {
                    result = result.merge((EffectiveNodeTypeImpl) tmpResults.get(i));
                    // store intermediate result
                    entCache.put(result);
                }
            }
        }
        // we're done
        return result;
    }

    /**
     * @inheritDoc
     */
    public synchronized EffectiveNodeType registerNodeType(QNodeTypeDefinition ntDef)
            throws InvalidNodeTypeDefException, RepositoryException {
        // validate the new nodetype definition
        EffectiveNodeTypeImpl ent = validator.validateNodeTypeDef(ntDef, registeredNTDefs);

        // persist new node type definition
        storage.registerNodeTypes(new QNodeTypeDefinition[] {ntDef});

        // update internal caches
        internalRegister(ntDef, ent);

        // notify listeners
        notifyRegistered(ntDef.getQName());
        return ent;
    }

    /**
     * @inheritDoc
     */
    public synchronized void registerNodeTypes(Collection ntDefs)
            throws InvalidNodeTypeDefException, RepositoryException {

        // validate new nodetype definitions
        Map defMap = validator.validateNodeTypeDefs(ntDefs, registeredNTDefs);
        storage.registerNodeTypes((QNodeTypeDefinition[])ntDefs.toArray(new QNodeTypeDefinition[ntDefs.size()]));

        // update internal cache
        internalRegister(defMap);

        // notify listeners
        for (Iterator iter = ntDefs.iterator(); iter.hasNext();) {
            QName ntName = ((QNodeTypeDefinition)iter.next()).getQName();
            notifyRegistered(ntName);
        }
    }

    /**
     * @inheritDoc
     */
    public void unregisterNodeType(QName nodeTypeName) throws NoSuchNodeTypeException, RepositoryException {
        HashSet ntNames = new HashSet();
        ntNames.add(nodeTypeName);
        unregisterNodeTypes(ntNames);
    }

    /**
     * @inheritDoc
     */
    public synchronized void unregisterNodeTypes(Collection nodeTypeNames)
            throws NoSuchNodeTypeException, RepositoryException {
        // do some preliminary checks
        for (Iterator iter = nodeTypeNames.iterator(); iter.hasNext();) {
            QName ntName = (QName) iter.next();
            if (!registeredNTDefs.containsKey(ntName)) {
                throw new NoSuchNodeTypeException(ntName.toString());
            }

            // check for node types other than those to be unregistered
            // that depend on the given node types
            Set dependents = getDependentNodeTypes(ntName);
            dependents.removeAll(nodeTypeNames);
            if (dependents.size() > 0) {
                StringBuffer msg = new StringBuffer();
                msg.append(ntName
                        + " can not be removed because the following node types depend on it: ");
                for (Iterator depIter = dependents.iterator(); depIter.hasNext();) {
                    msg.append(depIter.next());
                    msg.append(" ");
                }
                throw new RepositoryException(msg.toString());
            }
        }

        // persist removal of node type definitions
        // NOTE: conflict with existing content not asserted on client
        storage.unregisterNodeTypes((QName[]) nodeTypeNames.toArray(new QName[nodeTypeNames.size()]));


        // all preconditions are met, node types can now safely be unregistered
        internalUnregister(nodeTypeNames);

        // notify listeners
        for (Iterator iter = nodeTypeNames.iterator(); iter.hasNext();) {
            QName ntName = (QName) iter.next();
            notifyUnregistered(ntName);
        }
    }

    /**
     * @inheritDoc
     */
    public synchronized EffectiveNodeType reregisterNodeType(QNodeTypeDefinition ntd)
            throws NoSuchNodeTypeException, InvalidNodeTypeDefException,
            RepositoryException {
        QName name = ntd.getQName();
        if (!registeredNTDefs.containsKey(name)) {
            throw new NoSuchNodeTypeException(name.toString());
        }
        /* validate new node type definition */
        EffectiveNodeTypeImpl ent = validator.validateNodeTypeDef(ntd, registeredNTDefs);

        // first call reregistering on storage
        storage.reregisterNodeTypes(new QNodeTypeDefinition[]{ntd});

        // unregister old node type definition
        internalUnregister(name);
        // register new definition
        internalRegister(ntd, ent);

        // notify listeners
        notifyReRegistered(name);
        return ent;
    }

    /**
     * @inheritDoc
     */
    public QNodeTypeDefinition getNodeTypeDefinition(QName nodeTypeName)
        throws NoSuchNodeTypeException {
        QNodeTypeDefinition def = (QNodeTypeDefinition) registeredNTDefs.get(nodeTypeName);
        if (def == null) {
            throw new NoSuchNodeTypeException(nodeTypeName.toString());
        }
        return def;
    }

    //------------------------------------------------------------< private >---
    /**
     * Notify the listeners that a node type <code>ntName</code> has been registered.
     */
    private void notifyRegistered(QName ntName) {
        // copy listeners to array to avoid ConcurrentModificationException
        NodeTypeRegistryListener[] la =
                new NodeTypeRegistryListener[listeners.size()];
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

    private void internalRegister(Map defMap) {
        Iterator it = defMap.keySet().iterator();
        while (it.hasNext()) {
            QNodeTypeDefinition ntd = (QNodeTypeDefinition)it.next();
            internalRegister(ntd, (EffectiveNodeTypeImpl)defMap.get(ntd));
        }
    }

    private void internalRegister(QNodeTypeDefinition ntd, EffectiveNodeTypeImpl ent) {

        // store new effective node type instance
        entCache.put(ent);
        // register nt-definition
        registeredNTDefs.put(ntd.getQName(), ntd);

        // store property & child node definitions of new node type by id
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
        synchronized (propDefs) {
            for (int i = 0; i < pda.length; i++) {
                propDefs.add(pda[i]);
            }
        }
        QNodeDefinition[] nda = ntd.getChildNodeDefs();
        synchronized (nodeDefs) {
            for (int i = 0; i < nda.length; i++) {
                nodeDefs.add(nda[i]);
            }
        }
    }

    private void internalUnregister(QName name) {
        /*
         * NOTE: detection of built-in NodeTypes not possible, since the client
         * reads all nodetypes from the 'server' only without being able to
         * destinguish between built-in and custom-defined nodetypes.
         */
        QNodeTypeDefinition ntd = (QNodeTypeDefinition) registeredNTDefs.get(name);
        registeredNTDefs.remove(name);
        /*
         * remove all affected effective node types from aggregates cache
         * (copy keys first to prevent ConcurrentModificationException)
         */
        ArrayList keys = new ArrayList(entCache.keySet());
        for (Iterator keysIter = keys.iterator(); keysIter.hasNext();) {
            EffectiveNodeTypeCache.WeightedKey k =
                    (EffectiveNodeTypeCache.WeightedKey) keysIter.next();
            EffectiveNodeType ent = entCache.get(k);
            if (ent.includesNodeType(name)) {
                entCache.remove(k);
            }
        }

        // remove property & child node definitions
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
        synchronized (propDefs) {
            for (int i = 0; i < pda.length; i++) {
                propDefs.remove(pda[i]);
            }
        }
        synchronized (nodeDefs) {
            QNodeDefinition[] nda = ntd.getChildNodeDefs();
            for (int i = 0; i < nda.length; i++) {
                nodeDefs.remove(nda[i]);
            }
        }
    }

    private void internalUnregister(Collection ntNames) {
        for (Iterator iter = ntNames.iterator(); iter.hasNext();) {
            QName name = (QName) iter.next();
            internalUnregister(name);
        }
    }

   /**
     * Returns the names of those registered node types that have
     * dependencies on the given node type.
     *
     * @param nodeTypeName node type name
     * @return a set of node type <code>QName</code>s
     * @throws NoSuchNodeTypeException
     */
    private Set getDependentNodeTypes(QName nodeTypeName)
            throws NoSuchNodeTypeException {
        if (!registeredNTDefs.containsKey(nodeTypeName)) {
            throw new NoSuchNodeTypeException(nodeTypeName.toString());
        }

        // get names of those node types that have dependencies on the given nt
        HashSet names = new HashSet();
        Iterator iter = registeredNTDefs.values().iterator();
        while (iter.hasNext()) {
            QNodeTypeDefinition ntd = (QNodeTypeDefinition) iter.next();
            if (ntd.getDependencies().contains(nodeTypeName)) {
                names.add(ntd.getQName());
            }
        }
        return names;
    }

    //-----------------------------------------------------------< Dumpable >---
    /**
     * {@inheritDoc}
     */
    public void dump(PrintStream ps) {
        ps.println("NodeTypeRegistry (" + this + ")");
        ps.println();
        ps.println("Registered NodeTypes:");
        ps.println();
        Iterator iter = registeredNTDefs.values().iterator();
        while (iter.hasNext()) {
            QNodeTypeDefinition ntd = (QNodeTypeDefinition) iter.next();
            ps.println(ntd.getQName());
            QName[] supertypes = ntd.getSupertypes();
            ps.println("\tSupertypes");
            for (int i = 0; i < supertypes.length; i++) {
                ps.println("\t\t" + supertypes[i]);
            }
            ps.println("\tMixin\t" + ntd.isMixin());
            ps.println("\tOrderableChildNodes\t" + ntd.hasOrderableChildNodes());
            ps.println("\tPrimaryItemName\t" + (ntd.getPrimaryItemName() == null ? "<null>" : ntd.getPrimaryItemName().toString()));
            QPropertyDefinition[] pd = ntd.getPropertyDefs();
            for (int i = 0; i < pd.length; i++) {
                ps.print("\tPropertyDefinition");
                ps.println(" (declared in " + pd[i].getDeclaringNodeType() + ") ");
                ps.println("\t\tName\t\t" + (pd[i].definesResidual() ? "*" : pd[i].getQName().toString()));
                String type = pd[i].getRequiredType() == 0 ? "null" : PropertyType.nameFromValue(pd[i].getRequiredType());
                ps.println("\t\tRequiredType\t" + type);
                String[] vca = pd[i].getValueConstraints();
                StringBuffer constraints = new StringBuffer();
                if (vca == null) {
                    constraints.append("<null>");
                } else {
                    for (int n = 0; n < vca.length; n++) {
                        if (constraints.length() > 0) {
                            constraints.append(", ");
                        }
                        constraints.append(vca[n]);
                    }
                }
                ps.println("\t\tValueConstraints\t" + constraints.toString());
                String[] defVals = pd[i].getDefaultValues();
                StringBuffer defaultValues = new StringBuffer();
                if (defVals == null) {
                    defaultValues.append("<null>");
                } else {
                    for (int n = 0; n < defVals.length; n++) {
                        if (defaultValues.length() > 0) {
                            defaultValues.append(", ");
                        }
                        defaultValues.append(defVals[n]);
                    }
                }
                ps.println("\t\tDefaultValue\t" + defaultValues.toString());
                ps.println("\t\tAutoCreated\t" + pd[i].isAutoCreated());
                ps.println("\t\tMandatory\t" + pd[i].isMandatory());
                ps.println("\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(pd[i].getOnParentVersion()));
                ps.println("\t\tProtected\t" + pd[i].isProtected());
                ps.println("\t\tMultiple\t" + pd[i].isMultiple());
            }
            QNodeDefinition[] nd = ntd.getChildNodeDefs();
            for (int i = 0; i < nd.length; i++) {
                ps.print("\tNodeDefinition");
                ps.println(" (declared in " + nd[i].getDeclaringNodeType() + ") ");
                ps.println("\t\tName\t\t" + (nd[i].definesResidual() ? "*" : nd[i].getQName().toString()));
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
                ps.println("\n\t\tAutoCreated\t" + nd[i].isAutoCreated());
                ps.println("\t\tMandatory\t" + nd[i].isMandatory());
                ps.println("\t\tOnVersion\t" + OnParentVersionAction.nameFromValue(nd[i].getOnParentVersion()));
                ps.println("\t\tProtected\t" + nd[i].isProtected());
                ps.println("\t\tAllowsSameNameSiblings\t" + nd[i].allowsSameNameSiblings());
            }
        }
        ps.println();

        entCache.dump(ps);
    }
}
