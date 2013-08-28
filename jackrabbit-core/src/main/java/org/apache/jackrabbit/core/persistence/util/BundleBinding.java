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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.util.StringIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class implements efficient serialization methods for item states.
 */
public class BundleBinding {

    private static boolean VERIFY_BUNDLES = Boolean.getBoolean("jackrabbit.verifyBundles");

    private static boolean ALLOW_BROKEN_BUNDLES = Boolean.getBoolean("jackrabbit.allowBrokenBundleWrites");
    
    private static Logger log = LoggerFactory.getLogger(BundleBinding.class);
    
    static {
        if (VERIFY_BUNDLES) {
            log.warn("Please note reading and writing bundles is slightly slower " + 
                    "because the system property \"jackrabbit.verifyBundles\" is enabled. " + 
                    "See JCR-3652 (patch JCR-3652-b.patch is applied).");
        }
    }

    static final int BINARY_IN_BLOB_STORE = -1;

    static final int BINARY_IN_DATA_STORE = -2;

    /**
     * A special UUID used to mark the <code>null</code> parent identifier
     * of the root node. This is a proper type 1 UUID to prevent collisions
     * with other identifiers, even special non-random ones like
     * <code>00000000-0000-0000-0000-000000000000</code>.
     */
    static final NodeId NULL_PARENT_ID =
        new NodeId("bb4e9d10-d857-11df-937b-0800200c9a66");

    /**
     * serialization version 1
     */
    static final int VERSION_1 = 1;

    /**
     * serialization version 2
     */
    static final int VERSION_2 = 2;

    /**
     * serialization version 3
     */
    static final int VERSION_3 = 3;

    /**
     * current version
     */
    static final int VERSION_CURRENT = VERSION_3;

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
     * Returns the blob store that is associated with this binding.
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
        if (VERIFY_BUNDLES) {
            return new BundleReaderSlower(this, in).readBundle(id);
        }
        return new BundleReader(this, in).readBundle(id);
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
        if (VERIFY_BUNDLES) {
            writeBundleSlower(out, bundle);
            return;
        }
        new BundleWriter(this, out).writeBundle(bundle);
    }
    
    private void writeBundleSlower(OutputStream out, NodePropBundle bundle)
            throws IOException {
        byte[] data;
        IOException lastError = null;
        for (int i = 0; i < 5; i++) {
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            if (i < 3) {
                new BundleWriter(this, buff).writeBundle(bundle);
            } else {
                log.warn("Corrupt bundle: writing slower, i = " + i);
                new BundleWriterSlower(this, buff).writeBundle(bundle);
            }
            data = buff.toByteArray();
            NodeId id = bundle.getId();
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(data);
                new BundleReaderSlower(this, in).readBundleNormal(id);            
            } catch (IOException e) {
                lastError = e;
                log.warn("Corrupt bundle: could not read the bundle I wrote", e);
                ByteArrayInputStream in = new ByteArrayInputStream(data);
                try {
                    new BundleReader(this, in).readBundle(id);            
                    log.warn("Corrupt bundle: can fix error while reading");
                    if (i == 4 && ALLOW_BROKEN_BUNDLES) {
                        log.warn("Corrupt bundle: writing broken one");
                        break;
                    }
                } catch (IOException e2) {
                    lastError = e2;
                    log.warn("Corrupt bundle: could not read even when trying harder", e2);
                }
                continue;
            }
            out.write(data);
            return;
        }
        String msg = "Corrupt bundle: could not write a correct bundle, giving up: " + bundle;
        log.error(msg);
        throw new IOException(msg, lastError);
    }

}
