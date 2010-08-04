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

import org.apache.commons.collections.map.ReferenceMap;

import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;
import java.util.Map;

/**
 * <code>BindableRepositoryFactory</code> is an object factory that when given
 * a reference for a <code>BindableRepository</code> object, will create an
 * instance of the corresponding  <code>BindableRepository</code>.
 */
public class BindableRepositoryFactory implements ObjectFactory {

    /**
     * cache using <code>java.naming.Reference</code> objects as keys and
     * storing soft references to <code>BindableRepository</code> instances
     */
    private static final Map cache = new ReferenceMap();

    /**
     * {@inheritDoc}
     */
    public synchronized Object getObjectInstance(
            Object obj, Name name, Context nameCtx, Hashtable environment)
            throws RepositoryException {
        synchronized (cache) {
            Object instance = cache.get(obj);
            if (instance == null && obj instanceof Reference) {
                instance = new BindableRepository((Reference) obj);
                cache.put(obj, instance);
            }
            return instance;
        }
    }

}
