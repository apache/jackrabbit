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
package org.apache.jackrabbit.core.nodetype;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.core.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.Filter;
import org.jdom.filter.ContentFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

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

    private static final String ROOT_ELEMENT = "nodeTypes";
    private static final String NODETYPE_ELEMENT = "nodeType";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String MIXIN_ATTRIBUTE = "mixin";
    private static final String ORDERABLECHILDNODES_ATTRIBUTE = "orderableChildNodes";
    private static final String SUPERTYPES_ELEMENT = "supertypes";
    private static final String SUPERTYPE_ELEMENT = "supertype";
    private static final String PROPERTYDEF_ELEMENT = "propertyDef";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String VALUECONSTRAINTS_ELEMENT = "valueConstraints";
    private static final String VALUECONSTRAINT_ELEMENT = "valueConstraint";
    private static final String DEFAULTVALUES_ELEMENT = "defaultValues";
    private static final String DEFAULTVALUE_ELEMENT = "defaultValue";
    private static final String AUTOCREATE_ATTRIBUTE = "autoCreate";
    private static final String MANDATORY_ATTRIBUTE = "mandatory";
    private static final String PROTECTED_ATTRIBUTE = "protected";
    private static final String PRIMARYITEM_ATTRIBUTE = "primaryItem";
    private static final String MULTIPLE_ATTRIBUTE = "multiple";
    private static final String SAMENAMESIBS_ATTRIBUTE = "sameNameSibs";
    private static final String ONPARENTVERSION_ATTRIBUTE = "onParentVersion";
    private static final String CHILDNODEDEF_ELEMENT = "childNodeDef";
    private static final String REQUIREDPRIMARYTYPES_ELEMENT = "requiredPrimaryTypes";
    private static final String REQUIREDPRIMARYTYPE_ELEMENT = "requiredPrimaryType";
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
	    String msg = "internal error: failed to parse persistent node type definitions";
	    log.error(msg, jde);
	    throw new RepositoryException(msg, jde);
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
	Iterator iter = root.getChildren(NODETYPE_ELEMENT).iterator();
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
	Element root = new Element(ROOT_ELEMENT);

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
	    Element ntElem = new Element(NODETYPE_ELEMENT);
	    writeDef(ntd, ntElem, nsReg);
	    root.addContent(ntElem);
	}

	XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());
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
	    String msg = "invalid serialized node type definition [" + sntName + "]: invalid node type name: " + sntName;
	    log.error(msg, e);
	    throw new InvalidNodeTypeDefException(msg, e);
	}
	ntDef.setName(qntName);

	// supertypes
	ArrayList list = new ArrayList();
	Element typesElem = ntElem.getChild(SUPERTYPES_ELEMENT);
	if (typesElem != null) {
	    Iterator iter = typesElem.getChildren(SUPERTYPE_ELEMENT).iterator();
	    while (iter.hasNext()) {
		Element typeElem = (Element) iter.next();
		Filter filter = new ContentFilter(ContentFilter.TEXT | ContentFilter.CDATA);
		List content = typeElem.getContent(filter);
		if (!content.isEmpty()) {
		    String name = typeElem.getTextTrim();
		    try {
			list.add(QName.fromJCRName(name, nsResolver));
		    } catch (BaseException e) {
			String msg = "invalid serialized node type definition [" + sntName + "]: invalid supertype: " + name;
			log.error(msg, e);
			throw new InvalidNodeTypeDefException(msg, e);
		    }
		}
	    }
	    if (!list.isEmpty()) {
		ntDef.setSupertypes((QName[]) list.toArray(new QName[list.size()]));
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
	list.clear();
	Iterator iter = ntElem.getChildren(PROPERTYDEF_ELEMENT).iterator();
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
		    String msg = "invalid serialized node type definition [" + sntName + "]: invalid property name: " + propName;
		    log.error(msg, e);
		    throw new InvalidNodeTypeDefException(msg, e);
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
		    String msg = "invalid serialized node type definition [" + sntName + "]: invalid type: " + typeName;
		    log.error(msg, e);
		    throw new InvalidNodeTypeDefException(msg, e);
		}
	    }
	    // valueConstraints
	    Element constraintsElem = elem.getChild(VALUECONSTRAINTS_ELEMENT);
	    if (constraintsElem != null) {
		ArrayList list1 = new ArrayList();
		Iterator iter1 = constraintsElem.getChildren(VALUECONSTRAINT_ELEMENT).iterator();
		while (iter1.hasNext()) {
		    Element constraintElem = (Element) iter1.next();
		    Filter filter = new ContentFilter(ContentFilter.TEXT | ContentFilter.CDATA);
		    List content = constraintElem.getContent(filter);
		    if (!content.isEmpty()) {
			String constraint = constraintElem.getTextTrim();
			try {
			    list1.add(ValueConstraint.create(type, constraint));
			} catch (InvalidConstraintException e) {
			    String msg = "invalid serialized node type definition [" + sntName + "]: invalid constraint: " + constraint;
			    log.error(msg, e);
			    throw new InvalidNodeTypeDefException(msg, e);
			}
		    }
		}
		if (!list1.isEmpty()) {
		    pd.setValueConstraints((ValueConstraint[]) list1.toArray(new ValueConstraint[list1.size()]));
		}
	    }
	    // defaultValues
	    Element defValuesElem = elem.getChild(DEFAULTVALUES_ELEMENT);
	    if (defValuesElem != null) {
		int defValType = (type == PropertyType.UNDEFINED) ? PropertyType.STRING : type;
		ArrayList list1 = new ArrayList();
		Iterator iter1 = defValuesElem.getChildren(DEFAULTVALUE_ELEMENT).iterator();
		while (iter1.hasNext()) {
		    Element valueElem = (Element) iter1.next();
		    Filter filter = new ContentFilter(ContentFilter.TEXT | ContentFilter.CDATA);
		    List content = valueElem.getContent(filter);
		    if (!content.isEmpty()) {
			String defValue = valueElem.getTextTrim();
			try {
			    list1.add(InternalValue.valueOf(defValue, defValType));
			} catch (IllegalArgumentException e) {
			    String msg = "invalid serialized node type definition [" + sntName + "]: invalid defaultValue: " + defValue;
			    log.error(msg, e);
			    throw new InvalidNodeTypeDefException(msg, e);
			}
		    }
		}
		if (!list1.isEmpty()) {
		    pd.setDefaultValues((InternalValue[]) list1.toArray(new InternalValue[list1.size()]));
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
		    String msg = "invalid serialized node type definition [" + sntName + "]: invalid onVersion: " + onVersion;
		    log.error(msg, e);
		    throw new InvalidNodeTypeDefException(msg, e);
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
	iter = ntElem.getChildren(CHILDNODEDEF_ELEMENT).iterator();
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
		    String msg = "invalid serialized node type definition [" + sntName + "]: invalid child node name: " + nodeName;
		    log.error(msg, e);
		    throw new InvalidNodeTypeDefException(msg, e);
		}
	    }
	    // requiredPrimaryTypes
	    Element reqTtypesElem = elem.getChild(REQUIREDPRIMARYTYPES_ELEMENT);
	    if (reqTtypesElem != null) {
		ArrayList list1 = new ArrayList();
		Iterator iter1 = reqTtypesElem.getChildren(REQUIREDPRIMARYTYPE_ELEMENT).iterator();
		while (iter1.hasNext()) {
		    Element typeElem = (Element) iter1.next();
		    Filter filter = new ContentFilter(ContentFilter.TEXT | ContentFilter.CDATA);
		    List content = typeElem.getContent(filter);
		    if (!content.isEmpty()) {
			String name = typeElem.getTextTrim();
			try {
			    list1.add(QName.fromJCRName(name, nsResolver));
			} catch (BaseException e) {
			    String msg = "invalid serialized node type definition [" + sntName + "]: invalid requiredPrimaryType: " + name;
			    log.error(msg, e);
			    throw new InvalidNodeTypeDefException(msg, e);
			}
		    }
		}
		if (!list1.isEmpty()) {
		    cnd.setRequiredPrimaryTypes((QName[]) list1.toArray(new QName[list1.size()]));
		}
	    }
	    // defaultPrimaryType
	    String defaultPrimaryType = elem.getAttributeValue(DEFAULTPRIMARYTYPE_ATTRIBUTE);
	    if (defaultPrimaryType != null && defaultPrimaryType.length() > 0) {
		try {
		    cnd.setDefaultPrimaryType(QName.fromJCRName(defaultPrimaryType, nsResolver));
		} catch (BaseException e) {
		    String msg = "invalid serialized node type definition [" + sntName + "]: invalid defaultPrimaryType: " + defaultPrimaryType;
		    log.error(msg, e);
		    throw new InvalidNodeTypeDefException(msg, e);
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
		    String msg = "invalid serialized node type definition [" + sntName + "]: invalid onVersion: " + onVersion;
		    log.error(msg, e);
		    throw new InvalidNodeTypeDefException(msg, e);
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
	    QName[] qNames = ntd.getSupertypes();
	    if (qNames.length != 0) {
		Element typesElem = new Element(SUPERTYPES_ELEMENT);
		ntElem.addContent(typesElem);
		for (int i = 0; i < qNames.length; i++) {
		    Element typeElem = new Element(SUPERTYPE_ELEMENT);
		    typesElem.addContent(typeElem);
		    typeElem.setText(qNames[i].toJCRName(nsResolver));
		}
	    }

	    // mixin
	    ntElem.setAttribute(MIXIN_ATTRIBUTE, Boolean.toString(ntd.isMixin()));

	    // orderableChildNodes
	    ntElem.setAttribute(ORDERABLECHILDNODES_ATTRIBUTE, Boolean.toString(ntd.hasOrderableChildNodes()));

	    // property definitions
	    PropDef[] pda = ntd.getPropertyDefs();
	    for (int i = 0; i < pda.length; i++) {
		PropDef pd = pda[i];
		Element elem = new Element(PROPERTYDEF_ELEMENT);
		ntElem.addContent(elem);

		// name
		String name = pd.getName() == null ? "" : pd.getName().toJCRName(nsResolver);
		elem.setAttribute(NAME_ATTRIBUTE, name);
		// type
		elem.setAttribute(TYPE_ATTRIBUTE, PropertyType.nameFromValue(pd.getRequiredType()));
		// valueConstraints
		ValueConstraint[] vca = pd.getValueConstraints();
		if (vca != null && vca.length != 0) {
		    Element constraintsElem = new Element(VALUECONSTRAINTS_ELEMENT);
		    elem.addContent(constraintsElem);
		    for (int j = 0; j < vca.length; j++) {
			Element constraintElem = new Element(VALUECONSTRAINTS_ELEMENT);
			constraintsElem.addContent(constraintElem);
			constraintElem.setText(vca[j].getDefinition());
		    }
		}
		// defaultValues
		InternalValue[] defVals = pd.getDefaultValues();
		if (defVals != null && defVals.length != 0) {
		    Element valuesElem = new Element(DEFAULTVALUES_ELEMENT);
		    elem.addContent(valuesElem);
		    for (int j = 0; j < defVals.length; j++) {
			Element valueElem = new Element(DEFAULTVALUE_ELEMENT);
			valuesElem.addContent(valueElem);
			valueElem.setText(defVals[j].toString());
		    }
		}
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
		Element elem = new Element(CHILDNODEDEF_ELEMENT);
		ntElem.addContent(elem);

		// name
		String name = nd.getName() == null ? "" : nd.getName().toJCRName(nsResolver);
		elem.setAttribute(NAME_ATTRIBUTE, name);
		// requiredPrimaryTypes
		qNames = nd.getRequiredPrimaryTypes();
		if (qNames.length != 0) {
		    Element typesElem = new Element(REQUIREDPRIMARYTYPES_ELEMENT);
		    elem.addContent(typesElem);
		    for (int j = 0; j < qNames.length; j++) {
			Element typeElem = new Element(REQUIREDPRIMARYTYPE_ELEMENT);
			typesElem.addContent(typeElem);
			typeElem.setText(qNames[j].toJCRName(nsResolver));
		    }
		}
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
