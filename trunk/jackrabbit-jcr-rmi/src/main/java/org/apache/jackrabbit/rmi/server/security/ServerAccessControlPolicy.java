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
package org.apache.jackrabbit.rmi.server.security;

import java.rmi.RemoteException;

import javax.jcr.security.AccessControlPolicy;

import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlPolicy;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerObject;

public class ServerAccessControlPolicy extends ServerObject implements
        RemoteAccessControlPolicy {

    private final AccessControlPolicy acp;

    public ServerAccessControlPolicy(final AccessControlPolicy acp,
            final RemoteAdapterFactory factory)

    throws RemoteException {
        super(factory);
        this.acp = acp;
    }

    AccessControlPolicy getAccessControlPolicy() {
        return acp;
    }

}
