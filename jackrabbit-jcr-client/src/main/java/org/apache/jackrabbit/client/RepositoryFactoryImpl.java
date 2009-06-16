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
package org.apache.jackrabbit.client;

import org.apache.jackrabbit.jcr2spi.RepositoryImpl;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import java.util.Map;

/**
 * <code>RepositoryFactoryImpl</code>...
 */
public class RepositoryFactoryImpl implements RepositoryFactory {

    private static Logger log = LoggerFactory.getLogger(RepositoryFactoryImpl.class);

    public static final String REPOSITORY_CONFIG = "org.apache.jackrabbit.repository.config";

    //--------------------------------------------------< RepositoryFactory >---
    /**
     * Creates a JCR repository from the given <code>parameters</code>.
     * If the <code>parameters</code> map is <code>null</code> the default
     * repository  (i.e. JCR2SPI repository on top of SPI2DAVex) is returned.<p/>
     * If the <code>parameters</code> map contains a {@link #REPOSITORY_CONFIG}
     * entry it's value is expected to be a implementation of
     * {@link org.apache.jackrabbit.jcr2spi.config.RepositoryConfig} and the
     * repository will be created based on this configuration.<p/>
     * If the <code>parameters</code> map does not contain a {@link #REPOSITORY_CONFIG}
     * entry or if the corresponding value isn't a valid <code>RepositoryConfig</code>
     * an attempt is made to create a
     * {@link org.apache.jackrabbit.jcr2spi.config.RepositoryConfig} for any of
     * the known SPI implementations:
     * <ul>
     * <li>SPI2DAVex (see jackrabbit-spi2dav module)</li>
     * <li>SPI2DAV (see jackrabbit-spi2dav module)</li>
     * <li>SPI2JCR (see jackrabbit-spi2jcr module)</li>
     * </ul>
     * NOTE: If the <code>parameters</code> map contains an
     * {@link org.apache.jackrabbit.client.spilogger.RepositoryConfigImpl#PARAM_LOG_WRITER_PROVIDER PARAM_LOG_WRITER_PROVIDER}
     * entry the {@link org.apache.jackrabbit.spi.RepositoryService RepositoryService} obtained
     * from the configuration is wrapped by a SPI logger. See the
     * {@link org.apache.jackrabbit.spi.commons.logging.SpiLoggerFactory SpiLoggerFactory}
     * for details.
     *
     * @see RepositoryFactory#getRepository(java.util.Map)
     */
    public Repository getRepository(Map parameters) throws RepositoryException {
        RepositoryConfig config = null;
        if (parameters == null) {
            config = org.apache.jackrabbit.client.spi2dav.RepositoryConfigImpl.create((Map) null);
        } else {
            Object param = parameters.get(REPOSITORY_CONFIG);
            if (param != null && param instanceof RepositoryConfig) {
                config = (RepositoryConfig) param;
            }
            if (config == null) {
                config = org.apache.jackrabbit.client.spi2dav.RepositoryConfigImpl.create(parameters);
                if (config == null) {
                    config = org.apache.jackrabbit.client.spi2dav.RepositoryConfigImpl.create(parameters);
                }
                if (config == null) {
                    config = org.apache.jackrabbit.client.spi2jcr.RepositoryConfigImpl.create(parameters);
                }
            }
        }

        if (config != null) {
            config = org.apache.jackrabbit.client.spilogger.RepositoryConfigImpl.create(config, parameters);
            return RepositoryImpl.create(config);
        } else {
            log.debug("Unable to create Repository: Unknown parameters.");
            return null;
        }
    }
}