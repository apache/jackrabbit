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
package org.apache.jackrabbit.commons.query.sql2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.qom.BindVariableValue;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.Source;
import javax.jcr.query.qom.StaticOperand;

import org.apache.jackrabbit.commons.query.qom.JoinType;
import org.apache.jackrabbit.commons.query.qom.Operator;

/**
 * The SQL2 parser can convert a JCR-SQL2 query to a QueryObjectModel.
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
    private ArrayList<String> expected;

    // The bind variables
    private HashMap<String, BindVariableValue> bindVariables;

    // The list of selectors of this query
    private ArrayList<Selector> selectors;

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
     * Parse a JCR-SQL2 query and return the query object model
     *
     * @param query the query string
     * @return the query object model
     * @throws RepositoryException if parsing failed
     */
    public QueryObjectModel createQueryObjectModel(String query) throws RepositoryException {
        initialize(query);
        selectors = new ArrayList<Selector>();
        expected = new ArrayList<String>();
        bindVariables = new HashMap<String, BindVariableValue>();
        read();
        read("SELECT");
        int columnParseIndex = parseIndex;
        ArrayList<ColumnOrWildcard> list = parseColumns();
        read("FROM");
        Source source = parseSource();
        Column[] columnArray = resolveColumns(columnParseIndex, list);
        Constraint constraint = null;
        if (readIf("WHERE")) {
            constraint = parseConstraint();
        }
        Ordering[] orderings = null;
        if (readIf("ORDER")) {
            read("BY");
            orderings = parseOrder();
        }
        if (currentToken.length() > 0) {
            throw getSyntaxError("<end>");
        }
        return factory.createQuery(source, constraint, orderings, columnArray);
    }

    private Selector parseSelector() throws RepositoryException {
        String nodeTypeName = readName();
        if (readIf("AS")) {
            String selectorName = readName();
            return factory.selector(nodeTypeName, selectorName);
        } else {
            return factory.selector(nodeTypeName, nodeTypeName);
        }
    }

    private String readName() throws RepositoryException {
        if (readIf("[")) {
            if (currentTokenType == VALUE) {
                Value value = readString();
                read("]");
                return value.getString();
            } else {
                int level = 1;
                StringBuilder buff = new StringBuilder();
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

    private Source parseSource() throws RepositoryException {
        Selector selector = parseSelector();
        selectors.add(selector);
        Source source = selector;
        while (true) {
            JoinType type;
            if (readIf("RIGHT")) {
                read("OUTER");
                type = JoinType.RIGHT;
            } else if (readIf("LEFT")) {
                read("OUTER");
                type = JoinType.LEFT;
            } else if (readIf("INNER")) {
                type = JoinType.INNER;
            } else {
                break;
            }
            read("JOIN");
            selector = parseSelector();
            selectors.add(selector);
            read("ON");
            JoinCondition on = parseJoinCondition();
            source = type.join(factory, source, selector, on);
        }
        return source;
    }

    private JoinCondition parseJoinCondition() throws RepositoryException {
        boolean identifier = currentTokenType == IDENTIFIER;
        String name = readName();
        JoinCondition c;
        if (identifier && readIf("(")) {
            if ("ISSAMENODE".equalsIgnoreCase(name)) {
                String selector1 = readName();
                read(",");
                String selector2 = readName();
                if (readIf(",")) {
                    c = factory.sameNodeJoinCondition(selector1, selector2, readPath());
                } else {
                    c = factory.sameNodeJoinCondition(selector1, selector2, ".");
                }
            } else if ("ISCHILDNODE".equalsIgnoreCase(name)) {
                String childSelector = readName();
                read(",");
                c = factory.childNodeJoinCondition(childSelector, readName());
            } else if ("ISDESCENDANTNODE".equalsIgnoreCase(name)) {
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

    private Constraint parseCondition() throws RepositoryException {
        Constraint a;
        if (readIf("NOT")) {
            a = factory.not(parseConstraint());
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
                a = parseCondition(factory.propertyValue(getOnlySelectorName(identifier), identifier));
            }
        } else if ("[".equals(currentToken)) {
            String name = readName();
            if (readIf(".")) {
                a = parseCondition(factory.propertyValue(name, readName()));
            } else {
                a = parseCondition(factory.propertyValue(getOnlySelectorName(name), name));
            }
        } else {
            throw getSyntaxError();
        }
        return a;
    }

    private Constraint parseCondition(DynamicOperand left) throws RepositoryException {
        Constraint c;
        if (readIf("=")) {
            c = Operator.EQ.comparison(factory, left, parseStaticOperand());
        } else if (readIf("<>")) {
            c = Operator.NE.comparison(factory, left, parseStaticOperand());
        } else if (readIf("<")) {
            c = Operator.LT.comparison(factory, left, parseStaticOperand());
        } else if (readIf(">")) {
            c = Operator.GT.comparison(factory, left, parseStaticOperand());
        } else if (readIf("<=")) {
            c = Operator.LE.comparison(factory, left, parseStaticOperand());
        } else if (readIf(">=")) {
            c = Operator.GE.comparison(factory, left, parseStaticOperand());
        } else if (readIf("LIKE")) {
            c = Operator.LIKE.comparison(factory, left, parseStaticOperand());
        } else if (readIf("IS")) {
            boolean not = readIf("NOT");
            read("NULL");
            if (!(left instanceof PropertyValue)) {
                throw getSyntaxError("propertyName (NOT NULL is only supported for properties)");
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
                    throw new RepositoryException(
                            "Only property values can be tested for NOT IS NULL; got: "
                            + left.getClass().getName());
                }
                PropertyValue pv = (PropertyValue) left;
                c = getPropertyExistence(pv);
            } else {
                read("LIKE");
                c = factory.not(Operator.LIKE.comparison(
                        factory, left, parseStaticOperand()));
            }
        } else {
            throw getSyntaxError();
        }
        return c;
    }

    private PropertyExistence getPropertyExistence(PropertyValue p) throws InvalidQueryException, RepositoryException {
        return factory.propertyExistence(p.getSelectorName(), p.getPropertyName());
    }

    private Constraint parseConditionFuntionIf(String functionName) throws RepositoryException {
        Constraint c;
        if ("CONTAINS".equalsIgnoreCase(functionName)) {
            String name = readName();
            if (readIf(".")) {
                if (readIf("*")) {
                    read(",");
                    c = factory.fullTextSearch(
                            name, null, parseStaticOperand());
                } else {
                    String selector = name;
                    name = readName();
                    read(",");
                    c = factory.fullTextSearch(
                            selector, name, parseStaticOperand());
                }
            } else {
                read(",");
                c = factory.fullTextSearch(
                        getOnlySelectorName(name), name,
                        parseStaticOperand());
            }
        } else if ("ISSAMENODE".equalsIgnoreCase(functionName)) {
            String name = readName();
            if (readIf(",")) {
                c = factory.sameNode(name, readPath());
            } else {
                c = factory.sameNode(getOnlySelectorName(name), name);
            }
        } else if ("ISCHILDNODE".equalsIgnoreCase(functionName)) {
            String name = readName();
            if (readIf(",")) {
                c = factory.childNode(name, readPath());
            } else {
                c = factory.childNode(getOnlySelectorName(name), name);
            }
        } else if ("ISDESCENDANTNODE".equalsIgnoreCase(functionName)) {
            String name = readName();
            if (readIf(",")) {
                c = factory.descendantNode(name, readPath());
            } else {
                c = factory.descendantNode(getOnlySelectorName(name), name);
            }
        } else {
            return null;
        }
        read(")");
        return c;
    }

    private String readPath() throws RepositoryException {
        return readName();
    }

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
        if ("LENGTH".equalsIgnoreCase(functionName)) {
            op = factory.length(parsePropertyValue(readName()));
        } else if ("NAME".equalsIgnoreCase(functionName)) {
            if (isToken(")")) {
                op = factory.nodeName(getOnlySelectorName("NAME()"));
            } else {
                op = factory.nodeName(readName());
            }
        } else if ("LOCALNAME".equalsIgnoreCase(functionName)) {
            if (isToken(")")) {
                op = factory.nodeLocalName(getOnlySelectorName("LOCALNAME()"));
            } else {
                op = factory.nodeLocalName(readName());
            }
        } else if ("SCORE".equalsIgnoreCase(functionName)) {
            if (isToken(")")) {
                op = factory.fullTextSearchScore(getOnlySelectorName("SCORE()"));
            } else {
                op = factory.fullTextSearchScore(readName());
            }
        } else if ("LOWER".equalsIgnoreCase(functionName)) {
            op = factory.lowerCase(parseDynamicOperand());
        } else if ("UPPER".equalsIgnoreCase(functionName)) {
            op = factory.upperCase(parseDynamicOperand());
        } else {
            throw getSyntaxError("LENGTH, NAME, LOCALNAME, SCORE, LOWER, UPPER, or CAST");
        }
        read(")");
        return op;
    }

    private PropertyValue parsePropertyValue(String name) throws RepositoryException {
        if (readIf(".")) {
            return factory.propertyValue(name, readName());
        } else {
            return factory.propertyValue(getOnlySelectorName(name), name);
        }
    }

    private StaticOperand parseStaticOperand() throws RepositoryException {
        if (currentTokenType == PLUS) {
            read();
        } else if (currentTokenType == MINUS) {
            read();
            if (currentTokenType != VALUE) {
                throw getSyntaxError("number");
            }
            int valueType = currentValue.getType();
            switch (valueType) {
            case PropertyType.LONG:
                currentValue = valueFactory.createValue(-currentValue.getLong());
                break;
            case PropertyType.DOUBLE:
                currentValue = valueFactory.createValue(-currentValue.getDouble());
                break;
            case PropertyType.BOOLEAN:
                currentValue = valueFactory.createValue(!currentValue.getBoolean());
                break;
            case PropertyType.DECIMAL:
                currentValue = valueFactory.createValue(currentValue.getDecimal().negate());
                break;
            default:
                throw getSyntaxError("Illegal operation: -" + currentValue);
            }
        }
        if (currentTokenType == VALUE) {
            Literal literal = getUncastLiteral(currentValue);
            read();
            return literal;
        } else if (currentTokenType == PARAMETER) {
            read();
            String name = readName();
            if (readIf(":")) {
                name = name + ":" + readName();
            }
            BindVariableValue var = bindVariables.get(name);
            if (var == null) {
                var = factory.bindVariable(name);
                bindVariables.put(name, var);
            }
            return var;
        } else if (readIf("TRUE")) {
            Literal literal = getUncastLiteral(valueFactory.createValue(true));
            return literal;
        } else if (readIf("FALSE")) {
            Literal literal = getUncastLiteral(valueFactory.createValue(false));
            return literal;
        } else if (readIf("CAST")) {
            read("(");
            StaticOperand op = parseStaticOperand();
            if (!(op instanceof Literal)) {
                throw getSyntaxError("literal");
            }
            Literal literal = (Literal) op;
            Value value = literal.getLiteralValue();
            read("AS");
            value = parseCastAs(value);
            read(")");
            // CastLiteral
            literal = factory.literal(value);
            return literal;
        } else {
            throw getSyntaxError("static operand");
        }
    }

    /**
     * Create a literal from a parsed value.
     *
     * @param value the original value
     * @return the literal
     */
    private Literal getUncastLiteral(Value value) throws RepositoryException {
        return factory.literal(value);
    }

    private Value parseCastAs(Value value) throws RepositoryException {
        if (readIf("STRING")) {
            return valueFactory.createValue(value.getString());
        } else if(readIf("BINARY")) {
            return valueFactory.createValue(value.getBinary());
        } else if(readIf("DATE")) {
            return valueFactory.createValue(value.getDate());
        } else if(readIf("LONG")) {
            return valueFactory.createValue(value.getLong());
        } else if(readIf("DOUBLE")) {
            return valueFactory.createValue(value.getDouble());
        } else if(readIf("DECIMAL")) {
            return valueFactory.createValue(value.getDecimal());
        } else if(readIf("BOOLEAN")) {
            return valueFactory.createValue(value.getBoolean());
        } else if(readIf("NAME")) {
            return valueFactory.createValue(value.getString(), PropertyType.NAME);
        } else if(readIf("PATH")) {
            return valueFactory.createValue(value.getString(), PropertyType.PATH);
        } else if(readIf("REFERENCE")) {
            return valueFactory.createValue(value.getString(), PropertyType.REFERENCE);
        } else if(readIf("WEAKREFERENCE")) {
            return valueFactory.createValue(value.getString(), PropertyType.WEAKREFERENCE);
        } else if(readIf("URI")) {
            return valueFactory.createValue(value.getString(), PropertyType.URI);
        } else {
            throw getSyntaxError("data type (STRING|BINARY|...)");
        }
    }

    private Ordering[] parseOrder() throws RepositoryException {
        ArrayList<Ordering> orderList = new ArrayList<Ordering>();
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

    private ArrayList<ColumnOrWildcard> parseColumns() throws RepositoryException {
        ArrayList<ColumnOrWildcard> list = new ArrayList<ColumnOrWildcard>();
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
                        } else {
                            column.columnName = column.selectorName + "."
                                    + column.propertyName;
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

    private Column[] resolveColumns(int columnParseIndex, ArrayList<ColumnOrWildcard> list) throws RepositoryException {
        int oldParseIndex = parseIndex;
        // set the parse index to the column list, to get a more meaningful error message
        // if something is wrong
        this.parseIndex = columnParseIndex;
        try {
            ArrayList<Column> columns = new ArrayList<Column>();
            for (ColumnOrWildcard c : list) {
                if (c.propertyName == null) {
                    for (Selector selector : selectors) {
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
                        column = factory.column(getOnlySelectorName(c.propertyName), c.propertyName, c.columnName);
                    } else {
                        column = factory.column(getOnlySelectorName(c.propertyName), c.propertyName, c.propertyName);
                    }
                    columns.add(column);
                }
            }
            Column[] array = new Column[columns.size()];
            columns.toArray(array);
            return array;
        } finally {
            this.parseIndex = oldParseIndex;
        }
    }

    private boolean readIf(String token) throws RepositoryException {
        if (isToken(token)) {
            read();
            return true;
        }
        return false;
    }

    private boolean isToken(String token) {
        boolean result = token.equalsIgnoreCase(currentToken) && !currentTokenQuoted;
        if (result) {
            return true;
        }
        addExpected(token);
        return false;
    }

    private void read(String expected) throws RepositoryException {
        if (!expected.equalsIgnoreCase(currentToken) || currentTokenQuoted) {
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

    private Value readString() throws RepositoryException {
        if (currentTokenType != VALUE) {
            throw getSyntaxError("string value");
        }
        Value value = currentValue;
        read();
        return value;
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
                type = CHAR_STRING;
                types[i] = CHAR_STRING;
                startLoop = i;
                while (command[++i] != '\'') {
                    checkRunOver(i, len, startLoop);
                }
                break;
            case '\"':
                type = CHAR_QUOTED;
                types[i] = CHAR_QUOTED;
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
                    type = CHAR_NAME;
                } else if (c >= 'A' && c <= 'Z') {
                    type = CHAR_NAME;
                } else if (c >= '0' && c <= '9') {
                    type = CHAR_VALUE;
                } else {
                    if (Character.isJavaIdentifierPart(c)) {
                        type = CHAR_NAME;
                    }
                }
            }
            types[i] = (byte) type;
        }
        statementChars = command;
        types[len] = CHAR_END;
        characterTypes = types;
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
                    if (c == 'E' || c == 'e') {
                        readDecimal(start, i);
                        break;
                    }
                    checkLiterals(false);
                    currentValue = valueFactory.createValue(number);
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
        case CHAR_STRING:
            readString(i, '\'');
            return;
        case CHAR_QUOTED:
            readString(i, '\"');
            return;
        case CHAR_END:
            currentToken = "";
            currentTokenType = END;
            parseIndex = i;
            return;
        default:
            throw getSyntaxError();
        }
    }

    private void readString(int i, char end) throws RepositoryException {
        char[] chars = statementChars;
        String result = null;
        while (true) {
            for (int begin = i;; i++) {
                if (chars[i] == end) {
                    if (result == null) {
                        result = statement.substring(begin, i);
                    } else {
                        result += statement.substring(begin - 1, i);
                    }
                    break;
                }
            }
            if (chars[++i] != end) {
                break;
            }
            i++;
        }
        currentToken = "'";
        checkLiterals(false);
        currentValue = valueFactory.createValue(result);
        parseIndex = i;
        currentTokenType = VALUE;
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
        if (chars[i] == 'E' || chars[i] == 'e') {
            i++;
            if (chars[i] == '+' || chars[i] == '-') {
                i++;
            }
            if (types[i] != CHAR_VALUE) {
                throw getSyntaxError();
            }
            do {
                i++; // go until the first non-number
            } while (types[i] == CHAR_VALUE);
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

        currentValue = valueFactory.createValue(bd);
        currentTokenType = VALUE;
    }

    private InvalidQueryException getSyntaxError() {
        if (expected == null || expected.size() == 0) {
            return getSyntaxError(null);
        } else {
            StringBuilder buff = new StringBuilder();
            for (String exp : expected) {
                if (buff.length() > 0) {
                    buff.append(", ");
                }
                buff.append(exp);
            }
            return getSyntaxError(buff.toString());
        }
    }

    private InvalidQueryException getSyntaxError(String expected) {
        int index = Math.min(parseIndex, statement.length() - 1);
        String query = statement.substring(0, index) + "(*)" + statement.substring(index).trim();
        if (expected != null) {
            query += "; expected: " + expected;
        }
        return new InvalidQueryException("Query:\n" + query);
    }

    /**
     * Represents a column or a wildcard in a SQL expression.
     * This class is temporarily used during parsing.
     */
    static class ColumnOrWildcard {
        String selectorName;
        String propertyName;
        String columnName;
    }

    /**
     * Get the selector name if only one selector exists in the query.
     * If more than one selector exists, an exception is thrown.
     *
     * @param name the property name
     * @return the selector name
     */
    private String getOnlySelectorName(String propertyName) throws RepositoryException {
        if (selectors.size() > 1) {
            throw getSyntaxError("Need to specify the selector name for \"" + propertyName + "\" because the query contains more than one selector.");
        }
        return selectors.get(0).getSelectorName();
    }

}
