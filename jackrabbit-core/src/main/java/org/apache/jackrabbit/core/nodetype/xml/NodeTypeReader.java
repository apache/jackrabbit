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

import org.apache.jackrabbit.core.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.ItemDef;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.core.nodetype.ValueConstraint;
import org.apache.jackrabbit.core.util.DOMWalker;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.jackrabbit.value.ValueFactoryImpl;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.version.OnParentVersionAction;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

/**
 * Node type definition reader. This class is used to read the
 * persistent node type definition files used by Jackrabbit.
 */
public class NodeTypeReader {

    /**
     * Reads a node type definition file. The file contents are read from
     * the given input stream and the parsed node type definitions are
     * returned.
     *
     * @param xml XML input stream
     * @return node type definitions
     * @throws IOException                 if the node type definitions
     *                                     cannot be read
     * @throws InvalidNodeTypeDefException if the node type definition
     *                                     format is invalid
     */
    public static NodeTypeDef[] read(InputStream xml)
            throws IOException, InvalidNodeTypeDefException {
        try {
            NodeTypeReader reader = new NodeTypeReader(xml);
            return reader.getNodeTypeDefs();
        } catch (NameException e) {
            throw new InvalidNodeTypeDefException(
                    "Invalid namespace reference in a node type definition", e);
        } catch (NamespaceException e) {
            throw new InvalidNodeTypeDefException(
                    "Invalid namespace reference in a node type definition", e);
        }
    }

    /** The node type document walker. */
    private final DOMWalker walker;

    /** The namespaces associated with the node type XML document. */
    private final Properties namespaces;

    /** The name, path resolver. */
    private final NamePathResolver resolver;

    /**
     * Creates a node type definition file reader.
     *
     * @param xml node type definition file
     * @throws IOException if the node type definition file cannot be read
     */
    public NodeTypeReader(InputStream xml) throws IOException {
        walker = new DOMWalker(xml);
        namespaces = walker.getNamespaces();
        NamespaceResolver nsResolver = new AdditionalNamespaceResolver(namespaces);
        resolver = new DefaultNamePathResolver(nsResolver);
    }

    /**
     * Returns the namespaces declared in the node type definition
     * file.
     */
    public Properties getNamespaces() {
        return namespaces;
    }

    /**
     * Returns all node type definitions specified by node type elements
     * under the current element.
     *
     * @return node type definitions
     * @throws InvalidNodeTypeDefException if a definition is invalid
     * @throws NameException               if a definition contains an
     *                                     illegal name
     */
    public NodeTypeDef[] getNodeTypeDefs()
            throws InvalidNodeTypeDefException, NameException, NamespaceException {
        List defs = new ArrayList();
        while (walker.iterateElements(Constants.NODETYPE_ELEMENT)) {
            defs.add(getNodeTypeDef());
        }
        return (NodeTypeDef[]) defs.toArray(new NodeTypeDef[defs.size()]);
    }

    /**
     * Returns the node type definition specified by the current element.
     *
     * @return node type definition
     * @throws InvalidNodeTypeDefException if the definition is invalid
     * @throws NameException               if the definition contains an
     *                                     illegal name
     */
    private NodeTypeDef getNodeTypeDef()
            throws InvalidNodeTypeDefException, NameException, NamespaceException {
        NodeTypeDef type = new NodeTypeDef();

        type.setName(resolver.getQName(
                walker.getAttribute(Constants.NAME_ATTRIBUTE)));
        type.setMixin(Boolean.valueOf(
                walker.getAttribute(Constants.ISMIXIN_ATTRIBUTE))
                .booleanValue());
        type.setOrderableChildNodes(Boolean.valueOf(
                walker.getAttribute(Constants.HASORDERABLECHILDNODES_ATTRIBUTE))
                .booleanValue());
        String primaryItemName =
            walker.getAttribute(Constants.PRIMARYITEMNAME_ATTRIBUTE);
        if (primaryItemName != null && primaryItemName.length() > 0) {
            type.setPrimaryItemName(
                    resolver.getQName(primaryItemName));
        }

        // supertype declarations
        if (walker.enterElement(Constants.SUPERTYPES_ELEMENT)) {
            List supertypes = new ArrayList();
            while (walker.iterateElements(Constants.SUPERTYPE_ELEMENT)) {
                supertypes.add(
                        resolver.getQName(walker.getContent()));
            }
            type.setSupertypes((Name[])
                    supertypes.toArray(new Name[supertypes.size()]));
            walker.leaveElement();
        }

        // property definitions
        List properties = new ArrayList();
        while (walker.iterateElements(Constants.PROPERTYDEFINITION_ELEMENT)) {
            PropDefImpl def = getPropDef();
            def.setDeclaringNodeType(type.getName());
            properties.add(def);
        }
        type.setPropertyDefs((PropDef[])
                properties.toArray(new PropDef[properties.size()]));

        // child node definitions
        List nodes = new ArrayList();
        while (walker.iterateElements(Constants.CHILDNODEDEFINITION_ELEMENT)) {
            NodeDefImpl def = getChildNodeDef();
            def.setDeclaringNodeType(type.getName());
            nodes.add(def);
        }
        type.setChildNodeDefs((NodeDef[])
                nodes.toArray(new NodeDef[nodes.size()]));

        return type;
    }

    /**
     * Returns the property definition specified by the current element.
     *
     * @return property definition
     * @throws InvalidNodeTypeDefException if the definition is invalid
     * @throws NameException               if the definition contains an
     *                                     illegal name
     */
    private PropDefImpl getPropDef()
            throws InvalidNodeTypeDefException, NameException, NamespaceException {
        PropDefImpl def = new PropDefImpl();
        String name = walker.getAttribute(Constants.NAME_ATTRIBUTE);
        if (name.equals("*")) {
            def.setName(ItemDef.ANY_NAME);
        } else {
            def.setName(resolver.getQName(name));
        }

        // simple attributes
        def.setAutoCreated(Boolean.valueOf(
                walker.getAttribute(Constants.AUTOCREATED_ATTRIBUTE))
                .booleanValue());
        def.setMandatory(Boolean.valueOf(
                walker.getAttribute(Constants.MANDATORY_ATTRIBUTE))
                .booleanValue());
        def.setProtected(Boolean.valueOf(
                walker.getAttribute(Constants.PROTECTED_ATTRIBUTE))
                .booleanValue());
        def.setOnParentVersion(OnParentVersionAction.valueFromName(
                walker.getAttribute(Constants.ONPARENTVERSION_ATTRIBUTE)));
        def.setMultiple(Boolean.valueOf(
                walker.getAttribute(Constants.MULTIPLE_ATTRIBUTE))
                .booleanValue());
        def.setRequiredType(PropertyType.valueFromName(
                walker.getAttribute(Constants.REQUIREDTYPE_ATTRIBUTE)));

        // value constraints
        if (walker.enterElement(Constants.VALUECONSTRAINTS_ELEMENT)) {
            List constraints = new ArrayList();
            int type = def.getRequiredType();
            while (walker.iterateElements(Constants.VALUECONSTRAINT_ELEMENT)) {
                String constraint = walker.getContent();
                try {
                    constraints.add(ValueConstraint.create(
                            type, constraint.trim(), resolver));
                } catch (InvalidConstraintException e) {
                    throw new InvalidNodeTypeDefException(
                            "Invalid value constraint " + constraint, e);
                }
            }
            def.setValueConstraints((ValueConstraint[]) constraints.toArray(
                    new ValueConstraint[constraints.size()]));
            walker.leaveElement();
        }

        // default values
        if (walker.enterElement(Constants.DEFAULTVALUES_ELEMENT)) {
            List values = new ArrayList();
            int type = def.getRequiredType();
            if (type == PropertyType.UNDEFINED) {
                type = PropertyType.STRING;
            }
            while (walker.iterateElements(Constants.DEFAULTVALUE_ELEMENT)) {
                String value = walker.getContent();
                try {
                    values.add(InternalValue.create(ValueHelper.convert(
                            value, type, ValueFactoryImpl.getInstance()), resolver));
                } catch (RepositoryException e) {
                    throw new InvalidNodeTypeDefException(
                            "Unable to create default value: " + value, e);
                }
            }
            def.setDefaultValues((InternalValue[])
                    values.toArray(new InternalValue[values.size()]));
            walker.leaveElement();
        }

        return def;
    }

    /**
     * Returns the child node definition specified by the current element.
     *
     * @return child node definition
     * @throws NameException if the definition contains an illegal name
     */
    private NodeDefImpl getChildNodeDef() throws NameException, NamespaceException {
        NodeDefImpl def = new NodeDefImpl();
        String name = walker.getAttribute(Constants.NAME_ATTRIBUTE);
        if (name.equals("*")) {
            def.setName(ItemDef.ANY_NAME);
        } else {
            def.setName(resolver.getQName(name));
        }

        // simple attributes
        def.setAutoCreated(Boolean.valueOf(
                walker.getAttribute(Constants.AUTOCREATED_ATTRIBUTE))
                .booleanValue());
        def.setMandatory(Boolean.valueOf(
                walker.getAttribute(Constants.MANDATORY_ATTRIBUTE))
                .booleanValue());
        def.setProtected(Boolean.valueOf(
                walker.getAttribute(Constants.PROTECTED_ATTRIBUTE))
                .booleanValue());
        def.setOnParentVersion(OnParentVersionAction.valueFromName(
                walker.getAttribute(Constants.ONPARENTVERSION_ATTRIBUTE)));
        def.setAllowsSameNameSiblings(Boolean.valueOf(
                walker.getAttribute(Constants.SAMENAMESIBLINGS_ATTRIBUTE))
                .booleanValue());

        // default primary type
        String type =
            walker.getAttribute(Constants.DEFAULTPRIMARYTYPE_ATTRIBUTE);
        if (type != null && type.length() > 0) {
            def.setDefaultPrimaryType(resolver.getQName(type));
        }

        // required primary types
        if (walker.enterElement(Constants.REQUIREDPRIMARYTYPES_ELEMENT)) {
            List types = new ArrayList();
            while (walker.iterateElements(Constants.REQUIREDPRIMARYTYPE_ELEMENT)) {
                types.add(resolver.getQName(walker.getContent()));
            }
            def.setRequiredPrimaryTypes(
                    (Name[]) types.toArray(new Name[types.size()]));
            walker.leaveElement();
        } else {
            /* Default to nt:base?
            throw new InvalidNodeTypeDefException(
                    "Required primary type(s) not defined for child node "
                    + def.getName() + " of node type "
                    + def.getDeclaringNodeType());
            */
        }

        return def;
    }

}
