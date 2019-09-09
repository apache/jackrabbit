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

import javax.jcr.RepositoryException;
import org.apache.jackrabbit.api.JackrabbitValue;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.value.BinaryValue;

/**
 * Represents a binary value that is backed by a blob file value.
 */
class BinaryValueImpl extends BinaryValue implements JackrabbitValue {

    private final BLOBFileValue blob;

    /**
     * Construct a new object from the given blob.
     *
     * @param blob the blob
     */
    BinaryValueImpl(BLOBFileValue blob) throws RepositoryException {
        super(blob);
        this.blob = blob;
    }

    public String getContentIdentity() {
        DataIdentifier id = blob.getDataIdentifier();
        return id == null ? null : id.toString();
    }

    /**
     * Get the data identifier if one is available.
     *
     * @return the data identifier or null
     */
    DataIdentifier getDataIdentifier() {
        return blob.getDataIdentifier();
    }

    boolean usesDataStore(DataStore s) {
        return blob.usesDataStore(s);
    }

}
