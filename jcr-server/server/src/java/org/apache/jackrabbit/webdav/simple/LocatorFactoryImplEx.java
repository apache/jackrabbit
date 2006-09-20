/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
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
    protected String getRepositoryPath(String resourcePath, String wspPath) {
        if (resourcePath == null) {
            return resourcePath;
        }

        if (resourcePath.startsWith(wspPath)) {
            String repositoryPath = resourcePath.substring(wspPath.length());
            return (repositoryPath.length() == 0) ? "/" : repositoryPath;
        } else {
            throw new IllegalArgumentException("Unexpected format of resource path.");
        }
    }

    /**
     *
     * @see AbstractLocatorFactory#getResourcePath(String, String)
     */
    protected String getResourcePath(String repositoryPath, String wspPath) {
        if (repositoryPath == null) {
            throw new IllegalArgumentException("Cannot build resource path from 'null' repository path");
        }
        return (wspPath == null || repositoryPath.startsWith(wspPath)) ? repositoryPath : wspPath + repositoryPath;
    }
}
