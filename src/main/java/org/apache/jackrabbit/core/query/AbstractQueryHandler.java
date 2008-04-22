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
package org.apache.jackrabbit.core.query;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeIdIterator;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.core.state.NodeStateIterator;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import java.io.IOException;

/**
 * Implements default behaviour for some methods of {@link QueryHandler}.
 */
public abstract class AbstractQueryHandler implements QueryHandler {

    /**
     * The context for this query handler.
     */
    private QueryHandlerContext context;

    /**
     * The {@link OnWorkspaceInconsistency} handler. Defaults to 'fail'.
     */
    private OnWorkspaceInconsistency owi = OnWorkspaceInconsistency.FAIL;

    /**
     * Initializes this query handler by setting all properties in this class
     * with appropriate parameter values.
     *
     * @param context the context for this query handler.
     */
    public final void init(QueryHandlerContext context) throws IOException {
        this.context = context;
        doInit();
    }

    /**
     * This method must be implemented by concrete sub classes and will be
     * called from {@link #init}.
     */
    protected abstract void doInit() throws IOException;

    /**
     * Returns the context for this query handler.
     *
     * @return the <code>QueryHandlerContext</code> instance for this
     *         <code>QueryHandler</code>.
     */
    public QueryHandlerContext getContext() {
        return context;
    }

    /**
     * This default implementation calls the individual {@link #deleteNode(org.apache.jackrabbit.core.NodeId)}
     * and {@link #addNode(org.apache.jackrabbit.core.state.NodeState)} methods
     * for each entry in the iterators. First the nodes to remove are processed
     * then the nodes to add.
     *
     * @param remove uuids of nodes to remove.
     * @param add NodeStates to add.
     * @throws RepositoryException if an error occurs while indexing a node.
     * @throws IOException if an error occurs while updating the index.
     */
    public synchronized void updateNodes(NodeIdIterator remove, NodeStateIterator add)
            throws RepositoryException, IOException {
        while (remove.hasNext()) {
            deleteNode(remove.nextNodeId());
        }
        while (add.hasNext()) {
            addNode(add.nextNodeState());
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Throws {@link UnsupportedOperationException}.
     */
    public ExecutablePreparedQuery createExecutablePreparedQuery(
            SessionImpl session,
            ItemManager itemMgr,
            QueryObjectModelTree qomTree) throws InvalidQueryException {
        throw new UnsupportedOperationException(
                "This query handler does not support prepared queries");
    }

    /**
     * @return the {@link OnWorkspaceInconsistency} handler.
     */
    public OnWorkspaceInconsistency getOnWorkspaceInconsistencyHandler() {
        return owi;
    }

    //--------------------------< properties >----------------------------------

    /**
     * Sets the {@link OnWorkspaceInconsistency} handler with the given name.
     * Currently the only valid name is:
     * <ul>
     * <li><code>fail</code></li>
     * </ul>
     *
     * @param name the name of a {@link OnWorkspaceInconsistency} handler.
     */
    public void setOnWorkspaceInconsistency(String name) {
        owi = OnWorkspaceInconsistency.fromString(name);
    }

    /**
     * @return the name of the currently set {@link OnWorkspaceInconsistency}.
     */
    public String getOnWorkspaceInconsistency() {
        return owi.getName();
    }

}
