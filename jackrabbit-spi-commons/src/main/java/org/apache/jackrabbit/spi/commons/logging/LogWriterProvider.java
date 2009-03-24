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

/**
 * LogWriterProvider instances provide {@link LogWriter}s for the individual
 * SPI entities.
 */
public interface LogWriterProvider {

    /**
     * @param service
     * @return  A LogWriter for logging calls to <code>service</code>.
     */
    public LogWriter getLogWriter(RepositoryService service);

    /**
     * @param nameFactory
     * @return  A LogWriter for logging calls to <code>nameFactory</code>.
     */
    public LogWriter getLogWriter(NameFactory nameFactory);

    /**
     * @param pathFactory
     * @return  A LogWriter for logging calls to <code>pathFactory</code>.
     */
    public LogWriter getLogWriter(PathFactory pathFactory);

    /**
     * @param idFactory
     * @return  A LogWriter for logging calls to <code>idFactory</code>.
     */
    public LogWriter getLogWriter(IdFactory idFactory);

    /**
     * @param qValueFactory
     * @return  A LogWriter for logging calls to <code>qValueFactory</code>.
     */
    public LogWriter getLogWriter(QValueFactory qValueFactory);

    /**
     * @param sessionInfo
     * @return  A LogWriter for logging calls to <code>sessionInfo</code>.
     */
    public LogWriter getLogWriter(SessionInfo sessionInfo);

    /**
     * @param batch
     * @return  A LogWriter for logging calls to <code>batch</code>.
     */
    public LogWriter getLogWriter(Batch batch);
}
