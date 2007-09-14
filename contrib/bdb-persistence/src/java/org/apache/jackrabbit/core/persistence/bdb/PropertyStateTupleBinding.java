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
package org.apache.jackrabbit.core.persistence.bdb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.state.PropertyState;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class PropertyStateTupleBinding extends TupleBinding {

    private Log log = LogFactory.getLog(PropertyStateTupleBinding.class);

    private BLOBStore blobStore;
    private PropertyId id;

    public PropertyStateTupleBinding(BLOBStore blobStore) {
        this.blobStore = blobStore;
    }

    public PropertyStateTupleBinding(PropertyId propertyId, BLOBStore blobStore) {
        this.blobStore = blobStore;
        this.id = propertyId;
    }

    public Object entryToObject(TupleInput in) {

        PropertyState state = new PropertyState(id, PropertyState.STATUS_NEW, false);

        try {
            Serializer.deserialize(state, in, blobStore);
        } catch (Exception e) {
            // since the TupleInput methods do not throw any
            // exceptions the above call should neither...
            String msg = "error while deserializing property state";
            log.debug(msg);
            throw new RuntimeException(msg, e);
        }

        return state;
    }

    public void objectToEntry(Object o, TupleOutput out) {
        try {
            Serializer.serialize((PropertyState) o, out, blobStore);
        } catch (Exception e) {
            // since the TupleOutput methods do not throw any
            // exceptions the above call should neither...
            String msg = "error while serializing property state";
            log.debug(msg);
            throw new RuntimeException(msg, e);
        }
    }
}
