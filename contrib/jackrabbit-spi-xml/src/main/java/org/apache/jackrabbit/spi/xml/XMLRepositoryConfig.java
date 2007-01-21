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
package org.apache.jackrabbit.spi.xml;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.value.ValueFactoryImpl;

public class XMLRepositoryConfig implements RepositoryConfig {

    private final RepositoryService service;

    public XMLRepositoryConfig(XMLRepositoryService service) {
        this.service = service;
    }
    
    public RepositoryService getRepositoryService() throws RepositoryException {
        return service;
    }

    public CacheBehaviour getCacheBehaviour() {
        return null;
    }

    public String getDefaultWorkspaceName() {
        return "xml";
    }

    public int getPollingInterval() {
        return 1000;
    }

    public ValueFactory getValueFactory() {
        return ValueFactoryImpl.getInstance();
    }

}
