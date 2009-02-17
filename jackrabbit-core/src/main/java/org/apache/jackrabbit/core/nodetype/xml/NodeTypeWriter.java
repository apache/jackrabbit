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

import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.ValueConstraint;
import org.apache.jackrabbit.core.util.DOMBuilder;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.version.OnParentVersionAction;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.OutputStream;

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
            OutputStream xml, NodeTypeDef[] types, NamespaceRegistry registry)
            throws IOException, RepositoryException {
        try {
            NodeTypeWriter writer = new NodeTypeWriter(registry);
            for (int i = 0; i < types.length; i++) {
                writer.addNodeTypeDef(types[i]);
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
        for (int i = 0; i < prefixes.length; i++) {
            if (!"".equals(prefixes[i])) {
                String uri = registry.getURI(prefixes[i]);
                builder.setAttribute("xmlns:" + prefixes[i], uri);
            }
        }

        NamespaceResolver nsResolver = new AdditionalNamespaceResolver(registry);
        resolver = new DefaultNamePathResolver(nsResolver);
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
    private void addNodeTypeDef(NodeTypeDef def)
            throws NamespaceException, RepositoryException {
        builder.startElement(Constants.NODETYPE_ELEMENT);

        // simple attributes
        builder.setAttribute(
                Constants.NAME_ATTRIBUTE, resolver.getJCRName(def.getName()));
        builder.setAttribute(
                Constants.ISMIXIN_ATTRIBUTE, def.isMixin());
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
            for (int i = 0; i < supertypes.length; i++) {
                builder.addContentElement(
                        Constants.SUPERTYPE_ELEMENT,
                        resolver.getJCRName(supertypes[i]));
            }
            builder.endElement();
        }

        // property definitions
        PropDef[] properties = def.getPropertyDefs();
        for (int i = 0; i < properties.length; i++) {
            addPropDef(properties[i]);
        }

        // child node definitions
        NodeDef[] nodes = def.getChildNodeDefs();
        for (int i = 0; i < nodes.length; i++) {
            addChildNodeDef(nodes[i]);
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
    private void addPropDef(PropDef def)
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
                Constants.REQUIREDTYPE_ATTRIBUTE,
                PropertyType.nameFromValue(def.getRequiredType()));

        // value constraints
        ValueConstraint[] constraints = def.getValueConstraints();
        if (constraints != null && constraints.length > 0) {
            builder.startElement(Constants.VALUECONSTRAINTS_ELEMENT);
            for (int i = 0; i < constraints.length; i++) {
                builder.addContentElement(
                        Constants.VALUECONSTRAINT_ELEMENT,
                        constraints[i].getDefinition(resolver));
            }
            builder.endElement();
        }

        // default values
        InternalValue[] defaults = def.getDefaultValues();
        if (defaults != null && defaults.length > 0) {
            builder.startElement(Constants.DEFAULTVALUES_ELEMENT);
            for (int i = 0; i < defaults.length; i++) {
                builder.addContentElement(
                        Constants.DEFAULTVALUE_ELEMENT,
                        defaults[i].toJCRValue(resolver).getString());
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
    private void addChildNodeDef(NodeDef def)
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
        for (int i = 0; i < requiredTypes.length; i++) {
            builder.addContentElement(
                    Constants.REQUIREDPRIMARYTYPE_ELEMENT,
                    resolver.getJCRName(requiredTypes[i]));
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
