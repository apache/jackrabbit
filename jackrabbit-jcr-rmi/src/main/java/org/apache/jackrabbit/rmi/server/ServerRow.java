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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;

import org.apache.jackrabbit.rmi.remote.RemoteRow;
import org.apache.jackrabbit.rmi.value.SerialValueFactory;

/**
 * Remote adapter for the JCR {@link javax.jcr.query.Row Row} interface.
 * This class makes a local session available as an RMI service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteRow RemoteRow}
 * interface.
 *
 * @see javax.jcr.query.Row
 * @see org.apache.jackrabbit.rmi.remote.RemoteRow
 */
public class ServerRow extends ServerObject implements RemoteRow {

    /** The adapted local row. */
    private Row row;

    /**
     * Creates a remote adapter for the given local query row.
     *
     * @param row local query row
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerRow(Row row, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.row = row;
    }

    /** {@inheritDoc} */
    public Value[] getValues() throws RepositoryException, RemoteException {
        return getSerialValues(row.getValues());
    }

    /** {@inheritDoc} */
    public Value getValue(String propertyName)
            throws RepositoryException, RemoteException {
        return SerialValueFactory.makeSerialValue(row.getValue(propertyName));
    }
}
