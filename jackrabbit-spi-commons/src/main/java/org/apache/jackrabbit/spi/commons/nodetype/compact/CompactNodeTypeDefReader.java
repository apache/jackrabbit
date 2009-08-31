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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.spi.commons.nodetype.NodeTypeDefinitionFactory;
import org.apache.jackrabbit.spi.commons.nodetype.compact.QNodeTypeDefinitionsBuilder.QNodeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.compact.QNodeTypeDefinitionsBuilder.QNodeTypeDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.nodetype.compact.QNodeTypeDefinitionsBuilder.QPropertyDefinitionBuilder;
import org.apache.jackrabbit.spi.commons.query.qom.Operator;
import org.apache.jackrabbit.util.ISO9075;

/**
 * CompactNodeTypeDefReader. Parses node type definitions written in the compact
 * node type definition format and provides a list of QNodeTypeDefinition
 * objects that can then be used to register node types.
 *
 * <p/>
 * The EBNF grammar of the compact node type definition:<br>
 * <pre>
 * Cnd ::= {NamespaceMapping | NodeTypeDef}
 * NamespaceMapping ::= '<' Prefix '=' Uri '>'
 * Prefix ::= String
 * Uri ::= String
 * NodeTypeDef ::= NodeTypeName [Supertypes]
 *                 [NodeTypeAttribute {NodeTypeAttribute}]
 *                 {PropertyDef | ChildNodeDef}
 * NodeTypeName ::= '[' String ']'
 * Supertypes ::= '>' (StringList | '?')
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
 * ValueConstraints ::= '<' (StringList | '?')
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
 * Operator ::= '=' | '<>' | '<' | '<=' | '>' | '>=' | 'LIKE'
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
 */
public class CompactNodeTypeDefReader {

    /**
     * Default namespace mappings
     */
    public static final NamespaceMapping NS_DEFAULTS;
    static {
        try {
            NS_DEFAULTS = new NamespaceMapping();
            NS_DEFAULTS.setMapping(Name.NS_EMPTY_PREFIX, Name.NS_DEFAULT_URI);
            NS_DEFAULTS.setMapping(Name.NS_JCR_PREFIX, Name.NS_JCR_URI);
            NS_DEFAULTS.setMapping(Name.NS_MIX_PREFIX, Name.NS_MIX_URI);
            NS_DEFAULTS.setMapping(Name.NS_NT_PREFIX, Name.NS_NT_URI);
            NS_DEFAULTS.setMapping(Name.NS_REP_PREFIX, Name.NS_REP_URI);
        } catch (NamespaceException e) {
            throw new InternalError(e.toString());
        }
    }
    
    /**
     * the list of parsed QNodeTypeDefinition
     */
    private final List<QNodeTypeDefinition> nodeTypeDefs
            = new LinkedList<QNodeTypeDefinition>();

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
     * Convenience method that creates a new CND reader and parses the given
     * file directly.
     *
     * @param file A CND file
     * @return a new 'parsed' reader object
     * @throws ParseException if an error occurs
     * @throws IOException if an I/O error occurs.
     */
    public static CompactNodeTypeDefReader read(File file)
            throws ParseException, IOException {
        InputStream in = null;
        Reader r = null;
        try {
            in = new FileInputStream(file);
            r = new InputStreamReader(in, "utf8");
            return new CompactNodeTypeDefReader(r, file.getPath());
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }


    /**
     * Creates a new CND reader and parses the given stream directly.
     *
     * @param r a reader to the CND
     * @param systemId a informative id of the given stream
     * @throws ParseException if an error occurs
     */
    public CompactNodeTypeDefReader(Reader r, String systemId)
            throws ParseException {
        this(r, systemId, null, null);
    }

    /**
     * Creates a new CND reader and parses the given stream it directly.
     * If <code>builder</code> is <code>null</code> the reader uses the
     * default {@link QNodeTypeDefinitionsBuilderImpl}.
     *
     * @param r a reader to the CND
     * @param systemId a informative id of the given stream
     * @param builder build for creating new definitions or <code>null</code>
     * @throws ParseException if an error occurs
     */
    public CompactNodeTypeDefReader(Reader r, String systemId,
                                    QNodeTypeDefinitionsBuilder builder)
            throws ParseException {
        this(r, systemId, null, builder);
    }

    /**
     * Creates a new CND reader and parses the given stream it directly.
     *
     * @param r a reader to the CND
     * @param systemId a informative id of the given stream
     * @param mapping default namespace mapping to use
     * @throws ParseException if an error occurs
     */
    public CompactNodeTypeDefReader(Reader r, String systemId, NamespaceMapping mapping)
            throws ParseException {
        this(r, systemId, mapping, null);
    }

    /**
     * Creates a new CND reader and parses the given stream it directly.
     * If <code>builder</code> is <code>null</code> the reader uses the
     * default {@link QNodeTypeDefinitionsBuilderImpl}.
     *
     * @param r a reader to the CND
     * @param systemId a informative id of the given stream
     * @param mapping default namespace mapping to use
     * @param builder build for creating new definitions
     * @throws ParseException if an error occurs
     */
    public CompactNodeTypeDefReader(Reader r, String systemId, NamespaceMapping mapping,
            QNodeTypeDefinitionsBuilder builder) throws ParseException {

        this.builder = builder == null
                ? new QNodeTypeDefinitionsBuilderImpl()
                : builder;
        lexer = new Lexer(r, systemId);
        this.nsMapping = mapping == null
                ? new NamespaceMapping(NS_DEFAULTS)
                : mapping;
        this.resolver = new DefaultNamePathResolver(nsMapping);
        nextToken();
        parse();
    }

    /**
     * Returns the previously assigned system id
     * @return the system id
     */
    public String getSystemId() {
        return lexer.getSystemId();
    }

    /**
     * Returns the list of parsed QNodeTypeDefinition definitions.
     *
     * @return a collection of QNodeTypeDefinition objects
     */
    public List<QNodeTypeDefinition> getNodeTypeDefinitions() {
        return nodeTypeDefs;
    }

    /**
     * Convenience methdo that returns the list of parsed NodeTypeDefinition
     * definitions, using the {@link NodeTypeDefinitionFactory}.
     *
     * @param session repository session used for converting the definitions.
     * @return a collection of NodeTypeDefinition objects
     * @throws RepositoryException if an error occurs
     */
    public List<NodeTypeDefinition> getNodeTypeDefinitions(Session session)
            throws RepositoryException {
        NodeTypeDefinitionFactory fac = new NodeTypeDefinitionFactory(session);
        return fac.create(nodeTypeDefs);
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
     * @throws ParseException if an error during parsing occurs
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
            ntd.setAbstract(false);
            ntd.setQueryable(true);
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
     * @param ntd nodetype definition builder
     * @throws ParseException if an error during parsing occurs
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
     * @param ntd nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doSuperTypes(QNodeTypeDefinitionBuilder ntd) throws ParseException {
        Set<Name> supertypes = new HashSet<Name>();
        if (currentTokenEquals(Lexer.EXTENDS))
            do {
                nextToken();
                supertypes.add(toName(currentToken));
                nextToken();
            } while (currentTokenEquals(Lexer.LIST_DELIMITER));

        ntd.setSupertypes(supertypes.toArray(new Name[supertypes.size()]));
    }

    /**
     * processes the options
     *
     * @param ntd nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doOptions(QNodeTypeDefinitionBuilder ntd) throws ParseException {
        boolean hasOption = true;
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
                ntd.setPrimaryItemName(toName(currentToken));
                nextToken();
            } else {
                hasOption = false;
            }
        }
    }

    /**
     * processes the item definitions
     *
     * @param ntd nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doItemDefs(QNodeTypeDefinitionBuilder ntd) throws ParseException {
        List<QPropertyDefinition> propertyDefinitions = new LinkedList<QPropertyDefinition>();
        List<QNodeDefinition> nodeDefinitions = new LinkedList<QNodeDefinition>();
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
                pd.setValueConstraints(QValueConstraint.EMPTY_ARRAY);
                pd.setFullTextSearchable(true);
                pd.setQueryOrderable(true);
                pd.setAvailableQueryOperators(Operator.getAllQueryOperators());

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
        ntd.setPropertyDefs(propertyDefinitions.toArray(new QPropertyDefinition[propertyDefinitions.size()]));
        ntd.setChildNodeDefs(nodeDefinitions.toArray(new QNodeDefinition[nodeDefinitions.size()]));
    }

    /**
     * processes the property definition
     *
     * @param pd property definition builder
     * @param ntd declaring nodetype definition builder
     * @throws ParseException if an error during parsing occurs
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
     * @param pd property definition builder
     * @throws ParseException if an error during parsing occurs
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
        nextToken();
        if (!currentTokenEquals(Lexer.END_TYPE)) {
            lexer.fail("Missing '" + Lexer.END_TYPE + "' delimiter for end of property type");
        }
        nextToken();
    }

    /**
     * processes the property attributes
     *
     * @param pd property definition builder
     * @param ntd declaring nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doPropertyAttributes(QPropertyDefinitionBuilder pd,
                                      QNodeTypeDefinitionBuilder ntd)
            throws ParseException {
        while (currentTokenEquals(Lexer.PROP_ATTRIBUTE)) {
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
            } else if (currentTokenEquals(Lexer.NOFULLTEXT)) {
                pd.setFullTextSearchable(false);
            } else if (currentTokenEquals(Lexer.NOQUERYORDER)) {
                pd.setQueryOrderable(false);
            } else if (currentTokenEquals(Lexer.QUERYOPS)) {
                doPropertyQueryOperators(pd);
            }
            nextToken();
        }
    }

    /**
     * processes the property query operators
     *
     * @param pd the property definition builder
     * @throws ParseException if an error occurs
     */
    private void doPropertyQueryOperators(QPropertyDefinitionBuilder pd)
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
        pd.setAvailableQueryOperators(queryOps.toArray(new String[queryOps.size()]));
    }

    /**
     * processes the property default values
     *
     * @param pd property definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doPropertyDefaultValue(QPropertyDefinitionBuilder pd)
            throws ParseException {
        if (!currentTokenEquals(Lexer.DEFAULT)) {
            return;
        }
        List<QValue> defaultValues = new LinkedList<QValue>();
        do {
            nextToken();
            try {
                defaultValues.add(pd.createValue(currentToken, resolver));
            } catch (ValueFormatException e) {
                lexer.fail("'" + currentToken + "' is not a valid string representation of a value of type " + pd.getRequiredType());
            } catch (RepositoryException e) {
                lexer.fail("An error occured during value conversion of '" + currentToken + "'");
            }
            nextToken();
        } while (currentTokenEquals(Lexer.LIST_DELIMITER));
        pd.setDefaultValues(defaultValues.toArray(new QValue[defaultValues.size()]));
    }

    /**
     * processes the property value constraints
     *
     * @param pd property definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doPropertyValueConstraints(QPropertyDefinitionBuilder pd)
            throws ParseException {
        if (!currentTokenEquals(Lexer.CONSTRAINT)) {
            return;
        }
        List<QValueConstraint> constraints = new LinkedList<QValueConstraint>();
        do {
            nextToken();
            try {
                constraints.add(pd.createValueConstraint(currentToken, resolver));
            } catch (InvalidConstraintException e) {
                lexer.fail("'" + currentToken + "' is not a valid constraint expression for a value of type " + pd.getRequiredType());
            }
            nextToken();
        } while (currentTokenEquals(Lexer.LIST_DELIMITER));
        pd.setValueConstraints(constraints.toArray(new QValueConstraint[constraints.size()]));
    }

    /**
     * processes the childnode definition
     *
     * @param nd node definition builder
     * @param ntd declaring nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doChildNodeDefinition(QNodeDefinitionBuilder nd,
                                       QNodeTypeDefinitionBuilder ntd)
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
     * @param nd node definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doChildNodeRequiredTypes(QNodeDefinitionBuilder nd)
            throws ParseException {
        if (!currentTokenEquals(Lexer.BEGIN_TYPE)) {
            return;
        }
        List<Name> types = new LinkedList<Name>();
        do {
            nextToken();
            types.add(toName(currentToken));
            nextToken();
        } while (currentTokenEquals(Lexer.LIST_DELIMITER));
        nd.setRequiredPrimaryTypes(types.toArray(new Name[types.size()]));
        nextToken();
    }

    /**
     * processes the childnode default types
     *
     * @param nd node definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doChildNodeDefaultType(QNodeDefinitionBuilder nd)
            throws ParseException {
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
     * @param nd node definition builder
     * @param ntd declaring nodetype definition builder
     * @throws ParseException if an error during parsing occurs
     */
    private void doChildNodeAttributes(QNodeDefinitionBuilder nd,
                                       QNodeTypeDefinitionBuilder ntd)
            throws ParseException {
        while (currentTokenEquals(Lexer.NODE_ATTRIBUTE)) {
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
    }

    /**
     * Converts the given string into a <code>Name</code> using the current
     * namespace mapping.
     *
     * @param stringName jcr name
     * @return A <code>Name</code> object.
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
