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
package org.apache.jackrabbit.classloader;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>Util</code> provides helper methods for the repository
 * classloader and its class path entry and resource classes.
 * <p>
 * This class may not be extended or instantiated, it just contains static
 * utility methods.
 */
public class Util {

    /** default logging */
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    /** Private constructor to not instantiate */
    private Util() {
    }

    /**
     * Resolves the given <code>item</code> to a <code>Property</code> from
     * which contents can be read.
     * <p>
     * The following mechanism is used to derive the contents:
     * <ol>
     * <li>If the <code>item</code> is a property, this property is used</li>
     * <li>If the <code>item</code> is a node, three steps are tested:
     * <ol>
     * <li>If the node has a <code>jcr:content</code> child node, use that
     * child node in the next steps. Otherwise continue with the node.</li>
     * <li>Check for a <code>jcr:data</code> property and use that property
     * if existing.</li>
     * <li>Otherwise call <code>getPrimaryItem</code> method repeatedly until
     * a property is returned or until no more primary item is available.</li>
     * </ol>
     * </ol>
     * If no property can be resolved using the above algorithm or if the
     * resulting property is a multivalue property, <code>null</code> is
     * returned. Otherwise if the resulting property is a <code>REFERENCE</code>
     * property, the node referred to is retrieved and this method is called
     * recursively with the node. Otherwise, the resulting property is returned.
     * 
     * @param item The <code>Item</code> to resolve to a <code>Property</code>.
     * @return The resolved <code>Property</code> or <code>null</code> if
     *         the resolved property is a multi-valued property or the
     *         <code>item</code> is a node which cannot be resolved to a data
     *         property.
     * @throws ValueFormatException If the <code>item</code> resolves to a
     *             single-valued <code>REFERENCE</code> type property which
     *             cannot be resolved to the node referred to.
     * @throws RepositoryException if another error occurrs accessing the
     *             repository.
     */
    public static Property getProperty(Item item) throws ValueFormatException,
            RepositoryException {

        Property prop;
        if (item.isNode()) {

            // check whether the node has a jcr:content node (e.g. nt:file)
            Node node = (Node) item;
            if (node.hasNode("jcr:content")) {
                node = node.getNode("jcr:content");
            }

            // if the node has a jcr:data property, use that property
            if (node.hasProperty("jcr:data")) {
                
                prop = node.getProperty("jcr:data");

            } else {

                // otherwise try to follow default item trail
                try {
                    item = node.getPrimaryItem();
                    while (item.isNode()) {
                        item = ((Node) item).getPrimaryItem();
                    }
                    prop = (Property) item;
                } catch (ItemNotFoundException infe) {
                    // we don't actually care, but log for completeness
                    log.debug("getProperty: No primary items for "
                        + node.getPath(), infe);
                    return null;
                }
            }

        } else {

            prop = (Property) item;

        }

        // we get here with a property - otherwise an exception has already
        // been thrown
        if (prop.getDefinition().isMultiple()) {
            log.error("{} is a multivalue property", prop.getPath());
            return null;
        } else if (prop.getType() == PropertyType.REFERENCE) {
            Node node = prop.getNode();
            log.info("Property {} refers to node {}; finding primary item",
                prop.getPath(), node.getPath());
            return getProperty(node);
        }

        return prop;
    }

    /**
     * Returns the last modification time of the property, which is the long
     * value of the <code>jcr:lastModified</code> property of the parent node
     * of <code>prop</code>. If the parent node does not have a
     * <code>jcr:lastModified</code> property the current system time is
     * returned.
     * 
     * @param prop The property for which to return the last modification time.
     * @return The last modification time of the resource or the current time if
     *         the parent node of the property does not have a
     *         <code>jcr:lastModified</code> property.
     * @throws ItemNotFoundException If the parent node of the property cannot
     *             be retrieved.
     * @throws AccessDeniedException If (read) access to the parent node is
     *             denied.
     * @throws RepositoryException If any other error occurrs accessing the
     *             repository to retrieve the last modification time.
     */
    public static long getLastModificationTime(Property prop)
            throws ItemNotFoundException, PathNotFoundException,
            AccessDeniedException, RepositoryException {

        Node parent = prop.getParent();
        if (parent.hasProperty("jcr:lastModified")) {
            return parent.getProperty("jcr:lastModified").getLong();
        }

        return System.currentTimeMillis();
    }
}
