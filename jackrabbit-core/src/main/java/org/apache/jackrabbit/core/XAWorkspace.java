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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.XAItemStateManager;
import org.apache.jackrabbit.core.state.LocalItemStateManager;

import javax.jcr.NoSuchWorkspaceException;

/**
 * Workspace extension that works in an XA environment.
 */
public class XAWorkspace extends WorkspaceImpl {

    /**
     * the name of the workspace info attribute in the transaction context
     */
    private static final String ATTR_NAME_WORKSPACE_INFO =
            RepositoryImpl.WorkspaceInfo.class.getName();

    /**
     * Protected constructor.
     *
     * @param wspConfig The workspace configuration
     * @param stateMgr  The shared item state manager
     * @param rep       The repository
     * @param session   The session
     */
    protected XAWorkspace(WorkspaceConfig wspConfig,
                          SharedItemStateManager stateMgr, RepositoryImpl rep,
                          SessionImpl session) {

        super(wspConfig, stateMgr, rep, session);
    }

    /**
     * {@inheritDoc}
     */
    protected LocalItemStateManager createItemStateManager(SharedItemStateManager shared) {
        return new XAItemStateManager(shared, this, rep.getItemStateCacheFactory());
    }

    /**
     * Returns an internal XAResource that is used at the beginning of the
     * resources chain in {@link XASessionImpl#init()}. This resource will lock
     * the workspace on <code>prepare</code>.
     *
     * @return an internal XAResource
     */
    protected InternalXAResource getXAResourceBegin() {
        return new InternalXAResource() {
            public void associate(TransactionContext tx) {
            }

            public void beforeOperation(TransactionContext tx) {
            }

            public void prepare(TransactionContext tx)
                    throws TransactionException {
                try {
                    RepositoryImpl.WorkspaceInfo wspInfo =
                            rep.getWorkspaceInfo(wspConfig.getName());
                    wspInfo.lockAcquire();
                    tx.setAttribute(ATTR_NAME_WORKSPACE_INFO, wspInfo);
                } catch (NoSuchWorkspaceException e) {
                    throw new TransactionException("Error while preparing for transaction", e);
                }
            }

            public void commit(TransactionContext tx) {
            }

            public void rollback(TransactionContext tx) {
            }

            public void afterOperation(TransactionContext tx) {
            }
        };
    }

    /**
     * Returns an internal XAResource that is used at the end of the
     * resources chain in {@link XASessionImpl#init()}. This resource will unlock
     * the workspace on <code>commit</code> or on <code>rollback</code>.
     *
     * @return an internal XAResource
     */
    protected InternalXAResource getXAResourceEnd() {
        return new InternalXAResource() {
            public void associate(TransactionContext tx) {
            }

            public void beforeOperation(TransactionContext tx) {
            }

            public void prepare(TransactionContext tx) {
            }

            public void commit(TransactionContext tx) {
                RepositoryImpl.WorkspaceInfo wspInfo =
                        (RepositoryImpl.WorkspaceInfo)
                                tx.getAttribute(ATTR_NAME_WORKSPACE_INFO);
                if (wspInfo != null) {
                    wspInfo.lockRelease();
                    tx.removeAttribute(ATTR_NAME_WORKSPACE_INFO);
                }
            }

            public void rollback(TransactionContext tx) {
                RepositoryImpl.WorkspaceInfo wspInfo =
                        (RepositoryImpl.WorkspaceInfo)
                                tx.getAttribute(ATTR_NAME_WORKSPACE_INFO);
                if (wspInfo != null) {
                    wspInfo.lockRelease();
                    tx.removeAttribute(ATTR_NAME_WORKSPACE_INFO);
                }
            }

            public void afterOperation(TransactionContext tx) {
            }
        };
    }

}
