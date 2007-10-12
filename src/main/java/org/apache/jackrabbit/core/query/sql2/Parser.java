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
package org.apache.jackrabbit.core.query.sql2;

import org.apache.jackrabbit.core.query.jsr283.PreparedQuery;
import org.apache.jackrabbit.core.query.jsr283.qom.BindVariableValue;
import org.apache.jackrabbit.core.query.jsr283.qom.Column;
import org.apache.jackrabbit.core.query.jsr283.qom.Constraint;
import org.apache.jackrabbit.core.query.jsr283.qom.DynamicOperand;
import org.apache.jackrabbit.core.query.jsr283.qom.JoinCondition;
import org.apache.jackrabbit.core.query.jsr283.qom.Literal;
import org.apache.jackrabbit.core.query.jsr283.qom.Ordering;
import org.apache.jackrabbit.core.query.jsr283.qom.PropertyExistence;
import org.apache.jackrabbit.core.query.jsr283.qom.PropertyValue;
import org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants;
import org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelFactory;
import org.apache.jackrabbit.core.query.jsr283.qom.Selector;
import org.apache.jackrabbit.core.query.jsr283.qom.Source;
import org.apache.jackrabbit.core.query.jsr283.qom.StaticOperand;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.InvalidQueryException;

/**
 * The SQL2 parser can convert a JCR-SQL2 query to a PreparedQuery.
 */
public class Parser {

    // Character types, used during the tokenizer phase
    private static final int CHAR_END = -1, CHAR_VALUE = 2, CHAR_QUOTED = 3;
    private static final int CHAR_NAME = 4, CHAR_SPECIAL_1 = 5, CHAR_SPECIAL_2 = 6;
    private static final int CHAR_STRING = 7, CHAR_DECIMAL = 8;

    // Token types
    private static final int KEYWORD = 1, IDENTIFIER = 2, PARAMETER = 3, END = 4, VALUE = 5;
    private static final int MINUS = 12, PLUS = 13, OPEN = 14, CLOSE = 15;

    // The query as an array of characters and character types
    private String statement;
    private char[] statementChars;
    private int[] characterTypes;

    // The current state of the parser
    private int parseIndex;
    private int currentTokenType;
    private String currentToken;
    private boolean currentTokenQuoted;
    private Value currentValue;
    private ArrayList expected;

    // The bind variables
    private HashMap bindVariables;
    
    // The list of selectors of this query
    private ArrayList selectors;
    
    // SQL injection protection: if disabled, literals are not allowed
    private boolean allowTextLiterals = true, allowNumberLiterals = true;

    private QueryObjectModelFactory factory;
    private ValueFactory valueFactory;

    /**
     * Create a new parser. A parser can be re-used, but it is not thread safe.
     * 
     * @param factory the query object model factory
     * @param valueFactory the value factory
     */
    public Parser(QueryObjectModelFactory factory, ValueFactory valueFactory) {
        this.factory = factory;
        this.valueFactory = valueFactory;
    }

    /**
     * Parse a JCR-SQL2 query and return the prepared query.
     * 
     * @param query the query string
     * @return the prepared query
     * @throws RepositoryException if parsing failed
     */
    // Page 125
    public PreparedQuery createPreparedQuery(String query) throws RepositoryException {
        initialize(query);
        selectors = new ArrayList();
        expected = new ArrayList();
        bindVariables = new HashMap();
        read();
        read("SELECT");
        ArrayList list = parseColumns();
        read("FROM");
        Source source = parseSource();
        Column[] columnArray = resolveColumns(list);
        Constraint constraint = null;
        if (readIf("WHERE")) {
            constraint = parseConstraint();
        }
        Ordering[] orderings = null;
        if (readIf("ORDER")) {
            read("BY");
            orderings = parseOrder();
        }
        return factory.createQuery(source, constraint, orderings, columnArray);
    }    

    // Page 127
    private Selector parseSelector() throws RepositoryException {
        String nodeTypeName = readName();
        if (readIf("AS")) {
            String selectorName = readName();
            return factory.selector(nodeTypeName, selectorName);
        } else {
            return factory.selector(nodeTypeName);
        }
    }
    
    // Page 128
    private String readName() throws RepositoryException {
        if (readIf("[")) {
            if (currentTokenType == VALUE) {
                String s = readString();
                read("]");
                return s;
            } else {
                int level = 1;
                StringBuffer buff = new StringBuffer();
                while (true) {
                    if (isToken("]")) {
                        if (--level <= 0) {
                            read();
                            break;
                        }
                    } else if (isToken("[")) {
                        level++;
                    }
                    buff.append(readAny());
                }
                return buff.toString();
            }
        } else {
            return readAny();
        }
    }
    
    // Page 129
    private Source parseSource() throws RepositoryException {
        Selector selector = parseSelector();
        selectors.add(selector);
        Source source = selector;
        while (true) {
            int type;
            if (readIf("RIGHT")) {
                read("OUTER");
                type = QueryObjectModelConstants.JOIN_TYPE_RIGHT_OUTER;
            } else if (readIf("LEFT")) {
                read("OUTER");
                type = QueryObjectModelConstants.JOIN_TYPE_LEFT_OUTER;
            } else if (readIf("INNER")) {
                type = QueryObjectModelConstants.JOIN_TYPE_INNER;
            } else {
                break;
            }
            read("JOIN");
            selector = parseSelector();
            selectors.add(selector);
            read("ON");
            JoinCondition on = parseJoinCondition();
            source = factory.join(source, selector, type, on);
        }
        return source;
    }
    
    // Page 130
    private JoinCondition parseJoinCondition() throws RepositoryException {
        boolean identifier = currentTokenType == IDENTIFIER;
        String name = readName();
        JoinCondition c;
        if (identifier && readIf("(")) {
            if ("ISSAMENODE".equals(name)) {
                String selector1 = readName();
                read(",");
                String selector2 = readName();
                if (readIf(",")) {
                    c = factory.sameNodeJoinCondition(selector1, selector2, readPath());
                } else {
                    c = factory.sameNodeJoinCondition(selector1, selector2);
                }
            } else if ("ISCHILDNODE".equals(name)) {
                String childSelector = readName();
                read(",");
                c = factory.childNodeJoinCondition(childSelector, readName());
            } else if ("ISDESCENDANTNODE".equals(name)) {
                String descendantSelector = readName();
                read(",");
                c = factory.descendantNodeJoinCondition(descendantSelector, readName());
            } else {
                throw getSyntaxError("ISSAMENODE, ISCHILDNODE, or ISDESCENDANTNODE");
            }
            read(")");
            return c;
        } else {
            String selector1 = name;
            read(".");
            String property1 = readName();
            read("=");
            String selector2 = readName();
            read(".");
            return factory.equiJoinCondition(selector1, property1, selector2, readName());
        }
    }
    
    // Page 136
    private Constraint parseConstraint() throws RepositoryException {
        Constraint a = parseAnd();
        while (readIf("OR")) {
            a = factory.or(a, parseAnd());
        }
        return a;
    }

    private Constraint parseAnd() throws RepositoryException {
        Constraint a = parseCondition();
        while (readIf("AND")) {
            a = factory.and(a, parseCondition());
        }
        return a;
    }
    
    // Page 138
    private Constraint parseCondition() throws RepositoryException {
        Constraint a;
        if (readIf("NOT")) {
            a = parseConstraint();
        } else if (readIf("(")) {
            a = parseConstraint();
            read(")");
        } else if (currentTokenType == IDENTIFIER) {
            String identifier = readName();
            if (readIf("(")) {
                a = parseConditionFuntionIf(identifier);
                if (a == null) {
                    DynamicOperand op = parseExpressionFunction(identifier);
                    a = parseCondition(op);
                }
            } else if (readIf(".")) {
                a = parseCondition(factory.propertyValue(identifier, readName()));
            } else {
                a = parseCondition(factory.propertyValue(identifier));
            }
        } else {
            throw getSyntaxError();
        }
        return a;
    }
    
    // Page 141
    private Constraint parseCondition(DynamicOperand left) throws RepositoryException {
        Constraint c;
        if (readIf("=")) {
            c = factory.comparison(left,
                    QueryObjectModelConstants.OPERATOR_EQUAL_TO,
                    parseStaticOperand());
        } else if (readIf("<>")) {
            c = factory.comparison(left,
                    QueryObjectModelConstants.OPERATOR_NOT_EQUAL_TO,
                    parseStaticOperand());
        } else if (readIf("<")) {
            c = factory.comparison(left,
                    QueryObjectModelConstants.OPERATOR_LESS_THAN,
                    parseStaticOperand());
        } else if (readIf(">")) {
            c = factory.comparison(left,
                    QueryObjectModelConstants.OPERATOR_GREATER_THAN,
                    parseStaticOperand());
        } else if (readIf("<=")) {
            c = factory.comparison(left,
                    QueryObjectModelConstants.OPERATOR_LESS_THAN_OR_EQUAL_TO,
                    parseStaticOperand());
        } else if (readIf(">=")) {
            c = factory
                    .comparison(
                            left,
                            QueryObjectModelConstants.OPERATOR_GREATER_THAN_OR_EQUAL_TO,
                            parseStaticOperand());
        } else if (readIf("LIKE")) {
            c = factory.comparison(left,
                    QueryObjectModelConstants.OPERATOR_LIKE,
                    parseStaticOperand());
        } else if (readIf("IS")) {
            boolean not = readIf("NOT");
            read("NULL");
            if (!(left instanceof PropertyValue)) {
                this.getSyntaxError("propertyName (NOT NULL is only supported for properties)");
            }
            PropertyValue p = (PropertyValue) left;
            c = getPropertyExistence(p);
            if (!not) {
                c = factory.not(c);
            }
        } else if (readIf("NOT")) {
            if (readIf("IS")) {
                read("NULL");
                if (!(left instanceof PropertyValue)) {
                    throw new RepositoryException("Only property values can be tested for NOT IS NULL; got: " + left.getClass().getName());
                }
                PropertyValue pv = (PropertyValue) left;
                c = getPropertyExistence(pv);
            } else {
                read("LIKE");
                c = factory.not(factory.comparison(left,
                        QueryObjectModelConstants.OPERATOR_LIKE,
                        parseStaticOperand()));
            }
        } else {
            throw getSyntaxError();
        }
        return c;
    }
    
    private PropertyExistence getPropertyExistence(PropertyValue p) throws InvalidQueryException, RepositoryException {
        if (p.getSelectorName() == null) {
            return factory.propertyExistence(p.getPropertyName());
        } else {
            return factory.propertyExistence(p.getSelectorName(), p.getPropertyName());
        }
    }
    
    // Page 144
    private Constraint parseConditionFuntionIf(String functionName) throws RepositoryException {
        Constraint c;
        if ("CONTAINS".equals(functionName)) {
            String name = readName();
            if (readIf(".")) {
                if (readIf("*")) {
                    read(",");
                    c = factory.fullTextSearch(name, null, readString());
                } else {
                    String selector = name;
                    name = readName();
                    read(",");
                    c = factory.fullTextSearch(selector, name, readString());
                }
            } else {
                read(",");
                c = factory.fullTextSearch(name, readString());
            }
        } else if ("ISSAMENODE".equals(functionName)) {
            String name = readName();
            if (readIf(",")) {
                c = factory.sameNode(name, readPath());
            } else {
                c = factory.sameNode(name);
            }
        } else if ("ISCHILDNODE".equals(functionName)) {
            String name = readName();
            if (readIf(",")) {
                c = factory.childNode(name, readPath());
            } else {
                c = factory.childNode(name);
            }
        } else if ("ISDESCENDANTNODE".equals(functionName)) {
            String name = readName();
            if (readIf(",")) {
                c = factory.descendantNode(name, readPath());
            } else {
                c = factory.descendantNode(name);
            }
        } else {
            return null;
        }
        read(")");
        return c;
    }
    
    // Page 148
    private String readPath() throws RepositoryException {
        return readName();
    }
    
    // Page 149
    private DynamicOperand parseDynamicOperand() throws RepositoryException {
        boolean identifier = currentTokenType == IDENTIFIER;        
        String name = readName();
        if (identifier && readIf("(")) {
            return parseExpressionFunction(name);
        } else {
            return parsePropertyValue(name);
        }
    }
    
    private DynamicOperand parseExpressionFunction(String functionName) throws RepositoryException {
        DynamicOperand op;
        if ("LENGTH".equals(functionName)) {
            op = factory.length(parsePropertyValue(readName()));
        } else if ("NAME".equals(functionName)) {
            if (isToken(")")) {
                op = factory.nodeName();
            } else {
                op = factory.nodeName(readName());
            }
        } else if ("LOCALNAME".equals(functionName)) {
            if (isToken(")")) {
                op = factory.nodeLocalName();
            } else {
                op = factory.nodeLocalName(readName());
            }
        } else if ("SCORE".equals(functionName)) {
            if (isToken(")")) {
                op = factory.fullTextSearchScore();
            } else {
                op = factory.fullTextSearchScore(readName());
            }
        } else if ("LOWER".equals(functionName)) {
            op = factory.lowerCase(parseDynamicOperand());
        } else if ("UPPER".equals(functionName)) {
            op = factory.upperCase(parseDynamicOperand());
        } else {
            throw getSyntaxError("LENGTH, NAME, LOCALNAME, SCORE, LOWER, or UPPER");
        }
        read(")");
        return op;
    }
    
    // Page 150
    private PropertyValue parsePropertyValue(String name) throws RepositoryException {
        if (readIf(".")) {
            return factory.propertyValue(name, readName());
        } else {
            return factory.propertyValue(name);
        }
    }
    
    // Page 155
    private StaticOperand parseStaticOperand() throws RepositoryException {
        if (currentTokenType == PLUS) {
            read();
        } else if (currentTokenType == MINUS) {
            read();
            if (currentTokenType != VALUE) {
                throw getSyntaxError("number");
            }
            if (currentValue.getType() == PropertyType.LONG) {
                currentValue = valueFactory.createValue(-currentValue.getLong());
            } else if (currentValue.getType() == PropertyType.DOUBLE) {
                currentValue = valueFactory.createValue(-currentValue.getDouble());
            } else {
                // TODO decimal
                throw getSyntaxError("number");
            }
        } 
        if (currentTokenType == VALUE) {
            Literal literal = factory.literal(currentValue);
            read();
            return literal;
        } else if (currentTokenType == PARAMETER) {
            read();
            String name = readName();
            BindVariableValue var = (BindVariableValue) bindVariables.get(name);
            if (var == null) {
                var = factory.bindVariable(name);
                bindVariables.put(name, var);
            }
            return var;
        } else if (readIf("TRUE")) {
            Literal literal = factory.literal(valueFactory.createValue(true));
            return literal;
        } else if (readIf("FALSE")) {
            Literal literal = factory.literal(valueFactory.createValue(false));
            return literal;
        } else {
            throw getSyntaxError("static operand");
        }
    }

    // Page 157
    private Ordering[] parseOrder() throws RepositoryException {
        ArrayList orderList = new ArrayList();
        do {
            Ordering ordering;
            DynamicOperand op = parseDynamicOperand();
            if (readIf("DESC")) {
                ordering = factory.descending(op);
            } else {
                readIf("ASC");
                ordering = factory.ascending(op);
            }
            orderList.add(ordering);
        } while (readIf(","));
        Ordering[] orderings = new Ordering[orderList.size()];
        orderList.toArray(orderings);
        return orderings;
    }
    
    // Page 159
    private ArrayList parseColumns() throws RepositoryException {
        ArrayList list = new ArrayList();        
        if (readIf("*")) {
            list.add(new ColumnOrWildcard());
        } else {
            do {
                ColumnOrWildcard column = new ColumnOrWildcard();
                column.propertyName = readName();
                if (readIf(".")) {
                    column.selectorName = column.propertyName;
                    if (readIf("*")) {
                        column.propertyName = null;
                    } else {
                        column.propertyName = readName();
                        if (readIf("AS")) {
                            column.columnName = readName();
                        }
                    }
                } else {
                    if (readIf("AS")) {
                        column.columnName = readName();
                    }
                }
                list.add(column);
            } while (readIf(","));
        }
        return list;
    }
    
    private Column[] resolveColumns(ArrayList list) throws RepositoryException {
        ArrayList columns = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            ColumnOrWildcard c = (ColumnOrWildcard) list.get(i);
            if (c.propertyName == null) {
                for (int j = 0; j < selectors.size(); j++) {
                    Selector selector = (Selector) selectors.get(j);
                    if (c.selectorName == null
                            || c.selectorName
                                    .equals(selector.getSelectorName())) {
                        Column column = factory.column(selector
                                .getSelectorName(), null, null);
                        columns.add(column);
                    }
                }
            } else {
                Column column;
                if (c.selectorName != null) {
                    column = factory.column(c.selectorName, c.propertyName, c.columnName);
                } else if (c.columnName != null) {
                    column = factory.column(c.propertyName, c.columnName);
                } else {
                    column = factory.column(c.propertyName);
                }
                columns.add(column);
            }
        }
        Column[] array = new Column[columns.size()];
        columns.toArray(array);
        return array;
    }
    
    private boolean readIf(String token) throws RepositoryException {
        if (isToken(token)) {
            read();
            return true;
        }
        return false;
    }

    private boolean isToken(String token) {
        boolean result = token.equals(currentToken) && !currentTokenQuoted;
        if (result) {
            return true;
        }
        addExpected(token);
        return false;
    }

    private void read(String expected) throws RepositoryException {
        if (!expected.equals(currentToken) || currentTokenQuoted) {
            throw getSyntaxError(expected);
        }
        read();
    }

    private String readAny() throws RepositoryException {
        if (currentTokenType == END) {
            throw getSyntaxError("a token");
        }
        String s;
        if (currentTokenType == VALUE) {
            s = currentValue.getString();
        } else {
            s = currentToken;
        }
        read();
        return s;
    }

    private String readString() throws RepositoryException {
        if (currentTokenType != VALUE) {
            throw getSyntaxError("string value");
        }
        String s = currentValue.getString();
        read();
        return s;
    }    

    private void addExpected(String token) {
        if (expected != null) {
            expected.add(token);
        }
    }

    private void initialize(String query) throws InvalidQueryException {
        if (query == null) {
            query = "";
        }
        statement = query;
        int len = query.length() + 1;
        char[] command = new char[len];
        int[] types = new int[len];
        len--;
        query.getChars(0, len, command, 0);
        boolean changed = false;
        command[len] = ' ';
        int startLoop = 0;
        for (int i = 0; i < len; i++) {
            char c = command[i];
            int type = 0;
            switch (c) {
            case '/':
            case '-':
            case '(':
            case ')':
            case '{':
            case '}':
            case '*':
            case ',':
            case ';':
            case '+':
            case '%':
            case '?':
            case '$':
            case '[':
            case ']':
                type = CHAR_SPECIAL_1;
                break;
            case '!':
            case '<':
            case '>':
            case '|':
            case '=':
            case ':':
                type = CHAR_SPECIAL_2;
                break;
            case '.':
                type = CHAR_DECIMAL;
                break;
            case '\'':
                type = types[i] = CHAR_STRING;
                startLoop = i;
                while (command[++i] != '\'') {
                    checkRunOver(i, len, startLoop);
                }
                break;
            case '\"':
                type = types[i] = CHAR_QUOTED;
                startLoop = i;
                while (command[++i] != '\"') {
                    checkRunOver(i, len, startLoop);
                }
                break;
            case '_':
                type = CHAR_NAME;
                break;
            default:
                if (c >= 'a' && c <= 'z') {
                    command[i] = (char) (c - ('a' - 'A'));
                    changed = true;
                    type = CHAR_NAME;
                } else if (c >= 'A' && c <= 'Z') {
                    type = CHAR_NAME;
                } else if (c >= '0' && c <= '9') {
                    type = CHAR_VALUE;
                } else {
                    if (Character.isJavaIdentifierPart(c)) {
                        type = CHAR_NAME;
                        char u = Character.toUpperCase(c);
                        if (u != c) {
                            command[i] = u;
                            changed = true;
                        }
                    }
                }
            }
            types[i] = (byte) type;
        }
        statementChars = command;
        types[len] = CHAR_END;
        characterTypes = types;
        if (changed) {
            statement = new String(command);
        }
        parseIndex = 0;
    }

    private void checkRunOver(int i, int len, int startLoop) throws InvalidQueryException {
        if (i >= len) {
            parseIndex = startLoop;
            throw getSyntaxError();
        }
    }
    
    private void read() throws RepositoryException {
        currentTokenQuoted = false;
        if (expected != null) {
            expected.clear();
        }
        int[] types = characterTypes;
        int i = parseIndex;
        int type = types[i];
        while (type == 0) {
            type = types[++i];
        }
        int start = i;
        char[] chars = statementChars;
        char c = chars[i++];
        currentToken = "";
        switch (type) {
        case CHAR_NAME:
            while (true) {
                type = types[i];
                if (type != CHAR_NAME && type != CHAR_VALUE) {
                    c = chars[i];
                    break;
                }
                i++;
            }
            currentToken = statement.substring(start, i);
            if (currentToken.length() == 0) {
                throw getSyntaxError();
            }
            currentTokenType = IDENTIFIER;
            parseIndex = i;
            return;
        case CHAR_QUOTED: {
            String result = null;
            while (true) {
                for (int begin = i;; i++) {
                    if (chars[i] == '\"') {
                        if (result == null) {
                            result = statement.substring(begin, i);
                        } else {
                            result += statement.substring(begin - 1, i);
                        }
                        break;
                    }
                }
                if (chars[++i] != '\"') {
                    break;
                }
                i++;
            }
            currentToken = result;
            parseIndex = i;
            currentTokenQuoted = true;
            currentTokenType = IDENTIFIER;
            return;
        }
        case CHAR_SPECIAL_2:
            if (types[i] == CHAR_SPECIAL_2) {
                i++;
            }
            // fall through
        case CHAR_SPECIAL_1:
            currentToken = statement.substring(start, i);
            switch (c) {
            case '$':
                currentTokenType = PARAMETER;
                break;
            case '+':
                currentTokenType = PLUS;
                break;
            case '-':
                currentTokenType = MINUS;
                break;
            case '(':
                currentTokenType = OPEN;
                break;
            case ')':
                currentTokenType = CLOSE;
                break;
            default:
                currentTokenType = KEYWORD;
            }
            parseIndex = i;
            return;
        case CHAR_VALUE:
            long number = c - '0';
            while (true) {
                c = chars[i];
                if (c < '0' || c > '9') {
                    if (c == '.') {
                        readDecimal(start, i);
                        break;
                    }
                    if (c == 'E') {
                        readDecimal(start, i);
                        break;
                    }
                    checkLiterals(false);
                    currentValue = valueFactory.createValue((int) number);
                    currentTokenType = VALUE;
                    currentToken = "0";
                    parseIndex = i;
                    break;
                }
                number = number * 10 + (c - '0');
                if (number > Integer.MAX_VALUE) {
                    readDecimal(start, i);
                    break;
                }
                i++;
            }
            return;
        case CHAR_DECIMAL:
            if (types[i] != CHAR_VALUE) {
                currentTokenType = KEYWORD;
                currentToken = ".";
                parseIndex = i;
                return;
            }
            readDecimal(i - 1, i);
            return;
        case CHAR_STRING: {
            String result = null;
            while (true) {
                for (int begin = i;; i++) {
                    if (chars[i] == '\'') {
                        if (result == null) {
                            result = statement.substring(begin, i);
                        } else {
                            result += statement.substring(begin - 1, i);
                        }
                        break;
                    }
                }
                if (chars[++i] != '\'') {
                    break;
                }
                i++;
            }
            currentToken = "'";
            checkLiterals(false);
            currentValue = valueFactory.createValue(result);
            parseIndex = i;
            currentTokenType = VALUE;
            return;
        }
        case CHAR_END:
            currentToken = "";
            currentTokenType = END;
            parseIndex = i;
            return;
        default:
            throw getSyntaxError();
        }
    }
    
    private void checkLiterals(boolean text) throws InvalidQueryException {
        if (text && !allowTextLiterals || (!text && !allowNumberLiterals)) {
            throw getSyntaxError("bind variable (literals of this type not allowed)");
        }
    }

    private void readDecimal(int start, int i) throws RepositoryException {
        char[] chars = statementChars;
        int[] types = characterTypes;
        while (true) {
            int t = types[i];
            if (t != CHAR_DECIMAL && t != CHAR_VALUE) {
                break;
            }
            i++;
        }
        if (chars[i] == 'E') {
            i++;
            if (chars[i] == '+' || chars[i] == '-') {
                i++;
            }
            if (types[i] != CHAR_VALUE) {
                throw getSyntaxError();
            }
            while (types[++i] == CHAR_VALUE) {
                // go until the first non-number
            }
        }
        parseIndex = i;
        String sub = statement.substring(start, i);
        BigDecimal bd;
        try {
            bd = new BigDecimal(sub);
        } catch (NumberFormatException e) {
            throw new InvalidQueryException("Data conversion error converting " + sub + " to BigDecimal: " + e);
        }
        checkLiterals(false);
        // TODO BigDecimal or double?
        currentValue = valueFactory.createValue(bd.doubleValue());
        currentTokenType = VALUE;
    }
    
    private InvalidQueryException getSyntaxError() {
        if (expected == null || expected.size() == 0) {
            return getSyntaxError(null);
        } else {
            StringBuffer buff = new StringBuffer();
            for (int i = 0; i < expected.size(); i++) {
                if (i > 0) {
                    buff.append(", ");
                }
                buff.append(expected.get(i));
            }
            return getSyntaxError(buff.toString());
        }
    }
    
    private InvalidQueryException getSyntaxError(String expected) {
        int index = Math.min(parseIndex, statement.length() - 1);
        String query = statement.substring(0, index) + ">*<" + statement.substring(index).trim();
        if (expected != null) {
            query += "; expected: " + expected;
        }
        return new InvalidQueryException("Query:\n" + query);
    }    
    
    private static class ColumnOrWildcard {
        String selectorName;
        String propertyName;
        String columnName;
    }
    
}
