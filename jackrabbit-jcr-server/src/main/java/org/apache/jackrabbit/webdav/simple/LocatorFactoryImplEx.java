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
package org.apache.jackrabbit.webdav.simple;

import org.apache.jackrabbit.webdav.AbstractLocatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>LocatorFactoryImplEx</code>...
 */
public class LocatorFactoryImplEx extends AbstractLocatorFactory {

    private static Logger log = LoggerFactory.getLogger(LocatorFactoryImplEx.class);

    /**
     * Create a new factory
     *
     * @param pathPrefix Prefix, that needs to be removed in order to retrieve
     *                   the path of the repository item from a given <code>DavResourceLocator</code>.
     */
    public LocatorFactoryImplEx(String pathPrefix) {
        super(pathPrefix);
    }

    /**
     *
     * @see AbstractLocatorFactory#getRepositoryPath(String, String)
     */
    @Override
    protected String getRepositoryPath(String resourcePath, String wspPath) {
        if (resourcePath == null) {
            return resourcePath;
        }

        if (resourcePath.equals(wspPath) || startsWithWorkspace(resourcePath, wspPath)) {
            String repositoryPath = resourcePath.substring(wspPath.length());
            return (repositoryPath.length() == 0) ? "/" : repositoryPath;
        } else {
            throw new IllegalArgumentException("Unexpected format of resource path: " + resourcePath + " (workspace: " + wspPath + ")");
        }
    }

    /**
     *
     * @see AbstractLocatorFactory#getResourcePath(String, String)
     */
    @Override
    protected String getResourcePath(String repositoryPath, String wspPath) {
        if (repositoryPath == null) {
            throw new IllegalArgumentException("Cannot build resource path from 'null' repository path");
        }
        return wspPath + repositoryPath;
    }

    private boolean startsWithWorkspace(String repositoryPath, String wspPath) {
        if (wspPath == null) {
            return true;
        } else {
            return repositoryPath.startsWith(wspPath + "/");
        }
    }
}
