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
package org.apache.jackrabbit.commons;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * Abstract base class for implementing the JCR {@link Property} interface.
 * <p>
 * {@link Item} methods <em>without</em> a default implementation:
 * <ul>
 *   <li>{@link Item#getName()}</li>
 *   <li>{@link Item#getParent()}</li>
 *   <li>{@link Item#getSession()}</li>
 *   <li>{@link Item#isModified()}</li>
 *   <li>{@link Item#isNew()}</li>
 *   <li>{@link Item#isSame(Item)}</li>
 *   <li>{@link Item#refresh(boolean)}</li>
 *   <li>{@link Item#save()}</li>
 * </ul>
 * <p>
 * {@link Property} methods <em>without</em> a default implementation:
 * <ul>
 *   <li>{@link Property#getDefinition()}</li>
 *   <li>{@link Property#getValue()}</li>
 *   <li>{@link Property#getValues()}</li>
 * </ul>
 * <p>
 * <strong>NOTE:</strong> Many of the default method implementations in
 * this base class rely on the parent node being accessible through the
 * {@link Item#getParent()} call. It is possible (though unlikely) that
 * access controls deny access to a containing node even though a property
 * is accessible. In such cases the default method implementations in this
 * class <em>will not work</em>.
 */
public abstract class AbstractProperty extends AbstractItem
        implements Item, Property {

    //----------------------------------------------------------------< Item >

    /**
     * Accepts the given item visitor.
     * <p>
     * The default implementation calls {@link ItemVisitor#visit(Property)}
     * on the given visitor with this property as the argument.
     *
     * @param visitor item visitor
     * @throws RepositoryException if an error occurs
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    /**
     * Returns the path of this property.
     * <p>
     * The default implementation constructs the path from the path of the
     * parent node and the name of this property.
     *
     * @return property path
     * @throws RepositoryException if an error occurs
     */
    public String getPath() throws RepositoryException {
        StringBuffer buffer = new StringBuffer(getParent().getPath());
        if (buffer.length() > 1) {
            buffer.append('/');
        }
        buffer.append(getName());
        return buffer.toString();
    }

    /**
     * Returns <code>false</code>.
     *
     * @return <code>false</code>
     */
    public boolean isNode() {
        return false;
    }

    /**
     * Removes this property.
     * <p>
     * The default implementation calls {@link Node#setProperty(String, Value)}
     * with a <code>null</code> value on the parent node.
     *
     * @throws RepositoryException if an error occurs
     */
    public void remove() throws RepositoryException {
        getParent().setProperty(getName(), (Value) null);
    }

    //------------------------------------------------------------< Property >

    /**
     * Returns the boolean value of this property.
     * <p>
     * The default implementation forwards the method call to the
     * {@link Value} instance returned by the generic
     * {@link Property#getValue()} method.
     *
     * @return boolean value
     * @throws RepositoryException if an error occurs
     */
    public boolean getBoolean() throws RepositoryException {
        return getValue().getBoolean();
    }

    /**
     * Returns the date value of this property.
     * <p>
     * The default implementation forwards the method call to the
     * {@link Value} instance returned by the generic
     * {@link Property#getValue()} method.
     *
     * @return date value
     * @throws RepositoryException if an error occurs
     */
    public Calendar getDate() throws RepositoryException {
        return getValue().getDate();
    }

    /**
     * Returns the double value of this property.
     * <p>
     * The default implementation forwards the method call to the
     * {@link Value} instance returned by the generic
     * {@link Property#getValue()} method.
     *
     * @return double value
     * @throws RepositoryException if an error occurs
     */
    public double getDouble() throws RepositoryException {
        return getValue().getDouble();
    }

    /**
     * Returns the length of the value of this property.
     * <p>
     * The default implementation measures the length of the {@link Value}
     * instance returned by the generic {@link Property#getValue()} method.
     *
     * @return length of the property value
     * @throws RepositoryException if an error occurs
     */
    public long getLength() throws RepositoryException {
        return getLength(getValue());
    }

    /**
     * Returns the lengths of the values of this property.
     * <p>
     * The default implementation measures the lengths of the {@link Value}
     * instances returned by the generic {@link Property#getValues()} method.
     *
     * @return lengths of the property values
     * @throws RepositoryException if an error occurs
     */
    public long[] getLengths() throws RepositoryException {
        Value[] values = getValues();
        long[] lengths = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            lengths[i] = getLength(values[i]);
        }
        return lengths;
    }

    /**
     * Returns the long value of this property.
     * <p>
     * The default implementation forwards the method call to the
     * {@link Value} instance returned by the generic
     * {@link Property#getValue()} method.
     *
     * @return long value
     * @throws RepositoryException if an error occurs
     */
    public long getLong() throws RepositoryException {
        return getValue().getLong();
    }

    /**
     * Returns the node referenced by this property.
     * <p>
     * The default implementation checks that this property is a reference
     * property (or tries to convert the property value to a reference) and
     * uses {@link Session#getNodeByUUID(String)} to retrieve the
     * referenced node.
     *
     * @return node referenced by this property
     * @throws RepositoryException if an error occurs
     */
    public Node getNode() throws RepositoryException {
        Session session = getSession();
        Value value = getValue();
        if (value.getType() != PropertyType.REFERENCE
                && value.getType() != PropertyType.WEAKREFERENCE) {
            value = session.getValueFactory().createValue(
                    value.getString(), PropertyType.REFERENCE);
        }
        return session.getNodeByUUID(value.getString());
    }

    /**
     * Returns the binary value of this property.
     * <p>
     * The default implementation forwards the method call to the
     * {@link Value} instance returned by the generic
     * {@link Property#getValue()} method.
     *
     * @return binary value
     * @throws RepositoryException if an error occurs
     */
    public InputStream getStream() throws RepositoryException {
        return getValue().getStream();
    }

    /**
     * Returns the string value of this property.
     * <p>
     * The default implementation forwards the method call to the
     * {@link Value} instance returned by the generic
     * {@link Property#getValue()} method.
     *
     * @return string value
     * @throws RepositoryException if an error occurs
     */
    public String getString() throws RepositoryException {
        return getValue().getString();
    }

    /**
     * Returns the type of this property.
     * <p>
     * The default implementation forwards the method call to the
     * {@link Value} instance returned by the generic
     * {@link Property#getValue()} method.
     *
     * @return property type
     * @throws RepositoryException if an error occurs
     */
    public int getType() throws RepositoryException {
        return getValue().getType();
    }

    /**
     * Sets the value of this property.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#setProperty(String, Value)} method of the parent node
     * using the name of this property.
     *
     * @param value passed through
     * @throws RepositoryException if an error occurs
     */
    public void setValue(Value value) throws RepositoryException {
        getParent().setProperty(getName(), value);
    }

    /**
     * Sets the values of this property.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#setProperty(String, Value[])} method of the parent node
     * using the name of this property.
     *
     * @param values passed through
     * @throws RepositoryException if an error occurs
     */
    public void setValue(Value[] values) throws RepositoryException {
        getParent().setProperty(getName(), values);
    }

    /**
     * Sets the value of this property.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#setProperty(String, String)} method of the parent node
     * using the name of this property.
     *
     * @param value passed through
     * @throws RepositoryException if an error occurs
     */
    public void setValue(String value) throws RepositoryException {
        getParent().setProperty(getName(), value);
    }

    /**
     * Sets the values of this property.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#setProperty(String, String[])} method of the parent node
     * using the name of this property.
     *
     * @param values passed through
     * @throws RepositoryException if an error occurs
     */
    public void setValue(String[] values) throws RepositoryException {
        getParent().setProperty(getName(), values);
    }

    /**
     * Sets the value of this property.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#setProperty(String, InputStream)} method of the parent node
     * using the name of this property.
     *
     * @param value passed through
     * @throws RepositoryException if an error occurs
     */
    public void setValue(InputStream value) throws RepositoryException {
        getParent().setProperty(getName(), value);
    }

    /**
     * Sets the value of this property.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#setProperty(String, long)} method of the parent node
     * using the name of this property.
     *
     * @param value passed through
     * @throws RepositoryException if an error occurs
     */
    public void setValue(long value) throws RepositoryException {
        getParent().setProperty(getName(), value);
    }

    /**
     * Sets the value of this property.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#setProperty(String, double)} method of the parent node
     * using the name of this property.
     *
     * @param value passed through
     * @throws RepositoryException if an error occurs
     */
    public void setValue(double value) throws RepositoryException {
        getParent().setProperty(getName(), value);
    }

    /**
     * Sets the value of this property.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#setProperty(String, Calendar)} method of the parent node
     * using the name of this property.
     *
     * @param value passed through
     * @throws RepositoryException if an error occurs
     */
    public void setValue(Calendar value) throws RepositoryException {
        getParent().setProperty(getName(), value);
    }

    /**
     * Sets the value of this property.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#setProperty(String, boolean)} method of the parent node
     * using the name of this property.
     *
     * @param value passed through
     * @throws RepositoryException if an error occurs
     */
    public void setValue(boolean value) throws RepositoryException {
        getParent().setProperty(getName(), value);
    }

    /**
     * Sets the value of this property.
     * <p>
     * The default implementation forwards the call to the
     * {@link Node#setProperty(String, Node)} method of the parent node
     * using the name of this property.
     *
     * @param value passed through
     * @throws RepositoryException if an error occurs
     */
    public void setValue(Node value) throws RepositoryException {
        getParent().setProperty(getName(), value);
    }

    //-------------------------------------------------------------< private >

    /**
     * Returns the length of the given value.
     *
     * @param value value
     * @return length of the value
     * @throws RepositoryException if an error occurs
     */
    private long getLength(Value value) throws RepositoryException {
        if (value.getType() != PropertyType.BINARY) {
            return value.getString().length();
        } else {
            try {
                InputStream stream = value.getStream();
                try {
                    long length = 0;
                    byte[] buffer = new byte[4096];
                    int n = stream.read(buffer);
                    while (n != -1) {
                        length += n;
                        n = stream.read(buffer);
                    }
                    return length;
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
                throw new RepositoryException(
                        "Failed to count the length of a binary value", e);
            }
        }
    }

}
