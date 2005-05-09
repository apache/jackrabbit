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
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.jdom.Element;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Item;

/**
 * <code>RootItemCollection</code> represents the root node of the underlaying
 * repository. However, the display name the name of the workspace is returned
 * the root node is located.
 *
 * @todo currently the jcr root node is the same for all workspace resources... this is wrong...
 */
public class RootItemCollection extends VersionControlledItemCollection {

    private static Logger log = Logger.getLogger(RootItemCollection.class);

    /**
     * Create a new <code>RootItemCollection</code>.
     *
     * @param locator
     * @param session
     */
    protected RootItemCollection(DavResourceLocator locator, DavSession session,
                                 DavResourceFactory factory, Item item) {
        super(locator, session, factory, item);
    }

    //----------------------------------------------< DavResource interface >---
    /**
     * Returns the name of the workspace the underlaying root item forms part of.
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

    public void setProperty(DavProperty property) throws DavException {
	if (ItemResourceConstants.JCR_NAMESPACES.equals(property.getName())) {
	    // todo: register and unregister namespaces
	} else {
	    super.setProperty(property);
	}
    }

    //--------------------------------------------------------------------------
    protected void initProperties() {
	super.initProperties();
	try {
	    // init workspace specific properties
	    NamespaceRegistry nsReg = getRepositorySession().getWorkspace().getNamespaceRegistry();
	    String[] prefixes = nsReg.getPrefixes();
	    Element[] nsElems = new Element[prefixes.length];
	    for (int i = 0; i < prefixes.length; i++) {
		Element elem = new Element(XML_NAMESPACE, NAMESPACE);
                elem.addContent(new Element(XML_PREFIX).setText(prefixes[i]));
                elem.addContent(new Element(XML_URI)).setText(nsReg.getURI(prefixes[i]));
		nsElems[i] = elem;
	    }
	    properties.add(new DefaultDavProperty(ItemResourceConstants.JCR_NAMESPACES, nsElems, false));
	} catch (RepositoryException e) {
	    log.error("Failed to access NamespaceRegistry from the session/workspace: " + e.getMessage());
	}
    }
}