/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.jcr.property.NamespacesProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.log4j.Logger;

import javax.jcr.Item;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import java.util.Iterator;
import java.util.Properties;

/**
 * <code>RootItemCollection</code> represents the root node of the underlying
 * repository. However, the display name the name of the workspace is returned
 * the root node is located.
 */
//todo currently the jcr root node is the same for all workspace resources... this is wrong...
public class RootItemCollection extends VersionControlledItemCollection {

    private static Logger log = Logger.getLogger(RootItemCollection.class);

    /**
     * Create a new <code>RootItemCollection</code>.
     *
     * @param locator
     * @param session
     */
    protected RootItemCollection(DavResourceLocator locator, JcrDavSession session,
                                 DavResourceFactory factory, Item item) {
        super(locator, session, factory, item);
    }

    //----------------------------------------------< DavResource interface >---
    /**
     * Returns the name of the workspace the underlying root item forms part of.
     *
     * @return The workspace name
     * @see org.apache.jackrabbit.webdav.DavResource#getDisplayName()
     * @see javax.jcr.Workspace#getName()
     */
    public String getDisplayName() {
        return getLocator().getWorkspaceName();
    }

    /**
     * Retrieve the collection that has all root item / workspace collections
     * as internal members.
     *
     * @see org.apache.jackrabbit.webdav.DavResource#getCollection()
     */
    public DavResource getCollection() {
        DavResource collection = null;
        // create location with 'null' values for workspace-path and resource-path
        DavResourceLocator parentLoc = getLocator().getFactory().createResourceLocator(getLocator().getPrefix(), null, null);
        try {
            collection = createResourceFromLocator(parentLoc);
        } catch (DavException e) {
            log.error("Unexpected error while retrieving collection: " + e.getMessage());
        }
        return collection;
    }

    /**
     * Allows to alter the registered namespaces ({@link #JCR_NAMESPACES}) and
     * forwards any other property to the super class.<p/>
     * Note that again no property status is set. Any failure while setting
     * a property results in an exception (violating RFC 2518).
     *
     * @param property
     * @throws DavException
     * @see DavResource#setProperty(org.apache.jackrabbit.webdav.property.DavProperty)
     */
    public void setProperty(DavProperty property) throws DavException {
        if (JCR_NAMESPACES.equals(property.getName())) {
            NamespacesProperty nsp = new NamespacesProperty(property);
            try {
                Properties changes = nsp.getNamespaces();
                NamespaceRegistry nsReg = getRepositorySession().getWorkspace().getNamespaceRegistry();
                String[] registeredPrefixes = nsReg.getPrefixes();
                for (int i = 0; i < registeredPrefixes.length; i++) {
                    String prfx = registeredPrefixes[i];
                    if (!changes.containsKey(prfx)) {
                        // prefix not present amongst the new values any more > unregister
                        nsReg.unregisterNamespace(prfx);
                    } else if (changes.get(prfx).equals(nsReg.getURI(prfx))) {
                        // present with same uri-value >> no action required
                        changes.remove(prfx);
                    }
                }

                // try to register any prefix/uri pair that has a changed uri or
                // it has not been present before.
                Iterator prefixIt = changes.keySet().iterator();
                while (prefixIt.hasNext()) {
                    String prefix = (String)prefixIt.next();
                    String uri = (String)changes.get(prefix);
                    nsReg.registerNamespace(prefix, uri);
                }
            } catch (RepositoryException e) {
                throw new JcrDavException(e);
            }
        } else {
            super.setProperty(property);
        }
    }

    /**
     * Handles an attempt to set {@link #JCR_NAMESPACES} and forwards any other
     * set or remove requests to the super class.
     * Please note, that RFC 2518 is violated because setting {@link #JCR_NAMESPACES}
     * is handled out of the order indicated by the set and changes may be persisted
     * even if altering another property fails.
     *
     * @see #setProperty(DavProperty)
     * @see DefaultItemCollection#alterProperties(org.apache.jackrabbit.webdav.property.DavPropertySet, org.apache.jackrabbit.webdav.property.DavPropertyNameSet)
     */
    public MultiStatusResponse alterProperties(DavPropertySet setProperties, DavPropertyNameSet removePropertyNames) throws DavException {
        // TODO: respect order of the set and do not persist if super.alterProperties fails
        if (setProperties.contains(JCR_NAMESPACES)) {
            setProperty(setProperties.remove(JCR_NAMESPACES));
        }
        // let super-class handle the rest of the properties
        return super.alterProperties(setProperties, removePropertyNames);
    }

    //--------------------------------------------------------------------------
    protected void initProperties() {
        super.initProperties();
        try {
            // init workspace specific properties
            NamespaceRegistry nsReg = getRepositorySession().getWorkspace().getNamespaceRegistry();
            DavProperty namespacesProp = new NamespacesProperty(nsReg);
            properties.add(namespacesProp);
        } catch (RepositoryException e) {
            log.error("Failed to access NamespaceRegistry: " + e.getMessage());
        }
    }
}