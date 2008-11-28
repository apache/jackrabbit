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

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.jsr283.RepositoryFactory;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;

/**
 * <code>RepositoryFactoryImpl</code> implements a JSR 283 repository factory
 * that connects to a repository that is exposed through RMI.
 * <p/>
 * This implementation does not support the notion of a default repository.
 */
public class RepositoryFactoryImpl implements RepositoryFactory {

    /**
     * The name of the rmi url property.
     */
    public static final String REPOSITORY_RMI_URL
            = "org.apache.jackrabbit.repository.rmi.url";

    /**
     * The name of the local adapter factory property.
     */
    public static final String REPOSITORY_RMI_LOCAL_ADAPTER_FACTORY
            = "org.apache.jackrabbit.repository.rmi.local.adapter.factory";

    public Repository getRepository(Map parameters) throws RepositoryException {
        if (parameters == null) {
            // this implementation does not support a default repository
            return null;
        }

        String url = (String) parameters.get(REPOSITORY_RMI_URL);
        if (url == null) {
            // don't know how to handle
            return null;
        }

        LocalAdapterFactory factory = createLocalAdapterFactory(parameters);
        if (factory == null) {
            return new RMIRemoteRepository(url);
        } else {
            return new RMIRemoteRepository(factory, url);
        }
    }

    private LocalAdapterFactory createLocalAdapterFactory(Map params) {
        String className = (String) params.get(REPOSITORY_RMI_LOCAL_ADAPTER_FACTORY);
        if (className != null) {
            try {
                return (LocalAdapterFactory) Class.forName(className).newInstance();
            } catch (Exception e) {
                // return null and use default
            }
        }
        return null;
    }
}
