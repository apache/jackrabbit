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
package org.apache.jackrabbit.spi2dav;

import org.apache.jackrabbit.commons.webdav.NodeTypeConstants;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.nodetype.QItemDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QPropertyDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeTypeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.w3c.dom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.OnParentVersionAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to build <code>QNodeTypeDefinition</code>, <code>QNodeDefinition</code>
 * and <code>QPropertyDefinition</code> from the information retrieved from the
 * WebDAV response body.
 */
class DefinitionUtil implements NodeTypeConstants {

    private static Logger log = LoggerFactory.getLogger(DefinitionUtil.class);

    /**
     * Avoid instantiation.
     */
    private DefinitionUtil() {}

    /**
     * 
     * @param declaringNodeType
     * @param ndefElement
     * @param resolver
     * @return
     * @throws RepositoryException
     */
    static QNodeDefinition createQNodeDefinition(Name declaringNodeType, Element ndefElement, NamePathResolver resolver) throws RepositoryException {
        QNodeDefinitionBuilder builder = new QNodeDefinitionBuilder();

        buildQItemDefinition(declaringNodeType, ndefElement, resolver, builder);
        
        // TODO: webdav server sends jcr names -> nsResolver required. improve this.
        // NOTE: the server should send the namespace-mappings as addition ns-defininitions
        try {

            if (ndefElement.hasAttribute(DEFAULTPRIMARYTYPE_ATTRIBUTE)) {
                Name defaultPrimaryType = resolver.getQName(ndefElement.getAttribute(DEFAULTPRIMARYTYPE_ATTRIBUTE));
                builder.setDefaultPrimaryType(defaultPrimaryType);
            }

            Element reqPrimaryTypes = DomUtil.getChildElement(ndefElement, REQUIREDPRIMARYTYPES_ELEMENT, null);
            if (reqPrimaryTypes != null) {
                ElementIterator it = DomUtil.getChildren(reqPrimaryTypes, REQUIREDPRIMARYTYPE_ELEMENT, null);
                while (it.hasNext()) {
                    builder.addRequiredPrimaryType(resolver.getQName(DomUtil.getTextTrim(it.nextElement())));
                }
            } else {
                builder.addRequiredPrimaryType(NameConstants.NT_BASE);
            }

            if (ndefElement.hasAttribute(SAMENAMESIBLINGS_ATTRIBUTE)) {
                builder.setAllowsSameNameSiblings(Boolean.valueOf(ndefElement.getAttribute(SAMENAMESIBLINGS_ATTRIBUTE)));
            }
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
        return builder.build();
    }

    /**
     * 
     * @param declaringNodeType
     * @param pdefElement
     * @param resolver
     * @param qValueFactory
     * @return
     * @throws RepositoryException
     */
    static QPropertyDefinition createQPropertyDefinition(Name declaringNodeType, Element pdefElement,
                                                  NamePathResolver resolver, QValueFactory qValueFactory) throws RepositoryException {
        QPropertyDefinitionBuilder builder = new QPropertyDefinitionBuilder();

        buildQItemDefinition(declaringNodeType, pdefElement, resolver, builder);

        if (pdefElement.hasAttribute(REQUIREDTYPE_ATTRIBUTE)) {
            builder.setRequiredType(PropertyType.valueFromName(pdefElement.getAttribute(REQUIREDTYPE_ATTRIBUTE)));
        }

        if (pdefElement.hasAttribute(MULTIPLE_ATTRIBUTE)) {
            builder.setMultiple(Boolean.valueOf(pdefElement.getAttribute(MULTIPLE_ATTRIBUTE)));
        }

        if (pdefElement.hasAttribute(FULL_TEXT_SEARCHABLE_ATTRIBUTE)) {
            builder.setFullTextSearchable(Boolean.valueOf(pdefElement.getAttribute(FULL_TEXT_SEARCHABLE_ATTRIBUTE)));
        }
        if (pdefElement.hasAttribute(QUERY_ORDERABLE_ATTRIBUTE)) {
            builder.setQueryOrderable(Boolean.valueOf(pdefElement.getAttribute(QUERY_ORDERABLE_ATTRIBUTE)));
        }

        int requiredType = builder.getRequiredType();                
        Element child = DomUtil.getChildElement(pdefElement, DEFAULTVALUES_ELEMENT, null);
        if (child != null) {
            ElementIterator it = DomUtil.getChildren(child, DEFAULTVALUE_ELEMENT, null);
            while (it.hasNext()) {
                String jcrVal = DomUtil.getText(it.nextElement());
                if (jcrVal == null) {
                    jcrVal = "";
                }
                QValue qValue;
                if (requiredType == PropertyType.BINARY) {
                    // TODO: improve
                    Value v = new ValueFactoryQImpl(qValueFactory, resolver).createValue(jcrVal, requiredType);
                    qValue = ValueFormat.getQValue(v, resolver, qValueFactory);
                } else {
                    qValue = ValueFormat.getQValue(jcrVal, requiredType, resolver, qValueFactory);
                }
                builder.addDefaultValue(qValue);
            }
        } // else: no default values defined.

        child = DomUtil.getChildElement(pdefElement, VALUECONSTRAINTS_ELEMENT, null);
        if (child != null) {
            ElementIterator it = DomUtil.getChildren(child, VALUECONSTRAINT_ELEMENT, null);
            while (it.hasNext()) {
                String qValue = DomUtil.getText(it.nextElement());
                // in case of name and path constraint, the value must be
                // converted to SPI values
                // TODO: tobefixed. path-constraint may contain trailing *
                builder.addValueConstraint(ValueConstraint.create(requiredType, qValue, resolver));
            }
        }

        child = DomUtil.getChildElement(pdefElement, AVAILABLE_QUERY_OPERATORS_ELEMENT, null);
        if (child == null) {
            builder.setAvailableQueryOperators(new String[0]);
        } else {
            List<String> names = new ArrayList<String>();
            ElementIterator it = DomUtil.getChildren(child, AVAILABLE_QUERY_OPERATOR_ELEMENT, null);
            while (it.hasNext()) {
                String str = DomUtil.getText(it.nextElement());
                names.add(str);
            }
            builder.setAvailableQueryOperators(names.toArray(new String[names.size()]));
        }

        return builder.build();
    }

    static QNodeTypeDefinition createQNodeTypeDefinition(Element ntdElement, NamePathResolver resolver,
                                                         QValueFactory qValueFactory)
            throws RepositoryException {
        QNodeTypeDefinitionBuilder builder = new QNodeTypeDefinitionBuilder();

        // TODO: webdav-server currently sends jcr-names -> conversion needed
        // NOTE: the server should send the namespace-mappings as addition ns-defininitions
        try {
            if (ntdElement.hasAttribute(NAME_ATTRIBUTE)) {
                builder.setName(resolver.getQName(ntdElement.getAttribute(NAME_ATTRIBUTE)));
            }

            if (ntdElement.hasAttribute(PRIMARYITEMNAME_ATTRIBUTE)) {
                builder.setPrimaryItemName(resolver.getQName(ntdElement.getAttribute(PRIMARYITEMNAME_ATTRIBUTE)));
            }

            Element child = DomUtil.getChildElement(ntdElement, SUPERTYPES_ELEMENT, null);
            if (child != null) {
                ElementIterator stIter = DomUtil.getChildren(child, SUPERTYPE_ELEMENT, null);
                List<Name> qNames = new ArrayList<Name>();
                while (stIter.hasNext()) {
                    Name st = resolver.getQName(DomUtil.getTextTrim(stIter.nextElement()));
                    qNames.add(st);
                }
                builder.setSupertypes(qNames.toArray(new Name[qNames.size()]));
            }
            if (ntdElement.hasAttribute(ISMIXIN_ATTRIBUTE)) {
                builder.setMixin(Boolean.valueOf(ntdElement.getAttribute(ISMIXIN_ATTRIBUTE)));
            }
            if (ntdElement.hasAttribute(HASORDERABLECHILDNODES_ATTRIBUTE)) {
                builder.setOrderableChildNodes(Boolean.valueOf(ntdElement.getAttribute(HASORDERABLECHILDNODES_ATTRIBUTE)));
            }
            if (ntdElement.hasAttribute(ISABSTRACT_ATTRIBUTE)) {
                builder.setAbstract(Boolean.valueOf(ntdElement.getAttribute(ISABSTRACT_ATTRIBUTE)));
            }
            if (ntdElement.hasAttribute(ISQUERYABLE_ATTRIBUTE)) {
                builder.setQueryable(Boolean.valueOf(ntdElement.getAttribute(ISQUERYABLE_ATTRIBUTE)));
            }

            // nodeDefinitions
            ElementIterator it = DomUtil.getChildren(ntdElement, CHILDNODEDEFINITION_ELEMENT, null);
            List<QNodeDefinition> nds = new ArrayList<QNodeDefinition>();
            while (it.hasNext()) {
                nds.add(createQNodeDefinition(builder.getName(), it.nextElement(), resolver));
            }
            builder.setChildNodeDefs(nds.toArray(new QNodeDefinition[nds.size()]));

            // propertyDefinitions
            it = DomUtil.getChildren(ntdElement, PROPERTYDEFINITION_ELEMENT, null);
            List<QPropertyDefinition> pds = new ArrayList<QPropertyDefinition>();
            while (it.hasNext()) {
                pds.add(createQPropertyDefinition(builder.getName(), it.nextElement(), resolver, qValueFactory));
            }
            builder.setPropertyDefs(pds.toArray(new QPropertyDefinition[pds.size()]));
        } catch (NameException e) {
            log.error(e.getMessage());
            throw new RepositoryException(e);
        }

        return builder.build();
    }

    /**
     * 
     * @param declaringNodeType
     * @param itemDefElement
     * @param resolver
     * @param builder
     * @throws RepositoryException
     */
    private static void buildQItemDefinition(Name declaringNodeType,
                                             Element itemDefElement,
                                             NamePathResolver resolver,
                                             QItemDefinitionBuilder builder) throws RepositoryException {
        try {
            String attr = itemDefElement.getAttribute(DECLARINGNODETYPE_ATTRIBUTE);
            if (attr != null) {
                Name dnt = resolver.getQName(attr);
                if (declaringNodeType != null && !declaringNodeType.equals(dnt)) {
                    throw new RepositoryException("Declaring nodetype mismatch: In element = '" + dnt + "', Declaring nodetype = '" + declaringNodeType + "'");
                }
                builder.setDeclaringNodeType(dnt);
            } else {
                builder.setDeclaringNodeType(declaringNodeType);
            }

            if (itemDefElement.hasAttribute(NAME_ATTRIBUTE)) {
                String nAttr = itemDefElement.getAttribute(NAME_ATTRIBUTE);
                if (nAttr.length() > 0) {
                    builder.setName((isAnyName(nAttr)) ? NameConstants.ANY_NAME : resolver.getQName(nAttr));
                } else {
                    builder.setName(NameConstants.ROOT);
                }
            } else {
                // TODO: check if correct..
                builder.setName(NameConstants.ANY_NAME);
            }
        } catch (NameException e) {
            throw new RepositoryException(e);
        }

        if (itemDefElement.hasAttribute(AUTOCREATED_ATTRIBUTE)) {
            builder.setAutoCreated(Boolean.valueOf(itemDefElement.getAttribute(AUTOCREATED_ATTRIBUTE)));
        }
        if (itemDefElement.hasAttribute(MANDATORY_ATTRIBUTE)) {
            builder.setMandatory(Boolean.valueOf(itemDefElement.getAttribute(MANDATORY_ATTRIBUTE)));
        }
        if (itemDefElement.hasAttribute(PROTECTED_ATTRIBUTE)) {
            builder.setProtected(Boolean.valueOf(itemDefElement.getAttribute(PROTECTED_ATTRIBUTE)));
        }

        if (itemDefElement.hasAttribute(ONPARENTVERSION_ATTRIBUTE)) {
            builder.setOnParentVersion(OnParentVersionAction.valueFromName(itemDefElement.getAttribute(ONPARENTVERSION_ATTRIBUTE)));
        }
    }

    private static boolean isAnyName(String nameAttribute) {
        return NameConstants.ANY_NAME.getLocalName().equals(nameAttribute);
    }
}