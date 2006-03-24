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


require_once 'PHPCR/ValueFormatException.php';
require_once 'PHPCR/IllegalStateException.php';
require_once 'PHPCR/RepositoryException.php';


/**
 * A generic holder for the value of a property. A <code>Value</code> object can be used without knowing the actual
 * property type (<code>STRING</code>, <code>DOUBLE</code>, <code>BINARY</code> etc.).
 * <p>
 * Any implementation of this interface must match the behavior of the JCR-supplied classes ({@link BaseValue} and its
 * subclasses) in the following respects:
 * <ul>
 *   <li>
 *     A <code>Value</code> object returned by <code>Property.getValue()</code> can be read using type-specific
 *     <code>get</code> methods. These methods are divided into two groups:
 *     <ul>
 *       <li>
 *         The non-stream <code>get</code> methods <code>getString()</code>, <code>getDate()</code>,
 *         <code>getLong()</code>, <code>getDouble()</code> and <code>getBoolean()</code>.
 *       </li>
 *       <li>
 *          <code>getStream()</code>.
 *       </li>
 *     </ul>
 *    </li>
 *   <li>
 *     Once a <code>Value</code> object has been read once using <code>getStream()</code>, all subsequent calls to
 *     <code>getStream()</code> will return the same <code>Stream</code> object. This may mean, for example, that the
 *     stream returned is fully or partially consumed. In order to get a fresh stream the <code>Value</code> object
 *     must be reacquired via {@link Property#getValue()} or {@link Property#getValues()}.
 *   </li>
 *   <li>
 *     Once a <code>Value</code> object has been read once using <code>getStream()</code>, any subsequent call to any
 *     of the non-stream <code>get</code> methods will throw an <code>IllegalStateException</code>. In order to
 *     successfully invoke a non-stream <code>get</code> method, the <code>Value</code> must be reacquired.
 *   </li>
 *   <li>
 *     Once a <code>Value</code> object has been read once using a non-stream get method, any subsequent call to
 *     <code>getStream()</code> will throw an <code>IllegalStateException</code>. In order to successfully invoke
 *     <code>getStream()</code>, the <code>Value</code> must be reacquired.
 * </ul>
 * An implementation that obeys these restrictions can be found in the class {@link BaseValue} and its subclasses
 * {@link StringValue}, {@link LongValue}, {@link DoubleValue}, {@link BooleanValue}, {@link DateValue},
 * {@link BinaryValue}, {@link NameValue}, {@link PathValue} and {@link ReferenceValue}.
 * <p/>
 * Two <code>Value</code> instances, <code>v1</code> and <code>v2</code>, are considered equal if and only if:
 * <ul>
 * <li><code>v1.getType() == v2.getType()</code>, and,</li>
 * <li><code>v1.getString().equals(v2.getString())</code></li>
 * </ul>
 * Actually comparing two <code>Value</code> instances by converting them to
 * string form may not be practical in some cases (for example, if the values are very large
 * binaries). Consequently, the above is intended as a normative definition of <code>Value</code> equality
 * but not as a procedural test of equality. It is assumed that implementations will have efficient means
 * of determining equality that conform with the above definition.
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 */
interface Value
{
    /**
     * Returns a <code>String</code> representation of this value.
     * <p>
     * If this value cannot be converted to a string, a
     * <code>ValueFormatException</code> is thrown.
     * <p>
     * If <code>getStream</code> has previously been called on this
     * <code>Value</code> instance, an <code>IllegalStateException</code> is thrown.
     * In this case a new <code>Value</code> instance must be acquired in order to
     * successfully call <code>getString</code>.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return A <code>String</code> representation of the value of this property.
     * @throws ValueFormatException if conversion to a <code>String</code> is not possible.
     * @throws IllegalStateException if <code>getStream</code> has previously
     * been called on this <code>Value</code> instance.
     * @throws RepositoryException if another error occurs.
     */
    public function getString();

    /**
     * Returns an <code>InputStream</code> representation of this value.
     * USes the standard conversion to binary (see JCR specification)<p>
     * If this value cannot be converted to a stream, a
     * <code>ValueFormatException</code> is thrown.
     * <p>
     * If a non-stream <code>get</code> method has previously been called on this
     * <code>Value</code> instance, an <code>IllegalStateException</code> is thrown.
     * In this case a new <code>Value</code> instance must be acquired in order to successfully call
     * <code>getStream</code>.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return An <code>InputStream</code> representation of this value.
     * @throws ValueFormatException if conversion to an <code>InputStream</code> is not possible.
     * @throws IllegalStateException if a non-stream <code>get</code> method has previously
     * been called on this <code>Value</code> instance.
     * @throws RepositoryException if another error occurs.
     */
    public function getStream();

    /**
     * Returns a <code>long</code> representation of this value.
     * <p>
     * If this value cannot be converted to a <code>long</code>, a
     * <code>ValueFormatException</code> is thrown.
     * <p>
     * If <code>getStream</code> has previously been called on this
     * <code>Value</code> instance, an <code>IllegalStateException</code> is thrown.
     * In this case a new <code>Value</code> instance must be acquired in order to
     * successfully call <code>getLong</code>.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return A <code>long</code> representation of this value.
     * @throws ValueFormatException if conversion to a <code>long</code> is not possible.
     * @throws IllegalStateException if <code>getStream</code> has previously
     * been called on this <code>Value</code> instance.
     * @throws RepositoryException if another error occurs.
     */
    public function getLong();

    /**
     * Returns a <code>double</code> representation of this value.
     * <p>
     * If this value cannot be converted to a <code>double</code>, a
     * <code>ValueFormatException</code> is thrown.
     * <p>
     * If <code>getStream</code> has previously been called on this
     * <code>Value</code> instance, an <code>IllegalStateException</code> is thrown.
     * In this case a new <code>Value</code> instance must be acquired in order to
     * successfully call <code>getDouble</code>.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return A <code>double</code> representation of this value.
     * @throws ValueFormatException if conversion to a <code>double</code> is not possible.
     * @throws IllegalStateException if <code>getStream</code> has previously
     * been called on this <code>Value</code> instance.
     * @throws RepositoryException if another error occurs.
     */
    public function getDouble();

    /**
     * Returns date representation of this value.
     * <p>
     * If this value cannot be converted to a <code>Calendar</code>, a
     * <code>ValueFormatException</code> is thrown.
     * <p>
     * If <code>getStream</code> has previously been called on this
     * <code>Value</code> instance, an <code>IllegalStateException</code> is thrown.
     * In this case a new <code>Value</code> instance must be acquired in order to
     * successfully call <code>getDate</code>.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return date
     * @throws ValueFormatException if conversion to a <code>Calendar</code> is not possible.
     * @throws IllegalStateException if <code>getStream</code> has previously
     * been called on this <code>Value</code> instance.
     * @throws RepositoryException if another error occurs.
     */
    public function getDate();

    /**
     * Returns a <code>Boolean</code> representation of this value.
     * <p>
     * If this value cannot be converted to a <code>Boolean</code>, a
     * <code>ValueFormatException</code> is thrown.
     * <p>
     * If <code>getStream</code> has previously been called on this
     * <code>Value</code> instance, an <code>IllegalStateException</code> is thrown.
     * In this case a new <code>Value</code> instance must be acquired in order to
     * successfully call <code>getBoolean</code>.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @return A <code>Boolean</code> representation of this value.
     * @throws ValueFormatException if conversion to a <code>Boolean</code> is not possible.
     * @throws IllegalStateException if <code>getStream</code> has previously
     * been called on this <code>Value</code> instance.
     * @throws RepositoryException if another error occurs.
     */
    public function getBoolean();

    /**
     * Returns the <code>type</code> of this <code>Value</code>.
     * One of:
     * <ul>
     * <li><code>PropertyType::STRING</code></li>
     * <li><code>PropertyType::DATE</code></li>
     * <li><code>PropertyType::BINARY</code></li>
     * <li><code>PropertyType::DOUBLE</code></li>
     * <li><code>PropertyType::LONG</code></li>
     * <li><code>PropertyType::BOOLEAN</code></li>
     * <li><code>PropertyType::NAME</code></li>
     * <li><code>PropertyType::PATH</code></li>
     * <li><code>PropertyType::REFERENCE</code></li>
     * </ul>
     * See <code>{@link PropertyType}</code>.
     * <p>
     * The type returned is that which was set at property creation.
     */
    public function getType();
}

?>