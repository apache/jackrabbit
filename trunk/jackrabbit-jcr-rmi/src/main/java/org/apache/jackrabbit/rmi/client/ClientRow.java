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
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Row;

import org.apache.jackrabbit.rmi.remote.RemoteRow;

/**
 * Local adapter for the JCR-RMI {@link RemoteRow RemoteRow}
 * interface. This class makes a remote query row locally available using
 * the JCR {@link Row Row} interface.
 *
 * @see javax.jcr.query.Row Row
 * @see org.apache.jackrabbit.rmi.remote.RemoteRow
 */
public class ClientRow extends ClientObject implements Row {

    /** Current session. */
    private Session session;

    /** The remote query row. */
    private RemoteRow remote;

    /**
     * Creates a client adapter for the given remote query row.
     *
     * @param remote remote query row
     */
    public ClientRow(Session session, RemoteRow remote,
            LocalAdapterFactory factory) {
        super(factory);
        this.session = session;
        this.remote = remote;
    }

    /** {@inheritDoc} */
    public Value[] getValues() throws RepositoryException {
        try {
            return remote.getValues();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Value getValue(String s) throws RepositoryException {
        try {
            return remote.getValue(s);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node getNode() throws RepositoryException {
        try {
            return getFactory().getNode(session, remote.getNode());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node getNode(String selectorName) throws RepositoryException {
        try {
            return getFactory().getNode(session, remote.getNode(selectorName));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getPath() throws RepositoryException {
        try {
            return remote.getPath();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getPath(String selectorName) throws RepositoryException {
        try {
            return remote.getPath(selectorName);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public double getScore() throws RepositoryException {
        try {
            return remote.getScore();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public double getScore(String selectorName) throws RepositoryException {
        try {
            return remote.getScore(selectorName);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

}
