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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavLocatorFactory;

/**
 * <code>DavLocatorFactoryImpl</code>...
 */
public class DavLocatorFactoryImpl implements DavLocatorFactory {

    private static Logger log = Logger.getLogger(DavLocatorFactoryImpl.class);

    private final String pathPrefix;

    /**
     * Create a new factory
     *
     * @param pathPrefix Prefix, that needs to be removed in order to retrieve
     * the path of the repository item from a given <code>DavResourceLocator</code>.
     */
    public DavLocatorFactoryImpl(String pathPrefix) {
	this.pathPrefix = pathPrefix;
    }

    /**
     * Create a new <code>DavResourceLocator</code>. Any leading
     * path-prefix (as defined with the constructor) and trailing '/' with
     * the request handle is removed. The first label of the remaining handle is
     * treated as workspace name. The remaining part of the given request handle
     * is said to be the resource handle ("/" if an empty string remains).
     * If the request handle does neither provide workspace name nor resource
     * handle both values are set to <code>null</code>; the path object then
     * represents the root resource that has no corresponding item in the JCR
     * repository.
     *
     * @param prefix
     * @param requestHandle
     * @return a new <code>DavResourceLocator</code>
     * @throws IllegalArgumentException if the request handle is <code>null</code>
     */
    public DavResourceLocator createResourceLocator(String prefix, String requestHandle) {
	if (requestHandle == null) {
	    throw new IllegalArgumentException("Request handle must not be null.");
	}

	StringBuffer b = new StringBuffer("");
	if (prefix != null) {
	    b.append(prefix);
	    if (pathPrefix != null && !prefix.endsWith(pathPrefix)) {
		b.append(pathPrefix);
	    }
	}
	String rlPrefix = b.toString();

	// remove path-prefix defined with the servlet that may preceed the
	// the requestHandle
	if (pathPrefix != null && requestHandle.startsWith(pathPrefix)) {
	    requestHandle = requestHandle.substring(pathPrefix.length());
	}

	// remove trailing "/" that is present with collections
	if (requestHandle.endsWith("/")) {
	    requestHandle = requestHandle.substring(0, requestHandle.length()-1);
	}

	String resourcePath;
	String workspacePath;

	// an empty requestHandle (after removal of the "/") signifies a request
	// to the root that does not represent a repository item.
	if ("".equals(requestHandle)) {
	    resourcePath = null;
	    workspacePath = null;
	} else {
	    // look for the first slash ignoring the leading one
	    int pos = requestHandle.indexOf('/', 1);
	    if (pos == -1) {
		// request to a 'workspace' resource that in the same time
		// represent the root node of the repository.
		workspacePath = requestHandle;
		resourcePath = ItemResourceConstants.ROOT_ITEM_PATH;
	    } else {
		// separate the workspace name from the path of the repository
		// item.
		workspacePath = requestHandle.substring(0, pos);
		resourcePath = requestHandle.substring(pos);
	    }
	}

	return new DavResourceLocatorImpl(rlPrefix, workspacePath, resourcePath, this);
    }

    /**
     * Create a new <code>DavResourceLocator</code> from the specified prefix,
     * workspace path and resource path, whithout modifying the specified Strings.
     *
     * @param prefix
     * @param workspacePath
     * @param resourcePath
     * @return a new <code>DavResourceLocator</code>
     * @see DavLocatorFactory#createResourceLocator(String, String, String)
     */
    public DavResourceLocator createResourceLocator(String prefix, String workspacePath, String resourcePath) {
	return new DavResourceLocatorImpl(prefix, workspacePath, resourcePath, this);
    }

    /**
     * Private inner class <code>DavResourceLocatorImpl</code> implementing
     * the <code>DavResourceLocator</code> interface.
     */
    private class DavResourceLocatorImpl implements DavResourceLocator {

	private final String prefix;
	private final String workspacePath;
	private final String resourcePath;
	private final DavLocatorFactory factory;

	/**
	 * Create a new <code>DavResourceLocatorImpl</code>.
	 *
	 * @param prefix
	 * @param workspacePath
	 * @param resourcePath
	 */
	DavResourceLocatorImpl(String prefix, String workspacePath, String resourcePath, DavLocatorFactory factory) {
	    this.prefix = prefix;
	    this.workspacePath = workspacePath;
	    this.resourcePath = resourcePath;
	    this.factory = factory;
	}

	/**
	 * Return the prefix used to build the href String. This includes the initial
	 * hrefPrefix as well a the path prefix.
	 *
	 * @return prefix String used to build the href.
	 */
	public String getPrefix() {
	    return prefix;
	}

	/**
	 * Return the resource path of <code>null</code> if this locator object
	 * represents the '/' request handle. To a request handle specifying a
	 * workspace name only the '/' resource path is assigned, which represents
	 * the root node of the repository.
	 *
	 * @return resource path or <code>null</code>
	 * @see org.apache.jackrabbit.webdav.DavResourceLocator#getResourcePath()
	 */
	public String getResourcePath() {
	    return resourcePath;
	}

	/**
	 * Return the workspace path or <code>null</code> if this locator object
	 * represents the '/' request handle.
	 *
	 * @return workspace path or <code>null</code>
	 * @see org.apache.jackrabbit.webdav.DavResourceLocator#getWorkspacePath()
	 */
	public String getWorkspacePath() {
	    return workspacePath;
	}

	/**
	 * Return the workspace name or <code>null</code> if this locator object
	 * represents the '/' request handle.
	 *
	 * @return workspace name or <code>null</code>
	 * @see org.apache.jackrabbit.webdav.DavResourceLocator#getWorkspaceName()
	 */
	public String getWorkspaceName() {
	    if (workspacePath != null) {
		return workspacePath.substring(1);
	    }
	    return null;
	}

	/**
	 * Returns true if the specified locator object refers to a resource within
	 * the same workspace.
	 *
	 * @param locator
	 * @return true if the workspace name is equal to this workspace name.
	 * @see DavResourceLocator#isSameWorkspace(org.apache.jackrabbit.webdav.DavResourceLocator)
	 */
	public boolean isSameWorkspace(DavResourceLocator locator) {
	    return (locator == null) ? false : isSameWorkspace(locator.getWorkspaceName());
	}

	/**
	 * Returns true if the specified string equals to this workspace name or
	 * if this workspace name is null.
	 *
	 * @param workspaceName
	 * @return true if the workspace name is equal to this workspace name.
	 * @see DavResourceLocator#isSameWorkspace(String)
	 */
	public boolean isSameWorkspace(String workspaceName) {
	    if (getWorkspaceName() == null) {
		return true;
	    } else {
		return getWorkspaceName().equals(workspaceName);
	    }
	}

	/**
	 * Builds the 'href' from the prefix, the workspace name and the
	 * resource path present and assures a trailing '/' in case the href
	 * is used for collection.
	 *
	 * @param isCollection
	 * @return href String representing the text of the href element
	 * @see org.apache.jackrabbit.webdav.DavConstants#XML_HREF
	 * @see DavResourceLocator#getHref(boolean)
	 */
	public String getHref(boolean isCollection) {
	    StringBuffer href = new StringBuffer(prefix);
	    if (workspacePath != null) {
		href.append(workspacePath);
	    }
	    if (resourcePath != null) {
		href.append(resourcePath);
	    }
	    if (isCollection && href.charAt(href.length()-1) != '/') {
		href.append("/");
	    }
	    return href.toString();
	}

	/**
	 * Returns true if the 'workspaceName' field is <code>null</code>.
	 *
	 * @return true if the 'workspaceName' field is <code>null</code>.
	 * @see org.apache.jackrabbit.webdav.DavResourceLocator#isRootLocation()
	 */
	public boolean isRootLocation() {
	    return workspacePath == null;
	}

	/**
	 * Return the factory that created this locator.
	 *
	 * @return factory
	 * @see org.apache.jackrabbit.webdav.DavResourceLocator#getFactory()
	 */
	public DavLocatorFactory getFactory() {
	    return factory;
	}

	/**
	 * Computes the hash code using the prefix, the workspace name and the
	 * resource path.
	 *
	 * @return the hash code
	 */
	public int hashCode() {
	    int hashCode = prefix.hashCode();
	    if (workspacePath != null) {
		hashCode += workspacePath.hashCode();
	    }
	    if (resourcePath != null) {
		hashCode += resourcePath.hashCode();
	    }
	    return hashCode % Integer.MAX_VALUE;
	}

	/**
	 * Equality of locators is achieved if prefix and resource path
	 * are equal.
	 *
	 * @param obj the object to compare to
	 * @return <code>true</code> if the 2 objects are equal;
	 *         <code>false</code> otherwise
	 */
	public boolean equals(Object obj) {
	    if (obj instanceof DavResourceLocatorImpl) {
		DavResourceLocatorImpl locator = (DavResourceLocatorImpl) obj;
		boolean equalWsName = (workspacePath == null) ? locator.workspacePath == null : workspacePath.equals(locator.workspacePath);
		boolean equalRPath = (resourcePath == null) ? locator.resourcePath == null : resourcePath.equals(locator.resourcePath);

		return prefix.equals(locator.prefix) && equalWsName && equalRPath;
	    }
	    return false;
	}
    }
}

