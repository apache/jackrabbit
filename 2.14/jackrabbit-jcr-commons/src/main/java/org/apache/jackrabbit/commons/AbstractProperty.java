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
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

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
      * If this property is of type <code>REFERENCE</code>,
      * <code>WEAKREFERENCE</code> or <code>PATH</code> (or convertible to one of
      * these types) this method returns the <code>Node</code> to which this
      * property refers.
      * <p>
      * If this property is of type <code>PATH</code> and it contains a relative
      * path, it is interpreted relative to the parent node of this property. For
      * example "<code>.</code>" refers to the parent node itself,
      * "<code>..</code>" to the parent of the parent node and "<code>foo</code>"
      * to a sibling node of this property.
      *
      * @return the referenced Node
      * @throws ValueFormatException  if this property cannot be converted to a
      *                               referring type (<code>REFERENCE</code>, <code>WEAKREFERENCE</code> or
      *                               <code>PATH</code>), if the property is multi-valued or if this property
      *                               is a referring type but is currently part of the frozen state of a
      *                               version in version storage.
      * @throws ItemNotFoundException If this property is of type
      *                               <code>PATH</code> or <code>WEAKREFERENCE</code> and no target node
      *                               accessible by the current <code>Session</code> exists in this workspace.
      *                               Note that this applies even if the property is a <code>PATHS</code> and a
      *                               <i>property</i> exists at the specified location. To dereference to a
      *                               target property (as opposed to a target node), the method
      *                               <code>Property.getProperty</code> is used.
      * @throws RepositoryException   if another error occurs.
      */
    public Node getNode() throws ValueFormatException, RepositoryException {
        String value = getString();

        switch (getType()) {
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return getSession().getNodeByIdentifier(value);

            case PropertyType.PATH:
                try {
                    return (value.startsWith("/")) ? getSession().getNode(value) : getParent().getNode(value);
                } catch (PathNotFoundException e) {
                    throw new ItemNotFoundException(value);
                }

            case PropertyType.NAME:
                try {
                    return getParent().getNode(value);
                } catch (PathNotFoundException e) {
                    throw new ItemNotFoundException(value);
                }

            case PropertyType.STRING:
                try {
                    // interpret as identifier
                    Value refValue = getSession().getValueFactory().createValue(value, PropertyType.REFERENCE);
                    return getSession().getNodeByIdentifier(refValue.getString());
                } catch (ItemNotFoundException e) {
                    throw e;
                } catch (RepositoryException e) {
                    // try if STRING value can be interpreted as PATH value
                    Value pathValue = getSession().getValueFactory().createValue(value, PropertyType.PATH);
                    try {
                        return (value.startsWith("/")) ? getSession().getNode(pathValue.getString()) : getParent().getNode(pathValue.getString());
                    } catch (PathNotFoundException e1) {
                        throw new ItemNotFoundException(pathValue.getString());
                    }
                }

            default:
                throw new ValueFormatException("Property value cannot be converted to a PATH, REFERENCE or WEAKREFERENCE: " + value);
        }
    }

    /**
     * If this property is of type <code>PATH</code> (or convertible to this
     * type) this method returns the <code>Property</code> to which <i>this</i>
     * property refers.
     * <p>
     * If this property contains a relative path, it is interpreted relative to
     * the parent node of this property. Therefore, when resolving such a
     * relative path, the segment "<code>.</code>" refers to
     * the parent node itself, "<code>..</code>" to the parent of the parent
     * node and "<code>foo</code>" to a sibling property of this property or
     * this property itself.
     * <p>
     * For example, if this property is located at
     * <code>/a/b/c</code> and it has a value of "<code>../d</code>" then this
     * method will return the property at <code>/a/d</code> if such exists.
     * <p>
     * If this property is multi-valued, this method throws a
     * <code>ValueFormatException</code>.
     * <p>
     * If this property cannot be converted to a <code>PATH</code> then a
     * <code>ValueFormatException</code> is thrown.
     * <p>
     * If this property is currently part of the frozen state of a version in
     * version storage, this method will throw a <code>ValueFormatException</code>.
     *
     * @return the referenced property
     * @throws ValueFormatException  if this property cannot be converted to a
     *                               <code>PATH</code>, if the property is multi-valued or if this property is
     *                               a referring type but is currently part of the frozen state of a version
     *                               in version storage.
     * @throws ItemNotFoundException If no property accessible by the current
     *                               <code>Session</code> exists in this workspace at the specified path. Note
     *                               that this applies even if a <i>node</i> exists at the specified location.
     *                               To dereference to a target node, the method <code>Property.getNode</code>
     *                               is used.
     * @throws RepositoryException   if another error occurs.
     */
    public Property getProperty() throws RepositoryException {
        String value = getString();
        switch (getType()) {
            case PropertyType.PATH:
                try {
                    return (value.startsWith("/")) ? getSession().getProperty(value) : getParent().getProperty(value);
                } catch (PathNotFoundException e) {
                    throw new ItemNotFoundException(value);
                }

            case PropertyType.NAME:
                try {
                    return getParent().getProperty(value);
                } catch (PathNotFoundException e) {
                    throw new ItemNotFoundException(value);
                }

            default:
                try {
                    String path = getSession().getValueFactory().createValue(value, PropertyType.PATH).getString();
                    return (path.startsWith("/")) ? getSession().getProperty(path) : getParent().getProperty(path);
                } catch (PathNotFoundException e) {
                    throw new ItemNotFoundException(value);
                }
        }
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
