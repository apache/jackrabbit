/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.rmi;

import javax.jcr.Repository;

import junit.framework.TestCase;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientRepository;
import org.apache.jackrabbit.rmi.remote.RemoteRepository;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerRepository;
import org.easymock.MockControl;

public class RemoteRepositoryTest extends TestCase {

    private MockControl control;

    private Repository mock;
    
    private Repository repository;
    
    protected void setUp() throws Exception {
        control = MockControl.createControl(Repository.class);
        mock = (Repository) control.getMock();
        
        RemoteRepository remote =
            new ServerRepository(mock, new ServerAdapterFactory());
        repository = new ClientRepository(remote, new ClientAdapterFactory());
    }
    
    public void testRemoteMethods() throws Exception {
        RemoteTestHelper helper = new RemoteTestHelper(Repository.class);
        helper.testRemoteMethods(repository, mock, control);
    }

}
