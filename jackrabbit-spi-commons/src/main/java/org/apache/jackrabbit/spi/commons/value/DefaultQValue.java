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
package org.apache.jackrabbit.spi.commons.value;

import java.io.Serializable;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * <code>QValue</code> implementation for all valid <code>PropertyType</code>s
 * except for BINARY.
 */
public class DefaultQValue extends AbstractQValue implements Serializable {

    private static final long serialVersionUID = -3887529703765183611L;

    protected static final QValue TRUE = new DefaultQValue(Boolean.TRUE);
    protected static final QValue FALSE = new DefaultQValue(Boolean.FALSE);

    public DefaultQValue(String value, int type) {
        super(value, type);
    }

    public DefaultQValue(Long value) {
        super(value);
    }

    public DefaultQValue(Double value) {
        super(value);
    }

    public DefaultQValue(BigDecimal value) {
        super(value);
    }

    public DefaultQValue(Boolean value) {
        super(value);
    }

    public DefaultQValue(Name value) {
        super(value);
    }

    public DefaultQValue(Path value) {
        super(value);
    }

    public DefaultQValue(URI value) {
        super(value);
    }

    protected DefaultQValue(Calendar value) {
        super(value);
    }

    //-------------------------------------------------------------< QValue >---

    /**
     * @see QValue#getStream()
     */
    public InputStream getStream() throws RepositoryException {
        try {
            // convert via string
            return new ByteArrayInputStream(getString().getBytes(
                    AbstractQValueFactory.DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException(QValueFactoryImpl.DEFAULT_ENCODING +
                    " is not supported encoding on this platform", e);
        }
    }
}
