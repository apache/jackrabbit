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
package org.apache.jackrabbit.core.value;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Binary;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStore;

/**
 * Represents binary data which is backed by a resource or byte[].
 * Unlike <code>BinaryValue</code> it has no state, i.e.
 * the <code>getStream()</code> method always returns a fresh
 * <code>InputStream</code> instance.
 * <p>
 * <b>Important Note:</b>
 * <p>
 * This interface is for Jackrabbit-internal use only. Applications should
 * use <code>javax.jcr.ValueFactory</code> to create binary values.
 */
abstract class BLOBFileValue implements Binary {

    /**
     * Deletes the persistent resource backing this <code>BLOBFileValue</code>.
     *
     * @param pruneEmptyParentDirs if <code>true</code>, empty parent directories
     *                             will automatically be deleted
     */
    abstract void delete(boolean pruneEmptyParentDirs);

    /**
     * Returns a copy of this BLOB file value. The returned copy may also be
     * this object. However an implementation must guarantee that the returned
     * value has state that is independent from this value. Immutable values
     * can savely return the same value (this object).
     * <p>
     * Specifically, {@link #dispose()} on the returned value must not have an
     * effect on this value!
     *
     * @return a value that can be used independently from this value.
     * @throws RepositoryException if an error occur while copying this value.
     */
    abstract BLOBFileValue copy() throws RepositoryException;

    public abstract boolean equals(Object obj);

    public abstract String toString();

    public abstract int hashCode();

    /**
     * Get the data identifier if one is available.
     *
     * @return the data identifier or null
     */
    DataIdentifier getDataIdentifier() {
        return null;
    }

    //-----------------------------------------------------< javax.jcr.Binary >

    public int read(byte[] b, long position) throws IOException, RepositoryException {
        InputStream in = getStream();
        try {
            long skip = position;
            while (skip > 0) {
                long skipped = in.skip(skip);
                if (skipped <= 0) {
                    return -1;
                }
                skip -= skipped;
            }
            return in.read(b);
        } finally {
            in.close();
        }
    }

    /**
     * Check if this blob uses the given data store.
     *
     * @param s the other data store
     * @return true if it does
     */
    boolean usesDataStore(DataStore s) {
        return false;
    }

}
