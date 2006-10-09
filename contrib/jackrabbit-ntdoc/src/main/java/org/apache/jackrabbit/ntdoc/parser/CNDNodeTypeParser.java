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
package org.apache.jackrabbit.ntdoc.parser;

import java.io.*;
import java.util.*;

import org.apache.jackrabbit.ntdoc.model.*;
import org.apache.jackrabbit.ntdoc.parser.*;

/**
 * This class implements the CND parser.
 */
public final class CNDNodeTypeParser
        extends NodeTypeParser {
    /**
     * Orderable opt strings.
     */
    private final static String[] TOK_ORDERABLE =
            {"orderable", "ord", "o"};

    /**
     * Mixin opt strings.
     */
    private final static String[] TOK_MIXIN =
            {"mixin", "mix", "m"};

    /**
     * Primary opt strings.
     */
    private final static String[] TOK_PRIMARY =
            {"primary", "pri", "!"};

    /**
     * Autocreated opt strings.
     */
    private final static String[] TOK_AUTOCREATED =
            {"autocreated", "aut", "a"};

    /**
     * Mandatory opt strings.
     */
    private final static String[] TOK_MANDATORY =
            {"mandatory", "man", "m"};

    /**
     * Protected opt strings.
     */
    private final static String[] TOK_PROTECTED =
            {"protected", "pro", "p"};

    /**
     * Multiple opt strings.
     */
    private final static String[] TOK_MULTIPLE =
            {"multiple", "mul", "*"};

    /**
     * Copy option.
     */
    private final static String[] TOK_COPY =
            {"copy", "Copy", "COPY"};

    /**
     * Version option.
     */
    private final static String[] TOK_VERSION =
            {"version", "Version", "VERSION"};

    /**
     * Initialize option.
     */
    private final static String[] TOK_INITIALIZE =
            {"initialize", "Initialize", "INITIALIZE"};

    /**
     * Compute option.
     */
    private final static String[] TOK_COMPUTE =
            {"compute", "Compute", "COMPUTE"};

    /**
     * Ignore option.
     */
    private final static String[] TOK_IGNORE =
            {"ignore", "Ignore", "IGNORE"};

    /**
     * Abort option.
     */
    private final static String[] TOK_ABORT =
            {"abort", "Abort", "ABORT"};

    /**
     * String type.
     */
    private final static String[] TOK_STRING =
            {"string", "String", "STRING"};

    /**
     * String type.
     */
    private final static String[] TOK_BINARY =
            {"binary", "Binary", "BINARY"};

    /**
     * Long type.
     */
    private final static String[] TOK_LONG =
            {"long", "Long", "LONG"};

    /**
     * Double type.
     */
    private final static String[] TOK_DOUBLE =
            {"double", "Double", "DOUBLE"};

    /**
     * Boolean type.
     */
    private final static String[] TOK_BOOLEAN =
            {"boolean", "Boolean", "BOOLEAN"};

    /**
     * Date type.
     */
    private final static String[] TOK_DATE =
            {"date", "Date", "DATE"};

    /**
     * Name type.
     */
    private final static String[] TOK_NAME =
            {"name", "Name", "NAME"};

    /**
     * Path type.
     */
    private final static String[] TOK_PATH =
            {"path", "Path", "PATH"};

    /**
     * Reference type.
     */
    private final static String[] TOK_REFERENCE =
            {"reference", "Reference", "REFERENCE"};

    /**
     * Undefined type.
     */
    private final static String[] TOK_UNDEFINED =
            {"undefined", "Undefined", "UNDEFINED", "*"};

    /**
     * Tokenizer.
     */
    private StreamTokenizer tok;

    /**
     * Current token.
     */
    private String currentToken;

    /**
     * Initialize the parser.
     */
    private void initialize() {
        this.currentToken = null;
        this.tok = new StreamTokenizer(getReader());
        this.tok.lowerCaseMode(false);
        this.tok.eolIsSignificant(false);
        this.tok.slashStarComments(true);
        this.tok.slashSlashComments(true);
        this.tok.quoteChar('\'');
        this.tok.quoteChar('"');
        this.tok.wordChars('a', 'z');
        this.tok.wordChars('A', 'Z');
        this.tok.wordChars(':', ':');
        this.tok.wordChars('_', '_');
        this.tok.ordinaryChar('[');
        this.tok.ordinaryChar(']');
        this.tok.ordinaryChar('<');
        this.tok.ordinaryChar('>');
        this.tok.ordinaryChar('(');
        this.tok.ordinaryChar(')');
        this.tok.ordinaryChar('+');
        this.tok.ordinaryChar('-');
        this.tok.ordinaryChar('=');
        this.tok.ordinaryChar(',');
    }

    /**
     * Return the next token.
     */
    private void nextToken()
            throws IOException {
        try {
            int tokenType = this.tok.nextToken();
            if (tokenType == StreamTokenizer.TT_EOF) {
                this.currentToken = null;
            } else if (tokenType == StreamTokenizer.TT_WORD) {
                this.currentToken = this.tok.sval;
            } else if ((tokenType == '\'') || (tokenType == '"')) {
                this.currentToken = this.tok.sval;
            } else if (tokenType == StreamTokenizer.TT_NUMBER) {
                this.currentToken = String.valueOf(this.tok.nval);
            } else {
                this.currentToken = Character.toString((char) tokenType);
            }
        } catch (IOException e) {
            throw createFailure("Failed to read stream", e);
        }
    }

    /**
     * Return true if current token is value.
     */
    private boolean currentTokenEquals(String value) {
        if (value == this.currentToken) {
            return true;
        } else if ((value == null) || (this.currentToken == null)) {
            return false;
        } else {
            return this.currentToken.equals(value);
        }
    }

    /**
     * Return true if current token is value.
     */
    private boolean currentTokenEquals(char value) {
        return currentTokenEquals(Character.toString(value));
    }

    /**
     * Return true if current token is value.
     */
    private boolean currentTokenEquals(String[] values) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(this.currentToken)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Parse the stream.
     */
    public void parse()
            throws IOException {
        initialize();
        nextToken();
        while (this.currentToken != null) {
            if (!parseNamespace()) {
                parseNodeType();
            }
        }
    }

    /**
     * Parse namespace. Return true if namespace is parsed.
     */
    private boolean parseNamespace()
            throws IOException {
        if (!currentTokenEquals('<')) {
            return false;
        }

        nextToken();
        String prefix = this.currentToken;

        nextToken();
        if (!currentTokenEquals('=')) {
            throw createFailure("Missing '=' in namespace declaration");
        }

        nextToken();
        String uri = this.currentToken;

        nextToken();
        if (!currentTokenEquals('>')) {
            throw createFailure("Missing '>' in namespace declaration");
        }

        nextToken();
        addNamespace(prefix, uri);
        return true;
    }

    /**
     * Parse node type.
     */
    private void parseNodeType()
            throws IOException {
        String name = parseNodeTypeName();
        NodeType nt = addNodeType(name);
        nt.setSuperTypes(parseSuperTypes());
        parseNodeTypeOptions(nt);
        parseChildItemDefs(nt);
    }

    /**
     * Parse node type name.
     */
    private String parseNodeTypeName()
            throws IOException {
        if (!currentTokenEquals('[')) {
            throw createFailure("Missing '[' delimiter for beginning of node type name");
        }

        nextToken();
        String name = this.currentToken;

        nextToken();
        if (!currentTokenEquals(']')) {
            throw createFailure("Missing ']' delimiter for end of node type name");
        }

        nextToken();
        return name;
    }

    /**
     * Parse string list names.
     */
    private List parseStringList()
            throws IOException {
        ArrayList list = new ArrayList();
        while (true) {
            nextToken();
            list.add(this.currentToken);

            nextToken();
            if (!currentTokenEquals(',')) {
                break;
            }
        }

        return list;
    }

    /**
     * Parse super type names.
     */
    private List parseSuperTypes()
            throws IOException {
        if (!currentTokenEquals('>')) {
            return null;
        }

        return parseStringList();
    }

    /**
     * Parse the node type options.
     */
    private void parseNodeTypeOptions(NodeType nt)
            throws IOException {
        if (currentTokenEquals(TOK_ORDERABLE)) {
            nt.setOrderable(true);
            nextToken();
            if (currentTokenEquals(TOK_MIXIN)) {
                nt.setMixin(true);
                nextToken();
            }
        } else if (currentTokenEquals(TOK_MIXIN)) {
            nt.setMixin(true);
            nextToken();
            if (currentTokenEquals(TOK_ORDERABLE)) {
                nt.setOrderable(true);
                nextToken();
            }
        }
    }

    /**
     * Parse child item definitions.
     */
    private void parseChildItemDefs(NodeType nt)
            throws IOException {
        while (currentTokenEquals('-') || currentTokenEquals('+')) {
            if (currentTokenEquals('-')) {
                parsePropertyDef(nt);
            } else if (currentTokenEquals('+')) {
                parseChildNodeDef(nt);
            }
        }
    }

    /**
     * Parse property definition.
     */
    private void parsePropertyDef(NodeType nt)
            throws IOException {
        nextToken();
        PropertyDef def = new PropertyDef(this.currentToken);
        nt.addItemDef(def);

        nextToken();
        parsePropertyType(def);
        def.setDefaultValues(parsePropertyDefaultValues());
        parseChildItemOptions(def);
        def.setConstraints(parsePropertyConstraints());
    }

    /**
     * Parse the property type.
     */
    private void parsePropertyType(PropertyDef def)
            throws IOException {
        def.setRequiredType(PropertyDef.TYPE_STRING);
        if (currentTokenEquals('(')) {
            nextToken();
            if (currentTokenEquals(TOK_STRING)) {
                def.setRequiredType(PropertyDef.TYPE_STRING);
            } else if (currentTokenEquals(TOK_BINARY)) {
                def.setRequiredType(PropertyDef.TYPE_BINARY);
            } else if (currentTokenEquals(TOK_LONG)) {
                def.setRequiredType(PropertyDef.TYPE_LONG);
            } else if (currentTokenEquals(TOK_DOUBLE)) {
                def.setRequiredType(PropertyDef.TYPE_DOUBLE);
            } else if (currentTokenEquals(TOK_BOOLEAN)) {
                def.setRequiredType(PropertyDef.TYPE_BOOLEAN);
            } else if (currentTokenEquals(TOK_DATE)) {
                def.setRequiredType(PropertyDef.TYPE_DATE);
            } else if (currentTokenEquals(TOK_NAME)) {
                def.setRequiredType(PropertyDef.TYPE_NAME);
            } else if (currentTokenEquals(TOK_PATH)) {
                def.setRequiredType(PropertyDef.TYPE_PATH);
            } else if (currentTokenEquals(TOK_REFERENCE)) {
                def.setRequiredType(PropertyDef.TYPE_REFERENCE);
            } else if (currentTokenEquals(TOK_UNDEFINED)) {
                def.setRequiredType(PropertyDef.TYPE_UNDEFINED);
            } else {
                throw createFailure("Unkown property type '" + this.currentToken + "' specified");
            }

            nextToken();
            if (!currentTokenEquals(')')) {
                throw createFailure("Missing ')' delimiter for end of property type");
            }

            nextToken();
        }
    }

    /**
     * Parse property default values.
     */
    private List parsePropertyDefaultValues()
            throws IOException {
        if (!currentTokenEquals('=')) {
            return null;
        }

        return parseStringList();
    }

    /**
     * Parse property constraints.
     */
    private List parsePropertyConstraints()
            throws IOException {
        if (!currentTokenEquals('<')) {
            return null;
        }

        return parseStringList();
    }

    /**
     * Parse child item options.
     */
    private void parseChildItemOptions(ItemDef def)
            throws IOException {
        def.setOnParentVersion(ItemDef.OPV_COPY);
        while (true) {
            if (currentTokenEquals(TOK_PRIMARY)) {
                def.setPrimary(true);
            } else if (currentTokenEquals(TOK_AUTOCREATED)) {
                def.setAutoCreated(true);
            } else if (currentTokenEquals(TOK_MANDATORY)) {
                def.setMandatory(true);
            } else if (currentTokenEquals(TOK_PROTECTED)) {
                def.setProtected(true);
            } else if (currentTokenEquals(TOK_MULTIPLE)) {
                def.setMultiple(true);
            } else if (currentTokenEquals(TOK_COPY)) {
                def.setOnParentVersion(ItemDef.OPV_COPY);
            } else if (currentTokenEquals(TOK_VERSION)) {
                def.setOnParentVersion(ItemDef.OPV_VERSION);
            } else if (currentTokenEquals(TOK_INITIALIZE)) {
                def.setOnParentVersion(ItemDef.OPV_INITIALIZE);
            } else if (currentTokenEquals(TOK_COMPUTE)) {
                def.setOnParentVersion(ItemDef.OPV_COMPUTE);
            } else if (currentTokenEquals(TOK_IGNORE)) {
                def.setOnParentVersion(ItemDef.OPV_IGNORE);
            } else if (currentTokenEquals(TOK_ABORT)) {
                def.setOnParentVersion(ItemDef.OPV_ABORT);
            } else {
                break;
            }

            nextToken();
        }
    }

    /**
     * Parse child node definition.
     */
    private void parseChildNodeDef(NodeType nt)
            throws IOException {
        nextToken();
        NodeDef def = new NodeDef(this.currentToken);
        nt.addItemDef(def);

        nextToken();
        def.setRequiredPrimaryTypes(parseChildNodeRequiredTypes());
        def.setDefaultPrimaryType(parseChildNodeDefaultType());
        parseChildItemOptions(def);
    }

    /**
     * Parse child node required types.
     */
    private List parseChildNodeRequiredTypes()
            throws IOException {
        if (!currentTokenEquals('(')) {
            return null;
        }

        List list = parseStringList();
        if (!currentTokenEquals(')')) {
            throw createFailure("Missing ')' delimiter for end of child node required types");
        }

        nextToken();
        return list;
    }

    /**
     * Parse child node default type.
     */
    private String parseChildNodeDefaultType()
            throws IOException {
        if (!currentTokenEquals('=')) {
            return null;
        }

        nextToken();
        String defaultType = this.currentToken;
        nextToken();

        return defaultType;
    }


    /**
     * Return a failure message.
     */
    private IOException createFailure(String message) {
        return createFailure(message, null);
    }

    /**
     * Return a failure message.
     */
    private IOException createFailure(String message, Throwable cause) {
        IOException e = new IOException(createFailureMessage(message));
        if (cause != null) {
            e.initCause(cause);
        }

        return e;
    }

    /**
     * Create failure message.
     */
    private String createFailureMessage(String message) {
        StringBuffer str = new StringBuffer(message);
        str.append(" [").append(getSystemId()).append(":");
        str.append(this.tok.lineno()).append("]");
        return str.toString();
    }
}
