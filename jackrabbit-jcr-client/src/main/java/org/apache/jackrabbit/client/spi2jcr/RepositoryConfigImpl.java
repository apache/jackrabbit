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
package org.apache.jackrabbit.client.spi2jcr;

import org.apache.jackrabbit.client.AbstractRepositoryConfig;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi2jcr.BatchReadConfig;
import org.apache.jackrabbit.spi2jcr.RepositoryServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Repository;
import java.util.Map;

/**
 * <code>RepositoryConfigImpl</code>...
 */
public class RepositoryConfigImpl extends AbstractRepositoryConfig {

    private static Logger log = LoggerFactory.getLogger(org.apache.jackrabbit.client.spilogger.RepositoryConfigImpl.class);

    /**
     * Mandatory repository parameter: expects value to be an instance of {@link javax.jcr.Repository}.
     */
    public static final String PARAM_REPOSITORY = "org.apache.jackrabbit.spi2jcr.repository";
    /**
     * Optional batch read config paramater. If it is present the value is
     * expected to be an instance of {@link org.apache.jackrabbit.spi2jcr.BatchReadConfig}.
     */
    public static final String PARAM_BATCH_READ_CONFIG = "org.apache.jackrabbit.spi2jcr.batchReadConfig";

    private final RepositoryService service;

    public static RepositoryConfig create(Map parameters) {
        if (!parameters.containsKey(PARAM_REPOSITORY)) {
            return null;
        }
        try {
            return new RepositoryConfigImpl(parameters);
        } catch (RepositoryException e) {
            log.warn(e.getMessage());
            return null;
        }
    }

    private RepositoryConfigImpl(Map params) throws RepositoryException {
        super(params);
        Object repo = params.get(PARAM_REPOSITORY);
        if (repo == null || !(repo instanceof Repository)) {
            throw new RepositoryException();
        }
        BatchReadConfig brConfig = new BatchReadConfig();
        Object obj = params.get(PARAM_BATCH_READ_CONFIG);
        if (obj != null && obj instanceof BatchReadConfig) {
            brConfig = (BatchReadConfig) obj;
        }
        service = new RepositoryServiceImpl((Repository) repo, brConfig);
    }

    public RepositoryService getRepositoryService() throws RepositoryException {
        return service;
    }
}
