/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.core.nodetype;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.jcr.core.*;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.*;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * A <code>NodeTypeImpl</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.36 $, $Date: 2004/08/30 11:13:46 $
 */
public class NodeTypeImpl implements NodeType {

    private static Logger log = Logger.getLogger(NodeTypeImpl.class);

    private final NodeTypeDef ntd;
    private final EffectiveNodeType ent;
    private final NodeTypeManagerImpl ntMgr;
    // namespace resolver used to translate qualified names to JCR names
    private final NamespaceResolver nsResolver;

    /**
     * Package private constructor
     * <p/>
     * Creates a valid node type instance.
     * We assume that the node type definition is valid and all referenced
     * node types (supertypes, required node types etc.) do exist and are valid.
     *
     * @param ent        the effective (i.e. merged and resolved) node type representation
     * @param ntd        the definition of this node type
     * @param ntMgr      the node type manager associated with this node type
     * @param nsResolver namespace resolver
     */
    NodeTypeImpl(EffectiveNodeType ent, NodeTypeDef ntd, NodeTypeManagerImpl ntMgr, NamespaceResolver nsResolver) {
	this.ent = ent;
	this.ntMgr = ntMgr;
	this.nsResolver = nsResolver;
	try {
	    // store a clone of the definition
	    this.ntd = (NodeTypeDef) ntd.clone();
	} catch (CloneNotSupportedException e) {
	    // should never get here
	    log.fatal("internal error", e);
	    throw new InternalError(e.getMessage());
	}
    }

    /**
     * Returns the applicable child node definition for a child node with the
     * specified name.
     *
     * @param nodeName
     * @return
     * @throws RepositoryException if no applicable child node definition
     *                             could be found
     */
    public NodeDefImpl getApplicableChildNodeDef(QName nodeName)
	    throws RepositoryException {
	return getApplicableChildNodeDef(nodeName, null);
    }

    /**
     * Returns the applicable child node definition for a child node with the
     * specified name and node type.
     *
     * @param nodeName
     * @param nodeTypeName
     * @return
     * @throws RepositoryException if no applicable child node definition
     *                             could be found
     */
    public NodeDefImpl getApplicableChildNodeDef(QName nodeName, QName nodeTypeName)
	    throws RepositoryException {
	return new NodeDefImpl(ent.getApplicableChildNodeDef(nodeName, nodeTypeName),
		ntMgr, nsResolver);
    }

    /**
     * Returns the applicable property definition for a property with the
     * specified name and type.
     *
     * @param propertyName
     * @param type
     * @param multiValued
     * @return
     * @throws RepositoryException if no applicable property definition
     *                             could be found
     */
    public PropertyDefImpl getApplicablePropertyDef(QName propertyName, int type,
						    boolean multiValued)
	    throws RepositoryException {
	return new PropertyDefImpl(ent.getApplicablePropertyDef(propertyName, type, multiValued),
		ntMgr, nsResolver);
    }

    /**
     * Checks if this node type is directly or indirectly derived from the
     * specified node type.
     *
     * @param nodeTypeName
     * @return true if this node type is directly or indirectly derived from the
     *         specified node type, otherwise false.
     */
    public boolean isDerivedFrom(QName nodeTypeName) {
	return !nodeTypeName.equals(ntd.getName()) && ent.includesNodeType(nodeTypeName);
    }

    /**
     * Returns the definition of this node type.
     *
     * @return the definition of this node type
     */
    public NodeTypeDef getDefinition() {
	try {
	    // return a clone of the definition
	    return (NodeTypeDef) ntd.clone();
	} catch (CloneNotSupportedException e) {
	    // should never get here
	    log.fatal("internal error", e);
	    throw new InternalError(e.getMessage());
	}
    }

    /**
     * Returns an array containing only those child node definitions of this
     * node type (including the child node definitions inherited from supertypes
     * of this node type) where <code>{@link NodeDef#isAutoCreate()}</code>
     * returns <code>true</code>.
     *
     * @return an array of child node definitions.
     * @see NodeDef#isAutoCreate
     */
    public NodeDef[] getAutoCreateNodeDefs() {
	ChildNodeDef[] cnda = ent.getAutoCreateNodeDefs();
	NodeDef[] nodeDefs = new NodeDef[cnda.length];
	for (int i = 0; i < cnda.length; i++) {
	    nodeDefs[i] = new NodeDefImpl(cnda[i], ntMgr, nsResolver);
	}
	return nodeDefs;
    }

    /**
     * Returns an array containing only those property definitions of this
     * node type (including the property definitions inherited from supertypes
     * of this node type) where <code>{@link PropertyDef#isAutoCreate()}</code>
     * returns <code>true</code>.
     *
     * @return an array of property definitions.
     * @see PropertyDef#isAutoCreate
     */
    public PropertyDef[] getAutoCreatePropertyDefs() {
	PropDef[] pda = ent.getAutoCreatePropDefs();
	PropertyDef[] propDefs = new PropertyDef[pda.length];
	for (int i = 0; i < pda.length; i++) {
	    propDefs[i] = new PropertyDefImpl(pda[i], ntMgr, nsResolver);
	}
	return propDefs;
    }

    /**
     * Returns an array containing only those property definitions of this
     * node type (including the property definitions inherited from supertypes
     * of this node type) where <code>{@link PropertyDef#isMandatory()}</code>
     * returns <code>true</code>.
     *
     * @return an array of property definitions.
     * @see PropertyDef#isMandatory
     */
    public PropertyDef[] getMandatoryPropertyDefs() {
	PropDef[] pda = ent.getMandatoryPropDefs();
	PropertyDef[] propDefs = new PropertyDef[pda.length];
	for (int i = 0; i < pda.length; i++) {
	    propDefs[i] = new PropertyDefImpl(pda[i], ntMgr, nsResolver);
	}
	return propDefs;
    }

    /**
     * Returns an array containing only those child node definitions of this
     * node type (including the child node definitions inherited from supertypes
     * of this node type) where <code>{@link NodeDef#isMandatory()}</code>
     * returns <code>true</code>.
     *
     * @return an array of child node definitions.
     * @see NodeDef#isMandatory
     */
    public NodeDef[] getMandatoryNodeDefs() {
	ChildNodeDef[] cnda = ent.getMandatoryNodeDefs();
	NodeDef[] nodeDefs = new NodeDef[cnda.length];
	for (int i = 0; i < cnda.length; i++) {
	    nodeDefs[i] = new NodeDefImpl(cnda[i], ntMgr, nsResolver);
	}
	return nodeDefs;
    }

    /**
     * Tests if the value constraints defined in the property definition
     * <code>def</code> are satisfied by the the specified <code>values</code>.
     * <p/>
     * Note that the <i>protected</i> flag is not checked.
     *
     * @param def    The definiton of the property
     * @param values An array of <code>InternalValue</code> objects.
     * @throws ValueFormatException
     * @throws ConstraintViolationException
     * @throws RepositoryException
     */
    public static void checkSetPropertyValueConstraints(PropertyDefImpl def, InternalValue[] values)
	    throws ValueFormatException, ConstraintViolationException, RepositoryException {
	EffectiveNodeType.checkSetPropertyValueConstraints(def.unwrap(), values);
    }

    /**
     * Returns the 'internal', i.e. the fully qualified name.
     *
     * @return the qualified name
     */
    public QName getQName() {
	return ntd.getName();
    }

    /**
     * Returns all <i>inherited</i> supertypes of this node type.
     *
     * @return an array of <code>NodeType</code> objects.
     * @see #getSupertypes
     * @see #getDeclaredSupertypes
     */
    public NodeType[] getInheritedSupertypes() {
	// declared supertypes
	QName[] ntNames = ntd.getSupertypes();
	HashSet declared = new HashSet();
	for (int i = 0; i < ntNames.length; i++) {
	    declared.add(ntNames[i]);
	}
	// all supertypes
	ntNames = ent.getInheritedNodeTypes();

	// filter from all supertypes those that are not declared
	ArrayList inherited = new ArrayList();
	for (int i = 0; i < ntNames.length; i++) {
	    if (!declared.contains(ntNames[i])) {
		try {
		    inherited.add(ntMgr.getNodeType(ntNames[i]));
		} catch (NoSuchNodeTypeException e) {
		    // should never get here
		    log.error("undefined supertype", e);
		    return new NodeType[0];
		}
	    }
	}

	return (NodeType[]) inherited.toArray(new NodeType[inherited.size()]);
    }

    //-------------------------------------------------------------< NodeType >
    /**
     * @see NodeType#getName
     */
    public String getName() {
	try {
	    return ntd.getName().toJCRName(nsResolver);
	} catch (NoPrefixDeclaredException npde) {
	    // should never get here
	    log.error("encountered unregistered namespace in node type name", npde);
	    return ntd.getName().toString();
	}
    }

    /**
     * @see NodeType#isMixin
     */
    public boolean isMixin() {
	return ntd.isMixin();
    }

    /**
     * @see NodeType#hasOrderableChildNodes
     */
    public boolean hasOrderableChildNodes() {
	return ntd.hasOrderableChildNodes();
    }

    /**
     * @see NodeType#getSupertypes
     */
    public NodeType[] getSupertypes() {
	QName[] ntNames = ent.getInheritedNodeTypes();
	NodeType[] supertypes = new NodeType[ntNames.length];
	for (int i = 0; i < ntNames.length; i++) {
	    try {
		supertypes[i] = ntMgr.getNodeType(ntNames[i]);
	    } catch (NoSuchNodeTypeException e) {
		// should never get here
		log.error("undefined supertype", e);
		return new NodeType[0];
	    }
	}
	return supertypes;
    }

    /**
     * @see NodeType#getChildNodeDefs
     */
    public NodeDef[] getChildNodeDefs() {
	ChildNodeDef[] cnda = ent.getAllNodeDefs();
	NodeDef[] nodeDefs = new NodeDef[cnda.length];
	for (int i = 0; i < cnda.length; i++) {
	    nodeDefs[i] = new NodeDefImpl(cnda[i], ntMgr, nsResolver);
	}
	return nodeDefs;
    }

    /**
     * @see NodeType#getPropertyDefs
     */
    public PropertyDef[] getPropertyDefs() {
	PropDef[] pda = ent.getAllPropDefs();
	PropertyDef[] propDefs = new PropertyDef[pda.length];
	for (int i = 0; i < pda.length; i++) {
	    propDefs[i] = new PropertyDefImpl(pda[i], ntMgr, nsResolver);
	}
	return propDefs;
    }

    /**
     * @see NodeType#getDeclaredSupertypes
     */
    public NodeType[] getDeclaredSupertypes() {
	QName[] ntNames = ntd.getSupertypes();
	NodeType[] supertypes = new NodeType[ntNames.length];
	for (int i = 0; i < ntNames.length; i++) {
	    try {
		supertypes[i] = ntMgr.getNodeType(ntNames[i]);
	    } catch (NoSuchNodeTypeException e) {
		// should never get here
		log.error("undefined supertype", e);
		return new NodeType[0];
	    }
	}
	return supertypes;
    }

    /**
     * @see NodeType#getDeclaredChildNodeDefs
     */
    public NodeDef[] getDeclaredChildNodeDefs() {
	ChildNodeDef[] cnda = ntd.getChildNodeDefs();
	NodeDef[] nodeDefs = new NodeDef[cnda.length];
	for (int i = 0; i < cnda.length; i++) {
	    nodeDefs[i] = new NodeDefImpl(cnda[i], ntMgr, nsResolver);
	}
	return nodeDefs;
    }

    /**
     * @see NodeType#canSetProperty(String, Value)
     */
    public boolean canSetProperty(String propertyName, Value value) {
	if (value == null) {
	    // setting a property to null is equivalent of removing it
	    return canRemoveItem(propertyName);
	}
	try {
	    QName name = QName.fromJCRName(propertyName, nsResolver);
	    int type = (value == null) ? PropertyType.UNDEFINED : value.getType();
	    PropertyDefImpl def = getApplicablePropertyDef(name, type, false);
	    if (def.isProtected()) {
		return false;
	    }
	    if (def.isMultiple()) {
		return false;
	    }
	    InternalValue internalValue = InternalValue.create(value, nsResolver);
	    checkSetPropertyValueConstraints(def, new InternalValue[]{internalValue});
	    return true;
	} catch (BaseException be) {
	    // implementation specific exception, fall through
	} catch (RepositoryException re) {
	    // fall through
	}
	return false;
    }

    /**
     * @see NodeType#canSetProperty(String, Value[])
     */
    public boolean canSetProperty(String propertyName, Value values[]) {
	if (values == null) {
	    // setting a property to null is equivalent of removing it
	    return canRemoveItem(propertyName);
	}
	try {
	    QName name = QName.fromJCRName(propertyName, nsResolver);
	    int type = (values == null || values.length == 0) ? PropertyType.UNDEFINED : values[0].getType();
	    PropertyDefImpl def = getApplicablePropertyDef(name, type, true);
	    if (def.isProtected()) {
		return false;
	    }
	    if (!def.isMultiple()) {
		return false;
	    }
	    ArrayList list = new ArrayList();
	    // convert values and compact array (purge null entries)
	    for (int i = 0; i < values.length; i++) {
		if (values[i] != null) {
		    InternalValue internalValue = InternalValue.create(values[i], nsResolver);
		    list.add(internalValue);
		}
	    }
	    InternalValue[] internalValues = (InternalValue[]) list.toArray(new InternalValue[list.size()]);
	    checkSetPropertyValueConstraints(def, internalValues);
	    return true;
	} catch (BaseException be) {
	    // implementation specific exception, fall through
	} catch (RepositoryException re) {
	    // fall through
	}
	return false;
    }

    /**
     * @see NodeType#canAddChildNode(String)
     */
    public boolean canAddChildNode(String childNodeName) {
	try {
	    ent.checkAddNodeConstraints(QName.fromJCRName(childNodeName, nsResolver));
	    return true;
	} catch (BaseException be) {
	    // implementation specific exception, fall through
	} catch (RepositoryException re) {
	    // fall through
	}
	return false;
    }

    /**
     * @see NodeType#canAddChildNode(String, String)
     */
    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
	try {
	    ent.checkAddNodeConstraints(QName.fromJCRName(childNodeName, nsResolver), QName.fromJCRName(nodeTypeName, nsResolver));
	    return true;
	} catch (BaseException be) {
	    // implementation specific exception, fall through
	} catch (RepositoryException re) {
	    // fall through
	}
	return false;
    }

    /**
     * @see NodeType#canRemoveItem(String)
     */
    public boolean canRemoveItem(String itemName) {
	try {
	    ent.checkRemoveItemConstraints(QName.fromJCRName(itemName, nsResolver));
	    return true;
	} catch (BaseException be) {
	    // implementation specific exception, fall through
	} catch (RepositoryException re) {
	    // fall through
	}
	return false;
    }

    /**
     * @see NodeType#getDeclaredPropertyDefs
     */
    public PropertyDef[] getDeclaredPropertyDefs() {
	PropDef[] pda = ntd.getPropertyDefs();
	PropertyDef[] propDefs = new PropertyDef[pda.length];
	for (int i = 0; i < pda.length; i++) {
	    propDefs[i] = new PropertyDefImpl(pda[i], ntMgr, nsResolver);
	}
	return propDefs;
    }
}
