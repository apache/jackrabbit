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

package org.apache.jackrabbit.core.security.user;

import java.util.Iterator;
import java.util.function.Predicate;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.QueryBuilder.Direction;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.user.XPathQueryBuilder.Condition;
import org.apache.jackrabbit.core.security.user.XPathQueryBuilder.RelationOp;
import org.apache.jackrabbit.spi.commons.iterator.BoundedIterator;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;
import org.apache.jackrabbit.spi.commons.iterator.Transformer;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This evaluator for {@link org.apache.jackrabbit.api.security.user.Query}s use XPath
 * and some minimal client side filtering.
 */
public class XPathQueryEvaluator implements XPathQueryBuilder.ConditionVisitor {
    static final Logger log = LoggerFactory.getLogger(XPathQueryEvaluator.class);

    private final XPathQueryBuilder builder;
    private final UserManagerImpl userManager;
    private final SessionImpl session;
    private final StringBuilder xPath = new StringBuilder();

    public XPathQueryEvaluator(XPathQueryBuilder builder, UserManagerImpl userManager, SessionImpl session) {
        this.builder = builder;
        this.userManager = userManager;
        this.session = session;
    }

    public Iterator<Authorizable> eval() throws RepositoryException {
        xPath.append("//element(*,")
                .append(getNtName(builder.getSelector()))
                .append(')');

        Value bound = builder.getBound();
        long offset = builder.getOffset();
        if (bound != null && offset > 0) {
            log.warn("Found bound {} and offset {} in limit. Discarding offset.", bound, offset);
            offset = 0;
        }

        Condition condition = builder.getCondition();
        String sortCol = builder.getSortProperty();
        Direction sortDir = builder.getSortDirection();
        if (bound != null) {
            if (sortCol == null) {
                log.warn("Ignoring bound {} since no sort order is specified");
            } else {
                Condition boundCondition = builder.property(sortCol, getCollation(sortDir), bound);
                condition = condition == null
                        ? boundCondition
                        : builder.and(condition, boundCondition);
            }
        }

        if (condition != null) {
            xPath.append('[');
            condition.accept(this);
            xPath.append(']');
        }

        if (sortCol != null) {
            boolean ignoreCase = builder.getSortIgnoreCase();
            xPath.append(" order by ") 
                    .append(ignoreCase ? "" : "fn:lower-case(")
                    .append(sortCol)
                    .append(ignoreCase ? " " : ") ")
                    .append(sortDir.getDirection());
        }

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(xPath.toString(), Query.XPATH);
        long maxCount = builder.getMaxCount();
        if (maxCount == 0) {
            return Iterators.empty();
        }

        // If we are scoped to a group and have a limit, we have to apply the limit
        // here (inefficient!) otherwise we can apply the limit in the query
        if (builder.getGroupName() == null) {
            if (offset > 0) {
                query.setOffset(offset);
            }
            if (maxCount > 0) {
                query.setLimit(maxCount);
            }
            return toAuthorizables(execute(query));
        } else {
            Iterator<Authorizable> result = toAuthorizables(execute(query));
            Iterator<Authorizable> filtered = filter(result, builder.getGroupName(), builder.isDeclaredMembersOnly());
            return BoundedIterator.create(offset, maxCount, filtered);
        }
    }

    //------------------------------------------< ConditionVisitor >---

    public void visit(XPathQueryBuilder.NodeCondition condition) throws RepositoryException {
        String repPrincipal = session.getJCRName(UserConstants.P_PRINCIPAL_NAME);

        xPath.append('(')
                .append("jcr:like(@")
                .append(escapeForQuery(repPrincipal))
                .append(",'")
                .append(escapeForQuery(condition.getPattern()))
                .append("')")
                .append(" or ")
                .append("jcr:like(fn:name(),'")
                .append(escapeForQuery(escape(condition.getPattern())))
                .append("')")
                .append(')');
    }

    public void visit(XPathQueryBuilder.PropertyCondition condition) throws RepositoryException {
        RelationOp relOp = condition.getOp();
        if (relOp == RelationOp.EX) {
            xPath.append(escapeForQuery(condition.getRelPath()));
        } else if (relOp == RelationOp.LIKE) {
            xPath.append("jcr:like(")
                    .append(escapeForQuery(condition.getRelPath()))
                    .append(",'")
                    .append(escapeForQuery(condition.getPattern()))
                    .append("')");
        } else {
            xPath.append(escapeForQuery(condition.getRelPath()))
                    .append(condition.getOp().getOp())
                    .append(format(condition.getValue()));
        }
    }

    public void visit(XPathQueryBuilder.ContainsCondition condition) {
        xPath.append("jcr:contains(")
                .append(escapeForQuery(condition.getRelPath()))
                .append(",'")
                .append(escapeForQuery(condition.getSearchExpr()))
                .append("')");
    }

    public void visit(XPathQueryBuilder.ImpersonationCondition condition) {
        xPath.append("@rep:impersonators='")
                .append(escapeForQuery(condition.getName()))
                .append('\'');
    }

    public void visit(XPathQueryBuilder.NotCondition condition) throws RepositoryException {
        xPath.append("not(");
        condition.getCondition().accept(this);
        xPath.append(')');
    }

    public void visit(XPathQueryBuilder.AndCondition condition) throws RepositoryException {
        int count = 0;
        for (Condition c : condition) {
            xPath.append(count++ > 0 ? " and " : "");
            c.accept(this);
        }
    }

    public void visit(XPathQueryBuilder.OrCondition condition) throws RepositoryException {
        int pos = xPath.length();

        int count = 0;
        for (Condition c : condition) {
            xPath.append(count++ > 0 ? " or " : "");
            c.accept(this);
        }

        // Surround or clause with parentheses if it contains more than one term
        if (count > 1) {
            xPath.insert(pos, '(');
            xPath.append(')');
        }
    }

    //------------------------------------------< private >---

    /**
     * Escape <code>string</code> for matching in jcr escaped node names
     *
     * @param string string to escape
     * @return escaped string
     */
    public static String escape(String string) {
        StringBuilder result = new StringBuilder();

        int k = 0;
        int j;
        do {
            j = string.indexOf('%', k); // split on %
            if (j < 0) {
                // jcr escape trail
                result.append(Text.escapeIllegalJcrChars(string.substring(k)));
            } else if (j > 0 && string.charAt(j - 1) == '\\') {
                // literal occurrence of % -> jcr escape
                result.append(Text.escapeIllegalJcrChars(string.substring(k, j) + '%'));
            } else {
                // wildcard occurrence of % -> jcr escape all but %
                result.append(Text.escapeIllegalJcrChars(string.substring(k, j))).append('%');
            }

            k = j + 1;
        } while (j >= 0);

        return result.toString();
    }

    public static String escapeForQuery(String value) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                ret.append("\\\\");
            } else if (c == '\'') {
                ret.append("''");
            } else {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    private String getNtName(Class<? extends Authorizable> selector) throws RepositoryException {
        if (User.class.isAssignableFrom(selector)) {
            return session.getJCRName(UserConstants.NT_REP_USER);
        } else if (Group.class.isAssignableFrom(selector)) {
            return session.getJCRName(UserConstants.NT_REP_GROUP);
        } else {
            return session.getJCRName(UserConstants.NT_REP_AUTHORIZABLE);
        }
    }

    private static String format(Value value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.STRING:
            case PropertyType.BOOLEAN:
                return '\'' + value.getString() + '\'';

            case PropertyType.LONG:
            case PropertyType.DOUBLE:
                return value.getString();

            case PropertyType.DATE:
                return "xs:dateTime('" + value.getString() + "')";

            default:
                throw new RepositoryException("Property of type " + PropertyType.nameFromValue(value.getType()) +
                        " not supported");
        }
    }

    private static RelationOp getCollation(Direction direction) throws RepositoryException {
        switch (direction) {
            case ASCENDING:
                return RelationOp.GT;

            case DESCENDING:
                return RelationOp.LT;

            default:
                throw new RepositoryException("Unknown sort order " + direction);
        }
    }

    @SuppressWarnings("unchecked")
    private static Iterator<Node> execute(Query query) throws RepositoryException {
        return query.execute().getNodes();
    }

    private Iterator<Authorizable> toAuthorizables(Iterator<Node> nodes) {
        Transformer<Node, Authorizable> transformer = new Transformer<Node, Authorizable>() {
            public Authorizable transform(Node node) {
                try {
                    return userManager.getAuthorizable((NodeImpl) node);
                } catch (RepositoryException e) {
                    log.warn("Cannot create authorizable from node {}", node);
                    log.debug(e.getMessage(), e);
                    return null;
                }
            }
        };

        return Iterators.transformIterator(nodes, transformer);
    }

    private Iterator<Authorizable> filter(Iterator<Authorizable> authorizables, String groupName,
                                          boolean declaredMembersOnly) throws RepositoryException {

        Predicate<Authorizable> predicate;
        Authorizable groupAuth = userManager.getAuthorizable(groupName);
        if (groupAuth == null || !groupAuth.isGroup()) {
            predicate = x -> false;
        } else {
            final Group group = (Group) groupAuth;
            if (declaredMembersOnly) {
                predicate = new Predicate<Authorizable>() {
                    public boolean test(Authorizable authorizable) {
                        try {
                            return authorizable != null && group.isDeclaredMember(authorizable);
                        } catch (RepositoryException e) {
                            log.warn("Cannot determine whether {} is member of group {}", authorizable, group);
                            log.debug(e.getMessage(), e);
                            return false;
                        }
                    }
                };

            } else {
                predicate = new Predicate<Authorizable>() {
                    public boolean test(Authorizable authorizable) {
                        try {
                            return authorizable != null && group.isMember(authorizable);
                        } catch (RepositoryException e) {
                            log.warn("Cannot determine whether {} is member of group {}", authorizable, group);
                            log.debug(e.getMessage(), e);
                            return false;
                        }
                    }
                };
            }
        }

        return Iterators.filterIterator(authorizables, predicate);
    }

}
