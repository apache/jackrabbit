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

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ResourceFactoryImpl implements a simple DavLocatorFactory
 */
// todo improve special handling of root item....
public class LocatorFactoryImpl implements DavLocatorFactory {

    /** the default logger */
    private static final Logger log = LoggerFactory.getLogger(LocatorFactoryImpl.class);

    private final String repositoryPrefix;

    public LocatorFactoryImpl(String repositoryPrefix) {
        this.repositoryPrefix = repositoryPrefix;
    }

    public DavResourceLocator createResourceLocator(String prefix, String href) {
        // build prefix string and remove all prefixes from the given href.
        StringBuffer b = new StringBuffer("");
        if (prefix != null && prefix.length() > 0) {
            b.append(prefix);
            if (href.startsWith(prefix)) {
                href = href.substring(prefix.length());
            }
        }
        if (repositoryPrefix != null && repositoryPrefix.length() > 0 && !prefix.endsWith(repositoryPrefix)) {
            b.append(repositoryPrefix);
            if (href.startsWith(repositoryPrefix)) {
                href = href.substring(repositoryPrefix.length());
            }
        }

        // special treatment for root item, that has no name but '/' path.
        if (href == null || "".equals(href)) {
            href = "/";
        }
        return new Locator(b.toString(), Text.unescape(href), this);
    }

    public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
        return createResourceLocator(prefix, workspacePath, resourcePath, true);
    }

    public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String path, boolean isResourcePath) {
        return new Locator(prefix, path, this);
    }

    //--------------------------------------------------------------------------
    private static class Locator implements DavResourceLocator {

        private final String prefix;
        private final String resourcePath;
        private final DavLocatorFactory factory;
        private final String href;

        private Locator(String prefix, String resourcePath, DavLocatorFactory factory) {
            this.prefix = prefix;
            this.factory = factory;
            // remove trailing '/' that is not part of the resourcePath except for the root item.
            if (resourcePath.endsWith("/") && !"/".equals(resourcePath)) {
                resourcePath = resourcePath.substring(0, resourcePath.length()-1);
            }
            this.resourcePath = resourcePath;
            href = prefix + Text.escapePath(resourcePath);
        }

        public String getPrefix() {
            return prefix;
        }

        public String getResourcePath() {
            return resourcePath;
        }

        public String getWorkspacePath() {
            return "";
        }

        public String getWorkspaceName() {
            return "";
        }

        public boolean isSameWorkspace(DavResourceLocator locator) {
            return isSameWorkspace(locator.getWorkspaceName());
        }

        public boolean isSameWorkspace(String workspaceName) {
            return getWorkspaceName().equals(workspaceName);
        }

        public String getHref(boolean isCollection) {
            // avoid doubled trailing '/' for the root item
            String suffix = (isCollection && !isRootLocation()) ? "/" : "";
            return href + suffix;
        }

        public boolean isRootLocation() {
            return "/".equals(resourcePath);
        }

        public DavLocatorFactory getFactory() {
            return factory;
        }

        /**
         * Returns the same as {@link #getResourcePath()}. No encoding is performed
         * at all.
         * @see DavResourceLocator#getRepositoryPath()
         */
        public String getRepositoryPath() {
            return getResourcePath();
        }

        /**
         * Computes the hash code from the href, which is built using the final
         * fields prefix and resourcePath.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return href.hashCode();
        }

        /**
         * Equality of path is achieved if the specified object is a <code>DavResourceLocator</code>
         * object with the same hash code.
         *
         * @param obj the object to compare to
         * @return <code>true</code> if the 2 objects are equal;
         *         <code>false</code> otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DavResourceLocator) {
                DavResourceLocator other = (DavResourceLocator) obj;
                return hashCode() == other.hashCode();
            }
            return false;
        }
    }
}
