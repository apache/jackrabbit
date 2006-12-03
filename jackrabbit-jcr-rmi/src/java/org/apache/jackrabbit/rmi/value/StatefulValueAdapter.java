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
package org.apache.jackrabbit.rmi.value;

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
 * The <code>StatefullValueAdapter</code> class implements the committed value
 * state for some JCR <code>Value</code> as a part of the State design pattern
 * (Gof) used by this package.
 * <p>
 * This class implements {@link #readObject(ObjectInputStream)} and
 * {@link #writeObject(ObjectOutputStream)} overwriting the default behaviour.
 * The reason for this is, that we cannot guarantee delegatee value to be
 * serializable in which case the {@link #writeObject(ObjectOutputStream)}
 * must first create a serializable value. The
 * {@link #readObject(ObjectInputStream)} method is here just to guarantee
 * symetric implementation.
 *
 * @author Felix Meschberger
 * @since 0.16.4.1
 * @see SerialValue
 */
final class StatefulValueAdapter implements Serializable, StatefulValue {

    /** The serial version UID */
    private static final long serialVersionUID = -8467636003279312276L;

    /** The delegatee value. */
    private Value delegatee;

    /**
     * Creates an instance adapting the given JCR <code>Value</code> to the
     * State design pattern.
     *
     * @param delegatee The JCR <code>Value</code> providing the value date.
     */
    StatefulValueAdapter(Value delegatee) {
        this.delegatee = delegatee;
    }

    /** {@inheritDoc} */
    public InputStream getStream() throws ValueFormatException, RepositoryException {
        return delegatee.getStream();
    }

    /** {@inheritDoc} */
    public boolean getBoolean() throws ValueFormatException, RepositoryException {
        return delegatee.getBoolean();
    }

    /** {@inheritDoc} */
    public Calendar getDate() throws ValueFormatException, RepositoryException {
        return delegatee.getDate();
    }

    /** {@inheritDoc} */
    public double getDouble() throws ValueFormatException, RepositoryException {
        return delegatee.getDouble();
    }

    /** {@inheritDoc} */
    public long getLong() throws ValueFormatException, RepositoryException {
        return delegatee.getLong();
    }

    /** {@inheritDoc} */
    public String getString() throws ValueFormatException, RepositoryException {
        return delegatee.getString();
    }

    /** {@inheritDoc} */
    public int getType() {
        return delegatee.getType();
    }

    /**
     * Writes the delegate value to the given <code>ObjectOutputStream</code>.
     * If the delegatee is {@link SerialValue} it is directly written. Otherwise
     * the {@link SerialValueFactory} is asked to create a {@link StatefullValue}
     * from the delegatee, which is then written. The newly created
     * {@link StatefullValue} value also replaces the original delegatee
     * internally.
     *
     * @param out The destination to write the delegatee to.
     *
     * @throws IOException If an error occurrs writing the value or if an
     *      error occurrs creating the {@link StatefullValue} from the
     *      delegatee.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // if the delegatee value is a StatefullValue or SerialValue, serialize it
        if (delegatee instanceof StatefulValue
                || delegatee instanceof SerialValue) {
            out.writeObject(delegatee);
            return;
        }

        // otherwise create a SerialValue from the delegatee value to send
        try {
            SerialValueFactory factory = SerialValueFactory.getInstance();
            Value toSend;
            switch (getType()) {
                case PropertyType.BINARY:
                     toSend = factory.createBinaryValue(getStream());
                    break;
                case PropertyType.BOOLEAN:
                    toSend = factory.createBooleanValue(getBoolean());
                    break;
                case PropertyType.DATE:
                    toSend = factory.createDateValue(getDate());
                    break;
                case PropertyType.DOUBLE:
                    toSend = factory.createDoubleValue(getDouble());
                    break;
                case PropertyType.LONG:
                    toSend = factory.createLongValue(getLong());
                    break;
                case PropertyType.NAME:
                    toSend = factory.createNameValue(getString());
                    break;
                case PropertyType.PATH:
                    toSend = factory.createPathValue(getString());
                    break;
                case PropertyType.REFERENCE:
                    toSend = factory.createReferenceValue(getString());
                    break;
                case PropertyType.STRING:
                    toSend = factory.createStringValue(getString());
                    break;
                default:
                    throw new IOException("Unknown value type");
            }

            // replace the delegatee with the new one
            delegatee = toSend;

            // and finally send the serial value instance
            out.writeObject(toSend);
        } catch (RepositoryException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    /**
     * Reads an reconstructs the delegatee from the given
     * <code>ObjectInputStream</code>. The value read will either be an
     * instance of {@link SerialValue} or a {@link StatefullValue} depending
     * on the original delegatee written.
     *
     * @param in The <code>ObjectInputStream</code> from which to read the
     *      delegatee.
     *
     * @throws IOException If an error occurrs reading from the
     *      <code>ObjectInputStream</code> or if the runtime class of the
     *      value to be read cannot be found.
     */
    private void readObject(ObjectInputStream in) throws IOException {
        try {
            delegatee = (Value) in.readObject();
        } catch (ClassNotFoundException cnfe) {
            throw new IOException(
                    "Cannot load value object class: " + cnfe.getMessage());
        }
    }

}
