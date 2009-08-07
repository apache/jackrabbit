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
package org.apache.jackrabbit.core.data.db;

import java.util.ArrayList;

import javax.jcr.RepositoryException;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

/**
 * Implementation of a simple ConnectionRecoveryManager pool.
 * The maximum number of pooled objects can be set, and if more objects
 * are requested the pool waits until one object is put back.
 */
public class Pool {
    protected final int maxSize;
    protected final ArrayList all = new ArrayList();
    protected final DbDataStore factory;
    protected final LinkedQueue pool = new LinkedQueue();

    /**
     * Create a new pool using the given factory and maximum pool size.
     *
     * @param factory the db data store
     * @param maxSize the maximum number of objects in the pool.
     */
    protected Pool(DbDataStore factory, int maxSize) {
        this.factory = factory;
        this.maxSize = Math.max(1, maxSize);
    }

    /**
     * Get a connection from the pool. This method may open a new connection if
     * required, or if the maximum number of connections are opened, it will
     * wait for one to be returned.
     *
     * @return the connection
     */
    protected Object get() throws InterruptedException, RepositoryException {
        Object o = pool.poll(0);
        if (o == null) {
            synchronized (all) {
                if (all.size() < maxSize) {
                    o = factory.createNewConnection();
                    all.add(o);
                }
            }
            if (o == null) {
                o = pool.take();
            }
        }
        return o;
    }

    /**
     * But a connection back into the pool.
     *
     * @param o the connection
     */
    protected void add(Object o) throws InterruptedException {
        pool.put(o);
    }

    /**
     * Get all connections (even if they are currently being used).
     *
     * @return all connections
     */
    protected ArrayList getAll() {
        return all;
    }
}
