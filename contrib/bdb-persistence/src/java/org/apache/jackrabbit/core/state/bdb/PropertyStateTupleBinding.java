/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state.bdb;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

public class PropertyStateTupleBinding extends TupleBinding {

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
        try {
            PropertyState state = new PropertyState(id.getName(), id.getParentUUID(), PropertyState.STATUS_NEW, false);

            // type
            int type = in.readInt();
            state.setType(type);
            // multiValued
            boolean multiValued = in.readBoolean();
            state.setMultiValued(multiValued);
            // definitionId
            String s = in.readString();
            state.setDefinitionId(PropDefId.valueOf(s));
            // values
            int count = in.readInt(); // count
            InternalValue[] values = new InternalValue[count];
            for (int i = 0; i < count; i++) {
                InternalValue val;
                if (type == PropertyType.BINARY) {
                    s = in.readString(); // value (i.e. blobId)
                    // special handling required for binary value:
                    // the value stores the id of the blob resource in the blob store
                    val = InternalValue.create(blobStore.get(s));
                } else {
                    int len = in.readInt(); // lenght of byte[]
                    byte[] bytes = new byte[len];
                    in.read(bytes); // byte[]
                    s = new String(bytes, BerkeleyDBPersistenceManager.ENCODING);
                    val = InternalValue.valueOf(s, type);
                }
                values[i] = val;
            }
            state.setValues(values);

            return state;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void objectToEntry(Object o, TupleOutput out) {
        try {
            PropertyState state = (PropertyState) o;

            // type
            out.writeInt(state.getType());
            // multiValued
            out.writeBoolean(state.isMultiValued());
            // definitionId
            out.writeString(state.getDefinitionId().toString());
            // values
            InternalValue[] values = state.getValues();
            out.writeInt(values.length); // count
            for (int i = 0; i < values.length; i++) {
                InternalValue val = values[i];
                if (state.getType() == PropertyType.BINARY) {
                    // special handling required for binary value:
                    // spool binary value to file in blob store
                    BLOBFileValue blobVal = (BLOBFileValue) val.internalValue();
                    InputStream in = blobVal.getStream();
                    String blobId;
                    try {
                        blobId = blobStore.put((PropertyId) state.getId(), i, in, blobVal.getLength());
                    } finally {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    // store id of blob as property value
                    out.writeString(blobId); // value
                    // replace value instance with value
                    // backed by resource in blob store and delete temp file
                    values[i] = InternalValue.create(blobStore.get(blobId));
                    blobVal.discard();
                    blobVal = null; // gc hint
                } else {
                    byte[] bytes = val.toString().getBytes(BerkeleyDBPersistenceManager.ENCODING);
                    out.writeInt(bytes.length); // lenght of byte[]
                    out.write(bytes); // byte[]
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
