/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * <code>NodeTypeDefStore</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.7 $, $Date: 2004/08/30 11:13:46 $
 */
class NodeTypeDefStore {
    private static Logger log = Logger.getLogger(NodeTypeDefStore.class);

    private static final String ROOT_ELEMENT_NAME = "nodeTypes";
    private static final String NODETYPE_ELEMENT_NAME = "nodeType";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String MIXIN_ATTRIBUTE = "mixin";
    private static final String ORDERABLECHILDNODES_ATTRIBUTE = "orderableChildNodes";
    private static final String SUPERTYPES_ATTRIBUTE = "supertypes";
    private static final String PROPERTYDEF_ELEMENT_NAME = "propertyDef";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String VALUECONSTRAINT_ATTRIBUTE = "valueConstraint";
    private static final String DEFAULTVALUES_ATTRIBUTE = "defaultValues";
    private static final String AUTOCREATE_ATTRIBUTE = "autoCreate";
    private static final String MANDATORY_ATTRIBUTE = "mandatory";
    private static final String PROTECTED_ATTRIBUTE = "protected";
    private static final String PRIMARYITEM_ATTRIBUTE = "primaryItem";
    private static final String MULTIPLE_ATTRIBUTE = "multiple";
    private static final String SAMENAMESIBS_ATTRIBUTE = "sameNameSibs";
    private static final String ONPARENTVERSION_ATTRIBUTE = "onParentVersion";
    private static final String CHILDNODEDEF_ELEMENT_NAME = "childNodeDef";
    private static final String REQUIREDPRIMARYTYPES_ATTRIBUTE = "requiredPrimaryTypes";
    private static final String DEFAULTPRIMARYTYPE_ATTRIBUTE = "defaultPrimaryType";

    // map of node type names and node type definitions
    private HashMap ntDefs;

    /**
     * Empty default constructor.
     */
    NodeTypeDefStore() {
	ntDefs = new HashMap();
    }

    /**
     * @param in
     * @throws IOException
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    void load(InputStream in)
	    throws IOException, InvalidNodeTypeDefException, RepositoryException {
	SAXBuilder builder = new SAXBuilder();
	Element root = null;
	try {
	    Document doc = builder.build(in);
	    root = doc.getRootElement();
	} catch (JDOMException jde) {
	    String error = "internal error: failed to parse persistent node type definitions";
	    log.error(error);
	    throw new RepositoryException(error);
	}
	// read namespace declarations of root element
	Iterator nsIter = root.getAdditionalNamespaces().iterator();
	final HashMap prefixToURI = new HashMap();
	final HashMap uriToPrefix = new HashMap();
	while (nsIter.hasNext()) {
	    Namespace ns = (Namespace) nsIter.next();
	    prefixToURI.put(ns.getPrefix(), ns.getURI());
	    uriToPrefix.put(ns.getURI(), ns.getPrefix());
	}
	// add default namespace (empty uri)
	prefixToURI.put(NamespaceRegistryImpl.NS_EMPTY_PREFIX, NamespaceRegistryImpl.NS_DEFAULT_URI);
	uriToPrefix.put(NamespaceRegistryImpl.NS_DEFAULT_URI, NamespaceRegistryImpl.NS_EMPTY_PREFIX);

	NamespaceResolver nsResolver = new NamespaceResolver() {
	    public String getURI(String prefix) throws NamespaceException {
		if (!prefixToURI.containsKey(prefix)) {
		    throw new NamespaceException(prefix + ": unknown prefix");
		}
		return (String) prefixToURI.get(prefix);
	    }

	    public String getPrefix(String uri) throws NamespaceException {
		if (!uriToPrefix.containsKey(uri)) {
		    throw new NamespaceException(uri + ": unknown URI");
		}
		return (String) uriToPrefix.get(uri);
	    }
	};

	// read definitions
	Iterator iter = root.getChildren(NODETYPE_ELEMENT_NAME).iterator();
	while (iter.hasNext()) {
	    NodeTypeDef ntDef = readDef((Element) iter.next(), nsResolver);
	    add(ntDef);
	}
    }

    /**
     * @param out
     * @param nsReg
     * @throws IOException
     * @throws RepositoryException
     */
    void store(OutputStream out, NamespaceRegistryImpl nsReg)
	    throws IOException, RepositoryException {
	Element root = new Element(ROOT_ELEMENT_NAME);

	// namespace declarations
	String[] prefixes = nsReg.getPrefixes();
	for (int i = 0; i < prefixes.length; i++) {
	    String prefix = prefixes[i];
	    if ("".equals(prefix)) {
		continue;
	    }
	    String uri = nsReg.getURI(prefix);
	    root.addNamespaceDeclaration(Namespace.getNamespace(prefix, uri));
	}

	// node type definitions
	Iterator iter = all().iterator();
	while (iter.hasNext()) {
	    NodeTypeDef ntd = (NodeTypeDef) iter.next();
	    Element ntElem = new Element(NODETYPE_ELEMENT_NAME);
	    writeDef(ntd, ntElem, nsReg);
	    root.addContent(ntElem);
	}

	XMLOutputter serializer = new XMLOutputter("\t", true);
	serializer.output(new Document(root), out);
    }

    /**
     * @param ntd
     */
    void add(NodeTypeDef ntd) {
	ntDefs.put(ntd.getName(), ntd);
    }

    /**
     * @param name
     * @return
     */
    boolean remove(QName name) {
	return ntDefs.remove(name) != null ? true : false;
    }

    /**
     *
     */
    void removeAll() {
	ntDefs.clear();
    }

    /**
     * @param name
     * @return
     */
    boolean contains(QName name) {
	return ntDefs.containsKey(name);
    }

    /**
     * @param name
     * @return
     */
    NodeTypeDef get(QName name) {
	return (NodeTypeDef) ntDefs.get(name);
    }

    /**
     * @return
     */
    Collection all() {
	return Collections.unmodifiableCollection(ntDefs.values());
    }

    //------------------------------------------------------< private methods >
    private NodeTypeDef readDef(Element ntElem, NamespaceResolver nsResolver)
	    throws InvalidNodeTypeDefException {
	String sntName = ntElem.getAttributeValue(NAME_ATTRIBUTE);
	NodeTypeDef ntDef = new NodeTypeDef();
	// name
	QName qntName;
	try {
	    qntName = QName.fromJCRName(sntName, nsResolver);
	} catch (BaseException e) {
	    throw new InvalidNodeTypeDefException("invalid node type name:" + sntName, e);
	}
	ntDef.setName(qntName);

	// supertypes
	String supertypes = ntElem.getAttributeValue(SUPERTYPES_ATTRIBUTE);
	if (supertypes != null && supertypes.length() > 0) {
	    String[] sta = supertypes.split(",\\s*");
	    QName[] qNames = new QName[sta.length];
	    for (int i = 0; i < sta.length; i++) {
		try {
		    qNames[i] = QName.fromJCRName(sta[i], nsResolver);
		} catch (BaseException e) {
		    throw new InvalidNodeTypeDefException("invalid supertype name:" + sta[i], e);
		}
	    }
	    if (sta.length > 0) {
		ntDef.setSupertypes(qNames);
	    }
	}
	// mixin
	String mixin = ntElem.getAttributeValue(MIXIN_ATTRIBUTE);
	if (mixin != null && mixin.length() > 0) {
	    ntDef.setMixin(Boolean.valueOf(mixin).booleanValue());
	}
	// orderableChildNodes
	String orderableChildNodes = ntElem.getAttributeValue(ORDERABLECHILDNODES_ATTRIBUTE);
	if (orderableChildNodes != null && orderableChildNodes.length() > 0) {
	    ntDef.setOrderableChildNodes(Boolean.valueOf(orderableChildNodes).booleanValue());
	}

	// property definitions
	ArrayList list = new ArrayList();
	Iterator iter = ntElem.getChildren(PROPERTYDEF_ELEMENT_NAME).iterator();
	while (iter.hasNext()) {
	    Element elem = (Element) iter.next();
	    PropDef pd = new PropDef();
	    // declaring node type
	    pd.setDeclaringNodeType(qntName);
	    // name
	    String propName = elem.getAttributeValue(NAME_ATTRIBUTE);
	    if (propName != null && propName.length() > 0) {
		try {
		    pd.setName(QName.fromJCRName(propName, nsResolver));
		} catch (BaseException e) {
		    throw new InvalidNodeTypeDefException("invalid property name:" + propName, e);
		}
	    }
	    // type
	    String typeName = elem.getAttributeValue(TYPE_ATTRIBUTE);
	    int type = PropertyType.UNDEFINED;
	    if (typeName != null && typeName.length() > 0) {
		try {
		    type = PropertyType.valueFromName(typeName);
		    pd.setRequiredType(type);
		} catch (IllegalArgumentException e) {
		    String error = "invalid serialized node type definition: invalid type " + typeName;
		    log.error(error);
		    throw new InvalidNodeTypeDefException(error);
		}
	    }
	    // valueConstraint
	    String valueConstraint = elem.getAttributeValue(VALUECONSTRAINT_ATTRIBUTE);
	    if (valueConstraint != null && valueConstraint.length() > 0) {
		try {
		    pd.setValueConstraint(ValueConstraint.create(type, valueConstraint));
		} catch (InvalidConstraintException e) {
		    String error = "invalid serialized node type definition: invalid constraint " + valueConstraint;
		    log.error(error, e);
		    throw new InvalidNodeTypeDefException(error, e);
		}
	    }
	    // defaultValues
	    // @todo provide escaping for separator character within single value or change xml representation for defaultValues
	    String defaultValues = elem.getAttributeValue(DEFAULTVALUES_ATTRIBUTE);
	    if (defaultValues != null && defaultValues.length() > 0) {
		int defValType = (type == PropertyType.UNDEFINED) ? PropertyType.STRING : type;
		String[] dva = defaultValues.split(",\\s*");
		InternalValue[] defVals = new InternalValue[dva.length];
		for (int i = 0; i < dva.length; i++) {
		    defVals[i] = InternalValue.valueOf(dva[i], defValType);
		}
		if (defVals.length > 0) {
		    pd.setDefaultValues(defVals);
		}
	    }
	    // autoCreate
	    String autoCreate = elem.getAttributeValue(AUTOCREATE_ATTRIBUTE);
	    if (autoCreate != null && autoCreate.length() > 0) {
		pd.setAutoCreate(Boolean.valueOf(autoCreate).booleanValue());
	    }
	    // mandatory
	    String mandatory = elem.getAttributeValue(MANDATORY_ATTRIBUTE);
	    if (mandatory != null && mandatory.length() > 0) {
		pd.setMandatory(Boolean.valueOf(mandatory).booleanValue());
	    }
	    // onParentVersion
	    String onVersion = elem.getAttributeValue(ONPARENTVERSION_ATTRIBUTE);
	    if (onVersion != null && onVersion.length() > 0) {
		try {
		    pd.setOnParentVersion(OnParentVersionAction.valueFromName(onVersion));
		} catch (IllegalArgumentException e) {
		    String error = "invalid serialized node type definition: invalid onVersion " + onVersion;
		    log.error(error);
		    throw new InvalidNodeTypeDefException(error);
		}
	    }
	    // protected
	    String writeProtected = elem.getAttributeValue(PROTECTED_ATTRIBUTE);
	    if (writeProtected != null && writeProtected.length() > 0) {
		pd.setProtected(Boolean.valueOf(writeProtected).booleanValue());
	    }
	    // primaryItem
	    String primaryItem = elem.getAttributeValue(PRIMARYITEM_ATTRIBUTE);
	    if (primaryItem != null && primaryItem.length() > 0) {
		pd.setPrimaryItem(Boolean.valueOf(primaryItem).booleanValue());
	    }
	    // multiple
	    String multiple = elem.getAttributeValue(MULTIPLE_ATTRIBUTE);
	    if (multiple != null && multiple.length() > 0) {
		pd.setMultiple(Boolean.valueOf(multiple).booleanValue());
	    }

	    list.add(pd);
	}
	if (!list.isEmpty()) {
	    ntDef.setPropertyDefs((PropDef[]) list.toArray(new PropDef[list.size()]));
	}

	// child-node definitions
	list.clear();
	iter = ntElem.getChildren(CHILDNODEDEF_ELEMENT_NAME).iterator();
	while (iter.hasNext()) {
	    Element elem = (Element) iter.next();
	    ChildNodeDef cnd = new ChildNodeDef();
	    // declaring node type
	    cnd.setDeclaringNodeType(qntName);
	    // name
	    String nodeName = elem.getAttributeValue(NAME_ATTRIBUTE);
	    if (nodeName != null && nodeName.length() > 0) {
		try {
		    cnd.setName(QName.fromJCRName(nodeName, nsResolver));
		} catch (BaseException e) {
		    throw new InvalidNodeTypeDefException("invalid child node name:" + nodeName, e);
		}
	    }
	    // requiredPrimaryTypes
	    String requiredPrimaryTypes = elem.getAttributeValue(REQUIREDPRIMARYTYPES_ATTRIBUTE);
	    if (requiredPrimaryTypes != null && requiredPrimaryTypes.length() > 0) {
		String[] sta = requiredPrimaryTypes.split(",\\s*");
		QName[] qNames = new QName[sta.length];
		for (int i = 0; i < sta.length; i++) {
		    try {
			qNames[i] = QName.fromJCRName(sta[i], nsResolver);
		    } catch (BaseException e) {
			throw new InvalidNodeTypeDefException("invalid requiredPrimaryType:" + sta[i], e);
		    }
		}
		if (sta.length > 0) {
		    cnd.setRequiredPrimaryTypes(qNames);
		}
	    }
	    // defaultPrimaryType
	    String defaultPrimaryType = elem.getAttributeValue(DEFAULTPRIMARYTYPE_ATTRIBUTE);
	    if (defaultPrimaryType != null && defaultPrimaryType.length() > 0) {
		try {
		    cnd.setDefaultPrimaryType(QName.fromJCRName(defaultPrimaryType, nsResolver));
		} catch (BaseException e) {
		    throw new InvalidNodeTypeDefException("invalid defaultPrimaryType:" + defaultPrimaryType, e);
		}
	    }
	    // autoCreate
	    String autoCreate = elem.getAttributeValue(AUTOCREATE_ATTRIBUTE);
	    if (autoCreate != null && autoCreate.length() > 0) {
		cnd.setAutoCreate(Boolean.valueOf(autoCreate).booleanValue());
	    }
	    // mandatory
	    String mandatory = elem.getAttributeValue(MANDATORY_ATTRIBUTE);
	    if (mandatory != null && mandatory.length() > 0) {
		cnd.setMandatory(Boolean.valueOf(mandatory).booleanValue());
	    }
	    // onParentVersion
	    String onVersion = elem.getAttributeValue(ONPARENTVERSION_ATTRIBUTE);
	    if (onVersion != null && onVersion.length() > 0) {
		try {
		    cnd.setOnParentVersion(OnParentVersionAction.valueFromName(onVersion));
		} catch (IllegalArgumentException e) {
		    String error = "invalid serialized node type definition: invalid onVersion " + onVersion;
		    log.error(error);
		    throw new InvalidNodeTypeDefException(error);
		}
	    }
	    // protected
	    String writeProtected = elem.getAttributeValue(PROTECTED_ATTRIBUTE);
	    if (writeProtected != null && writeProtected.length() > 0) {
		cnd.setProtected(Boolean.valueOf(writeProtected).booleanValue());
	    }
	    // primaryItem
	    String primaryItem = elem.getAttributeValue(PRIMARYITEM_ATTRIBUTE);
	    if (primaryItem != null && primaryItem.length() > 0) {
		cnd.setPrimaryItem(Boolean.valueOf(primaryItem).booleanValue());
	    }
	    // sameNameSibs
	    String sameNameSibs = elem.getAttributeValue(SAMENAMESIBS_ATTRIBUTE);
	    if (sameNameSibs != null && sameNameSibs.length() > 0) {
		cnd.setAllowSameNameSibs(Boolean.valueOf(sameNameSibs).booleanValue());
	    }

	    list.add(cnd);
	}
	if (!list.isEmpty()) {
	    ntDef.setChildNodeDefs((ChildNodeDef[]) list.toArray(new ChildNodeDef[list.size()]));
	}

	return ntDef;
    }

    private void writeDef(NodeTypeDef ntd, Element ntElem, NamespaceResolver nsResolver)
	    throws RepositoryException {
	try {
	    // name
	    ntElem.setAttribute(NAME_ATTRIBUTE, ntd.getName().toJCRName(nsResolver));
	    // supertypes
	    StringBuffer supertypes = new StringBuffer();
	    QName[] qNames = ntd.getSupertypes();
	    for (int i = 0; i < qNames.length; i++) {
		if (supertypes.length() > 0) {
		    supertypes.append(",");
		}
		supertypes.append(qNames[i].toJCRName(nsResolver));
	    }
	    ntElem.setAttribute(SUPERTYPES_ATTRIBUTE, supertypes.toString());
	    // mixin
	    ntElem.setAttribute(MIXIN_ATTRIBUTE, Boolean.toString(ntd.isMixin()));
	    // orderableChildNodes
	    ntElem.setAttribute(ORDERABLECHILDNODES_ATTRIBUTE, Boolean.toString(ntd.hasOrderableChildNodes()));

	    // property definitions
	    PropDef[] pda = ntd.getPropertyDefs();
	    for (int i = 0; i < pda.length; i++) {
		PropDef pd = pda[i];
		Element elem = new Element(PROPERTYDEF_ELEMENT_NAME);
		ntElem.addContent(elem);

		// name
		String name = pd.getName() == null ? "" : pd.getName().toJCRName(nsResolver);
		elem.setAttribute(NAME_ATTRIBUTE, name);
		// type
		elem.setAttribute(TYPE_ATTRIBUTE, PropertyType.nameFromValue(pd.getRequiredType()));
		// valueConstraint
		String valueConstraint = pd.getValueConstraint() == null ? "" : pd.getValueConstraint().getDefinition();
		elem.setAttribute(VALUECONSTRAINT_ATTRIBUTE, valueConstraint);
		// defaultValues
		// @todo provide escaping for separator character within single value or change xml representation for defaultValues
		InternalValue[] defVals = pd.getDefaultValues();
		StringBuffer defaultValues = new StringBuffer();
		if (defVals != null) {
		    for (int n = 0; n < defVals.length; n++) {
			if (defaultValues.length() > 0) {
			    defaultValues.append(",");
			}
			defaultValues.append(defVals[n].toString());
		    }
		}

		elem.setAttribute(DEFAULTVALUES_ATTRIBUTE, defaultValues.toString());
		// autoCreate
		String autoCreate = elem.getAttributeValue(AUTOCREATE_ATTRIBUTE);
		if (autoCreate != null && autoCreate.length() > 0) {
		    pd.setAutoCreate(Boolean.valueOf(autoCreate).booleanValue());
		}
		// mandatory
		elem.setAttribute(MANDATORY_ATTRIBUTE, Boolean.toString(pd.isMandatory()));
		// onParentVersion
		elem.setAttribute(ONPARENTVERSION_ATTRIBUTE, OnParentVersionAction.nameFromValue(pd.getOnParentVersion()));
		// protected
		elem.setAttribute(PROTECTED_ATTRIBUTE, Boolean.toString(pd.isProtected()));
		// primaryItem
		elem.setAttribute(PRIMARYITEM_ATTRIBUTE, Boolean.toString(pd.isPrimaryItem()));
		// multiple
		elem.setAttribute(MULTIPLE_ATTRIBUTE, Boolean.toString(pd.isMultiple()));
	    }

	    // child-node definitions
	    ChildNodeDef[] nda = ntd.getChildNodeDefs();
	    for (int i = 0; i < nda.length; i++) {
		ChildNodeDef nd = nda[i];
		Element elem = new Element(CHILDNODEDEF_ELEMENT_NAME);
		ntElem.addContent(elem);

		// name
		String name = nd.getName() == null ? "" : nd.getName().toJCRName(nsResolver);
		elem.setAttribute(NAME_ATTRIBUTE, name);
		// requiredPrimaryTypes
		StringBuffer requiredPrimaryTypes = new StringBuffer();
		qNames = nd.getRequiredPrimaryTypes();
		for (int j = 0; j < qNames.length; j++) {
		    if (requiredPrimaryTypes.length() > 0) {
			requiredPrimaryTypes.append(",");
		    }
		    requiredPrimaryTypes.append(qNames[j].toJCRName(nsResolver));
		}
		elem.setAttribute(REQUIREDPRIMARYTYPES_ATTRIBUTE, requiredPrimaryTypes.toString());
		// defaultPrimaryType
		String defaultPrimaryType = nd.getDefaultPrimaryType() == null ? "" : nd.getDefaultPrimaryType().toJCRName(nsResolver);
		elem.setAttribute(DEFAULTPRIMARYTYPE_ATTRIBUTE, defaultPrimaryType);
		// autoCreate
		elem.setAttribute(AUTOCREATE_ATTRIBUTE, Boolean.toString(nd.isAutoCreate()));
		// mandatory
		elem.setAttribute(MANDATORY_ATTRIBUTE, Boolean.toString(nd.isMandatory()));
		// onParentVersion
		elem.setAttribute(ONPARENTVERSION_ATTRIBUTE, OnParentVersionAction.nameFromValue(nd.getOnParentVersion()));
		// protected
		elem.setAttribute(PROTECTED_ATTRIBUTE, Boolean.toString(nd.isProtected()));
		// primaryItem
		elem.setAttribute(PRIMARYITEM_ATTRIBUTE, Boolean.toString(nd.isPrimaryItem()));
		// sameNameSibs
		elem.setAttribute(SAMENAMESIBS_ATTRIBUTE, Boolean.toString(nd.allowSameNameSibs()));
	    }
	} catch (NoPrefixDeclaredException npde) {
	    // should never get here...
	    String msg = "internal error: encountered unregistered namespace";
	    log.error(msg, npde);
	    throw new RepositoryException(msg, npde);
	}
    }
}
