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

import java.io.Writer;

import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;

/**
 * {@link LogWriterProvider} instance which provides {@link WriterLogWriter}s.
 */
public class WriterLogWriterProvider implements LogWriterProvider {

    /**
     * internal writer
     */
    private final Writer log;

    /**
     * Creates a new WriterLogWriterProvider based on the given writer
     * @param log the writer
     */
    public WriterLogWriterProvider(Writer log) {
        this.log = log;
    }

    /**
     * Returns a {@link WriterLogWriter} if the logger for <code>
     * service.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(RepositoryService service) {
        return getLogWriterInternal(log, service);
    }

    /**
     * Returns a {@link WriterLogWriter} if the logger for <code>
     * nameFactory.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(NameFactory nameFactory) {
        return getLogWriterInternal(log, nameFactory);
    }

    /**
     * Returns a {@link WriterLogWriter} if the logger for <code>
     * pathFactory.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(PathFactory pathFactory) {
        return getLogWriterInternal(log, pathFactory);
    }

    /**
     * Returns a {@link WriterLogWriter} if the logger for <code>
     * idFactory.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(IdFactory idFactory) {
        return getLogWriterInternal(log, idFactory);
    }

    /**
     * Returns a {@link WriterLogWriter} if the logger for <code>
     * valueFactory.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(QValueFactory valueFactory) {
        return getLogWriterInternal(log, valueFactory);
    }

    /**
     * Returns a {@link WriterLogWriter} if the logger for <code>
     * sessionInfo.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(SessionInfo sessionInfo) {
        return getLogWriterInternal(log, sessionInfo);
    }

    /**
     * Returns a {@link WriterLogWriter} if the logger for <code>
     * batch.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(Batch batch) {
        return getLogWriterInternal(log, batch);
    }

    // -----------------------------------------------------< private >---

    private static LogWriter getLogWriterInternal(Writer log, Object object) {
        return new WriterLogWriter(log, object.getClass().getSimpleName());
    }

}