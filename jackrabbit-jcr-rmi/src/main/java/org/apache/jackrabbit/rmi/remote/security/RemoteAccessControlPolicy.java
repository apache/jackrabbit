/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.rmi.remote.security;

import java.rmi.Remote;

/**
 * Remote version of the JCR {@link javax.jcr.security.AccessControlPolicy
 * AccessControlPolicy} interface. Used by the
 * {@link org.apache.jackrabbit.rmi.server.security.ServerAccessControlPolicy
 * ServerAccessControlPolicy} and
 * {@link org.apache.jackrabbit.rmi.client.security.ClientAccessControlPolicy
 * ClientAccessControlPolicy} adapter base classes to provide transparent RMI
 * access to remote item definitions.
 * <p>
 * The methods in this interface are documented only with a reference to a
 * corresponding AccessControlPolicy method. The remote object will simply
 * forward the method call to the underlying AccessControlPolicy instance.
 * Argument and return values, as well as possible exceptions, are copied over
 * the network. Complex return values are returned as remote references to the
 * corresponding remote interface. RMI errors are signaled with
 * RemoteExceptions.
 *
 * @see javax.jcr.security.AccessControlPolicy
 * @see org.apache.jackrabbit.rmi.client.security.ClientAccessControlPolicy
 * @see org.apache.jackrabbit.rmi.server.security.ServerAccessControlPolicy
 */
public interface RemoteAccessControlPolicy extends Remote {

}
