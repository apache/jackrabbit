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

/**
 * JCR-RMI remote runtime exception. Used by the JCR-RMI client to wrap
 * RMI errors into RuntimeExceptions to avoid breaking the JCR interfaces.
 * <p>
 * Note that if a RemoteException is received by call that declares to
 * throw RepositoryExceptions, then the RemoteException is wrapped into
 * a RemoteRepositoryException.
 */
public class RemoteRuntimeException extends RuntimeException {

    /**
     * Creates a RemoteRuntimeException based on the given RemoteException.
     *
     * @param ex the remote exception
     */
    public RemoteRuntimeException(RemoteException ex) {
        super(ex);
    }

}
