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

import javax.transaction.xa.XAResource;

import javax.jcr.Repository;

import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.rmi.remote.RemoteXASession;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteXASession RemoteXASession}
 * interface.
 *
 * @since 1.4
 */
public class ClientXASession extends ClientSession implements XASession {

    /**
     * The adapted remote transaction enabled session.
     */
    private RemoteXASession remote;

    /**
     * Creates a client adapter for the given remote session which is
     * transaction enabled.
     */
    public ClientXASession(
            Repository repository, RemoteXASession remote,
            LocalAdapterFactory factory) {
        super(repository, remote, factory);
        this.remote = remote;
    }

    public XAResource getXAResource() {
        try {
            return getFactory().getXAResource(remote.getXAResource());
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

}
