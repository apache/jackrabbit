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

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;

/**
 * Static factory for creating instances of the various spi loggers derived from
 * {@link AbstractLogger}.
 * In the most simple case
 * <pre>
 *   RepositoryService loggingService = SpiLoggerFactory.create(service);
 * </pre>
 * creates a log wrapper for <code>service</code> which logs all calls to its methods
 * if logging at the debug level is enabled. If logging is not enabled, no log wrapper
 * is created at all and <code>service</code> itself is returned. There is thus virtually
 * no overhead from disabled loggers. Loggers are enabled and disabled via the
 * configuration mechanism of the logging framework which is in place.
 * <p>
 * There are log wrappers for the following SPI entities:
 * <table><caption>Log Wrappers</caption>
 * <tr><th>SPI entity</th><th>log wrapper</th></tr>
 * <tr><td>{@link RepositoryService}</td><td>{@link RepositoryServiceLogger}</td></tr>
 * <tr><td>{@link NameFactory}</td><td>{@link NameFactoryLogger}</td></tr>
 * <tr><td>{@link PathFactory}</td><td>{@link PathFactoryLogger}</td></tr>
 * <tr><td>{@link IdFactory}</td><td>{@link IdFactoryLogger}</td></tr>
 * <tr><td>{@link QValueFactory}</td><td>{@link QValueFactoryLogger}</td></tr>
 * <tr><td>{@link SessionInfo}</td><td>{@link SessionInfoLogger}</td></tr>
 * <tr><td>{@link Batch}</td><td>{@link BatchLogger}</td></tr>
 * </table>
 *
 * The more general form
 * <pre>
 *   RepositoryService loggingService = SpiLoggerFactory.create(service, logWriterProvider);
 * </pre>
 * allows specification of a {@link LogWriterProvider}. A LogWriterProvider provides the
 * {@link LogWriter}s for the individual SPI entities. If the LogWriter does not provide a
 * LogWriter for a certain SPI entity no log wrapper is created for that entity. In the case
 * of {@link Slf4jLogWriterProvider}, a LogWriter is only provided if the logger of the
 * implementation class of the respective SPI entity is names after the class and has debug
 * level enabled.
 */
public final class SpiLoggerFactory {

    private SpiLoggerFactory() {
        super();
    }

    /**
     * Shortcut for
     * <pre>
     *   create(service, new Slf4jLogWriterProvider());
     * </pre>
     * @see #create(RepositoryService, LogWriterProvider)
     * @param service
     * @return
     */
    public static RepositoryService create(RepositoryService service) {
        return create(service, new Slf4jLogWriterProvider());
    }

    /**
     * Returns a log wrapper for the given <code>service</code> which logs a calls to its
     * methods if <code>logWriterProvider</code> returns a {@link LogWriter} instance for
     * <code>service</code>. Otherwise returns <code>service</code>.
     * @param service
     * @param logWriterProvider
     * @return
     * @throws IllegalArgumentException if either argument is <code>null</code>
     */
    public static RepositoryService create(RepositoryService service, LogWriterProvider logWriterProvider) {
        if (service == null) {
            throw new IllegalArgumentException("Service must not be null");
        }
        if (logWriterProvider == null) {
            throw new IllegalArgumentException("LogWriterProvider must not be null");
        }

        LogWriter logWriter = logWriterProvider.getLogWriter(service);
        if (logWriter == null) {
            return service;
        }
        else {
            return new ServiceLogger(service, logWriterProvider, logWriter);
        }
    }

    /**
     * Returns a log wrapper for the given <code>nameFactory</code> which logs a calls to its
     * methods if <code>logWriterProvider</code> returns a {@link LogWriter} instance for
     * <code>nameFactory</code>. Otherwise returns <code>nameFactory</code>.
     * @param nameFactory
     * @param logWriterProvider
     * @return
     * @throws IllegalArgumentException if either argument is <code>null</code>
     */
    public static NameFactory create(NameFactory nameFactory, LogWriterProvider logWriterProvider) {
        if (nameFactory == null) {
            throw new IllegalArgumentException("NameFactory must not be null");
        }
        if (logWriterProvider == null) {
            throw new IllegalArgumentException("LogWriterProvider must not be null");
        }

        LogWriter logWriter = logWriterProvider.getLogWriter(nameFactory);
        if (logWriter == null) {
            return nameFactory;
        }
        else {
            return new NameFactoryLogger(nameFactory, logWriter);
        }
    }

    /**
     * Returns a log wrapper for the given <code>pathFactory</code> which logs a calls to its
     * methods if <code>logWriterProvider</code> returns a {@link LogWriter} instance for
     * <code>pathFactory</code>. Otherwise returns <code>pathFactory</code>.
     * @param pathFactory
     * @param logWriterProvider
     * @return
     * @throws IllegalArgumentException if either argument is <code>null</code>
     */
    public static PathFactory create(PathFactory pathFactory, LogWriterProvider logWriterProvider) {
        if (pathFactory == null) {
            throw new IllegalArgumentException("PathFactory must not be null");
        }
        if (logWriterProvider == null) {
            throw new IllegalArgumentException("LogWriterProvider must not be null");
        }

        LogWriter logWriter = logWriterProvider.getLogWriter(pathFactory);
        if (logWriter == null) {
            return pathFactory;
        }
        else {
            return new PathFactoryLogger(pathFactory, logWriter);
        }
    }

    /**
     * Returns a log wrapper for the given <code>idFactory</code> which logs a calls to its
     * methods if <code>logWriterProvider</code> returns a {@link LogWriter} instance for
     * <code>idFactory</code>. Otherwise returns <code>idFactory</code>.
     * @param idFactory
     * @param logWriterProvider
     * @return
     * @throws IllegalArgumentException if either argument is <code>null</code>
     */
    public static IdFactory create(IdFactory idFactory, LogWriterProvider logWriterProvider) {
        if (idFactory == null) {
            throw new IllegalArgumentException("IdFactory must not be null");
        }
        if (logWriterProvider == null) {
            throw new IllegalArgumentException("LogWriterProvider must not be null");
        }

        LogWriter logWriter = logWriterProvider.getLogWriter(idFactory);
        if (logWriter == null) {
            return idFactory;
        }
        else {
            return new IdFactoryLogger(idFactory, logWriter);
        }
    }

    /**
     * Returns a log wrapper for the given <code>qValueFactory</code> which logs a calls to its
     * methods if <code>logWriterProvider</code> returns a {@link LogWriter} instance for
     * <code>qValueFactory</code>. Otherwise returns <code>qValueFactory</code>.
     * @param qValueFactory
     * @param logWriterProvider
     * @return
     * @throws IllegalArgumentException if either argument is <code>null</code>
     */
    public static QValueFactory create(QValueFactory qValueFactory, LogWriterProvider logWriterProvider) {
        if (qValueFactory == null) {
            throw new IllegalArgumentException("QValueFactory must not be null");
        }
        if (logWriterProvider == null) {
            throw new IllegalArgumentException("LogWriterProvider must not be null");
        }

        LogWriter logWriter = logWriterProvider.getLogWriter(qValueFactory);
        if (logWriter == null) {
            return qValueFactory;
        }
        else {
            return new QValueFactoryLogger(qValueFactory, logWriter);
        }
    }

    /**
     * Returns a log wrapper for the given <code>sessionInfo</code> which logs a calls to its
     * methods if <code>logWriterProvider</code> returns a {@link LogWriter} instance for
     * <code>sessionInfo</code>. Otherwise returns <code>sessionInfo</code>.
     * @param sessionInfo
     * @param logWriterProvider
     * @return
     * @throws IllegalArgumentException if either argument is <code>null</code>
     */
    public static SessionInfo create(SessionInfo sessionInfo, LogWriterProvider logWriterProvider) {
        if (sessionInfo == null) {
            throw new IllegalArgumentException("SessionInfo must not be null");
        }
        if (logWriterProvider == null) {
            throw new IllegalArgumentException("LogWriterProvider must not be null");
        }

        LogWriter logWriter = logWriterProvider.getLogWriter(sessionInfo);
        if (logWriter == null) {
            return sessionInfo;
        }
        else {
            return new SessionInfoLogger(sessionInfo, logWriter);
        }
    }

    /**
     * Returns a log wrapper for the given <code>batch</code> which logs a calls to its
     * methods if <code>logWriterProvider</code> returns a {@link LogWriter} instance for
     * <code>batch</code>. Otherwise returns <code>batch</code>.
     * @param batch
     * @param logWriterProvider
     * @return
     * @throws IllegalArgumentException if either argument is <code>null</code>
     */
    public static Batch create(Batch batch, LogWriterProvider logWriterProvider) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch must not be null");
        }
        if (logWriterProvider == null) {
            throw new IllegalArgumentException("LogWriterProvider must not be null");
        }

        LogWriter logWriter = logWriterProvider.getLogWriter(batch);
        if (logWriter == null) {
            return batch;
        }
        else {
            return new BatchLogger(batch, logWriter);
        }
    }

    //------------------------------------------------------------< private >---
    /**
     * Helper class which wraps SPI entities returned from calls to {@link RepositoryService}
     * into log wrappers if the {@link LogWriterProvider} can provide a {@link LogWriter}.
     */
    private static class ServiceLogger extends RepositoryServiceLogger {
        private final LogWriterProvider logWriterProvider;

        public ServiceLogger(RepositoryService service, LogWriterProvider logWriterProvider, LogWriter logWriter) {
            super(service, logWriter);
            this.logWriterProvider = logWriterProvider;
        }

        @Override
        public NameFactory getNameFactory() throws RepositoryException {
            NameFactory result = super.getNameFactory();
            return result == null
                ? null
                : create(result, logWriterProvider);
        }

        @Override
        public PathFactory getPathFactory() throws RepositoryException {
            PathFactory result = super.getPathFactory();
            return result == null
                ? null
                : create(result, logWriterProvider);
        }

        @Override
        public IdFactory getIdFactory() throws RepositoryException {
            IdFactory result = super.getIdFactory();
            return result == null
                ? null
                : create(result, logWriterProvider);
        }

        @Override
        public QValueFactory getQValueFactory() throws RepositoryException {
            QValueFactory result = super.getQValueFactory();
            return result == null
                ? null
                : create(result, logWriterProvider);
        }

        @Override
        public SessionInfo obtain(final Credentials credentials, final String workspaceName)
                throws RepositoryException {

            SessionInfo result = super.obtain(credentials, workspaceName);
            return result == null
                ? null
                : create(result, logWriterProvider);
        }

        @Override
        public SessionInfo obtain(final SessionInfo sessionInfo, final String workspaceName)
                throws RepositoryException {

            SessionInfo result = super.obtain(sessionInfo, workspaceName);
            return result == null
                ? null
                : create(result, logWriterProvider);
        }

        @Override
        public SessionInfo impersonate(final SessionInfo sessionInfo, final Credentials credentials)
                throws RepositoryException {

            SessionInfo result = super.impersonate(sessionInfo, credentials);
            return result == null
                ? null
                : create(result, logWriterProvider);
        }

        @Override
        public Batch createBatch(final SessionInfo sessionInfo, final ItemId itemId)
                throws RepositoryException {

            Batch result = super.createBatch(sessionInfo, itemId);
            return result == null
                ? null
                : create(result, logWriterProvider);
        }
    }
}
