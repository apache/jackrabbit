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
package org.apache.jackrabbit.rmi.client.iterator;

import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteRow;

/**
 * A ClientIterator for iterating remote rows.
 */
public class ClientRowIterator extends ClientIterator implements RowIterator {

    /**
     * Creates a ClientRowIterator instance.
     *
     * @param iterator      remote iterator
     * @param factory       local adapter factory
     */
    public ClientRowIterator(
            RemoteIterator iterator, LocalAdapterFactory factory) {
        super(iterator, factory);
    }

    /**
     * Creates and returns a local adapter for the given remote row.
     *
     * @param remote remote reference
     * @return local adapter
     * @see ClientIterator#getObject(Object)
     */
    protected Object getObject(Object remote) {
        return getFactory().getRow((RemoteRow) remote);
    }

    /**
     * Returns the next row in this iteration.
     *
     * @return next row
     * @see RowIterator#nextRow()
     */
    public Row nextRow() {
        return (Row) next();
    }

}
