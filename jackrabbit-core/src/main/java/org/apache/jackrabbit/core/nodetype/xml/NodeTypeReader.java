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

import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.util.DOMWalker;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.value.InternalValueFactory;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QPropertyDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.QNodeTypeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.value.ValueHelper;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import javax.jcr.ValueFactory;
import javax.jcr.Value;
import javax.jcr.query.qom.QueryObjectModelConstants;
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
    public static QNodeTypeDefinition[] read(InputStream xml)
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

    private final ValueFactory valueFactory;

    private final QValueFactory qValueFactory = InternalValueFactory.getInstance();

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
        valueFactory = new ValueFactoryQImpl(qValueFactory, resolver);
    }

    /**
     * Returns the namespaces declared in the node type definition
     * file.
     * @return the namespaces
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
     * @throws NamespaceException if a namespace is not defined
     */
    public QNodeTypeDefinition[] getNodeTypeDefs()
            throws InvalidNodeTypeDefException, NameException, NamespaceException {
        List<QNodeTypeDefinition> defs = new ArrayList<QNodeTypeDefinition>();
        while (walker.iterateElements(Constants.NODETYPE_ELEMENT)) {
            defs.add(getNodeTypeDef());
        }
        return defs.toArray(new QNodeTypeDefinition[defs.size()]);
    }

    /**
     * Returns the node type definition specified by the current element.
     *
     * @return node type definition
     * @throws InvalidNodeTypeDefException if the definition is invalid
     * @throws NameException               if the definition contains an
     *                                     illegal name
     * @throws NamespaceException if a namespace is not defined
     */
    private QNodeTypeDefinition getNodeTypeDef()
            throws InvalidNodeTypeDefException, NameException, NamespaceException {
        QNodeTypeDefinitionBuilder type = new QNodeTypeDefinitionBuilder();

        type.setName(resolver.getQName(
                walker.getAttribute(Constants.NAME_ATTRIBUTE)));
        type.setMixin(Boolean.valueOf(
                walker.getAttribute(Constants.ISMIXIN_ATTRIBUTE)));
        type.setOrderableChildNodes(Boolean.valueOf(
                walker.getAttribute(Constants.HASORDERABLECHILDNODES_ATTRIBUTE)));
        type.setAbstract(Boolean.valueOf(
                walker.getAttribute(Constants.ISABSTRACT_ATTRIBUTE)));
        if (walker.getAttribute(Constants.ISQUERYABLE_ATTRIBUTE) != null) {
            type.setQueryable(Boolean.valueOf(
                    walker.getAttribute(Constants.ISQUERYABLE_ATTRIBUTE)));
        }
        String primaryItemName =
            walker.getAttribute(Constants.PRIMARYITEMNAME_ATTRIBUTE);
        if (primaryItemName != null && primaryItemName.length() > 0) {
            type.setPrimaryItemName(
                    resolver.getQName(primaryItemName));
        }

        // supertype declarations
        if (walker.enterElement(Constants.SUPERTYPES_ELEMENT)) {
            List<Name> supertypes = new ArrayList<Name>();
            while (walker.iterateElements(Constants.SUPERTYPE_ELEMENT)) {
                supertypes.add(
                        resolver.getQName(walker.getContent()));
            }
            type.setSupertypes(supertypes.toArray(new Name[supertypes.size()]));
            walker.leaveElement();
        }

        // property definitions
        List<QPropertyDefinition> properties = new ArrayList<QPropertyDefinition>();
        while (walker.iterateElements(Constants.PROPERTYDEFINITION_ELEMENT)) {
            QPropertyDefinitionBuilder def = getPropDef();
            def.setDeclaringNodeType(type.getName());
            properties.add(def.build());
        }
        type.setPropertyDefs(properties.toArray(new QPropertyDefinition[properties.size()]));

        // child node definitions
        List<QNodeDefinition> nodes = new ArrayList<QNodeDefinition>();
        while (walker.iterateElements(Constants.CHILDNODEDEFINITION_ELEMENT)) {
            QNodeDefinitionBuilder def = getChildNodeDef();
            def.setDeclaringNodeType(type.getName());
            nodes.add(def.build());
        }
        type.setChildNodeDefs(nodes.toArray(new QNodeDefinition[nodes.size()]));

        return type.build();
    }

    /**
     * Returns the property definition specified by the current element.
     *
     * @return property definition
     * @throws InvalidNodeTypeDefException if the definition is invalid
     * @throws NameException               if the definition contains an
     *                                     illegal name
     * @throws NamespaceException if a namespace is not defined
     */
    private QPropertyDefinitionBuilder getPropDef()
            throws InvalidNodeTypeDefException, NameException, NamespaceException {
        QPropertyDefinitionBuilder def = new QPropertyDefinitionBuilder();
        String name = walker.getAttribute(Constants.NAME_ATTRIBUTE);
        if (name.equals("*")) {
            def.setName(NameConstants.ANY_NAME);
        } else {
            def.setName(resolver.getQName(name));
        }

        // simple attributes
        def.setAutoCreated(Boolean.valueOf(
                walker.getAttribute(Constants.AUTOCREATED_ATTRIBUTE)));
        def.setMandatory(Boolean.valueOf(
                walker.getAttribute(Constants.MANDATORY_ATTRIBUTE)));
        def.setProtected(Boolean.valueOf(
                walker.getAttribute(Constants.PROTECTED_ATTRIBUTE)));
        def.setOnParentVersion(OnParentVersionAction.valueFromName(
                walker.getAttribute(Constants.ONPARENTVERSION_ATTRIBUTE)));
        def.setMultiple(Boolean.valueOf(
                walker.getAttribute(Constants.MULTIPLE_ATTRIBUTE)));
        def.setFullTextSearchable(Boolean.valueOf(
                walker.getAttribute(Constants.ISFULLTEXTSEARCHABLE_ATTRIBUTE)));
        def.setQueryOrderable(Boolean.valueOf(
                walker.getAttribute(Constants.ISQUERYORDERABLE_ATTRIBUTE)));
        String s = walker.getAttribute(Constants.AVAILABLEQUERYOPERATORS_ATTRIBUTE);
        if (s != null && s.length() > 0) {
            String[] ops = s.split(" ");
            List<String> queryOps = new ArrayList<String>();
            for (String op1 : ops) {
                String op = op1.trim();
                if (op.equals(Constants.EQ_ENTITY)) {
                    queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO);
                } else if (op.equals(Constants.NE_ENTITY)) {
                    queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO);
                } else if (op.equals(Constants.LT_ENTITY)) {
                    queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN);
                } else if (op.equals(Constants.LE_ENTITY)) {
                    queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO);
                } else if (op.equals(Constants.GT_ENTITY)) {
                    queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN);
                } else if (op.equals(Constants.GE_ENTITY)) {
                    queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO);
                } else if (op.equals(Constants.LIKE_ENTITY)) {
                    queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_LIKE);
                } else {
                    throw new InvalidNodeTypeDefException("'" + op + "' is not a valid query operator");
                }
            }
            def.setAvailableQueryOperators(queryOps.toArray(new String[queryOps.size()]));

        }
        def.setRequiredType(PropertyType.valueFromName(
                walker.getAttribute(Constants.REQUIREDTYPE_ATTRIBUTE)));

        // value constraints
        if (walker.enterElement(Constants.VALUECONSTRAINTS_ELEMENT)) {
            List<QValueConstraint> constraints = new ArrayList<QValueConstraint>();
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
            def.setValueConstraints(constraints.toArray(
                    new QValueConstraint[constraints.size()]));
            walker.leaveElement();
        }

        // default values
        if (walker.enterElement(Constants.DEFAULTVALUES_ELEMENT)) {
            List<InternalValue> values = new ArrayList<InternalValue>();
            int type = def.getRequiredType();
            if (type == PropertyType.UNDEFINED) {
                type = PropertyType.STRING;
            }
            while (walker.iterateElements(Constants.DEFAULTVALUE_ELEMENT)) {
                String value = walker.getContent();
                try {
                    Value v = ValueHelper.convert(value, type, valueFactory);
                    values.add((InternalValue) ValueFormat.getQValue(v, resolver, qValueFactory));
                } catch (RepositoryException e) {
                    throw new InvalidNodeTypeDefException(
                            "Unable to create default value: " + value, e);
                }
            }
            def.setDefaultValues(values.toArray(new InternalValue[values.size()]));
            walker.leaveElement();
        }

        return def;
    }

    /**
     * Returns the child node definition specified by the current element.
     *
     * @return child node definition
     * @throws NameException if the definition contains an illegal name
     * @throws NamespaceException if a namespace is not defined
     */
    private QNodeDefinitionBuilder getChildNodeDef() throws NameException, NamespaceException {
        QNodeDefinitionBuilder def = new QNodeDefinitionBuilder();
        String name = walker.getAttribute(Constants.NAME_ATTRIBUTE);
        if (name.equals("*")) {
            def.setName(NameConstants.ANY_NAME);
        } else {
            def.setName(resolver.getQName(name));
        }

        // simple attributes
        def.setAutoCreated(Boolean.valueOf(
                walker.getAttribute(Constants.AUTOCREATED_ATTRIBUTE)));
        def.setMandatory(Boolean.valueOf(
                walker.getAttribute(Constants.MANDATORY_ATTRIBUTE)));
        def.setProtected(Boolean.valueOf(
                walker.getAttribute(Constants.PROTECTED_ATTRIBUTE)));
        def.setOnParentVersion(OnParentVersionAction.valueFromName(
                walker.getAttribute(Constants.ONPARENTVERSION_ATTRIBUTE)));
        def.setAllowsSameNameSiblings(Boolean.valueOf(
                walker.getAttribute(Constants.SAMENAMESIBLINGS_ATTRIBUTE)));

        // default primary type
        String type =
            walker.getAttribute(Constants.DEFAULTPRIMARYTYPE_ATTRIBUTE);
        if (type != null && type.length() > 0) {
            def.setDefaultPrimaryType(resolver.getQName(type));
        }

        // required primary types
        if (walker.enterElement(Constants.REQUIREDPRIMARYTYPES_ELEMENT)) {
            List<Name> types = new ArrayList<Name>();
            while (walker.iterateElements(Constants.REQUIREDPRIMARYTYPE_ELEMENT)) {
                types.add(resolver.getQName(walker.getContent()));
            }
            def.setRequiredPrimaryTypes(types.toArray(new Name[types.size()]));
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
