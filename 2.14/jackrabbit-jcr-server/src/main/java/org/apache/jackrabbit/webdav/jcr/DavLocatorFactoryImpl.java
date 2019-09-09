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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.jackrabbit.webdav.AbstractLocatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>DavLocatorFactoryImpl</code>...
 */
public class DavLocatorFactoryImpl extends AbstractLocatorFactory {

    private static Logger log = LoggerFactory.getLogger(DavLocatorFactoryImpl.class);

    /**
     * Create a new factory
     *
     * @param pathPrefix Prefix, that needs to be removed in order to retrieve
     * the repository path from a given href.
     */
    public DavLocatorFactoryImpl(String pathPrefix) {
        super(pathPrefix);
    }

    //----------------------------------------------------------------------
    /**
     *
     * @param resourcePath
     * @param wspPath
     * @return
     * @see AbstractLocatorFactory#getRepositoryPath(String, String)
     */
    @Override
    protected String getRepositoryPath(String resourcePath, String wspPath) {
        if (resourcePath == null) {
            return null;
        }
        if (resourcePath.equals(wspPath)) {
            // workspace
            log.debug("Resource path represents workspace path -> repository path is null.");
            return null;
        } else {
            // a repository item  -> remove wspPath + /jcr:root
            String pfx = wspPath + ItemResourceConstants.ROOT_ITEM_RESOURCEPATH;
            if (resourcePath.startsWith(pfx)) {
                String repositoryPath = resourcePath.substring(pfx.length());
                return (repositoryPath.length() == 0) ? ItemResourceConstants.ROOT_ITEM_PATH : repositoryPath;
            } else {
                log.error("Unexpected format of resource path.");
                throw new IllegalArgumentException("Unexpected format of resource path: " + resourcePath + " (workspace: " + wspPath + ")");
            }
        }
    }

    /**
     *
     * @param repositoryPath
     * @param wspPath
     * @return
     * @see AbstractLocatorFactory#getResourcePath(String, String)
     */
    @Override
    protected String getResourcePath(String repositoryPath, String wspPath) {
        if (wspPath != null) {
            StringBuffer b = new StringBuffer(wspPath);
            if (repositoryPath != null) {
                b.append(ItemResourceConstants.ROOT_ITEM_RESOURCEPATH);
                if (!ItemResourceConstants.ROOT_ITEM_PATH.equals(repositoryPath)) {
                    b.append(repositoryPath);
                }
            }
            return b.toString();
        } else {
            log.debug("Workspace path is 'null' -> 'null' resource path");
            return null;
        }
    }
}

