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

import javax.jcr.Workspace;

import junit.framework.TestCase;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientWorkspace;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerWorkspace;
import org.easymock.MockControl;

public class RemoteWorkspaceTest extends TestCase {

    private MockControl control;

    private Workspace mock;
    
    private Workspace workspace;
    
    protected void setUp() throws Exception {
        control = MockControl.createControl(Workspace.class);
        mock = (Workspace) control.getMock();
        
        RemoteWorkspace remote = 
            new ServerWorkspace(mock, new ServerAdapterFactory());
        workspace = new ClientWorkspace(null, remote, new ClientAdapterFactory());
    }
    
    public void testRemoteMethods() throws Exception {
        RemoteTestHelper helper = new RemoteTestHelper(Workspace.class);
        helper.ignoreMethod("getQueryManager");         // TODO
        helper.ignoreMethod("getObservationManager");   // TODO
        helper.ignoreMethod("restore");                 // TODO
        helper.ignoreMethod("getSession");              // implemented locally
        helper.ignoreMethod("copy");                    // multiple methods
        helper.ignoreMethod("importXML");               // wrapped stream
        helper.ignoreMethod("getImportContentHandler"); // implemented locally
        helper.testRemoteMethods(workspace, mock, control);
    }

}
