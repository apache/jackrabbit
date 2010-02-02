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
package org.apache.jackrabbit.jcr2spi.query;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Source;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.commons.query.QueryObjectModelBuilderRegistry;
import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.ManagerProvider;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;

/**
 * <code>QueryObjectModelImpl</code> implements the jcr2spi query object model.
 */
public class QueryObjectModelImpl extends QueryImpl implements QueryObjectModel {

    /**
     * The query object model tree.
     */
    private final QueryObjectModelTree qomTree;

    public QueryObjectModelImpl(Session session,
                                ManagerProvider mgrProvider,
                                ItemManager itemMgr,
                                WorkspaceManager wspManager,
                                QueryObjectModelTree qomTree,
                                Node node)
            throws InvalidQueryException, RepositoryException {
        super(session, mgrProvider, itemMgr, wspManager,
                getSQL2ForQOM(qomTree), Query.JCR_SQL2, node);
        this.qomTree = qomTree;
    }

    /**
     * @return always {@link Query#JCR_JQOM}.
     */
    @Override
    public String getLanguage() {
        return Query.JCR_JQOM;
    }

    /**
     * {@inheritDoc}
     */
    public Source getSource() {
        return qomTree.getSource();
    }

    /**
     * {@inheritDoc}
     */
    public Constraint getConstraint() {
        return qomTree.getConstraint();
    }

    /**
     * {@inheritDoc}
     */
    public Ordering[] getOrderings() {
        return qomTree.getOrderings();
    }

    /**
     * {@inheritDoc}
     */
    public Column[] getColumns() {
        return qomTree.getColumns();
    }

    private static String getSQL2ForQOM(QueryObjectModelTree qomTree)
            throws InvalidQueryException {
        return QueryObjectModelBuilderRegistry.getQueryObjectModelBuilder(Query.JCR_JQOM).toString(new DummyQOM(qomTree));
    }

    private static class DummyQOM implements QueryObjectModel {

        /**
         * The query object model tree.
         */
        private final QueryObjectModelTree qomTree;

        public DummyQOM(QueryObjectModelTree qomTree) {
            this.qomTree = qomTree;
        }

        /**
         * {@inheritDoc}
         */
        public Source getSource() {
            return qomTree.getSource();
        }

        /**
         * {@inheritDoc}
         */
        public Constraint getConstraint() {
            return qomTree.getConstraint();
        }

        /**
         * {@inheritDoc}
         */
        public Ordering[] getOrderings() {
            return qomTree.getOrderings();
        }

        /**
         * {@inheritDoc}
         */
        public Column[] getColumns() {
            return qomTree.getColumns();
        }

        public QueryResult execute()
                throws InvalidQueryException, RepositoryException {
            throw new UnsupportedOperationException();
        }

        public void setLimit(long limit) {
            throw new UnsupportedOperationException();
        }

        public void setOffset(long offset) {
            throw new UnsupportedOperationException();
        }

        public String getStatement() {
            throw new UnsupportedOperationException();
        }

        public String getLanguage() {
            throw new UnsupportedOperationException();
        }

        public String getStoredQueryPath()
                throws ItemNotFoundException, RepositoryException {
            throw new UnsupportedOperationException();
        }

        public Node storeAsNode(String absPath) throws ItemExistsException,
                PathNotFoundException, VersionException,
                ConstraintViolationException, LockException,
                UnsupportedRepositoryOperationException, RepositoryException {
            throw new UnsupportedOperationException();
        }

        public void bindValue(String varName, Value value)
                throws IllegalArgumentException, RepositoryException {
            throw new UnsupportedOperationException();
        }

        public String[] getBindVariableNames() throws RepositoryException {
            throw new UnsupportedOperationException();
        }

    }
}
