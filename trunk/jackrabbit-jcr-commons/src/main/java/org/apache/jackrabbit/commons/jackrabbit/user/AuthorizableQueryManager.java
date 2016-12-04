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
package org.apache.jackrabbit.commons.jackrabbit.user;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.QueryBuilder;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.json.JsonHandler;
import org.apache.jackrabbit.commons.json.JsonParser;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * This class handles the translation of queries for users and groups from a
 * JSON format to the query model of Jackrabbit's user groups search
 * (see {@link org.apache.jackrabbit.api.security.user.UserManager#findAuthorizables(org.apache.jackrabbit.api.security.user.Query)
 * UserManager#findAuthorizables(Query)}).
 *
 * The JSON query format is defined as follows:
 * <pre>
{
  ( selector: "authorizable" | "user" | "group" )?        // Defaults to "authorizable", see QueryBuilder#setSelector()

  (
    scope:                                                // See QueryBuilder#setScope()
    {
      groupName: /* group name (String) * /
      ( declaredOnly: true | false )                      // Defaults to true
    }
  ) ?                                                     // Defaults to all

  ( condition: [ CONJUNCTION+ ] ) ?                       // Defaults to a 'true' condition, see QueryBuilder#setCondition()

  (
    order | sort:                                         // See QueryBuilder#setOrder()
    {
      property: /* relative path (String) * /
      ( direction: "asc" | "desc" )                       // Defaults to "asc"
      ( ignoreCase: true | false )                        // Defaults to "true", see QueryBuilder#setSortOrder()
    }
  ) ?                                                     // Defaults to document order

  (
    limit:                                                // See QueryBuilder#setLimit()
    {
      offset: /* Positive Integer * /                     // Takes precedence over bound if both are given
      bound:  /* String, Number, Boolean * /
      max:    /* Positive Integer or -1 * /               // Defaults to no limit (-1)
    }
  ) ?                                                     // Defaults to all
}

CONJUNCTION ::= COMPOUND | PRIMITIVE
COMPOUND    ::= [ PRIMITIVE+ ]
PRIMITIVE   ::= { ATOM | NEGATION }
NEGATION    ::= not: { ATOM }                             // See QueryBuilder#not()
ATOM        ::= named: /* pattern * /                     // Users, groups of that name. See QueryBuilder#nameMatches()
            |   exists: /* relative path * /              // See QueryBuilder#exists()
            |   impersonates: /* authorizable name * /    // See QueryBuilder#impersonates()
            |   RELOP:
                {
                  property: /* relative path * /
                  value: /* String, Number, Boolean * /   // According to the type of the property
                }
            |   like:                                     // See QueryBuilder#like()
                {
                  property: /* relative path * /
                  pattern: /* pattern * /
                }
            |   contains:                                 // See QueryBuilder#contains()
                {
                  property: /* relative path * /
                  expression: /* search expression * /
                }
RELOP       ::= neq | eq | lt | le | gt | ge              // See QueryBuilder#neq(), QueryBuilder#eq(), ...
</pre>
 *
 * <ul>
 * <li>A relative path refers to a property or a child node of an user or a group. Property names need to be
 * prefixed with the at (@) character. Invalid JCR characters need proper escaping. The current path is denoted
 * by a dot (.).</li>
 * <li>In a 'pattern' the percent character (%) represents any string of zero or more characters and the underscore
 * character (_) represents any single character. Any literal use of these characters and the backslash
 * character (\) must be escaped with a backslash character. The pattern is matched against
 * Authorizable#getID() and Authorizable#getPrincipal().</li>
 * <li>The syntax of 'expression' is [-]value { [OR] [-]value }.</li>
 * </ul>
 */
public class AuthorizableQueryManager {

    /**
     * Constant defining the default maximal size of the result set.
     */
    public static final int MAX_RESULT_COUNT = 2000;

    private final UserManager userManager;
    private final ValueFactory valueFactory;

    public AuthorizableQueryManager(UserManager userManager, ValueFactory valueFactory) {
        this.userManager = userManager;
        this.valueFactory = valueFactory;
    }

    public Iterator<Authorizable> execute(final String query) throws RepositoryException, IOException {
        try {
            return userManager.findAuthorizables(new Query() {
                public <T> void build(QueryBuilder<T> builder) {
                    try {
                        // Must request more than MAX_RESULT_COUNT records explicitly
                        builder.setLimit(0, MAX_RESULT_COUNT);
                        new QueryTranslator<T>(builder).translate(query);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            } else {
                throw e;
            }
        }
    }

    //------------------------------------------------------------< private >---

    private class QueryTranslator<T> implements JsonHandler {
        private final QueryBuilder<T> queryBuilder;
        private final Stack<JsonHandler> handlers = new Stack<JsonHandler>();

        public QueryTranslator(QueryBuilder<T> queryBuilder) {
            this.queryBuilder = queryBuilder;
            handlers.push(new HandlerBase() {
                @Override
                public void object() {
                    handlers.push(new ClausesHandler());
                }
            });
        }

        public void translate(String query) throws IOException {
            new JsonParser(this).parse(query);
            if (handlers.size() != 1) {
                throw new IOException("Missing closing parenthesis");
            }
        }

        public void object() throws IOException {
            handlers.peek().object();
        }

        public void endObject() throws IOException {
            handlers.peek().endObject();
        }

        public void array() throws IOException {
            handlers.peek().array();
        }

        public void endArray() throws IOException {
            handlers.peek().endArray();
        }

        public void key(String s) throws IOException {
            handlers.peek().key(s);
        }

        public void value(String s) throws IOException {
            handlers.peek().value(s);
        }

        public void value(boolean b) throws IOException {
            handlers.peek().value(b);
        }

        public void value(long l) throws IOException {
            handlers.peek().value(l);
        }

        public void value(double v) throws IOException {
            handlers.peek().value(v);
        }

        private Value valueFor(String s) {
            return valueFactory.createValue(s);
        }

        private Value valueFor(boolean b) {
            return valueFactory.createValue(b);
        }

        private Value valueFor(long l) {
            return valueFactory.createValue(l);
        }

        private Value valueFor(double v) {
            return valueFactory.createValue(v);
        }

        //----------------------------------------------------< HandlerBase >---

        private class HandlerBase implements JsonHandler {

            public void object() throws IOException {
                throw new IOException("Syntax error: '{'");
            }

            public void endObject() throws IOException {
                throw new IOException("Syntax error: '}'");
            }

            public void array() throws IOException {
                throw new IOException("Syntax error: '['");
            }

            public void endArray() throws IOException {
                throw new IOException("Syntax error: ']'");
            }

            public void key(String s) throws IOException {
                throw new IOException("Syntax error: key '" + s + '\'');
            }

            public void value(String s) throws IOException {
                throw new IOException("Syntax error: string '" + s + '\'');
            }

            public void value(boolean b) throws IOException {
                throw new IOException("Syntax error: boolean '" + b + '\'');
            }

            public void value(long l) throws IOException {
                throw new IOException("Syntax error: long '" + l + '\'');
            }

            public void value(double v) throws IOException {
                throw new IOException("Syntax error: double '" + v + '\'');
            }
        }

        //-------------------------------------------------< ClausesHandler >---

        private class ClausesHandler extends HandlerBase {
            private String currentKey;

            @Override
            public void object() throws IOException {
                handlers.push(handlerFor(currentKey));
            }

            @Override
            public void endObject() throws IOException {
                handlers.pop();
            }

            @Override
            public void array() throws IOException {
                handlers.push(handlerFor(currentKey));
            }

            @Override
            public void endArray() throws IOException {
                handlers.pop();
            }

            @Override
            public void key(String s) throws IOException {
                currentKey = s;
            }

            @Override
            public void value(String s) throws IOException {
                if ("selector".equals(currentKey)) {
                    queryBuilder.setSelector(selectorFor(s));
                } else {
                    throw new IOException("String value '" + s + "' is invalid for '" + currentKey + '\'');
                }
            }

            private Class<? extends Authorizable> selectorFor(String selector) throws IOException {
                if ("user".equals(selector)) {
                    return User.class;
                } else if ("group".equals(selector)) {
                    return Group.class;
                } else if ("authorizable".equals(selector)) {
                    return Authorizable.class;
                } else {
                    throw new IOException("Invalid selector '" + selector + '\'');
                }
            }

            private JsonHandler handlerFor(String key) throws IOException {
                if ("scope".equals(key)) {
                    return new ScopeHandler();
                } else if ("condition".equals(key)) {
                    return new ConditionHandler();
                } else if ("order".equals(key) || "sort".equals(key)) {
                    return new OrderHandler();
                } else if ("limit".equals(key)) {
                    return new LimitHandler();
                } else {
                    throw new IOException("Invalid clause '" + key + '\'');
                }
            }
        }

        //---------------------------------------------------< ScopeHandler >---

        private class ScopeHandler extends HandlerBase {
            private String currentKey;
            private String groupName;
            private Boolean declaredOnly;

            @Override
            public void endObject() throws IOException {
                if (groupName == null) {
                    throw new IOException("Missing groupName");
                } else {
                    queryBuilder.setScope(groupName, declaredOnly == null ? true : declaredOnly);
                }
                handlers.pop();
            }

            @Override
            public void key(String s) throws IOException {
                currentKey = s;
            }

            @Override
            public void value(String s) throws IOException {
                if ("groupName".equals(currentKey)) {
                    groupName = s;
                } else {
                    throw new IOException("Unexpected: '" + currentKey + ':' + s + '\'');
                }
            }

            @Override
            public void value(boolean b) throws IOException {
                if ("declaredOnly".equals(currentKey)) {
                    declaredOnly = b;
                } else {
                    throw new IOException("Unexpected: '" + currentKey + ':' + b + '\'');
                }
            }
        }

        //-----------------------------------------------< ConditionHandler >---

        private class ConditionHandler extends HandlerBase {
            private final List<ConditionBase> memberHandlers = new ArrayList<ConditionBase>();

            @Override
            public void object() throws IOException {
                PrimitiveHandler memberHandler = new PrimitiveHandler();
                memberHandlers.add(memberHandler);
                handlers.push(memberHandler);
            }

            @Override
            public void array() throws IOException {
                CompoundHandler memberHandler = new CompoundHandler();
                memberHandlers.add(memberHandler);
                handlers.push(memberHandler);
            }

            @Override
            public void endArray() throws IOException {
                if (memberHandlers.isEmpty()) {
                    throw new IOException("Empty search term");
                }

                Iterator<ConditionBase> memberHandler = memberHandlers.iterator();
                T condition = memberHandler.next().getCondition();
                while (memberHandler.hasNext()) {
                    condition = queryBuilder.and(condition, memberHandler.next().getCondition());
                }

                queryBuilder.setCondition(condition);

                handlers.pop();
            }

        }

        //--------------------------------------------------< ConditionBase >---

        private abstract class ConditionBase extends HandlerBase {
            public abstract T getCondition();
        }

        //------------------------------------------------< CompoundHandler >---

        private class CompoundHandler extends ConditionBase {
            private final List<ConditionBase> memberHandlers = new ArrayList<ConditionBase>();

            @Override
            public void object() throws IOException {
                PrimitiveHandler memberHandler = new PrimitiveHandler();
                memberHandlers.add(memberHandler);
                handlers.push(memberHandler);
            }

            @Override
            public void endArray() throws IOException {
                if (memberHandlers.isEmpty()) {
                    throw new IOException("Empty search term");
                }

                handlers.pop();
            }

            @Override
            public T getCondition() {
                Iterator<ConditionBase> memberHandler = memberHandlers.iterator();
                T condition = memberHandler.next().getCondition();
                while (memberHandler.hasNext()) {
                    condition = queryBuilder.or(condition, memberHandler.next().getCondition());
                }

                return condition;
            }
        }

        //-----------------------------------------------< PrimitiveHandler >---

        private class PrimitiveHandler extends ConditionBase {
            private String currentKey;
            private ConditionBase relOp;
            private ConditionBase not;
            private T condition;

            @Override
            public void object() throws IOException {
                if (hasCondition()) {
                    throw new IOException("Condition on '" + currentKey + "' not allowed since another " +
                            "condition is already set");
                }

                if ("not".equals(currentKey)) {
                    not = new PrimitiveHandler();
                    handlers.push(not);
                } else {
                    relOp = new RelOpHandler(currentKey);
                    handlers.push(relOp);
                }
            }

            @Override
            public void endObject() throws IOException {
                if (!hasCondition()) {
                    throw new IOException("Missing term");
                }

                if (relOp != null) {
                    condition = relOp.getCondition();
                } else if (condition == null) {
                    condition = queryBuilder.not(not.getCondition());
                }

                handlers.pop();
            }

            @Override
            public void key(String s) throws IOException {
                currentKey = s;
            }

            @Override
            public void value(String s) throws IOException {
                if (hasCondition()) {
                    throw new IOException("Condition on '" + currentKey + "' not allowed since another " +
                            "condition is already set");
                }

                if ("named".equals(currentKey)) {
                    condition = queryBuilder.nameMatches(s);
                } else if ("exists".equals(currentKey)) {
                    condition = queryBuilder.exists(s);
                } else if ("impersonates".equals(currentKey)) {
                    condition = queryBuilder.impersonates(s);
                } else {
                    throw new IOException("Invalid condition '" + currentKey + '\'');
                }
            }

            private boolean hasCondition() {
                return condition != null || relOp != null || not != null;
            }

            @Override
            public T getCondition() {
                return condition;
            }
        }

        //---------------------------------------------------< RelOpHandler >---

        private class RelOpHandler extends ConditionBase {
            private final String op;

            private String currentKey;
            private String property;
            private String pattern;
            private String expression;
            private Value value;
            private T condition;

            public RelOpHandler(String op) {
                this.op = op;
            }

            @Override
            public void endObject() throws IOException {
                if (property == null) {
                    throw new IOException("Property not set for condition '" + op + '\'');
                }

                if ("like".equals(op)) {
                    if (pattern == null) {
                        throw new IOException("Pattern not set for 'like' condition");
                    }
                    condition = queryBuilder.like(property, pattern);
                } else if ("contains".equals(op)) {
                    if (expression == null) {
                        throw new IOException("Expression not set for 'contains' condition");
                    }
                    condition = queryBuilder.contains(property, expression);
                } else {
                    if (value == null) {
                        throw new IOException("Value not set for '" + op + "' condition");
                    }

                    if ("eq".equals(op)) {
                        condition = queryBuilder.eq(property, value);
                    } else if ("neq".equals(op)) {
                        condition = queryBuilder.neq(property, value);
                    } else if ("lt".equals(op)) {
                        condition = queryBuilder.lt(property, value);
                    } else if ("le".equals(op)) {
                        condition = queryBuilder.le(property, value);
                    } else if ("ge".equals(op)) {
                        condition = queryBuilder.ge(property, value);
                    } else if ("gt".equals(op)) {
                        condition = queryBuilder.gt(property, value);
                    } else {
                        throw new IOException("Invalid condition: '" + op + '\'');
                    }
                }

                handlers.pop();
            }

            @Override
            public void key(String s) throws IOException {
                currentKey = s;
            }

            @Override
            public void value(String s) throws IOException {
                if ("property".equals(currentKey)) {
                    property = s;
                } else if ("pattern".equals(currentKey)) {
                    pattern = s;
                } else if ("expression".equals(currentKey)) {
                    expression = s;
                } else if ("value".equals(currentKey)) {
                    value = valueFor(s);
                } else {
                    throw new IOException("Expected one of 'property', 'pattern', 'expression', 'value' " +
                            "but found '" + currentKey + '\'');
                }
            }

            @Override
            public void value(boolean b) throws IOException {
                if ("value".equals(currentKey)) {
                    value = valueFor(b);
                } else {
                    throw new IOException("Expected 'value', found '" + currentKey + '\'');
                }
            }

            @Override
            public void value(long l) throws IOException {
                if ("value".equals(currentKey)) {
                    value = valueFor(l);
                } else {
                    throw new IOException("Expected 'value', found '" + currentKey + '\'');
                }
            }

            @Override
            public void value(double v) throws IOException {
                if ("value".equals(currentKey)) {
                    value = valueFor(v);
                } else {
                    throw new IOException("Expected 'value', found '" + currentKey + '\'');
                }
            }

            @Override
            public T getCondition() {
                return condition;
            }
        }

        //---------------------------------------------------< OrderHandler >---

        private class OrderHandler extends HandlerBase {
            private String currentKey;
            private String property;
            private QueryBuilder.Direction direction;
            private boolean ignoreCase = true;

            @Override
            public void endObject() throws IOException {
                if (property == null) {
                    throw new IOException("Missing property");
                } else {
                    queryBuilder.setSortOrder(property,
                            direction == null ? QueryBuilder.Direction.ASCENDING : direction,
                            ignoreCase);
                }
                handlers.pop();
            }

            @Override
            public void key(String s) throws IOException {
                currentKey = s;
            }

            @Override
            public void value(String s) throws IOException {
                if ("property".equals(currentKey)) {
                    property = s;
                } else if ("direction".equals(currentKey)) {
                    direction = directionFor(s);
                } else if ("ignoreCase".equals(currentKey)) {
                    ignoreCase = Boolean.valueOf(s);
                } else {
                    throw new IOException("Unexpected: '" + currentKey + ':' + s + '\'');
                }
            }

            private QueryBuilder.Direction directionFor(String direction) throws IOException {
                if ("asc".equals(direction)) {
                    return QueryBuilder.Direction.ASCENDING;
                } else if ("desc".equals(direction)) {
                    return QueryBuilder.Direction.DESCENDING;
                } else {
                    throw new IOException("Invalid direction '" + direction + '\'');
                }
            }
        }

        //---------------------------------------------------< LimitHandler >---

        private class LimitHandler extends HandlerBase {
            private String currentKey;
            private Long offset;
            private Value bound;
            private Long max;

            @Override
            public void endObject() throws IOException {
                if (offset != null) {
                    queryBuilder.setLimit(offset, max == null ? -1 : max);
                } else if (bound != null) {
                    queryBuilder.setLimit(bound, max == null ? -1 : max);
                } else {
                    throw new IOException("Missing bound or offset");
                }
                handlers.pop();
            }

            @Override
            public void key(String s) throws IOException {
                currentKey = s;
            }

            @Override
            public void value(String s) throws IOException {
                if ("bound".equals(currentKey)) {
                    bound = valueFor(s);
                } else {
                    throw new IOException("Unexpected: '" + currentKey + ':' + s + '\'');
                }
            }

            @Override
            public void value(boolean b) throws IOException {
                if ("bound".equals(currentKey)) {
                    bound = valueFor(b);
                } else {
                    throw new IOException("Unexpected: '" + currentKey + ':' + b + '\'');
                }
            }

            @Override
            public void value(long l) throws IOException {
                if ("bound".equals(currentKey)) {
                    bound = valueFor(l);
                } else if ("offset".equals(currentKey)) {
                    offset = l;
                } else if ("max".equals(currentKey)) {
                    max = l;
                } else {
                    throw new IOException("Unexpected: '" + currentKey + ':' + l + '\'');
                }
            }

            @Override
            public void value(double v) throws IOException {
                if ("bound".equals(currentKey)) {
                    bound = valueFor(v);
                } else {
                    throw new IOException("Unexpected: '" + currentKey + ':' + v + '\'');
                }
            }
        }
    }
}
