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

import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.value.AbstractQValueFactory;

import javax.jcr.RepositoryException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Calendar;
import java.net.URI;
import java.math.BigDecimal;

/**
 * <code>InternalValueFactory</code> implements a {@link QValueFactory} that
 * creates {@link InternalValue} instances for binary values.
 */
public final class InternalValueFactory extends AbstractQValueFactory {

    private static final QValueFactory INSTANCE = new InternalValueFactory(null);

    private final DataStore store;

    InternalValueFactory(DataStore store) {
        this.store = store;
    }

    public static QValueFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public QValue create(Calendar value) throws RepositoryException {
        return InternalValue.create(value);
    }

    @Override
    public QValue create(double value) throws RepositoryException {
        return InternalValue.create(value);
    }

    @Override
    public QValue create(long value) throws RepositoryException {
        return InternalValue.create(value);
    }

    @Override
    public QValue create(boolean value) throws RepositoryException {
        return InternalValue.create(value);
    }

    @Override
    public QValue create(Name value) throws RepositoryException {
        return InternalValue.create(value);
    }

    @Override
    public QValue create(Path value) throws RepositoryException {
        return InternalValue.create(value);
    }

    @Override
    public QValue create(URI value) throws RepositoryException {
        return InternalValue.create(value);
    }

    @Override
    public QValue create(BigDecimal value) throws RepositoryException {
        return InternalValue.create(value);
    }

    public QValue create(byte[] value) throws RepositoryException {
        if (store == null) {
            return InternalValue.create(value);
        } else {
            return InternalValue.create(new ByteArrayInputStream(value), store);
        }
    }

    public QValue create(InputStream value) throws RepositoryException, IOException {
        if (store == null) {
            return InternalValue.createTemporary(value);
        } else {
            return InternalValue.create(value, store);
        }
    }

    public QValue create(File value) throws RepositoryException, IOException {
        try (InputStream in = new FileInputStream(value)) {
            return create(in);
        }
    }

    @Override
    protected QValue createReference(String ref, boolean weak) {
        return InternalValue.create(new NodeId(ref), weak);
    }

    @Override
    protected QValue createString(String value) {
        return InternalValue.create(value);
    }
}