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

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.QueryBuilder;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XPathQueryBuilder implements QueryBuilder<XPathQueryBuilder.Condition> {

    /**
     * Relational operators for comparing a property to a value. Correspond
     * to the general comparison operators as define in JSR-170.
     * The {@link #EX} tests for existence of a property.
     */
    enum RelationOp {
        NE("!="),
        EQ("="),
        LT("<"),
        LE("<="),
        GT(">"),
        GE("=>"),
        EX(""),
        LIKE("like");

        private final String op;

        RelationOp(String op) {
            this.op = op;
        }

        public String getOp() {
            return op;
        }
    }

    interface Condition {
        void accept(ConditionVisitor visitor) throws RepositoryException;
    }

    interface ConditionVisitor {
        void visit(NodeCondition nodeCondition) throws RepositoryException;

        void visit(PropertyCondition condition) throws RepositoryException;

        void visit(ContainsCondition condition);

        void visit(ImpersonationCondition condition);

        void visit(NotCondition condition) throws RepositoryException;

        void visit(AndCondition condition) throws RepositoryException;

        void visit(OrCondition condition) throws RepositoryException;
    }

    private Class<? extends Authorizable> selector = Authorizable.class;
    private String groupName;
    private boolean declaredMembersOnly;
    private Condition condition;
    private String sortProperty;
    private Direction sortDirection = Direction.ASCENDING;
    private boolean sortIgnoreCase; 
    private Value bound;
    private long offset;
    private long maxCount = -1;

    Class<? extends Authorizable> getSelector() {
        return selector;
    }

    public String getGroupName() {
        return groupName;
    }

    public boolean isDeclaredMembersOnly() {
        return declaredMembersOnly;
    }

    Condition getCondition() {
        return condition;
    }

    String getSortProperty() {
        return sortProperty;
    }

    Direction getSortDirection() {
        return sortDirection;
    }

    boolean getSortIgnoreCase() {
        return sortIgnoreCase;
    }

    Value getBound() {
        return bound;
    }

    long getOffset() {
        return offset;
    }

    long getMaxCount() {
        return maxCount;
    }

    //------------------------------------------< QueryBuilder >---

    public void setSelector(Class<? extends Authorizable> selector) {
        this.selector = selector;
    }

    public void setScope(String groupName, boolean declaredOnly) {
        this.groupName = groupName;
        declaredMembersOnly = declaredOnly;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public void setSortOrder(String propertyName, Direction direction, boolean ignoreCase) {
        sortProperty = propertyName;
        sortDirection = direction;
        sortIgnoreCase = ignoreCase;
    }

    public void setSortOrder(String propertyName, Direction direction) {
        setSortOrder(propertyName, direction, false);
    }

    public void setLimit(Value bound, long maxCount) {
        offset = 0;   // Unset any previously set offset
        this.bound = bound;
        this.maxCount = maxCount;
    }

    public void setLimit(long offset, long maxCount) {
        bound = null; // Unset any previously set bound
        this.offset = offset;
        this.maxCount = maxCount;
    }

    public Condition property(String relPath, RelationOp op, Value value) {
        return new PropertyCondition(relPath, op, value);
    }

    public Condition nameMatches(String pattern) {
        return new NodeCondition(pattern);
    }

    public Condition neq(String relPath, Value value) {
        return new PropertyCondition(relPath, RelationOp.NE, value);
    }

    public Condition eq(String relPath, Value value) {
        return new PropertyCondition(relPath, RelationOp.EQ, value);
    }

    public Condition lt(String relPath, Value value) {
        return new PropertyCondition(relPath, RelationOp.LT, value);
    }

    public Condition le(String relPath, Value value) {
        return new PropertyCondition(relPath, RelationOp.LE, value);
    }

    public Condition gt(String relPath, Value value) {
        return new PropertyCondition(relPath, RelationOp.GT, value);
    }

    public Condition ge(String relPath, Value value) {
        return new PropertyCondition(relPath, RelationOp.GE, value);
    }

    public Condition exists(String relPath) {
        return new PropertyCondition(relPath, RelationOp.EX);
    }

    public Condition like(String relPath, String pattern) {
        return new PropertyCondition(relPath, RelationOp.LIKE, pattern);
    }

    public Condition contains(String relPath, String searchExpr) {
        return new ContainsCondition(relPath, searchExpr);
    }

    public Condition impersonates(String name) {
        return new ImpersonationCondition(name);
    }

    public Condition not(Condition condition) {
        return new NotCondition(condition);
    }

    public Condition and(Condition condition1, Condition condition2) {
        return new AndCondition(condition1, condition2);
    }

    public Condition or(Condition condition1, Condition condition2) {
        return new OrCondition(condition1, condition2);
    }

    //------------------------------------------< private >---

    static class NodeCondition implements Condition {
        private final String pattern;

        public NodeCondition(String pattern) {
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }

        public void accept(ConditionVisitor visitor) throws RepositoryException {
            visitor.visit(this);
        }
    }

    static class PropertyCondition implements Condition {
        private final String relPath;
        private final RelationOp op;
        private final Value value;
        private final String pattern;

        public PropertyCondition(String relPath, RelationOp op, Value value) {
            this.relPath = relPath;
            this.op = op;
            this.value = value;
            pattern = null;
        }

        public PropertyCondition(String relPath, RelationOp op, String pattern) {
            this.relPath = relPath;
            this.op = op;
            value = null;
            this.pattern = pattern;
        }

        public PropertyCondition(String relPath, RelationOp op) {
            this.relPath = relPath;
            this.op = op;
            value = null;
            pattern = null;
        }

        public String getRelPath() {
            return relPath;
        }

        public RelationOp getOp() {
            return op;
        }

        public Value getValue() {
            return value;
        }

        public String getPattern() {
            return pattern;
        }

        public void accept(ConditionVisitor visitor) throws RepositoryException {
            visitor.visit(this);
        }
    }

    static class ContainsCondition implements Condition {
        private final String relPath;
        private final String searchExpr;

        public ContainsCondition(String relPath, String searchExpr) {
            this.relPath = relPath;
            this.searchExpr = searchExpr;
        }

        public String getRelPath() {
            return relPath;
        }

        public String getSearchExpr() {
            return searchExpr;
        }

        public void accept(ConditionVisitor visitor) {
            visitor.visit(this);
        }
    }

    static class ImpersonationCondition implements Condition {
        private final String name;

        public ImpersonationCondition(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void accept(ConditionVisitor visitor) {
            visitor.visit(this);
        }
    }

    static class NotCondition implements Condition {
        private final Condition condition;

        public NotCondition(Condition condition) {
            this.condition = condition;
        }

        public Condition getCondition() {
            return condition;
        }

        public void accept(ConditionVisitor visitor) throws RepositoryException {
            visitor.visit(this);
        }
    }

    abstract static class CompoundCondition implements Condition, Iterable<Condition> {
        private final List<Condition> conditions = new ArrayList<Condition>();

        public CompoundCondition() {
            super();
        }

        public CompoundCondition(Condition condition1, Condition condition2) {
            conditions.add(condition1);
            conditions.add(condition2);
        }

        public void addCondition(Condition condition) {
            conditions.add(condition);
        }

        public Iterator<Condition> iterator() {
            return conditions.iterator();
        }
    }

    static class AndCondition extends CompoundCondition {
        public AndCondition(Condition condition1, Condition condition2) {
            super(condition1, condition2);
        }

        public void accept(ConditionVisitor visitor) throws RepositoryException {
            visitor.visit(this);
        }
    }

    static class OrCondition extends CompoundCondition {
        public OrCondition(Condition condition1, Condition condition2) {
            super(condition1, condition2);
        }

        public void accept(ConditionVisitor visitor) throws RepositoryException {
            visitor.visit(this);
        }
    }
}
