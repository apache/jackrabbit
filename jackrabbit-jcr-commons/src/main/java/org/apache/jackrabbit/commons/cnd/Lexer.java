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

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Lexer of the CND definition.
 */
public class Lexer {
    public static final char SINGLE_QUOTE = '\'';
    public static final char DOUBLE_QUOTE = '\"';
    public static final char BEGIN_NODE_TYPE_NAME = '[';
    public static final char END_NODE_TYPE_NAME = ']';
    public static final char EXTENDS = '>';
    public static final char LIST_DELIMITER = ',';
    public static final char PROPERTY_DEFINITION = '-';
    public static final char CHILD_NODE_DEFINITION = '+';
    public static final char BEGIN_TYPE = '(';
    public static final char END_TYPE = ')';
    public static final char DEFAULT = '=';
    public static final char CONSTRAINT = '<';

    public static final String[] ORDERABLE = new String[] {"orderable", "ord", "o"};
    public static final String[] MIXIN = new String[]{"mixin", "mix", "m"};
    public static final String[] ABSTRACT = new String[]{"abstract", "abs", "a"};
    public static final String[] NOQUERY = new String[]{"noquery", "nq"};
    public static final String[] QUERY = new String[]{"query", "q"};
    public static final String[] PRIMARYITEM = new String[]{"primaryitem", "!"};

    public static final String[] PRIMARY = new String[]{"primary", "pri", "!"};
    public static final String[] AUTOCREATED = new String[]{"autocreated", "aut", "a"};
    public static final String[] MANDATORY = new String[]{"mandatory", "man", "m"};
    public static final String[] PROTECTED = new String[]{"protected", "pro", "p"};
    public static final String[] MULTIPLE = new String[]{"multiple", "mul", "*"};
    public static final String[] SNS = new String[]{"sns", "*", "multiple"};
    public static final String[] QUERYOPS = new String[]{"queryops", "qop"};
    public static final String[] NOFULLTEXT = new String[]{"nofulltext", "nof"};
    public static final String[] NOQUERYORDER = new String[]{"noqueryorder", "nqord"};

    public static final String[] COPY = new String[]{"COPY"};
    public static final String[] VERSION = new String[]{"VERSION"};
    public static final String[] INITIALIZE = new String[]{"INITIALIZE"};
    public static final String[] COMPUTE = new String[]{"COMPUTE"};
    public static final String[] IGNORE = new String[]{"IGNORE"};
    public static final String[] ABORT = new String[]{"ABORT"};

    public static final String[] PROP_ATTRIBUTE;
    public static final String[] NODE_ATTRIBUTE;
    static {
        ArrayList<String> attr = new ArrayList<String>();
        attr.addAll(Arrays.asList(PRIMARY));
        attr.addAll(Arrays.asList(AUTOCREATED));
        attr.addAll(Arrays.asList(MANDATORY));
        attr.addAll(Arrays.asList(PROTECTED));
        attr.addAll(Arrays.asList(MULTIPLE));
        attr.addAll(Arrays.asList(QUERYOPS));
        attr.addAll(Arrays.asList(NOFULLTEXT));
        attr.addAll(Arrays.asList(NOQUERYORDER));
        attr.addAll(Arrays.asList(COPY));
        attr.addAll(Arrays.asList(VERSION));
        attr.addAll(Arrays.asList(INITIALIZE));
        attr.addAll(Arrays.asList(COMPUTE));
        attr.addAll(Arrays.asList(IGNORE));
        attr.addAll(Arrays.asList(ABORT));
        PROP_ATTRIBUTE = attr.toArray(new String[attr.size()]);
        attr = new ArrayList<String>();
        attr.addAll(Arrays.asList(PRIMARY));
        attr.addAll(Arrays.asList(AUTOCREATED));
        attr.addAll(Arrays.asList(MANDATORY));
        attr.addAll(Arrays.asList(PROTECTED));
        attr.addAll(Arrays.asList(SNS));
        attr.addAll(Arrays.asList(COPY));
        attr.addAll(Arrays.asList(VERSION));
        attr.addAll(Arrays.asList(INITIALIZE));
        attr.addAll(Arrays.asList(COMPUTE));
        attr.addAll(Arrays.asList(IGNORE));
        attr.addAll(Arrays.asList(ABORT));
        NODE_ATTRIBUTE = attr.toArray(new String[attr.size()]);
    }

    public static final String QUEROPS_EQUAL = "=";
    public static final String QUEROPS_NOTEQUAL = "<>";
    public static final String QUEROPS_LESSTHAN = "<";
    public static final String QUEROPS_LESSTHANOREQUAL = "<=";
    public static final String QUEROPS_GREATERTHAN = ">";
    public static final String QUEROPS_GREATERTHANOREQUAL = ">=";
    public static final String QUEROPS_LIKE = "LIKE";

    public static final String[] STRING = {"STRING"};
    public static final String[] BINARY = {"BINARY"};
    public static final String[] LONG = {"LONG"};
    public static final String[] DOUBLE = {"DOUBLE"};
    public static final String[] BOOLEAN = {"BOOLEAN"};
    public static final String[] DATE = {"DATE"};
    public static final String[] NAME = {"NAME"};
    public static final String[] PATH = {"PATH"};
    public static final String[] REFERENCE = {"REFERENCE"};
    public static final String[] WEAKREFERENCE = {"WEAKREFERENCE"};
    public static final String[] URI = {"URI"};
    public static final String[] DECIMAL = {"DECIMAL"};

    public static final String[] UNDEFINED = new String[]{"UNDEFINED", "*"};

    public static final String EOF = "eof";

    private final StreamTokenizer st;

    private final String systemId;

    /**
     * Creates an unitialized lexer on top of the given reader.
     * @param r the reader
     * @param systemId informational systemid of the given stream
     */
    public Lexer(Reader r, String systemId) {
        this.systemId = systemId;
        st = new StreamTokenizer(r);

        st.eolIsSignificant(false);

        st.lowerCaseMode(false);

        st.slashSlashComments(true);
        st.slashStarComments(true);

        st.wordChars('a', 'z');
        st.wordChars('A', 'Z');
        st.wordChars(':', ':');
        st.wordChars('_', '_');

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
     * @return the next token
     * @throws ParseException if an error during parsing occurs
     */
    public String getNextToken() throws ParseException {
        try {
            int tokenType = st.nextToken();
            if (tokenType == StreamTokenizer.TT_EOF) {
                return EOF;
            } else if (tokenType == StreamTokenizer.TT_WORD
                    || tokenType == SINGLE_QUOTE
                    || tokenType == DOUBLE_QUOTE) {
                return st.sval;
            } else if (tokenType == StreamTokenizer.TT_NUMBER) {
                return String.valueOf(st.nval);
            } else {
                return new String(new char[] {(char) tokenType});
            }
        } catch (IOException e) {
            fail("IOException while attempting to read input stream", e);
            return null;
        }
    }

    /**
     * Returns the system id
     * @return the system id
     */
    public String getSystemId() {
        return systemId;
    }

    public int getLineNumber() {
        return st.lineno();
    }

    /**
     * Creates a failure exception including the current line number and systemid.
     * @param message message
     * @throws ParseException the created exception
     */
    public void fail(String message) throws ParseException {
        throw new ParseException(message, getLineNumber(), -1, systemId);
    }

    /**
     * Creates a failure exception including the current line number and systemid.
     * @param message message
     * @param e root cause
     * @throws ParseException the created exception
     */
    public void fail(String message, Throwable e) throws ParseException {
        throw new ParseException(message, e, getLineNumber(), -1, systemId);
    }

    /**
     * Creates a failure exception including the current line number and systemid.
     * @param e root cause
     * @throws ParseException the created exception
     */
    public void fail(Throwable e) throws ParseException {
        throw new ParseException(e, getLineNumber(), -1, systemId);
    }
}
