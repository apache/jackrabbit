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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.NoSuchElementException;

/**
 * Remote version of the JCR {@link javax.jcr.RangeIterator} interface.
 * Used by the {@link org.apache.jackrabbit.rmi.server.iterator.ServerIterator} and
 * {@link org.apache.jackrabbit.rmi.client.iterator.ClientIterator} classes to
 * provide transparent RMI access to remote iterators.
 * <p>
 * This interface allows both the client and server side to control the
 * amount of buffering used to increase performance.
 */
public interface RemoteIterator extends Remote {

    /**
     * Returns the size of the iteration, or <code>-1</code> if the
     * size is unknown.
     *
     * @return size of the iteration, or <code>-1</code> if unknown
     * @throws RemoteException on RMI errors
     * @see javax.jcr.RangeIterator#getSize()
     */
    long getSize() throws RemoteException;

    /**
     * Skips the given number of elements in this iteration.
     *
     * @param items number of elements to skip
     * @throws NoSuchElementException if skipped past the last element
     * @throws RemoteException on RMI errors
     * @see javax.jcr.RangeIterator#skip(long)
     */
    void skip(long items) throws NoSuchElementException, RemoteException;

    /**
     * Returns an array of remote references to the next elements in this
     * iterator. Returns <code>null</code> if the end of this iteration has
     * been reached.
     * <p>
     * To reduce the amount of remote method calls, this method returns an
     * array of one or more elements in this iteration.
     *
     * @return array of remote references, or <code>null</code>
     * @throws IllegalArgumentException if <code>maxItems</code> is not positive
     * @throws RemoteException on RMI errors
     * @see java.util.Iterator#next()
     */
    Object[] nextObjects() throws IllegalArgumentException, RemoteException;

}
