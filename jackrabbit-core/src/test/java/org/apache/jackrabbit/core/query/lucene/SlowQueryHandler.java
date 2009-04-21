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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

import org.apache.jackrabbit.core.query.AbstractQueryHandler;
import org.apache.jackrabbit.core.query.ExecutableQuery;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.uuid.UUID;

/**
 * <code>SlowQueryHandler</code> implements a dummy query handler for testing
 * purpose.
 */
public class SlowQueryHandler extends AbstractQueryHandler {

    protected void doInit() throws IOException {
        // sleep for 10 seconds then try to read from the item state manager
        // the repository.xml is configured with a 5 second maxIdleTime
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            // ignore
        }
        NodeId id = new NodeId(UUID.randomUUID());
        getContext().getItemStateManager().hasItemState(id);
    }

    public void addNode(NodeState node) throws RepositoryException, IOException {
    }

    public void deleteNode(NodeId id) throws IOException {
    }

    public void close() throws IOException {
    }

    public ExecutableQuery createExecutableQuery(SessionImpl session,
                                                 ItemManager itemMgr, String statement,
                                                 String language)
            throws InvalidQueryException {
        return null;
    }

    public ExecutableQuery createExecutableQuery(SessionImpl session,
                                                 ItemManager itemMgr,
                                                 QueryObjectModelTree qomTree)
            throws InvalidQueryException {
        return null;
    }
}
