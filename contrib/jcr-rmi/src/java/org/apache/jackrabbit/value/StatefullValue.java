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

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * The <code>StatefullValue</code> interface defines the API used for the state
 * classes used by the {@link org.apache.jackrabbit.value.SerialValue} class.
 * <p>
 * This interface resembles the JCR Value interface but is only used internally
 * to the State pattern implementation of the
 * {@link org.apache.jackrabbit.value.SerialValue} class.
 * <p>
 * This interface is not intended to be implemented by clients. Rather any of
 * the concrete implementations of this class should be used or overwritten as
 * appropriate.
 * 
 * @version $Revision$, $Date$
 * @author Felix Meschberger
 * @since 0.16.4.1
 * 
 * @see org.apache.jackrabbit.value.SerialValue
 */
public interface StatefullValue {

    /**
     * Returns access to the underlying value through an
     * <code>InputStream</code>.
     * 
     * @throws ValueFormatException If a conversion of this value to an
     *      <code>InputStream</code> is not possible.
     * @throws IllegalStateException if the implementation is not in stream
     *      providing state.
     */
    public InputStream getStream() throws ValueFormatException, RepositoryException;
    
    /**
     * Returns the <code>boolean</code> representation of this value.
     * 
     * @throws ValueFormatException If a conversion of this value to the
     *      boolean type is not possible.
     * @throws IllegalStateException if the implementation is in stream
     *      providing state.
     */
    public boolean getBoolean() throws ValueFormatException, RepositoryException;
    
    /**
     * Returns the <code>Calendar</code> representation of this value.
     * 
     * @throws ValueFormatException If a conversion of this value to a
     *      <code>Calendar</code> is not possible.
     * @throws IllegalStateException if the implementation is in stream
     *      providing state.
     */
    public Calendar getDate() throws ValueFormatException, RepositoryException;
    
    /**
     * Returns the <code>double</code> representation of this value.
     * 
     * @throws ValueFormatException If a conversion of this value to the
     *      double type is not possible.
     * @throws IllegalStateException if the implementation is in stream
     *      providing state.
     */
    public double getDouble() throws ValueFormatException, RepositoryException;
    
    /**
     * Returns the <code>long</code> representation of this value.
     * 
     * @throws ValueFormatException If a conversion of this value to the
     *      long type is not possible.
     * @throws IllegalStateException if the implementation is in stream
     *      providing state.
     */
    public long getLong() throws ValueFormatException, RepositoryException;
    
    /**
     * Returns the <code>String</code> representation of this value.
     * 
     * @throws ValueFormatException If a conversion of this value to the
     *      string type is not possible.
     * @throws IllegalStateException if the implementation is in stream
     *      providing state.
     */
    public String getString() throws ValueFormatException, RepositoryException;
    
    /**
     * Returns the primary type of this value, which is one of the type
     * codes defined in the <code>javax.jcr.PropertyType</code> interface.
     */
    public int getType();
}
