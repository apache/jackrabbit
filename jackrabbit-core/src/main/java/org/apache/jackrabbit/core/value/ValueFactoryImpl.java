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
package org.apache.jackrabbit.core.value;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.spi.QValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.io.IOException;

/**
 * <code>ValueFactoryImpl</code>...
 */
public class ValueFactoryImpl extends ValueFactoryQImpl {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(ValueFactoryImpl.class);

    /**
     * Constructs a new <code>ValueFactoryQImpl</code> based
     * on an existing SPI <code>QValueFactory</code> and a
     * <code>NamePathResolver</code>.
     *
     * @param resolver wrapped <code>NamePathResolver</code>
     */
    public ValueFactoryImpl(NamePathResolver resolver) {
        super(InternalValueFactory.getInstance(), resolver);
    }

    public Value createValue(QValue qvalue) {
        if (qvalue instanceof InternalValue && PropertyType.BINARY == qvalue.getType()) {
            try {
                return new BinaryValueImpl(((InternalValue) qvalue).getBLOBFileValue());
            } catch (RepositoryException e) {
                // should not get here
                log.error(e.getMessage());
            }
        }
        return super.createValue(qvalue);
    }

    public Value createValue(InputStream value) {
        try {
            InternalValue qvalue = (InternalValue) getQValueFactory().create(value);
            return new BinaryValueImpl(qvalue.getBLOBFileValue());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (RepositoryException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Value createValue(String value, int type) throws ValueFormatException {
        if (PropertyType.BINARY == type) {
            try {
                InternalValue qvalue = (InternalValue) getQValueFactory().create(value, type);                
                return new BinaryValueImpl(qvalue.getBLOBFileValue());
            } catch (RepositoryException e) {
                throw new ValueFormatException(e);
            }
        } else {
            return super.createValue(value, type);
        }
    }
}