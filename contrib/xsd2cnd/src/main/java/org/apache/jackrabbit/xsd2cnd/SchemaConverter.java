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
package org.apache.jackrabbit.xsd2cnd;

import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.core.nodetype.ValueConstraint;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.QName;
import org.apache.xerces.dom3.bootstrap.DOMImplementationRegistry;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;

import javax.jcr.PropertyType;
import javax.jcr.version.OnParentVersionAction;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * SchemaConverter
 * Converts an XML Schema into a set of node type defintions (NodeTypeDef objects)
 * which can then be registered in a repository.
 */
public class SchemaConverter {
    private static final QName XML_TEXT_NODETYPE_NAME = new QName(QName.NS_JCR_URI, "Xmltext");
    private static final QName XML_CONTENT_PROPERTY_NAME = new QName(QName.NS_JCR_URI, "xmlContent");

    private final HashMap nodeTypeDefs = new HashMap();
    private final HashMap nodeTypeBaseNames = new HashMap();

    /**
     * Constructor
     */
    public SchemaConverter(String fileName) throws SchemaConversionException {
        File file = new File(fileName);
        convertSchema(file);
    }

    /**
     * Constructor
     */
    public SchemaConverter(File file) throws SchemaConversionException {
        convertSchema(file);
    }

    /**
     * getNodeTypeDefs
     */
    public List getNodeTypeDefs() {
        return new ArrayList(nodeTypeDefs.values());
    }

    /**
     * convertSchema
     */
    private void convertSchema(File file) throws SchemaConversionException {
        try {
            // Find an XMLSchema loader instance
            DOMImplementationRegistry registry =
                DOMImplementationRegistry.newInstance();
            XSImplementation implementation = (XSImplementation)
                registry.getDOMImplementation("XS-Loader");
            XSLoader loader = implementation.createXSLoader(null);

            // Load the XML Schema
            String uri = file.toURI().toString();
            XSModel xsModel = loader.loadURI(uri);

            // Convert top level complex type definitions to node types
            XSNamedMap map = xsModel.getComponents(XSConstants.TYPE_DEFINITION);
            for (int i = 0; i < map.getLength(); i++) {
                XSTypeDefinition tDef = (XSTypeDefinition) map.item(i);
                checkAndConvert(tDef, null, null);
            }
            //  Convert local (anonymous) complex type defs found in top level element declarations
            map = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
            for (int i = 0; i < map.getLength(); i++) {
                XSElementDeclaration eDec = (XSElementDeclaration) map.item(i);
                XSTypeDefinition tDef = eDec.getTypeDefinition();
                checkAndConvert(tDef, eDec.getNamespace(), eDec.getName());
            }
        } catch (ClassNotFoundException e) {
            throw new SchemaConversionException("XSLoader not found", e);
        } catch (InstantiationException e) {
            throw new SchemaConversionException("XSLoader instantiation error", e);
        } catch (IllegalAccessException e) {
            throw new SchemaConversionException("XSLoader access error", e);
        }
    }

    /**
     * checkAndConvert
     */
    private void checkAndConvert(XSTypeDefinition tDef, String namespace, String nameHint) throws SchemaConversionException {
        if (tDef.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
            XSComplexTypeDefinition ctDef = (XSComplexTypeDefinition) tDef;
            if (ctDef.getContentType() != XSComplexTypeDefinition.CONTENTTYPE_SIMPLE
                    || ctDef.getAttributeUses().getLength() > 0
                    || ctDef.getAttributeWildcard() != null) {
                convertComplexTypeDef(ctDef, namespace, nameHint);
            }
        }
    }

    /**
     * convertComplexTypeDef
     */
    private QName convertComplexTypeDef(XSComplexTypeDefinition ctDef, String namespace, String nameHint) throws SchemaConversionException {
        NodeTypeDef ntd;

        // Check if this ComplexTypeDef has already been converted
        // if so, return the name of the corresponding NodeTypeDef
        if (nodeTypeDefs.containsKey(ctDef)) {
            ntd = (NodeTypeDef) nodeTypeDefs.get(ctDef);
            return ntd.getName();
        }

        // Otherwise create a new NodeTypeDef and add it to the map
        // keyed by the CTDef from which it was converted
        NodeTypeDef ntDef = new NodeTypeDef();
        nodeTypeDefs.put(ctDef, ntDef);

        // Make name for the node type, inventing names for anonymous CTDefs
        // and avoiding repetitions.
        QName ntName;
        if (ctDef.getAnonymous()) {
            if (nameHint == null) {
                throw new SchemaConversionException("Anonymous complex type definition encountered without name hint");
            }
            namespace = noNull(namespace);
            QName baseName = new QName(namespace, nameHint + "Type");
            Integer count = (Integer) nodeTypeBaseNames.get(baseName);
            if (count == null) {
                nodeTypeBaseNames.put(baseName, new Integer(0));
                ntName = baseName;
            } else {
                int newCount = count.intValue() + 1;
                ntName = new QName(namespace, nameHint + "Type_" + Integer.toString(newCount));
                nodeTypeBaseNames.put(baseName, new Integer(newCount));
            }
        } else {
            ntName = new QName(noNull(ctDef.getNamespace()), ctDef.getName());
        }

        // set the name of the node type
        ntDef.setName(ntName);

        // Fill in the rest of the node type def
        buildNodeTypeDef(ntDef, ctDef);
        return ntName;
    }

    /**
     * buildNodeTypeDef
     */
    private void buildNodeTypeDef(NodeTypeDef nodeTypeDef, XSComplexTypeDefinition ctdef) throws SchemaConversionException {
        List propDefList = new ArrayList();
        List nodeDefList = new ArrayList();

        // Set supertype of node type. Currently all node types created direct subtypes of nt:base
        nodeTypeDef.setSupertypes(new QName[]{QName.NT_BASE});

        // Set mixin status. Currently all node types are set to mixin=false
        nodeTypeDef.setMixin(false);

        // The orderable status is false unless it is reset during deeper traversal of
        // this complex type definition. See method particleToDefs below
        boolean orderable = false;

        // Set Primary Item. Currently we set this to null: no primary item
        nodeTypeDef.setPrimaryItemName(null);

        // Convert attribute uses (<xs:attribute>) to property definitions
        XSObjectList list = ctdef.getAttributeUses();
        for (int i = 0; i < list.getLength(); i++) {
            XSAttributeUse attribUse = (XSAttributeUse) list.item(i);
            PropDef propDef = attributeUseToPropDef(attribUse);
            propDefList.add(propDef);
        }

        // Convert attribute wildcard (<xs:anyattribute>)to residual property
        XSWildcard wildcard = ctdef.getAttributeWildcard();
        if (wildcard != null) {
            PropDef propDef = wildcardPropDef();
            propDefList.add(propDef);
        }

        // If the content model of this complex type is simple, then we create a
        // PropDef corresponding to the XML text node defined by this content model.
        if (ctdef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE) {
            XSSimpleTypeDefinition stdef = ctdef.getSimpleType();
            //set to non-mandatory, single-value property called "jcr:xmlcharacters"
            PropDef propDef = simpleTypeToPropDef(stdef, XML_CONTENT_PROPERTY_NAME, false, false);
            propDefList.add(propDef);

            // If the content model of this complex type is element or mixed then we must convert the
            // contained XSParticle into a set of node and/or property defs
        } else if (ctdef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_ELEMENT
                || ctdef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED) {
            XSParticle p = ctdef.getParticle();

            // particleToDefs takes the two lists and adds to them as necessary. It also returns
            // an orderable indicator based on the top level compositor used within the complex typedef
            // (if there is one).
            orderable = particleToDefs(p, propDefList, nodeDefList);
        } else if (ctdef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_EMPTY) {
            // Xerces ignores type definitions with empty content
            // so a type with empty content should never be encountered.
        } else {
            throw new SchemaConversionException("Unrecognized content type");
        }

        // If the content model of this complex type is mixed, then we need to also add
        // an SNS node def with name jcr:xmltext to hold the multiple text nodes
        // that may be interspersed. We also ensure that the node type has an orderable setting
        // of true in this case. A node type for jcr:xmltext, called jcr:Xmltext,
        // is also added to the node type set.
        if (ctdef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_MIXED) {
            orderable = true;
            nodeDefList.add(createXMLTextNodeDef());
            addXMLTextNodeType();
        }

        // Set the orderable status of the node type def
        // this may have been modified by deeper traversal
        // through particleDefs.
        nodeTypeDef.setOrderableChildNodes(orderable);

        // Set the declaring node type in each of the accumulated property and node defs
        QName nodeTypeName = nodeTypeDef.getName();
        for (Iterator i = propDefList.listIterator(); i.hasNext();) {
            PropDefImpl propDef = (PropDefImpl) i.next();
            propDef.setDeclaringNodeType(nodeTypeName);
        }
        for (Iterator i = nodeDefList.listIterator(); i.hasNext();) {
            NodeDefImpl nodeDef = (NodeDefImpl) i.next();
            nodeDef.setDeclaringNodeType(nodeTypeName);
        }

        //Add the collected propDefs to the node type def
        nodeTypeDef.setPropertyDefs((PropDef[]) propDefList.toArray(new PropDef[propDefList.size()]));

        //Add the collected nodeDefs to the node type def
        nodeTypeDef.setChildNodeDefs((NodeDef[]) nodeDefList.toArray(new NodeDef[nodeDefList.size()]));
    }

    /**
     * particleToDefs
     */
    private boolean particleToDefs(XSParticle particle, List propDefList, List nodeDefList) throws SchemaConversionException {
        boolean orderable = false;

        XSTerm term = particle.getTerm();

        // If the maxoccurs setting of this particle is zero then this
        // particle does not contribute any node or property definition and
        // we do nothing and return.
        if (particle.getMaxOccurs() == 0) {
            return orderable;
        }

        // Determine the mandatory setting of the node or property
        // corresponding to this particle (if this particle does not
        // correspond to node or property this information is ignored).
        boolean mandatory = false;
        if (particle.getMinOccurs() > 0) {
            mandatory = true;
        }

        // Determine the same-name siblings setting of the node, or
        // the multiple setting of the property, corresponding to this
        // particle (if this particle does not correspond to
        // node or property this information is ignored).
        boolean multiple = false;
        if (particle.getMaxOccurs() > 1 || particle.getMaxOccursUnbounded()) {
            multiple = true;
        }

        // If this particle is an element declaration (an <xs:element>)
        // then it is converted into either a node or property def.
        if (term.getType() == XSConstants.ELEMENT_DECLARATION) {
            XSElementDeclaration eDec = (XSElementDeclaration) term;

            // Name for property or node def taken from the name of the element
            QName name = new QName(noNull(eDec.getNamespace()), eDec.getName());

            // Get the type definition for this element declaration
            XSTypeDefinition tDef = eDec.getTypeDefinition();

            // If this element declaration is of simple type
            // then it is converted into a property def
            if (tDef.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
                XSSimpleTypeDefinition stDef = (XSSimpleTypeDefinition) tDef;
                PropDef propDef = simpleTypeToPropDef(stDef, name, mandatory, multiple);
                propDefList.add(propDef);

                // If this element declaration is of complex type then
                // it is converted into either node or property def
            } else if (tDef.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                XSComplexTypeDefinition ctDef = (XSComplexTypeDefinition) tDef;

                // If the complex type definition contains a simple content model
                // and does not contain any attribute uses or attribute wildcards
                // then the enclosing element is converted to a property def
                if (ctDef.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE
                        && ctDef.getAttributeUses().getLength() == 0 && ctDef.getAttributeWildcard() == null) {
                    XSSimpleTypeDefinition std = ctDef.getSimpleType();
                    PropDef pd = simpleTypeToPropDef(std, name, mandatory, multiple);
                    propDefList.add(pd);

                    // If the complex type definition contains a complex content model
                    // or a simple content model with attribute uses or an attribute wildcard
                    // then the enclosing element is converted into a node def
                } else {
                    NodeDef nd = complexTypeToNodeDef(ctDef, name, mandatory, multiple);
                    nodeDefList.add(nd);
                }
            }

            // If this particle is a wildcard (an <xs:any> )then it
            // is converted into a node def.
        } else if (term.getType() == XSConstants.WILDCARD) {
            nodeDefList.add(wildcardNodeDef());

            // If this particle is a model group (one of
            // <xs:sequence>, <xs:choice> or <xs:all>) then
            // it subparticles must be processed.
        } else if (term.getType() == XSConstants.MODEL_GROUP) {
            XSModelGroup mg = (XSModelGroup) term;

            // If the top level compositor is <xs:sequence> we convert this to
            // mean that the node type corresponding to the complex type def will have
            // an orderable setting of true.
            if (mg.getCompositor() == XSModelGroup.COMPOSITOR_SEQUENCE) {
                orderable = true;
            }

            // We ignore any further nested compositors
            // by ignoring the return value of nested particleToDefs calls.
            XSObjectList list = mg.getParticles();
            for (int i = 0; i < list.getLength(); i++) {
                XSParticle pp = (XSParticle) list.item(i);
                particleToDefs(pp, propDefList, nodeDefList);
            }
        }
        return orderable;
    }

    /**
     * attributeUseToPropDef
     */
    private PropDef attributeUseToPropDef(XSAttributeUse au) {

        // The required setting of this attribute use becomes the
        // the mandatory setting of the property definition:
        // <xs:attribute ref="..." use="required | optional | prohibited">
        // note that if use is prohibited the attribute use itself is absent from the XSModel so
        // we would never get here anyway
        boolean mandatory = au.getRequired();

        // Get the contained attribute declaration
        XSAttributeDeclaration ad = au.getAttrDeclaration();

        // The name of the Attribute Declaration becomes the name of the PropDef
        QName name = new QName(noNull(ad.getNamespace()), ad.getName());

        // Since this is an attribute declaration we assume it converts to
        // a single value property (we ignore the XML Schema List Type)
        boolean multiple = false;

        // Get the simple type def for this attribute
        XSSimpleTypeDefinition std = ad.getTypeDefinition();

        // convert it to a propdef
        return simpleTypeToPropDef(std, name, mandatory, multiple);
    }

    /**
     * simpleTypeToPropDef
     */
    private PropDef simpleTypeToPropDef(XSSimpleTypeDefinition std, QName propertyName, boolean mandatory, boolean multiple) {

        // Create PropDef and set attributes passed in
        PropDefImpl propDef = new PropDefImpl();
        propDef.setName(propertyName);
        propDef.setMandatory(mandatory);
        propDef.setMultiple(multiple);

        // Set the property type from the built-in kind
        short kind = std.getBuiltInKind();
        int propertyType = convertBuiltInKindToPropertyType(kind);
        propDef.setRequiredType(propertyType);

        //todo:determine value constraints from schema
        propDef.setValueConstraints(new ValueConstraint[]{});

        //todo:determine default value from schema
        propDef.setDefaultValues(new InternalValue[]{});

        // Set the attributes not translated from schema
        propDef.setAutoCreated(false);
        propDef.setOnParentVersion(OnParentVersionAction.COPY);
        propDef.setProtected(false);

        return propDef;
    }

    /**
     * complexTypeToNodeDef
     */
    private NodeDef complexTypeToNodeDef(XSComplexTypeDefinition ctDef, QName nodeName, boolean mandatory, boolean multiple) throws SchemaConversionException {

        // Create NodeDef and set attributes passed in
        NodeDefImpl nodeDef = new NodeDefImpl();
        nodeDef.setName(nodeName);
        nodeDef.setMandatory(mandatory);
        nodeDef.setAllowsSameNameSiblings(multiple);

        // Create nodeType for this node def (will only be created if needed)
        QName nodeTypeName = convertComplexTypeDef(ctDef, nodeName.getNamespaceURI(), nodeName.getLocalName());
        nodeDef.setDefaultPrimaryType(nodeTypeName);
        nodeDef.setRequiredPrimaryTypes(new QName[]{nodeTypeName});

        // Set attributes not determined by schema
        nodeDef.setAutoCreated(false);
        nodeDef.setOnParentVersion(OnParentVersionAction.COPY);
        nodeDef.setProtected(false);
        return nodeDef;
    }

    /**
     * attributeWildcardToPropDef
     */
    private PropDef wildcardPropDef() {
        PropDefImpl propDef = new PropDefImpl();
        propDef.setName(PropDef.ANY_NAME);
        propDef.setMandatory(false);
        propDef.setMultiple(false);
        propDef.setRequiredType(PropertyType.UNDEFINED);
        propDef.setValueConstraints(new ValueConstraint[]{});
        propDef.setDefaultValues(new InternalValue[]{});
        propDef.setAutoCreated(false);
        propDef.setOnParentVersion(OnParentVersionAction.COPY);
        propDef.setProtected(false);
        return propDef;
    }

    /**
     * wildcardToNodeDef
     */
    private NodeDef wildcardNodeDef() {
        NodeDefImpl nodeDef = new NodeDefImpl();
        nodeDef.setName(NodeDef.ANY_NAME);
        nodeDef.setMandatory(false);
        nodeDef.setAllowsSameNameSiblings(false);
        nodeDef.setDefaultPrimaryType(NodeDef.ANY_NAME);
        nodeDef.setRequiredPrimaryTypes(new QName[]{});
        nodeDef.setAutoCreated(false);
        nodeDef.setOnParentVersion(OnParentVersionAction.COPY);
        nodeDef.setProtected(false);
        return nodeDef;
    }

    /**
     * createXMLtextNodeDef
     */
    private NodeDef createXMLTextNodeDef() {
        NodeDefImpl nodeDef = new NodeDefImpl();
        nodeDef.setName(QName.JCR_XMLTEXT);
        nodeDef.setMandatory(false);
        nodeDef.setAllowsSameNameSiblings(true);
        nodeDef.setDefaultPrimaryType(XML_TEXT_NODETYPE_NAME);
        nodeDef.setRequiredPrimaryTypes(new QName[]{XML_TEXT_NODETYPE_NAME});
        nodeDef.setAutoCreated(false);
        nodeDef.setOnParentVersion(OnParentVersionAction.COPY);
        nodeDef.setProtected(false);
        return nodeDef;
    }

    /**
     * addXMLTextNodeType
     */
    private void addXMLTextNodeType() {
        // If the XML text node type already exists, do nothing
        if (nodeTypeDefs.containsKey(XML_TEXT_NODETYPE_NAME)) {
            return;
        }

        // Otherwise create a new node type def and set its attributes
        NodeTypeDef ntd = new NodeTypeDef();
        ntd.setName(XML_TEXT_NODETYPE_NAME);
        ntd.setSupertypes(new QName[]{QName.NT_BASE});
        ntd.setPrimaryItemName(null);
        ntd.setMixin(false);
        ntd.setOrderableChildNodes(false);
        ntd.setChildNodeDefs(new NodeDef[]{});

        // The XML text node type defines a single property called jcr:xmlcharacters
        // Create the property def for this property
        PropDefImpl pd = new PropDefImpl();
        pd.setName(QName.JCR_XMLCHARACTERS);
        pd.setDeclaringNodeType(XML_TEXT_NODETYPE_NAME);
        pd.setAutoCreated(false);
        pd.setDefaultValues(new InternalValue[]{});
        pd.setMandatory(false);
        pd.setMultiple(false);
        pd.setOnParentVersion(OnParentVersionAction.COPY);
        pd.setProtected(false);
        pd.setRequiredType(PropertyType.STRING);
        pd.setValueConstraints(new ValueConstraint[]{});

        // Add the jcr:xmlcharacters properyt def to the node type def
        ntd.setPropertyDefs(new PropDef[]{pd});

        // Add the node type def to the set of node type defs
        nodeTypeDefs.put(ntd.getName(), ntd);
    }

    /**
     * convertBuiltInKindToPropertyType
     */
    private int convertBuiltInKindToPropertyType(short kind) {
        int propertyType;
        switch (kind) {
            case XSConstants.ANYSIMPLETYPE_DT:
            case XSConstants.STRING_DT:
            case XSConstants.ID_DT:
            case XSConstants.ENTITY_DT:
            case XSConstants.NOTATION_DT:
            case XSConstants.NORMALIZEDSTRING_DT:
            case XSConstants.TOKEN_DT:
            case XSConstants.LANGUAGE_DT:
            case XSConstants.NMTOKEN_DT:
                propertyType = PropertyType.STRING;
                break;
            case XSConstants.BOOLEAN_DT:
                propertyType = PropertyType.BOOLEAN;
                break;
            case XSConstants.DECIMAL_DT:
            case XSConstants.FLOAT_DT:
            case XSConstants.DOUBLE_DT:
                propertyType = PropertyType.DOUBLE;
                break;
            case XSConstants.DURATION_DT:
            case XSConstants.DATETIME_DT:
            case XSConstants.TIME_DT:
            case XSConstants.DATE_DT:
            case XSConstants.GYEARMONTH_DT:
            case XSConstants.GYEAR_DT:
            case XSConstants.GMONTHDAY_DT:
            case XSConstants.GDAY_DT:
            case XSConstants.GMONTH_DT:
                propertyType = PropertyType.DATE;
                break;
            case XSConstants.HEXBINARY_DT:
            case XSConstants.BASE64BINARY_DT:
            case XSConstants.ANYURI_DT:
                propertyType = PropertyType.BINARY;
                break;
            case XSConstants.QNAME_DT:
            case XSConstants.NAME_DT:
            case XSConstants.NCNAME_DT:
                propertyType = PropertyType.NAME;
                break;
            case XSConstants.IDREF_DT:
                propertyType = PropertyType.REFERENCE;
                break;
            case XSConstants.INTEGER_DT:
            case XSConstants.NONPOSITIVEINTEGER_DT:
            case XSConstants.NEGATIVEINTEGER_DT:
            case XSConstants.LONG_DT:
            case XSConstants.INT_DT:
            case XSConstants.SHORT_DT:
            case XSConstants.BYTE_DT:
            case XSConstants.NONNEGATIVEINTEGER_DT:
            case XSConstants.UNSIGNEDLONG_DT:
            case XSConstants.UNSIGNEDINT_DT:
            case XSConstants.UNSIGNEDSHORT_DT:
            case XSConstants.UNSIGNEDBYTE_DT:
            case XSConstants.POSITIVEINTEGER_DT:
                propertyType = PropertyType.LONG;
                break;
            case XSConstants.LISTOFUNION_DT:
            case XSConstants.LIST_DT:
            case XSConstants.UNAVAILABLE_DT:
                propertyType = PropertyType.STRING;
                break;
            default:
                propertyType = PropertyType.STRING;
                break;
        }
        return propertyType;
    }

    private String noNull(String s) {
        return s != null ? s : "";
    }
}
