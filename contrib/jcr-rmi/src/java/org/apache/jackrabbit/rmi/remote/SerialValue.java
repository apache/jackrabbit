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
package org.apache.jackrabbit.rmi.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;

import javax.jcr.BinaryValue;
import javax.jcr.BooleanValue;
import javax.jcr.DateValue;
import javax.jcr.DoubleValue;
import javax.jcr.LongValue;
import javax.jcr.NameValue;
import javax.jcr.PathValue;
import javax.jcr.PropertyType;
import javax.jcr.ReferenceValue;
import javax.jcr.RepositoryException;
import javax.jcr.StringValue;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Serializable {@link Value Value} decorator. A SerialValue decorator
 * makes it possible to serialize the contents of a Value object even
 * if the object itself is not serializable. For example the standard
 * JCR Value classes are not serializable.
 * <p>
 * Serialization is achieved by extracting and serializing the type and
 * underlying data of the Value object. On deserialization the type and
 * data information is used to create a standard JCR Value object as
 * a copy of the original value. This makes it possible to copy even
 * system-specific Value instances to a remote JVM that might not contain
 * the implementation class of the original Value object.
 * <p>
 * The SerialValue decorator adds no other functionality to the Value
 * interface. Normal method calls are simply forwarded to the decorated
 * Value object.
 * <p>
 * Note that a decorator object keeps a reference to the underlying value
 * object and uses the standard value access methods to perform serialization.
 * Serialization therefore affects the internal state of the underlying value!
 * On the other hand, the internal state of a value might interfere with the
 * serialization decorator. The safest course of action is to only decorate
 * and serialize fresh value objects and to discard them after serialization.
 *
 * @author Jukka Zitting
 * @see javax.jcr.Value
 * @see java.io.Serializable
 */
public class SerialValue implements Value, Serializable {

    /** Static serial version UID. */
    static final long serialVersionUID = 8070492457339121953L;

    /** The decorated value. */
    private Value value;

    /**
     * Creates a serialization decorator for the given value.
     *
     * @param value the value to be decorated
     */
    public SerialValue(Value value) {
        this.value = value;
    }

    /**
     * Utility method for decorating an array of values. The
     * returned array will contain SerialValue decorators for
     * all the given values. Note that the contents of the
     * original values will only be copied when the decorators
     * are serialized.
     * <p>
     * If the given array is <code>null</code>, then an empty
     * array is returned.
     *
     * @param values the values to be decorated
     * @return array of decorated values
     */
    public static Value[] makeSerialValueArray(Value[] values) {
        if (values != null) {
            Value[] serials = new Value[values.length];
            for (int i = 0; i < values.length; i++) {
                serials[i] = new SerialValue(values[i]);
            }
            return serials;
        } else {
            return new Value[0];
        }
    }

    /**
     * Serializes the underlying Value object. Instead of using
     * the normal serialization mechanism, the essential state
     * of the Value object is extracted and written to the serialization
     * stream as a type-value pair.
     *
     * @param out the serialization stream
     * @throws IOException on IO errors
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        try {
            int type = value.getType();
            out.writeInt(type);
            switch (type) {
            case PropertyType.BINARY:
                InputStream data = value.getStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] bytes = new byte[4096];
                for (int n = data.read(bytes); n != -1; n = data.read(bytes)) {
                    buffer.write(bytes, 0, n);
                }
                out.writeInt(buffer.size());
                buffer.writeTo(out);
                break;
            case PropertyType.BOOLEAN:
                out.writeBoolean(value.getBoolean());
                break;
            case PropertyType.DATE:
                out.writeObject(value.getDate());
                break;
            case PropertyType.DOUBLE:
                out.writeDouble(value.getDouble());
                break;
            case PropertyType.LONG:
                out.writeLong(value.getLong());
                break;
            case PropertyType.NAME:
            case PropertyType.PATH:
            case PropertyType.REFERENCE:
            case PropertyType.STRING:
                out.writeUTF(value.getString());
                break;
            default:
                throw new IOException("Unknown value type");
            }
        } catch (RepositoryException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Deserializes the underlying Value object. A new Value object
     * is created based on the type and state data read fro the
     * serialization stream.
     *
     * @param in the serialization stream
     * @throws IOException on IO errors
     */
    private void readObject(ObjectInputStream in) throws IOException {
        try {
            int type = in.readInt();
            switch (type) {
            case PropertyType.BINARY:
                byte[] bytes = new byte[in.readInt()];
                in.readFully(bytes);
                value = new BinaryValue(bytes);
                break;
            case PropertyType.BOOLEAN:
                value = new BooleanValue(in.readBoolean());
                break;
            case PropertyType.DATE:
                value =  new DateValue((Calendar) in.readObject());
                break;
            case PropertyType.DOUBLE:
                value = new DoubleValue(in.readDouble());
                break;
            case PropertyType.LONG:
                value = new LongValue(in.readLong());
                break;
            case PropertyType.NAME:
                value = NameValue.valueOf(in.readUTF());
                break;
            case PropertyType.PATH:
                value = PathValue.valueOf(in.readUTF());
                break;
            case PropertyType.REFERENCE:
                value = ReferenceValue.valueOf(in.readUTF());
                break;
            case PropertyType.STRING:
                value = new StringValue(in.readUTF());
                break;
            default:
                throw new IllegalStateException("Illegal serial value type");
            }
        } catch (ValueFormatException ex) {
            throw new IOException(ex.getMessage());
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Forwards the method call to the decorated value.
     * {@inheritDoc}
     */
    public boolean getBoolean() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value.getBoolean();
    }

    /**
     * Forwards the method call to the decorated value.
     * {@inheritDoc}
     */
    public Calendar getDate() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value.getDate();
    }

    /**
     * Forwards the method call to the decorated value.
     * {@inheritDoc}
     */
    public double getDouble() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value.getDouble();
    }

    /**
     * Forwards the method call to the decorated value.
     * {@inheritDoc}
     */
    public long getLong() throws ValueFormatException, IllegalStateException,
            RepositoryException {
        return value.getLong();
    }

    /**
     * Forwards the method call to the decorated value.
     * {@inheritDoc}
     */
    public InputStream getStream() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value.getStream();
    }

    /**
     * Forwards the method call to the decorated value.
     * {@inheritDoc}
     */
    public String getString() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value.getString();
    }

    /**
     * Forwards the method call to the decorated value.
     * {@inheritDoc}
     */
    public int getType() {
        return value.getType();
    }

}
