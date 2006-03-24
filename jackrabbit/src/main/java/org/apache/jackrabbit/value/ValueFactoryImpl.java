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
package org.apache.jackrabbit.value;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * This class implements the  <code>ValueFactory</code> interface.
 *
 * @see javax.jcr.Session#getValueFactory()
 */
public class ValueFactoryImpl implements ValueFactory {

    /**
     * Constructs a <code>ValueFactory</code> object.
     */
    public ValueFactoryImpl() {
    }

    //---------------------------------------------------------< ValueFactory >
    /**
     * {@inheritDoc}
     */
    public Value createValue(boolean value) {
        return new BooleanValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(Calendar value) {
        return new DateValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(double value) {
        return new DoubleValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(InputStream value) {
        return new BinaryValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(long value) {
        return new LongValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(Node value) throws RepositoryException {
        return new ReferenceValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(String value) {
        return new StringValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Value createValue(String value, int type)
            throws ValueFormatException {
        return ValueHelper.convert(value, type);
    }
}
