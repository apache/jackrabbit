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
package org.apache.jackrabbit.core.nodetype.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.version.OnParentVersionAction;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.commons.query.qom.Operator;
import org.apache.jackrabbit.core.util.DOMBuilder;
import org.apache.jackrabbit.core.value.InternalValueFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;

/**
 * Node type definition writer. This class is used to write the
 * persistent node type definition files used by Jackrabbit.
 */
public final class NodeTypeWriter {

    /**
     * Writes a node type definition file. The file contents are written
     * to the given output stream and will contain the given node type
     * definitions. The given namespace registry is used for namespace
     * mappings.
     *
     * @param xml XML output stream
     * @param registry namespace registry
     * @param types node types
     * @throws IOException         if the node type definitions cannot
     *                             be written
     * @throws RepositoryException on repository errors
     */
    public static void write(
            OutputStream xml, QNodeTypeDefinition[] types, NamespaceRegistry registry)
            throws IOException, RepositoryException {
        try {
            NodeTypeWriter writer = new NodeTypeWriter(registry);
            for (QNodeTypeDefinition type : types) {
                writer.addNodeTypeDef(type);
            }
            writer.write(xml);
        } catch (ParserConfigurationException e) {
            IOException e2 = new IOException(e.getMessage());
            e2.initCause(e);
            throw e2;
        } catch (NamespaceException e) {
            throw new RepositoryException(
                    "Invalid namespace reference in a node type definition", e);
        }
    }

    /** The node type document builder. */
    private final DOMBuilder builder;

    /** The namespace resolver. */
    private final NamePathResolver resolver;

    private final ValueFactoryQImpl factory;

    /**
     * Creates a node type definition file writer. The given namespace
     * registry is used for the XML namespace bindings.
     *
     * @param registry namespace registry
     * @throws ParserConfigurationException if the node type definition
     *                                      document cannot be created
     * @throws RepositoryException          if the namespace mappings cannot
     *                                      be retrieved from the registry
     */
    private NodeTypeWriter(NamespaceRegistry registry)
            throws ParserConfigurationException, RepositoryException {
        builder = new DOMBuilder(Constants.NODETYPES_ELEMENT);

        String[] prefixes = registry.getPrefixes();
        for (String prefix : prefixes) {
            if (!"".equals(prefix)) {
                String uri = registry.getURI(prefix);
                builder.setAttribute("xmlns:" + prefix, uri);
            }
        }

        NamespaceResolver nsResolver = new AdditionalNamespaceResolver(registry);
        resolver = new DefaultNamePathResolver(nsResolver);
        factory = new ValueFactoryQImpl(InternalValueFactory.getInstance(), resolver);
    }

    /**
     * Builds a node type definition element under the current element.
     *
     * @param def node type definition
     * @throws RepositoryException       if the default property values
     *                                   cannot be serialized
     * @throws NamespaceException if the node type definition contains
     *                                   invalid namespace references
     */
    private void addNodeTypeDef(QNodeTypeDefinition def)
            throws NamespaceException, RepositoryException {
        builder.startElement(Constants.NODETYPE_ELEMENT);

        // simple attributes
        builder.setAttribute(
                Constants.NAME_ATTRIBUTE, resolver.getJCRName(def.getName()));
        builder.setAttribute(
                Constants.ISMIXIN_ATTRIBUTE, def.isMixin());
        builder.setAttribute(
                Constants.ISQUERYABLE_ATTRIBUTE, def.isQueryable());
        builder.setAttribute(
                Constants.ISABSTRACT_ATTRIBUTE, def.isAbstract());
        builder.setAttribute(
                Constants.HASORDERABLECHILDNODES_ATTRIBUTE,
                def.hasOrderableChildNodes());

        // primary item name
        Name item = def.getPrimaryItemName();
        if (item != null) {
            builder.setAttribute(
                    Constants.PRIMARYITEMNAME_ATTRIBUTE,
                    resolver.getJCRName(item));
        } else {
            builder.setAttribute(Constants.PRIMARYITEMNAME_ATTRIBUTE, "");
        }

        // supertype declarations
        Name[] supertypes = def.getSupertypes();
        if (supertypes.length > 0) {
            builder.startElement(Constants.SUPERTYPES_ELEMENT);
            for (Name supertype : supertypes) {
                builder.addContentElement(
                        Constants.SUPERTYPE_ELEMENT,
                        resolver.getJCRName(supertype));
            }
            builder.endElement();
        }

        // property definitions
        QPropertyDefinition[] properties = def.getPropertyDefs();
        for (QPropertyDefinition property : properties) {
            addPropDef(property);
        }

        // child node definitions
        QNodeDefinition[] nodes = def.getChildNodeDefs();
        for (QNodeDefinition node : nodes) {
            addChildNodeDef(node);
        }

        builder.endElement();
    }

    /**
     * Builds a property definition element under the current element.
     *
     * @param def property definition
     * @throws RepositoryException       if the default values cannot
     *                                   be serialized
     * @throws NamespaceException if the property definition contains
     *                                   invalid namespace references
     */
    private void addPropDef(QPropertyDefinition def)
            throws NamespaceException, RepositoryException {
        builder.startElement(Constants.PROPERTYDEFINITION_ELEMENT);

        // simple attributes
        builder.setAttribute(
                Constants.NAME_ATTRIBUTE, resolver.getJCRName(def.getName()));
        builder.setAttribute(
                Constants.AUTOCREATED_ATTRIBUTE, def.isAutoCreated());
        builder.setAttribute(
                Constants.MANDATORY_ATTRIBUTE, def.isMandatory());
        builder.setAttribute(
                Constants.PROTECTED_ATTRIBUTE, def.isProtected());
        builder.setAttribute(
                Constants.ONPARENTVERSION_ATTRIBUTE,
                OnParentVersionAction.nameFromValue(def.getOnParentVersion()));
        builder.setAttribute(
                Constants.MULTIPLE_ATTRIBUTE, def.isMultiple());
        builder.setAttribute(
                Constants.ISFULLTEXTSEARCHABLE_ATTRIBUTE, def.isFullTextSearchable());
        builder.setAttribute(
                Constants.ISQUERYORDERABLE_ATTRIBUTE, def.isQueryOrderable());
        // TODO do properly...
        String[] qops = def.getAvailableQueryOperators();
        if (qops != null && qops.length > 0) {
            List<String> ops = Arrays.asList(qops);
            List<String> defaultOps = Arrays.asList(Operator.getAllQueryOperators());
            if (!ops.containsAll(defaultOps)) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < qops.length; i++) {
                    if (i > 0) {
                        sb.append(' ');
                    }
                    if (qops[i].equals(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO)) {
                        sb.append(Constants.EQ_ENTITY);
                    } else if (qops[i].equals(QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO)) {
                        sb.append(Constants.NE_ENTITY);
                    } else if (qops[i].equals(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN)) {
                        sb.append(Constants.GT_ENTITY);
                    } else if (qops[i].equals(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO)) {
                        sb.append(Constants.GE_ENTITY);
                    } else if (qops[i].equals(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN)) {
                        sb.append(Constants.LT_ENTITY);
                    } else if (qops[i].equals(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO)) {
                        sb.append(Constants.LE_ENTITY);
                    } else if (qops[i].equals(QueryObjectModelConstants.JCR_OPERATOR_LIKE)) {
                        sb.append(Constants.LIKE_ENTITY);
                    }
                }
                builder.setAttribute(
                        Constants.AVAILABLEQUERYOPERATORS_ATTRIBUTE, sb.toString());
            }
        }

        builder.setAttribute(
                Constants.REQUIREDTYPE_ATTRIBUTE,
                PropertyType.nameFromValue(def.getRequiredType()));

        // value constraints
        QValueConstraint[] constraints = def.getValueConstraints();
        if (constraints != null && constraints.length > 0) {
            builder.startElement(Constants.VALUECONSTRAINTS_ELEMENT);
            for (QValueConstraint constraint : constraints) {
                ValueConstraint vc = ValueConstraint.create(
                        def.getRequiredType(), constraint.getString());
                builder.addContentElement(
                        Constants.VALUECONSTRAINT_ELEMENT,
                        vc.getDefinition(resolver));
            }
            builder.endElement();
        }

        // default values
        QValue[] defaults = def.getDefaultValues();
        if (defaults != null && defaults.length > 0) {
            builder.startElement(Constants.DEFAULTVALUES_ELEMENT);
            for (QValue v : defaults) {
                builder.addContentElement(
                        Constants.DEFAULTVALUE_ELEMENT,
                        factory.createValue(v).getString());
            }
            builder.endElement();
        }

        builder.endElement();
    }

    /**
     * Builds a child node definition element under the current element.
     *
     * @param def child node definition
     * @throws NamespaceException if the child node definition contains
     *                                   invalid namespace references
     */
    private void addChildNodeDef(QNodeDefinition def)
            throws NamespaceException {
        builder.startElement(Constants.CHILDNODEDEFINITION_ELEMENT);

        // simple attributes
        builder.setAttribute(
                Constants.NAME_ATTRIBUTE, resolver.getJCRName(def.getName()));
        builder.setAttribute(
                Constants.AUTOCREATED_ATTRIBUTE, def.isAutoCreated());
        builder.setAttribute(
                Constants.MANDATORY_ATTRIBUTE, def.isMandatory());
        builder.setAttribute(
                Constants.PROTECTED_ATTRIBUTE, def.isProtected());
        builder.setAttribute(
                Constants.ONPARENTVERSION_ATTRIBUTE,
                OnParentVersionAction.nameFromValue(def.getOnParentVersion()));
        builder.setAttribute(
                Constants.SAMENAMESIBLINGS_ATTRIBUTE, def.allowsSameNameSiblings());

        // default primary type
        Name type = def.getDefaultPrimaryType();
        if (type != null) {
            builder.setAttribute(
                    Constants.DEFAULTPRIMARYTYPE_ATTRIBUTE,
                    resolver.getJCRName(type));
        } else {
            builder.setAttribute(Constants.DEFAULTPRIMARYTYPE_ATTRIBUTE, "");
        }

        // required primary types
        Name[] requiredTypes = def.getRequiredPrimaryTypes();
        builder.startElement(Constants.REQUIREDPRIMARYTYPES_ELEMENT);
        for (Name requiredType : requiredTypes) {
            builder.addContentElement(
                    Constants.REQUIREDPRIMARYTYPE_ELEMENT,
                    resolver.getJCRName(requiredType));
        }
        builder.endElement();

        builder.endElement();
    }

    /**
     * Writes the node type definition document to the given output stream.
     *
     * @param xml XML output stream
     * @throws IOException if the node type document could not be written
     */
    private void write(OutputStream xml) throws IOException {
        builder.write(xml);
    }

}
