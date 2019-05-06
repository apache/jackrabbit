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
package org.apache.jackrabbit.commons.cnd;

import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory.AbstractNodeDefinitionBuilder;
import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory.AbstractNodeTypeDefinitionBuilder;
import org.apache.jackrabbit.commons.cnd.DefinitionBuilderFactory.AbstractPropertyDefinitionBuilder;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.version.OnParentVersionAction;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

/**
 * CompactNodeTypeDefReader. Parses node type definitions written in the compact
 * node type definition format and provides a list of type definition
 * objects that can then be used to register node types.
 * <p>
 * The CompactNodeTypeDefReader is parameterizable in the type of the node type
 * definition <code>T</code> and the type of the namespace mapping <code>N</code>
 * which the parser should build. For types <code>T</code> and <code>N</code> the
 * parser's constructor takes a {@link DefinitionBuilderFactory} for
 * <code>T</code> and <code>N</code>.
 * <p>
 * The EBNF grammar of the compact node type definition:<br>
 * <pre>
 * Cnd ::= {NamespaceMapping | NodeTypeDef}
 * NamespaceMapping ::= '&lt;' Prefix '=' Uri '&gt;'
 * Prefix ::= String
 * Uri ::= String
 * NodeTypeDef ::= NodeTypeName [Supertypes]
 *                 [NodeTypeAttribute {NodeTypeAttribute}]
 *                 {PropertyDef | ChildNodeDef}
 * NodeTypeName ::= '[' String ']'
 * Supertypes ::= '&gt;' (StringList | '?')
 * NodeTypeAttribute ::= Orderable | Mixin | Abstract | Query |
 *                       PrimaryItem
 * Orderable ::= ('orderable' | 'ord' | 'o') ['?']
 * Mixin ::= ('mixin' | 'mix' | 'm') ['?']
 * Abstract ::= ('abstract' | 'abs' | 'a') ['?']
 * Query ::= ('noquery' | 'nq') | ('query' | 'q' )
 * PrimaryItem ::= ('primaryitem'| '!')(String | '?')
 * PropertyDef ::= PropertyName [PropertyType] [DefaultValues]
 *                 [PropertyAttribute {PropertyAttribute}]
 *                 [ValueConstraints]
 * PropertyName ::= '-' String
 * PropertyType ::= '(' ('STRING' | 'BINARY' | 'LONG' | 'DOUBLE' |
 *                       'BOOLEAN' | 'DATE' | 'NAME' | 'PATH' |
 *                       'REFERENCE' | 'WEAKREFERENCE' |
 *                       'DECIMAL' | 'URI' | 'UNDEFINED' | '*' |
 *                       '?') ')'
 * DefaultValues ::= '=' (StringList | '?')
 * ValueConstraints ::= '&lt;' (StringList | '?')
 * ChildNodeDef ::= NodeName [RequiredTypes] [DefaultType]
 *                  [NodeAttribute {NodeAttribute}]
 * NodeName ::= '+' String
 * RequiredTypes ::= '(' (StringList | '?') ')'
 * DefaultType ::= '=' (String | '?')
 * PropertyAttribute ::= Autocreated | Mandatory | Protected |
 *                       Opv | Multiple | QueryOps | NoFullText |
 *                       NoQueryOrder
 * NodeAttribute ::= Autocreated | Mandatory | Protected |
 *                   Opv | Sns
 * Autocreated ::= ('autocreated' | 'aut' | 'a' )['?']
 * Mandatory ::= ('mandatory' | 'man' | 'm') ['?']
 * Protected ::= ('protected' | 'pro' | 'p') ['?']
 * Opv ::= 'COPY' | 'VERSION' | 'INITIALIZE' | 'COMPUTE' |
 *         'IGNORE' | 'ABORT' | ('OPV' '?')
 * Multiple ::= ('multiple' | 'mul' | '*') ['?']
 * QueryOps ::= ('queryops' | 'qop')
 *              (('''Operator {','Operator}''') | '?')
 * Operator ::= '=' | '&lt;&gt;' | '&lt;' | '&lt;=' | '&gt;' | '&gt;=' | 'LIKE'
 * NoFullText ::= ('nofulltext' | 'nof') ['?']
 * NoQueryOrder ::= ('noqueryorder' | 'nqord') ['?']
 * Sns ::= ('sns' | '*') ['?']
 * StringList ::= String {',' String}
 * String ::= QuotedString | UnquotedString
 * QuotedString ::= SingleQuotedString | DoubleQuotedString
 * SingleQuotedString ::= ''' UnquotedString '''
 * DoubleQuotedString ::= '"' UnquotedString '"'
 * UnquotedString ::= XmlChar {XmlChar}
 * XmlChar ::= see 3.2.2 Local Names
 * </pre>
 *
 * @param <T>
 * @param <N>
 */
public class CompactNodeTypeDefReader<T, N> {

    /**
     * the list of parsed QNodeTypeDefinition
     */
    private final List<T> nodeTypeDefs = new LinkedList<T>();

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
    private final DefinitionBuilderFactory<T, N> factory;

    /**
     * Creates a new CND reader and parses the given stream.
     *
     * @param r        a reader to the CND
     * @param systemId a informative id of the given stream
     * @param factory  builder for creating new definitions and handling namespaces
     * @throws ParseException if an error occurs
     */
    public CompactNodeTypeDefReader(Reader r, String systemId,
                                    DefinitionBuilderFactory<T, N> factory) throws ParseException {

        this(r, systemId, null, factory);
    }

    /**
     * Creates a new CND reader and parses the given stream.
     *
     * @param r         a reader to the CND
     * @param systemId  a informative id of the given stream
     * @param nsMapping default namespace mapping to use
     * @param factory   builder for creating new definitions and handling namespaces
     * @throws ParseException if an error occurs
     */
    public CompactNodeTypeDefReader(Reader r, String systemId, N nsMapping,
                                    DefinitionBuilderFactory<T, N> factory) throws ParseException {

        super();

        this.factory = factory;
        lexer = new Lexer(r, systemId);
        if (nsMapping != null) {
            factory.setNamespaceMapping(nsMapping);
        }

        nextToken();
        parse();
    }

    /**
     * Returns the previously assigned system id
     *
     * @return the system id
     */
    public String getSystemId() {
        return lexer.getSystemId();
    }

    /**
     * Returns the list of parsed node type definitions definitions.
     *
     * @return a collection of node type definition objects
     */
    public List<T> getNodeTypeDefinitions() {
        return nodeTypeDefs;
    }

    /**
     * Returns the namespace mapping.
     *
     * @return
     */
    public N getNamespaceMapping() {
        return factory.getNamespaceMapping();
    }

    /**
     * Parses the definition
     *
     * @throws ParseException if an error during parsing occurs
     */
    private void parse() throws ParseException {
        while (!currentTokenEquals(Lexer.EOF)) {
            if (!doNameSpace()) {
                break;
            }
        }
        try {
            while (!currentTokenEquals(Lexer.EOF)) {
                AbstractNodeTypeDefinitionBuilder<T> ntd = factory.newNodeTypeDefinitionBuilder();
                ntd.setOrderableChildNodes(false);
                ntd.setMixin(false);
                ntd.setAbstract(false);
                ntd.setQueryable(true);
                doNodeTypeName(ntd);
                doSuperTypes(ntd);
                doOptions(ntd);
                doItemDefs(ntd);
                nodeTypeDefs.add(ntd.build());
            }
        } catch (RepositoryException e) {
            lexer.fail(e);
        }
    }


    /**
     * processes the namespace declaration
     *
     * @return <code>true</code> if a namespace was parsed
     * @throws ParseException if an error during parsing occurs
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
            factory.setNamespace(prefix, uri);
        } catch (RepositoryException e) {
            lexer.fail("Error setting namespace mapping " + currentToken, e);
        }
        nextToken();
        return true;
    }

    /**
     * processes the nodetype name
     *
     * @param ntd nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doNodeTypeName(AbstractNodeTypeDefinitionBuilder<T> ntd) throws ParseException {
        if (!currentTokenEquals(Lexer.BEGIN_NODE_TYPE_NAME)) {
            lexer.fail("Missing '" + Lexer.BEGIN_NODE_TYPE_NAME + "' delimiter for beginning of node type name");
        }
        nextToken();
        try {
            ntd.setName(currentToken);
        } catch (RepositoryException e) {
            lexer.fail("Error setting node type name " + currentToken, e);
        }

        nextToken();
        if (!currentTokenEquals(Lexer.END_NODE_TYPE_NAME)) {
            lexer.fail("Missing '" + Lexer.END_NODE_TYPE_NAME + "' delimiter for end of node type name, found " + currentToken);
        }
        nextToken();
    }

    /**
     * processes the superclasses
     *
     * @param ntd nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doSuperTypes(AbstractNodeTypeDefinitionBuilder<T> ntd) throws ParseException {

        if (currentTokenEquals(Lexer.EXTENDS))
            do {
                nextToken();
                try {
                    ntd.addSupertype(currentToken);
                } catch (RepositoryException e) {
                    lexer.fail("Error setting super type of " + ntd.getName() + " to " + currentToken, e);
                }
                nextToken();
            } while (currentTokenEquals(Lexer.LIST_DELIMITER));
    }

    /**
     * processes the options
     *
     * @param ntd nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doOptions(AbstractNodeTypeDefinitionBuilder<T> ntd) throws ParseException {

        boolean hasOption = true;
        try {
            while (hasOption) {
                if (currentTokenEquals(Lexer.ORDERABLE)) {
                    nextToken();
                    ntd.setOrderableChildNodes(true);
                } else if (currentTokenEquals(Lexer.MIXIN)) {
                    nextToken();
                    ntd.setMixin(true);
                } else if (currentTokenEquals(Lexer.ABSTRACT)) {
                    nextToken();
                    ntd.setAbstract(true);
                } else if (currentTokenEquals(Lexer.NOQUERY)) {
                    nextToken();
                    ntd.setQueryable(false);
                } else if (currentTokenEquals(Lexer.QUERY)) {
                    nextToken();
                    ntd.setQueryable(true);
                } else if (currentTokenEquals(Lexer.PRIMARYITEM)) {
                    nextToken();
                    ntd.setPrimaryItemName(currentToken);
                    nextToken();
                } else {
                    hasOption = false;
                }
            }
        } catch (RepositoryException e) {
            lexer.fail("Error setting option of " + ntd.getName() + " to " + currentToken, e);
        }
    }

    /**
     * processes the item definitions
     *
     * @param ntd nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doItemDefs(AbstractNodeTypeDefinitionBuilder<T> ntd) throws ParseException {
        while (currentTokenEquals(Lexer.PROPERTY_DEFINITION) || currentTokenEquals(Lexer.CHILD_NODE_DEFINITION)) {
            if (currentTokenEquals(Lexer.PROPERTY_DEFINITION)) {
                try {
                    AbstractPropertyDefinitionBuilder<T> pd = ntd.newPropertyDefinitionBuilder();
                    try {
                        pd.setAutoCreated(false);
                        pd.setDeclaringNodeType(ntd.getName());
                        pd.setMandatory(false);
                        pd.setMultiple(false);
                        pd.setOnParentVersion(OnParentVersionAction.COPY);
                        pd.setProtected(false);
                        pd.setRequiredType(PropertyType.STRING);
                        pd.setFullTextSearchable(true);
                        pd.setQueryOrderable(true);
                    } catch (RepositoryException e) {
                        lexer.fail("Error setting property definitions of " + pd.getName() + " to " + currentToken, e);
                    }
                    nextToken();
                    doPropertyDefinition(pd, ntd);
                    pd.build();
                } catch (RepositoryException e) {
                    lexer.fail("Error building property definition for " + ntd.getName(), e);
                }

            } else if (currentTokenEquals(Lexer.CHILD_NODE_DEFINITION)) {
                try {
                    AbstractNodeDefinitionBuilder<T> nd = ntd.newNodeDefinitionBuilder();
                    try {
                        nd.setAllowsSameNameSiblings(false);
                        nd.setAutoCreated(false);
                        nd.setDeclaringNodeType(ntd.getName());
                        nd.setMandatory(false);
                        nd.setOnParentVersion(OnParentVersionAction.COPY);
                        nd.setProtected(false);
                    } catch (RepositoryException e) {
                        lexer.fail("Error setting node definitions of " + nd.getName() + " to " + currentToken, e);
                    }

                    nextToken();
                    doChildNodeDefinition(nd, ntd);
                    nd.build();
                } catch (RepositoryException e) {
                    lexer.fail("Error building node definition for " + ntd.getName(), e);
                }
            }
        }
    }

    /**
     * processes the property definition
     *
     * @param pd  property definition builder
     * @param ntd declaring nodetype definition builder
     * @throws ParseException if an error during parsing occur
     */
    private void doPropertyDefinition(AbstractPropertyDefinitionBuilder<T> pd, AbstractNodeTypeDefinitionBuilder<T> ntd)
            throws ParseException {

        try {
            pd.setName(currentToken);
        } catch (RepositoryException e) {
            lexer.fail("Invalid property name '" + currentToken, e);
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
     * @param pd property definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doPropertyType(AbstractPropertyDefinitionBuilder<T> pd) throws ParseException {

        if (!currentTokenEquals(Lexer.BEGIN_TYPE)) {
            return;
        }
        nextToken();
        try {
            if (currentTokenEquals(Lexer.STRING)) {
                pd.setRequiredType(PropertyType.STRING);
            } else if (currentTokenEquals(Lexer.BINARY)) {
                pd.setRequiredType(PropertyType.BINARY);
            } else if (currentTokenEquals(Lexer.LONG)) {
                pd.setRequiredType(PropertyType.LONG);
            } else if (currentTokenEquals(Lexer.DECIMAL)) {
                pd.setRequiredType(PropertyType.DECIMAL);
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
            } else if (currentTokenEquals(Lexer.URI)) {
                pd.setRequiredType(PropertyType.URI);
            } else if (currentTokenEquals(Lexer.REFERENCE)) {
                pd.setRequiredType(PropertyType.REFERENCE);
            } else if (currentTokenEquals(Lexer.WEAKREFERENCE)) {
                pd.setRequiredType(PropertyType.WEAKREFERENCE);
            } else if (currentTokenEquals(Lexer.UNDEFINED)) {
                pd.setRequiredType(PropertyType.UNDEFINED);
            } else {
                lexer.fail("Unkown property type '" + currentToken + "' specified");
            }
        } catch (RepositoryException e) {
            lexer.fail("Error setting property type of " + pd.getName() + " to " + currentToken, e);
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
     * @param pd  property definition builder
     * @param ntd declaring nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doPropertyAttributes(AbstractPropertyDefinitionBuilder<T> pd,
                                      AbstractNodeTypeDefinitionBuilder<T> ntd) throws ParseException {

        try {
            while (currentTokenEquals(Lexer.PROP_ATTRIBUTE)) {
                if (currentTokenEquals(Lexer.PRIMARY)) {
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
                } else if (currentTokenEquals(Lexer.NOFULLTEXT)) {
                    pd.setFullTextSearchable(false);
                } else if (currentTokenEquals(Lexer.NOQUERYORDER)) {
                    pd.setQueryOrderable(false);
                } else if (currentTokenEquals(Lexer.QUERYOPS)) {
                    doPropertyQueryOperators(pd);
                }
                nextToken();
            }
        } catch (RepositoryException e) {
            lexer.fail("Error setting property attribute of " + pd.getName() + " to " + currentToken, e);
        }
    }

    /**
     * processes the property query operators
     *
     * @param pd the property definition builder
     * @throws ParseException if an error occurs
     */
    private void doPropertyQueryOperators(AbstractPropertyDefinitionBuilder<T> pd)
            throws ParseException {
        if (!currentTokenEquals(Lexer.QUERYOPS)) {
            return;
        }
        nextToken();

        String[] ops = currentToken.split(",");
        List<String> queryOps = new LinkedList<String>();
        for (String op : ops) {
            String s = op.trim();
            if (s.equals(Lexer.QUEROPS_EQUAL)) {
                queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO);
            } else if (s.equals(Lexer.QUEROPS_NOTEQUAL)) {
                queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO);
            } else if (s.equals(Lexer.QUEROPS_LESSTHAN)) {
                queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN);
            } else if (s.equals(Lexer.QUEROPS_LESSTHANOREQUAL)) {
                queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO);
            } else if (s.equals(Lexer.QUEROPS_GREATERTHAN)) {
                queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN);
            } else if (s.equals(Lexer.QUEROPS_GREATERTHANOREQUAL)) {
                queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO);
            } else if (s.equals(Lexer.QUEROPS_LIKE)) {
                queryOps.add(QueryObjectModelConstants.JCR_OPERATOR_LIKE);
            } else {
                lexer.fail("'" + s + "' is not a valid query operator");
            }
        }
        try {
            pd.setAvailableQueryOperators(queryOps.toArray(new String[queryOps.size()]));
        } catch (RepositoryException e) {
            lexer.fail("Error query operators for " + pd.getName() + " to " + currentToken, e);
        }
    }

    /**
     * processes the property default values
     *
     * @param pd property definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doPropertyDefaultValue(AbstractPropertyDefinitionBuilder<T> pd)
            throws ParseException {

        if (!currentTokenEquals(Lexer.DEFAULT)) {
            return;
        }

        do {
            nextToken();
            try {
                pd.addDefaultValues(currentToken);
            } catch (RepositoryException e) {
                lexer.fail("Error adding default value for " + pd.getName() + " to " + currentToken, e);
            }
            nextToken();
        } while (currentTokenEquals(Lexer.LIST_DELIMITER));
    }

    /**
     * processes the property value constraints
     *
     * @param pd property definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doPropertyValueConstraints(AbstractPropertyDefinitionBuilder<T> pd)
            throws ParseException {

        if (!currentTokenEquals(Lexer.CONSTRAINT)) {
            return;
        }

        do {
            nextToken();
            try {
                pd.addValueConstraint(currentToken);
            } catch (RepositoryException e) {
                lexer.fail("Error adding value constraint for " + pd.getName() + " to " + currentToken, e);
            }
            nextToken();
        } while (currentTokenEquals(Lexer.LIST_DELIMITER));
    }

    /**
     * processes the childnode definition
     *
     * @param nd  node definition builder
     * @param ntd declaring nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doChildNodeDefinition(AbstractNodeDefinitionBuilder<T> nd,
                                       AbstractNodeTypeDefinitionBuilder<T> ntd)
            throws ParseException {

        try {
            nd.setName(currentToken);
        } catch (RepositoryException e) {
            lexer.fail("Invalid child node name '" + currentToken, e);
        }
        nextToken();
        doChildNodeRequiredTypes(nd);
        doChildNodeDefaultType(nd);
        doChildNodeAttributes(nd, ntd);
    }

    /**
     * processes the childnode required types
     *
     * @param nd node definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doChildNodeRequiredTypes(AbstractNodeDefinitionBuilder<T> nd)
            throws ParseException {

        if (!currentTokenEquals(Lexer.BEGIN_TYPE)) {
            return;
        }

        do {
            nextToken();
            try {
                nd.addRequiredPrimaryType(currentToken);
            } catch (RepositoryException e) {
                lexer.fail("Error setting required primary type of " + nd.getName() + " to " + currentToken, e);
            }
            nextToken();
        } while (currentTokenEquals(Lexer.LIST_DELIMITER));
        nextToken();
    }

    /**
     * processes the childnode default types
     *
     * @param nd node definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doChildNodeDefaultType(AbstractNodeDefinitionBuilder<T> nd)
            throws ParseException {

        if (!currentTokenEquals(Lexer.DEFAULT)) {
            return;
        }
        nextToken();
        try {
            nd.setDefaultPrimaryType(currentToken);
        } catch (RepositoryException e) {
            lexer.fail("Error setting default primary type of " + nd.getName() + " to " + currentToken, e);
        }
        nextToken();
    }

    /**
     * processes the childnode attributes
     *
     * @param nd  node definition builder
     * @param ntd declaring nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doChildNodeAttributes(AbstractNodeDefinitionBuilder<T> nd,
                                       AbstractNodeTypeDefinitionBuilder<T> ntd)
            throws ParseException {

        try {
            while (currentTokenEquals(Lexer.NODE_ATTRIBUTE)) {
                if (currentTokenEquals(Lexer.PRIMARY)) {
                    ntd.setPrimaryItemName(nd.getName());
                } else if (currentTokenEquals(Lexer.AUTOCREATED)) {
                    nd.setAutoCreated(true);
                } else if (currentTokenEquals(Lexer.MANDATORY)) {
                    nd.setMandatory(true);
                } else if (currentTokenEquals(Lexer.PROTECTED)) {
                    nd.setProtected(true);
                } else if (currentTokenEquals(Lexer.SNS)) {
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
        } catch (RepositoryException e) {
            lexer.fail("Error setting child node attribute of " + nd.getName() + " to " + currentToken, e);
        }
    }

    /**
     * Gets the next token from the underlying lexer.
     *
     * @throws ParseException if the lexer fails to get the next token.
     * @see Lexer#getNextToken()
     */
    private void nextToken() throws ParseException {
        currentToken = lexer.getNextToken();
    }

    /**
     * Checks if the {@link #currentToken} is semantically equal to the given
     * argument ignoring the case.
     *
     * @param s the tokens to compare with
     * @return <code>true</code> if equals; <code>false</code> otherwise.
     */
    private boolean currentTokenEquals(String[] s) {
        for (String value : s) {
            if (currentToken.equalsIgnoreCase(value)) {
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
