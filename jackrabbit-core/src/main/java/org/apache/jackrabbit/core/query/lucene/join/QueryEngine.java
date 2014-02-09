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
package org.apache.jackrabbit.core.query.lucene.join;

import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.Source;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;
import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryFactory;
import org.apache.jackrabbit.core.query.lucene.sort.DynamicOperandFieldComparatorSource;
import org.apache.jackrabbit.core.query.lucene.sort.RowComparator;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryEngine {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory
            .getLogger(QueryEngine.class);
    
    //TODO remove this when the implementation is stable
    public static final String NATIVE_SORT_SYSTEM_PROPERTY = "useNativeSort";

    private static final boolean NATIVE_SORT = Boolean.valueOf(System
            .getProperty(NATIVE_SORT_SYSTEM_PROPERTY, "false"));

    private static final int printIndentStep = 4;
    
    private final Session session;

    private final LuceneQueryFactory lqf;

    private final NodeTypeManager ntManager;

    private final QueryObjectModelFactory qomFactory;

    private final ValueFactory valueFactory;

    private final OperandEvaluator evaluator;

    public QueryEngine(Session session, LuceneQueryFactory lqf,
            Map<String, Value> variables) throws RepositoryException {
        this.session = session;
        this.lqf = lqf;
        Workspace workspace = session.getWorkspace();
        this.ntManager = workspace.getNodeTypeManager();
        this.qomFactory = workspace.getQueryManager().getQOMFactory();
        this.valueFactory = session.getValueFactory();

        this.evaluator = new OperandEvaluator(valueFactory, variables);
    }

    public QueryResult execute(Column[] columns, Source source,
            Constraint constraint, Ordering[] orderings, long offset, long limit)
            throws RepositoryException {
        long time = System.currentTimeMillis();
        QueryResult qr = execute(columns, source, constraint, orderings,
                offset, limit, 2);
        log.debug("SQL2 QUERY execute took {} ms. native sort is {}.",
                System.currentTimeMillis() - time, NATIVE_SORT);
        return qr;
    }

    protected QueryResult execute(Column[] columns, Source source,
            Constraint constraint, Ordering[] orderings, long offset,
            long limit, int printIndentation) throws RepositoryException {
        if (source instanceof Selector) {
            return execute(columns, (Selector) source, constraint, orderings,
                    offset, limit, printIndentation);
        }
        if (source instanceof Join) {
            return execute(columns, (Join) source, constraint, orderings,
                    offset, limit, printIndentation);
        }
        throw new UnsupportedRepositoryOperationException(
                "Unknown source type: " + source);
    }

    protected QueryResult execute(Column[] columns, Join join,
            Constraint constraint, Ordering[] orderings, long offset,
            long limit, int printIndentation) throws RepositoryException {
        // Swap the join sources to normalize all outer joins to left
        if (JCR_JOIN_TYPE_RIGHT_OUTER.equalsIgnoreCase(join.getJoinType())) {
            log.debug(
                    "{} SQL2 RIGHT OUTER JOIN transformed to LEFT OUTER JOIN.",
                    genString(printIndentation));
            Join betterJoin = qomFactory.join(join.getRight(), join.getLeft(),
                    JCR_JOIN_TYPE_LEFT_OUTER, join.getJoinCondition());
            return execute(columns, betterJoin, constraint, orderings, offset,
                    limit, printIndentation);
        }
        JoinMerger merger = JoinMerger.getJoinMerger(join,
                getColumnMap(columns, getSelectorNames(join)), evaluator,
                qomFactory);
        ConstraintSplitter splitter = new ConstraintSplitter(constraint,
                qomFactory, merger.getLeftSelectors(),
                merger.getRightSelectors(), join);
        ConstraintSplitInfo csInfo = splitter.getConstraintSplitInfo();

        logQueryAnalysis(csInfo, printIndentation);

        boolean isOuterJoin = JCR_JOIN_TYPE_LEFT_OUTER.equalsIgnoreCase(join
                .getJoinType());
        QueryResult result = execute(merger, csInfo, isOuterJoin,
                printIndentation);

        long sort = System.currentTimeMillis();
        QueryResult sortedResult = sort(result, orderings, evaluator, offset,
                limit);
        log.debug(" {} SQL2 SORT took {} ms.", genString(printIndentation),
                System.currentTimeMillis() - sort);
        return sortedResult;
    }

    protected QueryResult execute(JoinMerger merger,
            ConstraintSplitInfo csInfo, boolean isOuterJoin,
            int printIndentation) throws RepositoryException {

        Comparator<Row> leftCo = new RowPathComparator(
                merger.getLeftSelectors());
        long timeJoinLeftSide = System.currentTimeMillis();

        if (csInfo.isMultiple()) {
            log.debug("{} SQL2 JOIN execute: there are multiple inner splits.",
                    genString(printIndentation));

            // first branch
            long bTime = System.currentTimeMillis();
            QueryResult branch1 = execute(merger,
                    csInfo.getLeftInnerConstraints(), isOuterJoin,
                    printIndentation + printIndentStep);
            Set<Row> allRows = new TreeSet<Row>(new RowPathComparator(
                    Arrays.asList(merger.getSelectorNames())));
            RowIterator ri1 = branch1.getRows();
            while (ri1.hasNext()) {
                Row r = ri1.nextRow();
                allRows.add(r);
            }
            log.debug("{} SQL2 JOIN executed first branch, took {} ms.",
                    genString(printIndentation), System.currentTimeMillis()
                            - bTime);

            // second branch
            bTime = System.currentTimeMillis();
            QueryResult branch2 = execute(merger,
                    csInfo.getRightInnerConstraints(), isOuterJoin,
                    printIndentation + printIndentStep);
            RowIterator ri2 = branch2.getRows();
            while (ri2.hasNext()) {
                Row r = ri2.nextRow();
                allRows.add(r);
            }
            log.debug("{} SQL2 JOIN executed second branch, took {} ms.",
                    genString(printIndentation), System.currentTimeMillis()
                            - bTime);
            return new SimpleQueryResult(merger.getColumnNames(),
                    merger.getSelectorNames(), new RowIteratorAdapter(allRows));
        }

        Set<Row> leftRows = buildLeftRowsJoin(csInfo, leftCo, printIndentation
                + printIndentStep);
        if (log.isDebugEnabled()) {
            timeJoinLeftSide = System.currentTimeMillis() - timeJoinLeftSide;
            log.debug(genString(printIndentation) + "SQL2 JOIN LEFT SIDE took "
                    + timeJoinLeftSide + " ms. fetched " + leftRows.size()
                    + " rows.");
        }

        // The join constraint information is split into:
        // - rightConstraints selects just the 'ON' constraints
        // - csInfo has the 'WHERE' constraints
        //
        // So, in the case of an OUTER JOIN we'll run 2 queries, one with
        // 'ON'
        // and one with 'ON' + 'WHERE' conditions
        // this way, at merge time in case of an outer join we can tell if
        // it's a 'null' row, or a bad row -> one that must not be returned.
        // This way at the end we'll have:
        // - rightRowsSet containing the 'ON' dataset
        // - excludingOuterJoinRowsSet: the 'ON' + 'WHERE' condition
        // dataset, or
        // NULL if there is no 'WHERE' condition

        long timeJoinRightSide = System.currentTimeMillis();
        List<Constraint> rightConstraints = merger
                .getRightJoinConstraints(leftRows);
        Comparator<Row> rightCo = new RowPathComparator(
                merger.getRightSelectors());

        if (leftRows == null || leftRows.isEmpty()) {
            return merger.merge(new RowIteratorAdapter((leftRows == null) ? Collections.emptySet() : leftRows),
                    new RowIteratorAdapter(new TreeSet<Row>()), null, rightCo);
        }

        Set<Row> rightRows = buildRightRowsJoin(csInfo, rightConstraints,
                isOuterJoin, rightCo, printIndentation + printIndentStep);

        // this has to be initialized as null
        Set<Row> excludingOuterJoinRowsSet = null;
        if (isOuterJoin && csInfo.getRightConstraint() != null) {
            excludingOuterJoinRowsSet = buildRightRowsJoin(csInfo,
                    rightConstraints, false, rightCo, printIndentation
                            + printIndentStep);
        }

        if (log.isDebugEnabled()) {
            timeJoinRightSide = System.currentTimeMillis() - timeJoinRightSide;
            log.debug(genString(printIndentation)
                    + "SQL2 JOIN RIGHT SIDE took " + timeJoinRightSide
                    + " ms. fetched " + rightRows.size() + " rows.");
        }
        // merge left with right datasets
        return merger.merge(new RowIteratorAdapter(leftRows),
                new RowIteratorAdapter(rightRows), excludingOuterJoinRowsSet,
                rightCo);

    }

    private Set<Row> buildLeftRowsJoin(ConstraintSplitInfo csi,
            Comparator<Row> comparator, int printIndentation)
            throws RepositoryException {

        if (csi.isMultiple()) {
            if (log.isDebugEnabled()) {
                log.debug(genString(printIndentation)
                        + "SQL2 JOIN LEFT SIDE there are multiple inner splits.");
            }
            Set<Row> leftRows = new TreeSet<Row>(comparator);
            leftRows.addAll(buildLeftRowsJoin(csi.getLeftInnerConstraints(),
                    comparator, printIndentation + printIndentStep));
            leftRows.addAll(buildLeftRowsJoin(csi.getRightInnerConstraints(),
                    comparator, printIndentation + printIndentStep));
            return leftRows;
        }

        Set<Row> leftRows = new TreeSet<Row>(comparator);
        QueryResult leftResult = execute(null, csi.getSource().getLeft(),
                csi.getLeftConstraint(), null, 0, -1, printIndentation);
        for (Row row : JcrUtils.getRows(leftResult)) {
            leftRows.add(row);
        }
        return leftRows;
    }

    /**
     * @param csi
     *            contains 'WHERE' constraints and the source information
     * @param rightConstraints
     *            contains 'ON' constraints
     * @param ignoreWhereConstraints
     * @param comparator
     *            used to merge similar rows together
     * @param printIndentation
     *            used in logging
     * @return the right-side dataset of the join operation
     * @throws RepositoryException
     */
    private Set<Row> buildRightRowsJoin(ConstraintSplitInfo csi,
            List<Constraint> rightConstraints, boolean ignoreWhereConstraints,
            Comparator<Row> comparator, int printIndentation)
            throws RepositoryException {

        if (csi.isMultiple()) {
            if (log.isDebugEnabled()) {
                log.debug(genString(printIndentation)
                        + "SQL2 JOIN RIGHT SIDE there are multiple inner splits.");
            }
            Set<Row> rightRows = new TreeSet<Row>(comparator);
            rightRows.addAll(buildRightRowsJoin(csi.getLeftInnerConstraints(),
                    rightConstraints, ignoreWhereConstraints, comparator,
                    printIndentation + printIndentStep));
            rightRows.addAll(buildRightRowsJoin(csi.getRightInnerConstraints(),
                    rightConstraints, ignoreWhereConstraints, comparator,
                    printIndentation + printIndentStep));
            return rightRows;
        }

        if (rightConstraints.size() < 500) {
            Set<Row> rightRows = new TreeSet<Row>(comparator);
            List<Constraint> localRightContraints = rightConstraints;
            Constraint rightConstraint = Constraints.and(qomFactory,
                    Constraints.or(qomFactory, localRightContraints),
                    csi.getRightConstraint());
            if (ignoreWhereConstraints) {
                rightConstraint = Constraints.or(qomFactory,
                        localRightContraints);
            }
            QueryResult rightResult = execute(null, csi.getSource().getRight(),
                    rightConstraint, null, 0, -1, printIndentation);
            for (Row row : JcrUtils.getRows(rightResult)) {
                rightRows.add(row);
            }
            return rightRows;
        }

        // the 'batch by 500' approach
        Set<Row> rightRows = new TreeSet<Row>(comparator);
        for (int i = 0; i < rightConstraints.size(); i += 500) {
            if (log.isDebugEnabled()) {
                log.debug(genString(printIndentation)
                        + "SQL2 JOIN RIGHT SIDE executing batch # " + i + ".");
            }
            List<Constraint> localRightContraints = rightConstraints.subList(i,
                    Math.min(i + 500, rightConstraints.size()));
            Constraint rightConstraint = Constraints.and(qomFactory,
                    Constraints.or(qomFactory, localRightContraints),
                    csi.getRightConstraint());
            if (ignoreWhereConstraints) {
                rightConstraint = Constraints.or(qomFactory,
                        localRightContraints);
            }

            QueryResult rightResult = execute(null, csi.getSource().getRight(),
                    rightConstraint, null, 0, -1, printIndentation);
            for (Row row : JcrUtils.getRows(rightResult)) {
                rightRows.add(row);
            }
        }
        return rightRows;
    }

    private static String genString(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private static void logQueryAnalysis(ConstraintSplitInfo csi,
            int printIndentation) throws RepositoryException {
        if (!log.isDebugEnabled()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(genString(printIndentation));
        sb.append("SQL2 JOIN analysis:");
        sb.append(IOUtils.LINE_SEPARATOR);
        sb.append(constraintSplitInfoToString(csi, 2));
        log.debug(sb.toString());
    }

    private static String constraintSplitInfoToString(ConstraintSplitInfo csi,
            int printIndentation) throws RepositoryException {

        if (csi.isMultiple()) {
            StringBuilder sb = new StringBuilder();
            sb.append(genString(printIndentation));
            sb.append("SQL2 JOIN inner split -> ");
            sb.append(IOUtils.LINE_SEPARATOR);
            sb.append(genString(printIndentation));
            sb.append("+");
            sb.append(IOUtils.LINE_SEPARATOR);
            sb.append(constraintSplitInfoToString(
                    csi.getLeftInnerConstraints(), printIndentation
                            + printIndentStep));
            sb.append(IOUtils.LINE_SEPARATOR);
            sb.append(genString(printIndentation));
            sb.append("+");
            sb.append(IOUtils.LINE_SEPARATOR);
            sb.append(constraintSplitInfoToString(
                    csi.getRightInnerConstraints(), printIndentation
                            + printIndentStep));
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(genString(printIndentation));
        sb.append("SQL2 JOIN source: ");
        sb.append(csi.getSource());
        sb.append(IOUtils.LINE_SEPARATOR);
        sb.append(genString(printIndentation));
        sb.append("SQL2 JOIN left constraint:  ");
        sb.append(csi.getLeftConstraint());
        sb.append(IOUtils.LINE_SEPARATOR);
        sb.append(genString(printIndentation));
        sb.append("SQL2 JOIN right constraint: ");
        sb.append(csi.getRightConstraint());
        return sb.toString();
    }

    protected QueryResult execute(Column[] columns, Selector selector,
            Constraint constraint, Ordering[] orderings, long offset,
            long limit, int printIndentation) throws RepositoryException {
        long time = System.currentTimeMillis();

        Map<String, NodeType> selectorMap = getSelectorNames(selector);
        String[] selectorNames = selectorMap.keySet().toArray(
                new String[selectorMap.size()]);

        Map<String, PropertyValue> columnMap = getColumnMap(columns,
                selectorMap);
        String[] columnNames = columnMap.keySet().toArray(
                new String[columnMap.size()]);

        Sort sort = new Sort();
        if (NATIVE_SORT) {
            sort = new Sort(createSortFields(orderings, session));
        }

        // if true it means that the LuceneQueryFactory should just let the
        // QueryEngine take care of sorting and applying offset and limit
        // constraints
        boolean externalSort = !NATIVE_SORT;
        RowIterator rows = null;
        try {
            rows = new RowIteratorAdapter(lqf.execute(columnMap, selector,
                    constraint, sort, externalSort, offset, limit));
        } catch (IOException e) {
            throw new RepositoryException("Failed to access the query index", e);
        } finally {
            log.debug(
                    "{}SQL2 SELECT took {} ms. selector: {}, columns: {}, constraint: {}, offset {}, limit {}",
                    new Object[] { genString(printIndentation),
                            System.currentTimeMillis() - time, selector,
                            Arrays.toString(columnNames), constraint, offset,
                            limit });
        }
        QueryResult result = new SimpleQueryResult(columnNames, selectorNames,
                rows);
        if (NATIVE_SORT) {
            return result;
        }

        long timeSort = System.currentTimeMillis();
        QueryResult sorted = sort(result, orderings, evaluator, offset, limit);
        log.debug("{}SQL2 SORT took {} ms.", genString(printIndentation),
                System.currentTimeMillis() - timeSort);
        return sorted;
    }

    public SortField[] createSortFields(Ordering[] orderings, Session session)
            throws RepositoryException {

        if (orderings == null || orderings.length == 0) {
            return new SortField[] { SortField.FIELD_SCORE };
        }
        // orderings[] -> (property, ordering)
        Map<String, Ordering> orderByProperties = new HashMap<String, Ordering>();
        for (Ordering o : orderings) {
            final String p = o.toString();
            if (!orderByProperties.containsKey(p)) {
                orderByProperties.put(p, o);
            }
        }
        final DynamicOperandFieldComparatorSource dofcs = new DynamicOperandFieldComparatorSource(
                session, evaluator, orderByProperties);

        List<SortField> sortFields = new ArrayList<SortField>();

        // as it turn out, orderByProperties.keySet() doesn't keep the original
        // insertion order
        for (Ordering o : orderings) {
            final String p = o.toString();
            // order on jcr:score does not use the natural order as
            // implemented in lucene. score ascending in lucene means that
            // higher scores are first. JCR specs that lower score values
            // are first.
            boolean isAsc = QueryObjectModelConstants.JCR_ORDER_ASCENDING
                    .equals(o.getOrder());
            if (JcrConstants.JCR_SCORE.equals(p)) {
                sortFields.add(new SortField(null, SortField.SCORE, !isAsc));
            } else {
                // TODO use native sort if available
                sortFields.add(new SortField(p, dofcs, !isAsc));
            }
        }
        return sortFields.toArray(new SortField[sortFields.size()]);
    }

    private Map<String, PropertyValue> getColumnMap(Column[] columns,
            Map<String, NodeType> selectors) throws RepositoryException {
        Map<String, PropertyValue> map = new LinkedHashMap<String, PropertyValue>();
        if (columns != null && columns.length > 0) {
            for (int i = 0; i < columns.length; i++) {
                String name = columns[i].getColumnName();
                if (name != null) {
                    map.put(name,
                            qomFactory.propertyValue(
                                    columns[i].getSelectorName(),
                                    columns[i].getPropertyName()));
                } else {
                    String selector = columns[i].getSelectorName();
                    map.putAll(getColumnMap(selector, selectors.get(selector)));
                }
            }
        } else {
            for (Map.Entry<String, NodeType> selector : selectors.entrySet()) {
                map.putAll(getColumnMap(selector.getKey(), selector.getValue()));
            }
        }
        return map;
    }

    private Map<String, PropertyValue> getColumnMap(String selector,
            NodeType type) throws RepositoryException {
        Map<String, PropertyValue> map = new LinkedHashMap<String, PropertyValue>();
        for (PropertyDefinition definition : type.getPropertyDefinitions()) {
            String name = definition.getName();
            if (!definition.isMultiple() && !"*".equals(name)) {
                // TODO: Add proper quoting
                map.put(selector + "." + name,
                        qomFactory.propertyValue(selector, name));
            }
        }
        return map;
    }

    private Map<String, NodeType> getSelectorNames(Source source)
            throws RepositoryException {
        if (source instanceof Selector) {
            Selector selector = (Selector) source;
            return Collections.singletonMap(selector.getSelectorName(),
                    getNodeType(selector));
        } else if (source instanceof Join) {
            Join join = (Join) source;
            Map<String, NodeType> map = new LinkedHashMap<String, NodeType>();
            map.putAll(getSelectorNames(join.getLeft()));
            map.putAll(getSelectorNames(join.getRight()));
            return map;
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown source type: " + source);
        }
    }

    private NodeType getNodeType(Selector selector) throws RepositoryException {
        try {
            return ntManager.getNodeType(selector.getNodeTypeName());
        } catch (NoSuchNodeTypeException e) {
            throw new InvalidQueryException(
                    "Selected node type does not exist: " + selector, e);
        }
    }

    /**
     * Sorts the given query results according to the given QOM orderings. If
     * one or more orderings have been specified, this method will iterate
     * through the entire original result set, order the collected rows, and
     * return a new result set based on the sorted collection of rows.
     * 
     * @param result
     *            original query results
     * @param orderings
     *            QOM orderings
     * @param offset
     *            result offset
     * @param limit
     *            result limit
     * @return sorted query results
     * @throws RepositoryException
     *             if the results can not be sorted
     */
    protected static QueryResult sort(QueryResult result,
            final Ordering[] orderings, OperandEvaluator evaluator,
            long offset, long limit) throws RepositoryException {
        if ((orderings != null && orderings.length > 0) || offset != 0
                || limit >= 0) {
            List<Row> rows = new ArrayList<Row>();

            RowIterator iterator = result.getRows();
            while (iterator.hasNext()) {
                rows.add(iterator.nextRow());
            }

            if (orderings != null && orderings.length > 0) {
                Collections.sort(rows, new RowComparator(orderings, evaluator));
            }

            if (offset > 0) {
                int size = rows.size();
                rows = rows.subList((int) Math.min(offset, size), size);
            }
            if (limit >= 0) {
                int size = rows.size();
                rows = rows.subList(0, (int) Math.min(limit, size));
            }

            return new SimpleQueryResult(result.getColumnNames(),
                    result.getSelectorNames(), new RowIteratorAdapter(rows));
        } else {
            return result;
        }
    }

}
