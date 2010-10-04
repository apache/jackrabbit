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
package org.apache.jackrabbit.core.persistence.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.util.StringIndex;

/**
 * This Class implements efficient serialization methods for item states.
 */
public class BundleBinding {

    static final int BINARY_IN_BLOB_STORE = -1;

    static final int BINARY_IN_DATA_STORE = -2;

    /**
     * serialization version 1
     */
    static final int VERSION_1 = 1;

    /**
     * serialization version 2
     */
    static final int VERSION_2 = 2;

    /**
     * current version
     */
    static final int VERSION_CURRENT = VERSION_2;

    /**
     * the namespace index
     */
    protected final StringIndex nsIndex;

    /**
     * the name index
     */
    protected final StringIndex nameIndex;

    /**
     * the blob store
     */
    protected final BLOBStore blobStore;

    /**
     * minimum size of binaries to store in blob store
     */
    protected long minBlobSize = 0x4000; // 16k

    /**
     * the error handling
     */
    protected final ErrorHandling errorHandling;

    /**
     * Data store for binary properties.
     */
    protected final DataStore dataStore;

    /**
     * Creates a new bundle binding
     *
     * @param errorHandling the error handling
     * @param blobStore the blobstore for retrieving blobs
     * @param nsIndex the namespace index
     * @param nameIndex the name index
     * @param dataStore the data store
     */
    public BundleBinding(
            ErrorHandling errorHandling, BLOBStore blobStore,
            StringIndex nsIndex, StringIndex nameIndex, DataStore dataStore) {
        this.errorHandling = errorHandling;
        this.nsIndex = nsIndex;
        this.nameIndex = nameIndex;
        this.blobStore = blobStore;
        this.dataStore = dataStore;
    }

    /**
     * Returns the minimum blob size
     * @see #setMinBlobSize(long) for details.
     * @return the minimum blob size
     */
    public long getMinBlobSize() {
        return minBlobSize;
    }

    /**
     * Sets the minimum blob size. Binary values that are shorted than this given
     * size will be inlined in the serialization stream, binary value that are
     * longer, will be stored in the blob store. default is 4k.
     *
     * @param minBlobSize the minimum blob size.
     */
    public void setMinBlobSize(long minBlobSize) {
        this.minBlobSize = minBlobSize;
    }

    /**
     * Returns the blob store that is assosiated with this binding.
     * @return the blob store
     */
    public BLOBStore getBlobStore() {
        return blobStore;
    }

    /**
     * Deserializes a <code>NodePropBundle</code> from a data input stream.
     *
     * @param in the input stream
     * @param id the node id for the new bundle
     * @return the bundle
     * @throws IOException if an I/O error occurs.
     */
    public NodePropBundle readBundle(InputStream in, NodeId id)
            throws IOException {
        return new BundleReader(this, in).readBundle(id);
    }

    /**
     * Checks a <code>NodePropBundle</code> from a data input stream.
     *
     * @param in the input stream
     * @return <code>true</code> if the data is valid;
     *         <code>false</code> otherwise.
     */
    public boolean checkBundle(InputStream in) {
        return new BundleReader(this, in).checkBundle();
    }

    /**
     * Serializes a <code>NodePropBundle</code> to a data output stream
     *
     * @param out the output stream
     * @param bundle the bundle to serialize
     * @throws IOException if an I/O error occurs.
     */
    public void writeBundle(OutputStream out, NodePropBundle bundle)
            throws IOException {
        new BundleWriter(this, out).writeBundle(bundle);
    }

}
