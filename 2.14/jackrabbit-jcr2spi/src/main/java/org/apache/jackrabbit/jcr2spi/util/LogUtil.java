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
package org.apache.jackrabbit.jcr2spi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;

/**
 * <code>LogUtil</code>...
 */
public final class LogUtil {

    private static Logger log = LoggerFactory.getLogger(LogUtil.class);

    /**
     * Avoid instantiation
     */
    private LogUtil() {}

    /**
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @param qPath path to convert
     * @param pathResolver
     * @return JCR path
     */
    public static String safeGetJCRPath(Path qPath, PathResolver pathResolver) {
        try {
            return pathResolver.getJCRPath(qPath);
        } catch (NamespaceException e) {
            log.error("failed to convert " + qPath + " to JCR path.");
            // return string representation of internal path as a fallback
            return qPath.toString();
        }
    }

    /**
     * Failsafe conversion of an <code>ItemState</code> to JCR path for use in
     * error messages etc.
     *
     * @param itemState
     * @param pathResolver
     * @return JCR path
     */
    public static String safeGetJCRPath(ItemState itemState, PathResolver pathResolver) {
        try {
            return safeGetJCRPath(itemState.getHierarchyEntry().getPath(), pathResolver);
        } catch (RepositoryException e) {
            log.error("failed to convert " + itemState.toString() + " to JCR path.");
            return itemState.toString();
        }
    }

    /**
     * Failsafe conversion of a <code>Name</code> to a JCR name for use in
     * error messages etc.
     *
     * @param qName
     * @param nameResolver
     * @return JCR name or String representation of the given <code>Name</code>
     * in case the resolution fails.
     */
    public static String saveGetJCRName(Name qName, NameResolver nameResolver) {
        try {
            return nameResolver.getJCRName(qName);
        } catch (NamespaceException e) {
            log.error("failed to convert " + qName + " to JCR name.");
            return qName.toString();
        }
    }

    /**
     * Failsafe conversion of an <code>ItemId</code> to a human readable string
     * resolving the path part of the specified id using the given path resolver.
     *
     * @param itemId
     * @param pathResolver
     * @return a String representation of the given <code>ItemId</code>.
     */
    public static String saveGetIdString(ItemId itemId, PathResolver pathResolver) {
        Path p = itemId.getPath();
        if (p == null || pathResolver == null) {
            return itemId.toString();
        } else {
            StringBuffer bf = new StringBuffer();
            String uniqueID = itemId.getUniqueID();
            if (uniqueID != null) {
                bf.append(uniqueID).append(" - ");
            }
            String jcrPath;
            try {
                jcrPath = pathResolver.getJCRPath(p);
            } catch (NamespaceException e) {
                jcrPath = p.toString();
            }
            bf.append(jcrPath);
            return bf.toString();
        }
    }
}