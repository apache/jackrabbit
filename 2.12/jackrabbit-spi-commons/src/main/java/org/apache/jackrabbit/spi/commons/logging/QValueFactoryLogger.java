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
package org.apache.jackrabbit.spi.commons.logging;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.net.URI;
import java.math.BigDecimal;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;

/**
 * Log wrapper for a {@link QValueFactory}.
 */
public class QValueFactoryLogger extends AbstractLogger implements QValueFactory {
    private final QValueFactory qValueFactory;

    /**
     * Create a new instance for the given <code>qValueFactory</code> which uses
     * <code>writer</code> for persisting log messages.
     * @param qValueFactory
     * @param writer
     */
    public QValueFactoryLogger(QValueFactory qValueFactory, LogWriter writer) {
        super(writer);
        this.qValueFactory = qValueFactory;
    }

    /**
     * @return  the wrapped QValueFactory
     */
    public QValueFactory getQValueFactory() {
        return qValueFactory;
    }

    public QValue create(final String value, final int type) throws RepositoryException {
        return (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.create(value, type);
            }}, "create(String, int)", new Object[]{value, new Integer(type)});
    }

    public QValue create(final Calendar value) throws RepositoryException {
        return (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.create(value);
            }}, "create(Calendar)", new Object[]{value});
    }

    public QValue create(final double value) throws RepositoryException {
        return (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.create(value);
            }}, "create(double)", new Object[]{new Double(value)});
    }

    public QValue create(final long value) throws RepositoryException {
        return (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.create(value);
            }}, "create(long)", new Object[]{new Long(value)});
    }

    public QValue create(final boolean value) throws RepositoryException {
        return (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.create(value);
            }}, "create(boolean)", new Object[]{Boolean.valueOf(value)});
    }

    public QValue create(final Name value) throws RepositoryException {
        return (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.create(value);
            }}, "create(Name)", new Object[]{value});
    }

    public QValue create(final Path value) throws RepositoryException {
        return (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.create(value);
            }}, "create(Path)", new Object[]{value});
    }

    public QValue create(final URI value) throws RepositoryException {
        return (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.create(value);
            }}, "create(URI)", new Object[]{value});
    }

    public QValue create(final BigDecimal value) throws RepositoryException {
        return (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.create(value);
            }}, "create(BigDecimal)", new Object[]{value});
    }

    public QValue create(final byte[] value) throws RepositoryException {
        return (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.create(value);
            }}, "create(byte[])", new Object[]{value});
    }

    public QValue create(final InputStream value) throws RepositoryException, IOException {
        final String methodName = "create(InputStream)";
        final Object[] args = new Object[]{value};
        final IOException[] ex = new IOException[1];

        QValue result = (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                try {
                    return qValueFactory.create(value);
                }
                catch (IOException e) {
                    ex[0] = e;
                    return null;
                }
            }}, methodName, args);

        if (ex[0] != null) {
            throw ex[0];
        }

        return result;
    }

    public QValue create(final File value) throws RepositoryException, IOException {
        final String methodName = "create(File)";
        final Object[] args = new Object[]{value};
        final IOException[] ex = new IOException[1];

        QValue result = (QValue) execute(new Callable() {
            public Object call() throws RepositoryException {
                try {
                    return qValueFactory.create(value);
                }
                catch (IOException e) {
                    ex[0] = e;
                    return null;
                }
            }}, methodName, args);

        if (ex[0] != null) {
            throw ex[0];
        }

        return result;
    }

    public QValue[] computeAutoValues(final QPropertyDefinition propertyDefinition) throws RepositoryException {
        return (QValue[]) execute(new Callable() {
            public Object call() throws RepositoryException {
                return qValueFactory.computeAutoValues(propertyDefinition);
            }}, "computeAutoValues(QPropertyDefinition)", new Object[]{propertyDefinition});
    }

}
