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
package org.apache.jackrabbit.spi.rmi.server;

import org.apache.jackrabbit.spi.rmi.remote.RemoteQueryInfo;
import org.apache.jackrabbit.spi.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.spi.rmi.common.QueryResultRowImpl;
import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.QueryResultRow;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.util.IteratorHelper;

import java.rmi.RemoteException;

/**
 * <code>ServerQueryInfo</code>...
 */
class ServerQueryInfo extends ServerObject implements RemoteQueryInfo {

    /**
     * The underlying query info.
     */
    private final QueryInfo queryInfo;

    /**
     * The number of rows to transmit in a single RMI call.
     */
    private final int iteratorBufferSize;

    /**
     * An id factory, which creates serializable ids.
     */
    private final IdFactory idFactory;

    public ServerQueryInfo(QueryInfo queryInfo,
                           int iteratorBufferSize,
                           IdFactory idFactory)
            throws RemoteException {
        this.queryInfo = queryInfo;
        this.iteratorBufferSize = iteratorBufferSize;
        this.idFactory = idFactory;
    }

    /**
     * {@inheritDoc}
     */
    public RemoteIterator getRows() throws RemoteException {
        return new ServerIterator(new IteratorHelper(queryInfo.getRows()) {
            public Object next() {
                QueryResultRow row = (QueryResultRow) super.next();
                NodeId rowId = row.getNodeId();
                rowId = idFactory.createNodeId(
                        rowId.getUniqueID(), rowId.getPath());
                return new QueryResultRowImpl(
                        rowId, row.getScore(), row.getValues());
            }
        }, iteratorBufferSize);
    }

    /**
     * {@inheritDoc}
     */
    public QName[] getColumnNames() throws RemoteException {
        return queryInfo.getColumnNames();
    }
}
