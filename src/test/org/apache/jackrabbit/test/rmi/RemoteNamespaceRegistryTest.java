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

import javax.jcr.NamespaceRegistry;

import junit.framework.TestCase;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientNamespaceRegistry;
import org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerNamespaceRegistry;
import org.easymock.MockControl;

public class RemoteNamespaceRegistryTest extends TestCase {

    private MockControl control;

    private NamespaceRegistry mock;
    
    private NamespaceRegistry registry;
    
    protected void setUp() throws Exception {
        control = MockControl.createControl(NamespaceRegistry.class);
        mock = (NamespaceRegistry) control.getMock();
        
        RemoteNamespaceRegistry remote =
            new ServerNamespaceRegistry(mock, new ServerAdapterFactory());
        registry =
            new ClientNamespaceRegistry(remote, new ClientAdapterFactory());
    }
    
    public void testRemoteMethods() throws Exception {
        RemoteTestHelper helper = new RemoteTestHelper(NamespaceRegistry.class);
        helper.testRemoteMethods(registry, mock, control);
    }

}
