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
package org.apache.jackrabbit.spi2davex;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Properties;

/** <code>ServiceStubImpl</code>... */
public class ServiceStubImpl extends org.apache.jackrabbit.spi2dav.ServiceStubImpl {

    private static Logger log = LoggerFactory.getLogger(ServiceStubImpl.class);

    public static final String PROP_DEFAULT_DEPTH = "org.apache.jackrabbit.spi2davex.defaultDepth";

    private RepositoryService service;

    /**
     * Implementations of this class must overwrite this constructor.
     *
     * @param env the environment variables. This parameter must not be null.
     */
    public ServiceStubImpl(Properties env) {
        super(env);
    }

    @Override
    public RepositoryService getRepositoryService() throws RepositoryException {
        if (service == null) {
            String uri = getProperty(PROP_REPOSITORY_URI);
            service = new RepositoryServiceImpl(uri, new BatchReadConfig() {
                public int getDepth(Path path, PathResolver resolver) {
                    String depthStr = getProperty(PROP_DEFAULT_DEPTH);
                    if (depthStr != null) {
                        try {
                            return Integer.parseInt(depthStr);
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    return 4;
                }
            });
        }
        return service;
    }
}
