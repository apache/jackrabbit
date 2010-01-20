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
package org.apache.jackrabbit.rmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.server.RemoteObject;
import java.util.Properties;

import javax.jcr.Repository;

import org.apache.jackrabbit.core.JackrabbitRepositoryStub;
import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.test.RepositoryStubException;

public class RepositoryStubImpl extends JackrabbitRepositoryStub {

    private RemoteRepository remote;

    private Repository repository;

    public RepositoryStubImpl(Properties env) {
        super(env);
    }

    @Override
    public synchronized Repository getRepository()
            throws RepositoryStubException {
        if (repository == null) {
            try {
                RemoteAdapterFactory raf = new ServerAdapterFactory();
                remote = raf.getRemoteRepository(super.getRepository());

                // Make sure that the remote reference survives serialization
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(buffer);
                oos.writeObject(RemoteObject.toStub(remote));
                oos.close();

                ObjectInputStream ois = new ObjectInputStream(
                        new ByteArrayInputStream(buffer.toByteArray()));
                LocalAdapterFactory laf = new ClientAdapterFactory();
                repository =
                    laf.getRepository((RemoteRepository) ois.readObject());
            } catch (Exception e) {
                throw new RepositoryStubException(e.getMessage());
            }
        }
        return repository;
    }

}
