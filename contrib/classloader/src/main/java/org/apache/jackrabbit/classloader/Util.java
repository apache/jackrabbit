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
 * The <code>Util</code> provides helper methods for the repository classloader
 * and its class path entry and resource classes.
 * <p>
 * This class may not be extended or instantiated, it just contains static
 * utility methods.
 *
 * @author Felix Meschberger
 */
public class Util {

    /** default logging */
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    /** Private constructor to not instantiate */
    private Util() {}

    /**
     * Resolves the given <code>item</code> to a <code>Property</code>. If the
     * <code>item</code> is a node, the <code>getPrimaryItem</code> method is
     * called repeatedly until a property is returned or until no more primary
     * item is available. If the resulting property is a multivalue property,
     * <code>null</code> is returned. Otherwise if the resulting property is
     * a <code>REFERENCE</code> property, the node referred to is retrieved
     * and this method is called recursively with the node. Otherwise, the
     * resulting property is returned.
     *
     * @param item The <code>Item</code> to resolve to a <code>Property</code>.
     *
     * @return The resolved <code>Property</code> or <code>null</code> if the
     *      resolved property is a multi-valued property.
     *
     * @throws ItemNotFoundException If the <code>item</code> is a node which
     *      cannot be resolved to a property through (repeated) calls to
     *      <code>Node.getPrimaryItem</code>.
     * @throws ValueFormatException If the <code>item</code> resolves to a
     *      single-valued <code>REFERENCE</code> type property which cannot
     *      be resolved to the node referred to.
     * @throws RepositoryException if another error occurrs accessing the
     *      repository.
     */
    public static Property getProperty(Item item)
            throws ItemNotFoundException, ValueFormatException,
            RepositoryException {

        // if the item is a node, get its primary item until either
        // no primary item exists any more or an ItemNotFoundException is thrown
        while (item.isNode()) {
            item = ((Node) item).getPrimaryItem();
        }

        // we get here with a property - otherwise an exception has already
        // been thrown
        Property prop = (Property) item;
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
     * Returns the last modification time of the property. If the property's
     * parent node is a <code>nt:resource</code> the <code>long</code> value
     * of the <code>jcr:lastModified</code> property of the parent node is
     * returned. Otherwise the current system time is returned.
     *
     * @param prop The property for which to return the last modification
     *      time.
     *
     * @return The last modification time of the resource or the current time
     *      if the property is not a child of an <code>nt:resource</code> node.
     *
     * @throws ItemNotFoundException If the parent node of the property cannot
     *      be retrieved.
     * @throws PathNotFoundException If the "jcr:lastModified" property of the
     *      parent node cannot be retrieved. This exception is unlikely in a
     *      correctly configured repository as the jcr:lastModified property
     *      has to be present in a node of type nt:resource.
     * @throws AccessDeniedException If (read) access to the parent node is
     *      denied.
     * @throws RepositoryException If any other error occurrs accessing the
     *      repository to retrieve the last modification time.
     */
    public static long getLastModificationTime(Property prop)
            throws ItemNotFoundException, PathNotFoundException,
            AccessDeniedException, RepositoryException {

        Node parent = prop.getParent();
        if (parent.isNodeType("nt:resource")) {
            return parent.getProperty("jcr:lastModified").getLong();
        }

        return System.currentTimeMillis();
    }
}
