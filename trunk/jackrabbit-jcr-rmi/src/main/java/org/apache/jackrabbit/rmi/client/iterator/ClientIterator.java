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

import java.rmi.RemoteException;
import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;

import org.apache.jackrabbit.rmi.client.ClientObject;
import org.apache.jackrabbit.rmi.client.LocalAdapterFactory;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.apache.jackrabbit.rmi.remote.RemoteIterator;

/**
 * A buffering local adapter for the JCR-RMI {@link RemoteIterator}
 * interface. This class makes the remote iterator locally available
 * using the JCR {@link RangeIterator} interface. The element arrays
 * returned by the remote iterator are buffered locally.
 * <p>
 * See the subclasses for type-specific versions of this abstract class.
 */
public abstract class ClientIterator extends ClientObject
        implements RangeIterator {

    /** The adapted remote iterator. */
    private final RemoteIterator remote;

    /**
     * The cached number of elements in the iterator, -1 if the iterator
     * size is unknown, or -2 if the size has not been retrieved from the
     * remote iterator.
     */
    private long size;

    /** The position of the buffer within the iteration. */
    private long positionOfBuffer;

    /** The position within the buffer of the iteration. */
    private int positionInBuffer;

    /**
     * The element buffer. Set to <code>null</code> when the end of the
     * iteration has been reached.
     */
    private Object[] buffer;

    /**
     * Creates a local adapter for the given remote iterator. The element
     * buffer is initially empty.
     *
     * @param remote        remote iterator
     * @param factory       local adapter factory
     */
    public ClientIterator(RemoteIterator remote, LocalAdapterFactory factory) {
        super(factory);
        this.remote = remote;
        this.size = -2;
        this.positionOfBuffer = 0;
        this.positionInBuffer = 0;
        this.buffer = new Object[0];
    }

    /**
     * Returns the current position within the iterator.
     *
     * @return current position
     * @see RangeIterator#getPosition()
     */
    public long getPosition() {
        return positionOfBuffer + positionInBuffer;
    }

    /**
     * Returns the size (the total number of elements) of this iteration.
     * Returns <code>-1</code> if the size is unknown.
     * <p>
     * To minimize the number of remote method calls, the size is retrieved
     * when this method is first called and cached for subsequent invocations.
     *
     * @return number of elements in the iteration, or <code>-1</code>
     * @throws RemoteRuntimeException on RMI errors
     * @see RangeIterator#getSize()
     */
    public long getSize() throws RemoteRuntimeException {
        if (size == -2) {
            try {
                size = remote.getSize();
            } catch (RemoteException e) {
                throw new RemoteRuntimeException(e);
            }
        }
        return size;
    }

    /**
     * Skips the given number of elements in this iteration.
     * <p>
     * The elements in the local element buffer are skipped first, and
     * a remote skip method call is made only if more elements are being
     * skipped than remain in the local buffer.
     *
     * @param skipNum the number of elements to skip
     * @throws NoSuchElementException if skipped past the last element
     * @throws RemoteRuntimeException on RMI errors
     * @see RangeIterator#skip(long)
     */
    public void skip(long skipNum)
            throws NoSuchElementException, RemoteRuntimeException {
        if (skipNum < 0) {
            throw new IllegalArgumentException("Negative skip is not allowed");
        } else if (buffer == null && skipNum > 0) {
            throw new NoSuchElementException("Skipped past the last element");
        } else if (positionInBuffer + skipNum <= buffer.length) {
            positionInBuffer += skipNum;
        } else {
            try {
                skipNum -= buffer.length - positionInBuffer;
                remote.skip(skipNum);
                positionInBuffer = buffer.length;
                positionOfBuffer += skipNum;
            } catch (RemoteException e) {
                throw new RemoteRuntimeException(e);
            } catch (NoSuchElementException e) {
                buffer = null; // End of iterator reached
                throw e;
            }
        }
    }

    /**
     * Advances the element buffer if there are no more elements in it. The
     * element buffer is set to <code>null</code> if the end of the iteration
     * has been reached.
     *
     * @throws RemoteException on RMI errors
     */
    private void advance() throws RemoteException {
        if (buffer != null && positionInBuffer == buffer.length) {
            positionOfBuffer += buffer.length;
            positionInBuffer = 0;
            buffer = remote.nextObjects();
            if (buffer == null) {
                size = positionOfBuffer;
            }
        }
    }

    /**
     * Checks if there are more elements in this iteration.
     *
     * @return <code>true</code> if there are more elements,
     *         <code>false</code> otherwise
     * @throws RemoteRuntimeException on RMI errors
     */
    public boolean hasNext() throws RemoteRuntimeException {
        try {
            advance();
            return buffer != null;
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

    /**
     * Returns a local adapter for the given remote object. This abstract
     * method is used by {@link #next()} to convert the remote references
     * returned by the remote iterator to local adapters.
     * <p>
     * Subclasses should implement this method to use the local adapter
     * factory to create local adapters of the specific element type.
     *
     * @param remote remote object
     * @return local adapter
     */
    protected abstract Object getObject(Object remote);

    /**
     * Returns the next element in this iteration.
     *
     * @return next element
     * @throws NoSuchElementException if there are no more elements
     * @throws RemoteRuntimeException on RMI errors
     */
    public Object next() throws NoSuchElementException, RemoteRuntimeException {
        try {
            advance();
            if (buffer == null) {
                throw new NoSuchElementException("End of iterator reached");
            } else {
                return getObject(buffer[positionInBuffer++]);
            }
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

    /**
     * Not supported.
     *
     * @throws UnsupportedOperationException always thrown
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
