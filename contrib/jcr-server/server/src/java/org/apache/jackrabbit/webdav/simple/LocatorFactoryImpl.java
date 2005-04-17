/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.jackrabbit.webdav.*;
import org.apache.log4j.Logger;

/**
 * ResourceFactoryImpl implements a simple DavLocatorFactory
 */
// todo improve special handling of root item....
public class LocatorFactoryImpl implements DavLocatorFactory {

    /** the default logger */
    private static final Logger log = Logger.getLogger(LocatorFactoryImpl.class);

    private final String repositoryPrefix;

    public LocatorFactoryImpl(String repositoryPrefix) {
        this.repositoryPrefix = repositoryPrefix;
    }

    public DavResourceLocator createResourceLocator(String prefix, String requestHandle) {
        String rPrefix = prefix + repositoryPrefix;
        String rHandle = requestHandle;
	// remove the configured repository prefix from the path
        if (rHandle != null && rHandle.startsWith(repositoryPrefix)) {
            rHandle = rHandle.substring(repositoryPrefix.length());
        }
	// special treatment for root item, that has no name but '/' path.
        if (rHandle == null || "".equals(rHandle)) {
            rHandle = "/";
        }
        return new Locator(rPrefix, rHandle, this);
    }

    public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
        return new Locator(prefix, resourcePath, this);
    }

    private class Locator implements DavResourceLocator {

        private final String prefix;
        private final String itemPath;
        private final DavLocatorFactory factory;

        private Locator(String prefix, String itemPath, DavLocatorFactory factory) {
            this.prefix = prefix;
            this.factory = factory;
	    // remove trailing '/' that is not part of the itemPath except for the root item.
            if (itemPath.endsWith("/") && !"/".equals(itemPath)) {
                itemPath = itemPath.substring(0, itemPath.length()-1);
            }
            this.itemPath = itemPath;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getResourcePath() {
            return itemPath;
        }

        public String getWorkspacePath() {
            return "";
        }

        public String getWorkspaceName() {
            return "";
        }

        public boolean isSameWorkspace(DavResourceLocator path) {
            return isSameWorkspace(path.getWorkspaceName());
        }

        public boolean isSameWorkspace(String workspaceName) {
            return getWorkspaceName().equals(workspaceName);
        }

        public String getHref(boolean isCollection) {
	    // avoid doubled trainling '/' for the root item
	    String suffix = (isCollection && !isRootLocation()) ? "/" : "";
            return prefix + itemPath + suffix;
        }

        public boolean isRootLocation() {
            return "/".equals(itemPath);
        }

        public DavLocatorFactory getFactory() {
            return factory;
        }

        /**
         * Computes the hash code using the prefix and the itemPath
         *
         * @return the hash code
         */
        public int hashCode() {
            int hashCode = prefix.hashCode();
            if (itemPath != null) {
                hashCode += itemPath.hashCode();
            }
            return hashCode % Integer.MAX_VALUE;
        }

        /**
         * Equality of path is achieved if the specified object is a <code>DavResourceLocator</code>
         * and the return values of the two <code>getHref(boolean)</code> methods are
         * equal.
         *
         * @param obj the object to compare to
         * @return <code>true</code> if the 2 objects are equal;
         *         <code>false</code> otherwise
         */
        public boolean equals(Object obj) {
            if (obj instanceof DavResourceLocator) {
                DavResourceLocator path = (DavResourceLocator) obj;
                this.getHref(true).equals(path.getHref(true));
            }
            return false;
        }
    }
}
