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

import org.apache.commons.io.IOExceptionWithCause;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Iterator;

/**
 * Implements default behaviour for some methods of {@link QueryHandler}.
 */
public abstract class AbstractQueryHandler implements QueryHandler {

    /**
     * Logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(AbstractQueryHandler.class);

    /**
     * Search index file system, or <code>null</code>
     */
    protected FileSystem fs;

    /**
     * The context for this query handler.
     */
    private QueryHandlerContext context;

    /**
     * The {@link OnWorkspaceInconsistency} handler. Defaults to 'fail'.
     */
    private OnWorkspaceInconsistency owi = OnWorkspaceInconsistency.FAIL;

    /**
     * The name of a class that extends {@link AbstractQueryImpl}.
     */
    private String queryClass = QueryImpl.class.getName();

    /**
     * The max idle time for this query handler until it is stopped. This
     * property is actually not used anymore.
     */
    private String idleTime;

    /**
     * Initializes this query handler by setting all properties in this class
     * with appropriate parameter values.
     *
     * @param fs search index file system, or <code>null</code>
     * @param context the context for this query handler.
     */
    public final void init(FileSystem fs, QueryHandlerContext context)
            throws IOException {
        this.fs = fs;
        this.context = context;
        doInit();
    }

    public void close() throws IOException {
        if (fs != null) {
            try {
                fs.close();
            } catch (FileSystemException e) {
                throw new IOExceptionWithCause(
                        "Unable to close search index file system: " + fs, e);
            }
        }
    }

    /**
     * This method must be implemented by concrete sub classes and will be
     * called from {@link #init}.
     *
     * @throws IOException If an error occurs.
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
    public synchronized void updateNodes(
            Iterator<NodeId> remove, Iterator<NodeState> add)
            throws RepositoryException, IOException {
        while (remove.hasNext()) {
            deleteNode(remove.next());
        }
        while (add.hasNext()) {
            addNode(add.next());
        }
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
     * Currently the valid names are:
     * <ul>
     * <li><code>fail</code></li>
     * <li><code>log</code></li>
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

    /**
     * Sets the name of the query class to use.
     *
     * @param queryClass the name of the query class to use.
     */
    public void setQueryClass(String queryClass) {
        this.queryClass = queryClass;
    }

    /**
     * @return the name of the query class to use.
     */
    public String getQueryClass() {
        return queryClass;
    }

    /**
     * Sets the query handler idle time.
     * @deprecated
     * This parameter is not supported any more.
     * Please use 'maxIdleTime' in the repository configuration.
     * 
     * @param idleTime the query handler idle time.
     */
    public void setIdleTime(String idleTime) {
        log.warn("Parameter 'idleTime' is not supported anymore. "
                + "Please use 'maxIdleTime' in the repository configuration.");
        this.idleTime = idleTime;
    }

    /**
     * @return the query handler idle time.
     */
    public String getIdleTime() {
        return idleTime;
    }

}
