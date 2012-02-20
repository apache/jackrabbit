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

import javax.jcr.Credentials;
import javax.jcr.Value;

import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.remote.RemoteSession;

/**
 * Dummy remote repository instance that throws a {@link RemoteException}
 * whenever any method is invoked. Used as a sentinel object by the
 * {@link SafeClientRepository} class.
 */
public class BrokenRemoteRepository implements RemoteRepository {

    /**
     * The remote exception thrown by methods of this instance.
     */
    private final RemoteException exception;

    /**
     * Creates a remote repository whose methods throw the given
     * exception.
     *
     * @param exception remote exception
     */
    public BrokenRemoteRepository(RemoteException exception) {
        this.exception = exception;
    }

    /**
     * Creates a remote repository whose methods trow a remote
     * exception with the given message.
     *
     * @param message exception message
     */
    public BrokenRemoteRepository(String message) {
        this(new RemoteException(message));
    }

    /**
     * Creates a remote repository whose methods throw a remote exception.
     */
    public BrokenRemoteRepository() {
        this(new RemoteException());
    }

    //----------------------------------------------------< RemoteRepository >

    /**
     * Throws a {@link RemoteException}.
     *
     * @param key ignored
     * @return nothing
     * @throws RemoteException always thrown
     */
    public String getDescriptor(String key) throws RemoteException {
        throw exception;
    }

    /**
     * Throws a {@link RemoteException}.
     *
     * @return nothing
     * @throws RemoteException always thrown
     */
    public String[] getDescriptorKeys() throws RemoteException {
        throw exception;
    }

    /**
     * Throws a {@link RemoteException}.
     *
     * @return nothing
     * @throws RemoteException always thrown
     */
    public RemoteSession login() throws RemoteException {
        throw exception;
    }

    /**
     * Throws a {@link RemoteException}.
     *
     * @param workspace ignored
     * @return nothing
     * @throws RemoteException always thrown
     */
    public RemoteSession login(String workspace) throws RemoteException {
        throw exception;
    }

    /**
     * Throws a {@link RemoteException}.
     *
     * @param credentials ignored
     * @return nothing
     * @throws RemoteException always thrown
     */
    public RemoteSession login(Credentials credentials) throws RemoteException {
        throw exception;
    }

    /**
     * Throws a {@link RemoteException}.
     *
     * @param workspace ignored
     * @param credentials ignored
     * @return nothing
     * @throws RemoteException always thrown
     */
    public RemoteSession login(Credentials credentials, String workspace)
            throws RemoteException {
        throw exception;
    }

    /**
     * Throws a {@link RemoteException}.
     *
     * @param key ignored
     * @return nothing
     * @throws RemoteException always thrown
     */
	public Value getDescriptorValue(String key) throws RemoteException {
        throw exception;
	}

    /**
     * Throws a {@link RemoteException}.
     *
     * @param key ignored
     * @return nothing
     * @throws RemoteException always thrown
     */
	public Value[] getDescriptorValues(String key) throws RemoteException {
        throw exception;
	}

    /**
     * Throws a {@link RemoteException}.
     *
     * @param key ignored
     * @return nothing
     * @throws RemoteException always thrown
     */
	public boolean isSingleValueDescriptor(String key) throws RemoteException {
        throw exception;
	}

    /**
     * Throws a {@link RemoteException}.
     *
     * @param key ignored
     * @return nothing
     * @throws RemoteException always thrown
     */
	public boolean isStandardDescriptor(String key) throws RemoteException {
        throw exception;
	}

}
