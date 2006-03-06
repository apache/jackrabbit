/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.nodetype.compact;

import java.io.StreamTokenizer;
import java.io.Reader;
import java.io.IOException;

/**
 * Lexer
 */
public class Lexer {
    public final static char SINGLE_QUOTE = '\'';
    public final static char DOUBLE_QUOTE = '\"';
    public final static char BEGIN_NODE_TYPE_NAME = '[';
    public final static char END_NODE_TYPE_NAME = ']';
    public final static char EXTENDS = '>';
    public final static char LIST_DELIMITER = ',';
    public final static char PROPERTY_DEFINITION = '-';
    public final static char CHILD_NODE_DEFINITION = '+';
    public final static char BEGIN_TYPE = '(';
    public final static char END_TYPE = ')';
    public final static char DEFAULT = '=';
    public final static char CONSTRAINT = '<';

    public final static String[] ORDERABLE = new String[] {"orderable", "ord", "o"};
    public final static String[] MIXIN = new String[]{"mixin", "mix", "m"};

    public final static String[] PRIMARY = new String[]{"primary", "pri", "!"};
    public final static String[] AUTOCREATED = new String[]{"autocreated", "aut", "a"};
    public final static String[] MANDATORY = new String[]{"mandatory", "man", "m"};
    public final static String[] PROTECTED = new String[]{"protected", "pro", "p"};
    public final static String[] MULTIPLE = new String[]{"multiple", "mul", "*"};

    public final static String[] COPY = new String[]{"copy", "Copy", "COPY"};
    public final static String[] VERSION = new String[]{"version", "Version", "VERSION"};
    public final static String[] INITIALIZE = new String[]{"initialize", "Initialize", "INITIALIZE"};
    public final static String[] COMPUTE = new String[]{"compute", "Compute", "COMPUTE"};
    public final static String[] IGNORE = new String[]{"ignore", "Ignore", "IGNORE"};
    public final static String[] ABORT = new String[]{"abort", "Abort", "ABORT"};

    public final static String[] ATTRIBUTE = new String[]{"primary", "pri", "!",
                                                          "autocreated", "aut", "a",
                                                          "mandatory", "man", "m",
                                                          "protected", "pro", "p",
                                                          "multiple", "mul", "*",
                                                          "copy", "Copy", "COPY",
                                                          "version", "Version", "VERSION",
                                                          "initialize", "Initialize", "INITIALIZE",
                                                          "compute", "Compute", "COMPUTE",
                                                          "ignore", "Ignore", "IGNORE",
                                                          "abort", "Abort", "ABORT"};

    public final static String[] STRING = {"string", "String", "STRING"};
    public final static String[] BINARY = {"binary", "Binary", "BINARY"};
    public final static String[] LONG = {"long", "Long", "LONG"};
    public final static String[] DOUBLE = {"double", "Double", "DOUBLE"};
    public final static String[] BOOLEAN = {"boolean", "Boolean", "BOOLEAN"};
    public final static String[] DATE = {"date", "Date", "DATE"};
    public final static String[] NAME = {"name", "Name", "NAME"};
    public final static String[] PATH = {"path", "Path", "PATH"};
    public final static String[] REFERENCE = {"reference", "Reference", "REFERENCE"};

    public final static String[] UNDEFINED = new String[]{"undefined", "Undefined", "UNDEFINED", "*"};

    public final static String EOF = "eof";

    private final StreamTokenizer st;

    private final String systemId;

    /**
     * Constructor
     * @param r
     */
    public Lexer(Reader r, String systemId){
        this.systemId = systemId;
        st = new StreamTokenizer(r);

        st.eolIsSignificant(false);

        st.lowerCaseMode(false);

        st.slashSlashComments(true);
        st.slashStarComments(true);

        st.wordChars('a','z');
        st.wordChars('A','Z');
        st.wordChars(':',':');

        st.quoteChar(SINGLE_QUOTE);
        st.quoteChar(DOUBLE_QUOTE);

        st.ordinaryChar(BEGIN_NODE_TYPE_NAME);
        st.ordinaryChar(END_NODE_TYPE_NAME);
        st.ordinaryChar(EXTENDS);
        st.ordinaryChar(LIST_DELIMITER);
        st.ordinaryChar(PROPERTY_DEFINITION);
        st.ordinaryChar(CHILD_NODE_DEFINITION);
        st.ordinaryChar(BEGIN_TYPE);
        st.ordinaryChar(END_TYPE);
        st.ordinaryChar(DEFAULT);
        st.ordinaryChar(CONSTRAINT);
    }

    /**
     * getNextToken
     *
     * @return
     * @throws ParseException
     */
    public String getNextToken() throws ParseException {
        try {
            int tokenType = st.nextToken();
            if (tokenType == StreamTokenizer.TT_EOF){
                return EOF;
            } else if(tokenType == StreamTokenizer.TT_WORD || tokenType == SINGLE_QUOTE || tokenType == DOUBLE_QUOTE){
                return st.sval;
            } else {
                return new String(new char[]{(char)tokenType});
            }
        } catch (IOException e){
            fail("IOException while attempting to read input stream", e);
            return null;
        }
    }

    public void fail(String message) throws ParseException {
        throw new ParseException(message, st.lineno(), -1, systemId);
    }

    public void fail(String message, Throwable e) throws ParseException {
        throw new ParseException(message, e, st.lineno(), -1, systemId);
    }

    public void fail(Throwable e) throws ParseException {
        throw new ParseException(e, st.lineno(), -1, systemId);
    }
}