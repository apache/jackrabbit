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

import org.apache.jackrabbit.spi.QItemDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

/**
 * An <code>EffectiveNodeType</code> represents one or more
 * <code>NodeType</code>s as one 'effective' node type where inheritance
 * is resolved.
 * <p/>
 * Instances of <code>EffectiveNodeType</code> are immutable.
 */
public class EffectiveNodeTypeImpl implements Cloneable, EffectiveNodeType {
    private static Logger log = LoggerFactory.getLogger(EffectiveNodeTypeImpl.class);

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
    private EffectiveNodeTypeImpl() {
        mergedNodeTypes = new TreeSet();
        inheritedNodeTypes = new TreeSet();
        allNodeTypes = new TreeSet();
        namedItemDefs = new HashMap();
        unnamedItemDefs = new ArrayList();
    }

    /**
     * Factory method: creates an effective node type
     * representation of a node type definition. Whereas all referenced
     * node types must exist (i.e. must be registered), the definition itself
     * is not required to be registered.
     *
     * @param ntReg
     * @param ntd
     * @param ntdMap 
     * @return
     * @throws NodeTypeConflictException
     * @throws NoSuchNodeTypeException
     */
    static EffectiveNodeTypeImpl create(NodeTypeRegistry ntReg, QNodeTypeDefinition ntd, Map ntdMap)
            throws NodeTypeConflictException, NoSuchNodeTypeException {
        // create empty effective node type instance
        EffectiveNodeTypeImpl ent = new EffectiveNodeTypeImpl();
        QName ntName = ntd.getQName();

        // prepare new instance
        ent.mergedNodeTypes.add(ntName);
        ent.allNodeTypes.add(ntName);

        // map of all item definitions (maps id to definition)
        // used to effectively detect ambiguous child definitions where
        // ambiguity is defined in terms of definition identity
        Set itemDefIds = new HashSet();

        QNodeDefinition[] cnda = ntd.getChildNodeDefs();
        for (int i = 0; i < cnda.length; i++) {
            // check if child node definition would be ambiguous within
            // this node type definition
            if (itemDefIds.contains(cnda[i])) {
                // conflict
                String msg;
                if (cnda[i].definesResidual()) {
                    msg = ntName + " contains ambiguous residual child node definitions";
                } else {
                    msg = ntName + " contains ambiguous definitions for child node named "
                            + cnda[i].getQName();
                }
                log.debug(msg);
                throw new NodeTypeConflictException(msg);
            } else {
                itemDefIds.add(cnda[i]);
            }
            if (cnda[i].definesResidual()) {
                // residual node definition
                ent.unnamedItemDefs.add(cnda[i]);
            } else {
                // named node definition
                QName name = cnda[i].getQName();
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
                        QItemDefinition qDef = (QItemDefinition) defs.get(j);
                        if (cnda[i].isAutoCreated() || qDef.isAutoCreated()) {
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
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            // check if property definition would be ambiguous within
            // this node type definition
            if (itemDefIds.contains(pda[i])) {
                // conflict
                String msg;
                if (pda[i].definesResidual()) {
                    msg = ntName + " contains ambiguous residual property definitions";
                } else {
                    msg = ntName + " contains ambiguous definitions for property named "
                            + pda[i].getQName();
                }
                log.debug(msg);
                throw new NodeTypeConflictException(msg);
            } else {
                itemDefIds.add(pda[i]);
            }
            if (pda[i].definesResidual()) {
                // residual property definition
                ent.unnamedItemDefs.add(pda[i]);
            } else {
                // named property definition
                QName name = pda[i].getQName();
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
                        QItemDefinition qDef = (QItemDefinition) defs.get(j);
                        if (pda[i].isAutoCreated() || qDef.isAutoCreated()) {
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
        if (supertypes.length > 0) {
            EffectiveNodeTypeImpl effSuperType = (EffectiveNodeTypeImpl)ntReg.getEffectiveNodeType(supertypes, ntdMap);
            ent.internalMerge(effSuperType, true);
        }

        // we're done
        return ent;
    }

    /**
     * Factory method: creates a new 'empty' effective node type instance
     *
     * @return a new EffectiveNodeType
     */
    static EffectiveNodeType create() {
        return new EffectiveNodeTypeImpl();
    }

    //--------------------------------------------------< EffectiveNodeType >---
    /**
     * @inheritDoc
     */
    public QName[] getInheritedNodeTypes() {
        return (QName[]) inheritedNodeTypes.toArray(new QName[inheritedNodeTypes.size()]);
    }

    /**
     * @inheritDoc
     */
    public QName[] getAllNodeTypes() {
        return (QName[]) allNodeTypes.toArray(new QName[allNodeTypes.size()]);
    }

    /**
     * @inheritDoc
     */
    public QName[] getMergedNodeTypes() {
        return (QName[]) mergedNodeTypes.toArray(new QName[mergedNodeTypes.size()]);
    }

    /**
     * @inheritDoc
     */
    public QNodeDefinition[] getAllNodeDefs() {
        if (namedItemDefs.size() == 0 && unnamedItemDefs.size() == 0) {
            return QNodeDefinition.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size() + unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            QItemDefinition qDef = (QItemDefinition) iter.next();
            if (qDef.definesNode()) {
                defs.add(qDef);
            }
        }
        iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                QItemDefinition qDef = (QItemDefinition) iter1.next();
                if (qDef.definesNode()) {
                    defs.add(qDef);
                }
            }
        }
        if (defs.size() == 0) {
            return QNodeDefinition.EMPTY_ARRAY;
        }
        return (QNodeDefinition[]) defs.toArray(new QNodeDefinition[defs.size()]);
    }

    /**
     * @inheritDoc
     */
    public QPropertyDefinition[] getAllPropDefs() {
        if (namedItemDefs.size() == 0 && unnamedItemDefs.size() == 0) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size() + unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            QItemDefinition qDef = (QItemDefinition) iter.next();
            if (!qDef.definesNode()) {
                defs.add(qDef);
            }
        }
        iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                QItemDefinition qDef = (QItemDefinition) iter1.next();
                if (!qDef.definesNode()) {
                    defs.add(qDef);
                }
            }
        }
        if (defs.size() == 0) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        return (QPropertyDefinition[]) defs.toArray(new QPropertyDefinition[defs.size()]);
    }

    /**
     * @inheritDoc
     */
    public QNodeDefinition[] getAutoCreateNodeDefs() {
        // since auto-create items must have a name,
        // we're only searching the named item definitions
        if (namedItemDefs.size() == 0) {
            return QNodeDefinition.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                QItemDefinition qDef = (QItemDefinition) iter1.next();
                if (qDef.definesNode() && qDef.isAutoCreated()) {
                    defs.add(qDef);
                }
            }
        }
        if (defs.size() == 0) {
            return QNodeDefinition.EMPTY_ARRAY;
        }
        return (QNodeDefinition[]) defs.toArray(new QNodeDefinition[defs.size()]);
    }

    /**
     * @inheritDoc
     */
    public QPropertyDefinition[] getAutoCreatePropDefs() {
        // since auto-create items must have a name,
        // we're only searching the named item definitions
        if (namedItemDefs.size() == 0) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                QItemDefinition qDef = (QItemDefinition) iter1.next();
                if (!qDef.definesNode() && qDef.isAutoCreated()) {
                    defs.add(qDef);
                }
            }
        }
        if (defs.size() == 0) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        return (QPropertyDefinition[]) defs.toArray(new QPropertyDefinition[defs.size()]);
    }

    /**
     * @inheritDoc
     */
    public QPropertyDefinition[] getMandatoryPropDefs() {
        // since mandatory items must have a name,
        // we're only searching the named item definitions
        if (namedItemDefs.size() == 0) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                QItemDefinition qDef = (QItemDefinition) iter1.next();
                if (!qDef.definesNode() && qDef.isMandatory()) {
                    defs.add(qDef);
                }
            }
        }
        if (defs.size() == 0) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        return (QPropertyDefinition[]) defs.toArray(new QPropertyDefinition[defs.size()]);
    }

    /**
     * @inheritDoc
     */
    public QNodeDefinition[] getMandatoryNodeDefs() {
        // since mandatory items must have a name,
        // we're only searching the named item definitions
        if (namedItemDefs.size() == 0) {
            return QNodeDefinition.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            List list = (List) iter.next();
            Iterator iter1 = list.iterator();
            while (iter1.hasNext()) {
                QItemDefinition qDef = (QItemDefinition) iter1.next();
                if (qDef.definesNode() && qDef.isMandatory()) {
                    defs.add(qDef);
                }
            }
        }
        if (defs.size() == 0) {
            return QNodeDefinition.EMPTY_ARRAY;
        }
        return (QNodeDefinition[]) defs.toArray(new QNodeDefinition[defs.size()]);
    }

    /**
     * @inheritDoc
     */
    public boolean includesNodeType(QName nodeTypeName) {
        return allNodeTypes.contains(nodeTypeName);
    }

    /**
     * @inheritDoc
     */
    public boolean includesNodeTypes(QName[] nodeTypeNames) {
        return allNodeTypes.containsAll(Arrays.asList(nodeTypeNames));
    }

    /**
     * @inheritDoc
     */
    public void checkAddNodeConstraints(QName name, NodeTypeRegistry ntReg)
            throws ConstraintViolationException {
        try {
            getApplicableNodeDefinition(name, null, ntReg);
        } catch (NoSuchNodeTypeException e) {
            String msg = "internal eror: inconsistent node type";
            log.debug(msg);
            throw new ConstraintViolationException(msg, e);
        }
    }

    /**
     * @inheritDoc
     */
    public void checkAddNodeConstraints(QName name, QName nodeTypeName, NodeTypeRegistry ntReg)
            throws ConstraintViolationException, NoSuchNodeTypeException {
        QNodeDefinition nd = getApplicableNodeDefinition(name, nodeTypeName, ntReg);
        if (nd.isProtected()) {
            throw new ConstraintViolationException(name + " is protected");
        }
        if (nd.isAutoCreated()) {
            throw new ConstraintViolationException(name + " is auto-created and can not be manually added");
        }
    }

    /**
     * @inheritDoc
     */
    public void checkRemoveItemConstraints(QName name) throws ConstraintViolationException {
        /**
         * as there might be multiple definitions with the same name and we
         * don't know which one is applicable, we check all of them
         */
        QItemDefinition[] defs = getNamedItemDefs(name);
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
     * @inheritDoc
     */
    public QNodeDefinition getApplicableNodeDefinition(QName name, QName nodeTypeName,
                                                       NodeTypeRegistry ntReg)
            throws NoSuchNodeTypeException, ConstraintViolationException {
        EffectiveNodeType entTarget;
        if (nodeTypeName != null) {
            entTarget = ntReg.getEffectiveNodeType(nodeTypeName);
        } else {
            entTarget = null;
        }

        // try named node definitions first
        QItemDefinition[] defs = getNamedItemDefs(name);
        if (defs != null) {
            for (int i = 0; i < defs.length; i++) {
                QItemDefinition def = defs[i];
                if (def.definesNode()) {
                    QNodeDefinition nd = (QNodeDefinition) def;
                    // node definition with that name exists
                    if (entTarget != null && nd.getRequiredPrimaryTypes() != null) {
                        // check 'required primary types' constraint
                        if (entTarget.includesNodeTypes(nd.getRequiredPrimaryTypes())) {
                            // found named node definition
                            return nd;
                        }
                    } else {
                        if (nd.getDefaultPrimaryType() != null) {
                            // found node definition with default node type
                            return nd;
                        }
                    }
                }
            }
        }

        // no item with that name defined;
        // try residual node definitions
        QNodeDefinition[] nda = getUnnamedNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            QNodeDefinition nd = nda[i];
            if (entTarget != null && nd.getRequiredPrimaryTypes() != null) {
                // check 'required primary types' constraint
                if (entTarget.includesNodeTypes(nd.getRequiredPrimaryTypes())) {
                    // found residual node definition
                    return nd;
                }
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
     * @inheritDoc
     */
    public QPropertyDefinition getApplicablePropertyDefinition(QName name, int type,
                                                               boolean multiValued)
        throws ConstraintViolationException {
        // try named property definitions first
        QPropertyDefinition match =
                getMatchingPropDef(getNamedPropDefs(name), type, multiValued);
        if (match != null) {
            return match;
        }

        // no item with that name defined;
        // try residual property definitions
        match = getMatchingPropDef(getUnnamedPropDefs(), type, multiValued);
        if (match != null) {
            return match;
        }

        // no applicable definition found
        throw new ConstraintViolationException("no matching property definition found for " + name);
    }

    /**
     * @inheritDoc
     */
    public QPropertyDefinition[] getApplicablePropertyDefinitions(QName name, int type, boolean multiValued) throws ConstraintViolationException {
      
        QPropertyDefinition named[] = getNamedPropDefs(name);
        QPropertyDefinition unnamed[] = getUnnamedPropDefs();
        QPropertyDefinition all[] = new QPropertyDefinition[named.length + unnamed.length];
        for (int i = 0; i < all.length; i++) {
            if (i < named.length) {
                all[i] = named[i]; 
            }
            else {
                all[i] = unnamed[i - named.length];
            }
        }
      
        QPropertyDefinition result[] = getMatchingPropDefs(all, type, multiValued);
        if (result.length == 0) {
            throw new ConstraintViolationException("no matching property definition found for " + name);
        }
        else {
            return result;
        }
    }

    /**
     * @inheritDoc
     */
    public QPropertyDefinition getApplicablePropertyDefinition(QName name, int type)
            throws ConstraintViolationException {
        // try named property definitions first
        QPropertyDefinition match = getMatchingPropDef(getNamedPropDefs(name), type);
        if (match != null) {
            return match;
        }

        // no item with that name defined;
        // try residual property definitions
        match = getMatchingPropDef(getUnnamedPropDefs(), type);
        if (match != null) {
            return match;
        }

        // no applicable definition found
        throw new ConstraintViolationException("no matching property definition found for " + name);
    }

    //---------------------------------------------< impl. specific methods >---

    private QItemDefinition[] getNamedItemDefs() {
        if (namedItemDefs.size() == 0) {
            return QItemDefinition.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(namedItemDefs.size());
        Iterator iter = namedItemDefs.values().iterator();
        while (iter.hasNext()) {
            defs.addAll((List) iter.next());
        }
        if (defs.size() == 0) {
            return QItemDefinition.EMPTY_ARRAY;
        }
        return (QItemDefinition[]) defs.toArray(new QItemDefinition[defs.size()]);
    }

    private QItemDefinition[] getUnnamedItemDefs() {
        if (unnamedItemDefs.size() == 0) {
            return QItemDefinition.EMPTY_ARRAY;
        }
        return (QItemDefinition[]) unnamedItemDefs.toArray(new QItemDefinition[unnamedItemDefs.size()]);
    }

    private QItemDefinition[] getNamedItemDefs(QName name) {
        List defs = (List) namedItemDefs.get(name);
        if (defs == null || defs.size() == 0) {
            return QItemDefinition.EMPTY_ARRAY;
        }
        return (QItemDefinition[]) defs.toArray(new QItemDefinition[defs.size()]);
    }

    private QNodeDefinition[] getUnnamedNodeDefs() {
        if (unnamedItemDefs.size() == 0) {
            return QNodeDefinition.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            QItemDefinition qDef = (QItemDefinition) iter.next();
            if (qDef.definesNode()) {
                defs.add(qDef);
            }
        }
        if (defs.size() == 0) {
            return QNodeDefinition.EMPTY_ARRAY;
        }
        return (QNodeDefinition[]) defs.toArray(new QNodeDefinition[defs.size()]);
    }

    private QPropertyDefinition[] getNamedPropDefs(QName name) {
        List list = (List) namedItemDefs.get(name);
        if (list == null || list.size() == 0) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(list.size());
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            QItemDefinition qDef = (QItemDefinition) iter.next();
            if (!qDef.definesNode()) {
                defs.add(qDef);
            }
        }
        if (defs.size() == 0) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        return (QPropertyDefinition[]) defs.toArray(new QPropertyDefinition[defs.size()]);
    }

    private QPropertyDefinition[] getUnnamedPropDefs() {
        if (unnamedItemDefs.size() == 0) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        ArrayList defs = new ArrayList(unnamedItemDefs.size());
        Iterator iter = unnamedItemDefs.iterator();
        while (iter.hasNext()) {
            QItemDefinition qDef = (QItemDefinition) iter.next();
            if (!qDef.definesNode()) {
                defs.add(qDef);
            }
        }
        if (defs.size() == 0) {
            return QPropertyDefinition.EMPTY_ARRAY;
        }
        return (QPropertyDefinition[]) defs.toArray(new QPropertyDefinition[defs.size()]);
    }

    private QPropertyDefinition getMatchingPropDef(QPropertyDefinition[] defs, int type) {
        QPropertyDefinition match = null;
        for (int i = 0; i < defs.length; i++) {
            QItemDefinition qDef = defs[i];
            if (!qDef.definesNode()) {
                QPropertyDefinition pd = (QPropertyDefinition) qDef;
                int reqType = pd.getRequiredType();
                // match type
                if (reqType == PropertyType.UNDEFINED
                        || type == PropertyType.UNDEFINED
                        || reqType == type) {
                    if (match == null) {
                        match = pd;
                    } else {
                        // check if this definition is a better match than
                        // the one we've already got
                        if (match.getRequiredType() != pd.getRequiredType()) {
                            if (match.getRequiredType() == PropertyType.UNDEFINED) {
                                // found better match
                                match = pd;
                            }
                        } else {
                            if (match.isMultiple() && !pd.isMultiple()) {
                                // found better match
                                match = pd;
                            }
                        }
                    }
                    if (match.getRequiredType() != PropertyType.UNDEFINED
                            && !match.isMultiple()) {
                        // found best possible match, get outta here
                        return match;
                    }
                }
            }
        }
        return match;
    }

    private QPropertyDefinition getMatchingPropDef(QPropertyDefinition[] defs, int type,
                                       boolean multiValued) {
        QPropertyDefinition match = null;
        for (int i = 0; i < defs.length; i++) {
            QItemDefinition qDef = defs[i];
            if (!qDef.definesNode()) {
                QPropertyDefinition pd = (QPropertyDefinition) qDef;
                int reqType = pd.getRequiredType();
                // match type
                if (reqType == PropertyType.UNDEFINED
                        || type == PropertyType.UNDEFINED
                        || reqType == type) {
                    // match multiValued flag
                    if (multiValued == pd.isMultiple()) {
                        // found match
                        if (pd.getRequiredType() != PropertyType.UNDEFINED) {
                            // found best possible match, get outta here
                            return pd;
                        } else {
                            if (match == null) {
                                match = pd;
                            }
                        }
                    }
                }
            }
        }
        return match;
    }

    private QPropertyDefinition[] getMatchingPropDefs(QPropertyDefinition[] defs, int type, boolean multiValued) {
        List result = Collections.EMPTY_LIST;
    
        for (int i = 0; i < defs.length; i++) {
            QItemDefinition qDef = defs[i];
            if (!qDef.definesNode()) {
                QPropertyDefinition pd = (QPropertyDefinition)qDef;
                int reqType = pd.getRequiredType();
                // match type
                if (reqType == PropertyType.UNDEFINED || type == PropertyType.UNDEFINED || reqType == type) {
                    // match multiValued flag
                    if (multiValued == pd.isMultiple()) {
                        // found match
                        if (result.isEmpty()) {
                            result = Collections.singletonList(pd);
                        }
                        else {
                            if (result.size() == 1) {
                                result = new ArrayList(result);
                            }
                            result.add(pd);
                        }
                    }
                }
            }
        }
        return (QPropertyDefinition[])result.toArray(QPropertyDefinition.EMPTY_ARRAY);
    }

    /**
     * Merges another <code>EffectiveNodeType</code> with this one.
     * Checks for merge conflicts.
     *
     * @param other
     * @return
     * @throws NodeTypeConflictException
     */
    EffectiveNodeTypeImpl merge(EffectiveNodeTypeImpl other)
            throws NodeTypeConflictException {
        // create a clone of this instance and perform the merge on
        // the 'clone' to avoid a potentially inconsistant state
        // of this instance if an exception is thrown during
        // the merge.
        EffectiveNodeTypeImpl copy = (EffectiveNodeTypeImpl) clone();
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
    private synchronized void internalMerge(EffectiveNodeTypeImpl other, boolean supertype)
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
        QItemDefinition[] defs = other.getNamedItemDefs();
        for (int i = 0; i < defs.length; i++) {
            QItemDefinition qDef = defs[i];
            if (includesNodeType(qDef.getDeclaringNodeType())) {
                // ignore redundant definitions
                continue;
            }
            QName name = qDef.getQName();
            List existingDefs = (List) namedItemDefs.get(name);
            if (existingDefs != null) {
                if (existingDefs.size() > 0) {
                    // there already exists at least one definition with that name
                    for (int j = 0; j < existingDefs.size(); j++) {
                        QItemDefinition qItemDef = (QItemDefinition) existingDefs.get(j);
                        // make sure none of them is auto-create
                        if (qDef.isAutoCreated() || qItemDef.isAutoCreated()) {
                            // conflict
                            String msg = "The item definition for '" + name
                                    + "' in node type '"
                                    + qDef.getDeclaringNodeType()
                                    + "' conflicts with node type '"
                                    + qItemDef.getDeclaringNodeType()
                                    + "': name collision with auto-create definition";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                        // check ambiguous definitions
                        if (qDef.definesNode() == qItemDef.definesNode()) {
                            if (!qDef.definesNode()) {
                                // property definition
                                QPropertyDefinition pd = (QPropertyDefinition) qDef;
                                QPropertyDefinition epd = (QPropertyDefinition) qItemDef;
                                // compare type & multiValued flag
                                if (pd.getRequiredType() == epd.getRequiredType()
                                        && pd.isMultiple() == epd.isMultiple()) {
                                    // conflict
                                    String msg = "The property definition for '"
                                            + name + "' in node type '"
                                            + qDef.getDeclaringNodeType()
                                            + "' conflicts with node type '"
                                            + qItemDef.getDeclaringNodeType()
                                            + "': ambiguous property definition";
                                    log.debug(msg);
                                    throw new NodeTypeConflictException(msg);
                                }
                            } else {
                                // child node definition
                                // conflict
                                String msg = "The child node definition for '"
                                        + name + "' in node type '"
                                        + qDef.getDeclaringNodeType()
                                        + "' conflicts with node type '"
                                        + qItemDef.getDeclaringNodeType()
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
            existingDefs.add(qDef);
        }

        // residual item definitions
        defs = other.getUnnamedItemDefs();
        for (int i = 0; i < defs.length; i++) {
            QItemDefinition qDef = defs[i];
            if (includesNodeType(qDef.getDeclaringNodeType())) {
                // ignore redundant definitions
                continue;
            }
            Iterator iter = unnamedItemDefs.iterator();
            while (iter.hasNext()) {
                QItemDefinition existing = (QItemDefinition) iter.next();
                // compare with existing definition
                if (qDef.definesNode() == existing.definesNode()) {
                    if (!qDef.definesNode()) {
                        // property definition
                        QPropertyDefinition pd = (QPropertyDefinition) qDef;
                        QPropertyDefinition epd = (QPropertyDefinition) existing;
                        // compare type & multiValued flag
                        if (pd.getRequiredType() == epd.getRequiredType()
                                && pd.isMultiple() == epd.isMultiple()
                                && pd.getOnParentVersion() == epd.getOnParentVersion()) {
                            // conflict
                            // TODO: need to take more aspects into account
                            // TODO: getMatchingPropDef needs to check this as well
                            String msg = "A property definition in node type '"
                                    + qDef.getDeclaringNodeType()
                                    + "' conflicts with node type '"
                                    + existing.getDeclaringNodeType()
                                    + "': ambiguous residual property definition";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                    } else {
                        // child node definition
                        QNodeDefinition nd = (QNodeDefinition) qDef;
                        QNodeDefinition end = (QNodeDefinition) existing;
                        // compare required & default primary types
                        if (Arrays.equals(nd.getRequiredPrimaryTypes(), end.getRequiredPrimaryTypes())
                                && (nd.getDefaultPrimaryType() == null
                                ? end.getDefaultPrimaryType() == null
                                : nd.getDefaultPrimaryType().equals(end.getDefaultPrimaryType()))) {
                            // conflict
                            String msg = "A child node definition in node type '"
                                    + qDef.getDeclaringNodeType()
                                    + "' conflicts with node type '"
                                    + existing.getDeclaringNodeType()
                                    + "': ambiguous residual child node definition";
                            log.debug(msg);
                            throw new NodeTypeConflictException(msg);
                        }
                    }
                }
            }
            unnamedItemDefs.add(qDef);
        }
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
        EffectiveNodeTypeImpl clone = new EffectiveNodeTypeImpl();

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
