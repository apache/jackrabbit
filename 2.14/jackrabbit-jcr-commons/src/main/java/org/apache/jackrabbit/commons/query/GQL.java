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
package org.apache.jackrabbit.commons.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;

/**
 * <code>GQL</code> is a simple fulltext query language, which supports field
 * prefixes similar to Lucene or Google queries.
 * <p>
 * GQL basically consists of a list of query terms that are optionally prefixed
 * with a property name. E.g.: <code>title:jackrabbit</code>. When a property
 * prefix is omitted, GQL will perform a fulltext search on all indexed
 * properties of a node. There are a number of pseudo properties that have
 * special meaning:
 * <ul>
 * <li><code><b>path:</b></code> only search nodes below this path. If you
 * specify more than one term with a path prefix, only the last one will be
 * considered.</li>
 * <li><code><b>type:</b></code> only return nodes of the given node types. This
 * includes primary as well as mixin types. You may specify multiple comma
 * separated node types. GQL will return nodes that are of any of the specified
 * types.</li>
 * <li><code><b>order:</b></code> order the result by the given properties. You
 * may specify multiple comma separated property names. To order the result in
 * descending order simply prefix the property name with a minus. E.g.:
 * <code>order:-name</code>. Using a plus sign will return the result in
 * ascending order, which is also the default.</li>
 * <li><code><b>limit:</b></code> limits the number of results using an
 * interval. E.g.: <code>limit:10..20</code> Please note that the interval is
 * zero based, start is inclusive and end is exclusive. You may also specify an
 * open interval: <code>limit:10..</code> or <code>limit:..20</code> If the dots
 * are omitted and only one value is specified GQL will return at most this
 * number of results. E.g. <code>limit:10</code> (will return the first 10
 * results)</li>
 * <li><code><b>name:</b></code> a constraint on the name of the returned nodes.
 * The following wild cards are allowed: '*', matching any character sequence of
 * length 0..n; '?', matching any single character.</li>
 * </ul>
 * <p>
 * <b>Property name</b>
 * <p>
 * Instead of a property name you may also specify a relative path to a
 * property. E.g.: <code>"jcr:content/jcr:mimeType":text/plain</code>
 * <p>
 * <b>Double quotes</b>
 * <p>
 * The property name as well as the value may enclosed in double quotes. For
 * certain use cases this is required. E.g. if you want to search for a phrase:
 * <code>title:"apache jackrabbit"</code>. Similarly you need to enclose the
 * property name if it contains a colon: <code>"jcr:title":apache</code>,
 * otherwise the first colon is interpreted as the separator between the
 * property name and the value. This also means that a value that contains
 * a colon does not need to be enclosed in double quotes.
 * <p>
 * <b>Escaping</b>
 * <p>
 * To imply double-quotes(&quot;), backslash(\), and colon(:) literally you can
 * escape them with a backslash. E.g. similar to example above in quoting for colon,
 * <code>"jcr:title":apache</code> is equiavalent to jcr\:title:apache.
 * <p>
 * <b>Auto prefixes</b>
 * <p>
 * When a property, node or node type name does not have a namespace prefix GQL
 * will guess the prefix by looking up item and node type definitions in the
 * node type manager. If it finds a definition with a local name that matches
 * it will automatically assign the prefix that is in the definition. This means
 * that you can write: '<code>type:file</code>' and GQL will return nodes that are
 * of node type <code>nt:file</code>. Similarly you can write:
 * <code>order:lastModified</code> and your result nodes will be sorted by their
 * <code>jcr:lastModified</code> property value.
 * <p>
 * <b>Common path prefix</b>
 * <p>
 * For certain queries it is useful to specify a common path prefix for the
 * GQL query statement. See {@link #execute(String, Session, String)}. E.g. if
 * you are searching for file nodes with matches in their resource node. The
 * common path prefix is prepended to every term (except to pseudo properties)
 * before the query is executed. This means you can write:
 * '<code>type:file jackrabbit</code>' and call execute with three parameters,
 * where the third parameter is <code>jcr:content</code>. GQL will return
 * <code>nt:file</code> nodes with <code>jcr:content</code> nodes that contain
 * matches for <code>jackrabbit</code>.
 * <p>
 * <b>Excerpts</b>
 * <p>
 * To get an excerpt for the current row in the result set simply call
 * {@link Row#getValue(String) Row.getValue("rep:excerpt()");}. Please note
 * that this is feature is Jackrabbit specific and will not work with other
 * implementations!
 * <p>
 * <b>Parser callbacks</b>
 * <p>
 * You can get callbacks for each field and query term pair using the method
 * {@link #parse(String, Session, ParserCallback)}. This may be useful when you
 * want to do some transformation on the GQL before it is actually executed.
 */
public final class GQL {

    /**
     * Constant for <code>path</code> keyword.
     */
    private static final String PATH = "path";

    /**
     * Constant for <code>type</code> keyword.
     */
    private static final String TYPE = "type";

    /**
     * Constant for <code>order</code> keyword.
     */
    private static final String ORDER = "order";

    /**
     * Constant for <code>limit</code> keyword.
     */
    private static final String LIMIT = "limit";

    /**
     * Constant for <code>name</code> keyword.
     */
    private static final String NAME = "name";

    /**
     * Constant for <code>OR</code> operator.
     */
    private static final String OR = "OR";

    /**
     * Constant for <code>jcr:mixinTypes</code> property name.
     */
    private static final String JCR_MIXIN_TYPES = "jcr:mixinTypes";

    /**
     * Constant for <code>jcr:primaryType</code> property name.
     */
    private static final String JCR_PRIMARY_TYPE = "jcr:primaryType";

    /**
     * Constant for <code>jcr:root</code>.
     */
    private static final String JCR_ROOT = "jcr:root";

    /**
     * Constant for <code>jcr:contains</code> function name.
     */
    private static final String JCR_CONTAINS = "jcr:contains";

    /**
     * Constant for <code>jcr:score</code> pseudo property name.
     */
    private static final String JCR_SCORE = "jcr:score";

    /**
     * Constant for <code>descending</code> order modifier.
     */
    private static final String DESCENDING = "descending";

    /**
     * Constant for <code>rep:excerpt</code> function name. (Jackrabbit
     * specific).
     */
    private static final String REP_EXCERPT = "rep:excerpt";
    
    /**
     * A pseudo-property for native xpath conditions.
     */
    private static final String NATIVE_XPATH = "jcr:nativeXPath";

    /**
     * The GQL query statement.
     */
    private final String statement;

    /**
     * The session that will exeucte the query.
     */
    private final Session session;

    /**
     * List that contains all {@link PropertyExpression}s.
     */
    private final List<Expression> conditions = new ArrayList<Expression>();

    /**
     * An optional common path prefix for the GQL query.
     */
    private final String commonPathPrefix;

    /**
     * An optional filter that may include/exclude result rows.
     */
    private final Filter filter;

    /**
     * Maps local names of node types to prefixed names.
     */
    private Map<String, String[]> ntNames;

    /**
     * Maps local names of child node definitions to prefixed child node names.
     */
    private Map<String, String> childNodeNames;

    /**
     * Maps local names of property definitions to prefixed property names.
     */
    private Map<String, String> propertyNames;

    /**
     * The path constraint. Defaults to: <code>//*</code>
     */
    private String pathConstraint = "//*";

    /**
     * The optional type constraints.
     */
    private OptionalExpression typeConstraints = null;

    /**
     * The order by expression. Defaults to: @jcr:score descending
     */
    private Expression orderBy = new OrderByExpression();

    /**
     * A result offset.
     */
    private int offset = 0;

    /**
     * The number of results to return at most.
     */
    private int numResults = Integer.MAX_VALUE;

    /**
     * Constructs a GQL.
     *
     * @param statement the GQL query.
     * @param session the session that will execute the query.
     * @param commonPathPrefix a common path prefix for the GQL query.
     * @param filter an optional filter that may include/exclude result rows.
     */
    private GQL(String statement, Session session,
                String commonPathPrefix, Filter filter) {
        this.statement = statement;
        this.session = session;
        this.commonPathPrefix = commonPathPrefix;
        this.filter = filter;
    }

    /**
     * Executes the GQL query and returns the result as a row iterator.
     *
     * @param statement the GQL query.
     * @param session the session that will execute the query.
     * @return the result.
     */
    public static RowIterator execute(String statement,
                                      Session session) {
        return execute(statement, session, null);
    }

    /**
     * Executes the GQL query and returns the result as a row iterator.
     *
     * @param statement the GQL query.
     * @param session the session that will execute the query.
     * @param commonPathPrefix a common path prefix for the GQL query.
     * @return the result.
     */
    public static RowIterator execute(String statement,
                                      Session session,
                                      String commonPathPrefix) {
        return execute(statement, session, commonPathPrefix, null);
    }

    /**
     * Executes the GQL query and returns the result as a row iterator.
     *
     * @param statement the GQL query.
     * @param session the session that will execute the query.
     * @param commonPathPrefix a common path prefix for the GQL query.
     * @param filter an optional filter that may include/exclude result rows.
     * @return the result.
     */
    public static RowIterator execute(String statement,
                                      Session session,
                                      String commonPathPrefix,
                                      Filter filter) {
        GQL query = new GQL(statement, session, commonPathPrefix, filter);
        return query.execute();
    }
    
    /**
     * Executes the GQL query and returns the result as a row iterator.
     *
     * @param jcrQuery the native JCR query.
     * @param jcrQueryLanguage the JCR query language
     * @param session the session that will execute the query.
     * @param commonPathPrefix a common path prefix for the GQL query.
     * @param filter an optional filter that may include/exclude result rows.
     * @return the result.
     */
    public static RowIterator executeXPath(String jcrQuery,
                                      String jcrQueryLanguage,
                                      Session session,
                                      String commonPathPrefix,
                                      Filter filter) {
        GQL query = new GQL("", session, commonPathPrefix, filter);
        return query.executeJcrQuery(jcrQuery, jcrQueryLanguage);
    }
    
    /**
     * Translate the GQL query to XPath.
     *
     * @param statement the GQL query.
     * @param session the session that will execute the query.
     * @param commonPathPrefix a common path prefix for the GQL query.
     * @return the xpath statement.
     */
    public static String translateToXPath(String statement,
            Session session,
            String commonPathPrefix) throws RepositoryException {
        GQL query = new GQL(statement, session, commonPathPrefix, null);
        return query.translateStatement();
    }

    /**
     * Parses the given <code>statement</code> and generates callbacks for each
     * GQL term parsed.
     *
     * @param statement the GQL statement.
     * @param session   the current session to resolve namespace prefixes.
     * @param callback  the callback handler.
     * @throws RepositoryException if an error occurs while parsing.
     */
    public static void parse(String statement,
                             Session session,
                             ParserCallback callback)
            throws RepositoryException {
        GQL query = new GQL(statement, session, null, null);
        query.parse(callback);
    }

    /**
     * Defines a filter for query result rows.
     */
    public interface Filter {

        /**
         * Returns <code>true</code> if the given <code>row</code> should be
         * included in the result.
         *
         * @param row the row to check.
         * @return <code>true</code> if the row should be included,
         *         <code>false</code> otherwise.
         * @throws RepositoryException if an error occurs while reading from the
         *                             repository.
         */
        public boolean include(Row row) throws RepositoryException;
    }

    /**
     * Defines a callback interface that may be implemented by client code to
     * get a callback for each GQL term that is parsed.
     */
    public interface ParserCallback {

        /**
         * A GQL term was parsed.
         *
         * @param property the name of the property or an empty string if the
         *                 term is not prefixed.
         * @param value    the value of the term.
         * @param optional whether this term is prefixed with an OR operator.
         * @throws RepositoryException if an error occurs while processing the
         *                             term.
         */
        public void term(String property, String value, boolean optional)
                throws RepositoryException;
    }

    //-----------------------------< internal >---------------------------------

    /**
     * Executes the GQL query and returns the result as a row iterator.
     *
     * @return the result.
     */
    private RowIterator execute() {
        String xpath;
        try {
            xpath = translateStatement();
        } catch (RepositoryException e) {
            // in case of error return empty result
            return RowIteratorAdapter.EMPTY;
        }
        return executeJcrQuery(xpath, Query.XPATH);
    }
    
    private RowIterator executeJcrQuery(String jcrQuery, String jcrQueryLanguage) {
        try {
            QueryManager qm = session.getWorkspace().getQueryManager();
            RowIterator nodes = qm.createQuery(jcrQuery, jcrQueryLanguage).execute().getRows();
            if (filter != null) {
                nodes = new FilteredRowIterator(nodes);
            }
            if (offset > 0) {
                try {
                    nodes.skip(offset);
                } catch (NoSuchElementException e) {
                    return RowIteratorAdapter.EMPTY;
                }
            }
            if (numResults == Integer.MAX_VALUE) {
                return new RowIterAdapter(nodes, nodes.getSize());
            }
            List<Row> resultRows = new ArrayList<Row>();
            while (numResults-- > 0 && nodes.hasNext()) {
                resultRows.add(nodes.nextRow());
            }
            return new RowIterAdapter(resultRows, resultRows.size());
        } catch (RepositoryException e) {
            // in case of error return empty result
            return RowIteratorAdapter.EMPTY;
        }        
    }

    /**
     * Translates the GQL query into a XPath statement.
     *
     * @return the XPath equivalent.
     * @throws RepositoryException if an error occurs while translating the query.
     */
    private String translateStatement() throws RepositoryException {
        parse(new ParserCallback() {
            public void term(String property, String value, boolean optional)
                    throws RepositoryException {
                pushExpression(property, value, optional);
            }
        });
        StringBuffer stmt = new StringBuffer();
        // path constraint
        stmt.append(pathConstraint);
        // predicate
        RequiredExpression predicate = new RequiredExpression();
        if (typeConstraints != null) {
            predicate.addOperand(typeConstraints);
        }
        for (Expression condition : conditions) {
            predicate.addOperand(condition);
        }
        if (predicate.getSize() > 0) {
            stmt.append("[");
        }
        predicate.toString(stmt);
        if (predicate.getSize() > 0) {
            stmt.append("]");
        }
        stmt.append(" ");
        // order by
        orderBy.toString(stmt);
        return stmt.toString();
    }

    /**
     * Resolves and collects all node types that match <code>ntName</code>.
     *
     * @param ntName the name of a node type (optionally without prefix).
     * @throws RepositoryException if an error occurs while reading from the
     *                             node type manager.
     */
    private void collectNodeTypes(String ntName)
            throws RepositoryException {
        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();

        String[] resolvedNames = resolveNodeTypeName(ntName);

        // now resolve node type hierarchy
        for (String resolvedName : resolvedNames) {
            try {
                NodeType base = ntMgr.getNodeType(resolvedName);
                if (base.isMixin()) {
                    // search for nodes where jcr:mixinTypes is set to this mixin
                    addTypeConstraint(new MixinComparision(resolvedName));
                } else {
                    // search for nodes where jcr:primaryType is set to this type
                    addTypeConstraint(new PrimaryTypeComparision(resolvedName));
                }

                // now search for all node types that are derived from base
                NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
                while (allTypes.hasNext()) {
                    NodeType nt = allTypes.nextNodeType();
                    NodeType[] superTypes = nt.getSupertypes();
                    if (Arrays.asList(superTypes).contains(base)) {
                        if (nt.isMixin()) {
                            addTypeConstraint(new MixinComparision(nt.getName()));
                        } else {
                            addTypeConstraint(new PrimaryTypeComparision(nt.getName()));
                        }
                    }
                }
            } catch (NoSuchNodeTypeException e) {
                // add anyway -> will not match anything
                addTypeConstraint(new PrimaryTypeComparision(resolvedName));
            }
        }
    }

    /**
     * Adds an expression to the {@link #typeConstraints}.
     *
     * @param expr the expression to add.
     */
    private void addTypeConstraint(Expression expr) {
        if (typeConstraints == null) {
            typeConstraints = new OptionalExpression();
        }
        typeConstraints.addOperand(expr);
    }

    /**
     * Resolves the given <code>ntName</code> and returns all node type names
     * where the local name matches <code>ntName</code>.
     *
     * @param ntName the name of a node type (optionally without prefix).
     * @return the matching node type names.
     * @throws RepositoryException if an error occurs while reading from the
     *                             node type manager.
     */
    private String[] resolveNodeTypeName(String ntName)
            throws RepositoryException {
        String[] names;
        if (isPrefixed(ntName)) {
            names = new String[]{ntName};
        } else {
            if (ntNames == null) {
                NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                ntNames = new HashMap<String, String[]>();
                NodeTypeIterator it = ntMgr.getAllNodeTypes();
                while (it.hasNext()) {
                    String name = it.nextNodeType().getName();
                    String localName = name;
                    int idx = name.indexOf(':');
                    if (idx != -1) {
                        localName = name.substring(idx + 1);
                    }
                    String[] nts = ntNames.get(localName);
                    if (nts == null) {
                        nts = new String[]{name};
                    } else {
                        String[] tmp = new String[nts.length + 1];
                        System.arraycopy(nts, 0, tmp, 0, nts.length);
                        tmp[nts.length] = name;
                        nts = tmp;
                    }
                    ntNames.put(localName, nts);
                }
            }
            names = ntNames.get(ntName);
            if (names == null) {
                names = new String[]{ntName};
            }
        }
        return names;
    }

    /**
     * Resolves the given property name. If the name has a prefix then the name
     * is returned immediately as is. Otherwise the node type manager is
     * searched for a property definition that defines a named property with
     * a local name that matches the provided <code>name</code>. If such a match
     * is found the name of the property definition is returned.
     *
     * @param name the name of a property (optionally without a prefix).
     * @return the resolved property name.
     * @throws RepositoryException if an error occurs while reading from the
     *                             node type manager.
     */
    private String resolvePropertyName(String name)
            throws RepositoryException {
        if (isPrefixed(name)) {
            return name;
        }
        if (propertyNames == null) {
            propertyNames = new HashMap<String, String>();
            if (session != null) {
                NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
                NodeTypeIterator it = ntMgr.getAllNodeTypes();
                while (it.hasNext()) {
                    NodeType nt = it.nextNodeType();
                    PropertyDefinition[] defs = nt.getDeclaredPropertyDefinitions();
                    for (PropertyDefinition def : defs) {
                        String pn = def.getName();
                        if (!pn.equals("*")) {
                            String localName = pn;
                            int idx = pn.indexOf(':');
                            if (idx != -1) {
                                localName = pn.substring(idx + 1);
                            }
                            propertyNames.put(localName, pn);
                        }
                    }
                }
            }
        }
        String pn = propertyNames.get(name);
        if (pn != null) {
            return pn;
        } else {
            return name;
        }
    }

    /**
     * Resolves the given node name. If the name has a prefix then the name
     * is returned immediately as is. Otherwise the node type manager is
     * searched for a node definition that defines a named child node with
     * a local name that matches the provided <code>name</code>. If such a match
     * is found the name of the node definition is returned.
     *
     * @param name the name of a node (optionally without a prefix).
     * @return the resolved node name.
     * @throws RepositoryException if an error occurs while reading from the
     *                             node type manager.
     */
    private String resolveChildNodeName(String name)
            throws RepositoryException {
        if (isPrefixed(name)) {
            return name;
        }
        if (childNodeNames == null) {
            childNodeNames = new HashMap<String, String>();
            NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
            NodeTypeIterator it = ntMgr.getAllNodeTypes();
            while (it.hasNext()) {
                NodeType nt = it.nextNodeType();
                NodeDefinition[] defs = nt.getDeclaredChildNodeDefinitions();
                for (NodeDefinition def : defs) {
                    String cnn = def.getName();
                    if (!cnn.equals("*")) {
                        String localName = cnn;
                        int idx = cnn.indexOf(':');
                        if (idx != -1) {
                            localName = cnn.substring(idx + 1);
                        }
                        childNodeNames.put(localName, cnn);
                    }
                }
            }
        }
        String cnn = childNodeNames.get(name);
        if (cnn != null) {
            return cnn;
        } else {
            return name;
        }
    }

    /**
     * @param name a JCR name.
     * @return <code>true</code> if <code>name</code> contains a namespace
     *         prefix; <code>false</code> otherwise.
     */
    private static boolean isPrefixed(String name) {
        return name.indexOf(':') != -1;
    }

    /**
     * Parses the GQL query statement.
     *
     * @param callback the parser callback.
     * @throws RepositoryException if an error occurs while reading from the
     *                             repository.
     */
    private void parse(ParserCallback callback) throws RepositoryException {
        char[] stmt = new char[statement.length() + 1];
        statement.getChars(0, statement.length(), stmt, 0);
        stmt[statement.length()] = ' ';
        StringBuffer property = new StringBuffer();
        StringBuffer value = new StringBuffer();
        boolean quoted = false;
        boolean escaped = false;
        boolean optional = false;
        for (char c : stmt) {
            switch (c) {
                case ' ':
                    if (quoted) {
                        value.append(c);
                    } else {
                        if (value.length() > 0) {
                            String p = property.toString();
                            String v = value.toString();
                            if (v.equals(OR) && p.length() == 0) {
                                optional = true;
                            } else {
                                callback.term(p, v, optional);
                                optional = false;
                            }
                            property.setLength(0);
                            value.setLength(0);
                        }
                    }
                    break;
                case ':':
                    if (quoted || escaped) {
                        value.append(c);
                    } else {
                        if (property.length() == 0) {
                            // turn value into property name
                            property.append(value);
                            value.setLength(0);
                        } else {
                            // colon in value
                            value.append(c);
                        }
                    }
                    break;
                case '"':
                    if (escaped) {
                        value.append(c);
                    } else {
                        quoted = !quoted;
                    }
                    break;
                case '\\':
                    if (escaped) {
                        value.append(c);
                    }
                    escaped = !escaped;
                    break;
                    // noise
                case '*':
                case '?':
                    if (property.toString().equals(NAME)) {
                        // allow wild cards in name
                        value.append(c);
                        break;
                    }
                case '~':
                case '^':
                case '[':
                case ']':
                case '{':
                case '}':
                case '!':
                    break;
                default:
                    value.append(c);
            }
            if (c != '\\') {
                escaped = false;
            }
        }
    }

    /**
     * Pushes an expression.
     *
     * @param property the property name of the currently parsed expression.
     * @param value the value of the currently parsed expression.
     * @param optional whether the previous token was the <code>OR</code> operator.
     * @throws RepositoryException if an error occurs while reading from the
     *                             node type manager.
     */
    private void pushExpression(String property,
                                String value,
                                boolean optional) throws RepositoryException {
        if (property.equals(PATH)) {
            String path;
            if (value.startsWith("/")) {
                path = "/" + JCR_ROOT + value;
            } else {
                path = value;
            }
            pathConstraint = ISO9075.encodePath(path) + "//*";
        } else if (property.equals(TYPE)) {
            String[] nts = Text.explode(value, ',');
            if (nts.length > 0) {
                for (String nt : nts) {
                    collectNodeTypes(nt);
                }
            }
        } else if (property.equals(ORDER)) {
            orderBy = new OrderByExpression(value);
        } else if (property.equals(LIMIT)) {
            int idx = value.indexOf("..");
            if (idx != -1) {
                String lower = value.substring(0, idx);
                String uppper = value.substring(idx + "..".length());
                if (lower.length() > 0) {
                    try {
                        offset = Integer.parseInt(lower);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                if (uppper.length() > 0) {
                    try {
                        numResults = Integer.parseInt(uppper) - offset;
                        if (numResults < 0) {
                            numResults = Integer.MAX_VALUE;
                        }
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            } else {
                // numResults only?
                try {
                    numResults = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        } else {
            Expression expr;
            if (property.equals(NAME)) {
                expr = new NameExpression(value);
            } else {
                expr = new ContainsExpression(property, value);
            }

            if (optional) {
                Expression last = conditions.get(conditions.size() - 1);
                if (last instanceof OptionalExpression) {
                    ((OptionalExpression) last).addOperand(expr);
                } else {
                    OptionalExpression op = new OptionalExpression();
                    op.addOperand(last);
                    op.addOperand(expr);
                    conditions.set(conditions.size() - 1, op);
                }
            } else {
                conditions.add(expr);
            }
        }
    }

    /**
     * Checks if the <code>value</code> is prohibited (prefixed with a dash)
     * and returns the value without the prefix.
     *
     * @param value the value to check.
     * @return the un-prefixed value.
     */
    private static String checkProhibited(String value) {
        if (value.startsWith("-")) {
            return value.substring(1);
        } else {
            return value;
        }
    }

    /**
     * An expression in GQL.
     */
    private interface Expression {

        void toString(StringBuffer buffer) throws RepositoryException;
    }

    /**
     * Base class for all property expressions.
     */
    private abstract class PropertyExpression implements Expression {

        protected final String property;

        protected final String value;

        PropertyExpression(String property, String value) {
            this.property = property;
            this.value = value;
        }
    }

    /**
     * Base class for mixin and primary type expressions.
     */
    private abstract class ValueComparison extends PropertyExpression {

        ValueComparison(String property, String value) {
            super(property, value);
        }

        public void toString(StringBuffer buffer)
                throws RepositoryException {
            buffer.append("@");
            buffer.append(ISO9075.encode(resolvePropertyName(property)));
            buffer.append("='").append(value).append("'");
        }
    }

    /**
     * A mixin type comparison.
     */
    private class MixinComparision extends ValueComparison {

        MixinComparision(String value) {
            super(JCR_MIXIN_TYPES, value);
        }
    }

    /**
     * A primary type comparision.
     */
    private class PrimaryTypeComparision extends ValueComparison {

        PrimaryTypeComparision(String value) {
            super(JCR_PRIMARY_TYPE, value);
        }
    }

    /**
     * A name expression.
     */
    private static class NameExpression implements Expression {

        private final String value;

        NameExpression(String value) {
            String tmp = value;
            tmp = tmp.replaceAll("'", "''");
            tmp = tmp.replaceAll("\\*", "\\%");
            tmp = tmp.replaceAll("\\?", "\\_");
            tmp = tmp.toLowerCase();
            this.value = tmp;
        }

        public void toString(StringBuffer buffer)
                throws RepositoryException {
            buffer.append("jcr:like(fn:lower-case(fn:name()), '");
            buffer.append(value);
            buffer.append("')");
        }
    }

    /**
     * A single contains expression.
     */
    private final class ContainsExpression extends PropertyExpression {

        private boolean prohibited = false;

        ContainsExpression(String property, String value) {
            super(property, checkProhibited(value.toLowerCase()));
            this.prohibited = value.startsWith("-");
        }

        public void toString(StringBuffer buffer)
                throws RepositoryException {
            if (property.equals(NATIVE_XPATH)) {
                buffer.append(value);
                return;
            }        
            if (prohibited) {
                buffer.append("not(");
            }
            buffer.append(JCR_CONTAINS).append("(");
            if (property.length() == 0) {
                // node scope
                if (commonPathPrefix == null) {
                    buffer.append(".");
                } else {
                    buffer.append(ISO9075.encodePath(commonPathPrefix));
                }
            } else {
                // property scope
                String[] parts = Text.explode(property, '/');
                if (commonPathPrefix != null) {
                    buffer.append(ISO9075.encodePath(commonPathPrefix));
                    buffer.append("/");
                }
                String slash = "";
                for (int i = 0; i < parts.length; i++) {
                    if (i == parts.length - 1) {
                        if (!parts[i].equals(".")) {
                            // last part
                            buffer.append(slash);
                            buffer.append("@");
                            buffer.append(ISO9075.encode(
                                    resolvePropertyName(parts[i])));
                        }
                    } else {
                        buffer.append(slash);
                        buffer.append(ISO9075.encode(
                                resolveChildNodeName(parts[i])));
                    }
                    slash = "/";
                }
            }
            buffer.append(", '");
            // properly escape apostrophe. See JCR-3157
            String escapedValue = value.replaceAll("'", "\\\\''");
            if (value.indexOf(' ') != -1) {
                // phrase
                buffer.append('"').append(escapedValue).append('"');
            } else {
                buffer.append(escapedValue);
            }
            buffer.append("')");
            if (prohibited) {
                buffer.append(")");
            }
        }
    }

    /**
     * Base class for n-ary expression.
     */
    private abstract class NAryExpression implements Expression {

        private final List<Expression> operands = new ArrayList<Expression>();

        public void toString(StringBuffer buffer)
                throws RepositoryException {
            if (operands.size() > 1) {
                buffer.append("(");
            }
            String op = "";
            for (Expression expr : operands) {
                buffer.append(op);
                expr.toString(buffer);
                op = getOperation();
            }
            if (operands.size() > 1) {
                buffer.append(")");
            }
        }

        void addOperand(Expression expr) {
            operands.add(expr);
        }

        int getSize() {
            return operands.size();
        }

        protected abstract String getOperation();
    }

    /**
     * An expression that requires all its operands to match.
     */
    private class RequiredExpression extends NAryExpression {

        protected String getOperation() {
            return " and ";
        }
    }

    /**
     * An expression where at least one operand must match.
     */
    private class OptionalExpression extends NAryExpression {

        protected String getOperation() {
            return " or ";
        }
    }

    /**
     * An order by expression.
     */
    private class OrderByExpression implements Expression {

        private final String value;

        OrderByExpression() {
            this.value = "";
        }

        OrderByExpression(String value) {
            this.value = value;
        }

        public void toString(StringBuffer buffer)
                throws RepositoryException {
            int start = buffer.length();
            buffer.append("order by ");
            List<String> names = new ArrayList<String>(Arrays.asList(Text.explode(value, ',')));
            int length = buffer.length();
            String comma = "";
            for (String name : names) {
                boolean asc;
                if (name.equals("-")) {
                    // no order by at all
                    buffer.delete(start, buffer.length());
                    return;
                }
                if (name.startsWith("-")) {
                    name = name.substring(1);
                    asc = false;
                } else if (name.startsWith("+")) {
                    name = name.substring(1);
                    asc = true;
                } else {
                    // default is ascending
                    asc = true;
                }
                if (name.length() > 0) {
                    buffer.append(comma);
                    name = createPropertyName(resolvePropertyName(name));
                    buffer.append(name);
                    if (!asc) {
                        buffer.append(" ").append(DESCENDING);
                    }
                    comma = ", ";
                }
            }
            if (buffer.length() == length) {
                // no order by
                defaultOrderBy(buffer);
            }
        }

        private String createPropertyName(String name) {
            if (name.contains("/")) {
                String[] labels = name.split("/");

                name = "";
                for (int i = 0; i < labels.length; i++) {
                    String label = ISO9075.encode(labels[i]);
                    if (i < (labels.length - 1)) {
                        name += label + "/";
                    } else {
                        name += "@" + label;
                    }
                }
                return name;
            } else {
                return "@" + ISO9075.encode(name);
            }
        }

        private void defaultOrderBy(StringBuffer buffer) {
            buffer.append("@").append(JCR_SCORE).append(" ").append(DESCENDING);
        }
    }

    /**
     * A row adapter for rep:excerpt values in the result.
     */
    private static final class RowAdapter implements Row {

        private final Row row;

        private final String excerptPath;

        private RowAdapter(Row row, String excerptPath) {
            this.row = row;
            this.excerptPath = excerptPath;
        }

        public Value[] getValues() throws RepositoryException {
            return row.getValues();
        }

        public Value getValue(String propertyName) throws ItemNotFoundException, RepositoryException {
            if (propertyName.startsWith(REP_EXCERPT)) {
                propertyName = REP_EXCERPT + "(" + excerptPath + ")";
            }
            return row.getValue(propertyName);
        }

        public Node getNode() throws RepositoryException {
            return row.getNode();
        }

        public Node getNode(String selectorName) throws RepositoryException {
            return row.getNode(selectorName);
        }

        public String getPath() throws RepositoryException {
            return row.getPath();
        }

        public String getPath(String selectorName) throws RepositoryException {
            return row.getPath(selectorName);
        }

        public double getScore() throws RepositoryException {
            return row.getScore();
        }

        public double getScore(String selectorName) throws RepositoryException {
            return row.getScore(selectorName);
        }
    }

    /**
     * Customized row iterator adapter, which returns {@link RowAdapter}.
     */
    private final class RowIterAdapter extends RowIteratorAdapter {

        private final long size;

        public RowIterAdapter(RangeIterator rangeIterator, long size) {
            super(rangeIterator);
            this.size = size;
        }

        public RowIterAdapter(Collection collection, long size) {
            super(collection);
            this.size = size;
        }

        public Row nextRow() {
            Row next = super.nextRow();
            if (commonPathPrefix != null) {
                next = new RowAdapter(next, commonPathPrefix);
            } else {
                next = new RowAdapter(next, ".");
            }
            return next;
        }

        public long getSize() {
            return size;
        }
    }

    /**
     * A row iterator that applies a {@link GQL#filter} on the underlying rows.
     */
    private final class FilteredRowIterator implements RowIterator {

        /**
         * The underlying rows.
         */
        private final RowIterator rows;

        /**
         * The next row to return or <code>null</code> if there is none.
         */
        private Row next;

        /**
         * The current position.
         */
        private long position = 0;

        /**
         * Filters the given <code>rows</code>.
         *
         * @param rows the rows to filter.
         */
        public FilteredRowIterator(RowIterator rows) {
            this.rows = rows;
            fetchNext();
        }

        /**
         * {@inheritDoc}
         */
        public void skip(long skipNum) {
            while (skipNum-- > 0 && hasNext()) {
                nextRow();
            }
        }

        /**
         * {@inheritDoc}
         */
        public long getSize() {
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        public long getPosition() {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        public Object next() {
            return nextRow();
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public Row nextRow() {
            if (next == null) {
                throw new NoSuchElementException();
            } else {
                try {
                    return next;
                } finally {
                    position++;
                    fetchNext();
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return next != null;
        }

        /**
         * Fetches the next {@link Row}.
         */
        private void fetchNext() {
            next = null;
            while (next == null && rows.hasNext()) {
                Row r = rows.nextRow();
                try {
                    if (filter.include(r)) {
                        next = r;
                        return;
                    }
                } catch (RepositoryException e) {
                    // ignore
                }
            }
        }
    }
}

