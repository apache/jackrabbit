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
package org.apache.jackrabbit.rmi.repository;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;

/**
 * Proxy for a remote repository bound in RMI. The configured repository is
 * looked up from RMI lazily during each method call. Thus the RMI entry
 * does not need to exist when this class is instantiated. The RMI entry
 * can also be replaced with another repository during the lifetime of an
 * instance of this class.
 *
 * @since 1.4
 */
public class RMIRemoteRepository extends ProxyRepository {

    /**
     * Creates a proxy for the remote repository in the given RMI URL.
     *
     * @param factory local adapter factory
     * @param url RMI URL of the remote repository
     */
    public RMIRemoteRepository(LocalAdapterFactory factory, String url) {
        super(new RMIRemoteRepositoryFactory(factory, url));
    }

    /**
     * Creates a proxy for the remote repository in the given RMI URL.
     * Uses {@link ClientAdapterFactory} as the default
     * local adapter factory.
     *
     * @param url URL of the remote repository
     */
    public RMIRemoteRepository(String url) {
        this(new ClientAdapterFactory(), url);
    }

}
