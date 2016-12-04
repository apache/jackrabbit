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
package org.apache.jackrabbit.webdav.jcr.version;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.jcr.DefaultItemCollection;
import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.jcr.property.JcrDavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.HrefProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.apache.jackrabbit.webdav.version.VersionHistoryResource;
import org.apache.jackrabbit.webdav.version.VersionResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import java.util.ArrayList;

/**
 * <code>VersionHistoryItemCollection</code> represents a JCR version history.
 *
 * @see VersionHistory
 */
public class VersionHistoryItemCollection extends DefaultItemCollection
        implements VersionHistoryResource {

    private static Logger log = LoggerFactory.getLogger(VersionHistoryItemCollection.class);

    /**
     * Create a new <code>VersionHistoryItemCollection</code> resource.
     *
     * @param resourcePath
     * @param session
     * @param factory
     */
    public VersionHistoryItemCollection(DavResourceLocator resourcePath,
                                        JcrDavSession session,
                                        DavResourceFactory factory,
                                        Item item) {
        super(resourcePath, session, factory, item);
        if (item == null || !(item instanceof VersionHistory)) {
            throw new IllegalArgumentException("VersionHistory item expected.");
        }
    }

    //----------------------------------------------< DavResource interface >---
    /**
     * @see org.apache.jackrabbit.webdav.DavResource#getSupportedMethods()
     */
    @Override
    public String getSupportedMethods() {
        StringBuffer sb = new StringBuffer(ItemResourceConstants.METHODS);
        sb.append(", ").append(VersionHistoryResource.METHODS);
        return sb.toString();
    }

    @Override
    public DavProperty<?> getProperty(DavPropertyName name) {
        DavProperty prop = super.getProperty(name);
        if (prop == null) {
            // required, protected version-set property for version-history resource
            try {
                if (ROOT_VERSION.equals(name)) {
                    // required root-version property for version-history resource
                    String rootVersionHref = getLocatorFromItem(((VersionHistory)item).getRootVersion()).getHref(true);
                    prop = new HrefProperty(ROOT_VERSION, rootVersionHref, true);
                } else if (VERSION_SET.equals(name)) {
                    VersionIterator vIter = ((VersionHistory) item).getAllVersions();
                    prop = getHrefProperty(VERSION_SET, vIter, true);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        
        return prop;
    }

    /**
     * Removing a version resource is achieved by calling <code>removeVersion</code>
     * on the versionhistory item this version belongs to.
     *
     * @throws DavException if the version does not exist or if an error occurs
     * while deleting.
     * @see DavResource#removeMember(org.apache.jackrabbit.webdav.DavResource)
     */
    @Override
    public void removeMember(DavResource member) throws DavException {
        if (exists()) {
            VersionHistory versionHistory = (VersionHistory) item;
            try {
                versionHistory.removeVersion(getItemName(member.getLocator().getRepositoryPath()));
            } catch (RepositoryException e) {
                throw new JcrDavException(e);
            }
        } else {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
    }
    //-----------------------------------< VersionHistoryResource interface >---
    /**
     * Return an array of {@link VersionResource}s representing all versions
     * present in the underlying JCR version history.
     *
     * @return array of {@link VersionResource}s representing all versions
     * present in the underlying JCR version history.
     * @throws DavException
     * @see org.apache.jackrabbit.webdav.version.VersionHistoryResource#getVersions()
     */
    public VersionResource[] getVersions() throws DavException {
        try {
            VersionIterator vIter = ((VersionHistory)item).getAllVersions();
            ArrayList<VersionResource> l = new ArrayList<VersionResource>();
            while (vIter.hasNext()) {
                DavResourceLocator versionLoc = getLocatorFromItem(vIter.nextVersion());
                VersionResource vr = (VersionResource) createResourceFromLocator(versionLoc);
                l.add(vr);
            }
            return l.toArray(new VersionResource[l.size()]);
        } catch (RepositoryException e) {
            throw new JcrDavException(e);
        }
    }

    //--------------------------------------------------------------------------
    @Override
    protected void initPropertyNames() {
        super.initPropertyNames();

        if (exists()) {
            names.addAll(JcrDavPropertyNameSet.VERSIONHISTORY_SET);
        }
    }

    /**
     * Fill the property set for this resource.
     */
    @Override
    protected void initProperties() {
        super.initProperties();

        // change resource type defined by default item collection
        properties.add(new ResourceType(ResourceType.VERSION_HISTORY));

        // jcr specific property pointing to the node this history belongs to
        try {
            properties.add(new DefaultDavProperty<String>(JCR_VERSIONABLEUUID, ((VersionHistory)item).getVersionableIdentifier()));
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }
}
