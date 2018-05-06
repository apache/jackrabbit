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

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.jackrabbit.commons.query.qom.JoinType;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.query.lucene.HierarchyResolver;
import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.query.qom.ChildNodeJoinConditionImpl;
import org.apache.jackrabbit.spi.commons.query.qom.DefaultQOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.DescendantNodeJoinConditionImpl;
import org.apache.jackrabbit.spi.commons.query.qom.EquiJoinConditionImpl;
import org.apache.jackrabbit.spi.commons.query.qom.JoinConditionImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SameNodeJoinConditionImpl;
import org.apache.lucene.index.IndexReader;

/**
 * <code>Join</code> implements the result of a join.
 */
public class Join implements MultiColumnQueryHits {

    /**
     * The outer query hits.
     */
    protected final MultiColumnQueryHits outer;

    /**
     * The score node index of the outer query hits.
     */
    protected final int outerScoreNodeIndex;

    /**
     * Whether this is an inner join.
     */
    protected final boolean innerJoin;

    /**
     * The join condition.
     */
    protected final Condition condition;

    /**
     * The selector names.
     */
    protected final Name[] selectorNames;

    /**
     * An array of empty inner query hits.
     */
    protected final ScoreNode[] emptyInnerHits;

    /**
     * A buffer for joined score node rows.
     */
    protected final List<ScoreNode[]> buffer = new LinkedList<ScoreNode[]>();

    /**
     * Creates a new join.
     *
     * @param outer               the outer query hits.
     * @param outerScoreNodeIndex the score node index of the outer query hits
     *                            that is used for the join.
     * @param innerJoin           whether this is an inner join.
     * @param condition           the join condition.
     */
    private Join(MultiColumnQueryHits outer,
                 int outerScoreNodeIndex,
                 boolean innerJoin,
                 Condition condition) {
        this.outer = outer;
        this.outerScoreNodeIndex = outerScoreNodeIndex;
        this.innerJoin = innerJoin;
        this.condition = condition;
        this.emptyInnerHits = new ScoreNode[condition.getInnerSelectorNames().length];
        // outer selector names go to the left, inner selector
        // names go to the right.
        // this needs to be in sync with ScoreNode[] aggregration/joining
        // in nextScoreNodes() !
        this.selectorNames = new Name[outer.getSelectorNames().length + emptyInnerHits.length];
        System.arraycopy(outer.getSelectorNames(), 0, selectorNames, 0, outer.getSelectorNames().length);
        System.arraycopy(condition.getInnerSelectorNames(), 0, selectorNames, outer.getSelectorNames().length, emptyInnerHits.length);
    }

    /**
     * Creates a new join result.
     *
     * @param left      the left query hits.
     * @param right     the right query hits.
     * @param joinType  the join type.
     * @param condition the QOM join condition.
     * @param reader    the index reader.
     * @param resolver  the hierarchy resolver.
     * @param nsMappings namespace mappings of this index
     * @param hmgr      the hierarchy manager of the workspace.
     * @return the join result.
     * @throws IOException if an error occurs while executing the join.
     */
    public static Join create(final MultiColumnQueryHits left,
                              final MultiColumnQueryHits right,
                              final JoinType joinType,
                              final JoinConditionImpl condition,
                              final IndexReader reader,
                              final HierarchyResolver resolver,
                              final NamespaceMappings nsMappings,
                              final HierarchyManager hmgr)
            throws IOException {
        try {
            return (Join) condition.accept(new DefaultQOMTreeVisitor() {

                private boolean isInner = JoinType.INNER == joinType;
                private MultiColumnQueryHits outer;
                private int outerIdx;

                public Object visit(DescendantNodeJoinConditionImpl node, Object data)
                        throws Exception {
                    MultiColumnQueryHits ancestor = getSourceWithName(node.getAncestorSelectorQName(), left, right);
                    MultiColumnQueryHits descendant = getSourceWithName(node.getDescendantSelectorQName(), left, right);
                    Condition c;
                    if (isInner
                            || descendant == left && JoinType.LEFT == joinType
                            || descendant == right && JoinType.RIGHT == joinType) {
                        // also applies to inner join
                        // assumption: DescendantNodeJoin is more
                        // efficient than AncestorNodeJoin, TODO: verify
                        outer = descendant;
                        outerIdx = getIndex(outer, node.getDescendantSelectorQName());
                        c = new DescendantNodeJoin(ancestor, node.getAncestorSelectorQName(), reader, resolver);
                    } else {
                        // left == ancestor
                        outer = ancestor;
                        outerIdx = getIndex(outer, node.getAncestorSelectorQName());
                        c = new AncestorNodeJoin(descendant, node.getDescendantSelectorQName(), reader, resolver);
                    }
                    return new Join(outer, outerIdx, isInner, c);
                }

                public Object visit(EquiJoinConditionImpl node, Object data)
                        throws Exception {
                    MultiColumnQueryHits src1 = getSourceWithName(node.getSelector1QName(), left, right);
                    MultiColumnQueryHits src2 = getSourceWithName(node.getSelector2QName(), left, right);
                    MultiColumnQueryHits inner;
                    Name innerName;
                    Name innerPropName;
                    Name outerPropName;
                    if (isInner
                            || src1 == left && JoinType.LEFT == joinType
                            || src1 == right && JoinType.RIGHT == joinType) {
                        outer = src1;
                        outerIdx = getIndex(outer, node.getSelector1QName());
                        inner = src2;
                        innerName = node.getSelector2QName();
                        innerPropName = node.getProperty2QName();
                        outerPropName = node.getProperty1QName();
                    } else {
                        outer = src2;
                        outerIdx = getIndex(outer, node.getSelector2QName());
                        inner = src1;
                        innerName = node.getSelector1QName();
                        innerPropName = node.getProperty1QName();
                        outerPropName = node.getProperty2QName();
                    }

                    Condition c = new EquiJoin(
                            inner, getIndex(inner, innerName), nsMappings,
                            reader, innerPropName, outerPropName);
                    return new Join(outer, outerIdx, isInner, c);
                }

                public Object visit(ChildNodeJoinConditionImpl node, Object data)
                        throws Exception {
                    MultiColumnQueryHits child = getSourceWithName(node.getChildSelectorQName(), left, right);
                    MultiColumnQueryHits parent = getSourceWithName(node.getParentSelectorQName(), left, right);
                    Condition c;
                    if (child == left && JoinType.LEFT == joinType
                            || child == right && JoinType.RIGHT == joinType) {
                        outer = child;
                        outerIdx = getIndex(outer, node.getChildSelectorQName());
                        c = new ChildNodeJoin(parent, reader, resolver, node);
                    } else {
                        // also applies to inner joins
                        // assumption: ParentNodeJoin is more efficient than
                        // ChildNodeJoin, TODO: verify
                        outer = parent;
                        outerIdx = getIndex(outer, node.getParentSelectorQName());
                        c = new ParentNodeJoin(child, reader, resolver, node);
                    }
                    return new Join(outer, outerIdx, isInner, c);
                }

                public Object visit(SameNodeJoinConditionImpl node, Object data)
                        throws Exception {
                    MultiColumnQueryHits src1 = getSourceWithName(node.getSelector1QName(), left, right);
                    MultiColumnQueryHits src2 = getSourceWithName(node.getSelector2QName(), left, right);
                    Condition c;
                    if (isInner
                            || src1 == left && JoinType.LEFT == joinType
                            || src1 == right && JoinType.RIGHT == joinType) {
                        outer = src1;
                        outerIdx = getIndex(outer, node.getSelector1QName());
                        Path selector2Path = node.getSelector2QPath();
                        if (selector2Path == null || (selector2Path.getLength() == 1 && selector2Path.denotesCurrent())) {
                            c = new SameNodeJoin(src2, node.getSelector2QName(), reader);
                        } else {
                            c = new DescendantPathNodeJoin(src2, node.getSelector2QName(),
                                    node.getSelector2QPath(), hmgr);
                        }
                    } else {
                        outer = src2;
                        outerIdx = getIndex(outer, node.getSelector2QName());
                        Path selector2Path = node.getSelector2QPath();
                        if (selector2Path == null || (selector2Path.getLength() == 1 && selector2Path.denotesCurrent())) {
                            c = new SameNodeJoin(src1, node.getSelector1QName(), reader);
                        } else {
                            c = new AncestorPathNodeJoin(src1, node.getSelector1QName(),
                                    node.getSelector2QPath(), hmgr);
                        }
                    }
                    return new Join(outer, outerIdx, isInner, c);
                }
            }, null);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ex = new IOException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode[] nextScoreNodes() throws IOException {
        if (!buffer.isEmpty()) {
            return buffer.remove(0);
        }
        do {
            // refill buffer
            ScoreNode[] sn = outer.nextScoreNodes();
            if (sn == null) {
                return null;
            }
            ScoreNode[][] nodes = condition.getMatchingScoreNodes(sn[outerScoreNodeIndex]);
            if (nodes != null) {
                for (ScoreNode[] node : nodes) {
                    // create array with both outer and inner
                    ScoreNode[] tmp = new ScoreNode[sn.length + node.length];
                    System.arraycopy(sn, 0, tmp, 0, sn.length);
                    System.arraycopy(node, 0, tmp, sn.length, node.length);
                    buffer.add(tmp);
                }
            } else if (!innerJoin) {
                // create array with both inner and outer
                ScoreNode[] tmp = new ScoreNode[sn.length + emptyInnerHits.length];
                System.arraycopy(sn, 0, tmp, 0, sn.length);
                System.arraycopy(emptyInnerHits, 0, tmp, sn.length, emptyInnerHits.length);
                buffer.add(tmp);
            }
        } while (buffer.isEmpty());

        return buffer.remove(0);
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getSelectorNames() {
        return selectorNames;
    }

    /**
     * {@inheritDoc}
     * Closes {@link #outer} source and the {@link #condition}.
     */
    public void close() throws IOException {
        IOException ex = null;
        try {
            outer.close();
        } catch (IOException e) {
            ex = e;
        }
        try {
            condition.close();
        } catch (IOException e) {
            if (ex == null) {
                ex = e;
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    /**
     * This default implementation always returns <code>-1</code>.
     *
     * @return always <code>-1</code>.
     */
    public int getSize() {
        return -1;
    }

    /**
     * {@inheritDoc}
     * Skips by calling {@link #nextScoreNodes()} <code>n</code> times. Sub
     * classes may provide a more performance implementation.
     */
    public void skip(int n) throws IOException {
        while (n-- > 0) {
            if (nextScoreNodes() == null) {
                return;
            }
        }
    }

    protected static MultiColumnQueryHits getSourceWithName(
            Name selectorName,
            MultiColumnQueryHits left,
            MultiColumnQueryHits right) {
        if (Arrays.asList(left.getSelectorNames()).contains(selectorName)) {
            return left;
        } else if (Arrays.asList(right.getSelectorNames()).contains(selectorName)) {
            return right;
        } else {
            throw new IllegalArgumentException("unknown selector name: " + selectorName);
        }
    }

    /**
     * Returns the index of the selector with the given <code>selectorName</code>
     * within the given <code>source</code>.
     *
     * @param source       a source.
     * @param selectorName a selector name.
     * @return the index within the source or <code>-1</code> if the name does
     *         not exist in <code>source</code>.
     */
    protected static int getIndex(MultiColumnQueryHits source,
                                  Name selectorName) {
        return Arrays.asList(source.getSelectorNames()).indexOf(selectorName);
    }
}
