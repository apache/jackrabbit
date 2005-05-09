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
package org.apache.jackrabbit.value;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Stateful value implementation. This class implements the Context
 * part of the State design pattern (GoF) used for managing the JCR Value
 * states.
 * <p>
 * Instances of this class are issued by the {@link org.apache.jackrabbit.value.SerialValueFactory}
 * and are <code>Serializable</code>.
 *
 * @see org.apache.jackrabbit.value.SerialValueFactory
 */
final class SerialValue implements Value, Serializable {

    /** Static serial version UID. */
    static final long serialVersionUID = 6970955308427991717L;
    
    /**
     * Type of the underlying value. Note that this type is never changed
     * even if the value state changes. Thus the type is memorized in this
     * member variable when the value instance is created.
     */
    private int type;

    /**
     * The underlying value instance. This is the state reference that
     * changes during state transitions.
     */
    private StatefullValue value;

    /**
     * Creates a generic value instance.  A new InitialValue instance
     * that wraps the given static value instance is created to intercept
     * the first getter method. The created InitialValue instance has
     * the responsibility of changing the value state.
     *
     * @param value underlying static value instance
     */
    SerialValue(StatefullValue value) {
        this.type = value.getType();
        this.value = new InitialValue(this, value);
    }

    /**
     * Changes the value state. This method is invoked by the
     * {@link InitialValue InitialValue} class to commit the value state.
     *
     * @param value new value state
     */
    void setValue(StatefullValue value) {
        this.value = value;
    }

    /**
     * Returns the binary representation of the value. The actual behaviour
     * depends on the current state of the value, see the JCR documentation
     * for the details.
     *
     * @return binary value
     * @throws IllegalStateException if the value is in non-stream state
     * @throws RepositoryException if another error occurs
     * @see Value#getStream()
     */
    public InputStream getStream() throws IllegalStateException,
            RepositoryException {
        return value.getStream();
    }

    /**
     * Returns the string representation of the value. The actual behaviour
     * depends on the current state of the value, see the JCR documentation
     * for the details.
     *
     * @return string value
     * @throws ValueFormatException if conversion to string is not possible
     * @throws IllegalStateException if the value is in stream state
     * @throws RepositoryException if another error occurs
     * @see Value#getString()
     */
    public String getString() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value.getString();
    }

    /**
     * Returns the long representation of the value. The actual behaviour
     * depends on the current state of the value, see the JCR documentation
     * for the details.
     *
     * @return long value
     * @throws ValueFormatException if conversion to long is not possible
     * @throws IllegalStateException if the value is in stream state
     * @throws RepositoryException if another error occurs
     * @see Value#getLong()
     */
    public long getLong() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value.getLong();
    }

    /**
     * Returns the double representation of the value. The actual behaviour
     * depends on the current state of the value, see the JCR documentation
     * for the details.
     *
     * @return double value
     * @throws ValueFormatException if conversion to double is not possible
     * @throws IllegalStateException if the value is in stream state
     * @throws RepositoryException if another error occurs
     * @see Value#getDouble()
     */
    public double getDouble() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value.getDouble();
    }

    /**
     * Returns the date representation of the value. The actual behaviour
     * depends on the current state of the value, see the JCR documentation
     * for the details.
     *
     * @return date value
     * @throws ValueFormatException if conversion to date is not possible
     * @throws IllegalStateException if the value is in stream state
     * @throws RepositoryException if another error occurs
     * @see Value#getDate()
     */
    public Calendar getDate() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value.getDate();
    }

    /**
     * Returns the boolean representation of the value. The actual behaviour
     * depends on the current state of the value, see the JCR documentation
     * for the details.
     *
     * @return boolean value
     * @throws ValueFormatException if conversion to boolean is not possible
     * @throws IllegalStateException if the value is in stream state
     * @throws RepositoryException if another error occurs
     * @see Value#getDouble()
     */
    public boolean getBoolean() throws ValueFormatException,
            IllegalStateException, RepositoryException {
        return value.getBoolean();
    }

    /**
     * Returns the value type. Note that value type remains the same even
     * if the underlying value is converted to another type during the
     * stream/non-stream state transition.
     *
     * @return value type
     * @see javax.jcr.PropertyType
     * @see Value#getType()
     */
    public int getType() {
        return type;
    }

    //---------- Serializable interface support --------------------------------

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
            // write real type
            out.writeInt(type);

            switch (type) {
                case PropertyType.BINARY:
                    InputStream data = value.getStream();
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] bytes = new byte[4096];
                    for (int n = data.read(bytes); n != -1; n = data
                        .read(bytes)) {
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
            SerialValueFactory factory = SerialValueFactory.getInstance();
            StatefullValue realValue;
            int type = in.readInt();
            switch (type) {
                case PropertyType.BINARY:
                    byte[] bytes = new byte[in.readInt()];
                    in.readFully(bytes);
                    ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
                    realValue = factory.createBinaryValue(bin);
                    break;
                case PropertyType.BOOLEAN:
                    realValue = factory.createBooleanValue(in.readBoolean());
                    break;
                case PropertyType.DATE:
                    realValue = factory.createDateValue((Calendar) in.readObject());
                    break;
                case PropertyType.DOUBLE:
                    realValue = factory.createDoubleValue(in.readDouble());
                    break;
                case PropertyType.LONG:
                    realValue = factory.createLongValue(in.readLong());
                    break;
                case PropertyType.NAME:
                    realValue = factory.createNameValue(in.readUTF());
                    break;
                case PropertyType.PATH:
                    realValue = factory.createPathValue(in.readUTF());
                    break;
                case PropertyType.REFERENCE:
                    realValue = factory.createReferenceValue(in.readUTF());
                    break;
                case PropertyType.STRING:
                    realValue = factory.createStringValue(in.readUTF());
                    break;
                default:
                    throw new IllegalStateException("Illegal serial value type");
            }

            // now we set the fields
            this.type = type;
            this.value = new InitialValue(this, realValue);

        } catch (ValueFormatException vfe) {
            throw new IOException(vfe.getMessage());
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex.getMessage());
        }
    }
}
