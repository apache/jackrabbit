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
package org.apache.jackrabbit.spi2jcr;

import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.RepositoryServiceFactory;
import org.apache.jackrabbit.spi.commons.ItemInfoCacheImpl;

public class Spi2jcrRepositoryServiceFactory implements RepositoryServiceFactory {

    /**
     * Mandatory repository parameter: expects value to be an instance of {@link javax.jcr.Repository}.
     */
    public static final String PARAM_REPOSITORY = "org.apache.jackrabbit.spi2jcr.Repository";

    /**
     * Optional batch read configuration parameter. If it is present the value is
     * expected to be an instance of {@link org.apache.jackrabbit.spi2jcr.BatchReadConfig}.
     */
    public static final String PARAM_BATCH_READ_CONFIG = "org.apache.jackrabbit.spi2jcr.BatchReadConfig";

    /**
     * Optional configuration parameter: It's value determines the size of the
     * {@link ItemInfoCache} cache. Defaults to {@link ItemInfoCacheImpl#DEFAULT_CACHE_SIZE}.
     */
    public static final String PARAM_ITEMINFO_CACHE_SIZE = "org.apache.jackrabbit.spi2jcr.ItemInfoCacheSize";

    public RepositoryService createRepositoryService(Map<?, ?> parameters) throws RepositoryException {
        if (parameters == null) {
            throw new RepositoryException("Parameter " + PARAM_REPOSITORY + " missing");
        }

        Object repo = parameters.get(PARAM_REPOSITORY);
        if (repo == null || !(repo instanceof Repository)) {
            throw new RepositoryException("Parameter " + PARAM_REPOSITORY + " missing or not an instance of Repository");
        }

        BatchReadConfig brConfig;
        Object obj = parameters.get(PARAM_BATCH_READ_CONFIG);
        if (obj != null && obj instanceof BatchReadConfig) {
            brConfig = (BatchReadConfig) obj;
        }
        else {
            brConfig = new BatchReadConfig();
        }

        int itemInfoCacheSize = ItemInfoCacheImpl.DEFAULT_CACHE_SIZE;
        Object param = parameters.get(PARAM_ITEMINFO_CACHE_SIZE);
        if (param != null) {
            try {
                itemInfoCacheSize = Integer.parseInt(param.toString());
            }
            catch (NumberFormatException e) {
                // ignore, use default
            }
        }

        return new RepositoryServiceImpl((Repository) repo, brConfig, itemInfoCacheSize);
    }

}
