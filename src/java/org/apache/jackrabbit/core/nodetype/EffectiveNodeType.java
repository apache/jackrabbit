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

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.QName;
import org.apache.log4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * An <code>EffectiveNodeType</code> represents one or more
 * <code>NodeType</code>s as one 'effective' node type where inheritance
 * is resolved.
 * <p/>
 * Instances of <code>EffectiveNodeType</code> are immutable.
 */
public class EffectiveNodeType implements Cloneable {
    private static Logger log = Logger.getLogger(EffectiveNodeType.class);

    // node type registry
    private final NodeTypeRegistry ntReg;

    // list of explicitly aggregated {i.e. merged) node types
    private final TreeSet mergedNodeTypes;
    // list of implicitly aggregated {through inheritance) node types
    private final TreeSet inheritedNodeTypes;
    // list of all either explicitly (through aggregation) or implicitly
    // (through inheritance) included node types.
    private final TreeSet allNodeTypes;
    // map of named item definitions (maps name to list of definitions)
    private final HashMap namedItemDefs;
    // list of unnamed item definitions (i.e. residual definitions)
    private final ArrayList unnamedItemDefs;

    /**
     * private constructor.
     */
    private EffectiveNodeType(NodeTypeRegistry ntReg) {
        this.ntReg = ntReg;
        mergedNodeTypes = new TreeSet();
        inheritedNodeTypes = new TreeSet();
        allNodeTypes = new TreeSet();
        namedItemDefs = new HashMap();
        unnamedItemDefs = new ArrayList();
    }

    /**
     * Factory method: creates an effective node type
     * representation of an existing (i.e. registered) node type.
     *
     * @param ntReg
     * @param nodeTypeName
     * @return
     * @throws NodeTypeConflictException
     * @throws NoSuchNodeTypeException
     */
    static EffectiveNodeType create(NodeTypeRegistry ntReg, QName nodeTypeName)
            throws NodeTypeConflictException, NoSuchNodeTypeException {
        return create(ntReg, ntReg.getNodeTypeDef(nodeTypeName));
    }

    /**
     * Factory method: creates an effective node type
     * representation of a node type definition. Whereas all referenced
     * node types must exist (i.e. must be registered), the definition itself
     * is not required to be registered.
     *
     * @param ntReg
     * @param ntd
     * @return
     * @throws NodeTypeConflictException
     * @throws NoSuchNodeTypeException
     */
    public static EffectiveNodeType create(NodeTypeRegistry ntReg, NodeTypeDef ntd)
            throws NodeTypeConflictException, NoSuchNodeTypeException {
        // create empty effective node type instance
        EffectiveNodeType ent = new EffectiveNodeType(ntReg);
        QName ntName = ntd.getName();

        // prepare new instance
        ent.mergedNodeTypes.add(ntName);
        ent.allNodeTypes.add(ntName);

        NodeDef[] cnda = ntd.getChildNodeDefs();
        for (int i = 0; i < cnda.length; i++) {
            if (cnda[i].definesResidual()) {
                // residual node definition
                ent.unnamedItemDefs.add(cnda[i]);
            } else {
                // named node definition
                QName name = cnda[i].getName();
                List defs = (List) ent.namedItemDefs.get(name);
                if (defs == null) {
                    defs = new ArrayList();
                    ent.namedItemDefs.put(name, defs);
                }
                if (defs.size() > 0) {
                    /**
                     * there already exists at least one definition with that
                     * name; make sure none of them is auto-create
                     */
                    for (int j = 0; j < defs.size(); j++) {
                        ItemDef def = (ItemDef) defs.get(j);
                        if (cnda[i].isAutoCreated() || def.isAutoCreated()) {
                            // conflict
                            String msg = "There are more than one 'auto-create' item definitions for '"
                                + name + "' in node type '" + ntName + "'";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                    }
                }
                defs.add(cnda[i]);
            }
        }
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            if (pda[i].definesResidual()) {
                // residual property definition
                ent.unnamedItemDefs.add(pda[i]);
            } else {
                // named property definition
                QName name = pda[i].getName();
                List defs = (List) ent.namedItemDefs.get(name);
                if (defs == null) {
                    defs = new ArrayList();
                    ent.namedItemDefs.put(name, defs);
                }
                if (defs.size() > 0) {
                    /**
                     * there already exists at least one definition with that
                     * name; make sure none of them is auto-create
                     */
                    for (int j = 0; j < defs.size(); j++) {
                        ItemDef def = (ItemDef) defs.get(j);
                        if (pda[i].isAutoCreated() || def.isAutoCreated()) {
                            // conflict
                            String msg = "There are more than one 'auto-create' item definitions for '"
                                + name + "' in node type '" + ntName + "'";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                    }
                }
                defs.add(pda[i]);
            }
        }

        // resolve supertypes recursively
        QName[] supertypes = ntd.getSupertypes();
        if (supertypes != null && supertypes.length > 0) {
            ent.internalMerge(ntReg.getEffectiveNodeType(supertypes), true);
        }

        // we're done
        return ent;
    }

    /**
     * Factory method: creates a new 'empty' effective node type instance
     *
     * @return
     */
    static EffectiveNodeType create(NodeTypeRegistry ntReg) {
        return new EffectiveNodeType(ntReg);
    }

    public QName[] getMergedNodeTypes() {
        return (QName[]) mergedNodeTypes.toArray(new QName[mergedNodeTypes.size()]);
    }

    public QName[] getInheritedNodeTypes() {
        return (QName[]) inheritedNodeTypes.toArray(new QName[inheritedNodeTypes.size()]);
    }

    public QName[] getAllNodeTypes() {
        return (QName[]) allNodeTypes.toArray(new QName[allNodeTypes.size()]);
    }

    public ItemDef[] getAllItemDefs() {
        ArrayList defs = new ArrayList(namedItemDefs.size() + unnamedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            defs.addAll((List) iter.next());
        }
        defs.addAll(unnamedItemDefs);
        return (ItemDef[]) defs.toArray(new ItemDef[defs.size()]);
    }

    public ItemDef[] getNamedItemDefs() {
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            defs.addAll((List) iter.next());
        }
        return (ItemDef[]) defs.toArray(new ItemDef[defs.size()]);
    }

    public ItemDef[] getUnnamedItemDefs() {
        return (ItemDef[]) unnamedItemDefs.toArray(new ItemDef[unnamedItemDefs.size()]);
    }

    public boolean hasNamedItemDef(QName name) {
        return namedItemDefs.containsKey(name);
    }

    public ItemDef[] getNamedItemDefs(QName name) {
        List defs = (List) namedItemDefs.get(name);
        if (defs == null) {
            return null;
        }
        return (ItemDef[]) defs.toArray(new ItemDef[defs.size()]);
    }

    public NodeDef[] getAllNodeDefs() {
        ArrayList defs = new ArrayList(namedItemDefs.size() + unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            ItemDef def = (ItemDef) iter.next();
            if (def.definesNode()) {
                defs.add(def);
            }
        }
        iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (def.definesNode()) {
                    defs.add(def);
                }
            }
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    public NodeDef[] getNamedNodeDefs() {
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (def.definesNode()) {
                    defs.add(def);
                }
            }
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    public NodeDef[] getUnnamedNodeDefs() {
        ArrayList defs = new ArrayList(unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            ItemDef def = (ItemDef) iter.next();
            if (def.definesNode()) {
                defs.add(def);
            }
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    public NodeDef[] getAutoCreateNodeDefs() {
        // since auto-create items must have a name,
        // we're only searching the named item definitions
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (def.definesNode() && def.isAutoCreated()) {
                    defs.add(def);
                }
            }
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    public PropDef[] getAllPropDefs() {
        ArrayList defs = new ArrayList(namedItemDefs.size() + unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            ItemDef def = (ItemDef) iter.next();
            if (!def.definesNode()) {
                defs.add(def);
            }
        }
        iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (!def.definesNode()) {
                    defs.add(def);
                }
            }
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public PropDef[] getNamedPropDefs() {
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (!def.definesNode()) {
                    defs.add(def);
                }
            }
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public PropDef[] getUnnamedPropDefs() {
        ArrayList defs = new ArrayList(unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            ItemDef def = (ItemDef) iter.next();
            if (!def.definesNode()) {
                defs.add(def);
            }
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public PropDef[] getAutoCreatePropDefs() {
        // since auto-create items must have a name,
        // we're only searching the named item definitions
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (!def.definesNode() && def.isAutoCreated()) {
                    defs.add(def);
                }
            }
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public PropDef[] getMandatoryPropDefs() {
        // since mandatory items must have a name,
        // we're only searching the named item definitions
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (!def.definesNode() && def.isMandatory()) {
                    defs.add(def);
                }
            }
        }
        return (PropDef[]) defs.toArray(new PropDef[defs.size()]);
    }

    public NodeDef[] getMandatoryNodeDefs() {
        // since mandatory items must have a name,
        // we're only searching the named item definitions
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                ItemDef def = (ItemDef) iter1.next();
                if (def.definesNode() && def.isMandatory()) {
                    defs.add(def);
                }
            }
        }
        return (NodeDef[]) defs.toArray(new NodeDef[defs.size()]);
    }

    /**
     * Determines whether this effective node type representation includes
     * (either through inheritance or aggregation) the given node type.
     *
     * @param nodeTypeName name of node type
     * @return <code>true</code> if the given node type is included, otherwise
     *         <code>false</code>
     */
    public boolean includesNodeType(QName nodeTypeName) {
        return allNodeTypes.contains(nodeTypeName);
    }

    /**
     * Tests if the value constraints defined in the property definition
     * <code>pd</code> are satisfied by the the specified <code>values</code>.
     * <p/>
     * Note that the <i>protected</i> flag is not checked. Also note that no
     * type conversions are attempted if the type of the given values does not
     * match the required type as specified in the given definition.
     *
     * @param pd
     * @param values
     * @throws ConstraintViolationException
     */
    public static void checkSetPropertyValueConstraints(PropDef pd,
                                                        InternalValue[] values)
            throws ConstraintViolationException, RepositoryException {
        // check multi-value flag
        if (!pd.isMultiple() && values != null && values.length > 1) {
            throw new ConstraintViolationException("the property is not multi-valued");
        }

        ValueConstraint[] constraints = pd.getValueConstraints();
        if (constraints == null || constraints.length == 0) {
            // no constraints to check
            return;
        }
        if (values != null && values.length > 0) {
            // check value constraints on every value
            for (int i = 0; i < values.length; i++) {
                // constraints are OR-ed together
                boolean satisfied = false;
                ConstraintViolationException cve = null;
                for (int j = 0; j < constraints.length; j++) {
                    try {
                        constraints[j].check(values[i]);
                        satisfied = true;
                        break;
                    } catch (ConstraintViolationException e) {
                        cve = e;
                        continue;
                    }
                }
                if (!satisfied) {
                    // re-throw last exception we encountered
                    throw cve;
                }
            }
        }
    }

    /**
     * @param name
     * @throws ConstraintViolationException
     */
    public void checkAddNodeConstraints(QName name)
            throws ConstraintViolationException {
        try {
            getApplicableChildNodeDef(name, null);
        } catch (NoSuchNodeTypeException nsnte) {
            String msg = "internal eror: inconsistent node type";
            log.debug(msg);
            throw new ConstraintViolationException(msg, nsnte);
        }
    }

    /**
     * @param name
     * @param nodeTypeName
     * @throws ConstraintViolationException
     * @throws NoSuchNodeTypeException
     */
    public void checkAddNodeConstraints(QName name, QName nodeTypeName)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        NodeDef nd = getApplicableChildNodeDef(name, nodeTypeName);
        if (nd.isProtected()) {
            throw new ConstraintViolationException(name + " is protected");
        }
        if (nd.isAutoCreated()) {
            throw new ConstraintViolationException(name + " is auto-created and can not be manually added");
        }
    }

    /**
     * Returns the applicable child node definition for a child node with the
     * specified name and node type.
     *
     * @param name
     * @param nodeTypeName
     * @return
     * @throws NoSuchNodeTypeException
     * @throws ConstraintViolationException if no applicable child node definition
     *                                      could be found
     */
    public NodeDef getApplicableChildNodeDef(QName name, QName nodeTypeName)
            throws NoSuchNodeTypeException, ConstraintViolationException {
        // try named node definitions first
        ItemDef[] defs = getNamedItemDefs(name);
        if (defs != null) {
            for (int i = 0; i < defs.length; i++) {
                ItemDef def = defs[i];
                if (def.definesNode()) {
                    NodeDef nd = (NodeDef) def;
                    // node definition with that name exists
                    if (nodeTypeName != null) {
                        try {
                            // check node type constraints
                            checkRequiredPrimaryType(nodeTypeName, nd.getRequiredPrimaryTypes());
                        } catch (ConstraintViolationException cve) {
                            // ignore and try next
                            continue;
                        }
                        // found node definition
                        return nd;
                    } else {
                        if (nd.getDefaultPrimaryType() == null) {
                            // no default node type defined, try next
                            continue;
                        } else {
                            // found node definition with default node type
                            return nd;
                        }
                    }
                }
            }
        }

        // no item with that name defined;
        // try residual node definitions
        NodeDef[] nda = getUnnamedNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            NodeDef nd = nda[i];
            if (nodeTypeName != null) {
                try {
                    // check node type constraint
                    checkRequiredPrimaryType(nodeTypeName, nd.getRequiredPrimaryTypes());
                } catch (ConstraintViolationException e) {
                    // ignore and try next
                    continue;
                }
                // found residual node definition
                return nd;
            } else {
                // since no node type has been specified for the new node,
                // it must be determined from the default node type;
                if (nd.getDefaultPrimaryType() != null) {
                    // found residual node definition with default node type
                    return nd;
                }
            }
        }

        // no applicable definition found
        throw new ConstraintViolationException("no matching child node definition found for " + name);
    }

    /**
     * Returns the applicable property definition for a property with the
     * specified name, type and multiValued characteristic.
     *
     * @param name
     * @param type
     * @param multiValued
     * @return
     * @throws ConstraintViolationException if no applicable property definition
     *                                      could be found
     */
    public PropDef getApplicablePropertyDef(QName name, int type,
                                            boolean multiValued)
            throws ConstraintViolationException {
        // try named property definitions first
        ItemDef[] defs = getNamedItemDefs(name);
        if (defs != null) {
            for (int i = 0; i < defs.length; i++) {
                ItemDef def = defs[i];
                if (!def.definesNode()) {
                    PropDef pd = (PropDef) def;
                    int reqType = pd.getRequiredType();
                    // property definition with that name exists
                    // match type
                    if (reqType == PropertyType.UNDEFINED
                            || type == PropertyType.UNDEFINED
                            || reqType == type) {
                        // match multiValued flag
                        if (multiValued == pd.isMultiple()) {
                            // found match
                            return pd;
                        }
                    }
                }
            }
        }

        // no item with that name defined;
        // try residual property definitions
        PropDef[] pda = getUnnamedPropDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            int reqType = pd.getRequiredType();
            // match type
            if (reqType == PropertyType.UNDEFINED
                    || type == PropertyType.UNDEFINED
                    || reqType == type) {
                // match multiValued flag
                if (multiValued == pd.isMultiple()) {
                    // found match
                    return pd;
                }
            }
        }

        // no applicable definition found
        throw new ConstraintViolationException("no matching property definition found for " + name);
    }

    /**
     * @param name
     * @throws ConstraintViolationException
     */
    public void checkRemoveItemConstraints(QName name) throws ConstraintViolationException {
        /**
         * as there might be multiple definitions with the same name and we
         * don't know which one is applicable, we check all of them
         */
        ItemDef[] defs = getNamedItemDefs(name);
        if (defs != null) {
            for (int i = 0; i < defs.length; i++) {
                if (defs[i].isMandatory()) {
                    throw new ConstraintViolationException("can't remove mandatory item");
                }
                if (defs[i].isProtected()) {
                    throw new ConstraintViolationException("can't remove protected item");
                }
            }
        }
    }

    /**
     * @param nodeTypeName
     * @param requiredPrimaryTypes
     * @throws ConstraintViolationException
     * @throws NoSuchNodeTypeException
     */
    public void checkRequiredPrimaryType(QName nodeTypeName, QName[] requiredPrimaryTypes)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        if (requiredPrimaryTypes == null) {
            // no constraint
            return;
        }
        EffectiveNodeType ent;
        try {
            ent = ntReg.getEffectiveNodeType(nodeTypeName);
        } catch (RepositoryException re) {
            String msg = "failed to check node type constraint";
            log.debug(msg);
            throw new ConstraintViolationException(msg, re);
        }
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            if (!ent.includesNodeType(requiredPrimaryTypes[i])) {
                throw new ConstraintViolationException("node type constraint not satisfied: " + requiredPrimaryTypes[i]);
            }
        }
    }

    /**
     * Merges another <code>EffectiveNodeType</code> with this one.
     * Checks for merge conflicts.
     *
     * @param other
     * @return
     * @throws NodeTypeConflictException
     */
    EffectiveNodeType merge(EffectiveNodeType other)
            throws NodeTypeConflictException {
        // create a clone of this instance and perform the merge on
        // the 'clone' to avoid a potentially inconsistant state
        // of this instance if an exception is thrown during
        // the merge.
        EffectiveNodeType copy = (EffectiveNodeType) clone();
        copy.internalMerge(other, false);
        return copy;
    }

    /**
     * Internal helper method which merges another <code>EffectiveNodeType</code>
     * instance with <i>this</i> instance.
     * <p/>
     * Warning: This instance might be in an inconsistent state if an exception
     * is thrown.
     *
     * @param other
     * @param supertype true if the merge is a result of inheritance, i.e. <code>other</code>
     *                  represents one or more supertypes of this instance; otherwise false, i.e.
     *                  the merge is the result of an explicit aggregation
     * @throws NodeTypeConflictException
     */
    private synchronized void internalMerge(EffectiveNodeType other, boolean supertype)
            throws NodeTypeConflictException {
        QName[] nta = other.getAllNodeTypes();
        int includedCount = 0;
        for (int i = 0; i < nta.length; i++) {
            if (includesNodeType(nta[i])) {
                // redundant node type
                log.debug("node type '" + nta[i] + "' is already contained.");
                includedCount++;
            }
        }
        if (includedCount == nta.length) {
            // total overlap, ignore
            return;
        }

        // named item definitions
        ItemDef[] defs = other.getNamedItemDefs();
        for (int i = 0; i < defs.length; i++) {
            ItemDef def = defs[i];
            if (includesNodeType(def.getDeclaringNodeType())) {
                // ignore redundant definitions
                continue;
            }
            QName name = def.getName();
            List existingDefs = (List) namedItemDefs.get(name);
            if (existingDefs != null) {
                if (existingDefs.size() > 0) {
                    // there already exists at least one definition with that name
                    for (int j = 0; j < existingDefs.size(); j++) {
                        ItemDef existingDef = (ItemDef) existingDefs.get(j);
                        // make sure none of them is auto-create
                        if (def.isAutoCreated() || existingDef.isAutoCreated()) {
                            // conflict
                            String msg = "The item definition for '" + name
                                    + "' in node type '"
                                    + def.getDeclaringNodeType()
                                    + "' conflicts with node type '"
                                    + existingDef.getDeclaringNodeType()
                                    + "': name collision with auto-create definition";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                        // check ambiguous definitions
                        if (def.definesNode() == existingDef.definesNode()) {
                            if (!def.definesNode()) {
                                // property definition
                                PropDef pd = (PropDef) def;
                                PropDef epd = (PropDef) existingDef;
                                // compare type & multiValued flag
                                if (pd.getRequiredType() == epd.getRequiredType()
                                        && pd.isMultiple() == epd.isMultiple()) {
                                    // conflict
                                    String msg = "The property definition for '"
                                            + name + "' in node type '"
                                            + def.getDeclaringNodeType()
                                            + "' conflicts with node type '"
                                            + existingDef.getDeclaringNodeType()
                                            + "': ambiguous property definition";
                                    log.debug(msg);
                                    throw new NodeTypeConflictException(msg);
                                }
                            } else {
                                // child node definition
                                // conflict
                                String msg = "The child node definition for '"
                                        + name + "' in node type '"
                                        + def.getDeclaringNodeType()
                                        + "' conflicts with node type '"
                                        + existingDef.getDeclaringNodeType()
                                        + "': ambiguous child node definition";
                                log.debug(msg);
                                throw new NodeTypeConflictException(msg);
                            }
                        }
                    }
                }
            } else {
                existingDefs = new ArrayList();
                namedItemDefs.put(name, existingDefs);
            }
            existingDefs.add(def);
        }

        // residual item definitions
        defs = other.getUnnamedItemDefs();
        for (int i = 0; i < defs.length; i++) {
            ItemDef def = defs[i];
            if (includesNodeType(def.getDeclaringNodeType())) {
                // ignore redundant definitions
                continue;
            }
            Iterator iter = unnamedItemDefs.iterator();
            while (iter.hasNext()) {
                ItemDef existing = (ItemDef) iter.next();
                // compare with existing definition
                if (def.definesNode() == existing.definesNode()) {
                    if (!def.definesNode()) {
                        // property definition
                        PropDef pd = (PropDef) def;
                        PropDef epd = (PropDef) existing;
                        // compare type & multiValued flag
                        if (pd.getRequiredType() == epd.getRequiredType()
                                && pd.isMultiple() == epd.isMultiple()) {
                            // conflict
                            String msg = "A property definition in node type '"
                                    + def.getDeclaringNodeType()
                                    + "' conflicts with node type '"
                                    + existing.getDeclaringNodeType()
                                    + "': ambiguous residual property definition";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                    } else {
                        // child node definition
                        // conflict
                        String msg = "A child node definition in node type '"
                                + def.getDeclaringNodeType()
                                + "' conflicts with node type '"
                                + existing.getDeclaringNodeType()
                                + "': ambiguous residual child node definition";
                        log.debug(msg);
                        throw new NodeTypeConflictException(msg);
                    }
                }
            }
            // @todo do further checks for ambiguous definitions & other conflicts
            unnamedItemDefs.add(def);
        }
        // @todo implement further validations

        for (int i = 0; i < nta.length; i++) {
            allNodeTypes.add(nta[i]);
        }

        if (supertype) {
            // implicit merge as result of inheritance

            // add other merged node types as supertypes
            nta = other.getMergedNodeTypes();
            for (int i = 0; i < nta.length; i++) {
                inheritedNodeTypes.add(nta[i]);
            }
            // add supertypes of other merged node types as supertypes
            nta = other.getInheritedNodeTypes();
            for (int i = 0; i < nta.length; i++) {
                inheritedNodeTypes.add(nta[i]);
            }
        } else {
            // explicit merge

            // merge with other merged node types
            nta = other.getMergedNodeTypes();
            for (int i = 0; i < nta.length; i++) {
                mergedNodeTypes.add(nta[i]);
            }
            // add supertypes of other merged node types as supertypes
            nta = other.getInheritedNodeTypes();
            for (int i = 0; i < nta.length; i++) {
                inheritedNodeTypes.add(nta[i]);
            }
        }
    }

    protected Object clone() {
        EffectiveNodeType clone = new EffectiveNodeType(ntReg);

        clone.mergedNodeTypes.addAll(mergedNodeTypes);
        clone.inheritedNodeTypes.addAll(inheritedNodeTypes);
        clone.allNodeTypes.addAll(allNodeTypes);
        Iterator iter = namedItemDefs.keySet().iterator();
        while (iter.hasNext()) {
            Object key = iter.next();
            List list = (List) namedItemDefs.get(key);
            clone.namedItemDefs.put(key, new ArrayList(list));
        }
        clone.unnamedItemDefs.addAll(unnamedItemDefs);

        return clone;
    }
}
