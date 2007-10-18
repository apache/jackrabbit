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
package org.apache.jackrabbit.spi.rmi.client;

import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.rmi.remote.RemoteQueryInfo;

import javax.jcr.RangeIterator;
import java.rmi.RemoteException;

/**
 * <code>ClientQueryInfo</code> wraps a remote query info and exposes it as a
 * SPI query info.
 */
class ClientQueryInfo implements QueryInfo {

    /**
     * The remote query info.
     */
    private final RemoteQueryInfo queryInfo;

    /**
     * Creates a new client query info wrapping a remote query info.
     *
     * @param queryInfo the remote query info.
     */
    ClientQueryInfo(RemoteQueryInfo queryInfo) {
        this.queryInfo = queryInfo;
    }

    /**
     * {@inheritDoc}
     */
    public RangeIterator getRows() {
        try {
            return new ClientIterator(queryInfo.getRows());
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getColumnNames() {
        try {
            return queryInfo.getColumnNames();
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }
}
