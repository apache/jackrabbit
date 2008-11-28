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
package org.apache.jackrabbit.core.jndi;

import java.util.Map;
import java.util.Hashtable;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.naming.InitialContext;

import org.apache.jackrabbit.api.jsr283.RepositoryFactory;

/**
 * <code>RepositoryFactoryImpl</code> implements a repository factory that
 * obtains a repository instance through JNDI.
 * <p/>
 * This implementation does not support the notion of a default repository.
 */
public class RepositoryFactoryImpl implements RepositoryFactory {

    /**
     * The name of the JNDI name property.
     */
    public static final String REPOSITORY_JNDI_NAME
            = "org.apache.jackrabbit.repository.jndi.name";

    public Repository getRepository(Map parameters) throws RepositoryException {
        if (parameters == null) {
            // this implementation does not support a default repository
            return null;
        }

        String name = (String) parameters.get(REPOSITORY_JNDI_NAME);
        if (name == null) {
            // don't know how to handle
            return null;
        }

        try {
            Hashtable environment = new Hashtable(parameters);
            InitialContext context = new InitialContext(environment);
            return (Repository) context.lookup(name);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }
}
