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
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.spi.ItemId;

import javax.jcr.RepositoryException;

/**
 * <code>LogUtil</code>...
 */
public class LogUtil {

    private static Logger log = LoggerFactory.getLogger(LogUtil.class);

    /**
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @param qPath path to convert
     * @param nsResolver
     * @return JCR path
     */
    public static String safeGetJCRPath(Path qPath, NamespaceResolver nsResolver) {
        try {
            return PathFormat.format(qPath, nsResolver);
        } catch (NoPrefixDeclaredException npde) {
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
     * @param nsResolver
     * @return JCR path
     */
    public static String safeGetJCRPath(ItemState itemState, NamespaceResolver nsResolver) {
        try {
            return safeGetJCRPath(itemState.getHierarchyEntry().getPath(), nsResolver);
        } catch (RepositoryException e) {
            ItemId id = itemState.getId();
            log.error("failed to convert " + id + " to JCR path.");
            return id.toString();
        }
    }

    /**
     * Failsafe conversion of a <code>QName</code> to a JCR name for use in
     * error messages etc.
     *
     * @param qName
     * @param nsResolver
     * @return JCR name or String representation of the given <code>QName</code>
     * in case the resolution fails.
     */
    public static String saveGetJCRName(QName qName, NamespaceResolver nsResolver) {
        try {
            return NameFormat.format(qName, nsResolver);
        } catch (NoPrefixDeclaredException e) {
            log.error("failed to convert " + qName + " to JCR name.");
            return qName.toString();
        }
    }
}