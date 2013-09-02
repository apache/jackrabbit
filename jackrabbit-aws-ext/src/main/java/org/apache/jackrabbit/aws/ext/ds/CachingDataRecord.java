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

package org.apache.jackrabbit.aws.ext.ds;

import java.io.InputStream;
import org.apache.jackrabbit.core.data.AbstractDataRecord;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;

/**
 * A data record.
 */
public class CachingDataRecord extends AbstractDataRecord {

    private final CachingDataStore store;

    public CachingDataRecord(CachingDataStore store, DataIdentifier identifier) {
        super(store, identifier);
        this.store = store;
    }

    public long getLastModified() {
        try {
            return store.getLastModified(getIdentifier());
        } catch (DataStoreException dse) {
            return 0;
        }
    }

    public long getLength() throws DataStoreException {
        return store.getLength(getIdentifier());
    }

    public InputStream getStream() throws DataStoreException {
        return store.getStream(getIdentifier());
    }

}
