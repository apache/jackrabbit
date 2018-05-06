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

import org.apache.jackrabbit.api.ReferenceBinary;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import javax.jcr.RepositoryException;

/**
 * Represents binary data which is stored in the data store.
 */
class BLOBInDataStore extends BLOBFileValue implements ReferenceBinary {

    private final DataStore store;
    private final DataIdentifier identifier;

    /**
     * the prefix of the string representation of this value
     */
    private static final String PREFIX = "dataStore:";

    /**
     * The default logger
     */
    private static Logger log = LoggerFactory.getLogger(BLOBInDataStore.class);

    /** the audit logger */
    private static Logger auditLogger = LoggerFactory.getLogger("org.apache.jackrabbit.core.audit");

    private BLOBInDataStore(DataStore store, DataIdentifier identifier) {
        assert store != null;
        assert identifier != null;
        this.store = store;
        this.identifier = identifier;
    }

    void delete(boolean pruneEmptyParentDirs) {
        // do nothing
    }

    public void dispose() {
        // do nothing
    }

    DataIdentifier getDataIdentifier() {
        return identifier;
    }

    BLOBFileValue copy() throws RepositoryException {
        return this;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BLOBInDataStore) || obj == null) {
            return false;
        }
        BLOBInDataStore other = (BLOBInDataStore) obj;
        return store == other.store && identifier.equals(other.identifier);
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

    public long getSize() {
        try {
            return getDataRecord().getLength();
        } catch (DataStoreException e) {
            log.warn("getSize for " + identifier + " failed", e);
            return -1;
        }
    }

    public InputStream getStream() throws RepositoryException {
        return getDataRecord().getStream();
    }

    @Override
    public String getReference() {
        try {
            return getDataRecord().getReference();
        } catch (DataStoreException e) {
            log.warn("Unable to access the reference to " + identifier, e);
            return null;
        }
    }

    public String toString() {
        return PREFIX + identifier;
    }

    static BLOBInDataStore getInstance(DataStore store, String s) {
        String id = s.substring(PREFIX.length());
        DataIdentifier identifier = new DataIdentifier(id);
        return new BLOBInDataStore(store, identifier);
    }

    static BLOBInDataStore getInstance(DataStore store, DataIdentifier identifier) {
        return new BLOBInDataStore(store, identifier);
    }

    static BLOBInDataStore getInstance(DataStore store, InputStream in) throws DataStoreException {
        DataRecord rec = store.addRecord(in);
        DataIdentifier identifier = rec.getIdentifier();
        if (auditLogger.isDebugEnabled()) {
            auditLogger.debug("{} ({})", identifier, rec.getLength());
        }
        return new BLOBInDataStore(store, identifier);
    }

    /**
     * Checks if String can be converted to an instance of this class.
     * @param s
     * @return true if it can be converted
     */
    static boolean isInstance(String s) {
        return s.startsWith(PREFIX);
    }

    private DataRecord getDataRecord() throws DataStoreException {
        // may not keep the record, otherwise garbage collection doesn't work
        return store.getRecord(identifier);
    }

    boolean usesDataStore(DataStore s) {
        return store == s;
    }

}
