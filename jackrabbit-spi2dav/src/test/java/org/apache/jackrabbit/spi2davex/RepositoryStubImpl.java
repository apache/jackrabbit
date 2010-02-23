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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import java.util.Properties;

/**
 * <code>RepositoryStubImpl</code>...
 */
public class RepositoryStubImpl extends org.apache.jackrabbit.spi2dav.RepositoryStubImpl {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(RepositoryStubImpl.class);

    /**
     * Overwritten constructor from base class.
     */
    public RepositoryStubImpl(Properties env) {
        super(env);
    }

    @Override
    protected RepositoryService createService(String uri) throws RepositoryException {
        BatchReadConfig brc = new BatchReadConfig() {
            public int getDepth(Path path, PathResolver resolver) throws NamespaceException {
                return 4;
            }
        };
        return new RepositoryServiceImpl(uri, brc);
    }
}
