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
package org.apache.jackrabbit.rmi.jackrabbit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.rmi.client.ClientWorkspace;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.xml.sax.InputSource;

public class ClientJackrabbitWorkspace extends ClientWorkspace
        implements JackrabbitWorkspace {

    private final RemoteJackrabbitWorkspace remote;

    public ClientJackrabbitWorkspace(
            Session session, RemoteJackrabbitWorkspace remote,
            LocalAdapterFactory factory) {
        super(session, remote, factory);
        this.remote = remote;
    }

    public void createWorkspace(String name) throws RepositoryException {
        try {
            remote.createWorkspace(name, null);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public void createWorkspace(String name, InputSource template)
            throws RepositoryException {
        try {
            InputStream input = template.getByteStream();
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1000];
                int n = input.read(buffer);
                while (n != -1) {
                    output.write(buffer, 0, n);
                    n = input.read(buffer);
                }
                remote.createWorkspace(name, output.toByteArray());
            } finally {
                input.close();
            }
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        } catch (IOException e) {
            throw new RepositoryException("Error reading workspace template", e);
        }
    }

}
