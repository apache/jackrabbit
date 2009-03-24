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

import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LogWriterProvider} instance which provides {@link Slf4jLogWriter}s.
 */
public class Slf4jLogWriterProvider implements LogWriterProvider {

    /**
     * Returns a {@link Slf4jLogWriter} if the logger for <code>
     * service.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(RepositoryService service) {
        return getLogWriterInternal(service);
    }

    /**
     * Returns a {@link Slf4jLogWriter} if the logger for <code>
     * nameFactory.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(NameFactory nameFactory) {
        return getLogWriterInternal(nameFactory);
    }

    /**
     * Returns a {@link Slf4jLogWriter} if the logger for <code>
     * pathFactory.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(PathFactory pathFactory) {
        return getLogWriterInternal(pathFactory);
    }

    /**
     * Returns a {@link Slf4jLogWriter} if the logger for <code>
     * idFactory.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(IdFactory idFactory) {
        return getLogWriterInternal(idFactory);
    }

    /**
     * Returns a {@link Slf4jLogWriter} if the logger for <code>
     * valueFactory.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(QValueFactory valueFactory) {
        return getLogWriterInternal(valueFactory);
    }

    /**
     * Returns a {@link Slf4jLogWriter} if the logger for <code>
     * sessionInfo.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(SessionInfo sessionInfo) {
        return getLogWriterInternal(sessionInfo);
    }

    /**
     * Returns a {@link Slf4jLogWriter} if the logger for <code>
     * batch.getClass()</code> has debug level enabled. Returns
     * <code>null</code> otherwise.
     * {@inheritDoc}
     */
    public LogWriter getLogWriter(Batch batch) {
        return getLogWriterInternal(batch);
    }

    // -----------------------------------------------------< private >---

    private static LogWriter getLogWriterInternal(Object object) {
        Logger log = LoggerFactory.getLogger(object.getClass());
        return log.isDebugEnabled()
            ? new Slf4jLogWriter(log)
            : null;
    }

}
