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
package org.apache.jackrabbit.rmi.server.iterator;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;

import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerObject;


/**
 * Remote adapter for the JCR {@link RangeIterator} interface. This
 * class makes a local iterator available as an RMI service using the
 * {@link RemoteIterator} interface.
 */
public abstract class ServerIterator extends ServerObject
        implements RemoteIterator {

    /** The adapted local iterator. */
    private final RangeIterator iterator;

    /** The maximum number of elements to send per request. */
    private final int maxBufferSize;

    /**
     * The cached number of elements in the iterator, -1 if the iterator
     * size is unknown, or -2 if the size has not been retrieved from the
     * adapted local iterator. This variable is useful in cases when the
     * underlying iterator does not know its sizes (getSize() returns -1)
     * but we reach the end of the iterator in a nextObjects() call and
     * can thus determine the size of the iterator.
     */
    private long size;

    /**
     * Creates a remote adapter for the given local item.
     *
     * @param iterator      local iterator to be adapted
     * @param factory       remote adapter factory
     * @param maxBufferSize maximum buffer size
     * @throws RemoteException on RMI errors
     */
    public ServerIterator(
            RangeIterator iterator, RemoteAdapterFactory factory,
            int maxBufferSize) throws RemoteException {
        super(factory);
        this.iterator = iterator;
        this.maxBufferSize = maxBufferSize;
        this.size = -2;
    }

    /**
     * Returns the size of the iterator. The size is cached by invoking the
     * adapted local iterator when this method is first called or by
     * determining the size from an end-of-iterator condition in nextObjects().
     *
     * @return size of the iterator
     * @throws RemoteException on RMI errors
     */
    @Override
    public long getSize() throws RemoteException {
        if (size == -2) {
            size = iterator.getSize();
        }
        return size;
    }

    /**
     * Skips the given number of elements.
     *
     * @param items number of elements to skip
     * @throws NoSuchElementException if skipped past the last element
     * @throws RemoteException on RMI errors
     */
    @Override
    public void skip(long items)
            throws NoSuchElementException, RemoteException {
        try {
            iterator.skip(items);
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException(e.getMessage());
        }
    }

    /**
     * Returns a remote adapter for the given local object. This abstract
     * method is used by {@link #nextObjects()} to convert the local
     * objects to remote references to be sent to the client.
     * <p>
     * Subclasses should implement this method to use the remote adapter
     * factory to create remote adapters of the specific element type.
     *
     * @param object local object
     * @return remote adapter
     * @throws RemoteException on RMI errors
     */
    protected abstract Object getRemoteObject(Object object)
            throws RemoteException;

    /**
     * Returns an array of remote references to the next elements in this
     * iteration.
     *
     * @return array of remote references, or <code>null</code>
     * @throws RemoteException on RMI errors
     */
    @Override
    public Object[] nextObjects() throws RemoteException {
        ArrayList items = new ArrayList();
        while (items.size() < maxBufferSize && iterator.hasNext()) {
            items.add(getRemoteObject(iterator.next()));
        }
        if (items.size() > 0) {
            return items.toArray(new Object[items.size()]);
        } else {
            size = iterator.getPosition();
            return null;
        }
    }

}
