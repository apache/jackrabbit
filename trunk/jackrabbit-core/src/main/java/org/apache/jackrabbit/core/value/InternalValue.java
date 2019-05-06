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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.AbstractQValue;
import org.apache.jackrabbit.spi.commons.value.AbstractQValueFactory;
import org.apache.jackrabbit.spi.commons.value.QValueValue;
import org.apache.jackrabbit.util.ISO8601;

/**
 * <code>InternalValue</code> represents the internal format of a property value.
 * <p>
 * The following table specifies the internal format for every property type:
 * <table><caption>Internal format of property types</caption>
 * <tr><td><b>PropertyType</b></td><td><b>Internal Format</b></td></tr>
 * <tr><td>STRING</td><td>String</td></tr>
 * <tr><td>LONG</td><td>Long</td></tr>
 * <tr><td>DOUBLE</td><td>Double</td></tr>
 * <tr><td>DATE</td><td>Calendar</td></tr>
 * <tr><td>BOOLEAN</td><td>Boolean</td></tr>
 * <tr><td>NAME</td><td>Name</td></tr>
 * <tr><td>PATH</td><td>Path</td></tr>
 * <tr><td>URI</td><td>URI</td></tr>
 * <tr><td>DECIMAL</td><td>BigDecimal</td></tr>
 * <tr><td>BINARY</td><td>BLOBFileValue</td></tr>
 * <tr><td>REFERENCE</td><td>{@link NodeId}</td></tr>
 * </table>
 */
public class InternalValue extends AbstractQValue {

    private static final long serialVersionUID = -7340744360527434409L;

    public static final InternalValue[] EMPTY_ARRAY = new InternalValue[0];

    private static final InternalValue BOOLEAN_TRUE = new InternalValue(true);

    private static final InternalValue BOOLEAN_FALSE = new InternalValue(false);

    /**
     * Temporary binary values smaller or equal this size are kept in memory
     */
    private static final int MIN_BLOB_FILE_SIZE = 1024;

    //------------------------------------------------------< factory methods >
    /**
     * Create a new internal value from the given JCR value.
     * Large binary values are stored in a temporary file.
     *
     * @param value the JCR value
     * @param resolver
     * @return the created internal value
     * @throws RepositoryException
     * @throws ValueFormatException
     */
    public static InternalValue create(Value value, NamePathResolver resolver)
            throws ValueFormatException, RepositoryException {
        return create(value, resolver, null);
    }

    /**
     * Create a new internal value from the given JCR value.
     * If the data store is enabled, large binary values are stored in the data store.
     *
     * @param value the JCR value
     * @param resolver
     * @param store the data store
     * @return the created internal value
     * @throws RepositoryException
     * @throws ValueFormatException
     */
    public static InternalValue create(Value value, NamePathResolver resolver, DataStore store)
            throws ValueFormatException, RepositoryException {
        switch (value.getType()) {
            case PropertyType.BINARY:
                BLOBFileValue blob = null;
                if (value instanceof BinaryValueImpl) {
                    BinaryValueImpl bin = (BinaryValueImpl) value;
                    DataIdentifier identifier = bin.getDataIdentifier();
                    if (identifier != null) {
                        if (bin.usesDataStore(store)) {
                            // access the record to ensure it is not garbage collected
                            store.getRecord(identifier);
                            blob = BLOBInDataStore.getInstance(store, identifier);
                        } else {
                            if (store.getRecordIfStored(identifier) != null) {
                                // it exists - so we don't need to stream it again
                                // but we need to create a new object because the original
                                // one might be in a different data store (repository)
                                blob = BLOBInDataStore.getInstance(store, identifier);
                            }
                        }
                    }
                }
                if (blob == null) {
                    Binary b = value.getBinary();
                    boolean dispose = false;
                    try {
                        if (b instanceof BLOBFileValue) {
                            // use as is
                            blob = (BLOBFileValue) b;
                        } else {
                            // create a copy from the stream
                            dispose = true;
                            blob = getBLOBFileValue(store, b.getStream(), true);
                        }
                    } finally {
                        if (dispose) {
                            b.dispose();
                        }
                    }
                }
                return new InternalValue(blob);
            case PropertyType.BOOLEAN:
                return create(value.getBoolean());
            case PropertyType.DATE:
                return create(value.getDate());
            case PropertyType.DOUBLE:
                return create(value.getDouble());
            case PropertyType.DECIMAL:
                return create(value.getDecimal());
            case PropertyType.LONG:
                return create(value.getLong());
            case PropertyType.REFERENCE:
                return create(new NodeId(value.getString()));
            case PropertyType.WEAKREFERENCE:
                return create(new NodeId(value.getString()), true);
            case PropertyType.URI:
                try {
                    return create(new URI(value.getString()));
                } catch (URISyntaxException e) {
                    throw new ValueFormatException(e.getMessage());
                }
            case PropertyType.NAME:
                try {
                    if (value instanceof QValueValue) {
                        QValue qv = ((QValueValue) value).getQValue();
                        if (qv instanceof InternalValue) {
                            return (InternalValue) qv;
                        } else {
                            return create(qv.getName());
                        }
                    } else {
                        return create(resolver.getQName(value.getString()));
                    }
                } catch (NameException e) {
                    throw new ValueFormatException(e.getMessage());
                }
            case PropertyType.PATH:
                try {
                    if (value instanceof QValueValue) {
                        QValue qv = ((QValueValue) value).getQValue();
                        if (qv instanceof InternalValue) {
                            return (InternalValue) qv;
                        } else {
                            return create(qv.getPath());
                        }
                    } else {
                        return create(resolver.getQPath(value.getString(), false));
                    }
                } catch (MalformedPathException mpe) {
                    throw new ValueFormatException(mpe.getMessage());
                }
            case PropertyType.STRING:
                return create(value.getString());
            default:
                throw new IllegalArgumentException("illegal value");
        }
    }

    public static InternalValue create(QValue value)
            throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.BINARY:
                try {
                    return create(value.getString().getBytes(AbstractQValueFactory.DEFAULT_ENCODING));
                } catch (UnsupportedEncodingException e) {
                    throw new InternalError(AbstractQValueFactory.DEFAULT_ENCODING + " not supported");
                }
            case PropertyType.BOOLEAN:
                return new InternalValue(value.getBoolean());
            case PropertyType.DATE:
                return new InternalValue(value.getCalendar());
            case PropertyType.DOUBLE:
                return new InternalValue(value.getDouble());
            case PropertyType.DECIMAL:
                return new InternalValue(value.getDecimal());
            case PropertyType.LONG:
                return new InternalValue(value.getLong());
            case PropertyType.REFERENCE:
                return create(new NodeId(value.getString()));
            case PropertyType.WEAKREFERENCE:
                return create(new NodeId(value.getString()), true);
            case PropertyType.URI:
                return new InternalValue(value.getURI());
            case PropertyType.NAME:
                return new InternalValue(value.getName());
            case PropertyType.PATH:
                return new InternalValue(value.getPath());
            case PropertyType.STRING:
                return new InternalValue(value.getString(), PropertyType.STRING);
            default:
                throw new IllegalArgumentException("illegal value");
        }
    }

    public static InternalValue[] create(QValue[] values)
            throws RepositoryException {
        if (values == null) {
            return null;
        }
        InternalValue[] tmp = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            tmp[i] = InternalValue.create(values[i]);
        }
        return tmp;
    }

    /**
     * Get the internal value for this blob.
     *
     * @param identifier the identifier
     * @param store the data store
     * @param verify verify if the record exists, and return null if not
     * @return the internal value or null
     */
    static InternalValue getInternalValue(DataIdentifier identifier, DataStore store, boolean verify) throws DataStoreException {
        if (verify) {
            if (store.getRecordIfStored(identifier) == null) {
                return null;
            }
        } else {
            // access the record to ensure it is not garbage collected
            store.getRecord(identifier);
        }
        // it exists - so we don't need to stream it again
        // but we need to create a new object because the original
        // one might be in a different data store (repository)
        BLOBFileValue blob = BLOBInDataStore.getInstance(store, identifier);
        return new InternalValue(blob);
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(String value) {
        return new InternalValue(value, PropertyType.STRING);
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(long value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(double value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(Calendar value) {
        return new InternalValue(value);
    }
    
    /**
     * https://issues.apache.org/jira/browse/JCR-3083
     * 
     * @param value
     * @return the created value
     */
    public static InternalValue createDate(String value) {
        return new InternalValue(value, PropertyType.DATE);
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(BigDecimal value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(URI value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(boolean value) {
        return value ? BOOLEAN_TRUE : BOOLEAN_FALSE;
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(byte[] value) {
        return new InternalValue(BLOBInMemory.getInstance(value));
    }

    /**
     * Create an internal value that is backed by a temporary file.
     *
     * @param value the stream
     * @return the internal value
     * @throws RepositoryException
     */
    public static InternalValue createTemporary(InputStream value) throws RepositoryException {
        return new InternalValue(getBLOBFileValue(null, value, true));
    }

    /**
     * Create an internal value that is stored in the data store (if enabled).
     *
     * @param value the input stream
     * @param store
     * @return the internal value
     * @throws RepositoryException
     */
    public static InternalValue create(InputStream value, DataStore store) throws RepositoryException {
        return new InternalValue(getBLOBFileValue(store, value, false));
    }

    /**
     * @param value
     * @return
     * @throws RepositoryException
     */
    public static InternalValue create(InputStream value) throws RepositoryException {
        return create(value, null);
    }

    /**
     * @param value
     * @return
     * @throws IOException
     */
    public static InternalValue create(FileSystemResource value) throws IOException {
        return new InternalValue(BLOBInResource.getInstance(value));
    }

    /**
     * Create a binary object with the given identifier.
     *
     * @param store the data store
     * @param id the identifier
     * @return the value
     */
    public static InternalValue create(DataStore store, String id) {
        return new InternalValue(getBLOBFileValue(store, id));
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(Name value) {
        return new InternalValue(value);
    }

    /**
     * @param values
     * @return the created value
     */
    public static InternalValue[] create(Name[] values) {
        InternalValue[] ret = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = new InternalValue(values[i]);
        }
        return ret;
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(Path value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return the created value
     */
    public static InternalValue create(NodeId value) {
        return create(value, false);
    }

    /**
     * @param value
     * @param weak
     * @return the created value
     */
    public static InternalValue create(NodeId value, boolean weak) {
        return new InternalValue(value, weak);
    }

    //----------------------------------------------------< conversions, etc. >

    BLOBFileValue getBLOBFileValue() {
        assert val != null && type == PropertyType.BINARY;
        return (BLOBFileValue) val;
    }

    public NodeId getNodeId() {
        assert val != null && (type == PropertyType.REFERENCE || type == PropertyType.WEAKREFERENCE);
        return (NodeId) val;
    }

    public Calendar getDate() throws RepositoryException {
        assert val != null && type == PropertyType.DATE;
        return getCalendar();
    }

    /**
     * Create a copy of this object. Immutable values will return itself,
     * while mutable values will return a copy.
     *
     * @return itself or a copy
     * @throws RepositoryException
     */
    public InternalValue createCopy() throws RepositoryException {
        if (type != PropertyType.BINARY) {
            // for all types except BINARY it's safe to return 'this' because the
            // wrapped value is immutable (and therefore this instance as well)
            return this;
        }
        // return a copy of the wrapped BLOBFileValue
        return new InternalValue(((BLOBFileValue) val).copy());
    }

    /**
     * Parses the given string as an <code>InternalValue</code> of the
     * specified type. The string must be in the format returned by the
     * <code>InternalValue.toString()</code> method.
     *
     * @param s a <code>String</code> containing the <code>InternalValue</code>
     *          representation to be parsed.
     * @param type
     * @return the <code>InternalValue</code> represented by the arguments
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as an <code>InternalValue</code> of the
     *                                  specified type.
     * @see #toString()
     */
    public static InternalValue valueOf(String s, int type) {
        switch (type) {
            case PropertyType.BOOLEAN:
                return create(Boolean.valueOf(s));
            case PropertyType.DATE:
                return create(ISO8601.parse(s));
            case PropertyType.DOUBLE:
                return create(Double.parseDouble(s));
            case PropertyType.LONG:
                return create(Long.parseLong(s));
            case PropertyType.DECIMAL:
                return create(new BigDecimal(s));
            case PropertyType.REFERENCE:
                return create(new NodeId(s));
            case PropertyType.WEAKREFERENCE:
                return create(new NodeId(s), true);
            case PropertyType.PATH:
                return create(PathFactoryImpl.getInstance().create(s));
            case PropertyType.NAME:
                return create(NameFactoryImpl.getInstance().create(s));
            case PropertyType.URI:
                return create(URI.create(s));
            case PropertyType.STRING:
                return create(s);

            case PropertyType.BINARY:
                throw new IllegalArgumentException(
                        "this method does not support the type PropertyType.BINARY");
            default:
                throw new IllegalArgumentException("illegal type: " + type);
        }
    }

    //-------------------------------------------------------< implementation >
    private InternalValue(String value, int type) {
        super(value, type);
    }
    
    private InternalValue(Name value) {
        super(value);
    }

    private InternalValue(long value) {
        super(value);
    }

    private InternalValue(double value) {
        super(value);
    }

    private InternalValue(Calendar value) {
        super(value);
    }

    private InternalValue(boolean value) {
        super(value);
    }

    private InternalValue(URI value) {
        super(value);
    }

    private InternalValue(BigDecimal value) {
        super(value);
    }

    private InternalValue(BLOBFileValue value) {
        super(value, PropertyType.BINARY);
    }

    private InternalValue(Path value) {
        super(value);
    }

    private InternalValue(NodeId value, boolean weak) {
        super(value, weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE);
    }

    /**
     * Create a BLOB value from in input stream. Small objects will create an in-memory object,
     * while large objects are stored in the data store or in a temp file (if the store parameter is not set).
     *
     * @param store the data store (optional)
     * @param in the input stream
     * @param temporary if the file should be deleted when discard is called (ignored if a data store is used)
     * @return the value
     * @throws RepositoryException
     */
    private static BLOBFileValue getBLOBFileValue(DataStore store, InputStream in, boolean temporary) throws RepositoryException {
        int maxMemorySize;
        if (store != null) {
            maxMemorySize = store.getMinRecordLength() - 1;
        } else {
            maxMemorySize = MIN_BLOB_FILE_SIZE;
        }
        maxMemorySize = Math.max(0, maxMemorySize);
        byte[] buffer = new byte[maxMemorySize];
        int pos = 0, len = maxMemorySize;
        try {
            while (pos < maxMemorySize) {
                int l = in.read(buffer, pos, len);
                if (l < 0) {
                    break;
                }
                pos += l;
                len -= l;
            }
        } catch (IOException e) {
            throw new RepositoryException("Could not read from stream", e);
        }
        if (pos < maxMemorySize) {
            // shrink the buffer
            byte[] data = new byte[pos];
            System.arraycopy(buffer, 0, data, 0, pos);
            return BLOBInMemory.getInstance(data);
        } else {
            // a few bytes are already read, need to re-build the input stream
            in = new SequenceInputStream(new ByteArrayInputStream(buffer, 0, pos), in);
            if (store != null) {
                return BLOBInDataStore.getInstance(store, in);
            } else {
                return BLOBInTempFile.getInstance(in, temporary);
            }
        }
    }

    private static BLOBFileValue getBLOBFileValue(DataStore store, String id) {
        if (BLOBInMemory.isInstance(id)) {
            return BLOBInMemory.getInstance(id);
        } else if (BLOBInDataStore.isInstance(id)) {
            return BLOBInDataStore.getInstance(store, id);
        } else {
            throw new IllegalArgumentException("illegal binary id: " + id);
        }
    }

    public boolean isInDataStore() {
        return val instanceof BLOBInDataStore;
    }

    //-------------------------------------------------------------< QValue >---
    /**
     * @see org.apache.jackrabbit.spi.QValue#getLength()
     */
    @Override
    public long getLength() throws RepositoryException {
        if (PropertyType.BINARY == type) {
            return ((Binary) val).getSize();
        } else {
            return super.getLength();
        }
    }

    /**
     * @see org.apache.jackrabbit.spi.QValue#getStream()
     */
    public InputStream getStream() throws RepositoryException {
        if (type == PropertyType.BINARY) {
            return ((Binary) val).getStream();
        } else {
            try {
                // convert via string
                return new ByteArrayInputStream(getString().getBytes(InternalValueFactory.DEFAULT_ENCODING));
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(InternalValueFactory.DEFAULT_ENCODING + " is not supported encoding on this platform", e);
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.spi.QValue#getBinary()
     */
    @Override
    public Binary getBinary() throws RepositoryException {
        if (type == PropertyType.BINARY) {
            // return an independent copy that can be disposed without
            // affecting this value
            return ((BLOBFileValue) val).copy();
        } else {
            try {
                // convert via string
                byte[] data = getString().getBytes(InternalValueFactory.DEFAULT_ENCODING);
                return BLOBInMemory.getInstance(data);
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(InternalValueFactory.DEFAULT_ENCODING + " is not supported encoding on this platform", e);
            }
        }
    }

    /**
     * @see org.apache.jackrabbit.spi.QValue#discard()
     */
    @Override
    public void discard() {
        if (type == PropertyType.BINARY) {
            BLOBFileValue bfv = (BLOBFileValue) val;
            bfv.dispose();
        } else {
            super.discard();
        }
    }

    /**
     * Delete persistent binary objects. This method does not delete objects in
     * the data store.
     */
    public void deleteBinaryResource() {
        if (type == PropertyType.BINARY) {
            BLOBFileValue bfv = (BLOBFileValue) val;
            bfv.delete(true);
        }
    }

    public boolean equals(Object object) {
        if (object instanceof InternalValue) {
            InternalValue that = (InternalValue) object;
            if (type == PropertyType.DATE) {
                try {
                    return that.type == PropertyType.DATE
                            && getDate().getTimeInMillis() == that.getDate()
                                    .getTimeInMillis();
                } catch (RepositoryException e) {
                    return false;
                }
            } else {
                return type == that.type && val.equals(that.val);
            }
        } else {
            return false;
        }
    }

}
