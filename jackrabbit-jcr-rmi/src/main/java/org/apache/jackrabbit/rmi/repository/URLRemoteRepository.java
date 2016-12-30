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

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.jackrabbit.rmi.client.ClientAdapterFactory;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;

/**
 * Proxy for a remote repository accessed via a URL. The configured URL is
 * dereferenced lazily during each method call. Thus the resource pointed to
 * by the URL does not need to exist when this class is instantiated. The
 * resource can also be replaced with another remote repository instance
 * during the lifetime of an instance of this class.
 *
 * @since 1.4
 */
public class URLRemoteRepository extends ProxyRepository {

    /**
     * Creates a proxy for the remote repository at the given URL.
     *
     * @param factory local adapter factory
     * @param url URL of the remote repository
     */
    public URLRemoteRepository(LocalAdapterFactory factory, URL url) {
        super(new URLRemoteRepositoryFactory(factory, url));
    }

    /**
     * Creates a proxy for the remote repository at the given URL.
     * Uses {@link ClientAdapterFactory} as the default
     * local adapter factory.
     *
     * @param url URL of the remote repository
     */
    public URLRemoteRepository(URL url) {
        this(new ClientAdapterFactory(), url);
    }

    /**
     * Creates a proxy for the remote repository at the given URL.
     * Uses {@link ClientAdapterFactory} as the default
     * local adapter factory.
     *
     * @param url URL of the remote repository
     * @throws MalformedURLException if the given URL is malformed
     */
    public URLRemoteRepository(String url) throws MalformedURLException {
        this(new URL(url));
    }

}
