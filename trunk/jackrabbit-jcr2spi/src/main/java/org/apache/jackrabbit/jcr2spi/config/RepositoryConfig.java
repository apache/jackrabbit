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
package org.apache.jackrabbit.jcr2spi.config;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.RepositoryService;

/**
 * This class bundles the information required by JCR2SPI to
 * bootstrap an SPI implementation.
 * <p>
 * Instances of this class <em>should</em> implement
 * {@link javax.naming.Referenceable} in order to make JCR2SPI's
 * {@link javax.jcr.Repository} itself referenceable.
 */
public interface RepositoryConfig {

    public RepositoryService getRepositoryService() throws RepositoryException;

    public CacheBehaviour getCacheBehaviour();

    public int getItemCacheSize();

    /**
     * Specifies an interval used for polling the {@link RepositoryService} for changes.
     * @return  the poll timeout in milliseconds.
     */
    public int getPollTimeout();

    public <T> T getConfiguration(String name, T defaultValue);
}
