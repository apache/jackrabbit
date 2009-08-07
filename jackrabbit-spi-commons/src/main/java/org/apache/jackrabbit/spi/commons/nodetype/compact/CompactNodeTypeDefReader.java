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
package org.apache.jackrabbit.spi.commons.nodetype.compact;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.spi.commons.nodetype.compact.QNodeTypeDefinitionsBuilder.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.compact.QNodeTypeDefinitionsBuilder.QNodeTypeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.compact.QNodeTypeDefinitionsBuilder.QPropertyDefinitionBuilder;
import org.apache.jackrabbit.util.ISO9075;

/**
 * CompactNodeTypeDefReader. Parses node type definitions written in the compact
 * node type definition format and returns a list of QNodeTypeDefinition objects that
 * can then be used to register node types.
 * <p/>
 * The EBNF grammar of the compact node type definition:<br>
 * <pre>
 * cnd ::= ns_mapping* node_type_def+
 *
 * ns_mapping ::= "&lt;" prefix "=" namespace "&gt;"
 *
 * prefix ::= string
 *
 * namespace ::= string
 *
 * node_type_def ::= node_type_name [super_types] [options] {property_def | node_def}
 *
 * node_type_name ::= "[" string "]"
 *
 * super_types ::= "&gt;" string_list
 *
 * options ::= orderable_opt | mixin_opt | orderable_opt mixin_opt | mixin_opt orderable_opt
 *
 * orderable_opt ::= "orderable" | "ord" | "o"
 *
 * mixin_opt ::= "mixin" | "mix" | "m"
 *
 * property_def ::= "-" property_name [property_type_decl] [default_values] [attributes] [value_constraints]
 *
 * property_name ::= string
 *
 * property_type_decl ::= "(" property_type ")"
 *
 * property_type ::= "STRING" | "String |"string" |
 *                   "BINARY" | "Binary" | "binary" |
 *                   "LONG" | "Long" | "long" |
 *                   "DOUBLE" | "Double" | "double" |
 *                   "BOOLEAN" | "Boolean" | "boolean" |
 *                   "DATE" | "Date" | "date" |
 *                   "NAME | "Name | "name |
 *                   "PATH" | "Path" | "path" |
 *                   "REFERENCE" | "Reference" | "reference" |
 *                   "UNDEFINED" | "Undefined" | "undefined" | "*"
 *
 *
 * default_values ::= "=" string_list
 *
 * value_constraints ::= "&lt;" string_list
 *
 * node_def ::= "+" node_name [required_types] [default_type] [attributes]
 *
 * node_name ::= string
 *
 * required_types ::= "(" string_list ")"
 *
 * default_type ::= "=" string
 *
 * attributes ::= "primary" | "pri" | "!" |
 *                "autocreated" | "aut" | "a" |
 *                "mandatory" | "man" | "m" |
 *                "protected" | "pro" | "p" |
 *                "multiple" | "mul" | "*" |
 *                "COPY" | "Copy" | "copy" |
 *                "VERSION" | "Version" | "version" |
 *                "INITIALIZE" | "Initialize" | "initialize" |
 *                "COMPUTE" | "Compute" | "compute" |
 *                "IGNORE" | "Ignore" | "ignore" |
 *                "ABORT" | "Abort" | "abort"
 *
 * string_list ::= string {"," string}
 *
 * string ::= quoted_string | unquoted_string
 *
 * quoted_string :: = "'" unquoted_string "'"
 *
 * unquoted_string ::= [A-Za-z0-9:_]+
 * </pre>
 */
public class CompactNodeTypeDefReader {

    /**
     * Empty array of value constraints
     */
    private final static String[] EMPTY_VALUE_CONSTRAINTS = new String[0];

    /**
     * the list of parsed QNodeTypeDefinition
     */
    private final List nodeTypeDefs = new LinkedList();

    /**
     * the current namespace mapping
     */
    private final NamespaceMapping nsMapping;

    /**
     * Name and Path resolver
     */
    private final NamePathResolver resolver;

    /**
     * the underlying lexer
     */
    private final Lexer lexer;

    /**
     * the current token
     */
    private String currentToken;

    /**
     * The builder for QNodeTypeDefinitions
     */
    private final QNodeTypeDefinitionsBuilder builder;

    /**
     * Creates a new CND reader.
     * @param r
     * @param systemId
     * @param builder
     * @throws ParseException
     */
    public CompactNodeTypeDefReader(Reader r, String systemId, QNodeTypeDefinitionsBuilder builder) throws ParseException {
        this(r, systemId, new NamespaceMapping(), builder);
    }


    /**
     * Creates a new CND reader.
     * @param r
     * @param builder
     * @throws ParseException
     */
    public CompactNodeTypeDefReader(Reader r, String systemId, NamespaceMapping mapping,
            QNodeTypeDefinitionsBuilder builder) throws ParseException {

        this.builder = builder;
        lexer = new Lexer(r, systemId);
        this.nsMapping = mapping;
        this.resolver = new DefaultNamePathResolver(nsMapping);
        nextToken();
        parse();
    }

    /**
     * Returns the list of parsed QNodeTypeDefinition definitions.
     *
     * @return a List of QNodeTypeDefinition objects
     */
    public List getNodeTypeDefs() {
        return nodeTypeDefs;
    }

    /**
     * Returns the namespace mapping.
     *
     * @return a NamespaceMapping object.
     */
    public NamespaceMapping getNamespaceMapping() {
        return nsMapping;
    }

    /**
     * Parses the definition
     *
     * @throws ParseException
     */
    private void parse() throws ParseException {
        while (!currentTokenEquals(Lexer.EOF)) {
            if (!doNameSpace()) {
                break;
            }
        }
        while (!currentTokenEquals(Lexer.EOF)) {
            QNodeTypeDefinitionBuilder ntd = builder.newQNodeTypeDefinition();
            ntd.setOrderableChildNodes(false);
            ntd.setMixin(false);
            ntd.setPrimaryItemName(null);
            doNodeTypeName(ntd);
            doSuperTypes(ntd);
            doOptions(ntd);
            doItemDefs(ntd);
            nodeTypeDefs.add(ntd.build());
        }
    }



    /**
     * processes the namespace declaration
     *
     * @return
     * @throws ParseException
     */
    private boolean doNameSpace() throws ParseException {
        if (!currentTokenEquals('<')) {
            return false;
        }
        nextToken();
        String prefix = currentToken;
        nextToken();
        if (!currentTokenEquals('=')) {
            lexer.fail("Missing = in namespace decl.");
        }
        nextToken();
        String uri = currentToken;
        nextToken();
        if (!currentTokenEquals('>')) {
            lexer.fail("Missing > in namespace decl.");
        }
        try {
            nsMapping.setMapping(prefix, uri);
        } catch (NamespaceException e) {
            // ignore
        }
        nextToken();
        return true;
    }

    /**
     * processes the nodetype name
     *
     * @param ntd
     * @throws ParseException
     */
    private void doNodeTypeName(QNodeTypeDefinitionBuilder ntd) throws ParseException {
        if (!currentTokenEquals(Lexer.BEGIN_NODE_TYPE_NAME)) {
            lexer.fail("Missing '" + Lexer.BEGIN_NODE_TYPE_NAME + "' delimiter for beginning of node type name");
        }
        nextToken();
        ntd.setName(toName(currentToken));

        nextToken();
        if (!currentTokenEquals(Lexer.END_NODE_TYPE_NAME)) {
            lexer.fail("Missing '" + Lexer.END_NODE_TYPE_NAME + "' delimiter for end of node type name, found " + currentToken);
        }
        nextToken();
    }

    /**
     * processes the superclasses
     *
     * @param ntd
     * @throws ParseException
     */
    private void doSuperTypes(QNodeTypeDefinitionBuilder ntd) throws ParseException {
        // a set would be nicer here, in case someone defines a supertype twice.
        // but due to issue [JCR-333], the resulting node type definition is
        // not symmetric anymore and the tests will fail.
        ArrayList supertypes = new ArrayList();
        if (currentTokenEquals(Lexer.EXTENDS))
            do {
                nextToken();
                supertypes.add(toName(currentToken));
                nextToken();
            } while (currentTokenEquals(Lexer.LIST_DELIMITER));

        ntd.setSupertypes((Name[]) supertypes.toArray(new Name[0]));
    }

    /**
     * processes the options
     *
     * @param ntd
     * @throws ParseException
     */
    private void doOptions(QNodeTypeDefinitionBuilder ntd) throws ParseException {
        if (currentTokenEquals(Lexer.ORDERABLE)) {
            ntd.setOrderableChildNodes(true);
            nextToken();
            if (currentTokenEquals(Lexer.MIXIN)) {
                ntd.setMixin(true);
                nextToken();
            }
        } else if (currentTokenEquals(Lexer.MIXIN)) {
            ntd.setMixin(true);
            nextToken();
            if (currentTokenEquals(Lexer.ORDERABLE)) {
                ntd.setOrderableChildNodes(true);
                nextToken();
            }
        }
    }

    /**
     * processes the item definitions
     *
     * @param ntd
     * @throws ParseException
     */
    private void doItemDefs(QNodeTypeDefinitionBuilder ntd) throws ParseException {
        List propertyDefinitions = new ArrayList();
        List nodeDefinitions = new ArrayList();
        while (currentTokenEquals(Lexer.PROPERTY_DEFINITION) || currentTokenEquals(Lexer.CHILD_NODE_DEFINITION)) {
            if (currentTokenEquals(Lexer.PROPERTY_DEFINITION)) {
                QPropertyDefinitionBuilder pd = ntd.newQPropertyDefinition();

                pd.setAutoCreated(false);
                pd.setDeclaringNodeType(ntd.getName());
                pd.setDefaultValues(null);
                pd.setMandatory(false);
                pd.setMultiple(false);
                pd.setOnParentVersion(OnParentVersionAction.COPY);
                pd.setProtected(false);
                pd.setRequiredType(PropertyType.STRING);
                pd.setValueConstraints(EMPTY_VALUE_CONSTRAINTS);

                nextToken();
                doPropertyDefinition(pd, ntd);
                propertyDefinitions.add(pd.build());

            } else if (currentTokenEquals(Lexer.CHILD_NODE_DEFINITION)) {
                QNodeDefinitionBuilder nd = ntd.newQNodeDefinitionBuilder();

                nd.setAllowsSameNameSiblings(false);
                nd.setAutoCreated(false);
                nd.setDeclaringNodeType(ntd.getName());
                nd.setMandatory(false);
                nd.setOnParentVersion(OnParentVersionAction.COPY);
                nd.setProtected(false);
                nd.setDefaultPrimaryType(null);
                nd.setRequiredPrimaryTypes(new Name[]{NameConstants.NT_BASE});

                nextToken();
                doChildNodeDefinition(nd, ntd);
                nodeDefinitions.add(nd.build());
            }
        }

        ntd.setPropertyDefs((QPropertyDefinition[]) propertyDefinitions
                .toArray(new QPropertyDefinition[0]));

        ntd.setChildNodeDefs((QNodeDefinition[]) nodeDefinitions.toArray(new QNodeDefinition[0]));
    }

    /**
     * processes the property definition
     *
     * @param pd
     * @param ntd
     * @throws ParseException
     */
    private void doPropertyDefinition(QPropertyDefinitionBuilder pd, QNodeTypeDefinitionBuilder ntd)
            throws ParseException {
        if (currentToken.equals("*")) {
            pd.setName(NameConstants.ANY_NAME);
        } else {
            pd.setName(toName(currentToken));
        }
        nextToken();
        doPropertyType(pd);
        doPropertyDefaultValue(pd);
        doPropertyAttributes(pd, ntd);
        doPropertyValueConstraints(pd);
    }

    /**
     * processes the property type
     *
     * @param pd
     * @throws ParseException
     */
    private void doPropertyType(QPropertyDefinitionBuilder pd) throws ParseException {
        if (!currentTokenEquals(Lexer.BEGIN_TYPE)) {
            return;
        }
        nextToken();
        if (currentTokenEquals(Lexer.STRING)) {
            pd.setRequiredType(PropertyType.STRING);
        } else if (currentTokenEquals(Lexer.BINARY)) {
            pd.setRequiredType(PropertyType.BINARY);
        } else if (currentTokenEquals(Lexer.LONG)) {
            pd.setRequiredType(PropertyType.LONG);
        } else if (currentTokenEquals(Lexer.DOUBLE)) {
            pd.setRequiredType(PropertyType.DOUBLE);
        } else if (currentTokenEquals(Lexer.BOOLEAN)) {
            pd.setRequiredType(PropertyType.BOOLEAN);
        } else if (currentTokenEquals(Lexer.DATE)) {
            pd.setRequiredType(PropertyType.DATE);
        } else if (currentTokenEquals(Lexer.NAME)) {
            pd.setRequiredType(PropertyType.NAME);
        } else if (currentTokenEquals(Lexer.PATH)) {
            pd.setRequiredType(PropertyType.PATH);
        } else if (currentTokenEquals(Lexer.REFERENCE)) {
            pd.setRequiredType(PropertyType.REFERENCE);
        } else if (currentTokenEquals(Lexer.UNDEFINED)) {
            pd.setRequiredType(PropertyType.UNDEFINED);
        } else {
            lexer.fail("Unkown property type '" + currentToken + "' specified");
        }
        nextToken();
        if (!currentTokenEquals(Lexer.END_TYPE)) {
            lexer.fail("Missing '" + Lexer.END_TYPE + "' delimiter for end of property type");
        }
        nextToken();
    }

    /**
     * processes the property attributes
     *
     * @param pd
     * @param ntd
     * @throws ParseException
     */
    private void doPropertyAttributes(QPropertyDefinitionBuilder pd, QNodeTypeDefinitionBuilder ntd) throws ParseException {
        while (currentTokenEquals(Lexer.ATTRIBUTE)) {
            if (currentTokenEquals(Lexer.PRIMARY)) {
                if (ntd.getPrimaryItemName() != null) {
                    String name = null;
                    try {
                        name = resolver.getJCRName(ntd.getName());
                    } catch (NamespaceException e) {
                        // Should never happen, checked earlier
                    }
                    lexer.fail("More than one primary item specified in node type '" + name + "'");
                }
                ntd.setPrimaryItemName(pd.getName());
            } else if (currentTokenEquals(Lexer.AUTOCREATED)) {
                pd.setAutoCreated(true);
            } else if (currentTokenEquals(Lexer.MANDATORY)) {
                pd.setMandatory(true);
            } else if (currentTokenEquals(Lexer.PROTECTED)) {
                pd.setProtected(true);
            } else if (currentTokenEquals(Lexer.MULTIPLE)) {
                pd.setMultiple(true);
            } else if (currentTokenEquals(Lexer.COPY)) {
                pd.setOnParentVersion(OnParentVersionAction.COPY);
            } else if (currentTokenEquals(Lexer.VERSION)) {
                pd.setOnParentVersion(OnParentVersionAction.VERSION);
            } else if (currentTokenEquals(Lexer.INITIALIZE)) {
                pd.setOnParentVersion(OnParentVersionAction.INITIALIZE);
            } else if (currentTokenEquals(Lexer.COMPUTE)) {
                pd.setOnParentVersion(OnParentVersionAction.COMPUTE);
            } else if (currentTokenEquals(Lexer.IGNORE)) {
                pd.setOnParentVersion(OnParentVersionAction.IGNORE);
            } else if (currentTokenEquals(Lexer.ABORT)) {
                pd.setOnParentVersion(OnParentVersionAction.ABORT);
            }
            nextToken();
        }
    }

    /**
     * processes the property default values
     *
     * @param pd
     * @throws ParseException
     */
    private void doPropertyDefaultValue(QPropertyDefinitionBuilder pd) throws ParseException {
        if (!currentTokenEquals(Lexer.DEFAULT)) {
            return;
        }
        List defaultValues = new ArrayList();
        do {
            nextToken();
            QValue value = null;
            try {
                value = pd.createValue(currentToken, resolver);
            } catch (ValueFormatException e) {
                lexer.fail("'" + currentToken + "' is not a valid string representation of a value of type " + pd.getRequiredType());
            } catch (RepositoryException e) {
                lexer.fail("An error occured during value conversion of '" + currentToken + "'");
            }
            defaultValues.add(value);
            nextToken();
        } while (currentTokenEquals(Lexer.LIST_DELIMITER));
        pd.setDefaultValues((QValue[]) defaultValues.toArray(new QValue[0]));
    }

    /**
     * processes the property value constraints
     *
     * @param pd
     * @throws ParseException
     */
    private void doPropertyValueConstraints(QPropertyDefinitionBuilder pd) throws ParseException {
        if (!currentTokenEquals(Lexer.CONSTRAINT)) {
            return;
        }
        List constraints = new ArrayList();
        do {
            nextToken();
            String constraint = null;
            try {
                constraint = pd.createValueConstraint(currentToken, resolver);
            } catch (InvalidConstraintException e) {
                lexer.fail("'" + currentToken + "' is not a valid constraint expression for a value of type " + pd.getRequiredType());
            }
            constraints.add(constraint);
            nextToken();
        } while (currentTokenEquals(Lexer.LIST_DELIMITER));
        pd.setValueConstraints((String[]) constraints.toArray(new String[0]));
    }

    /**
     * processes the childnode definition
     *
     * @param nd
     * @param ntd
     * @throws ParseException
     */
    private void doChildNodeDefinition(QNodeDefinitionBuilder nd, QNodeTypeDefinitionBuilder ntd)
            throws ParseException {
        if (currentTokenEquals('*')) {
            nd.setName(NameConstants.ANY_NAME);
        } else {
            nd.setName(toName(currentToken));
        }
        nextToken();
        doChildNodeRequiredTypes(nd);
        doChildNodeDefaultType(nd);
        doChildNodeAttributes(nd, ntd);
    }

    /**
     * processes the childnode required types
     *
     * @param nd
     * @throws ParseException
     */
    private void doChildNodeRequiredTypes(QNodeDefinitionBuilder nd) throws ParseException {
        if (!currentTokenEquals(Lexer.BEGIN_TYPE)) {
            return;
        }
        List types = new ArrayList();
        do {
            nextToken();
            types.add(toName(currentToken));
            nextToken();
        } while (currentTokenEquals(Lexer.LIST_DELIMITER));
        nd.setRequiredPrimaryTypes((Name[]) types.toArray(new Name[0]));
        nextToken();
    }

    /**
     * processes the childnode default types
     *
     * @param nd
     * @throws ParseException
     */
    private void doChildNodeDefaultType(QNodeDefinitionBuilder nd) throws ParseException {
        if (!currentTokenEquals(Lexer.DEFAULT)) {
            return;
        }
        nextToken();
        nd.setDefaultPrimaryType(toName(currentToken));
        nextToken();
    }

    /**
     * processes the childnode attributes
     *
     * @param nd
     * @param ntd
     * @throws ParseException
     */
    private void doChildNodeAttributes(QNodeDefinitionBuilder nd, QNodeTypeDefinitionBuilder ntd) throws ParseException {
        while (currentTokenEquals(Lexer.ATTRIBUTE)) {
            if (currentTokenEquals(Lexer.PRIMARY)) {
                if (ntd.getPrimaryItemName() != null) {
                    String name = null;
                    try {
                        name = resolver.getJCRName(ntd.getName());
                    } catch (NamespaceException e) {
                        // Should never happen, checked earlier
                    }
                    lexer.fail("More than one primary item specified in node type '" + name + "'");
                }
                ntd.setPrimaryItemName(nd.getName());
            } else if (currentTokenEquals(Lexer.AUTOCREATED)) {
                nd.setAutoCreated(true);
            } else if (currentTokenEquals(Lexer.MANDATORY)) {
                nd.setMandatory(true);
            } else if (currentTokenEquals(Lexer.PROTECTED)) {
                nd.setProtected(true);
            } else if (currentTokenEquals(Lexer.MULTIPLE)) {
                nd.setAllowsSameNameSiblings(true);
            } else if (currentTokenEquals(Lexer.COPY)) {
                nd.setOnParentVersion(OnParentVersionAction.COPY);
            } else if (currentTokenEquals(Lexer.VERSION)) {
                nd.setOnParentVersion(OnParentVersionAction.VERSION);
            } else if (currentTokenEquals(Lexer.INITIALIZE)) {
                nd.setOnParentVersion(OnParentVersionAction.INITIALIZE);
            } else if (currentTokenEquals(Lexer.COMPUTE)) {
                nd.setOnParentVersion(OnParentVersionAction.COMPUTE);
            } else if (currentTokenEquals(Lexer.IGNORE)) {
                nd.setOnParentVersion(OnParentVersionAction.IGNORE);
            } else if (currentTokenEquals(Lexer.ABORT)) {
                nd.setOnParentVersion(OnParentVersionAction.ABORT);
            }
            nextToken();
        }
    }

    /**
     * Converts the given string into a qualified name using the current
     * namespace mapping.
     *
     * @param stringName
     * @return the qualified name
     * @throws ParseException if the conversion fails
     */
    private Name toName(String stringName) throws ParseException {
        try {
            Name n = resolver.getQName(stringName);
            String decodedLocalName = ISO9075.decode(n.getLocalName());
            return builder.createName(n.getNamespaceURI(), decodedLocalName);
        } catch (NameException e) {
            lexer.fail("Error while parsing '" + stringName + "'", e);
            return null;
        } catch (NamespaceException e) {
            lexer.fail("Error while parsing '" + stringName + "'", e);
            return null;
        }
    }

    /**
     * Gets the next token from the underlying lexer.
     *
     * @see Lexer#getNextToken()
     * @throws ParseException if the lexer fails to get the next token.
     */
    private void nextToken() throws ParseException {
        currentToken = lexer.getNextToken();
    }

    /**
     * Checks if the {@link #currentToken} is semantically equal to the given
     * argument.
     *
     * @param s the tokens to compare with
     * @return <code>true</code> if equals; <code>false</code> otherwise.
     */
    private boolean currentTokenEquals(String[] s) {
        for (int i = 0; i < s.length; i++) {
            if (currentToken.equals(s[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the {@link #currentToken} is semantically equal to the given
     * argument.
     *
     * @param c the tokens to compare with
     * @return <code>true</code> if equals; <code>false</code> otherwise.
     */
    private boolean currentTokenEquals(char c) {
        return currentToken.length() == 1 && currentToken.charAt(0) == c;
    }

    /**
     * Checks if the {@link #currentToken} is semantically equal to the given
     * argument.
     *
     * @param s the tokens to compare with
     * @return <code>true</code> if equals; <code>false</code> otherwise.
     */
    private boolean currentTokenEquals(String s) {
        return currentToken.equals(s);
    }

}
