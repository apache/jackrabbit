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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.api.ReferenceBinary;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.value.BinaryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>ValueFactoryImpl</code>...
 */
public class ValueFactoryImpl extends ValueFactoryQImpl {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(ValueFactoryImpl.class);

    private final DataStore store;

    /**
     * Constructs a new <code>ValueFactoryQImpl</code> based
     * on an existing SPI <code>QValueFactory</code> and a
     * <code>NamePathResolver</code>.
     *
     * @param resolver wrapped <code>NamePathResolver</code>
     */
    public ValueFactoryImpl(NamePathResolver resolver, DataStore store) {
        super(new InternalValueFactory(store), resolver);
        this.store = store;
    }

    @Override
    public Value createValue(QValue qvalue) {
        if (qvalue instanceof InternalValue && PropertyType.BINARY == qvalue.getType()) {
            try {
                return new BinaryValueImpl(((InternalValue) qvalue).getBLOBFileValue().copy());
            } catch (RepositoryException e) {
                // should not get here
                log.error(e.getMessage(), e);
            }
        }
        return super.createValue(qvalue);
    }

    @Override
    public Binary createBinary(InputStream stream) throws RepositoryException {
        try {
            QValue value = getQValueFactory().create(stream);
            if (value instanceof InternalValue) {
                return ((InternalValue) value).getBLOBFileValue();
            } else {
                return new BinaryImpl(stream);
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        } finally {
            // JCR-2903
            IOUtils.closeQuietly(stream);
        }
    }

    @Override
    public Value createValue(Binary binary) {
        try {
            if (binary instanceof BLOBInDataStore) {
                BLOBInDataStore blob = (BLOBInDataStore) binary;
                DataIdentifier identifier = blob.getDataIdentifier();
                InternalValue value;
                if (blob.usesDataStore(store)) {
                    value = InternalValue.getInternalValue(identifier, store, false);
                } else {
                    value = InternalValue.getInternalValue(identifier, store, true);
                }
                if (value != null) {
                    // if the value is already in this data store
                    return new BinaryValueImpl(value.getBLOBFileValue());
                }
            } else if (binary instanceof BLOBFileValue) {
                return new BinaryValueImpl(((BLOBFileValue) binary).copy());
            } else if (binary instanceof ReferenceBinary) {
                String reference = ((ReferenceBinary) binary).getReference();
                DataRecord record = store.getRecordFromReference(reference);
                if (record != null) {
                    return new BinaryValueImpl(BLOBInDataStore.getInstance(
                            store, record.getIdentifier()));
                }
            }
            return createValue(binary.getStream());
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            // ignore - the super method may be smarter
        }
        return super.createValue(binary);
    }

    @Override
    public Value createValue(InputStream value) {
        try {
            InternalValue qvalue = (InternalValue) getQValueFactory().create(value);
            return new BinaryValueImpl(qvalue.getBLOBFileValue());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        } finally {
            // JCR-2903
            IOUtils.closeQuietly(value);
        }
    }

    @Override
    public Value createValue(String value, int type) throws ValueFormatException {
        if (PropertyType.BINARY == type) {
            try {
                InternalValue qvalue = (InternalValue) getQValueFactory().create(value, type);
                return new BinaryValueImpl(qvalue.getBLOBFileValue());
            } catch (RepositoryException e) {
                throw new ValueFormatException(e);
            }
        } else {
            return super.createValue(value, type);
        }
    }
}