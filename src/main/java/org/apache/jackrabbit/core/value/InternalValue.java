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
package org.apache.jackrabbit.core.value;

import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.value.BinaryValue;
import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.apache.jackrabbit.value.NameValue;
import org.apache.jackrabbit.value.PathValue;
import org.apache.jackrabbit.value.ReferenceValue;
import org.apache.jackrabbit.value.StringValue;
import org.apache.jackrabbit.value.ValueHelper;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * <code>InternalValue</code> represents the internal format of a property value.
 * <p/>
 * The following table specifies the internal format for every property type:
 * <pre>
 * <table>
 * <tr><b>PropertyType</b><td></td><td><b>Internal Format</b></td></tr>
 * <tr>STRING<td></td><td>String</td></tr>
 * <tr>LONG<td></td><td>Long</td></tr>
 * <tr>DOUBLE<td></td><td>Double</td></tr>
 * <tr>DATE<td></td><td>Calendar</td></tr>
 * <tr>BOOLEAN<td></td><td>Boolean</td></tr>
 * <tr>NAME<td></td><td>QName</td></tr>
 * <tr>PATH<td></td><td>Path</td></tr>
 * <tr>BINARY<td></td><td>BLOBFileValue</td></tr>
 * <tr>REFERENCE<td></td><td>UUID</td></tr>
 * </table>
 * </pre>
 */
public class InternalValue {

    public static final InternalValue[] EMPTY_ARRAY = new InternalValue[0];

    public static final InternalValue BOOLEAN_TRUE = create(true);

    public static final InternalValue BOOLEAN_FALSE = create(false);

    private final Object val;
    private final int type;

    //------------------------------------------------------< factory methods >
    /**
     * @param value
     * @param nsResolver
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public static InternalValue create(Value value, NamespaceResolver nsResolver)
            throws ValueFormatException, RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }

        switch (value.getType()) {
            case PropertyType.BINARY:
                try {
                    if (value instanceof BLOBFileValue) {
                        return new InternalValue((BLOBFileValue) value);
                    } else {
                        InputStream stream = value.getStream();
                        try {
                            return new InternalValue(new BLOBFileValue(stream));
                        } finally {
                            try {
                                stream.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                } catch (IOException ioe) {
                    throw new ValueFormatException(ioe.getMessage());
                }
            case PropertyType.BOOLEAN:
                return value.getBoolean() ? BOOLEAN_TRUE : BOOLEAN_FALSE;
            case PropertyType.DATE:
                return new InternalValue(value.getDate());
            case PropertyType.DOUBLE:
                return new InternalValue(value.getDouble());
            case PropertyType.LONG:
                return new InternalValue(value.getLong());
            case PropertyType.REFERENCE:
                return new InternalValue(new UUID(value.getString()));
            case PropertyType.NAME:
                try {
                    return new InternalValue(QName.fromJCRName(value.getString(), nsResolver));
                } catch (IllegalNameException ine) {
                    throw new ValueFormatException(ine.getMessage());
                } catch (UnknownPrefixException upe) {
                    throw new ValueFormatException(upe.getMessage());
                }
            case PropertyType.PATH:
                try {
                    return new InternalValue(Path.create(value.getString(), nsResolver, false));
                } catch (MalformedPathException mpe) {
                    throw new ValueFormatException(mpe.getMessage());
                }
            case PropertyType.STRING:
                return new InternalValue(value.getString());

            default:
                throw new IllegalArgumentException("illegal value");
        }
    }

    /**
     * @param value
     * @param targetType
     * @param nsResolver
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public static InternalValue create(Value value, int targetType,
                                       NamespaceResolver nsResolver)
            throws ValueFormatException, RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }
        return create(ValueHelper.convert(value, targetType), nsResolver);
    }

    /**
     * @param value
     * @param targetType
     * @param nsResolver
     * @return
     * @throws ValueFormatException
     * @throws RepositoryException
     */
    public static InternalValue create(String value, int targetType,
                                       NamespaceResolver nsResolver)
            throws ValueFormatException, RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("null value");
        }
        return create(ValueHelper.convert(value, targetType), nsResolver);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(String value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(long value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(double value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(Calendar value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(boolean value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(byte[] value) {
        return new InternalValue(new BLOBFileValue(value));
    }

    /**
     * @param value
     * @return
     * @throws IOException
     */
    public static InternalValue create(InputStream value) throws IOException {
        return new InternalValue(new BLOBFileValue(value));
    }

    /**
     * @param value
     * @return
     * @throws IOException
     */
    public static InternalValue create(FileSystemResource value)
            throws IOException {
        return new InternalValue(new BLOBFileValue(value));
    }

    /**
     * @param value
     * @return
     * @throws IOException
     */
    public static InternalValue create(File value) throws IOException {
        return new InternalValue(new BLOBFileValue(value));
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(QName value) {
        return new InternalValue(value);
    }

    /**
     * @param values
     * @return
     */
    public static InternalValue[] create(QName[] values) {
        InternalValue[] ret = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = new InternalValue(values[i]);
        }
        return ret;
    }

    /**
     * @param values
     * @return
     */
    public static InternalValue[] create(String[] values) {
        InternalValue[] ret = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = new InternalValue(values[i]);
        }
        return ret;
    }

    /**
     * @param values
     * @return
     */
    public static InternalValue[] create(Calendar[] values) {
        InternalValue[] ret = new InternalValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = new InternalValue(values[i]);
        }
        return ret;
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(Path value) {
        return new InternalValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static InternalValue create(UUID value) {
        return new InternalValue(value);
    }

    //----------------------------------------------------< conversions, etc. >
    /**
     * @param nsResolver
     * @return
     * @throws RepositoryException
     */
    public Value toJCRValue(NamespaceResolver nsResolver)
            throws RepositoryException {
        switch (type) {
            case PropertyType.BINARY:
                return new BinaryValue(((BLOBFileValue) val).getStream());
            case PropertyType.BOOLEAN:
                return new BooleanValue(((Boolean) val));
            case PropertyType.DATE:
                return new DateValue((Calendar) val);
            case PropertyType.DOUBLE:
                return new DoubleValue((Double) val);
            case PropertyType.LONG:
                return new LongValue((Long) val);
            case PropertyType.REFERENCE:
                return ReferenceValue.valueOf(((UUID) val).toString());
            case PropertyType.PATH:
                try {
                    return PathValue.valueOf(((Path) val).toJCRPath(nsResolver));
                } catch (NoPrefixDeclaredException npde) {
                    // should never get here...
                    throw new RepositoryException("internal error: encountered unregistered namespace", npde);
                }
            case PropertyType.NAME:
                try {
                    return NameValue.valueOf(((QName) val).toJCRName(nsResolver));
                } catch (NoPrefixDeclaredException npde) {
                    // should never get here...
                    throw new RepositoryException("internal error: encountered unregistered namespace", npde);
                }
            case PropertyType.STRING:
                return new StringValue((String) val);
            default:
                throw new RepositoryException("illegal internal value type");
        }
    }

    /**
     * @return
     */
    public Object internalValue() {
        return val;
    }

    /**
     * @return
     */
    public int getType() {
        return type;
    }

    /**
     * @return
     * @throws RepositoryException
     */
    public InternalValue createCopy() throws RepositoryException {
        switch (type) {
            case PropertyType.BINARY:
                try {
                    InputStream stream = ((BLOBFileValue) val).getStream();
                    try {
                        return new InternalValue(new BLOBFileValue(stream));
                    } finally {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                } catch (IOException ioe) {
                    throw new RepositoryException("failed to copy binary value", ioe);
                }
            case PropertyType.BOOLEAN:
                return new InternalValue(((Boolean) val).booleanValue());
            case PropertyType.DATE:
                return new InternalValue((Calendar) val);
            case PropertyType.DOUBLE:
                return new InternalValue(((Double) val).doubleValue());
            case PropertyType.LONG:
                return new InternalValue(((Long) val).longValue());
            case PropertyType.REFERENCE:
                return new InternalValue((UUID) val);
            case PropertyType.PATH:
                return new InternalValue((Path) val);
            case PropertyType.NAME:
                return new InternalValue((QName) val);
            case PropertyType.STRING:
                return new InternalValue((String) val);
            default:
                throw new RepositoryException("illegal internal value type");
        }
    }

    /**
     * Returns the string representation of this internal value. If this is a
     * <i>binary</i> value then the path of its backing file will be returned.
     *
     * @return string representation of this internal value
     */
    public String toString() {
        if (type == PropertyType.DATE) {
            return ISO8601.format((Calendar) val);
        } else {
            return val.toString();
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof InternalValue) {
            InternalValue other = (InternalValue) obj;
            return val.equals(other.val);
        }
        return false;
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

    /**
     * @param s
     * @param type
     * @return
     */
    public static InternalValue valueOf(String s, int type) {
        switch (type) {
            case PropertyType.BOOLEAN:
                return new InternalValue(Boolean.valueOf(s).booleanValue());
            case PropertyType.DATE:
                return new InternalValue(ISO8601.parse(s));
            case PropertyType.DOUBLE:
                return new InternalValue(Double.valueOf(s).doubleValue());
            case PropertyType.LONG:
                return new InternalValue(Long.valueOf(s).longValue());
            case PropertyType.REFERENCE:
                return new InternalValue(new UUID(s));
            case PropertyType.PATH:
                return new InternalValue(Path.valueOf(s));
            case PropertyType.NAME:
                return new InternalValue(QName.valueOf(s));
            case PropertyType.STRING:
                return new InternalValue(s);

            case PropertyType.BINARY:
                throw new IllegalArgumentException("this method does not support the type PropertyType.BINARY");
            default:
                throw new IllegalArgumentException("illegal type");
        }
    }

    //-------------------------------------------------------< implementation >
    private InternalValue(String value) {
        val = value;
        type = PropertyType.STRING;
    }

    private InternalValue(QName value) {
        val = value;
        type = PropertyType.NAME;
    }

    private InternalValue(long value) {
        val = new Long(value);
        type = PropertyType.LONG;
    }

    private InternalValue(double value) {
        val = new Double(value);
        type = PropertyType.DOUBLE;
    }

    private InternalValue(Calendar value) {
        val = value;
        type = PropertyType.DATE;
    }

    private InternalValue(boolean value) {
        val = new Boolean(value);
        type = PropertyType.BOOLEAN;
    }

    private InternalValue(BLOBFileValue value) {
        val = value;
        type = PropertyType.BINARY;
    }

    private InternalValue(Path value) {
        val = value;
        type = PropertyType.PATH;
    }

    private InternalValue(UUID value) {
        val = value;
        type = PropertyType.REFERENCE;
    }
}
