<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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


require_once 'PHPCR/Item.php';
require_once 'PHPCR/Node.php';
require_once 'PHPCR/RepositoryException.php';
require_once 'PHPCR/ValueFormatException.php';
require_once 'PHPCR/nodetype/PropertyDefinition.php';
require_once 'PHPCR/version/VersionException.php';
require_once 'PHPCR/lock/LockException.php';
require_once 'PHPCR/nodetype/ConstraintViolationException.php';


/**
 * A <code>Property</code> object represents the smallest granularity of content
 * storage.
 * <p>
 * <b>Level 1 and 2</b>
 * <p>
 * A property must have one and only one parent node. A property does
 * not have children. When we say that node A "has" property B it means that B
 * is a child of A.
 * <p>
 * A property consists of a name and a value. See <code>Value</code>.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
interface Property extends Item
{
    /**
     * Sets the value of this property to <code>value</code>.
     * If this property's property type is not constrained by the node type of
     * its parent node, then the property type is changed to that of the supplied
     * <code>value</code>. If the property type is constrained, then a
     * best-effort conversion is attempted. If conversion fails, a
     * <code>ValueFormatException</code> is thrown immediately (not on <code>save</code>).
     * The change will be persisted (if valid) on <code>save</code>
     * <p/>
     * A <code>ConstraintViolationException</code> is thrown if a node-type
     * or other constraint violation is detected immediately. Otherwise,
     * if the violation is only detected later, at <code>save</code>, then a
     * <code>ConstraintViolationException</code> is thrown by that method.
     * Implementations may differ as to which constraints are enforced immediately,
     * and which on <code>save</code>.
     * <p/>
     * A <code>VersionException</code> is thrown if the parent node of this property is
     * versionable and checked-in or is non-versionable but its nearest versionable
     * ancestor is checked-in.
     * <p/>
     * A <code>LockException</code> is thrown if a lock prevents the setting of the value.
     *
     * @throws ValueFormatException if the type or format of the specified value
     * is incompatible with the type of this property.
     * @throws VersionException if the parent node of this property is
     * versionable and checked-in or is non-versionable but its nearest versionable
     * ancestor is checked-in.
     * @throws LockException if a lock prevents the setting of the value.
     * @throws ConstraintViolationException if a node-type or other constraint violation is detected immediately.
     * @throws RepositoryException if another error occurs.
     */
    public function setValue( $value );

    /**
     * Returns the value of this  property as a generic
     * <code>Value</code> object.
     * <p>
     * If the property is multi-valued, this method throws a <code>ValueFormatException</code>.
     *
     * @throws ValueFormatException if the property is multi-valued.
     * @throws RepositoryException if another error occurs.
     *
     * @return the value
     */
    public function getValue();

    /**
     * Returns an array of all the values of this property. Used to access
     * multi-value properties. If the property is single-valued, this method throws a
     * <code>ValueFormatException</code>. The array returned is a copy of the stored
     * values, so changes to it are not reflected in internal storage.
     *
     * @throws ValueFormatException if the property is single-valued.
     * @throws RepositoryException if another error occurs.
     *
     * @return a <code>Value</code> array
     */
    public function getValues();

    /**
     * Returns a <code>String</code> representation of the value of this
     * property. A shortcut for
     * <code>Property.getValue().getString()</code>. See {@link Value}.
     * If this property is multi-valued, this method throws a <code>ValueFormatException</code>.
     * <p>
     * If the value of this property cannot be converted to a
     * string, a <code>ValueFormatException</code> is thrown.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return A string representation of the value of this property.
     * @throws ValueFormatException if conversion to a string is not possible or if the
     * property is multi-valued.
     * @throws RepositoryException if another error occurs.
     */
    public function getString();

    /**
     * Returns an <code>InputStream</code> representation of the value of this
     * property. A shortcut for
     * <code>Property.getValue().getStream()</code>. See {@link Value}.
     * If this property is multi-valued, this method throws a <code>ValueFormatException</code>.
     * <p>
     * If the value of this property cannot be converted to a
     * stream, a <code>ValueFormatException</code> is thrown.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return A stream representation of the value of this property.
     * @throws ValueFormatException if conversion to a string is not possible or if the
     * property is multi-valued.
     * @throws RepositoryException if another error occurs
     */
    public function getStream();

    /**
     * Returns a <code>long</code> representation of the value of this
     * property. A shortcut for
     * <code>Property.getValue().getLong()</code>. See {@link Value}.
     * If this property is multi-valued, this method throws a <code>ValueFormatException</code>.
     * <p>
     * If the value of this property cannot be converted to a
     * <code>long</code>, a <code>ValueFormatException</code> is thrown.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return A <code>long</code> representation of the value of this property.
     * @throws ValueFormatException if conversion to a string is not possible or if the
     * property is multi-valued.
     * @throws RepositoryException if another error occurs
     */
    public function getLong();

    /**
     * Returns a <code>double</code> representation of the value of this
     * property. A shortcut for
     * <code>Property.getValue().getDouble()</code>. See {@link Value}.
     * If this property is multi-valued, this method throws a <code>ValueFormatException</code>.
     * <p>
     * If the value of this property cannot be converted to a
     * double, a <code>ValueFormatException</code> is thrown.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return A double representation of the value of this property.
     * @throws ValueFormatException if conversion to a string is not possible or if the
     * property is multi-valued.
     * @throws RepositoryException if another error occurs
     */
    public function getDouble();

    /**
     * Returns date. A shortcut for
     * <code>Property.getValue().getDate()</code>. See {@link Value}.
     * If this property is multi-valued, this method throws a <code>ValueFormatException</code>.
     * <p>
     * If the value of this property cannot be converted to a
     * date, a <code>ValueFormatException</code> is thrown.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return A date (<code>Calendar</code> object)  representation of the value of this property.
     * @throws ValueFormatException if conversion to a string is not possible or if the
     * property is multi-valued.
     * @throws RepositoryException if another error occurs
     */
    public function getDate();

    /**
     * Returns a <code>boolean</code> representation of the value of this
     * property. A shortcut for
     * <code>Property.getValue().getBoolean()</code>. See {@link Value}.
     * If this property is multi-valued, this method throws a <code>ValueFormatException</code>.
     * <p>
     * If the value of this property cannot be converted to a
     * boolean, a <code>ValueFormatException</code> is thrown.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return A boolean representation of the value of this property.
     * @throws ValueFormatException if conversion to a string is not possible or if the
     * property is multi-valued.
     * @throws RepositoryException if another error occurs
     */
    public function getBoolean();

    /**
     * If this property is of type <code>REFERENCE</code>
     * this method returns the node to which this property refers.
     * <p>
     * If this property is multi-valued, this method throws a <code>ValueFormatException</code>.
     * <p>
     * If this property cannot be coverted to a reference, then a <code>ValueFormatException</code> is thrown.
     * <p>
     * If this property is a REFERENCE property but is currently part of the frozen state of a version in version
     * storage, this method will throw a <code>ValueFormatException</code>.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return the referenced Node
     * @throws ValueFormatException if this property cannot be converted to a reference, if the
     * property is multi-valued or if this property is a REFERENCE property but is currently part of the frozen
     * state of a version in version storage.
     * @throws RepositoryException if another error occurs
     */
    public function getNode();

    /**
     * Returns the length of the value of this property.
     * <p>
     * Returns the length in bytes if the value
     * is a <code>PropertyType.BINARY</code>, otherwise it returns the number
     * of characters needed to display the value (for strings this is the string
     * length, for numeric types it is the number of characters needed to
     * display the number). Returns �1 if the implementation cannot determine
     * the length.
     *
     * If this property is multi-valued, this method throws a <code>ValueFormatException</code>.
     *
     * @return an <code>long</code>.
     * @throws ValueFormatException if this property is multi-valued.
     * @throws RepositoryException if another error occurs.
     */
    public function getLength();

    /**
     * Returns an array holding the lengths of the values of this (multi-value) property in bytes
     * if the values are <code>PropertyType.BINARY</code>, otherwise it returns the number of
     * characters needed to display each value (for strings this is the string length, for
     * numeric types it is the number of characters needed to display the number). The order of the
     * length values corresponds to the order of the values in the property.
     * <p/>
     * Returns a <code>�1</code> in the appropriate position if the implementation cannot determine
     * the length of a value.
     * <p/>
     * If this property is single-valued, this method throws a <code>ValueFormatException</code>.
     * <p/>
     * A RepositoryException is thrown if another error occurs.
     * @return an array of lengths
     * @throws ValueFormatException if this property is single-valued.
     * @throws RepositoryException if another error occurs.
     */
    public function getLengths();

    /**
     * Returns the property definition that applies to this property. In some cases there may appear to
     * be more than one definition that could apply to this node. However, it is assumed that upon
     * creation of this property, a single particular definition was used and it is <i>that</i>
     * definition that this method returns. How this governing definition is selected upon property
     * creation from among others which may have been applicable is an implemention issue and is not
     * covered by this specification.
     *
     * @see NodeType#getPropertyDefinitions
     * @throws RepositoryException if an error occurs.
     * @return a <code>PropertyDefinition</code> object.
     */
    public function getDefinition();

    /**
     * Returns the type of this <code>Property</code>. One of:
     * <ul>
     * <li><code>PropertyType::STRING</code></li>
     * <li><code>PropertyType::BINARY</code></li>
     * <li><code>PropertyType::DATE</code></li>
     * <li><code>PropertyType::DOUBLE</code></li>
     * <li><code>PropertyType::LONG</code></li>
     * <li><code>PropertyType::BOOLEAN</code></li>
     * <li><code>PropertyType::NAME</code></li>
     * <li><code>PropertyType::PATH</code></li>
     * <li><code>PropertyType::REFERENCE</code></li>
     * </ul>
     * The type returned is that which was set at property creation. Note that for some property <code>p</code>,
     * the type returned by <code>p.getType()</code> may differ from the type returned by
     * <code>p.getDefinition.getRequiredType()</code> only in the case where the latter returns <code>UNDEFINED</code>.
     * The type of a property instance is never <code>UNDEFINED</code> (it must always have some actual type).
     *
     * @return an int
     * @throws RepositoryException if an error occurs
     */
    public function getType();
}

?>